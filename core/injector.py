import os
import time
import ctypes
from ctypes import wintypes
from typing import Optional
from pynput.keyboard import Controller

user32 = ctypes.WinDLL("user32")
kernel32 = ctypes.WinDLL("kernel32")
kb = Controller()

INPUT_KEYBOARD = 1
VK_CONTROL = 0x11
VK_V = 0x56
VK_MENU = 0x12
VK_ESCAPE = 0x1B
KEYEVENTF_KEYUP = 0x0002
KEYEVENTF_UNICODE = 0x0004
CF_UNICODETEXT = 13
GMEM_MOVEABLE = 0x0002
SW_RESTORE = 9

class MOUSEINPUT(ctypes.Structure):
    _fields_ = [
        ("dx", wintypes.LONG),
        ("dy", wintypes.LONG),
        ("mouseData", wintypes.DWORD),
        ("dwFlags", wintypes.DWORD),
        ("time", wintypes.DWORD),
        ("dwExtraInfo", ctypes.c_size_t),
    ]

class KEYBDINPUT(ctypes.Structure):
    _fields_ = [
        ("wVk", wintypes.WORD),
        ("wScan", wintypes.WORD),
        ("dwFlags", wintypes.DWORD),
        ("time", wintypes.DWORD),
        ("dwExtraInfo", ctypes.c_size_t),
    ]

class HARDWAREINPUT(ctypes.Structure):
    _fields_ = [
        ("uMsg", wintypes.DWORD),
        ("wParamL", wintypes.WORD),
        ("wParamH", wintypes.WORD),
    ]

class _INPUT_UNION(ctypes.Union):
    _fields_ = [
        ("mi", MOUSEINPUT),
        ("ki", KEYBDINPUT),
        ("hi", HARDWAREINPUT),
    ]

class INPUT(ctypes.Structure):
    _anonymous_ = ("u",)
    _fields_ = [
        ("type", wintypes.DWORD),
        ("u", _INPUT_UNION),
    ]

user32.SendInput.argtypes = [wintypes.UINT, ctypes.POINTER(INPUT), ctypes.c_int]
user32.SendInput.restype = wintypes.UINT
user32.OpenClipboard.argtypes = [wintypes.HWND]
user32.OpenClipboard.restype = wintypes.BOOL
user32.CloseClipboard.argtypes = []
user32.CloseClipboard.restype = wintypes.BOOL
user32.EmptyClipboard.argtypes = []
user32.EmptyClipboard.restype = wintypes.BOOL
user32.GetClipboardData.argtypes = [wintypes.UINT]
user32.GetClipboardData.restype = wintypes.HANDLE
user32.SetClipboardData.argtypes = [wintypes.UINT, wintypes.HANDLE]
user32.SetClipboardData.restype = wintypes.HANDLE
user32.IsClipboardFormatAvailable.argtypes = [wintypes.UINT]
user32.IsClipboardFormatAvailable.restype = wintypes.BOOL

kernel32.GlobalAlloc.argtypes = [wintypes.UINT, ctypes.c_size_t]
kernel32.GlobalAlloc.restype = wintypes.HGLOBAL
kernel32.GlobalLock.argtypes = [wintypes.HGLOBAL]
kernel32.GlobalLock.restype = wintypes.LPVOID
kernel32.GlobalUnlock.argtypes = [wintypes.HGLOBAL]
kernel32.GlobalUnlock.restype = wintypes.BOOL
kernel32.GlobalFree.argtypes = [wintypes.HGLOBAL]
kernel32.GlobalFree.restype = wintypes.HGLOBAL

