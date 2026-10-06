"""OwnVoice UI package.

Like `core`, the widgets are resolved lazily (PEP 562) so that importing a single
UI module (or a test that only needs `ui.design_tokens`) does not create Tk
widgets, tray icons and the whole customtkinter stack up front.

`from ui import FloatingWidget` and `import ui.floating_widget` behave as before.
"""

from importlib import import_module

_EXPORTS = {
    "FloatingWidget": ".floating_widget",
    "SettingsWindow": ".settings_window",
    "TrayIcon": ".tray_icon",
}

__all__ = list(_EXPORTS)


def __getattr__(name):
    module_path = _EXPORTS.get(name)
    if module_path is None:
        raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
    module = import_module(module_path, __name__)
    value = getattr(module, name)
    globals()[name] = value
    return value


def __dir__():
    return sorted(set(globals()) | set(_EXPORTS))
