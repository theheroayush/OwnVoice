import threading
import webbrowser
import customtkinter as ctk
import sounddevice as sd

ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

class SettingsWindow:
    def __init__(self, config_manager, ai_engine, audio_recorder=None, on_settings_changed=None):
        self.config = config_manager
        self.ai_engine = ai_engine
        self.audio_recorder = audio_recorder
        self.on_settings_changed = on_settings_changed
        self.window = None
        self.is_open = False
        self.test_stream = None
        self.test_running = False

    def show(self):
        if self.is_open and self.window:
            self.window.lift()
            self.window.focus_force()
            return

        self.is_open = True
        self.window = ctk.CTkToplevel()
        self.window.title("OwnVoice — Settings & Audio Devices")
        self.window.geometry("740x560")
        self.window.minsize(680, 500)
        self.window.protocol("WM_DELETE_WINDOW", self._on_close)

        self.window.grid_columnconfigure(0, weight=1)
        self.window.grid_rowconfigure(0, weight=1)

        tabview = ctk.CTkTabview(self.window)
        tabview.grid(row=0, column=0, padx=20, pady=20, sticky="nsew")

        tab_general = tabview.add("⚡ General & Microphone")
        tab_ai = tabview.add("🔑 Gemini AI Engine")
        tab_history = tabview.add("📜 History")

        # 1. Microphone & Shortcut Tab
        ctk.CTkLabel(tab_general, text="Microphone Input Device (Auto-Configured for WASAPI 48kHz):", font=ctk.CTkFont(weight="bold", size=14)).pack(anchor="w", padx=15, pady=(10, 4))
        
        dev_list = []
        self.dev_map = {}
        for idx, name in self.audio_recorder.get_input_devices():
            dev_list.append(name)
            self.dev_map[name] = idx

        current_dev_idx = self.config.get("input_device_index", 9)
        current_dev_name = dev_list[0] if dev_list else "Default Microphone"
        for name, idx in self.dev_map.items():
            if idx == current_dev_idx:
                current_dev_name = name
                break

        self.dev_var = ctk.StringVar(value=current_dev_name)
        dev_menu = ctk.CTkOptionMenu(
            tab_general,
            values=dev_list or ["Default Microphone"],
            variable=self.dev_var,
            command=self._on_device_changed,
            width=420
        )
        dev_menu.pack(anchor="w", padx=15, pady=(0, 10))

        # Live Mic Volume Meter
        vu_frame = ctk.CTkFrame(tab_general, fg_color="#1E293B", corner_radius=8)
        vu_frame.pack(fill="x", padx=15, pady=(0, 15))
        
        ctk.CTkLabel(vu_frame, text="Live Microphone Input Level (Speak to test):", font=ctk.CTkFont(size=12, weight="bold")).pack(anchor="w", padx=12, pady=(8, 4))
        self.vu_progress = ctk.CTkProgressBar(vu_frame, width=320, height=12)
        self.vu_progress.set(0.0)
        self.vu_progress.pack(fill="x", padx=12, pady=(0, 8))

        # Shortcut & Tone
        ctk.CTkLabel(tab_general, text="Global Dictation Shortcut:", font=ctk.CTkFont(weight="bold", size=14)).pack(anchor="w", padx=15, pady=(5, 4))
        self.hk_var = ctk.StringVar(value=self.config.get("hotkey", "f8"))
        hk_menu = ctk.CTkOptionMenu(
            tab_general,
            values=["f8", "f9", "<ctrl>+<shift>+<space>", "<caps_lock>", "<alt>+<space>"],
            variable=self.hk_var,
            command=self._on_hotkey_changed,
            width=220
        )
        hk_menu.pack(anchor="w", padx=15, pady=(0, 12))

        ctk.CTkLabel(tab_general, text="Dictation Tone / Style:", font=ctk.CTkFont(weight="bold", size=14)).pack(anchor="w", padx=15, pady=(5, 4))
        self.mode_var = ctk.StringVar(value=self.config.get("dictation_mode", "smart_flow"))
        mode_menu = ctk.CTkOptionMenu(
            tab_general,
            values=["smart_flow", "verbatim", "code", "bullet_notes", "formal_email"],
            variable=self.mode_var,
            command=self._on_mode_changed,
            width=220
        )
        mode_menu.pack(anchor="w", padx=15, pady=(0, 15))

        # 2. AI Engine Tab
        ctk.CTkLabel(tab_ai, text="Google AI Studio Gemini API Key:", font=ctk.CTkFont(weight="bold", size=14)).pack(anchor="w", padx=15, pady=(15, 5))
        self.api_entry = ctk.CTkEntry(tab_ai, placeholder_text="Enter API Key", show="*")
        self.api_entry.insert(0, self.config.get("google_api_key", ""))
        self.api_entry.pack(fill="x", padx=15, pady=(0, 10))

        btn_row = ctk.CTkFrame(tab_ai, fg_color="transparent")
        btn_row.pack(anchor="w", padx=15, pady=5)

        ctk.CTkButton(btn_row, text="💾 Save Key", command=self._save_api_key, width=120).pack(side="left", padx=(0, 10))
        ctk.CTkButton(btn_row, text="⚡ Test Speed", command=self._test_connection, width=120, fg_color="#10B981").pack(side="left", padx=(0, 10))
        ctk.CTkButton(btn_row, text="Get Free Key", command=lambda: webbrowser.open("https://aistudio.google.com/apikey"), width=120, fg_color="#6366F1").pack(side="left")

        self.api_status = ctk.CTkLabel(tab_ai, text="", font=ctk.CTkFont(size=12))
        self.api_status.pack(anchor="w", padx=15, pady=10)

        # 3. History Tab
        history = self.config.load_history()
        scroll = ctk.CTkScrollableFrame(tab_history, height=320)
        scroll.pack(fill="both", expand=True, padx=10, pady=10)

        if not history:
            ctk.CTkLabel(scroll, text="No dictations yet. Press F8 and start talking in MS Word!").pack(pady=40)
        else:
            for item in history:
                card = ctk.CTkFrame(scroll, fg_color="#1E293B", corner_radius=8)
                card.pack(fill="x", pady=4, padx=5)
                ctk.CTkLabel(card, text=f"🕒 {item.get('timestamp')} &bull; {item.get('mode')}", font=ctk.CTkFont(size=11), text_color="gray").pack(anchor="w", padx=10, pady=(6, 2))
                ctk.CTkLabel(card, text=item.get("text", ""), wraplength=550, justify="left").pack(anchor="w", padx=10, pady=(0, 8))

        self._start_vu_monitor()

    def _start_vu_monitor(self):
        self.test_running = True
        def monitor():
            import math, struct
            dev_idx = self.config.get("input_device_index", 9)
            def callback(indata, frames, time_info, status):
                raw = bytes(indata)
                count = len(raw) // 2
                if count > 0 and self.is_open:
                    shorts = struct.unpack(f"<{count}h", raw[:count*2])
                    rms = math.sqrt(sum(s*s for s in shorts) / count)
                    level = min(1.0, float(rms * 0.015))
                    if self.window and self.vu_progress:
                        try:
                            self.window.after(0, lambda: self.vu_progress.set(level))
                        except Exception:
                            pass
            try:
                self.test_stream = sd.RawInputStream(samplerate=48000, channels=2, dtype="int16", device=dev_idx, callback=callback, blocksize=2048)
                self.test_stream.start()
                while self.test_running and self.is_open:
                    import time
                    time.sleep(0.1)
                if self.test_stream:
                    self.test_stream.stop()
                    self.test_stream.close()
            except Exception:
                pass
        threading.Thread(target=monitor, daemon=True).start()

    def _on_device_changed(self, choice):
        idx = self.dev_map.get(choice, 9)
        self.config.set("input_device_index", idx)
        if self.test_stream:
            try:
                self.test_stream.stop()
                self.test_stream.close()
            except Exception:
                pass
            self._start_vu_monitor()

    def _on_hotkey_changed(self, choice):
        self.config.set("hotkey", choice)
        if self.on_settings_changed:
            self.on_settings_changed()

    def _on_mode_changed(self, choice):
        self.config.set("dictation_mode", choice)

    def _save_api_key(self):
        k = self.api_entry.get().strip()
        self.config.set("google_api_key", k)
        self.api_status.configure(text="Key saved! ✓", text_color="#10B981")

    def _test_connection(self):
        self.api_status.configure(text="Testing...", text_color="#38BDF8")
        def run():
            s, m, l = self.ai_engine.test_connection(self.api_entry.get().strip())
            color = "#10B981" if s else "#EF4444"
            if self.window and self.api_status:
                self.api_status.configure(text=m, text_color=color)
        threading.Thread(target=run, daemon=True).start()

    def _on_close(self):
        self.test_running = False
        if self.test_stream:
            try:
                self.test_stream.stop()
                self.test_stream.close()
            except Exception:
                pass
        self.is_open = False
        if self.window:
            self.window.destroy()
            self.window = None
