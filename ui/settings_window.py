import threading
import webbrowser
import customtkinter as ctk
import sounddevice as sd

ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

class SettingsWindow:
    def __init__(self, config_manager, ai_engine, audio_recorder=None, snippet_engine=None, on_settings_changed=None):
        self.config = config_manager
        self.ai_engine = ai_engine
        self.audio_recorder = audio_recorder
        self.snippet_engine = snippet_engine
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
        self.window.geometry("760x580")
        self.window.minsize(700, 520)
        self.window.protocol("WM_DELETE_WINDOW", self._on_close)

        self.window.grid_columnconfigure(0, weight=1)
        self.window.grid_rowconfigure(0, weight=1)

        tabview = ctk.CTkTabview(self.window)
        tabview.grid(row=0, column=0, padx=20, pady=20, sticky="nsew")

        tab_general = tabview.add("⚡ General & Audio")
        tab_snippets = tabview.add("✂️ Voice Snippets")
        tab_ai = tabview.add("🔑 Gemini AI Engine")
        tab_history = tabview.add("📜 History")

        # ----------------- Tab 1: General -----------------
        ctk.CTkLabel(tab_general, text="Microphone Input Device (Safe Verified Devices):", font=ctk.CTkFont(weight="bold", size=13)).pack(anchor="w", padx=15, pady=(10, 4))
        
        dev_list = []
        self.dev_map = {}
        if self.audio_recorder:
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
            width=460
        )
        dev_menu.pack(anchor="w", padx=15, pady=(0, 10))

        # Live Mic VU Meter
        vu_frame = ctk.CTkFrame(tab_general, fg_color="#1E293B", corner_radius=8)
        vu_frame.pack(fill="x", padx=15, pady=(0, 15))
        
        ctk.CTkLabel(vu_frame, text="Live Microphone Input Level (Speak to verify):", font=ctk.CTkFont(size=12, weight="bold")).pack(anchor="w", padx=12, pady=(8, 4))
        self.vu_progress = ctk.CTkProgressBar(vu_frame, width=320, height=12)
        self.vu_progress.set(0.0)
        self.vu_progress.pack(fill="x", padx=12, pady=(0, 8))

        # Auto-Context Toggle
        self.auto_ctx_var = ctk.BooleanVar(value=self.config.get("auto_context", True))
        auto_ctx_cb = ctk.CTkCheckBox(
            tab_general,
            text="App-Aware Tone Detection (Auto-switches tone for Code, Word, Email & Chat)",
            variable=self.auto_ctx_var,
            command=self._on_auto_ctx_changed,
            font=ctk.CTkFont(weight="bold")
        )
        auto_ctx_cb.pack(anchor="w", padx=15, pady=(0, 12))

        # Hotkey selector
        ctk.CTkLabel(tab_general, text="Global Dictation Shortcut:", font=ctk.CTkFont(weight="bold", size=13)).pack(anchor="w", padx=15, pady=(5, 4))
        self.hk_var = ctk.StringVar(value=self.config.get("hotkey", "f8"))
        hk_menu = ctk.CTkOptionMenu(
            tab_general,
            values=["f8", "f9", "<ctrl>+<shift>+<space>", "<caps_lock>", "<alt>+<space>"],
            variable=self.hk_var,
            command=self._on_hotkey_changed,
            width=220
        )
        hk_menu.pack(anchor="w", padx=15, pady=(0, 10))

        # ----------------- Tab 2: Voice Snippets -----------------
        ctk.CTkLabel(tab_snippets, text="Voice Snippets & Text Expander", font=ctk.CTkFont(weight="bold", size=14)).pack(anchor="w", padx=15, pady=(10, 2))
        ctk.CTkLabel(tab_snippets, text="Say the trigger phrase while dictating, and OwnVoice will expand it instantly.", text_color="gray", font=ctk.CTkFont(size=11)).pack(anchor="w", padx=15, pady=(0, 10))

        add_frame = ctk.CTkFrame(tab_snippets, fg_color="#1E293B", corner_radius=8)
        add_frame.pack(fill="x", padx=15, pady=(0, 10))
        
        self.snip_trig_entry = ctk.CTkEntry(add_frame, placeholder_text="Spoken Trigger (e.g. 'my meeting')", width=180)
        self.snip_trig_entry.pack(side="left", padx=10, pady=8)
        
        self.snip_exp_entry = ctk.CTkEntry(add_frame, placeholder_text="Expansion Text (e.g. 'https://cal.com/ayush')", width=300)
        self.snip_exp_entry.pack(side="left", padx=5, pady=8)

        ctk.CTkButton(add_frame, text="➕ Add", width=80, command=self._add_snippet).pack(side="left", padx=10, pady=8)

        self.snippet_scroll = ctk.CTkScrollableFrame(tab_snippets, height=260)
        self.snippet_scroll.pack(fill="both", expand=True, padx=15, pady=5)
        self._refresh_snippets_list()

        # ----------------- Tab 3: AI Engine -----------------
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

        # ----------------- Tab 4: History -----------------
        history = self.config.load_history()
        scroll = ctk.CTkScrollableFrame(tab_history, height=320)
        scroll.pack(fill="both", expand=True, padx=10, pady=10)

        if not history:
            ctk.CTkLabel(scroll, text="No dictations yet. Press F8 and start talking in any search box or document!").pack(pady=40)
        else:
            for item in history:
                card = ctk.CTkFrame(scroll, fg_color="#1E293B", corner_radius=8)
                card.pack(fill="x", pady=4, padx=5)
                ctk.CTkLabel(card, text=f"🕒 {item.get('timestamp')} • {item.get('mode', 'smart_flow')}", font=ctk.CTkFont(size=11), text_color="gray").pack(anchor="w", padx=10, pady=(6, 2))
                ctk.CTkLabel(card, text=item.get("text", ""), wraplength=550, justify="left").pack(anchor="w", padx=10, pady=(0, 8))

        self._start_vu_monitor()

    def _refresh_snippets_list(self):
        for widget in self.snippet_scroll.winfo_children():
            widget.destroy()
        
        snippets = self.snippet_engine.get_snippets() if self.snippet_engine else {}
        if not snippets:
            ctk.CTkLabel(self.snippet_scroll, text="No snippets configured. Add one above!").pack(pady=20)
            return

        for trig, exp in snippets.items():
            card = ctk.CTkFrame(self.snippet_scroll, fg_color="#0F172A", corner_radius=6)
            card.pack(fill="x", pady=3, padx=5)
            
            ctk.CTkLabel(card, text=f'🗣️ "{trig}"', font=ctk.CTkFont(weight="bold", size=12), text_color="#38BDF8").pack(side="left", padx=10, pady=6)
            ctk.CTkLabel(card, text=f"➔  {exp}", font=ctk.CTkFont(size=11), text_color="#E2E8F0").pack(side="left", padx=5, pady=6)
            
            del_btn = ctk.CTkButton(card, text="✕", width=28, height=24, fg_color="#7F1D1D", hover_color="#EF4444", command=lambda t=trig: self._delete_snippet(t))
            del_btn.pack(side="right", padx=10, pady=4)

    def _add_snippet(self):
        trig = self.snip_trig_entry.get().strip()
        exp = self.snip_exp_entry.get().strip()
        if trig and exp and self.snippet_engine:
            current = self.snippet_engine.get_snippets()
            current[trig] = exp
            self.snippet_engine.save_snippets(current)
            self.snip_trig_entry.delete(0, "end")
            self.snip_exp_entry.delete(0, "end")
            self._refresh_snippets_list()

    def _delete_snippet(self, trig):
        if self.snippet_engine:
            current = self.snippet_engine.get_snippets()
            if trig in current:
                del current[trig]
                self.snippet_engine.save_snippets(current)
                self._refresh_snippets_list()

    def _on_auto_ctx_changed(self):
        self.config.set("auto_context", self.auto_ctx_var.get())

    def stop_vu_monitor(self):
        """Allows app.py to yield exclusive mic access when recording starts."""
        self.test_running = False
        if self.test_stream:
            try:
                self.test_stream.stop()
                self.test_stream.close()
            except Exception:
                pass
            self.test_stream = None

    def _start_vu_monitor(self):
        self.stop_vu_monitor()
        self.test_running = True
        
        def monitor():
            import time, math, struct
            # If recorder is currently capturing, just read its volume
            while self.test_running and self.is_open:
                if self.audio_recorder and self.audio_recorder.is_recording:
                    vol = self.audio_recorder.get_current_volume()
                    if self.window and self.vu_progress:
                        try:
                            self.window.after(0, lambda v=vol: self.vu_progress.set(v))
                        except Exception:
                            pass
                    time.sleep(0.05)
                    continue

                # When idle, listen with gentle non-exclusive stream
                if not self.test_stream and self.test_running and self.is_open:
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
                        self.test_stream = sd.RawInputStream(
                            samplerate=48000,
                            channels=2,
                            dtype="int16",
                            device=dev_idx,
                            callback=callback,
                            blocksize=2048
                        )
                        self.test_stream.start()
                    except Exception:
                        pass
                time.sleep(0.1)

            self.stop_vu_monitor()

        threading.Thread(target=monitor, daemon=True).start()

    def _on_device_changed(self, choice):
        idx = self.dev_map.get(choice, 9)
        self.config.set("input_device_index", idx)
        self.stop_vu_monitor()
        self._start_vu_monitor()

    def _on_hotkey_changed(self, choice):
        self.config.set("hotkey", choice)
        if self.on_settings_changed:
            self.on_settings_changed()

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
        self.stop_vu_monitor()
        self.is_open = False
        if self.window:
            self.window.destroy()
            self.window = None