user32.GetForegroundWindow.argtypes = []
user32.GetForegroundWindow.restype = wintypes.HWND
user32.GetWindowThreadProcessId.argtypes = [wintypes.HWND, ctypes.POINTER(wintypes.DWORD)]
user32.GetWindowThreadProcessId.restype = wintypes.DWORD
user32.AttachThreadInput.argtypes = [wintypes.DWORD, wintypes.DWORD, wintypes.BOOL]
user32.AttachThreadInput.restype = wintypes.BOOL
user32.SetForegroundWindow.argtypes = [wintypes.HWND]
user32.SetForegroundWindow.restype = wintypes.BOOL
user32.BringWindowToTop.argtypes = [wintypes.HWND]
user32.BringWindowToTop.restype = wintypes.BOOL
user32.IsWindow.argtypes = [wintypes.HWND]
user32.IsWindow.restype = wintypes.BOOL
user32.IsIconic.argtypes = [wintypes.HWND]
user32.IsIconic.restype = wintypes.BOOL
user32.ShowWindow.argtypes = [wintypes.HWND, ctypes.c_int]
user32.ShowWindow.restype = wintypes.BOOL
user32.keybd_event.argtypes = [wintypes.BYTE, wintypes.BYTE, wintypes.DWORD, ctypes.c_size_t]
user32.keybd_event.restype = None
kernel32.GetCurrentThreadId.argtypes = []
kernel32.GetCurrentThreadId.restype = wintypes.DWORD

