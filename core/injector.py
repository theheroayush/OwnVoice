import time
import ctypes
from ctypes import wintypes
import pyperclip
from pynput.keyboard import Controller, Key

user32 = ctypes.windll.user32
kernel32 = ctypes.windll.kernel32
kb = Controller()

VK_CONTROL = 0x11
VK_V = 0x56
KEYEVENTF_KEYUP = 0x0002

class CursorInjector:
    def __init__(self, config_manager):
        self.config = config_manager
        self.last_target_hwnd = None
        self.overlay_hwnd = None

    def set_overlay_hwnd(self, hwnd):
        self.overlay_hwnd = hwnd

    def update_target_hwnd(self, hwnd=None):
        if hwnd is None:
            hwnd = user32.GetForegroundWindow()
        if hwnd != 0 and hwnd != self.overlay_hwnd:
            self.last_target_hwnd = hwnd

    def refocus_target(self):
        """Brings the target search box / document window back to the absolute foreground."""
        if self.last_target_hwnd and user32.IsWindow(self.last_target_hwnd):
            try:
                cur_thread = kernel32.GetCurrentThreadId()
                target_thread = user32.GetWindowThreadProcessId(self.last_target_hwnd, None)
                
                user32.AttachThreadInput(cur_thread, target_thread, True)
                user32.SetForegroundWindow(self.last_target_hwnd)
                user32.BringWindowToTop(self.last_target_hwnd)
                user32.SetFocus(self.last_target_hwnd)
                user32.AttachThreadInput(cur_thread, target_thread, False)
            except Exception as e:
                print(f"Error refocusing target window: {e}")

    def inject_text(self, text: str):
        """
        Injects text into whatever search box, document, or app the user was focused on.
        Uses dual-layer strategy: Active Window Focus Restoration + Clipboard Ctrl+V + pynput fallback.
        """
        if not text:
            return

        print(f"[INJECTOR] Starting injection for: '{text}' (Target HWND: {self.last_target_hwnd})")

        # 1. Re-focus the target window / search box
        self.refocus_target()
        time.sleep(0.06)

        # 2. Put text on clipboard
        try:
            pyperclip.copy(text)
        except Exception as e:
            print(f"Clipboard copy error: {e}")

        time.sleep(0.04)

        # 3. Simulate Ctrl + V using Win32 keybd_event & pynput
        try:
            user32.keybd_event(VK_CONTROL, 0, 0, 0)
            user32.keybd_event(VK_V, 0, 0, 0)
            time.sleep(0.03)
            user32.keybd_event(VK_V, 0, KEYEVENTF_KEYUP, 0)
            user32.keybd_event(VK_CONTROL, 0, KEYEVENTF_KEYUP, 0)
        except Exception as e:
            print(f"Win32 keybd_event paste error: {e}")

        # Fallback with pynput controller
        try:
            with kb.pressed(Key.ctrl):
                kb.press('v')
                kb.release('v')
        except Exception as e:
            print(f"pynput paste error: {e}")

        print(f"[INJECTOR] Injection completed successfully for '{text[:40]}'")
