import threading
import webbrowser
import customtkinter as ctk


ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

class SettingsWindow:
    def __init__(self, config_manager, ai_engine, audio_recorder=None, snippet_engine=None, on_settings_changed=None, bridge_server=None):
        self.config = config_manager
        self.ai_engine = ai_engine
        self.audio_recorder = audio_recorder
        self.snippet_engine = snippet_engine
        self.on_settings_changed = on_settings_changed
        self.bridge_server = bridge_server
        self.window = None
        self.is_open = False
        self.test_stream = None
        self.test_running = False
        self._vu_paused = False
        self.dev_map = {}
        self.test_key_btn = None
        self.key_status_label = None
        self.qr_label = None
        self.pin_label = None

    def show(self, initial_tab=None):
        if self.is_open and self.window:
            self.window.lift()
            self.window.focus_force()
            if initial_tab and hasattr(self, "tabview") and self.tabview:
                try:
                    self.tabview.set(initial_tab)
                except Exception:
                    pass
            return

        self.is_open = True
        self.window = ctk.CTkToplevel()
        self.window.title("OwnVoice — Settings & Audio Devices")
        self.window.geometry("780x600")
        self.window.minsize(720, 540)
        self.window.protocol("WM_DELETE_WINDOW", self._on_close)

        self.window.grid_columnconfigure(0, weight=1)
        self.window.grid_rowconfigure(0, weight=1)

        tabview = ctk.CTkTabview(self.window)
        self.tabview = tabview
        tabview.grid(row=0, column=0, padx=20, pady=20, sticky="nsew")

        tab_general = tabview.add("⚡ General & Audio")
        tab_link = tabview.add("📱 Phone Link")
        tab_snippets = tabview.add("✂️ Voice Snippets")
        tab_vocab = tabview.add("🧠 Personal Vocabulary")
        tab_ai = tabview.add("🔑 Gemini AI Engine")
        tab_history = tabview.add("📜 History")

        self._setup_phone_link_tab(tab_link)

        if initial_tab:
            try:
                self.window.after(50, lambda: self.tabview.set(initial_tab))
            except Exception:
                pass

        # ----------------- Tab 1: General -----------------
        ctk.CTkLabel(tab_general, text="Microphone Input Device (Safe Verified Devices):", font=ctk.CTkFont(weight="bold", size=13)).pack(anchor="w", padx=15, pady=(10, 4))
        
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
            text="App-Aware Tone Detection (Auto-switches tone for Code, Search, Word, Email & Chat)",
            variable=self.auto_ctx_var,
            command=self._on_auto_ctx_changed,
            font=ctk.CTkFont(weight="bold")
        )
        auto_ctx_cb.pack(anchor="w", padx=15, pady=(0, 8))

        # Sound Effects Toggle (v2.0)
        self.sound_fx_var = ctk.BooleanVar(value=self.config.get("sound_effects", True))
        sound_fx_cb = ctk.CTkCheckBox(
            tab_general,
            text="Sound Effects (Acoustic audio cues on recording start, stop & success)",
            variable=self.sound_fx_var,
            command=self._on_sound_fx_changed,
            font=ctk.CTkFont(weight="bold")
        )
        sound_fx_cb.pack(anchor="w", padx=15, pady=(0, 12))

        # Hotkey selector & Default Tone row
        opts_row = ctk.CTkFrame(tab_general, fg_color="transparent")
        opts_row.pack(fill="x", padx=15, pady=(0, 10))

        hk_col = ctk.CTkFrame(opts_row, fg_color="transparent")
        hk_col.pack(side="left", padx=(0, 20))
        ctk.CTkLabel(hk_col, text="Global Dictation Shortcut:", font=ctk.CTkFont(weight="bold", size=13)).pack(anchor="w", pady=(0, 4))
        self.hk_var = ctk.StringVar(value=self.config.get("hotkey", "f8"))
        hk_menu = ctk.CTkOptionMenu(
            hk_col,
            values=["f8", "f9", "<ctrl>+<shift>+<space>", "<caps_lock>", "<alt>+<space>"],
            variable=self.hk_var,
            command=self._on_hotkey_changed,
            width=210
        )
        hk_menu.pack(anchor="w")

        tone_col = ctk.CTkFrame(opts_row, fg_color="transparent")
        tone_col.pack(side="left")
        ctk.CTkLabel(tone_col, text="Default Fallback Tone:", font=ctk.CTkFont(weight="bold", size=13)).pack(anchor="w", pady=(0, 4))
        self.tone_var = ctk.StringVar(value=self.config.get("dictation_mode", "smart_flow"))
        tone_menu = ctk.CTkOptionMenu(
            tone_col,
            values=["smart_flow", "search", "code", "formal_email", "formal_document", "translate_hindi", "translate_english", "bullet_notes", "verbatim"],
            variable=self.tone_var,
            command=self._on_tone_changed,
            width=210
        )
        tone_menu.pack(anchor="w")

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

        # ----------------- Tab 3: Personal Vocabulary (v2.0) -----------------
        ctk.CTkLabel(tab_vocab, text="Personal Vocabulary & AI Self-Correction (v2.0)", font=ctk.CTkFont(weight="bold", size=14)).pack(anchor="w", padx=15, pady=(10, 2))
        ctk.CTkLabel(tab_vocab, text="Teach Gemini custom names, local cities, and startup jargon. Gemini will prioritize this exact spelling.", text_color="gray", font=ctk.CTkFont(size=11)).pack(anchor="w", padx=15, pady=(0, 10))

        self.self_corr_var = ctk.BooleanVar(value=self.config.get("self_correction", True))
        self_corr_cb = ctk.CTkCheckBox(
            tab_vocab,
            text="Spoken Mid-Sentence Self-Correction (Auto-cleans 'meet at 4, actually make it 5 PM')",
            variable=self.self_corr_var,
            command=self._on_self_corr_changed,
            font=ctk.CTkFont(weight="bold")
        )
        self_corr_cb.pack(anchor="w", padx=15, pady=(0, 12))

        vocab_add_frame = ctk.CTkFrame(tab_vocab, fg_color="#1E293B", corner_radius=8)
        vocab_add_frame.pack(fill="x", padx=15, pady=(0, 10))

        self.vocab_entry = ctk.CTkEntry(vocab_add_frame, placeholder_text="Word / Name / Jargon (e.g. 'Aarav', 'Bengaluru', 'Kubernetes')", width=380)
        self.vocab_entry.pack(side="left", padx=10, pady=8)

        ctk.CTkButton(vocab_add_frame, text="➕ Add Term", width=100, command=self._add_vocab_term).pack(side="left", padx=10, pady=8)

        self.vocab_scroll = ctk.CTkScrollableFrame(tab_vocab, height=240)
        self.vocab_scroll.pack(fill="both", expand=True, padx=15, pady=5)
        self._refresh_vocab_list()

        # ----------------- Tab 4: AI Engine -----------------
        ctk.CTkLabel(tab_ai, text="Google AI Studio Gemini API Key:", font=ctk.CTkFont(weight="bold", size=14)).pack(anchor="w", padx=15, pady=(15, 5))
        
        key_input_row = ctk.CTkFrame(tab_ai, fg_color="transparent")
        key_input_row.pack(fill="x", padx=15, pady=(0, 6))

        self.api_entry = ctk.CTkEntry(key_input_row, placeholder_text="Enter API Key", show="*")
        self.api_entry.insert(0, self.config.get("google_api_key", ""))
        self.api_entry.pack(side="left", fill="x", expand=True, padx=(0, 10))

        self.test_key_btn = ctk.CTkButton(
            key_input_row,
            text="🔑 Test Key",
            command=self._test_key,
            width=110,
            fg_color="#0284C7",
            hover_color="#0369A1"
        )
        self.test_key_btn.pack(side="right")

        self.key_status_label = ctk.CTkLabel(tab_ai, text="", font=ctk.CTkFont(size=12, weight="bold"))
        self.key_status_label.pack(anchor="w", padx=15, pady=(0, 8))

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

        if self.audio_recorder:
            dev_idx = self.config.get("input_device_index", None)
            self.audio_recorder.start_monitoring(device_index=dev_idx)
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

    def _on_sound_fx_changed(self):
        val = self.sound_fx_var.get()
        self.config.set("sound_effects", val)
        try:
            from core.sound_effects import sound_effects
            sound_effects.enabled = val
        except Exception:
            pass

    def _on_tone_changed(self, new_tone):
        self.config.set("dictation_mode", new_tone)

    def _on_self_corr_changed(self):
        self.config.set("self_correction", self.self_corr_var.get())

    def _refresh_vocab_list(self):
        for widget in self.vocab_scroll.winfo_children():
            widget.destroy()

        vocab = self.config.get_vocabulary() if hasattr(self.config, "get_vocabulary") else self.config.get("vocabulary", [])
        if not vocab:
            ctk.CTkLabel(self.vocab_scroll, text="No custom vocabulary added yet. Add terms above!").pack(pady=20)
            return

        for term in vocab:
            card = ctk.CTkFrame(self.vocab_scroll, fg_color="#0F172A", corner_radius=6)
            card.pack(fill="x", pady=3, padx=5)

            ctk.CTkLabel(card, text=f"🏷️  {term}", font=ctk.CTkFont(weight="bold", size=12), text_color="#38BDF8").pack(side="left", padx=10, pady=6)

            del_btn = ctk.CTkButton(
                card,
                text="✕",
                width=28,
                height=24,
                fg_color="#7F1D1D",
                hover_color="#EF4444",
                command=lambda t=term: self._delete_vocab_term(t)
            )
            del_btn.pack(side="right", padx=10, pady=4)

    def _add_vocab_term(self):
        term = self.vocab_entry.get().strip()
        if term:
            if hasattr(self.config, "add_vocabulary_term"):
                self.config.add_vocabulary_term(term)
            else:
                current = list(self.config.get("vocabulary", []))
                if term not in current:
                    current.append(term)
                    self.config.set("vocabulary", current)
            self.vocab_entry.delete(0, "end")
            self._refresh_vocab_list()

    def _delete_vocab_term(self, term):
        if hasattr(self.config, "remove_vocabulary_term"):
            self.config.remove_vocabulary_term(term)
        else:
            current = list(self.config.get("vocabulary", []))
            if term in current:
                current.remove(term)
                self.config.set("vocabulary", current)
        self._refresh_vocab_list()

    def stop_vu_monitor(self):
        """Cleanly yields VU meter updates during active dictation without disrupting the unified stream."""
        self._vu_paused = True

    def resume_vu_monitor(self):
        """Resumes VU meter updates once active dictation completes."""
        self._vu_paused = False

    def _start_vu_monitor(self):
        self.test_running = True
        self._vu_paused = False
        
        def monitor():
            import time
            while self.test_running and self.is_open:
                # Cleanly yield to active dictation to prevent audio device contention or UI queue flooding
                is_rec = getattr(self.audio_recorder, "is_recording", False) if self.audio_recorder else False
                if self._vu_paused or is_rec:
                    time.sleep(0.08)
                    continue

                if self.audio_recorder:
                    vol = self.audio_recorder.get_current_volume()
                    if self.window and self.vu_progress:
                        def safe_set(v=vol):
                            if self.is_open and self.window and self.vu_progress and not self._vu_paused:
                                try:
                                    self.vu_progress.set(v)
                                except Exception:
                                    pass
                        try:
                            self.window.after(0, safe_set)
                        except Exception:
                            pass
                time.sleep(0.04)

        threading.Thread(target=monitor, daemon=True).start()

    def _on_device_changed(self, choice):
        idx = self.dev_map.get(choice, None)
        if idx is not None:
            self.config.set("input_device_index", idx)
            self.config.set("input_device_name", choice)
            if self.audio_recorder and not getattr(self.audio_recorder, "is_recording", False):
                self.audio_recorder.stop_monitoring()
                self.audio_recorder.start_monitoring(device_index=idx)

    def _on_hotkey_changed(self, choice):
        self.config.set("hotkey", choice)
        if self.on_settings_changed:
            self.on_settings_changed()

    def _save_api_key(self):
        k = self.api_entry.get().strip() if self.api_entry else ""
        self.config.set("google_api_key", k)
        self.api_status.configure(text="Key saved! ✓", text_color="#10B981")

    def _set_key_status_ui(self, text: str, is_valid: bool = None):
        if is_valid is True:
            color = "#10B981"  # clear green
        elif is_valid is False:
            color = "#EF4444"  # clear red
        else:
            color = "#38BDF8"  # blue/testing

        if hasattr(self, "key_status_label") and self.key_status_label:
            try:
                self.key_status_label.configure(text=text, text_color=color)
            except Exception:
                pass
        if hasattr(self, "api_status") and self.api_status:
            try:
                self.api_status.configure(text=text, text_color=color)
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
            if not self.is_open or not self.window:
                return
            if getattr(self, "_key_test_result", None) is not None:
                valid, msg = self._key_test_result
                self._set_key_status_ui(msg, valid)
            else:
                try:
                    self.window.after(20, poll_result)
                except Exception:
                    pass

        threading.Thread(target=ping, daemon=True).start()
        if self.window:
            try:
                self.window.after(20, poll_result)
            except Exception:
                pass

    def _test_connection(self):
        self.api_status.configure(text="Testing...", text_color="#38BDF8")
        self._conn_test_result = None
        key = self.api_entry.get().strip() if self.api_entry else ""

        def run():
            try:
                s, m, l = self.ai_engine.test_connection(key)
                color = "#10B981" if s else "#EF4444"
                self._conn_test_result = (m, color)
            except Exception as e:
                self._conn_test_result = (str(e), "#EF4444")

        def poll_conn():
            if not self.is_open or not self.window:
                return
            if getattr(self, "_conn_test_result", None) is not None:
                msg, color = self._conn_test_result
                if self.api_status:
                    try:
                        self.api_status.configure(text=msg, text_color=color)
                    except Exception:
                        pass
            else:
                try:
                    self.window.after(20, poll_conn)
                except Exception:
                    pass

        threading.Thread(target=run, daemon=True).start()
        if self.window:
            try:
                self.window.after(20, poll_conn)
            except Exception:
                pass

    def _on_close(self):
        self.test_running = False
        self._vu_paused = False
        self.is_open = False
        if self.audio_recorder:
            self.audio_recorder.stop_monitoring()
        if self.window:
            try:
                self.window.destroy()
            except Exception:
                pass
            self.window = None

    def _setup_phone_link_tab(self, tab_link):
        ctk.CTkLabel(tab_link, text="📱 Universal Phone Link (Ecosystem Bridge)", font=ctk.CTkFont(weight="bold", size=15)).pack(anchor="w", padx=15, pady=(10, 2))
        ctk.CTkLabel(
            tab_link,
            text="Turn your Android phone into a wireless microphone that types directly into your PC cursor over local Wi-Fi.",
            text_color="gray",
            font=ctk.CTkFont(size=11)
        ).pack(anchor="w", padx=15, pady=(0, 10))

        status_frame = ctk.CTkFrame(tab_link, fg_color="#1E293B", corner_radius=8)
        status_frame.pack(fill="x", padx=15, pady=(0, 12))

        local_ip = self.bridge_server.local_ip if self.bridge_server else "127.0.0.1"
        port = self.bridge_server.port if self.bridge_server else 8765

        ctk.CTkLabel(
            status_frame,
            text=f"🟢 Local Wi-Fi Discovery Active • Server: {local_ip}:{port} • Auto-Discovery: UDP 8766",
            font=ctk.CTkFont(weight="bold", size=12),
            text_color="#10B981"
        ).pack(side="left", padx=12, pady=8)

        content_card = ctk.CTkFrame(tab_link, fg_color="#0F172A", corner_radius=10)
        content_card.pack(fill="both", expand=True, padx=15, pady=(0, 10))

        left_col = ctk.CTkFrame(content_card, fg_color="transparent")
        left_col.pack(side="left", padx=20, pady=15, fill="y")

        self.qr_label = ctk.CTkLabel(left_col, text="")
        self.qr_label.pack(pady=(0, 8))

        ctk.CTkLabel(left_col, text="📷 Scan with OwnVoice Camera", font=ctk.CTkFont(size=11), text_color="#94A3B8").pack()

        right_col = ctk.CTkFrame(content_card, fg_color="transparent")
        right_col.pack(side="left", fill="both", expand=True, padx=(10, 20), pady=15)

        ctk.CTkLabel(right_col, text="3 Zero-Friction Ways to Connect:", font=ctk.CTkFont(weight="bold", size=13), text_color="#38BDF8").pack(anchor="w", pady=(0, 8))

        # Option 1: 1-Tap Auto-Discover
        opt1 = ctk.CTkFrame(right_col, fg_color="#1E293B", corner_radius=6)
        opt1.pack(fill="x", pady=4)
        ctk.CTkLabel(opt1, text="⚡ Option 1: 1-Tap Auto-Discover (Recommended)", font=ctk.CTkFont(weight="bold", size=11), text_color="#34D399").pack(anchor="w", padx=10, pady=(6, 2))
        ctk.CTkLabel(opt1, text="If phone & PC are on the same Wi-Fi, open OwnVoice on phone and tap '1-Tap Auto-Discover'. Connected in <1 second with zero typing!", font=ctk.CTkFont(size=10), text_color="#CBD5E1", wraplength=340, justify="left").pack(anchor="w", padx=10, pady=(0, 6))

        # Option 2: QR Code
        opt2 = ctk.CTkFrame(right_col, fg_color="#1E293B", corner_radius=6)
        opt2.pack(fill="x", pady=4)
        ctk.CTkLabel(opt2, text="📷 Option 2: Scan QR Code (WhatsApp Web style)", font=ctk.CTkFont(weight="bold", size=11), text_color="#38BDF8").pack(anchor="w", padx=10, pady=(6, 2))
        ctk.CTkLabel(opt2, text="In OwnVoice on phone, tap 'Scan QR Code' and point camera at the code on the left.", font=ctk.CTkFont(size=10), text_color="#CBD5E1", wraplength=340, justify="left").pack(anchor="w", padx=10, pady=(0, 6))

        # Option 3: 6-Digit PIN
        opt3 = ctk.CTkFrame(right_col, fg_color="#1E293B", corner_radius=6)
        opt3.pack(fill="x", pady=4)
        ctk.CTkLabel(opt3, text="🔢 Option 3: Enter 6-Digit PIN", font=ctk.CTkFont(weight="bold", size=11), text_color="#FBBF24").pack(anchor="w", padx=10, pady=(6, 2))

        pin_row = ctk.CTkFrame(opt3, fg_color="transparent")
        pin_row.pack(anchor="w", padx=10, pady=(2, 6))

        pin_val = self.bridge_server.current_pin if self.bridge_server else "000000"
        self.pin_label = ctk.CTkLabel(
            pin_row,
            text=f"  {pin_val[:3]} {pin_val[3:]}  ",
            font=ctk.CTkFont(family="Consolas", size=18, weight="bold"),
            fg_color="#0F172A",
            text_color="#FBBF24",
            corner_radius=6
        )
        self.pin_label.pack(side="left", padx=(0, 10))

        ctk.CTkButton(
            pin_row,
            text="🔄 New PIN",
            width=80,
            height=28,
            command=self._regenerate_bridge_pin,
            fg_color="#334155",
            hover_color="#475569"
        ).pack(side="left")

        self._refresh_qr_image()

    def _regenerate_bridge_pin(self):
        if self.bridge_server:
            new_pin = self.bridge_server.regenerate_pin()
            if self.pin_label:
                self.pin_label.configure(text=f"  {new_pin[:3]} {new_pin[3:]}  ")
            self._refresh_qr_image()

    def _refresh_qr_image(self):
        if self.bridge_server and self.qr_label:
            try:
                pil_img = self.bridge_server.generate_qr_image(size=180)
                ctk_img = ctk.CTkImage(light_image=pil_img, dark_image=pil_img, size=(180, 180))
                self.qr_label.configure(image=ctk_img)
            except Exception as e:
                print(f"[SettingsWindow] QR render error: {e}")
