"""Sandbox-only pystray stand-in."""
class _Dummy:
    def __init__(self, *a, **k):
        self._running = False
    def __getattr__(self, name):
        if name.startswith("__"):
            raise AttributeError(name)
        def _noop(*a, **k):
            return None
        return _noop

class Icon(_Dummy):
    def __init__(self, name=None, icon=None, title=None, menu=None, **k):
        super().__init__()
        self.name = name
        self.title = title
        self.menu = menu
        self.icon = icon
    def run(self, *a, **k):
        return None
    def run_detached(self, *a, **k):
        return None
    def stop(self):
        return None
    def update_menu(self):
        return None
    def visible_setter(self, v):
        pass

class Menu(_Dummy):
    def __init__(self, *items, **k):
        super().__init__(*items, **k)

class MenuItem(_Dummy):
    def __init__(self, text=None, action=None, **k):
        super().__init__()
        self.text = text
        self.action = action