class CursorInjector:
    def __init__(self, config_manager=None):
        self.config = config_manager
        self.last_target_hwnd: Optional[int] = None
        self.overlay_hwnd: Optional[int] = None
        self._clip_fallback_text = ""

    def set_overlay_hwnd(self, hwnd: int):
        self.overlay_hwnd = hwnd

    def update_target_hwnd(self, hwnd: Optional[int] = None) -> bool:
        if hwnd is None:
            hwnd = user32.GetForegroundWindow()
        if not hwnd or not user32.IsWindow(hwnd):
            return False
        if hwnd == self.overlay_hwnd:
            return False

        pid = wintypes.DWORD()
        user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
        if pid.value == os.getpid():
            return False

        self.last_target_hwnd = hwnd
        return True

    def refocus_target(self) -> bool:
        if not self.last_target_hwnd or not user32.IsWindow(self.last_target_hwnd):
            return False

        try:
            fg_hwnd = user32.GetForegroundWindow()
            if fg_hwnd == self.last_target_hwnd:
                return True

            cur_thread = kernel32.GetCurrentThreadId()
            fg_thread = user32.GetWindowThreadProcessId(fg_hwnd, None) if fg_hwnd else 0
            target_thread = user32.GetWindowThreadProcessId(self.last_target_hwnd, None)

            attached_fg = False
            attached_target = False
            try:
                if fg_thread and fg_thread != cur_thread:
                    user32.AttachThreadInput(cur_thread, fg_thread, True)
                    attached_fg = True

                if target_thread and target_thread != cur_thread:
                    user32.AttachThreadInput(cur_thread, target_thread, True)
                    attached_target = True

                # Pulse VK_MENU to bypass foreground locks
                user32.keybd_event(VK_MENU, 0, 0, 0)
                user32.keybd_event(VK_MENU, 0, KEYEVENTF_KEYUP, 0)
                # Immediately pulse VK_ESCAPE so document menu ribbon is never left active
                user32.keybd_event(VK_ESCAPE, 0, 0, 0)
                user32.keybd_event(VK_ESCAPE, 0, KEYEVENTF_KEYUP, 0)

                if user32.IsIconic(self.last_target_hwnd):
                    user32.ShowWindow(self.last_target_hwnd, SW_RESTORE)

                user32.SetForegroundWindow(self.last_target_hwnd)
                user32.BringWindowToTop(self.last_target_hwnd)
            finally:
                if attached_fg:
                    user32.AttachThreadInput(cur_thread, fg_thread, False)
                if attached_target:
                    user32.AttachThreadInput(cur_thread, target_thread, False)

            return True
        except Exception as e:
            print(f"Error refocusing target window: {e}")
            return False

    def get_clipboard_text(self) -> str:
        text = ""
        for attempt in range(5):
            if user32.OpenClipboard(self.overlay_hwnd):
                try:
                    if user32.IsClipboardFormatAvailable(CF_UNICODETEXT):
                        h_data = user32.GetClipboardData(CF_UNICODETEXT)
                        if h_data:
                            ptr = kernel32.GlobalLock(h_data)
                            if ptr:
                                try:
                                    text = ctypes.wstring_at(ptr)
                                finally:
                                    kernel32.GlobalUnlock(h_data)
                    return text
                finally:
                    user32.CloseClipboard()
            time.sleep(0.01 * (attempt + 1))
        return text or self._clip_fallback_text

    def set_clipboard_text(self, text: str) -> bool:
        if text is None:
            text = ""
        self._clip_fallback_text = text
        self._last_clip_op_real_win32 = False
        text_bytes = (text + "\0").encode("utf-16le")
        nbytes = len(text_bytes)

        hwnds_to_try = [self.overlay_hwnd, None] if self.overlay_hwnd else [None]
        for attempt in range(5):
            for h in hwnds_to_try:
                if user32.OpenClipboard(h):
                    try:
                        user32.EmptyClipboard()
                        h_glob = kernel32.GlobalAlloc(GMEM_MOVEABLE, nbytes)
                        if not h_glob:
                            continue
                        ptr = kernel32.GlobalLock(h_glob)
                        if not ptr:
                            kernel32.GlobalFree(h_glob)
                            continue
                        try:
                            ctypes.memmove(ptr, text_bytes, nbytes)
                        finally:
                            kernel32.GlobalUnlock(h_glob)
                        res = user32.SetClipboardData(CF_UNICODETEXT, h_glob)
                        if res:
                            self._last_clip_op_real_win32 = True
                            return True
                        else:
                            kernel32.GlobalFree(h_glob)
                    finally:
                        user32.CloseClipboard()
            time.sleep(0.01 * (attempt + 1))
        self._last_clip_op_real_win32 = False
        return False

    def _send_ctrl_v(self) -> bool:
        inputs = (INPUT * 4)()
        inputs[0].type = INPUT_KEYBOARD
        inputs[0].ki = KEYBDINPUT(wVk=VK_CONTROL, wScan=0, dwFlags=0, time=0, dwExtraInfo=0)
        inputs[1].type = INPUT_KEYBOARD
        inputs[1].ki = KEYBDINPUT(wVk=VK_V, wScan=0, dwFlags=0, time=0, dwExtraInfo=0)
        inputs[2].type = INPUT_KEYBOARD
        inputs[2].ki = KEYBDINPUT(wVk=VK_V, wScan=0, dwFlags=KEYEVENTF_KEYUP, time=0, dwExtraInfo=0)
        inputs[3].type = INPUT_KEYBOARD
        inputs[3].ki = KEYBDINPUT(wVk=VK_CONTROL, wScan=0, dwFlags=KEYEVENTF_KEYUP, time=0, dwExtraInfo=0)

        sent = user32.SendInput(4, inputs, ctypes.sizeof(INPUT))
        if sent < 4:
            # Fallback if SendInput is blocked by UIPI elevation
            try:
                user32.keybd_event(VK_CONTROL, 0, 0, 0)
                user32.keybd_event(VK_V, 0, 0, 0)
                time.sleep(0.02)
                user32.keybd_event(VK_V, 0, KEYEVENTF_KEYUP, 0)
                user32.keybd_event(VK_CONTROL, 0, KEYEVENTF_KEYUP, 0)
                return True
            except Exception:
                return False
        return True

    def _send_unicode_string(self, text: str) -> bool:
        inputs = []
        for ch in text:
            code = ord(ch)
            if code > 0xFFFF:
                lead = 0xD800 + ((code - 0x10000) >> 10)
                trail = 0xDC00 + ((code - 0x10000) & 0x3FF)
                surrogates = [lead, trail]
            else:
                surrogates = [code]

            for s in surrogates:
                inp_down = INPUT()
                inp_down.type = INPUT_KEYBOARD
                inp_down.ki = KEYBDINPUT(wVk=0, wScan=s, dwFlags=KEYEVENTF_UNICODE, time=0, dwExtraInfo=0)
                inputs.append(inp_down)

                inp_up = INPUT()
                inp_up.type = INPUT_KEYBOARD
                inp_up.ki = KEYBDINPUT(wVk=0, wScan=s, dwFlags=KEYEVENTF_UNICODE | KEYEVENTF_KEYUP, time=0, dwExtraInfo=0)
                inputs.append(inp_up)

        if not inputs:
            return True

        total_sent = 0
        batch_size = 128
        for i in range(0, len(inputs), batch_size):
            chunk = inputs[i : i + batch_size]
            arr = (INPUT * len(chunk))(*chunk)
            sent = user32.SendInput(len(chunk), arr, ctypes.sizeof(INPUT))
            total_sent += sent

        if total_sent == 0:
            try:
                kb.type(text)
                return True
            except Exception:
                return False
        return True

    def inject_text(self, text: str) -> bool:
        if not text:
            return False

        self.refocus_target()
        time.sleep(0.05)

        orig_clip = self.get_clipboard_text()
        clip_ok = self.set_clipboard_text(text)

        if not clip_ok or not getattr(self, "_last_clip_op_real_win32", False):
            # Real OS clipboard was inaccessible (e.g. access denied / locked).
            # Direct Unicode typing fallback via SendInput KEYEVENTF_UNICODE!
            return self._send_unicode_string(text)

        time.sleep(0.03)
        self._send_ctrl_v()

        # 250ms post-paste delay so heavy apps (Word, Chrome, Electron) have time to process WM_PASTE before clipboard restore
        time.sleep(0.25)
        if orig_clip:
            self.set_clipboard_text(orig_clip)

        return True

    def reset_streaming(self):
        """Resets the streaming text accumulator for a new recording session."""
        self._streamed_text = ""

    def inject_streaming_delta(self, current_full_text: str) -> bool:
        """
        Incrementally types newly spoken words into the active search bar/document.
        If the new text extends previous text, sends only the delta.
        If a word was re-evaluated, sends backspaces for the corrected portion.
        """
        if not current_full_text:
            return True

        if not hasattr(self, "_streamed_text"):
            self._streamed_text = ""

        self.refocus_target()

        prev = self._streamed_text
        curr = current_full_text

        if curr == prev:
            return True

        common_len = 0
        min_len = min(len(prev), len(curr))
        while common_len < min_len and prev[common_len] == curr[common_len]:
            common_len += 1

        backspaces_needed = len(prev) - common_len
        delta_to_type = curr[common_len:]

        if backspaces_needed > 0:
            VK_BACK = 0x08
            inputs = []
            for _ in range(backspaces_needed):
                d = INPUT()
                d.type = INPUT_KEYBOARD
                d.ki = KEYBDINPUT(wVk=VK_BACK, wScan=0, dwFlags=0, time=0, dwExtraInfo=0)
                u = INPUT()
                u.type = INPUT_KEYBOARD
                u.ki = KEYBDINPUT(wVk=VK_BACK, wScan=0, dwFlags=KEYEVENTF_KEYUP, time=0, dwExtraInfo=0)
                inputs.extend([d, u])
            arr = (INPUT * len(inputs))(*inputs)
            user32.SendInput(len(inputs), arr, ctypes.sizeof(INPUT))
            time.sleep(0.01)

        if delta_to_type:
            self._send_unicode_string(delta_to_type)

        self._streamed_text = curr
        return True

    def replace_streamed_text(self, old_text: str, polished_text: str) -> bool:
        """
        Seamlessly replaces the raw streamed draft with the final polished text in place.
        """
        if not polished_text or polished_text == old_text:
            return True

        if not old_text:
            return self.inject_text(polished_text)

        self.refocus_target()
        time.sleep(0.03)

        VK_BACK = 0x08
        inputs = []
        for _ in range(len(old_text)):
            d = INPUT()
            d.type = INPUT_KEYBOARD
            d.ki = KEYBDINPUT(wVk=VK_BACK, wScan=0, dwFlags=0, time=0, dwExtraInfo=0)
            u = INPUT()
            u.type = INPUT_KEYBOARD
            u.ki = KEYBDINPUT(wVk=VK_BACK, wScan=0, dwFlags=KEYEVENTF_KEYUP, time=0, dwExtraInfo=0)
            inputs.extend([d, u])

        batch_size = 128
        for i in range(0, len(inputs), batch_size):
            chunk = inputs[i : i + batch_size]
            arr = (INPUT * len(chunk))(*chunk)
            user32.SendInput(len(chunk), arr, ctypes.sizeof(INPUT))

        time.sleep(0.03)
        self.inject_text(polished_text)
        self._streamed_text = polished_text
        return True
