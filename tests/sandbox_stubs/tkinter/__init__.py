"""Sandbox-only generic `tkinter` stand-in.

Any attribute access returns a permissive dummy class/object so that
UI modules can be imported and instantiated headlessly.
"""
import abc


class _Dummy:
    def __init__(self, *args, **kwargs):
        self._args = args
        self._kwargs = kwargs
        self._items = []

    def __getattr__(self, name):
        if name.startswith("__") and name.endswith("__"):
            raise AttributeError(name)

        def _noop(*args, **kwargs):
            if name.startswith("create_"):
                self._items.append(args)
                return len(self._items)
            if name in ("winfo_x", "winfo_y", "winfo_width", "winfo_height",
                        "winfo_rootx", "winfo_rooty", "winfo_screenwidth",
                        "winfo_screenheight", "winfo_reqwidth", "winfo_reqheight"):
                return 100
            if name.startswith("winfo_") or name.startswith("cget"):
                return 1
            if name in ("bbox", "coords"):
                return (0, 0, 10, 10)
            return None

        return _noop

    def __setitem__(self, k, v):
        pass

    def __getitem__(self, k):
        return None


class _DummyVar:
    def __init__(self, *args, **kwargs):
        self._value = args[0] if args else kwargs.get("value", "")
        self._callback = None

    def get(self):
        return self._value

    def set(self, value):
        self._value = value

    def trace_add(self, *args, **kwargs):
        return "trace"

    def trace(self, *args, **kwargs):
        return "trace"


BooleanVar = _DummyVar
StringVar = _DummyVar
IntVar = _DummyVar
DoubleVar = _DummyVar


class Tk(_Dummy, metaclass=abc.ABCMeta):
    pass


class _TclError(Exception):
    pass


class Toplevel(_Dummy, metaclass=abc.ABCMeta):
    pass


def _factory(name):
    if name in ("BooleanVar", "StringVar", "IntVar", "DoubleVar"):
        return _DummyVar
    return abc.ABCMeta(name, (_Dummy,), {})


def __getattr__(name):
    return _factory(name)


# ---- extra standard names so `from tkinter import *` works in third-party libs ----
import importlib as _importlib
_constants = _importlib.import_module("tkinter.constants")

for _n in dir(_constants):
    if not _n.startswith("_"):
        globals().setdefault(_n, getattr(_constants, _n))

for _n in ["Variable", "StringVar", "IntVar", "DoubleVar", "BooleanVar"]:
    globals()[_n] = _DummyVar

for _n in [
    "Canvas", "Frame", "Label", "Button", "Entry", "Text", "Menu", "Scrollbar",
    "Listbox", "Checkbutton", "Radiobutton", "Scale", "Spinbox", "PhotoImage",
    "Misc", "Widget", "Place", "Pack", "Grid", "PanedWindow", "LabelFrame",
    "Canvas", "OptionMenu", "Message",
]:
    globals().setdefault(_n, _factory(_n))

globals()["Tcl"] = lambda *a, **k: None
globals()["getboolean"] = lambda v: bool(v)
globals()["image_names"] = lambda: []
