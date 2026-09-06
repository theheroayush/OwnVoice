import os
import sys
import subprocess
import ctypes
from typing import Tuple

IS_WINDOWS = sys.platform == "win32"
IS_MACOS = sys.platform == "darwin"
IS_LINUX = sys.platform.startswith("linux")

class _DummyWinDLL:
    def __getattr__(self, name):
        def _dummy_func(*args, **kwargs):
            return 0
        return _dummy_func

if IS_WINDOWS and hasattr(ctypes, "WinDLL"):
    from ctypes import wintypes
    user32 = ctypes.WinDLL("user32")
    kernel32 = ctypes.WinDLL("kernel32")

    PROCESS_QUERY_LIMITED_INFORMATION = 0x1000

    # Explicit Win32 signatures for 64-bit safety
    user32.GetWindowTextW.argtypes = [wintypes.HWND, wintypes.LPWSTR, ctypes.c_int]
    user32.GetWindowTextW.restype = ctypes.c_int
    user32.GetWindowTextLengthW.argtypes = [wintypes.HWND]
    user32.GetWindowTextLengthW.restype = ctypes.c_int
    user32.GetWindowThreadProcessId.argtypes = [wintypes.HWND, ctypes.POINTER(wintypes.DWORD)]
    user32.GetWindowThreadProcessId.restype = wintypes.DWORD
    user32.IsWindow.argtypes = [wintypes.HWND]
    user32.IsWindow.restype = wintypes.BOOL

    kernel32.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
    kernel32.OpenProcess.restype = wintypes.HANDLE
    kernel32.CloseHandle.argtypes = [wintypes.HANDLE]
    kernel32.CloseHandle.restype = wintypes.BOOL
    kernel32.QueryFullProcessImageNameW.argtypes = [wintypes.HANDLE, wintypes.DWORD, wintypes.LPWSTR, ctypes.POINTER(wintypes.DWORD)]
    kernel32.QueryFullProcessImageNameW.restype = wintypes.BOOL
else:
    user32 = _DummyWinDLL()
    kernel32 = _DummyWinDLL()


def _mac_get_active_app() -> Tuple[str, str]:
    """Queries active application and window title on macOS via AppleScript."""
    try:
        cmd = [
            "osascript", "-e",
            'tell application "System Events" to get {name of first application process whose frontmost is true, name of window 1 of (first application process whose frontmost is true)}'
        ]
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=0.3)
        if res.returncode == 0 and res.stdout.strip():
            parts = res.stdout.strip().split(", ")
            app_name = parts[0].strip().lower() if len(parts) > 0 else ""
            win_title = parts[1].strip() if len(parts) > 1 else ""
            return app_name, win_title
    except Exception:
        pass
    return "", ""


def _linux_get_active_app() -> Tuple[str, str]:
    """Queries active application on Linux via xdotool."""
    try:
        res = subprocess.run(["xdotool", "getactivewindow", "getwindowclassname"], capture_output=True, text=True, timeout=0.2)
        if res.returncode == 0:
            return res.stdout.strip().lower(), ""
    except Exception:
        pass
    return "", ""


