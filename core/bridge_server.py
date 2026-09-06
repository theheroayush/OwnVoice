import json
import socket
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler
from typing import Optional, Callable

def get_local_ip() -> str:
    """Finds the primary local LAN IPv4 address."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        # Doesn't actually connect, just probes route
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = "127.0.0.1"
    finally:
        s.close()
    return ip

class BridgeRequestHandler(BaseHTTPRequestHandler):
    injector_callback: Optional[Callable[[str], bool]] = None

    def log_message(self, format, *args):
        # Suppress noisy console access logs
        pass

    def do_GET(self):
        if self.path == "/status":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            payload = json.dumps({"status": "ready", "version": "2.1.0", "device": "Windows Desktop"})
            self.wfile.write(payload.encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        if self.path == "/inject":
            content_len = int(self.headers.get("Content-Length", 0))
            post_body = self.rfile.read(content_len)
            try:
                data = json.loads(post_body.decode("utf-8"))
                text = data.get("text", "")
                success = False
                if text and BridgeRequestHandler.injector_callback:
                    success = BridgeRequestHandler.injector_callback(text)

                self.send_response(200 if success else 400)
                self.send_header("Content-Type", "application/json")
                self.send_header("Access-Control-Allow-Origin", "*")
                self.end_headers()
                resp = json.dumps({"success": success, "injected_chars": len(text)})
                self.wfile.write(resp.encode("utf-8"))
            except Exception as e:
                self.send_response(500)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                resp = json.dumps({"success": False, "error": str(e)})
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

class BridgeServer:
    """
    Local Wi-Fi Universal Dictation Bridge server.
    Receives dictated text from Android and types directly into Windows cursor.
    """

    def __init__(self, port: int = 8765, on_inject: Optional[Callable[[str], bool]] = None):
        self.port = port
        self.local_ip = get_local_ip()
        BridgeRequestHandler.injector_callback = on_inject
        self.server: Optional[HTTPServer] = None
        self._thread: Optional[threading.Thread] = None
        self.is_running = False

    def start(self):
        if self.is_running:
            return

        try:
            self.server = HTTPServer(("0.0.0.0", self.port), BridgeRequestHandler)
            self.is_running = True
            self._thread = threading.Thread(target=self.server.serve_forever, daemon=True)
            self._thread.start()
            print(f"[BridgeServer] Listening on {self.local_ip}:{self.port}")
        except Exception as e:
            print(f"[BridgeServer] Could not start server: {e}")
            self.is_running = False

    def stop(self):
        if self.server:
            self.server.shutdown()
            self.server.server_close()
            self.server = None
        self.is_running = False
