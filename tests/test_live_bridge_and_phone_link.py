import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import time
import socket
import urllib.request
import json
from config import config_manager
from core.bridge_server import BridgeServer
from core.injector import CursorInjector
from core.note_store import NoteStore

def run_live_bridge_test():
    print("================================================================")
    print(" OWNVOICE LIVE DESKTOP & ANDROID BRIDGE TEST SUITE")
    print("================================================================")

    store = NoteStore()
    injector = CursorInjector(config_manager)
    bs = BridgeServer(
        port=8765,
        on_inject=injector.inject_text,
        note_store=store
    )
    bs.start()
    time.sleep(0.5)

    ip = bs.local_ip
    port = bs.port
    pin = bs.current_pin
    token = bs.pairing_token

    print(f"[*] Desktop Bridge running at http://{ip}:{port}")
    print(f"[*] Pairing PIN: {pin} | Token: {token}")

    def post(path, data):
        url = f"http://{ip}:{port}{path}"
        req = urllib.request.Request(
            url,
            data=json.dumps(data).encode("utf-8"),
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req, timeout=3) as resp:
            return json.loads(resp.read().decode("utf-8"))

    def get(path):
        url = f"http://{ip}:{port}{path}"
        with urllib.request.urlopen(url, timeout=3) as resp:
            return json.loads(resp.read().decode("utf-8"))

    results = {}

    try:
        # 1. Desktop Status Check
        status = get("/status")
        results["desktop_status"] = (status.get("status") == "ready" and status.get("device_name") != "")
        print(f"[1] Desktop Status Check: {'PASS' if results['desktop_status'] else 'FAIL'}")

        # 2. UDP Discovery Responder
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.settimeout(2.0)
        sock.sendto(b'{"cmd":"discover"}', ("127.0.0.1", 8766))
        udp_data, _ = sock.recvfrom(2048)
        udp_json = json.loads(udp_data.decode("utf-8"))
        results["udp_discovery"] = (udp_json.get("service") == "ownvoice-bridge" and udp_json.get("port") == port)
        sock.close()
        print(f"[2] UDP Wi-Fi Auto-Discovery: {'PASS' if results['udp_discovery'] else 'FAIL'}")

        # 3. Phone Pairing Handshake
        pair_res = post("/pair", {"pin": pin, "device": "Android Phone Live Test"})
        results["pairing"] = (pair_res.get("success") == True and pair_res.get("token") == token)
        print(f"[3] Phone Pairing Handshake: {'PASS' if results['pairing'] else 'FAIL'}")

        # 4. Air-Typing: Single Character Injection
        type_char = post("/inject", {"text": "A", "action": "type", "token": token, "client": "Android Keyboard"})
        results["air_typing_char"] = (type_char.get("success") == True and type_char.get("injected_chars") == 1)
        print(f"[4] Phone Air-Typing (Character): {'PASS' if results['air_typing_char'] else 'FAIL'}")

        # 5. Air-Typing: Word / Sentence Injection
        type_word = post("/inject", {"text": "OwnVoice Live Test", "action": "type", "token": token, "client": "Android Keyboard"})
        results["air_typing_phrase"] = (type_word.get("success") == True and type_word.get("injected_chars") == len("OwnVoice Live Test"))
        print(f"[5] Phone Air-Typing (Phrase): {'PASS' if results['air_typing_phrase'] else 'FAIL'}")

        # 6. Phone Keystroke: Backspace
        bs_res = post("/inject", {"action": "backspace", "token": token, "client": "Android Keyboard"})
        results["backspace"] = (bs_res.get("success") == True and bs_res.get("action") == "backspace")
        print(f"[6] Phone Remote Backspace: {'PASS' if results['backspace'] else 'FAIL'}")

        # 7. Phone Keystroke: Enter
        enter_res = post("/inject", {"action": "enter", "token": token, "client": "Android Keyboard"})
        results["enter"] = (enter_res.get("success") == True and enter_res.get("action") == "enter")
        print(f"[7] Phone Remote Enter: {'PASS' if results['enter'] else 'FAIL'}")

        # 8. Phone Trackpad: Mouse Move
        mouse_move = post("/inject", {"action": "mouse_move", "dx": 15, "dy": 15, "token": token})
        results["mouse_move"] = (mouse_move.get("success") == True)
        print(f"[8] Phone Trackpad Movement: {'PASS' if results['mouse_move'] else 'FAIL'}")

        # 9. Phone Trackpad: Mouse Click
        mouse_click = post("/inject", {"action": "mouse_click", "button": "left", "token": token})
        results["mouse_click"] = (mouse_click.get("success") == True)
        print(f"[9] Phone Trackpad Click: {'PASS' if results['mouse_click'] else 'FAIL'}")

        # 10. Phone Trackpad: Mouse Scroll
        mouse_scroll = post("/inject", {"action": "mouse_scroll", "delta": 2, "token": token})
        results["mouse_scroll"] = (mouse_scroll.get("success") == True)
        print(f"[10] Phone Trackpad Scroll: {'PASS' if results['mouse_scroll'] else 'FAIL'}")

        # 11. Cross-Device Clipboard: Phone pushes to PC
        clip_push = post("/clipboard", {"text": "Copied on Android Phone", "token": token})
        results["clipboard_push"] = (clip_push.get("success") == True)
        print(f"[11] Phone-to-PC Clipboard Push: {'PASS' if results['clipboard_push'] else 'FAIL'}")

        # 12. Cross-Device Clipboard: Phone reads PC clipboard
        clip_get = get("/clipboard")
        results["clipboard_pull"] = (clip_get.get("success") == True and "OwnVoice" in clip_get.get("text", ""))
        print(f"[12] PC-to-Phone Clipboard Read: {'PASS' if results['clipboard_pull'] else 'FAIL'}")

        # 13. Voice Note Sync: Phone sends structured note to Laptop SQLite
        note_res = post("/note/create", {
            "text": "Meeting with investors discussed pre-seed round at 10M valuation",
            "category": "general",
            "source": "android",
            "inject": False,
            "token": token
        })
        note_dict = note_res.get("note", {})
        results["note_sync"] = (note_res.get("success") == True and note_dict.get("id") is not None)
        print(f"[13] Phone Voice Note SQLite Sync: {'PASS' if results['note_sync'] else 'FAIL'}")

        # 14. QR Code & Pairing URI Generation
        uri = bs.get_pairing_uri()
        qr_img = bs.generate_qr_image(200)
        results["qr_code"] = (uri.startswith("ownvoice://pair") and qr_img is not None and qr_img.size == (200, 200))
        print(f"[14] Phone Link Pairing URI & QR: {'PASS' if results['qr_code'] else 'FAIL'}")

        # 15. Meeting Mode: Phone syncs meeting notes directly to PC Documents/OwnVoice/Meetings
        meeting_res = post("/meeting/save", {
            "title": "Strategy Session",
            "content": "# Strategy Session\n- Focus on tier-1 craft\n- Native audio capture",
            "token": token
        })
        saved_file = Path(meeting_res.get("file_path", "")) if meeting_res.get("file_path") else None
        results["meeting_mode_sync"] = (
            meeting_res.get("success") == True and
            saved_file is not None and
            saved_file.exists() and
            "Strategy Session" in saved_file.read_text(encoding="utf-8")
        )
        print(f"[15] Phone-to-PC Meeting Mode Document Sync: {'PASS' if results['meeting_mode_sync'] else 'FAIL'}")

    finally:
        bs.stop()

    print("================================================================")
    all_passed = all(results.values())
    total = len(results)
    passed = sum(1 for v in results.values() if v)
    print(f" SUMMARY: {passed}/{total} Live Integration Tests Passed")
    print(f" OVERALL RESULT: {'[SUCCESS] ALL SYSTEMS OPERATIONAL' if all_passed else '[FAILURE] SOME TESTS FAILED'}")
    print("================================================================")
    assert all_passed, "Not all tests passed!"

if __name__ == "__main__":
    run_live_bridge_test()
