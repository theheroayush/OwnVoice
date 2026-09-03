import os
import sys
import time
import ctypes
from ctypes import wintypes
import threading
from typing import Callable, Optional
from pynput import keyboard

# Windows Virtual Key codes
VK_F8 = 0x77
VK_F9 = 0x78
VK_CAPITAL = 0x14

WM_KEYDOWN = 0x0100
WM_KEYUP = 0x0101
WM_SYSKEYDOWN = 0x0104
WM_SYSKEYUP = 0x0105

class HotkeyManager:
    def __init__(
        self,
        on_start: Callable[[], None],
        on_stop: Callable[[], None],
        is_recording_fn: Optional[Callable[[], bool]] = None,
        config_manager=None
    ):
        # Support both (on_start, on_stop, is_recording_fn, config) and (on_start, on_stop, config)
        if config_manager is None and not callable(is_recording_fn) and is_recording_fn is not None:
            config_manager = is_recording_fn
            is_recording_fn = None

        self.on_start = on_start
        self.on_stop = on_stop
        self.is_recording_fn = is_recording_fn
        self.config = config_manager
        self.is_active = False
        self.current_keys = set()
        self.listener = None
        self.lock = threading.Lock()
        self.last_toggle_time = 0.0

    def sync_state(self, is_recording: bool):
        """Synchronizes internal hotkey state with authoritative recorder / UI state."""
        with self.lock:
            self.is_active = is_recording

    def _trigger_toggle(self):
        now = time.monotonic()
        if now - self.last_toggle_time < 0.35:
            return  # 350ms minimum debounce
        self.last_toggle_time = now

        if not self.is_active:
            self.is_active = True
            threading.Thread(target=self.on_start, daemon=True).start()
        else:
            self.is_active = False
            threading.Thread(target=self.on_stop, daemon=True).start()

    def _trigger_start(self):
        now = time.monotonic()
        if now - self.last_toggle_time < 0.35:
            return
        self.last_toggle_time = now

        if not self.is_active:
            self.is_active = True
            threading.Thread(target=self.on_start, daemon=True).start()

    def _trigger_stop(self):
        if self.is_active:
            self.is_active = False
            threading.Thread(target=self.on_stop, daemon=True).start()

    def _get_target_vk(self) -> Optional[int]:
        hotkey_str = (self.config.get("hotkey", "f8") if self.config else "f8").lower().replace(" ", "")
        if hotkey_str in ("f8", "<f8>"):
            return VK_F8
        if hotkey_str in ("f9", "<f9>"):
            return VK_F9
        if hotkey_str in ("caps_lock", "<caps_lock>", "capslock"):
            return VK_CAPITAL
        return None

    def _win32_event_filter(self, msg, data):
        """
        Suppresses single-key hotkeys (F8/F9/CapsLock) from passing to foreground applications
        to prevent alert dings and Word extend-selection alerts, while dropping OS auto-repeat events.
        """
        try:
            target_vk = self._get_target_vk()
            if target_vk is not None and getattr(data, "vkCode", None) == target_vk:
                key_obj = (
                    keyboard.Key.f8 if target_vk == VK_F8 else
                    (keyboard.Key.f9 if target_vk == VK_F9 else keyboard.Key.caps_lock)
                )
                mode = self.config.get("hotkey_mode", "toggle") if self.config else "toggle"

                if msg in (WM_KEYDOWN, WM_SYSKEYDOWN):
                    with self.lock:
                        now = time.monotonic()
                        # Drop OS auto-repeat events if key is already held within debounce window
                        if key_obj in self.current_keys and (now - self.last_toggle_time < 0.35):
                            if self.listener:
                                try:
                                    self.listener.suppress_event()
                                except Exception as e:
                                    if "Suppress" in type(e).__name__:
                                        raise
                            return False

                        self.current_keys.add(key_obj)
                        if mode == "toggle":
                            self._trigger_toggle()
                        elif mode == "push_to_talk":
                            self._trigger_start()

                elif msg in (WM_KEYUP, WM_SYSKEYUP):
                    with self.lock:
                        self.current_keys.discard(key_obj)
                        if mode == "push_to_talk":
                            self._trigger_stop()

                # Suppress keystroke in Windows low-level hook
                if self.listener:
                    try:
                        self.listener.suppress_event()
                    except Exception as e:
                        if "Suppress" in type(e).__name__:
                            raise
                return False

            return True
        except Exception as e:
            if "Suppress" in type(e).__name__:
                raise
            return True

    def _is_hotkey_triggered(self, key=None) -> bool:
        hotkey_str = (self.config.get("hotkey", "f8") if self.config else "f8").lower().replace(" ", "")

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
            # Drop OS auto-repeat events
            if key in self.current_keys:
                return

            self.current_keys.add(key)
            mode = self.config.get("hotkey_mode", "toggle") if self.config else "toggle"

            if self._is_hotkey_triggered(key):
                if mode == "toggle":
                    self._trigger_toggle()
                elif mode == "push_to_talk":
                    self._trigger_start()

    def _on_release(self, key):
        with self.lock:
            self.current_keys.discard(key)
            mode = self.config.get("hotkey_mode", "toggle") if self.config else "toggle"
            if mode == "push_to_talk":
                if not self._is_hotkey_triggered():
                    self._trigger_stop()

    def start(self):
        if self.listener is None:
            self.listener = keyboard.Listener(
                on_press=self._on_press,
                on_release=self._on_release,
                win32_event_filter=self._win32_event_filter
            )
            self.listener.daemon = True
            self.listener.start()

    def stop(self):
        if self.listener:
            try:
                self.listener.stop()
            except Exception:
                pass
            self.listener = None
