import os
import sys
import time
import subprocess
import ctypes
from typing import Optional
from pynput.keyboard import Controller, Key
from pynput.mouse import Controller as MouseController, Button as MouseButton

IS_WINDOWS = sys.platform == "win32"
IS_MACOS = sys.platform == "darwin"
IS_LINUX = sys.platform.startswith("linux")

kb = Controller()
mouse_ctrl = MouseController()

INPUT_MOUSE = 0
INPUT_KEYBOARD = 1
MOUSEEVENTF_MOVE = 0x0001
MOUSEEVENTF_LEFTDOWN = 0x0002
MOUSEEVENTF_LEFTUP = 0x0004
MOUSEEVENTF_RIGHTDOWN = 0x0008
MOUSEEVENTF_RIGHTUP = 0x0010
MOUSEEVENTF_MIDDLEDOWN = 0x0020
MOUSEEVENTF_MIDDLEUP = 0x0040
MOUSEEVENTF_WHEEL = 0x0800
MOUSEEVENTF_ABSOLUTE = 0x8000

VK_CONTROL = 0x11
VK_V = 0x56
VK_MENU = 0x12
VK_ESCAPE = 0x1B
KEYEVENTF_KEYUP = 0x0002
KEYEVENTF_UNICODE = 0x0004
CF_UNICODETEXT = 13
GMEM_MOVEABLE = 0x0002
SW_RESTORE = 9

# Safe Win32 type definitions that evaluate on all platforms without crashing
class _DummyWinDLL:
    """Fallback dummy DLL object for non-Windows platforms and testing."""
    def __getattr__(self, name):
        def _dummy_func(*args, **kwargs):
            return 0
        return _dummy_func

if IS_WINDOWS and hasattr(ctypes, "WinDLL"):
    from ctypes import wintypes

    user32 = ctypes.WinDLL("user32")
    kernel32 = ctypes.WinDLL("kernel32")

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
    user32.GetClipboardSequenceNumber.argtypes = []
    user32.GetClipboardSequenceNumber.restype = wintypes.DWORD
    kernel32.GetCurrentThreadId.argtypes = []
    kernel32.GetCurrentThreadId.restype = wintypes.DWORD
else:
    user32 = _DummyWinDLL()
    kernel32 = _DummyWinDLL()

    class INPUT:
        pass


def _mac_get_clipboard() -> str:
    """Reads system clipboard on macOS via pbpaste."""
    try:
        proc = subprocess.run(["pbpaste"], capture_output=True, text=True, timeout=1)
        if proc.returncode == 0:
            return proc.stdout
    except Exception:
        pass
    return ""


def _mac_set_clipboard(text: str) -> bool:
    """Sets system clipboard on macOS via pbcopy."""
    try:
        proc = subprocess.run(["pbcopy"], input=text, text=True, timeout=1)
        return proc.returncode == 0
    except Exception:
        return False


def _linux_get_clipboard() -> str:
    """Reads system clipboard on Linux via wl-paste (Wayland) or xclip/xsel (X11)."""
    try:
        proc = subprocess.run(["wl-paste", "--no-newline"], capture_output=True, text=True, timeout=1)
        if proc.returncode == 0:
            return proc.stdout
    except Exception:
        pass
    try:
        proc = subprocess.run(["xclip", "-selection", "clipboard", "-o"], capture_output=True, text=True, timeout=1)
        if proc.returncode == 0:
            return proc.stdout
    except Exception:
        pass
    try:
        proc = subprocess.run(["xsel", "--clipboard", "--output"], capture_output=True, text=True, timeout=1)
        if proc.returncode == 0:
            return proc.stdout
    except Exception:
        pass
    return ""


def _linux_set_clipboard(text: str) -> bool:
    """Sets system clipboard on Linux via wl-copy (Wayland) or xclip/xsel (X11)."""
    try:
        proc = subprocess.run(["wl-copy"], input=text, text=True, timeout=1)
        if proc.returncode == 0:
            return True
    except Exception:
        pass
    try:
        proc = subprocess.run(["xclip", "-selection", "clipboard"], input=text, text=True, timeout=1)
        if proc.returncode == 0:
            return True
    except Exception:
        pass
    try:
        proc = subprocess.run(["xsel", "--clipboard", "--input"], input=text, text=True, timeout=1)
        if proc.returncode == 0:
            return True
    except Exception:
        pass
    return False


