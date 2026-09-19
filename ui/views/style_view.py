"""
OwnVoice Desktop Hub — Writing Style View
Matches media_1789815606395.png with 4 primary default style cards, app-specific dropdown table with sample quotes,
and right live preview panel with speech formatting and one-click copy.
"""
import customtkinter as ctk
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_PRIMARY_HOVER, ACCENT_EMERALD, ACCENT_SKY, ACCENT_AMBER,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED, TEXT_SUBTLE
)

class StyleView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager

        self.preview_styles = {
            "Casual": "Hey, can you share the meeting notes with the gang and remind them about the deadline next week?",
            "Natural": "Can you share the meeting notes with the team and also remind them about the deadline next week?",
            "Formal": "Please circulate the meeting minutes to the team and reiterate the upcoming deadline next week.",
            "Concise": "Share meeting notes with team; remind deadline next week."
        }
        self.active_preview_style = "Natural"

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(0, weight=1)

        self.scroll = ctk.CTkScrollableFrame(self, fg_color=BG_APP, corner_radius=0)
        self.scroll.grid(row=0, column=0, sticky="nsew", padx=24, pady=16)
        self.scroll.grid_columnconfigure(0, weight=1)

        self._build_header()
        self._build_split_layout()

    def _build_header(self):
        hdr = ctk.CTkFrame(self.scroll, fg_color="transparent")
        hdr.pack(fill="x", pady=(0, 16))

        ctk.CTkLabel(hdr, text="Aa  YOUR VOICE", font=ctk.CTkFont(size=10, weight="bold"), text_color=ACCENT_SKY).pack(anchor="w")

        title_row = ctk.CTkFrame(hdr, fg_color="transparent")
        title_row.pack(anchor="w", pady=(2, 2))
        ctk.CTkLabel(title_row, text="Writing ", font=ctk.CTkFont(family="Segoe UI", size=24, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")
        ctk.CTkLabel(title_row, text="style", font=ctk.CTkFont(family="Segoe UI", size=24, weight="bold"), text_color="#818CF8").pack(side="left")

        ctk.CTkLabel(hdr, text="OwnVoice adapts to your context, so your words always sound right.", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(anchor="w")

    def _build_split_layout(self):
        split = ctk.CTkFrame(self.scroll, fg_color="transparent")
        split.pack(fill="x", pady=(0, 16))
        split.grid_columnconfigure(0, weight=3)
        split.grid_columnconfigure(1, weight=2)

        # Left Column: Default Style Cards + App-Specific Styles Table
        left = ctk.CTkFrame(split, fg_color="transparent")
        left.grid(row=0, column=0, sticky="nsew", padx=(0, 16))

        # Section 1: Default Style
        def_card = ctk.CTkFrame(left, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        def_card.pack(fill="x", pady=(0, 16))

        ctk.CTkLabel(def_card, text="Default style", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=16, pady=(14, 2))
        ctk.CTkLabel(def_card, text="Choose how OwnVoice should write when there's no specific context.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 12))

        # 4 Cards Row
        cards_grid = ctk.CTkFrame(def_card, fg_color="transparent")
        cards_grid.pack(fill="x", padx=16, pady=(0, 16))
        for col in range(4):
            cards_grid.grid_columnconfigure(col, weight=1)

        styles = [
            ("casual", "Casual", "💬", "Relaxed and\nfriendly"),
            ("smart_flow", "Natural", "✨", "Balanced and\nconversational"),
            ("formal_email", "Formal", "💼", "Professional\nand polished"),
            ("code", "Concise", "📑", "Short and to\nthe point")
        ]

        current_mode = self.config.get("dictation_mode", "smart_flow")
        self.style_cards = {}

        for col, (mode_key, label, icon, desc) in enumerate(styles):
            is_active = (mode_key == current_mode) or (mode_key == "smart_flow" and current_mode not in ["casual", "formal_email", "code"])
            
            c = ctk.CTkFrame(
                cards_grid,
                fg_color="#1E2038" if is_active else "#0D121D",
                corner_radius=10,
                border_color="#4F46E5" if is_active else BORDER_CARD,
                border_width=1.5 if is_active else 1
            )
            c.grid(row=0, column=col, sticky="nsew", padx=4)
            self.style_cards[mode_key] = c

            c_top = ctk.CTkFrame(c, fg_color="transparent")
            c_top.pack(fill="x", padx=10, pady=(10, 4))
            ctk.CTkLabel(c_top, text=icon, font=ctk.CTkFont(size=15)).pack(side="left")
            
            if is_active:
                chk = ctk.CTkLabel(c_top, text="✓", font=ctk.CTkFont(size=11, weight="bold"), text_color="#FFFFFF", fg_color=ACCENT_PRIMARY, corner_radius=10, width=20, height=20)
                chk.pack(side="right")
                c._check_widget = chk
            else:
                c._check_widget = None

            lbl_w = ctk.CTkLabel(c, text=label, font=ctk.CTkFont(size=13, weight="bold"), text_color=TEXT_PRIMARY)
            lbl_w.pack(anchor="w", padx=10, pady=(2, 2))

            desc_w = ctk.CTkLabel(c, text=desc, font=ctk.CTkFont(size=10), text_color=TEXT_MUTED, justify="left")
            desc_w.pack(anchor="w", padx=10, pady=(0, 10))

            # Bind clicks
            for w in [c, c_top, lbl_w, desc_w]:
                w.bind("<Button-1>", lambda e, k=mode_key: self._select_mode(k))

        # Section 2: App-Specific Styles Table
        app_card = ctk.CTkFrame(left, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        app_card.pack(fill="x")

        app_hdr = ctk.CTkFrame(app_card, fg_color="transparent")
        app_hdr.pack(fill="x", padx=16, pady=(14, 2))

        ctk.CTkLabel(app_hdr, text="App-specific styles", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")

        self.auto_ctx_var = ctk.BooleanVar(value=self.config.get("auto_context", True))
        auto_switch = ctk.CTkSwitch(
            app_hdr,
            text="Auto-detect app context",
            variable=self.auto_ctx_var,
            command=self._on_auto_ctx_changed,
            font=ctk.CTkFont(size=11, weight="bold"),
            progress_color=ACCENT_PRIMARY
        )
        auto_switch.pack(side="right")

        ctk.CTkLabel(app_card, text="OwnVoice can automatically adjust its writing style based on the app you're using.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 12))

        apps_table = [
            ("💬", "WhatsApp", ["More casual", "Casual", "Natural"], "More casual", '"Sure, I\'ll be there!"'),
            ("✉️", "Email", ["Professional", "Formal", "Natural"], "Professional", '"Please find the details below."'),
            ("📄", "Documents", ["Natural", "Formal", "Casual"], "Natural", '"Here\'s a quick summary..."'),
            ("💻", "Code", ["Concise", "Technical", "Natural"], "Concise", '"fix: update auth flow"'),
            ("👥", "Slack Teams", ["Natural", "Casual", "Concise"], "Natural", '"Noted! I\'ll take a look."'),
            ("🌐", "Browser / Search", ["Concise", "Natural"], "Concise", '"best laptops under 50k"'),
            ("🔲", "Others", ["Use default style", "Natural", "Formal"], "Use default style", "Follows your default style")
        ]

        for icon, app_name, options, def_choice, sample in apps_table:
            row = ctk.CTkFrame(app_card, fg_color="transparent", height=38)
            row.pack(fill="x", padx=16, pady=3)
            row.grid_columnconfigure(0, weight=2)
            row.grid_columnconfigure(1, weight=2)
            row.grid_columnconfigure(2, weight=3)

            left_label = ctk.CTkFrame(row, fg_color="transparent")
            left_label.grid(row=0, column=0, sticky="w")
            ctk.CTkLabel(left_label, text=f"{icon}  {app_name}", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")

            opt = ctk.CTkOptionMenu(
                row,
                values=options,
                width=135,
                height=28,
                fg_color="#0D121D",
                button_color="#1E293B",
                button_hover_color=BG_CARD_HOVER,
                dropdown_fg_color=BG_CARD,
                font=ctk.CTkFont(size=11)
            )
            opt.set(def_choice)
            opt.grid(row=0, column=1, sticky="w", padx=8)

            sample_lbl = ctk.CTkLabel(row, text=sample, font=ctk.CTkFont(size=11), text_color=TEXT_MUTED)
            sample_lbl.grid(row=0, column=2, sticky="w", padx=8)

        ctk.CTkLabel(app_card, text="").pack(pady=4)

        # Right Column: Live Preview Panel
        right = ctk.CTkFrame(split, fg_color="transparent")
        right.grid(row=0, column=1, sticky="nsew")

        preview_card = ctk.CTkFrame(right, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        preview_card.pack(fill="x", pady=(0, 14))

        ctk.CTkLabel(preview_card, text="Live preview", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=16, pady=(14, 2))
        ctk.CTkLabel(preview_card, text="See how OwnVoice will format your speech.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 10))

        # Preview pills
        pills_row = ctk.CTkFrame(preview_card, fg_color="transparent")
        pills_row.pack(fill="x", padx=16, pady=(0, 12))

        self.preview_btns = {}
        for p in ["Casual", "Natural", "Formal", "Concise"]:
            is_active = (p == self.active_preview_style)
            pb = ctk.CTkButton(
                pills_row,
                text=p,
                width=65,
                height=26,
                corner_radius=13,
                font=ctk.CTkFont(size=11, weight="bold"),
                fg_color=ACCENT_PRIMARY if is_active else "#0D121D",
                hover_color=ACCENT_PRIMARY_HOVER if is_active else BG_CARD_HOVER,
                text_color=TEXT_PRIMARY if is_active else TEXT_MUTED,
                command=lambda s=p: self._set_preview_style(s)
            )
            pb.pack(side="left", padx=(0, 6))
            self.preview_btns[p] = pb

        # Input Bubble
        in_bubble = ctk.CTkFrame(preview_card, fg_color="#0D121D", corner_radius=8, border_color=BORDER_CARD, border_width=1)
        in_bubble.pack(fill="x", padx=16, pady=(0, 8))
        ctk.CTkLabel(
            in_bubble,
            text='Hey, can you share the meeting notes with the team and also remind them about the deadline next week?',
            font=ctk.CTkFont(size=11),
            text_color=TEXT_SECONDARY,
            wraplength=250,
            justify="left"
        ).pack(padx=12, pady=10)

        # Down arrow
        ctk.CTkLabel(preview_card, text="↓", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_MUTED).pack(pady=(0, 8))

        # Output Card with Copy
        out_bubble = ctk.CTkFrame(preview_card, fg_color="#1E2038", corner_radius=8, border_color="#3B3D6A", border_width=1)
        out_bubble.pack(fill="x", padx=16, pady=(0, 8))

        out_top = ctk.CTkFrame(out_bubble, fg_color="transparent")
        out_top.pack(fill="x", padx=10, pady=(8, 2))

        self.output_text_lbl = ctk.CTkLabel(
            out_bubble,
            text=self.preview_styles["Natural"],
            font=ctk.CTkFont(size=11, weight="bold"),
            text_color=TEXT_PRIMARY,
            wraplength=220,
            justify="left"
        )
        self.output_text_lbl.pack(anchor="w", padx=12, pady=(0, 10))

        copy_btn = ctk.CTkButton(
            out_top,
            text="📋",
            width=26,
            height=26,
            font=ctk.CTkFont(size=11),
            fg_color="transparent",
            hover_color=BG_CARD_HOVER,
            text_color=ACCENT_SKY,
            command=self._copy_preview
        )
        copy_btn.pack(side="right")

        # Status check
        ctk.CTkLabel(
            preview_card,
            text="🟢 Clean, natural and ready to use.",
            font=ctk.CTkFont(size=11, weight="bold"),
            text_color=ACCENT_EMERALD
        ).pack(anchor="w", padx=16, pady=(0, 14))

        # Pro Tip Card
        pro_card = ctk.CTkFrame(right, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        pro_card.pack(fill="x", pady=(0, 10))
        p_top = ctk.CTkFrame(pro_card, fg_color="transparent")
        p_top.pack(fill="x", padx=14, pady=(12, 2))
        ctk.CTkLabel(p_top, text="💡 Pro tip", font=ctk.CTkFont(size=12, weight="bold"), text_color=ACCENT_AMBER).pack(side="left")
        ctk.CTkLabel(p_top, text="›", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_MUTED).pack(side="right")
        ctk.CTkLabel(
            pro_card,
            text="You can always override the style on the fly. Try saying 'write this formally' or 'make this shorter' while dictating.",
            font=ctk.CTkFont(size=10),
            text_color=TEXT_MUTED,
            wraplength=260,
            justify="left"
        ).pack(anchor="w", padx=14, pady=(0, 12))

        # Value Card: Make it truly yours
        val_card = ctk.CTkFrame(right, fg_color="#1E2038", corner_radius=10, border_color="#3B3D6A", border_width=1)
        val_card.pack(fill="x")
        v_top = ctk.CTkFrame(val_card, fg_color="transparent")
        v_top.pack(fill="x", padx=14, pady=(12, 2))
        ctk.CTkLabel(v_top, text="✨ Make it truly yours", font=ctk.CTkFont(size=12, weight="bold"), text_color="#A5B4FC").pack(side="left")
        ctk.CTkLabel(v_top, text="›", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_MUTED).pack(side="right")
        ctk.CTkLabel(
            val_card,
            text="Combine vocabulary, snippets and style to get the most accurate results.",
            font=ctk.CTkFont(size=10),
            text_color=TEXT_MUTED,
            wraplength=260,
            justify="left"
        ).pack(anchor="w", padx=14, pady=(0, 12))

    def _set_preview_style(self, style_name):
        self.active_preview_style = style_name
        for s, btn in self.preview_btns.items():
            is_active = (s == style_name)
            btn.configure(
                fg_color=ACCENT_PRIMARY if is_active else "#0D121D",
                hover_color=ACCENT_PRIMARY_HOVER if is_active else BG_CARD_HOVER,
                text_color=TEXT_PRIMARY if is_active else TEXT_MUTED
            )
        self.output_text_lbl.configure(text=self.preview_styles.get(style_name, ""))

    def _copy_preview(self):
        text = self.output_text_lbl.cget("text")
        self.clipboard_clear()
        self.clipboard_append(text)

    def _select_mode(self, mode_key):
        self.config.set("dictation_mode", mode_key)
        for k, c in self.style_cards.items():
            is_active = (k == mode_key)
            c.configure(
                fg_color="#1E2038" if is_active else "#0D121D",
                border_color="#4F46E5" if is_active else BORDER_CARD
            )
            # Toggle checkmark widget
            if hasattr(c, "_check_widget") and c._check_widget:
                c._check_widget.destroy()
                c._check_widget = None
            if is_active:
                c_top = c.winfo_children()[0]
                chk = ctk.CTkLabel(c_top, text="✓", font=ctk.CTkFont(size=11, weight="bold"), text_color="#FFFFFF", fg_color=ACCENT_PRIMARY, corner_radius=10, width=20, height=20)
                chk.pack(side="right")
                c._check_widget = chk

    def _on_auto_ctx_changed(self):
        self.config.set("auto_context", self.auto_ctx_var.get())
