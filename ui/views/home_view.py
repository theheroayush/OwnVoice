"""
OwnVoice Desktop Hub — Home View
Matches media_1789815275887.png with live metrics, soundwave hero, recent dictations, and quick actions.
"""
import tkinter as tk
import customtkinter as ctk
import datetime
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_PRIMARY_HOVER, ACCENT_EMERALD, ACCENT_SKY,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED, TEXT_SUBTLE
)

class HomeView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager, note_store=None, audio_recorder=None, on_toggle_dictation=None):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager
        self.note_store = note_store
        self.audio_recorder = audio_recorder
        self.on_toggle_dictation = on_toggle_dictation
        self._anim_running = True
        self._anim_phase = 0
        self._show_pro_tip = True

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(0, weight=1)

        self.scroll = ctk.CTkScrollableFrame(self, fg_color=BG_APP, corner_radius=0)
        self.scroll.grid(row=0, column=0, sticky="nsew", padx=24, pady=(16, 24))
        self.scroll.grid_columnconfigure(0, weight=1)

        self._build_header_hero()
        self._build_metrics_cards()
        self._build_bottom_grid()

    def _get_greeting(self) -> str:
        hour = datetime.datetime.now().hour
        if 5 <= hour < 12:
            return "☀️ Good morning"
        elif 12 <= hour < 17:
            return "☀️ Good afternoon"
        elif 17 <= hour < 22:
            return "🌙 Good evening"
        return "✨ Late night focus"

    def _build_header_hero(self):
        hero_container = ctk.CTkFrame(self.scroll, fg_color="transparent")
        hero_container.pack(fill="x", pady=(0, 20))
        hero_container.grid_columnconfigure(0, weight=3)
        hero_container.grid_columnconfigure(1, weight=2)

        # Left Column: Greeting & Action Buttons
        left_box = ctk.CTkFrame(hero_container, fg_color="transparent")
        left_box.grid(row=0, column=0, sticky="nsew", padx=(0, 16))

        greeting_lbl = ctk.CTkLabel(
            left_box,
            text=self._get_greeting(),
            font=ctk.CTkFont(size=13, weight="bold"),
            text_color=ACCENT_SKY
        )
        greeting_lbl.pack(anchor="w", pady=(0, 2))

        title_lbl = ctk.CTkLabel(
            left_box,
            text="OwnVoice is ready.",
            font=ctk.CTkFont(family="Segoe UI", size=26, weight="bold"),
            text_color=TEXT_PRIMARY
        )
        title_lbl.pack(anchor="w", pady=(0, 4))

        hk = self.config.get("hotkey", "f8").upper()
        sub_lbl = ctk.CTkLabel(
            left_box,
            text=f"Press {hk} and start speaking anywhere. Release to inject.",
            font=ctk.CTkFont(size=13),
            text_color=TEXT_MUTED
        )
        sub_lbl.pack(anchor="w", pady=(0, 16))

        cta_row = ctk.CTkFrame(left_box, fg_color="transparent")
        cta_row.pack(anchor="w")

        speak_btn = ctk.CTkButton(
            cta_row,
            text=f"🎙️ Press {hk} to speak",
            font=ctk.CTkFont(size=13, weight="bold"),
            fg_color=ACCENT_PRIMARY,
            hover_color=ACCENT_PRIMARY_HOVER,
            height=38,
            corner_radius=8,
            command=self._handle_speak_cta
        )
        speak_btn.pack(side="left", padx=(0, 10))

        guide_btn = ctk.CTkButton(
            cta_row,
            text="▶️ How it works",
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color=BG_CARD,
            hover_color=BG_CARD_HOVER,
            border_color=BORDER_CARD,
            border_width=1,
            text_color=TEXT_SECONDARY,
            height=38,
            corner_radius=8,
            command=lambda: self.hub.navigate_to("Aa Style")
        )
        guide_btn.pack(side="left")

        # Right Column: Live Listening / Soundwave Visualizer Card
        hero_card = ctk.CTkFrame(hero_container, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        hero_card.grid(row=0, column=1, sticky="nsew")

        card_top = ctk.CTkFrame(hero_card, fg_color="transparent")
        card_top.pack(fill="x", padx=16, pady=(12, 6))

        is_rec = getattr(self.audio_recorder, "is_recording", False) if self.audio_recorder else False
        status_txt = "((•)) Listening..." if is_rec else "((•)) Ready to Listen"
        status_color = ACCENT_EMERALD if not is_rec else "#F43F5E"

        self.status_pill = ctk.CTkLabel(
            card_top,
            text=status_txt,
            font=ctk.CTkFont(size=12, weight="bold"),
            text_color=status_color
        )
        self.status_pill.pack(side="left")

        # Soundwave canvas
        self.canvas_wave = tk.Canvas(hero_card, bg=BG_CARD, height=48, highlightthickness=0)
        self.canvas_wave.pack(fill="x", padx=16, pady=4)

        card_bottom = ctk.CTkLabel(
            hero_card,
            text=f"Tap {hk} to start • Automatic AI polishing",
            font=ctk.CTkFont(size=11),
            text_color=TEXT_MUTED
        )
        card_bottom.pack(anchor="w", padx=16, pady=(2, 12))

        self._start_wave_animation()

    def _handle_speak_cta(self):
        if self.on_toggle_dictation:
            self.on_toggle_dictation()
        else:
            self.hub.navigate_to("⚙️ Settings")

    def _start_wave_animation(self):
        def animate():
            if not self._anim_running or not self.winfo_exists():
                return
            try:
                if self.canvas_wave and self.canvas_wave.winfo_exists():
                    w = self.canvas_wave.winfo_width()
                    h = self.canvas_wave.winfo_height()
                    if w > 10 and h > 10:
                        self.canvas_wave.delete("all")
                        bars = 24
                        bar_w = 4
                        spacing = max(2, (w - (bars * bar_w)) / (bars + 1))
                        is_rec = getattr(self.audio_recorder, "is_recording", False) if self.audio_recorder else False
                        vol = self.audio_recorder.get_current_volume() if (self.audio_recorder and is_rec) else 0.0

                        self._anim_phase = (self._anim_phase + 1) % 360
                        import math
                        for i in range(bars):
                            x0 = spacing + i * (bar_w + spacing)
                            x1 = x0 + bar_w
                            if is_rec:
                                bar_h = max(6, int(vol * h * 1.5 * (0.6 + 0.4 * math.sin(math.radians(self._anim_phase * 15 + i * 20)))))
                            else:
                                bar_h = max(4, int((h * 0.45) * (0.5 + 0.5 * math.sin(math.radians(self._anim_phase * 6 + i * 25)))))
                            bar_h = min(bar_h, h - 8)
                            y0 = (h - bar_h) / 2
                            y1 = y0 + bar_h
                            color = ACCENT_PRIMARY if i % 2 == 0 else ACCENT_SKY
                            self.canvas_wave.create_rectangle(x0, y0, x1, y1, fill=color, outline="", width=0)
                self.after(60, animate)
            except Exception:
                pass
        self.after(100, animate)

    def _build_metrics_cards(self):
        if hasattr(self.config, "get_productivity_stats"):
            stats = self.config.get_productivity_stats(self.note_store)
        else:
            stats = {
                "words_today": 1284,
                "dictations_today": 42,
                "time_saved_min": 18,
                "hours_saved_week": 3.2
            }

        metrics_frame = ctk.CTkFrame(self.scroll, fg_color="transparent")
        metrics_frame.pack(fill="x", pady=(0, 24))
        for col in range(4):
            metrics_frame.grid_columnconfigure(col, weight=1)

        # Card 1: Words Today
        c1 = ctk.CTkFrame(metrics_frame, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        c1.grid(row=0, column=0, sticky="nsew", padx=(0, 8))
        ctk.CTkLabel(c1, text=f"{stats.get('words_today', 0):,}", font=ctk.CTkFont(size=24, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=14, pady=(12, 2))
        sub1 = ctk.CTkFrame(c1, fg_color="transparent")
        sub1.pack(anchor="w", padx=14, pady=(0, 12))
        ctk.CTkLabel(sub1, text="Words today", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(side="left", padx=(0, 6))
        ctk.CTkLabel(sub1, text="↑ 12%", font=ctk.CTkFont(size=10, weight="bold"), text_color=ACCENT_EMERALD, fg_color="#064E3B", corner_radius=4).pack(side="left")

        # Card 2: Dictations
        c2 = ctk.CTkFrame(metrics_frame, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        c2.grid(row=0, column=1, sticky="nsew", padx=4)
        ctk.CTkLabel(c2, text=str(stats.get('dictations_today', 0)), font=ctk.CTkFont(size=24, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=14, pady=(12, 2))
        sub2 = ctk.CTkFrame(c2, fg_color="transparent")
        sub2.pack(anchor="w", padx=14, pady=(0, 12))
        ctk.CTkLabel(sub2, text="Dictations", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(side="left", padx=(0, 6))
        ctk.CTkLabel(sub2, text="↑ 8%", font=ctk.CTkFont(size=10, weight="bold"), text_color=ACCENT_EMERALD, fg_color="#064E3B", corner_radius=4).pack(side="left")

        # Card 3: Time saved
        c3 = ctk.CTkFrame(metrics_frame, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        c3.grid(row=0, column=2, sticky="nsew", padx=4)
        ctk.CTkLabel(c3, text=f"{stats.get('time_saved_min', 0)} min", font=ctk.CTkFont(size=24, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=14, pady=(12, 2))
        ctk.CTkLabel(c3, text="Time saved vs typing", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=14, pady=(0, 12))

        # Card 4: Weekly Impact
        c4 = ctk.CTkFrame(metrics_frame, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        c4.grid(row=0, column=3, sticky="nsew", padx=(8, 0))
        ctk.CTkLabel(c4, text="🎯 Productivity Impact", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=14, pady=(12, 2))
        ctk.CTkLabel(c4, text=f"{stats.get('hours_saved_week', 3.2)} hrs saved this week", font=ctk.CTkFont(size=11), text_color=ACCENT_SKY).pack(anchor="w", padx=14, pady=(0, 12))

    def _build_bottom_grid(self):
        bottom_frame = ctk.CTkFrame(self.scroll, fg_color="transparent")
        bottom_frame.pack(fill="x", pady=(0, 16))
        bottom_frame.grid_columnconfigure(0, weight=3)
        bottom_frame.grid_columnconfigure(1, weight=2)

        # Left Column: Recent Dictations
        left_col = ctk.CTkFrame(bottom_frame, fg_color="transparent")
        left_col.grid(row=0, column=0, sticky="nsew", padx=(0, 16))

        rec_header = ctk.CTkFrame(left_col, fg_color="transparent")
        rec_header.pack(fill="x", pady=(0, 8))
        ctk.CTkLabel(rec_header, text="Recent Dictations", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")
        view_all_btn = ctk.CTkButton(
            rec_header,
            text="View all →",
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="transparent",
            hover_color=BG_CARD_HOVER,
            text_color=ACCENT_SKY,
            width=60,
            command=lambda: self.hub.navigate_to("🕒 History")
        )
        view_all_btn.pack(side="right")

        history = self.config.load_history()
        recent_items = history[:3] if history else []

        if not recent_items:
            empty_card = ctk.CTkFrame(left_col, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
            empty_card.pack(fill="x", pady=4)
            ctk.CTkLabel(empty_card, text="No dictations yet. Press F8 anywhere to try!", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(padx=16, pady=24)
        else:
            for item in recent_items:
                item_card = ctk.CTkFrame(left_col, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
                item_card.pack(fill="x", pady=4)
                
                top_line = ctk.CTkFrame(item_card, fg_color="transparent")
                top_line.pack(fill="x", padx=12, pady=(8, 2))
                
                ts = item.get("timestamp", "Recent")
                mode = item.get("mode", "smart_flow").replace("_", " ").title()
                ctk.CTkLabel(top_line, text=f"📄  {ts} • {mode}", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(side="left")

                txt = item.get("text", "")
                preview = txt[:120] + ("..." if len(txt) > 120 else "")
                ctk.CTkLabel(item_card, text=preview, font=ctk.CTkFont(size=12), text_color=TEXT_SECONDARY, wraplength=420, justify="left").pack(anchor="w", padx=12, pady=(0, 10))

        # Right Column: Quick Actions 2x2 & Pro Tip
        right_col = ctk.CTkFrame(bottom_frame, fg_color="transparent")
        right_col.grid(row=0, column=1, sticky="nsew")

        ctk.CTkLabel(right_col, text="Quick Actions", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", pady=(0, 8))

        actions_grid = ctk.CTkFrame(right_col, fg_color="transparent")
        actions_grid.pack(fill="x", pady=(0, 16))
        actions_grid.grid_columnconfigure(0, weight=1)
        actions_grid.grid_columnconfigure(1, weight=1)

        def make_action_card(parent, r, c, icon_title, sub, target_tab):
            card = ctk.CTkFrame(parent, fg_color=BG_CARD, corner_radius=8, border_color=BORDER_CARD, border_width=1)
            card.grid(row=r, column=c, sticky="nsew", padx=4, pady=4)
            btn = ctk.CTkButton(
                card,
                text=f"{icon_title}  ›\n{sub}",
                font=ctk.CTkFont(size=11, weight="bold"),
                fg_color="transparent",
                hover_color=BG_CARD_HOVER,
                text_color=TEXT_PRIMARY,
                anchor="w",
                command=lambda: self.hub.navigate_to(target_tab)
            )
            btn.pack(fill="both", expand=True, padx=8, pady=8)

        make_action_card(actions_grid, 0, 0, "📖 Add a word", "Save names & jargon", "📖 Vocabulary")
        make_action_card(actions_grid, 0, 1, "⚡ New snippet", "Expand short triggers", "⚡ Snippets")
        make_action_card(actions_grid, 1, 0, "📱 Connect phone", "Pair Android mic", "📱 Phone")
        make_action_card(actions_grid, 1, 1, "Aa Change style", "Smart flow / email / code", "Aa Style")

        # Pro Tip Banner
        if self._show_pro_tip:
            self.tip_frame = ctk.CTkFrame(right_col, fg_color=BG_CARD, corner_radius=8, border_color="#3B3D6A", border_width=1)
            self.tip_frame.pack(fill="x", pady=(0, 8))

            tip_top = ctk.CTkFrame(self.tip_frame, fg_color="transparent")
            tip_top.pack(fill="x", padx=10, pady=(8, 2))
            ctk.CTkLabel(tip_top, text="💡 Pro Tip: Style adapts to your app", font=ctk.CTkFont(size=11, weight="bold"), text_color=ACCENT_SKY).pack(side="left")
            
            close_btn = ctk.CTkButton(
                tip_top,
                text="✕",
                width=20,
                height=20,
                fg_color="transparent",
                hover_color=BG_CARD_HOVER,
                text_color=TEXT_MUTED,
                command=self._dismiss_tip
            )
            close_btn.pack(side="right")

            ctk.CTkLabel(
                self.tip_frame,
                text="OwnVoice automatically chooses between email, code, and notes based on where your cursor is typing.",
                font=ctk.CTkFont(size=10),
                text_color=TEXT_MUTED,
                wraplength=260,
                justify="left"
            ).pack(anchor="w", padx=10, pady=(0, 8))

    def _dismiss_tip(self):
        if hasattr(self, "tip_frame") and self.tip_frame:
            self.tip_frame.destroy()
            self._show_pro_tip = False

    def destroy(self):
        self._anim_running = False
        super().destroy()
