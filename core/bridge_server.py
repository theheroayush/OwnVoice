import json
import random
import secrets
import socket
import threading
import time
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Optional, Callable
import qrcode
from PIL import Image

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

class BridgeState:
    current_pin: str = f"{random.randint(100000, 999999)}"
    pairing_token: str = secrets.token_hex(8)
    device_name: str = socket.gethostname()
    last_activity: Optional[dict] = None
    injector_callback: Optional[Callable[[str], bool]] = None
    http_port: int = 8765

class BridgeRequestHandler(BaseHTTPRequestHandler):

    def log_message(self, format, *args):
        # Suppress noisy console access logs
        pass

    def do_GET(self):
        if self.path == "/status":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            payload = json.dumps({
                "status": "ready",
                "version": "2.5.0",
                "device_name": BridgeState.device_name,
                "ip": get_local_ip(),
                "port": BridgeState.http_port,
                "last_activity": BridgeState.last_activity
            })
            self.wfile.write(payload.encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
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
                resp = json.dumps({
                    "success": True,
                    "token": BridgeState.pairing_token,
                    "device_name": BridgeState.device_name
                })
                self.wfile.write(resp.encode("utf-8"))
            else:
                self.send_response(401)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                resp = json.dumps({"success": False, "error": "Invalid 6-digit PIN"})
                self.wfile.write(resp.encode("utf-8"))

        elif self.path == "/inject":
            client_token = str(data.get("token", "")).strip()
            text = data.get("text", "")
            client_device = data.get("client", "Android Phone")

            # Validate pairing token (or allow if pin is provided)
            is_auth = (client_token == BridgeState.pairing_token) or (data.get("pin") == BridgeState.current_pin)
            # Graceful local fallback if token is absent
            if not is_auth and not client_token:
                is_auth = True

            if not is_auth:
                self.send_response(403)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                resp = json.dumps({"success": False, "error": "Unauthorized: Token mismatch"})
                self.wfile.write(resp.encode("utf-8"))
                return

            success = False
            if text and BridgeState.injector_callback:
                success = BridgeState.injector_callback(text)

            if success:
                BridgeState.last_activity = {
                    "device": client_device,
                    "time": time.time(),
                    "chars": len(text),
                    "event": "injected"
                }

            self.send_response(200 if success else 400)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            resp = json.dumps({"success": success, "injected_chars": len(text)})
            self.wfile.write(resp.encode("utf-8"))
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
                            "version": "2.5.0",
                            "device_name": BridgeState.device_name,
                            "ip": current_ip,
                            "port": self.http_port,
                            "pin": BridgeState.current_pin,
                            "token": BridgeState.pairing_token
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


class BridgeServer:
    """
    Universal Wi-Fi Dictation Bridge server for Windows Desktop.
    Provides HTTP keystroke receiver, UDP auto-discovery, and QR code generation.
    """

    def __init__(self, port: int = 8765, on_inject: Optional[Callable[[str], bool]] = None):
        self.port = port
        self.local_ip = get_local_ip()
        BridgeState.http_port = port
        BridgeState.injector_callback = on_inject
        self.server: Optional[HTTPServer] = None
        self._thread: Optional[threading.Thread] = None
        self.discovery = BridgeDiscoveryResponder(http_port=port, discovery_port=8766)
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
        return BridgeState.current_pin

    def get_pairing_uri(self) -> str:
        ip = get_local_ip()
        return f"ownvoice://pair?ip={ip}&port={self.port}&name={BridgeState.device_name}&pin={BridgeState.current_pin}&token={BridgeState.pairing_token}"

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
            self.server = HTTPServer(("0.0.0.0", self.port), BridgeRequestHandler)
            self.is_running = True
            self._thread = threading.Thread(target=self.server.serve_forever, daemon=True, name="BridgeServerHTTP")
            self._thread.start()
            self.discovery.start()
            print(f"[BridgeServer] Listening on {self.local_ip}:{self.port} (UDP Discovery on 8766)")
        except Exception as e:
            print(f"[BridgeServer] Could not start server: {e}")
            self.is_running = False

    def stop(self):
        self.discovery.stop()
        if self.server:
            try:
                self.server.shutdown()
                self.server.server_close()
            except Exception:
                pass
            self.server = None
        self.is_running = False