class CursorInjector:
    """
    Cross-platform cursor and clipboard injector.
    - Windows: Uses high-performance Win32 SendInput, AttachThreadInput, and native Win32 clipboard API.
    - macOS: Uses pbcopy/pbpaste and pynput Command+V paste / typing.
    - Linux: Uses wl-copy/xclip and pynput Ctrl+V paste / typing.
    """
    def __init__(self, config_manager=None):
        self.config = config_manager
        self.last_target_hwnd: Optional[int] = None
        self.overlay_hwnd: Optional[int] = None
        self._clip_fallback_text = ""
        self.last_injected_length = 0
        self._last_clip_op_real_win32 = False

    def set_overlay_hwnd(self, hwnd: int):
        self.overlay_hwnd = hwnd

    def update_target_hwnd(self, hwnd: Optional[int] = None) -> bool:
        if not (IS_WINDOWS and not isinstance(user32, _DummyWinDLL)):
            return True

        if hwnd is None:
            hwnd = user32.GetForegroundWindow()
        if not hwnd or not user32.IsWindow(hwnd):
            return False
        if hwnd == self.overlay_hwnd:
            return False

        from ctypes import wintypes
        pid = wintypes.DWORD()
        user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
        if pid.value == os.getpid():
            return False

        self.last_target_hwnd = hwnd
        return True

    def refocus_target(self) -> bool:
        if not (IS_WINDOWS and not isinstance(user32, _DummyWinDLL)):
            return True

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
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
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

        if IS_MACOS:
            val = _mac_get_clipboard()
            return val if val else self._clip_fallback_text

        if IS_LINUX:
            val = _linux_get_clipboard()
            return val if val else self._clip_fallback_text

        return self._clip_fallback_text

    def set_clipboard_text(self, text: str) -> bool:
        if text is None:
            text = ""
        self._clip_fallback_text = text
        self._last_clip_op_real_win32 = False

        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
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

        if IS_MACOS:
            ok = _mac_set_clipboard(text)
            self._last_clip_op_real_win32 = ok
            return ok

        if IS_LINUX:
            ok = _linux_set_clipboard(text)
            self._last_clip_op_real_win32 = ok
            return ok

        return True

    def _send_ctrl_v(self) -> bool:
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
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

        if IS_MACOS:
            try:
                with kb.pressed(Key.cmd):
                    kb.tap('v')
                return True
            except Exception:
                return False

        if IS_LINUX:
            try:
                with kb.pressed(Key.ctrl):
                    kb.tap('v')
                return True
            except Exception:
                return False

        return False

    def _send_unicode_string(self, text: str) -> bool:
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
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

        # macOS / Linux direct typing
        try:
            kb.type(text)
            return True
        except Exception:
            return False

    def erase_last(self, count: Optional[int] = None) -> bool:
        """Erase last injected chunk or specific character count hands-free via backspace."""
        self.refocus_target()
        time.sleep(0.05)
        n = count if count is not None else getattr(self, "last_injected_length", 0)
        if n <= 0:
            n = 1
        n = min(n, 500)
        for _ in range(n):
            kb.tap(Key.backspace)
        self.last_injected_length = 0
        return True

    def erase_all(self) -> bool:
        """Clear the current line/field hands-free."""
        self.refocus_target()
        time.sleep(0.05)
        if IS_MACOS:
            # On macOS, Cmd+Backspace clears to beginning of line
            try:
                with kb.pressed(Key.cmd):
                    kb.tap(Key.backspace)
                self.last_injected_length = 0
                return True
            except Exception:
                pass

        with kb.pressed(Key.shift):
            kb.tap(Key.home)
        time.sleep(0.02)
        kb.tap(Key.backspace)
        self.last_injected_length = 0
        return True

    def inject_text(self, text: str) -> bool:
        if not text:
            return False

        self.refocus_target()
        time.sleep(0.05)

        self.last_injected_length = len(text)
        orig_clip = self.get_clipboard_text()
        clip_ok = self.set_clipboard_text(text)

        if not clip_ok or not getattr(self, "_last_clip_op_real_win32", False):
            # Fallback if clipboard was inaccessible: direct typing
            return self._send_unicode_string(text)

        time.sleep(0.03)
        self._send_ctrl_v()

        # 250ms post-paste delay so apps process paste before clipboard restore
        time.sleep(0.25)
        if orig_clip:
            self.set_clipboard_text(orig_clip)

        return True

    def move_mouse(self, dx: int, dy: int) -> bool:
        """Moves mouse relative to current position with hardware-level SendInput on Windows."""
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
            try:
                inp = INPUT()
                inp.type = INPUT_MOUSE
                inp.mi = MOUSEINPUT(dx=int(dx), dy=int(dy), mouseData=0, dwFlags=MOUSEEVENTF_MOVE, time=0, dwExtraInfo=0)
                sent = user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT))
                return sent > 0
            except Exception:
                pass
        try:
            mouse_ctrl.move(int(dx), int(dy))
            return True
        except Exception:
            return False

    def mouse_click(self, button: str = "left", double: bool = False) -> bool:
        """Executes a left, right, or middle mouse click (or double click)."""
        btn = button.lower()
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
            try:
                down_flag = MOUSEEVENTF_LEFTDOWN if btn == "left" else (MOUSEEVENTF_RIGHTDOWN if btn == "right" else MOUSEEVENTF_MIDDLEDOWN)
                up_flag = MOUSEEVENTF_LEFTUP if btn == "left" else (MOUSEEVENTF_RIGHTUP if btn == "right" else MOUSEEVENTF_MIDDLEUP)

                inp_down = INPUT()
                inp_down.type = INPUT_MOUSE
                inp_down.mi = MOUSEINPUT(dx=0, dy=0, mouseData=0, dwFlags=down_flag, time=0, dwExtraInfo=0)

                inp_up = INPUT()
                inp_up.type = INPUT_MOUSE
                inp_up.mi = MOUSEINPUT(dx=0, dy=0, mouseData=0, dwFlags=up_flag, time=0, dwExtraInfo=0)

                user32.SendInput(1, ctypes.byref(inp_down), ctypes.sizeof(INPUT))
                time.sleep(0.015)
                user32.SendInput(1, ctypes.byref(inp_up), ctypes.sizeof(INPUT))

                if double:
                    time.sleep(0.04)
                    user32.SendInput(1, ctypes.byref(inp_down), ctypes.sizeof(INPUT))
                    time.sleep(0.015)
                    user32.SendInput(1, ctypes.byref(inp_up), ctypes.sizeof(INPUT))
                return True
            except Exception:
                pass
        try:
            p_btn = MouseButton.left if btn == "left" else (MouseButton.right if btn == "right" else MouseButton.middle)
            mouse_ctrl.click(p_btn, 2 if double else 1)
            return True
        except Exception:
            return False

    def mouse_down(self, button: str = "left") -> bool:
        """Holds down the specified mouse button (drag initiation)."""
        btn = button.lower()
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
            try:
                down_flag = MOUSEEVENTF_LEFTDOWN if btn == "left" else (MOUSEEVENTF_RIGHTDOWN if btn == "right" else MOUSEEVENTF_MIDDLEDOWN)
                inp = INPUT()
                inp.type = INPUT_MOUSE
                inp.mi = MOUSEINPUT(dx=0, dy=0, mouseData=0, dwFlags=down_flag, time=0, dwExtraInfo=0)
                return user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT)) > 0
            except Exception:
                pass
        try:
            p_btn = MouseButton.left if btn == "left" else (MouseButton.right if btn == "right" else MouseButton.middle)
            mouse_ctrl.press(p_btn)
            return True
        except Exception:
            return False

    def mouse_up(self, button: str = "left") -> bool:
        """Releases the specified mouse button (drag completion)."""
        btn = button.lower()
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
            try:
                up_flag = MOUSEEVENTF_LEFTUP if btn == "left" else (MOUSEEVENTF_RIGHTUP if btn == "right" else MOUSEEVENTF_MIDDLEUP)
                inp = INPUT()
                inp.type = INPUT_MOUSE
                inp.mi = MOUSEINPUT(dx=0, dy=0, mouseData=0, dwFlags=up_flag, time=0, dwExtraInfo=0)
                return user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT)) > 0
            except Exception:
                pass
        try:
            p_btn = MouseButton.left if btn == "left" else (MouseButton.right if btn == "right" else MouseButton.middle)
            mouse_ctrl.release(p_btn)
            return True
        except Exception:
            return False

    def mouse_scroll(self, delta: int) -> bool:
        """Scrolls vertically (positive delta = scroll up, negative delta = scroll down)."""
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
            try:
                wheel_amount = int(delta * 120) if abs(delta) <= 10 else int(delta)
                inp = INPUT()
                inp.type = INPUT_MOUSE
                inp.mi = MOUSEINPUT(dx=0, dy=0, mouseData=wheel_amount, dwFlags=MOUSEEVENTF_WHEEL, time=0, dwExtraInfo=0)
                return user32.SendInput(1, ctypes.byref(inp), ctypes.sizeof(INPUT)) > 0
            except Exception:
                pass
        try:
            mouse_ctrl.scroll(0, delta)
            return True
        except Exception:
            return False

    def get_clipboard_seq(self) -> int:
        """Gets Windows clipboard sequence counter to detect updates without locking clipboard."""
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL) and hasattr(user32, "GetClipboardSequenceNumber"):
            try:
                return user32.GetClipboardSequenceNumber()
            except Exception:
                pass
        return 0

