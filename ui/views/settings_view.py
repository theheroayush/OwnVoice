"""
OwnVoice Desktop Hub — Settings View
Configures verified microphone devices, live VU meter, global hotkeys, sound effects, and Gemini API credentials.
"""
import threading
import webbrowser
import customtkinter as ctk
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_PRIMARY_HOVER, ACCENT_EMERALD, ACCENT_SKY, ACCENT_CRIMSON,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED
)

class SettingsView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager, ai_engine=None, audio_recorder=None, on_settings_changed=None):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager
        self.ai_engine = ai_engine
        self.audio_recorder = audio_recorder
        self.on_settings_changed = on_settings_changed

        self.dev_map = {}
        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(0, weight=1)

        self.scroll = ctk.CTkScrollableFrame(self, fg_color=BG_APP, corner_radius=0)
        self.scroll.grid(row=0, column=0, sticky="nsew", padx=24, pady=16)
        self.scroll.grid_columnconfigure(0, weight=1)

        self._build_header()
        self._build_audio_card()
        self._build_shortcut_card()
        self._build_api_card()

    def _build_header(self):
        ctk.CTkLabel(self.scroll, text="Settings & Hardware", font=ctk.CTkFont(family="Segoe UI", size=22, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(self.scroll, text="Configure your audio devices, global shortcuts, and Gemini AI credentials.", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(anchor="w", pady=(2, 16))

    def _build_audio_card(self):
        card = ctk.CTkFrame(self.scroll, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        card.pack(fill="x", pady=(0, 16))

        ctk.CTkLabel(card, text="Microphone Input Device", font=ctk.CTkFont(size=13, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=16, pady=(14, 4))
        ctk.CTkLabel(card, text="Safe Verified Audio Inputs filtered for zero latency and low background noise.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 8))

        dev_list = []
        self.dev_map = {}
        if self.audio_recorder:
            for idx, name in self.audio_recorder.get_input_devices():
                dev_list.append(name)
                self.dev_map[name] = idx

        saved_name = self.config.get("input_device_name", None)
        current_dev_idx = self.config.get("input_device_index", None)
        current_dev_name = dev_list[0] if dev_list else "Default Microphone"
        if saved_name and saved_name in self.dev_map:
            current_dev_name = saved_name
        elif current_dev_idx is not None:
            for name, idx in self.dev_map.items():
                if idx == current_dev_idx:
                    current_dev_name = name
                    break

        self.dev_var = ctk.StringVar(value=current_dev_name)
        dev_menu = ctk.CTkOptionMenu(
            card,
            values=dev_list or ["Default Microphone"],
            variable=self.dev_var,
            command=self._on_device_changed,
            height=34,
            fg_color="#0D121D",
            button_color="#1E293B",
            button_hover_color=BG_CARD_HOVER,
            dropdown_fg_color=BG_CARD
        )
        dev_menu.pack(fill="x", padx=16, pady=(0, 14))

        # Live VU Meter
        vu_box = ctk.CTkFrame(card, fg_color="#0D121D", corner_radius=8, border_color=BORDER_CARD, border_width=1)
        vu_box.pack(fill="x", padx=16, pady=(0, 16))

        ctk.CTkLabel(vu_box, text="Live Microphone Input Level (Speak to test):", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_SECONDARY).pack(anchor="w", padx=12, pady=(8, 4))
        self.vu_progress = ctk.CTkProgressBar(vu_box, height=10, progress_color=ACCENT_PRIMARY)
        self.vu_progress.set(0.0)
        self.vu_progress.pack(fill="x", padx=12, pady=(0, 10))

    def _on_device_changed(self, choice):
        idx = self.dev_map.get(choice, None)
        if idx is not None:
            self.config.set("input_device_index", idx)
            self.config.set("input_device_name", choice)
            if self.audio_recorder and not getattr(self.audio_recorder, "is_recording", False):
                self.audio_recorder.stop_monitoring()
                self.audio_recorder.start_monitoring(device_index=idx)

    def _build_shortcut_card(self):
        card = ctk.CTkFrame(self.scroll, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        card.pack(fill="x", pady=(0, 16))

        # Hotkey selector
        ctk.CTkLabel(card, text="Global Dictation Shortcut", font=ctk.CTkFont(size=13, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=16, pady=(14, 4))
        ctk.CTkLabel(card, text="Press and hold or tap to toggle dictation anywhere across Windows.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 8))

        self.hk_var = ctk.StringVar(value=self.config.get("hotkey", "f8"))
        hk_menu = ctk.CTkOptionMenu(
            card,
            values=["f8", "f9", "<ctrl>+<shift>+<space>", "<caps_lock>", "<alt>+<space>"],
            variable=self.hk_var,
            command=self._on_hotkey_changed,
            height=34,
            width=260,
            fg_color="#0D121D",
            button_color="#1E293B",
            button_hover_color=BG_CARD_HOVER,
            dropdown_fg_color=BG_CARD
        )
        hk_menu.pack(anchor="w", padx=16, pady=(0, 14))

        # Sound effects toggle
        self.sound_fx_var = ctk.BooleanVar(value=self.config.get("sound_effects", True))
        sound_cb = ctk.CTkCheckBox(
            card,
            text="Acoustic Sound Effects (Audio cues on start, stop, and transcription completion)",
            variable=self.sound_fx_var,
            command=self._on_sound_fx_changed,
            font=ctk.CTkFont(size=12, weight="bold"),
            text_color=TEXT_PRIMARY
        )
        sound_cb.pack(anchor="w", padx=16, pady=(0, 14))

    def _on_hotkey_changed(self, choice):
        self.config.set("hotkey", choice)
        if self.on_settings_changed:
            self.on_settings_changed()

    def _on_sound_fx_changed(self):
        val = self.sound_fx_var.get()
        self.config.set("sound_effects", val)
        try:
            from core.sound_effects import sound_effects
            sound_effects.enabled = val
        except Exception:
            pass

    def _build_api_card(self):
        card = ctk.CTkFrame(self.scroll, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        card.pack(fill="x", pady=(0, 16))

        ctk.CTkLabel(card, text="Google Gemini AI Engine Credentials", font=ctk.CTkFont(size=13, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=16, pady=(14, 4))
        ctk.CTkLabel(card, text="OwnVoice uses Google AI Studio Gemini Flash for sub-second, multi-modal voice processing.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 8))

        input_row = ctk.CTkFrame(card, fg_color="transparent")
        input_row.pack(fill="x", padx=16, pady=(0, 8))

        self.api_entry = ctk.CTkEntry(
            input_row,
            placeholder_text="Enter your Gemini API key (AIza...)",
            show="*",
            height=36,
            fg_color="#0D121D",
            border_color=BORDER_CARD,
            text_color=TEXT_PRIMARY
        )
        self.api_entry.insert(0, self.config.get("google_api_key", ""))
        self.api_entry.pack(side="left", fill="x", expand=True, padx=(0, 10))

        self.test_key_btn = ctk.CTkButton(
            input_row,
            text="🔑 Test Key",
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="#0284C7",
            hover_color="#0369A1",
            width=100,
            height=36,
            command=self._test_key
        )
        self.test_key_btn.pack(side="right")

        self.key_status_label = ctk.CTkLabel(card, text="", font=ctk.CTkFont(size=12, weight="bold"))
        self.key_status_label.pack(anchor="w", padx=16, pady=(0, 10))

        btn_row = ctk.CTkFrame(card, fg_color="transparent")
        btn_row.pack(anchor="w", padx=16, pady=(0, 16))

        ctk.CTkButton(
            btn_row,
            text="💾 Save Key",
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color=ACCENT_PRIMARY,
            hover_color=ACCENT_PRIMARY_HOVER,
            width=110,
            height=32,
            command=self._save_api_key
        ).pack(side="left", padx=(0, 10))

        ctk.CTkButton(
            btn_row,
            text="⚡ Test Speed",
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="#059669",
            hover_color="#047857",
            width=110,
            height=32,
            command=self._test_speed
        ).pack(side="left", padx=(0, 10))

        ctk.CTkButton(
            btn_row,
            text="Get Free Key ↗",
            font=ctk.CTkFont(size=12),
            fg_color="#1E293B",
            hover_color=BG_CARD_HOVER,
            width=120,
            height=32,
            command=lambda: webbrowser.open("https://aistudio.google.com/apikey")
        ).pack(side="left")

    def _save_api_key(self):
        k = self.api_entry.get().strip() if self.api_entry else ""
        self.config.set("google_api_key", k)
        self.key_status_label.configure(text="Key saved successfully! ✓", text_color=ACCENT_EMERALD)

    def _set_key_status_ui(self, text: str, is_valid: bool = None):
        if is_valid is True:
            color = "#10B981"  # clear green
        elif is_valid is False:
            color = "#EF4444"  # clear red
        else:
            color = "#38BDF8"  # blue/testing

        if self.key_status_label:
            try:
                self.key_status_label.configure(text=text, text_color=color)
            except Exception:
                pass

    def _test_key(self):
        k = self.api_entry.get().strip() if self.api_entry else ""
        if not k:
            self._set_key_status_ui("Invalid Key", False)
            return

        self._set_key_status_ui("Testing Key...", None)
        self._key_test_result = None

        def ping():
            is_valid = False
            msg = "Invalid Key"
            try:
                if self.ai_engine and hasattr(self.ai_engine, "test_key"):
                    is_valid, msg = self.ai_engine.test_key(k)
                elif self.ai_engine and hasattr(self.ai_engine, "test_connection"):
                    ok, conn_msg, _ = self.ai_engine.test_connection(k)
                    is_valid = ok
                    msg = "Valid Key" if ok else "Invalid Key"
                else:
                    import requests
                    r = requests.get(f"https://generativelanguage.googleapis.com/v1beta/models?key={k}", timeout=5)
                    is_valid = (r.status_code == 200)
                    msg = "Valid Key" if is_valid else "Invalid Key"
            except Exception:
                is_valid = False
                msg = "Invalid Key"
            self._key_test_result = (is_valid, msg)

        def poll_result():
            if getattr(self, "_key_test_result", None) is not None:
                valid, msg = self._key_test_result
                self._set_key_status_ui(msg, valid)
            else:
                try:
                    self.after(20, poll_result)
                except Exception:
                    pass

        threading.Thread(target=ping, daemon=True).start()
        try:
            self.after(20, poll_result)
        except Exception:
            pass

    def _test_speed(self):
        k = self.api_entry.get().strip() if self.api_entry else ""
        self._set_key_status_ui("Testing...", None)
        self._conn_test_result = None

        def run():
            try:
                if self.ai_engine and hasattr(self.ai_engine, "test_connection"):
                    ok, msg, lat = self.ai_engine.test_connection(k)
                    status = f"{msg} (Latency: {lat:.0f}ms)" if ok else msg
                    self._conn_test_result = (status, ok)
                else:
                    import time, requests
                    t0 = time.time()
                    r = requests.get(f"https://generativelanguage.googleapis.com/v1beta/models?key={k}", timeout=5)
                    lat = (time.time() - t0) * 1000
                    ok = (r.status_code == 200)
                    status = f"Response OK ({lat:.0f}ms)" if ok else f"HTTP {r.status_code}"
                    self._conn_test_result = (status, ok)
            except Exception as e:
                self._conn_test_result = (f"Connection failed: {str(e)[:25]}", False)

        def poll_speed():
            if getattr(self, "_conn_test_result", None) is not None:
                status, ok = self._conn_test_result
                self._set_key_status_ui(status, ok)
            else:
                try:
                    self.after(20, poll_speed)
                except Exception:
                    pass

        threading.Thread(target=run, daemon=True).start()
        try:
            self.after(20, poll_speed)
        except Exception:
            pass
