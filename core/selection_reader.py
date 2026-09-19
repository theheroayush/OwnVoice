import sys
import time
import ctypes
from typing import Optional, Tuple
from pynput.keyboard import Controller, Key

IS_WINDOWS = sys.platform == "win32"
keyboard = Controller()

if IS_WINDOWS and hasattr(ctypes, "WinDLL"):
    from ctypes import wintypes
    user32 = ctypes.WinDLL("user32", use_last_error=True)
    kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)

    CF_UNICODETEXT = 13
    GMEM_MOVEABLE = 0x0002

    user32.OpenClipboard.argtypes = [wintypes.HWND]
    user32.OpenClipboard.restype = wintypes.BOOL
    user32.CloseClipboard.argtypes = []
    user32.CloseClipboard.restype = wintypes.BOOL
    user32.GetClipboardData.argtypes = [wintypes.UINT]
    user32.GetClipboardData.restype = wintypes.HANDLE
    user32.SetClipboardData.argtypes = [wintypes.UINT, wintypes.HANDLE]
    user32.SetClipboardData.restype = wintypes.HANDLE
    user32.EmptyClipboard.argtypes = []
    user32.EmptyClipboard.restype = wintypes.BOOL
    user32.IsClipboardFormatAvailable.argtypes = [wintypes.UINT]
    user32.IsClipboardFormatAvailable.restype = wintypes.BOOL

    if hasattr(user32, "GetClipboardSequenceNumber"):
        user32.GetClipboardSequenceNumber.argtypes = []
        user32.GetClipboardSequenceNumber.restype = wintypes.DWORD

    kernel32.GlobalLock.argtypes = [wintypes.HGLOBAL]
    kernel32.GlobalLock.restype = wintypes.LPVOID
    kernel32.GlobalUnlock.argtypes = [wintypes.HGLOBAL]
    kernel32.GlobalUnlock.restype = wintypes.BOOL
    kernel32.GlobalAlloc.argtypes = [wintypes.UINT, ctypes.c_size_t]
    kernel32.GlobalAlloc.restype = wintypes.HGLOBAL


class SelectionReader:
    """
    Non-destructive screen selection reader.
    Captures highlighted text across Windows applications (VS Code, Chrome, Word, Slack)
    by executing a micro-copy (Ctrl+C) and immediately restoring the original clipboard.
    """

    @staticmethod
    def _win_get_clipboard_text() -> Optional[str]:
        if not IS_WINDOWS:
            return None
        for _ in range(5):
            if user32.OpenClipboard(None):
                try:
                    if not user32.IsClipboardFormatAvailable(CF_UNICODETEXT):
                        return None
                    h_mem = user32.GetClipboardData(CF_UNICODETEXT)
                    if not h_mem:
                        return None
                    ptr = kernel32.GlobalLock(h_mem)
                    if not ptr:
                        return None
                    try:
                        return ctypes.c_wchar_p(ptr).value
                    finally:
                        kernel32.GlobalUnlock(h_mem)
                finally:
                    user32.CloseClipboard()
            time.sleep(0.01)
        return None

    @staticmethod
    def _win_set_clipboard_text(text: str) -> bool:
        if not IS_WINDOWS:
            return False
        if text is None:
            return False
        for _ in range(5):
            if user32.OpenClipboard(None):
                try:
                    user32.EmptyClipboard()
                    encoded = text.encode("utf-16-le") + b"\x00\x00"
                    h_mem = kernel32.GlobalAlloc(GMEM_MOVEABLE, len(encoded))
                    if not h_mem:
                        return False
                    ptr = kernel32.GlobalLock(h_mem)
                    if not ptr:
                        return False
                    ctypes.memmove(ptr, encoded, len(encoded))
                    kernel32.GlobalUnlock(h_mem)
                    user32.SetClipboardData(CF_UNICODETEXT, h_mem)
                    return True
                finally:
                    user32.CloseClipboard()
            time.sleep(0.01)
        return False

    @classmethod
    def get_selected_text(cls, max_wait_ms: int = 70) -> Optional[str]:
        """
        Reads currently highlighted/selected text in the active foreground window.
        Preserves whatever was already on the user's clipboard.
        """
        if not IS_WINDOWS:
            return None

        # 1. Snapshot previous clipboard text
        prev_clipboard = cls._win_get_clipboard_text()

        # 2. Get baseline clipboard sequence number
        seq_before = 0
        if hasattr(user32, "GetClipboardSequenceNumber"):
            seq_before = user32.GetClipboardSequenceNumber()

        # 3. Trigger synthetic Ctrl+C
        try:
            keyboard.press(Key.ctrl)
            keyboard.press('c')
            keyboard.release('c')
            keyboard.release(Key.ctrl)
        except Exception:
            return None

        # 4. Wait for target app to update clipboard
        selected_text = None
        start_t = time.time()
        deadline = start_t + (max_wait_ms / 1000.0)

        while time.time() < deadline:
            time.sleep(0.015)
            if hasattr(user32, "GetClipboardSequenceNumber"):
                seq_curr = user32.GetClipboardSequenceNumber()
                if seq_curr != seq_before:
                    selected_text = cls._win_get_clipboard_text()
                    break
            else:
                curr_text = cls._win_get_clipboard_text()
                if curr_text != prev_clipboard:
                    selected_text = curr_text
                    break

        # 5. Restore previous clipboard state immediately
        if prev_clipboard is not None:
            if selected_text != prev_clipboard:
                cls._win_set_clipboard_text(prev_clipboard)
        elif selected_text is not None:
            # If clipboard was empty before, empty it again
            for _ in range(3):
                if user32.OpenClipboard(None):
                    try:
                        user32.EmptyClipboard()
                        break
                    finally:
                        user32.CloseClipboard()
                time.sleep(0.01)

        # 6. Return captured selection (if non-empty)
        if selected_text and selected_text.strip():
            return selected_text.strip()

        return None
