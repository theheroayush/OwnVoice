"""Sandbox-only tkinter.font stand-in."""
class Font:
    def __init__(self, *a, **k):
        pass
    def measure(self, text):
        return len(str(text)) * 7
    def metrics(self, *a, **k):
        return 12
    def actual(self, *a, **k):
        return {"family": "Segoe UI", "size": 12}
    def configure(self, *a, **k):
        pass
    def cget(self, *a, **k):
        return None
def nametofont(*a, **k):
    return Font()
def families(*a, **k):
    return ["Segoe UI", "Arial"]
