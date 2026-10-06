"""Sandbox-only in-memory pyperclip stand-in."""
import threading

class PyperclipException(Exception):
    pass

_lock = threading.Lock()
_value = ""

def copy(text):
    global _value
    with _lock:
        _value = "" if text is None else str(text)

def paste():
    with _lock:
        return _value

def determine_clipboard():
    return "sandbox"

def is_available():
    return True
