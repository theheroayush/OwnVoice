"""Sandbox-only minimal `pynput` stand-in (keyboard + mouse) for headless testing."""
import time

class _SpecialKey:
    def __init__(self, name):
        self._name = name
    def __repr__(self):
        return "Key.%s" % self._name
    def __eq__(self, other):
        return isinstance(other, _SpecialKey) and other._name == self._name
    def __hash__(self):
        return hash(self._name)
    def __getattr__(self, item):
        return 1


class Key:
    pass


BINDINGS = {
    "alt": ["alt", "alt_l", "alt_r", "alt_gr"],
    "alt_l": ["alt_l", "alt", "alt_r"],
    "alt_r": ["alt_r", "alt"],
    "backspace": ["backspace"],
    "caps_lock": ["caps_lock"],
    "cmd": ["cmd", "cmd_l", "cmd_r"],
    "cmd_l": ["cmd", "cmd_l"],
    "cmd_r": ["cmd", "cmd_r"],
    "ctrl": ["ctrl", "ctrl_l", "ctrl_r"],
    "ctrl_l": ["ctrl_l", "ctrl"],
    "ctrl_r": ["ctrl_r", "ctrl"],
    "delete": ["delete"],
    "down": ["down"],
    "end": ["end"],
    "enter": ["enter"],
    "esc": ["esc"],
    "f8": ["f8"],
    "f9": ["f9"],
    "home": ["home"],
    "left": ["left"],
    "page_down": ["page_down"],
    "page_up": ["page_up"],
    "right": ["right"],
    "shift": ["shift", "shift_l", "shift_r"],
    "shift_l": ["shift_l"],
    "shift_r": ["shift_r"],
    "space": ["space"],
    "tab": ["tab"],
    "f1": ["f1"],
    "f2": ["f2"],
    "f3": ["f3"],
    "f4": ["f4"],
    "f5": ["f5"],
    "f6": ["f6"],
    "f7": ["f7"],
    "f10": ["f10"],
    "f11": ["f11"],
    "f12": ["f12"],
    "f13": ["f13"],
    "f14": ["f14"],
    "f15": ["f15"],
    "f16": ["f16"],
    "f17": ["f17"],
    "f18": ["f18"],
    "f19": ["f19"],
    "f20": ["f20"],
    "insert": ["insert"],
    "pause": ["pause"],
    "print_screen": ["print_screen"],
    "scroll_lock": ["scroll_lock"],
    "num_lock": ["num_lock"],
    "menu": ["menu"],
    "up": ["up"],
}

_created = {}
for _name, _alts in BINDINGS.items():
    _key = _SpecialKey(_name)
    for _n in _alts:
        _created.setdefault(_n, _key)

for _n, _k in _created.items():
    setattr(Key, _n, _k)


class KeyCode:
    def __init__(self, char=None, vk=None):
        self.char = char
        self.vk = vk
    def __repr__(self):
        return "KeyCode(%r)" % (self.char,)
    def __eq__(self, other):
        return isinstance(other, KeyCode) and other.char == self.char and other.vk == self.vk
    def __hash__(self):
        return hash((self.char, self.vk))


class Controller:
    def __init__(self):
        self.pressed = []

    def press(self, key):
        self.pressed.append(key)

    def release(self, key):
        pass

    def tap(self, key):
        self.press(key)

    def type(self, text):
        self.pressed.extend(list(text))

    def type_message(self, text):
        self.type(text)


class Listener:
    def __init__(self, on_press=None, on_release=None, win32_event_filter=None, **kwargs):
        self.on_press = on_press
        self.on_release = on_release
        self.win32_event_filter = win32_event_filter
        self.daemon = True
        self._running = False
        self._thread = None

    def start(self):
        self._running = True

    def stop(self):
        self._running = False

    def join(self, *args):
        pass

    def run(self):
        while self._running:
            time.sleep(0.05)


class _MouseButton:
    left = "left"
    right = "right"
    middle = "middle"


class _MouseController:
    def __init__(self):
        self.position = (0, 0)
        self.clicked = []

    def position_setter(self, pos):
        self.position = pos

    def click(self, button, count=1):
        self.clicked.append((button, count))

    def press(self, button):
        self.clicked.append((button, 1))

    def release(self, button):
        pass

    def scroll(self, dx, dy):
        pass


keyboard = type("keyboard", (), {})()
keyboard.Key = Key
keyboard.KeyCode = KeyCode
keyboard.Controller = Controller
keyboard.Listener = Listener
keyboard.press = lambda k: None
keyboard.release = lambda k: None

mouse = type("mouse", (), {})()
mouse.Controller = _MouseController
mouse.Button = _MouseButton
mouse.Listener = Listener