class ContextDetector:
    """
    Cross-platform application context detector.
    - Windows: 100% Native Win32 (<0.2ms)
    - macOS: AppleScript frontmost process query
    - Linux: xdotool / proc active window query
    """

    CODING_PROCESSES = {
        # Windows
        "code.exe", "code - insiders.exe", "cursor.exe", "devenv.exe",
        "windowsterminal.exe", "wt.exe", "warp.exe", "powershell.exe",
        "pwsh.exe", "cmd.exe", "pycharm64.exe", "pycharm.exe",
        "idea64.exe", "idea.exe", "clion64.exe", "clion.exe",
        "webstorm64.exe", "webstorm.exe", "rider64.exe", "rider.exe",
        "goland64.exe", "goland.exe", "datagrip64.exe", "datagrip.exe",
        "rubymine64.exe", "sublime_text.exe", "alacritty.exe",
        "wezterm-gui.exe", "git-bash.exe", "mintty.exe", "zed.exe",
        "nvim-qt.exe", "gvim.exe", "terminal.exe",
        # macOS / Linux process names
        "code", "cursor", "xcode", "terminal", "iterm2", "alacritty",
        "kitty", "wezterm", "sublime_text", "pycharm", "idea", "zed",
        "gnome-terminal", "konsole", "terminator", "warp"
    }

    DOCUMENT_PROCESSES = {
        # Windows
        "winword.exe", "wordpad.exe", "notepad.exe", "notepad++.exe",
        "obsidian.exe", "notion.exe", "excel.exe", "powerpnt.exe",
        "acrobat.exe", "foxitreader.exe", "typora.exe",
        # macOS / Linux
        "pages", "numbers", "keynote", "textedit", "obsidian", "notion",
        "libreoffice", "soffice.bin", "gedit", "kate"
    }

    EMAIL_PROCESSES = {
        # Windows
        "outlook.exe", "olk.exe", "thunderbird.exe", "mail.exe",
        # macOS / Linux
        "mail", "thunderbird", "outlook"
    }

    CHAT_PROCESSES = {
        # Windows
        "slack.exe", "teams.exe", "ms-teams.exe", "msteams.exe",
        "whatsapp.exe", "discord.exe", "telegram.exe", "signal.exe",
        "skype.exe", "element.exe",
        # macOS / Linux
        "slack", "discord", "whatsapp", "telegram", "teams", "messages", "signal"
    }

    BROWSER_PROCESSES = {
        # Windows
        "chrome.exe", "msedge.exe", "brave.exe", "firefox.exe",
        "opera.exe", "vivaldi.exe", "arc.exe",
        # macOS / Linux
        "google chrome", "safari", "brave browser", "firefox", "arc", "opera", "vivaldi"
    }

    SEARCH_KEYWORDS = (
        "google search", " - google", "youtube", "amazon",
        "flipkart", "bing search", "duckduckgo", " - search"
    )

    @staticmethod
    def get_window_info(hwnd: int) -> Tuple[str, str]:
        """Returns (process_name, window_title) for a given window."""
        if IS_WINDOWS and not isinstance(user32, _DummyWinDLL):
            if not hwnd or not user32.IsWindow(hwnd):
                return "", ""

            from ctypes import wintypes

            # Window Title
            length = user32.GetWindowTextLengthW(hwnd)
            buff = ctypes.create_unicode_buffer(length + 1)
            user32.GetWindowTextW(hwnd, buff, length + 1)
            title = buff.value.strip()

            # Process Executable
            pid = wintypes.DWORD()
            user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
            
            exe_name = ""
            h_proc = kernel32.OpenProcess(0x1000, False, pid.value)
            if h_proc:
                buf = ctypes.create_unicode_buffer(1024)
                size = wintypes.DWORD(1024)
                if kernel32.QueryFullProcessImageNameW(h_proc, 0, buf, ctypes.byref(size)):
                    exe_name = buf.value.split("\\")[-1].lower()
                kernel32.CloseHandle(h_proc)

            return exe_name, title

        if IS_MACOS:
            return _mac_get_active_app()

        if IS_LINUX:
            return _linux_get_active_app()

        return "", ""

    @classmethod
    def detect_tone(cls, hwnd: int) -> Tuple[str, str]:
        """
        Detects app category and returns (mode_key, display_label):
        - ('code', 'Code')
        - ('formal_email', 'Email')
        - ('formal_document', 'Doc')
        - ('chat', 'Chat')
        - ('search', 'Search')
        - ('smart_flow', 'Flow')
        """
        exe, title = cls.get_window_info(hwnd)
        title_lower = title.lower()

        # 1. Coding environment detection
        if (exe in cls.CODING_PROCESSES or
            "visual studio" in title_lower or
            "vscode" in title_lower or
            "sublime text" in title_lower or
            "windows terminal" in title_lower or
            title_lower.endswith("powershell") or
            title_lower.endswith("cmd.exe")):
            return "code", "Code"

        # 2. Email client detection
        if (exe in cls.EMAIL_PROCESSES or
            "gmail" in title_lower or
            "outlook" in title_lower or
            "thunderbird" in title_lower or
            title_lower.endswith(" - mail")):
            return "formal_email", "Email"

        # 3. Chat / Messaging detection
        if (exe in cls.CHAT_PROCESSES or
            "slack" in title_lower or
            "discord" in title_lower or
            "whatsapp" in title_lower or
            "telegram" in title_lower or
            "teams" in title_lower or
            "messenger" in title_lower):
            return "chat", "Chat"

        # 4. Search engine & query detection
        if (exe in cls.BROWSER_PROCESSES and any(kw in title_lower for kw in cls.SEARCH_KEYWORDS)) or (
            "google search" in title_lower or "bing search" in title_lower or "duckduckgo" in title_lower
        ):
            return "search", "Search"

        # 5. Document / Editing tools (including UWP Notepad & Word)
        if (exe in cls.DOCUMENT_PROCESSES or
            "google docs" in title_lower or
            " - word" in title_lower or
            title_lower.endswith("word") or
            " - notepad" in title_lower or
            title_lower == "notepad" or
            "excel" in title_lower or
            "powerpoint" in title_lower or
            "obsidian" in title_lower or
            "notion" in title_lower):
            return "formal_document", "Doc"

        return "smart_flow", "Flow"
