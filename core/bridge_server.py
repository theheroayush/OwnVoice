import json
import base64
import urllib.parse
import random
import secrets
import socket
import threading
import time
from pathlib import Path
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Optional, Callable, Set, Dict, Any
import qrcode
from PIL import Image
from core.injector import CursorInjector
from core.selection_reader import SelectionReader

_default_injector = CursorInjector()

def get_local_ip() -> str:
    """Finds the primary local LAN IPv4 address."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Doesn't actually connect, just probes route to default gateway
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "127.0.0.1"
    finally:
        s.close()
    return ip

def is_private_ip(ip: str) -> bool:
    """Checks if an incoming client IP is on a local private network or loopback."""
    if not ip or ip in ("127.0.0.1", "localhost", "::1", "::ffff:127.0.0.1"):
        return True
    return (
        ip.startswith("192.168.") or
        ip.startswith("10.") or
        ip.startswith("172.") or
        ip.startswith("::ffff:192.168.") or
        ip.startswith("::ffff:10.")
    )

AUTH_FILE = Path.home() / ".ownvoice_bridge_auth.json"

def _load_or_create_auth():
    try:
        if AUTH_FILE.exists():
            with open(AUTH_FILE, "r", encoding="utf-8") as f:
                data = json.load(f)
                pin = str(data.get("pin", "")).strip()
                token = str(data.get("token", "")).strip()
                if len(pin) == 6 and token:
                    return pin, token
    except Exception:
        pass
    new_pin = f"{random.randint(100000, 999999)}"
    new_token = secrets.token_hex(8)
    try:
        with open(AUTH_FILE, "w", encoding="utf-8") as f:
            json.dump({"pin": new_pin, "token": new_token}, f)
    except Exception:
        pass
    return new_pin, new_token

def log_bridge_event(msg: str):
    try:
        from app import log_event
        log_event(f"[Bridge] {msg}")
    except Exception:
        print(f"[Bridge] {msg}")

_init_pin, _init_token = _load_or_create_auth()

class BridgeState:
    current_pin: str = _init_pin
    pairing_token: str = _init_token
    device_name: str = socket.gethostname()
    api_key: str = ""
    last_activity: Optional[dict] = None
    injector_callback: Optional[Callable[[str], bool]] = None
    injector_instance: Optional[Any] = _default_injector
    open_link_callback: Optional[Callable[[], None]] = None
    http_port: int = 8765
    latest_pc_clipboard: dict = {"text": "", "timestamp": 0.0, "seq": 0}
    recent_injected_clips: Set[str] = set()
    cloud_relay_url: str = "http://127.0.0.1:8767"
    cloud_relay_connected: bool = False
    note_store: Optional[Any] = None
    note_engine: Optional[Any] = None
    selection_reader: Optional[Any] = None

def dispatch_bridge_payload(data: dict) -> dict:
    """Central action dispatcher handling text injection, keystrokes, trackpad, and mouse events."""
    action = str(data.get("action", "")).strip().lower()
    key = str(data.get("key", "")).strip().lower()
    text = data.get("text", "")
    injector = getattr(BridgeState, "injector_instance", None) or _default_injector

    if action == "mouse_move":
        dx = int(data.get("dx", 0))
        dy = int(data.get("dy", 0))
        ok = injector.move_mouse(dx, dy)
        return {"success": ok, "action": "mouse_move", "dx": dx, "dy": dy}
    elif action == "mouse_click":
        btn = str(data.get("button", "left"))
        double = bool(data.get("double", False))
        ok = injector.mouse_click(btn, double)
        return {"success": ok, "action": "mouse_click", "button": btn}
    elif action == "mouse_down":
        btn = str(data.get("button", "left"))
        ok = injector.mouse_down(btn)
        return {"success": ok, "action": "mouse_down", "button": btn}
    elif action == "mouse_up":
        btn = str(data.get("button", "left"))
        ok = injector.mouse_up(btn)
        return {"success": ok, "action": "mouse_up", "button": btn}
    elif action == "mouse_scroll":
        delta = int(data.get("dy", data.get("delta", 0)))
        ok = injector.mouse_scroll(delta)
        return {"success": ok, "action": "mouse_scroll", "delta": delta}
    elif action == "backspace" or key == "backspace" or text == "\b":
        try:
            from pynput.keyboard import Controller, Key as PynputKey
            kb = Controller()
            kb.tap(PynputKey.backspace)
            return {"success": True, "action": "backspace"}
        except Exception:
            return {"success": False, "error": "Backspace failed"}
    elif action == "enter" or key == "enter" or text == "\n":
        try:
            from pynput.keyboard import Controller, Key as PynputKey
            kb = Controller()
            kb.tap(PynputKey.enter)
            return {"success": True, "action": "enter"}
        except Exception:
            return {"success": False, "error": "Enter failed"}
    elif action == "clear" or key == "clear":
        ok = injector.erase_all()
        return {"success": ok, "action": "clear"}
    elif action == "clipboard_set":
        new_clip = str(data.get("text", "")).strip()
        if new_clip:
            BridgeState.recent_injected_clips.add(new_clip)
            injector.set_clipboard_text(new_clip)
        return {"success": True, "action": "clipboard_set"}
    elif text:
        # Direct keystroke or snippet injection
        if len(text) <= 3 and hasattr(injector, "_send_unicode_string"):
            try:
                ok = injector._send_unicode_string(text)
                if ok:
                    return {"success": True, "injected_chars": len(text)}
            except Exception:
                pass
        if BridgeState.injector_callback:
            try:
                ok = BridgeState.injector_callback(text)
                if ok:
                    return {"success": True, "injected_chars": len(text)}
            except Exception:
                pass
        ok = injector.inject_text(text)
        return {"success": ok, "injected_chars": len(text)}

    return {"success": False, "error": "Unknown or empty payload"}

class BridgeRequestHandler(BaseHTTPRequestHandler):

    def log_message(self, format, *args):
        # Suppress noisy default stderr console logs; we log cleanly to app.log
        pass

    def do_GET(self):
        log_bridge_event(f"GET {self.path} from {self.client_address[0]}")
        if self.path == "/open_link":
            if BridgeState.open_link_callback:
                try:
                    BridgeState.open_link_callback()
                except Exception:
                    pass
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(b'{"success": true}')
            return
        elif self.path == "/clipboard":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            payload = json.dumps({
                "success": True,
                "text": BridgeState.latest_pc_clipboard.get("text", ""),
                "timestamp": BridgeState.latest_pc_clipboard.get("timestamp", 0.0),
                "seq": BridgeState.latest_pc_clipboard.get("seq", 0)
            })
            self.wfile.write(payload.encode("utf-8"))
            return
        elif self.path == "/context/selection":
            selection = SelectionReader.get_selected_text() or ""
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps({"success": True, "selection": selection}).encode("utf-8"))
            return
        elif self.path.startswith("/notes/search"):
            parsed = urllib.parse.urlparse(self.path)
            params = urllib.parse.parse_qs(parsed.query)
            q = params.get("q", [""])[0]
            store = BridgeState.note_store
            notes = store.search_notes(q) if store else []
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps({"success": True, "notes": notes}).encode("utf-8"))
            return
        elif self.path.startswith("/notes"):
            parsed = urllib.parse.urlparse(self.path)
            params = urllib.parse.parse_qs(parsed.query)
            cat = params.get("category", [None])[0]
            store = BridgeState.note_store
            notes = store.list_notes(category=cat) if store else []
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps({"success": True, "notes": notes}).encode("utf-8"))
            return
        elif self.path == "/status":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            payload = json.dumps({
                "status": "ready",
                "version": "3.0.0",
                "device_name": BridgeState.device_name,
                "ip": get_local_ip(),
                "port": BridgeState.http_port,
                "token": BridgeState.pairing_token,
                "pin": BridgeState.current_pin,
                "api_key": BridgeState.api_key,
                "latest_clipboard": BridgeState.latest_pc_clipboard.get("text", ""),
                "clipboard_seq": BridgeState.latest_pc_clipboard.get("seq", 0),
                "relay_connected": BridgeState.cloud_relay_connected,
                "last_activity": BridgeState.last_activity
            })
            self.wfile.write(payload.encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        log_bridge_event(f"POST {self.path} from {self.client_address[0]}")
        content_len = int(self.headers.get("Content-Length", 0))
        post_body = self.rfile.read(content_len)
        try:
            data = json.loads(post_body.decode("utf-8"))
        except Exception:
            data = {}

        if self.path == "/pair":
            client_pin = str(data.get("pin", "")).strip()
            client_device = str(data.get("device", "Android Phone")).strip()
            if client_pin == BridgeState.current_pin:
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                BridgeState.last_activity = {
                    "device": client_device,
                    "time": time.time(),
                    "event": "paired"
                }
                log_bridge_event(f"Device paired successfully: {client_device} ({self.client_address[0]})")
                resp = json.dumps({
                    "success": True,
                    "token": BridgeState.pairing_token,
                    "device_name": BridgeState.device_name,
                    "api_key": BridgeState.api_key
                })
                self.wfile.write(resp.encode("utf-8"))
            else:
                self.send_response(401)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                log_bridge_event(f"PIN mismatch from {self.client_address[0]}: provided '{client_pin}', expected '{BridgeState.current_pin}'")
                resp = json.dumps({"success": False, "error": "Invalid 6-digit PIN"})
                self.wfile.write(resp.encode("utf-8"))

        elif self.path == "/clipboard":
            client_token = str(data.get("token", "")).strip()
            client_ip = self.client_address[0]
            if not (is_private_ip(client_ip) or client_token == BridgeState.pairing_token or not client_token):
                self.send_response(403)
                self.end_headers()
                return

            new_text = str(data.get("text", "")).strip()
            if new_text:
                BridgeState.recent_injected_clips.add(new_text)
                injector = getattr(BridgeState, "injector_instance", None) or _default_injector
                injector.set_clipboard_text(new_text)
                log_bridge_event(f"PC Clipboard set from phone ({len(new_text)} chars)")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps({"success": True, "text": new_text}).encode("utf-8"))
            return

        elif self.path in ("/meeting", "/meeting/save"):
            title = str(data.get("title", "Meeting_Notes")).strip()
            content = str(data.get("content", "")).strip()
            docs_dir = Path.home() / "Documents" / "OwnVoice" / "Meetings"
            docs_dir.mkdir(parents=True, exist_ok=True)
            sanitized_title = "".join(c for c in title if c.isalnum() or c in (" ", "_", "-")).strip().replace(" ", "_")
            if not sanitized_title:
                sanitized_title = "Meeting_Notes"
            timestamp_str = time.strftime("%Y%m%d_%H%M%S")
            filename = f"{sanitized_title}_{timestamp_str}.md"
            file_path = docs_dir / filename
            file_path.write_text(content, encoding="utf-8")
            log_bridge_event(f"Meeting notes saved: {file_path}")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps({"success": True, "file_path": str(file_path), "filename": filename}).encode("utf-8"))
            return

        elif self.path in ("/note/create", "/note"):
            client_token = str(data.get("token", "")).strip()
            client_ip = self.client_address[0]
            is_local = is_private_ip(client_ip)
            is_auth = (client_token == BridgeState.pairing_token) or is_local or not client_token
            if not is_auth:
                self.send_response(403)
                self.end_headers()
                return

            raw_text = str(data.get("text", "")).strip()
            audio_b64 = str(data.get("audio_b64", "")).strip()
            audio_bytes = None
            if audio_b64:
                try:
                    audio_bytes = base64.b64decode(audio_b64)
                except Exception:
                    pass

            category = str(data.get("category", "auto")).strip()
            selection = data.get("selection")
            if selection is None:
                selection = SelectionReader.get_selected_text()

            inject = bool(data.get("inject", True))
            source = str(data.get("source", "android")).strip()

            note_engine = BridgeState.note_engine
            note_store = BridgeState.note_store

            if note_engine:
                note_dict = note_engine.structure_note(
                    audio_wav_bytes=audio_bytes,
                    raw_text=raw_text,
                    category=category,
                    selection_context=selection,
                    source=source
                )
            else:
                note_dict = {
                    "title": "Voice Note",
                    "category": category if category != "auto" else "general",
                    "summary": raw_text[:120],
                    "structured_content": raw_text,
                    "raw_transcript": raw_text,
                    "tags": ["voice"],
                    "source": source
                }

            if note_store:
                nid = note_store.save_note(
                    title=note_dict.get("title", "Voice Note"),
                    category=note_dict.get("category", "general"),
                    structured_content=note_dict.get("structured_content", ""),
                    raw_transcript=note_dict.get("raw_transcript", raw_text),
                    tags=note_dict.get("tags", []),
                    source=source
                )
                note_dict["id"] = nid

            if inject:
                injector = getattr(BridgeState, "injector_instance", None) or _default_injector
                text_to_inject = note_dict.get("structured_content") or raw_text
                if text_to_inject:
                    injector.inject_text(text_to_inject)

            log_bridge_event(f"Structured note created from {source}: '{note_dict.get('title')}'")
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps({"success": True, "note": note_dict}).encode("utf-8"))
            return

        elif self.path in ("/inject", "/mouse"):
            client_token = str(data.get("token", "")).strip()
            text = data.get("text", "")
            client_device = data.get("client", "Android Phone")
            client_ip = self.client_address[0]

            is_local = is_private_ip(client_ip)
            is_auth = (
                (client_token == BridgeState.pairing_token) or
                (data.get("pin") == BridgeState.current_pin) or
                is_local or
                not client_token
            )

            if not is_auth:
                self.send_response(403)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                log_bridge_event(f"Unauthorized injection attempt from {client_ip}")
                resp = json.dumps({"success": False, "error": "Unauthorized: Token mismatch"})
                self.wfile.write(resp.encode("utf-8"))
                return

            res = dispatch_bridge_payload(data)
            success = res.get("success", False)

            if success:
                BridgeState.last_activity = {
                    "device": client_device,
                    "time": time.time(),
                    "chars": len(str(text)),
                    "event": res.get("action", "injected")
                }
                log_bridge_event(f"Handled action '{res.get('action')}' from {client_device} ({client_ip})")
            else:
                log_bridge_event(f"Action FAILED from {client_device} ({client_ip}): {res}")

            self.send_response(200 if success else 400)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps(res).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()


class BridgeDiscoveryResponder:
    """
    Listens on UDP port 8766 for discovery probes from Android devices on the local Wi-Fi.
    Replies with JSON metadata containing IP, port, device name, PIN, and pairing token.
    """
    def __init__(self, http_port: int = 8765, discovery_port: int = 8766):
        self.http_port = http_port
        self.discovery_port = discovery_port
        self.is_running = False
        self._thread: Optional[threading.Thread] = None
        self._sock: Optional[socket.socket] = None

    def start(self):
        if self.is_running:
            return
        self.is_running = True
        self._thread = threading.Thread(target=self._listen_loop, daemon=True, name="BridgeDiscoveryResponder")
        self._thread.start()

    def _listen_loop(self):
        try:
            self._sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
            self._sock.bind(("0.0.0.0", self.discovery_port))
            self._sock.settimeout(1.0)

            while self.is_running:
                try:
                    data, addr = self._sock.recvfrom(2048)
                    msg = data.decode("utf-8", errors="ignore").strip()
                    if "DISCOVER" in msg.upper() or "OWNVOICE" in msg.upper():
                        current_ip = get_local_ip()
                        resp_data = {
                            "service": "ownvoice-bridge",
                            "version": "3.0.0",
                            "device_name": BridgeState.device_name,
                            "ip": current_ip,
                            "port": self.http_port,
                            "pin": BridgeState.current_pin,
                            "token": BridgeState.pairing_token,
                            "api_key": BridgeState.api_key
                        }
                        payload = json.dumps(resp_data).encode("utf-8")
                        self._sock.sendto(payload, addr)
                except socket.timeout:
                    continue
                except Exception:
                    pass
        except Exception as e:
            print(f"[BridgeDiscovery] Error in discovery listener: {e}")
        finally:
            if self._sock:
                try:
                    self._sock.close()
                except Exception:
                    pass

    def stop(self):
        self.is_running = False
        if self._sock:
            try:
                self._sock.close()
            except Exception:
                pass


class ClipboardSyncWatcher:
    """Watches Windows clipboard sequence counter and caches PC clipboard updates."""
    def __init__(self, injector, poll_interval: float = 0.3):
        self.injector = injector
        self.poll_interval = poll_interval
        self.is_running = False
        self._thread: Optional[threading.Thread] = None
        self._last_seq = 0
        self._last_text = ""

    def start(self):
        if self.is_running:
            return
        self.is_running = True
        self._thread = threading.Thread(target=self._run, daemon=True, name="ClipboardSyncWatcher")
        self._thread.start()

    def _run(self):
        while self.is_running:
            try:
                seq = self.injector.get_clipboard_seq()
                if seq != self._last_seq and seq > 0:
                    self._last_seq = seq
                    text = self.injector.get_clipboard_text()
                    if text and text != self._last_text:
                        self._last_text = text
                        # Don't echo text that the phone just pushed to PC
                        if text not in BridgeState.recent_injected_clips:
                            BridgeState.latest_pc_clipboard = {
                                "text": text,
                                "timestamp": time.time(),
                                "seq": seq
                            }
                            log_bridge_event(f"PC Clipboard captured ({len(text)} chars)")
                        else:
                            BridgeState.recent_injected_clips.discard(text)
            except Exception:
                pass
            time.sleep(self.poll_interval)

    def stop(self):
        self.is_running = False


class CloudRelayWorker:
    """Connects to the Zero-Knowledge Cloud Relay as 'pc' to receive commands from anywhere in the world."""
    def __init__(self, relay_url: str = "http://127.0.0.1:8767"):
        self.relay_url = relay_url
        self.is_running = False
        self._thread: Optional[threading.Thread] = None

    def start(self):
        if self.is_running or not self.relay_url:
            return
        self.is_running = True
        self._thread = threading.Thread(target=self._run_loop, daemon=True, name="CloudRelayWorker")
        self._thread.start()

    def _run_loop(self):
        import asyncio
        try:
            asyncio.run(self._async_loop())
        except Exception:
            pass

    async def _async_loop(self):
        try:
            import aiohttp
        except ImportError:
            log_bridge_event("aiohttp not installed; Cloud Relay worker disabled")
            return

        ws_url = self.relay_url.replace("http://", "ws://").replace("https://", "wss://")
        if not ws_url.endswith("/ws"):
            ws_url = ws_url.rstrip("/") + "/ws"

        full_url = f"{ws_url}?room={BridgeState.pairing_token}&role=pc"
        log_bridge_event(f"Cloud Relay worker starting: {full_url}")

        while self.is_running:
            try:
                timeout = aiohttp.ClientTimeout(total=None)
                async with aiohttp.ClientSession(timeout=timeout) as session:
                    async with session.ws_connect(full_url, heartbeat=20) as ws:
                        BridgeState.cloud_relay_connected = True
                        log_bridge_event("Connected to Cloud Relay successfully")

                        async for msg in ws:
                            if not self.is_running:
                                break
                            if msg.type == aiohttp.WSMsgType.TEXT:
                                try:
                                    payload = json.loads(msg.data)
                                    res = dispatch_bridge_payload(payload)
                                    await ws.send_str(json.dumps(res))
                                except Exception as e:
                                    log_bridge_event(f"Relay dispatch error: {e}")
                            elif msg.type in (aiohttp.WSMsgType.CLOSED, aiohttp.WSMsgType.ERROR):
                                break
            except Exception:
                BridgeState.cloud_relay_connected = False
                import asyncio
                await asyncio.sleep(4)
            finally:
                BridgeState.cloud_relay_connected = False

    def stop(self):
        self.is_running = False


class CustomHTTPServer(HTTPServer):
    allow_reuse_address = True

class BridgeServer:
    """
    Universal Wi-Fi Dictation Bridge server for Windows Desktop.
    Provides HTTP keystroke receiver, UDP auto-discovery, and QR code generation.
    """

    def __init__(
        self,
        port: int = 8765,
        on_inject: Optional[Callable[[str], bool]] = None,
        api_key: str = "",
        on_open_link: Optional[Callable[[], None]] = None,
        cloud_relay_url: str = "http://127.0.0.1:8767",
        note_store: Optional[Any] = None,
        note_engine: Optional[Any] = None,
        selection_reader: Optional[Any] = None
    ):
        self.port = port
        self.local_ip = get_local_ip()
        BridgeState.http_port = port
        BridgeState.injector_callback = on_inject
        BridgeState.cloud_relay_url = cloud_relay_url
        if note_store:
            BridgeState.note_store = note_store
        if note_engine:
            BridgeState.note_engine = note_engine
        if selection_reader:
            BridgeState.selection_reader = selection_reader

        if on_inject and hasattr(on_inject, "__self__"):
            BridgeState.injector_instance = on_inject.__self__
        else:
            BridgeState.injector_instance = _default_injector

        if on_open_link:
            BridgeState.open_link_callback = on_open_link
        if api_key:
            BridgeState.api_key = api_key

        self.server: Optional[HTTPServer] = None
        self._thread: Optional[threading.Thread] = None
        self.discovery = BridgeDiscoveryResponder(http_port=port, discovery_port=8766)
        self.clipboard_watcher = ClipboardSyncWatcher(injector=BridgeState.injector_instance)
        self.cloud_relay_worker = CloudRelayWorker(relay_url=cloud_relay_url)
        self.is_running = False

    @property
    def current_pin(self) -> str:
        return BridgeState.current_pin

    @property
    def pairing_token(self) -> str:
        return BridgeState.pairing_token

    @property
    def device_name(self) -> str:
        return BridgeState.device_name

    def regenerate_pin(self) -> str:
        BridgeState.current_pin = f"{random.randint(100000, 999999)}"
        BridgeState.pairing_token = secrets.token_hex(8)
        try:
            with open(AUTH_FILE, "w", encoding="utf-8") as f:
                json.dump({"pin": BridgeState.current_pin, "token": BridgeState.pairing_token}, f)
        except Exception:
            pass
        return BridgeState.current_pin

    def get_pairing_uri(self) -> str:
        ip = get_local_ip()
        uri = f"ownvoice://pair?ip={ip}&port={self.port}&name={BridgeState.device_name}&pin={BridgeState.current_pin}&token={BridgeState.pairing_token}"
        if BridgeState.api_key:
            uri += f"&api_key={BridgeState.api_key}"
        return uri

    def generate_qr_image(self, size: int = 240) -> Image.Image:
        """Generates a high-contrast PIL Image QR code representing the pairing URI."""
        uri = self.get_pairing_uri()
        qr = qrcode.QRCode(
            version=1,
            error_correction=qrcode.constants.ERROR_CORRECT_M,
            box_size=8,
            border=2,
        )
        qr.add_data(uri)
        qr.make(fit=True)
        img = qr.make_image(fill_color="#0F172A", back_color="#FFFFFF").convert("RGB")
        return img.resize((size, size), Image.Resampling.LANCZOS)

    def start(self):
        if self.is_running:
            return

        try:
            self.server = CustomHTTPServer(("0.0.0.0", self.port), BridgeRequestHandler)
            self.is_running = True
            self._thread = threading.Thread(target=self.server.serve_forever, daemon=True, name="BridgeServerHTTP")
            self._thread.start()
            self.discovery.start()
            self.clipboard_watcher.start()
            self.cloud_relay_worker.start()
            log_bridge_event(f"Listening on {self.local_ip}:{self.port} (UDP Discovery on 8766)")
        except Exception as e:
            log_bridge_event(f"Could not start server on port {self.port}: {e}")
            self.is_running = False

    def stop(self):
        self.discovery.stop()
        self.clipboard_watcher.stop()
        self.cloud_relay_worker.stop()
        if self.server:
            try:
                self.server.shutdown()
                self.server.server_close()
            except Exception:
                pass
            self.server = None
        self.is_running = False
