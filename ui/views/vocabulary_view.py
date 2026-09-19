"""
OwnVoice Desktop Hub — Vocabulary View
Matches media_1789815301774.png with color-coded badges, term table, and "Add a word" side drawer with live preview.
"""
import customtkinter as ctk
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_PRIMARY_HOVER, ACCENT_CRIMSON,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED, TEXT_SUBTLE,
    get_badge_colors
)

class VocabularyView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager
        self.active_category_filter = "All categories"
        self.search_query = ""

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(1, weight=1)

        self._build_header()
        self._build_main_split()
        self._load_terms()

    def _build_header(self):
        header = ctk.CTkFrame(self, fg_color="transparent")
        header.grid(row=0, column=0, sticky="ew", padx=24, pady=(16, 12))
        header.grid_columnconfigure(0, weight=1)
        header.grid_columnconfigure(1, weight=1)

        left = ctk.CTkFrame(header, fg_color="transparent")
        left.grid(row=0, column=0, sticky="w")
        ctk.CTkLabel(left, text="Vocabulary", font=ctk.CTkFont(family="Segoe UI", size=22, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(left, text="Teach Gemini how to spell names, jargon, and company terms correctly.", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(anchor="w")

        # Right Controls: Filter + Search
        right = ctk.CTkFrame(header, fg_color="transparent")
        right.grid(row=0, column=1, sticky="e")

        self.cat_filter_menu = ctk.CTkOptionMenu(
            right,
            values=["All categories", "Name", "Place", "Technology", "Company", "Jargon"],
            command=self._on_cat_filter_changed,
            width=140,
            height=32,
            fg_color=BG_CARD,
            button_color="#1E293B",
            button_hover_color=BG_CARD_HOVER,
            dropdown_fg_color=BG_CARD
        )
        self.cat_filter_menu.pack(side="left", padx=(0, 8))

        search_box = ctk.CTkFrame(right, fg_color=BG_CARD, corner_radius=8, border_color=BORDER_CARD, border_width=1)
        search_box.pack(side="left")
        ctk.CTkLabel(search_box, text="🔍", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(side="left", padx=(8, 4))
        self.search_entry = ctk.CTkEntry(
            search_box,
            placeholder_text="Search words...",
            width=180,
            fg_color="transparent",
            border_width=0,
            text_color=TEXT_PRIMARY
        )
        self.search_entry.pack(side="left", padx=(0, 6), pady=4)
        self.search_entry.bind("<KeyRelease>", self._on_search_changed)

    def _on_cat_filter_changed(self, choice):
        self.active_category_filter = choice
        self._load_terms()

    def _on_search_changed(self, event=None):
        self.search_query = self.search_entry.get().strip().lower()
        self._load_terms()

    def _build_main_split(self):
        split = ctk.CTkFrame(self, fg_color="transparent")
        split.grid(row=1, column=0, sticky="nsew", padx=24, pady=(0, 16))
        split.grid_columnconfigure(0, weight=3)
        split.grid_columnconfigure(1, weight=2)
        split.grid_rowconfigure(0, weight=1)

        # Left Column: Words Table
        table_container = ctk.CTkFrame(split, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        table_container.grid(row=0, column=0, sticky="nsew", padx=(0, 12))
        table_container.grid_columnconfigure(0, weight=1)
        table_container.grid_rowconfigure(1, weight=1)

        # Table Header Bar
        table_hdr = ctk.CTkFrame(table_container, fg_color="#182133", corner_radius=0, height=36)
        table_hdr.grid(row=0, column=0, sticky="ew")
        table_hdr.grid_columnconfigure(0, weight=3)
        table_hdr.grid_columnconfigure(1, weight=2)
        table_hdr.grid_columnconfigure(2, weight=2)
        table_hdr.grid_columnconfigure(3, weight=1)

        ctk.CTkLabel(table_hdr, text="WORD / PHRASE", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_MUTED).grid(row=0, column=0, sticky="w", padx=16, pady=8)
        ctk.CTkLabel(table_hdr, text="CATEGORY", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_MUTED).grid(row=0, column=1, sticky="w", padx=8, pady=8)
        ctk.CTkLabel(table_hdr, text="REPLACEMENT", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_MUTED).grid(row=0, column=2, sticky="w", padx=8, pady=8)
        ctk.CTkLabel(table_hdr, text="", font=ctk.CTkFont(size=11, weight="bold")).grid(row=0, column=3, sticky="e", padx=16, pady=8)

        # Table Scrollable Body
        self.words_scroll = ctk.CTkScrollableFrame(table_container, fg_color="transparent", corner_radius=0)
        self.words_scroll.grid(row=1, column=0, sticky="nsew")
        self.words_scroll.grid_columnconfigure(0, weight=1)

        # Right Column: "Add a Word" Drawer
        self.drawer = ctk.CTkFrame(split, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        self.drawer.grid(row=0, column=1, sticky="nsew")
        self.drawer.grid_columnconfigure(0, weight=1)

        ctk.CTkLabel(self.drawer, text="Add a word", font=ctk.CTkFont(size=16, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=16, pady=(16, 2))
        ctk.CTkLabel(self.drawer, text="Gemini will prioritize this exact spelling.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 14))

        # Field 1: Word or phrase
        ctk.CTkLabel(self.drawer, text="Word or phrase *", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_SECONDARY).pack(anchor="w", padx=16, pady=(0, 4))
        self.word_input = ctk.CTkEntry(self.drawer, placeholder_text="e.g. Aarav, Bengaluru, Chanakya", height=34, fg_color="#0D121D", border_color=BORDER_CARD)
        self.word_input.pack(fill="x", padx=16, pady=(0, 12))
        self.word_input.bind("<KeyRelease>", self._update_preview)

        # Field 2: Category
        ctk.CTkLabel(self.drawer, text="Category", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_SECONDARY).pack(anchor="w", padx=16, pady=(0, 4))
        self.category_select = ctk.CTkOptionMenu(
            self.drawer,
            values=["Jargon", "Name", "Place", "Technology", "Company"],
            command=lambda v: self._update_preview(),
            height=34,
            fg_color="#0D121D",
            button_color="#1E293B",
            button_hover_color=BG_CARD_HOVER,
            dropdown_fg_color=BG_CARD
        )
        self.category_select.pack(fill="x", padx=16, pady=(0, 12))

        # Field 3: Sounds like / Replacement
        ctk.CTkLabel(self.drawer, text="Replacement / Sounds like", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_SECONDARY).pack(anchor="w", padx=16, pady=(0, 4))
        self.replacement_input = ctk.CTkEntry(self.drawer, placeholder_text="Optional phonetic or expansion", height=34, fg_color="#0D121D", border_color=BORDER_CARD)
        self.replacement_input.pack(fill="x", padx=16, pady=(0, 14))
        self.replacement_input.bind("<KeyRelease>", self._update_preview)

        # Live Preview Box
        preview_card = ctk.CTkFrame(self.drawer, fg_color="#0D121D", corner_radius=8, border_color=BORDER_CARD, border_width=1)
        preview_card.pack(fill="x", padx=16, pady=(0, 16))
        
        ctk.CTkLabel(preview_card, text="LIVE PREVIEW", font=ctk.CTkFont(size=10, weight="bold"), text_color=TEXT_MUTED).pack(anchor="w", padx=12, pady=(8, 2))
        self.preview_lbl = ctk.CTkLabel(
            preview_card,
            text='Saying "Aarav will meet in Bengaluru" ->\n"Aarav will meet in Bengaluru"',
            font=ctk.CTkFont(size=11),
            text_color=TEXT_SECONDARY,
            wraplength=260,
            justify="left"
        )
        self.preview_lbl.pack(anchor="w", padx=12, pady=(0, 10))

        # Action Buttons
        btn_row = ctk.CTkFrame(self.drawer, fg_color="transparent")
        btn_row.pack(fill="x", padx=16, pady=(0, 16))

        save_btn = ctk.CTkButton(
            btn_row,
            text="➕ Save Word",
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color=ACCENT_PRIMARY,
            hover_color=ACCENT_PRIMARY_HOVER,
            height=34,
            command=self._save_word
        )
        save_btn.pack(side="left", fill="x", expand=True, padx=(0, 8))

        clear_btn = ctk.CTkButton(
            btn_row,
            text="Clear",
            font=ctk.CTkFont(size=12),
            fg_color="#1E293B",
            hover_color=BG_CARD_HOVER,
            height=34,
            width=70,
            command=self._clear_inputs
        )
        clear_btn.pack(side="right")

    def _update_preview(self, event=None):
        w = self.word_input.get().strip() or "Word"
        repl = self.replacement_input.get().strip() or w
        self.preview_lbl.configure(text=f'When you say "{w}" in a sentence:\n→ Transcribed accurately as "{repl}"')

    def _clear_inputs(self):
        self.word_input.delete(0, "end")
        self.replacement_input.delete(0, "end")
        self._update_preview()

    def _save_word(self):
        term = self.word_input.get().strip()
        cat = self.category_select.get()
        repl = self.replacement_input.get().strip()
        if term:
            if hasattr(self.config, "add_vocabulary_term"):
                self.config.add_vocabulary_term(term, term_type=cat, replacement=repl)
            self._clear_inputs()
            self._load_terms()

    def _load_terms(self):
        for w in self.words_scroll.winfo_children():
            w.destroy()

        if hasattr(self.config, "get_structured_vocabulary"):
            vocab = self.config.get_structured_vocabulary()
        elif hasattr(self.config, "get_vocabulary"):
            vocab = [{"term": t, "type": "Jargon", "replacement": t} for t in self.config.get_vocabulary()]
        else:
            vocab = []

        # Apply Category Filter
        if self.active_category_filter != "All categories":
            vocab = [v for v in vocab if v.get("type", "").lower() == self.active_category_filter.lower()]

        # Apply Search Query
        if self.search_query:
            vocab = [v for v in vocab if self.search_query in v.get("term", "").lower()]

        if not vocab:
            empty = ctk.CTkLabel(self.words_scroll, text="No vocabulary terms found. Add one on the right!", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED)
            empty.pack(pady=30)
            return

        for idx, item in enumerate(vocab):
            row = ctk.CTkFrame(self.words_scroll, fg_color="transparent" if idx % 2 == 0 else "#0D121D", height=38, corner_radius=6)
            row.pack(fill="x", pady=1)
            row.grid_columnconfigure(0, weight=3)
            row.grid_columnconfigure(1, weight=2)
            row.grid_columnconfigure(2, weight=2)
            row.grid_columnconfigure(3, weight=1)

            term = item.get("term", "")
            cat = item.get("type", "Jargon")
            repl = item.get("replacement", term)
            badge_meta = get_badge_colors(cat)

            # Term
            ctk.CTkLabel(row, text=term, font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_PRIMARY).grid(row=0, column=0, sticky="w", padx=16, pady=8)

            # Badge
            badge = ctk.CTkLabel(
                row,
                text=cat,
                font=ctk.CTkFont(size=10, weight="bold"),
                fg_color=badge_meta["bg"],
                text_color=badge_meta["fg"],
                corner_radius=4,
                padx=6,
                pady=2
            )
            badge.grid(row=0, column=1, sticky="w", padx=8, pady=8)

            # Replacement
            ctk.CTkLabel(row, text=repl, font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).grid(row=0, column=2, sticky="w", padx=8, pady=8)

            # Delete button
            del_btn = ctk.CTkButton(
                row,
                text="✕",
                width=24,
                height=24,
                font=ctk.CTkFont(size=10, weight="bold"),
                fg_color="transparent",
                hover_color=ACCENT_CRIMSON,
                text_color=TEXT_MUTED,
                command=lambda t=term: self._delete_term(t)
            )
            del_btn.grid(row=0, column=3, sticky="e", padx=16, pady=8)

    def _delete_term(self, term):
        self.config.remove_vocabulary_term(term)
        self._load_terms()
