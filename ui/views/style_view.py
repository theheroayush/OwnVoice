"""
OwnVoice Desktop Hub — Style View
Matches modern tone selector, self-correction controls, and custom prompt instructions.
"""
import customtkinter as ctk
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_PRIMARY_HOVER, ACCENT_SKY,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED
)

class StyleView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(0, weight=1)

        self.scroll = ctk.CTkScrollableFrame(self, fg_color=BG_APP, corner_radius=0)
        self.scroll.grid(row=0, column=0, sticky="nsew", padx=24, pady=16)
        self.scroll.grid_columnconfigure(0, weight=1)

        self._build_header()
        self._build_modes_grid()
        self._build_toggles()
        self._build_custom_prompt()

    def _build_header(self):
        ctk.CTkLabel(self.scroll, text="Style & Intelligence", font=ctk.CTkFont(family="Segoe UI", size=22, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(self.scroll, text="Configure how Gemini shapes your voice into perfectly formatted text.", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(anchor="w", pady=(2, 16))

    def _build_modes_grid(self):
        modes_header = ctk.CTkFrame(self.scroll, fg_color="transparent")
        modes_header.pack(fill="x", pady=(0, 8))
        ctk.CTkLabel(modes_header, text="Default Fallback Tone", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")

        current_mode = self.config.get("dictation_mode", "smart_flow")

        tones = [
            ("smart_flow", "✨ Smart Flow", "Natural punctuation, removes stutters & 'um/uh' filler."),
            ("formal_email", "✉️ Formal Email", "Executive business tone with polite salutations & sign-offs."),
            ("code", "💻 Code & Terminal", "Transforms speech into camelCase, snake_case, and shell commands."),
            ("formal_document", "📑 Formal Document", "Structured paragraphs suitable for reports, briefs, and essays."),
            ("bullet_notes", "📋 Bulleted Notes", "Synthesizes rapid-fire thoughts into clean bullet points."),
            ("search", "🔍 Search Query", "Distills rambling speech into crisp, search-engine ready queries."),
            ("translate_hindi", "🇮🇳 Hindi Translation", "Translates spoken English/Hinglish speech directly into pure Hindi."),
            ("verbatim", "🎙️ Verbatim Raw", "Raw speech-to-text without AI rewrites or punctuation shifts.")
        ]

        grid = ctk.CTkFrame(self.scroll, fg_color="transparent")
        grid.pack(fill="x", pady=(0, 20))
        grid.grid_columnconfigure(0, weight=1)
        grid.grid_columnconfigure(1, weight=1)

        self.tone_cards = {}
        for idx, (mode_key, title, desc) in enumerate(tones):
            row = idx // 2
            col = idx % 2
            is_active = (mode_key == current_mode)

            card = ctk.CTkFrame(grid, fg_color=BG_CARD_HOVER if is_active else BG_CARD, corner_radius=10, border_color=ACCENT_PRIMARY if is_active else BORDER_CARD, border_width=1)
            card.grid(row=row, column=col, sticky="nsew", padx=6, pady=6)
            self.tone_cards[mode_key] = card

            btn = ctk.CTkButton(
                card,
                text=f"{title}\n{desc}",
                font=ctk.CTkFont(size=12, weight="bold"),
                fg_color="transparent",
                hover_color=BG_CARD_HOVER,
                text_color=TEXT_PRIMARY,
                anchor="w",
                command=lambda k=mode_key: self._select_mode(k)
            )
            btn.pack(fill="both", expand=True, padx=12, pady=10)

    def _select_mode(self, mode_key):
        self.config.set("dictation_mode", mode_key)
        for k, card in self.tone_cards.items():
            is_active = (k == mode_key)
            card.configure(
                fg_color=BG_CARD_HOVER if is_active else BG_CARD,
                border_color=ACCENT_PRIMARY if is_active else BORDER_CARD
            )

    def _build_toggles(self):
        toggles_card = ctk.CTkFrame(self.scroll, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        toggles_card.pack(fill="x", pady=(0, 20))

        # Auto-context toggle
        self.auto_ctx_var = ctk.BooleanVar(value=self.config.get("auto_context", True))
        cb1 = ctk.CTkCheckBox(
            toggles_card,
            text="App-Aware Tone Detection (Auto-adapts for VS Code, Chrome, Outlook, Word, Slack)",
            variable=self.auto_ctx_var,
            command=self._on_auto_ctx_changed,
            font=ctk.CTkFont(size=12, weight="bold"),
            text_color=TEXT_PRIMARY
        )
        cb1.pack(anchor="w", padx=16, pady=(14, 6))
        ctk.CTkLabel(
            toggles_card,
            text="When focused in an IDE, code mode is auto-applied. When typing in an email client, formal tone is activated.",
            font=ctk.CTkFont(size=11),
            text_color=TEXT_MUTED
        ).pack(anchor="w", padx=44, pady=(0, 12))

        # Self-correction toggle
        self.self_corr_var = ctk.BooleanVar(value=self.config.get("self_correction", True))
        cb2 = ctk.CTkCheckBox(
            toggles_card,
            text="Spoken Mid-Sentence Self-Correction",
            variable=self.self_corr_var,
            command=self._on_self_corr_changed,
            font=ctk.CTkFont(size=12, weight="bold"),
            text_color=TEXT_PRIMARY
        )
        cb2.pack(anchor="w", padx=16, pady=(0, 6))
        ctk.CTkLabel(
            toggles_card,
            text="Saying 'Let's meet at 3 PM, actually make that 4 PM' will cleanly produce 'Let's meet at 4 PM'.",
            font=ctk.CTkFont(size=11),
            text_color=TEXT_MUTED
        ).pack(anchor="w", padx=44, pady=(0, 14))

    def _on_auto_ctx_changed(self):
        self.config.set("auto_context", self.auto_ctx_var.get())

    def _on_self_corr_changed(self):
        self.config.set("self_correction", self.self_corr_var.get())

    def _build_custom_prompt(self):
        prompt_card = ctk.CTkFrame(self.scroll, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        prompt_card.pack(fill="x", pady=(0, 16))

        ctk.CTkLabel(prompt_card, text="Custom AI Persona & Prompt Instructions", font=ctk.CTkFont(size=13, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=16, pady=(14, 4))
        ctk.CTkLabel(prompt_card, text="Instruct Gemini on specific vocabulary rules, tone guidelines, or formatting preferences.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 10))

        self.prompt_text = ctk.CTkTextbox(
            prompt_card,
            height=90,
            fg_color="#0D121D",
            border_color=BORDER_CARD,
            border_width=1,
            corner_radius=8,
            font=ctk.CTkFont(size=12),
            wrap="word"
        )
        self.prompt_text.pack(fill="x", padx=16, pady=(0, 10))
        self.prompt_text.insert("1.0", self.config.get("custom_instructions", ""))

        btn_row = ctk.CTkFrame(prompt_card, fg_color="transparent")
        btn_row.pack(fill="x", padx=16, pady=(0, 14))

        self.save_prompt_btn = ctk.CTkButton(
            btn_row,
            text="💾 Save Instructions",
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color=ACCENT_PRIMARY,
            hover_color=ACCENT_PRIMARY_HOVER,
            command=self._save_instructions
        )
        self.save_prompt_btn.pack(side="left")

    def _save_instructions(self):
        txt = self.prompt_text.get("1.0", "end").strip()
        self.config.set("custom_instructions", txt)
        self.save_prompt_btn.configure(text="✓ Saved!", fg_color="#059669")
        self.after(1500, lambda: self.save_prompt_btn.configure(text="💾 Save Instructions", fg_color=ACCENT_PRIMARY))
