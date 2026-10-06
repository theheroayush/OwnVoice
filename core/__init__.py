"""OwnVoice core package.

Submodules are resolved lazily (PEP 562) instead of being imported eagerly.

Importing `core.audio_recorder` or `core.ai_engine` pulls in numpy and requests,
so the previous eager `from .audio_recorder import AudioRecorder` here meant that
touching *any* `core.*` submodule — even the pure-ctypes `core.context_detector` —
loaded ~370 modules and tens of megabytes. That directly conflicts with the
idle-memory budget (feature F15) and slows startup.

`from core import AudioRecorder` and `import core.audio_recorder` keep working
exactly as before; the names are resolved on first access and then cached.
"""

from importlib import import_module

_EXPORTS = {
    "sound_effects": ".sound_effects",
    "AudioRecorder": ".audio_recorder",
    "AIEngine": ".ai_engine",
    "CursorInjector": ".injector",
    "HotkeyManager": ".hotkey_manager",
}

__all__ = list(_EXPORTS)


def __getattr__(name):
    """Import the backing submodule only when the attribute is actually used."""
    module_path = _EXPORTS.get(name)
    if module_path is None:
        raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
    module = import_module(module_path, __name__)
    value = getattr(module, name)
    globals()[name] = value  # cache for subsequent lookups
    return value


def __dir__():
    return sorted(set(globals()) | set(_EXPORTS))
