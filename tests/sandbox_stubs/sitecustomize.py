"""Sandbox-only: provide a fake `ctypes.windll` so Windows-targeted tests can execute."""
import ctypes


class _WinFunc:
    def __init__(self, name):
        self._name = name

    def __call__(self, *args, **kwargs):
        return 1

    def __getattr__(self, item):
        return _WinFunc(item)


class _WinDLL:
    def __getattr__(self, item):
        return _WinFunc(item)


if not hasattr(ctypes, "windll"):
    ctypes.windll = _WinDLL()
    ctypes.WinDLL = _WinDLL
