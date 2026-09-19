"""
OwnVoice Desktop Hub — Snippets View
Matches media_1789815310304.png with category pills, snippet list cards, and Edit/New snippet drawer.
"""
import customtkinter as ctk
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_PRIMARY_HOVER, ACCENT_SKY, ACCENT_CRIMSON,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED, TEXT_SUBTLE,
    get_badge_colors
)

class SnippetsView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager, snippet_engine=None):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager
        self.snippet_engine = snippet_engine
        self.active_category_filter = "All"
        self.search_query = ""
        self.active_trigger = None

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(2, weight=1)

        self._build_header()
        self._build_filter_pills()
        self._build_split_view()
        self._load_snippets()

    def _build_header(self):
        header = ctk.CTkFrame(self, fg_color="transparent")
        header.grid(row=0, column=0, sticky="ew", padx=24, pady=(16, 8))
        header.grid_columnconfigure(0, weight=1)
        header.grid_columnconfigure(1, weight=1)

        left = ctk.CTkFrame(header, fg_color="transparent")
        left.grid(row=0, column=0, sticky="w")
        ctk.CTkLabel(left, text="Snippets", font=ctk.CTkFont(family="Segoe UI", size=22, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(left, text="Create voice triggers to insert long text, templates, and links instantly.", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(anchor="w")

        # Search Bar
        search_box = ctk.CTkFrame(header, fg_color=BG_CARD, corner_radius=8, border_color=BORDER_CARD, border_width=1)
        search_box.grid(row=0, column=1, sticky="e")

        ctk.CTkLabel(search_box, text="🔍", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(side="left", padx=(8, 4))
        self.search_entry = ctk.CTkEntry(
            search_box,
            placeholder_text="Search snippets...",
            width=200,
            fg_color="transparent",
            border_width=0,
            text_color=TEXT_PRIMARY
        )
        self.search_entry.pack(side="left", padx=(0, 6), pady=4)
        self.search_entry.bind("<KeyRelease>", self._on_search_changed)

    def _build_filter_pills(self):
        pills_bar = ctk.CTkFrame(self, fg_color="transparent")
        pills_bar.grid(row=1, column=0, sticky="ew", padx=24, pady=(0, 12))

        categories = ["All", "Link", "Contact", "Email", "Text", "Meeting", "Code"]
        self.pill_buttons = {}

        for cat in categories:
            is_active = (cat == self.active_category_filter)
            btn = ctk.CTkButton(
                pills_bar,
                text=cat,
                width=60,
                height=28,
                corner_radius=14,
                font=ctk.CTkFont(size=11, weight="bold"),
                fg_color=ACCENT_PRIMARY if is_active else BG_CARD,
                hover_color=ACCENT_PRIMARY_HOVER if is_active else BG_CARD_HOVER,
                border_color=BORDER_CARD if not is_active else ACCENT_PRIMARY,
                border_width=1,
                text_color=TEXT_PRIMARY if is_active else TEXT_MUTED,
                command=lambda c=cat: self._set_filter(c)
            )
            btn.pack(side="left", padx=(0, 6))
            self.pill_buttons[cat] = btn

    def _set_filter(self, cat):
        self.active_category_filter = cat
        for c, btn in self.pill_buttons.items():
            is_active = (c == cat)
            btn.configure(
                fg_color=ACCENT_PRIMARY if is_active else BG_CARD,
                hover_color=ACCENT_PRIMARY_HOVER if is_active else BG_CARD_HOVER,
                border_color=BORDER_CARD if not is_active else ACCENT_PRIMARY,
                text_color=TEXT_PRIMARY if is_active else TEXT_MUTED
            )
        self._load_snippets()

    def _on_search_changed(self, event=None):
        self.search_query = self.search_entry.get().strip().lower()
        self._load_snippets()

    def _build_split_view(self):
        split = ctk.CTkFrame(self, fg_color="transparent")
        split.grid(row=2, column=0, sticky="nsew", padx=24, pady=(0, 16))
        split.grid_columnconfigure(0, weight=3)
        split.grid_columnconfigure(1, weight=2)
        split.grid_rowconfigure(0, weight=1)

        # Left: Scrollable Snippets Cards
        self.snippets_scroll = ctk.CTkScrollableFrame(split, fg_color="transparent", corner_radius=0)
        self.snippets_scroll.grid(row=0, column=0, sticky="nsew", padx=(0, 12))
        self.snippets_scroll.grid_columnconfigure(0, weight=1)

        # Right: Edit / New Snippet Drawer
        self.drawer = ctk.CTkFrame(split, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        self.drawer.grid(row=0, column=1, sticky="nsew")
        self.drawer.grid_columnconfigure(0, weight=1)

        drawer_top = ctk.CTkFrame(self.drawer, fg_color="transparent")
        drawer_top.pack(fill="x", padx=16, pady=(14, 2))
        
        self.drawer_title = ctk.CTkLabel(drawer_top, text="Edit snippet", font=ctk.CTkFont(size=16, weight="bold"), text_color=TEXT_PRIMARY)
        self.drawer_title.pack(side="left")

        new_btn = ctk.CTkButton(
            drawer_top,
            text="➕ New",
            width=60,
            height=26,
            font=ctk.CTkFont(size=11, weight="bold"),
            fg_color="#1E293B",
            hover_color=BG_CARD_HOVER,
            command=self._clear_form
        )
        new_btn.pack(side="right")

        ctk.CTkLabel(self.drawer, text="OwnVoice expands this phrase automatically as you speak.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 12))

        # Field 1: Spoken Trigger
        ctk.CTkLabel(self.drawer, text="Spoken Trigger Phrase *", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_SECONDARY).pack(anchor="w", padx=16, pady=(0, 4))
        self.trigger_input = ctk.CTkEntry(self.drawer, placeholder_text="e.g. 'my meeting' or 'portfolio link'", height=34, fg_color="#0D121D", border_color=BORDER_CARD)
        self.trigger_input.pack(fill="x", padx=16, pady=(0, 10))

        # Field 2: Snippet Title
        ctk.CTkLabel(self.drawer, text="Title / Label", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_SECONDARY).pack(anchor="w", padx=16, pady=(0, 4))
        self.title_input = ctk.CTkEntry(self.drawer, placeholder_text="e.g. Meeting Link", height=34, fg_color="#0D121D", border_color=BORDER_CARD)
        self.title_input.pack(fill="x", padx=16, pady=(0, 10))

        # Field 3: Category
        ctk.CTkLabel(self.drawer, text="Category", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_SECONDARY).pack(anchor="w", padx=16, pady=(0, 4))
        self.category_select = ctk.CTkOptionMenu(
            self.drawer,
            values=["Text", "Link", "Contact", "Email", "Meeting", "Code"],
            height=34,
            fg_color="#0D121D",
            button_color="#1E293B",
            button_hover_color=BG_CARD_HOVER,
            dropdown_fg_color=BG_CARD
        )
        self.category_select.pack(fill="x", padx=16, pady=(0, 10))

        # Field 4: Expansion Text
        ctk.CTkLabel(self.drawer, text="Expansion Text *", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_SECONDARY).pack(anchor="w", padx=16, pady=(0, 4))
        self.expansion_text = ctk.CTkTextbox(
            self.drawer,
            height=90,
            fg_color="#0D121D",
            border_color=BORDER_CARD,
            border_width=1,
            corner_radius=8,
            font=ctk.CTkFont(size=12),
            wrap="word"
        )
        self.expansion_text.pack(fill="x", padx=16, pady=(0, 10))

        # Switch: Auto-format
        self.auto_format_var = ctk.BooleanVar(value=True)
        self.auto_format_sw = ctk.CTkSwitch(
            self.drawer,
            text="Auto-format expansion in sentence context",
            variable=self.auto_format_var,
            font=ctk.CTkFont(size=11),
            text_color=TEXT_SECONDARY
        )
        self.auto_format_sw.pack(anchor="w", padx=16, pady=(0, 14))

        # Drawer Footer Actions
        drawer_actions = ctk.CTkFrame(self.drawer, fg_color="transparent")
        drawer_actions.pack(fill="x", padx=16, pady=(0, 14))

        self.save_btn = ctk.CTkButton(
            drawer_actions,
            text="💾 Save Snippet",
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color=ACCENT_PRIMARY,
            hover_color=ACCENT_PRIMARY_HOVER,
            height=34,
            command=self._save_snippet
        )
        self.save_btn.pack(side="left", fill="x", expand=True, padx=(0, 8))

        self.delete_btn = ctk.CTkButton(
            drawer_actions,
            text="🗑️ Delete",
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="#7F1D1D",
            hover_color=ACCENT_CRIMSON,
            height=34,
            width=75,
            command=self._delete_active_snippet
        )
        self.delete_btn.pack(side="right")

    def _clear_form(self):
        self.active_trigger = None
        self.drawer_title.configure(text="New snippet")
        self.trigger_input.delete(0, "end")
        self.title_input.delete(0, "end")
        self.category_select.set("Text")
        self.expansion_text.delete("1.0", "end")
        self.auto_format_var.set(True)

    def _select_snippet(self, trig, data):
        self.active_trigger = trig
        self.drawer_title.configure(text="Edit snippet")
        self.trigger_input.delete(0, "end")
        self.trigger_input.insert(0, trig)
        self.title_input.delete(0, "end")
        self.title_input.insert(0, data.get("title", trig.title()))
        self.category_select.set(data.get("category", "Text"))
        self.expansion_text.delete("1.0", "end")
        self.expansion_text.insert("1.0", data.get("expansion", ""))
        self.auto_format_var.set(data.get("auto_format", True))

        # Update card borders
        for k, card in self._card_widgets.items():
            if k == trig:
                card.configure(border_color=ACCENT_PRIMARY, fg_color=BG_CARD_HOVER)
            else:
                card.configure(border_color=BORDER_CARD, fg_color=BG_CARD)

    def _load_snippets(self):
        for w in self.snippets_scroll.winfo_children():
            w.destroy()

        snippets = self.snippet_engine.get_structured_snippets() if self.snippet_engine else {}
        self._card_widgets = {}

        # Filter by category pill
        if self.active_category_filter != "All":
            snippets = {
                k: v for k, v in snippets.items()
                if v.get("category", "").lower() == self.active_category_filter.lower()
            }

        # Filter by search
        if self.search_query:
            snippets = {
                k: v for k, v in snippets.items()
                if self.search_query in k.lower() or
                   self.search_query in v.get("title", "").lower() or
                   self.search_query in v.get("expansion", "").lower()
            }

        if not snippets:
            empty = ctk.CTkLabel(self.snippets_scroll, text="No snippets match this filter. Create one on the right!", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED)
            empty.pack(pady=30)
            return

        for trig, data in snippets.items():
            cat = data.get("category", "Text")
            badge_meta = get_badge_colors(cat)

            card = ctk.CTkFrame(self.snippets_scroll, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
            card.pack(fill="x", pady=4)
            self._card_widgets[trig] = card

            # Card Header: Badge + Title
            top = ctk.CTkFrame(card, fg_color="transparent")
            top.pack(fill="x", padx=12, pady=(10, 4))

            badge = ctk.CTkLabel(
                top,
                text=cat,
                font=ctk.CTkFont(size=10, weight="bold"),
                fg_color=badge_meta["bg"],
                text_color=badge_meta["fg"],
                corner_radius=4,
                padx=6,
                pady=2
            )
            badge.pack(side="left")

            title_txt = data.get("title", trig.title())
            ctk.CTkLabel(top, text=f"  {title_txt}", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")

            # Spoken Trigger row
            trig_lbl = ctk.CTkLabel(
                card,
                text=f'🗣️ Say: "{trig}"',
                font=ctk.CTkFont(size=12, weight="bold"),
                text_color=ACCENT_SKY
            )
            trig_lbl.pack(anchor="w", padx=12, pady=(2, 2))

            # Expansion text preview
            exp = data.get("expansion", "")
            exp_preview = (exp[:90] + "...") if len(exp) > 90 else exp
            exp_lbl = ctk.CTkLabel(
                card,
                text=f"↳  {exp_preview}",
                font=ctk.CTkFont(size=11),
                text_color=TEXT_MUTED,
                wraplength=380,
                justify="left"
            )
            exp_lbl.pack(anchor="w", padx=12, pady=(0, 10))

            # Bind selection click
            for w in [card, top, badge, trig_lbl, exp_lbl]:
                w.bind("<Button-1>", lambda e, t=trig, d=data: self._select_snippet(t, d))

        # Default select the first snippet if none selected
        if self.active_trigger is None and snippets:
            first_trig = list(snippets.keys())[0]
            self._select_snippet(first_trig, snippets[first_trig])

    def _save_snippet(self):
        trig = self.trigger_input.get().strip()
        title = self.title_input.get().strip()
        cat = self.category_select.get()
        exp = self.expansion_text.get("1.0", "end").strip()
        auto_fmt = self.auto_format_var.get()

        if trig and exp and self.snippet_engine:
            # If trigger was renamed, delete old trigger
            if self.active_trigger and self.active_trigger.lower() != trig.lower():
                self.snippet_engine.delete_snippet(self.active_trigger)
            self.snippet_engine.save_structured_snippet(
                trigger=trig,
                expansion=exp,
                category=cat,
                auto_format=auto_fmt,
                title=title
            )
            self.active_trigger = trig
            self._load_snippets()

    def _delete_active_snippet(self):
        if self.active_trigger and self.snippet_engine:
            self.snippet_engine.delete_snippet(self.active_trigger)
            self._clear_form()
            self._load_snippets()
