import threading
from pynput import keyboard

class HotkeyManager:
    def __init__(self, on_start, on_stop, config_manager):
        self.on_start = on_start
        self.on_stop = on_stop
        self.config = config_manager
        self.is_active = False
        self.current_keys = set()
        self.listener = None
        self.lock = threading.Lock()

    def _is_hotkey_triggered(self, key=None) -> bool:
        hotkey_str = self.config.get("hotkey", "f8").lower().replace(" ", "")
        
        if hotkey_str in ("f8", "<f8>"):
            return key == keyboard.Key.f8 or keyboard.Key.f8 in self.current_keys
        if hotkey_str in ("f9", "<f9>"):
            return key == keyboard.Key.f9 or keyboard.Key.f9 in self.current_keys
        if hotkey_str in ("caps_lock", "<caps_lock>", "capslock"):
            return key == keyboard.Key.caps_lock or keyboard.Key.caps_lock in self.current_keys

        parts = hotkey_str.split("+")
        for part in parts:
            matched = False
            if part in ("<ctrl>", "ctrl", "control"):
                matched = any(k in self.current_keys for k in (keyboard.Key.ctrl, keyboard.Key.ctrl_l, keyboard.Key.ctrl_r))
            elif part in ("<shift>", "shift"):
                matched = any(k in self.current_keys for k in (keyboard.Key.shift, keyboard.Key.shift_l, keyboard.Key.shift_r))
            elif part in ("<alt>", "alt"):
                matched = any(k in self.current_keys for k in (keyboard.Key.alt, keyboard.Key.alt_l, keyboard.Key.alt_r))
            elif part in ("<space>", "space"):
                matched = keyboard.Key.space in self.current_keys
            elif len(part) == 1:
                matched = any(getattr(k, 'char', None) == part for k in self.current_keys)
            
            if not matched:
                return False
        return True

    def _on_press(self, key):
        with self.lock:
            self.current_keys.add(key)
            mode = self.config.get("hotkey_mode", "toggle")

            if self._is_hotkey_triggered(key):
                if mode == "toggle":
                    if not self.is_active:
                        self.is_active = True
                        threading.Thread(target=self.on_start, daemon=True).start()
                    else:
                        self.is_active = False
                        threading.Thread(target=self.on_stop, daemon=True).start()
                elif mode == "push_to_talk":
                    if not self.is_active:
                        self.is_active = True
                        threading.Thread(target=self.on_start, daemon=True).start()

    def _on_release(self, key):
        with self.lock:
            if key in self.current_keys:
                self.current_keys.remove(key)

            mode = self.config.get("hotkey_mode", "toggle")
            if mode == "push_to_talk" and self.is_active:
                if not self._is_hotkey_triggered():
                    self.is_active = False
                    threading.Thread(target=self.on_stop, daemon=True).start()

    def start(self):
        if self.listener is None:
            self.listener = keyboard.Listener(on_press=self._on_press, on_release=self._on_release)
            self.listener.daemon = True
            self.listener.start()

    def stop(self):
        if self.listener:
            self.listener.stop()
            self.listener = None
