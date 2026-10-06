"""Sandbox-only tkinter submodule stand-in."""
def __getattr__(name):
    def _noop(*args, **kwargs):
        return None
    return _noop
