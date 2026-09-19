"""
OwnVoice Desktop Hub — History View
Matches media_1789815294121.png with search, filter pills, split-view list, and detail inspector card.
"""
import tkinter as tk
import customtkinter as ctk
import datetime
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_PRIMARY_HOVER, ACCENT_SKY, ACCENT_CRIMSON,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED, TEXT_SUBTLE,
    get_badge_colors
)

class HistoryView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager, note_store=None):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager
        self.note_store = note_store

        self.selected_item = None
        self.active_filter = "All"
        self.search_query = ""

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(2, weight=1)

        self._build_header()
        self._build_filter_pills()
        self._build_split_view()
        self._load_items()

    def _build_header(self):
        header = ctk.CTkFrame(self, fg_color="transparent")
        header.grid(row=0, column=0, sticky="ew", padx=24, pady=(16, 8))
        header.grid_columnconfigure(0, weight=1)
        header.grid_columnconfigure(1, weight=1)

        left = ctk.CTkFrame(header, fg_color="transparent")
        left.grid(row=0, column=0, sticky="w")
        ctk.CTkLabel(left, text="History", font=ctk.CTkFont(family="Segoe UI", size=22, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(left, text="Search, filter, and review all past voice dictations.", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(anchor="w")

        # Search Bar on right
        search_box = ctk.CTkFrame(header, fg_color=BG_CARD, corner_radius=8, border_color=BORDER_CARD, border_width=1)
        search_box.grid(row=0, column=1, sticky="e")

        ctk.CTkLabel(search_box, text="🔍", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(side="left", padx=(10, 4))
        self.search_entry = ctk.CTkEntry(
            search_box,
            placeholder_text="Search dictations... (Ctrl+K)",
            width=260,
            fg_color="transparent",
            border_width=0,
            text_color=TEXT_PRIMARY
        )
        self.search_entry.pack(side="left", padx=(0, 6), pady=4)
        self.search_entry.bind("<KeyRelease>", self._on_search_changed)

    def _build_filter_pills(self):
        pills_bar = ctk.CTkFrame(self, fg_color="transparent")
        pills_bar.grid(row=1, column=0, sticky="ew", padx=24, pady=(4, 12))

        categories = ["All", "Notes", "Emails", "Messages", "Code", "Documents", "Others"]
        self.pill_buttons = {}

        for cat in categories:
            is_active = (cat == self.active_filter)
            btn = ctk.CTkButton(
                pills_bar,
                text=cat,
                width=65,
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
        self.active_filter = cat
        for c, btn in self.pill_buttons.items():
            is_active = (c == cat)
            btn.configure(
                fg_color=ACCENT_PRIMARY if is_active else BG_CARD,
                hover_color=ACCENT_PRIMARY_HOVER if is_active else BG_CARD_HOVER,
                border_color=BORDER_CARD if not is_active else ACCENT_PRIMARY,
                text_color=TEXT_PRIMARY if is_active else TEXT_MUTED
            )
        self._load_items()

    def _on_search_changed(self, event=None):
        self.search_query = self.search_entry.get().strip().lower()
        self._load_items()

    def _build_split_view(self):
        split = ctk.CTkFrame(self, fg_color="transparent")
        split.grid(row=2, column=0, sticky="nsew", padx=24, pady=(0, 16))
        split.grid_columnconfigure(0, weight=3)
        split.grid_columnconfigure(1, weight=2)
        split.grid_rowconfigure(0, weight=1)

        # Left Column: Scrollable List of cards
        self.list_scroll = ctk.CTkScrollableFrame(split, fg_color="transparent", corner_radius=0)
        self.list_scroll.grid(row=0, column=0, sticky="nsew", padx=(0, 12))
        self.list_scroll.grid_columnconfigure(0, weight=1)

        # Right Column: Inspector Card / Detail Drawer
        self.detail_card = ctk.CTkFrame(split, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        self.detail_card.grid(row=0, column=1, sticky="nsew")
        self.detail_card.grid_columnconfigure(0, weight=1)
        self.detail_card.grid_rowconfigure(1, weight=1)

        # Inspector Top Header
        self.detail_header = ctk.CTkFrame(self.detail_card, fg_color="transparent")
        self.detail_header.grid(row=0, column=0, sticky="ew", padx=16, pady=(14, 8))

        self.detail_badge = ctk.CTkLabel(
            self.detail_header,
            text="Meeting",
            font=ctk.CTkFont(size=11, weight="bold"),
            fg_color="#312E81",
            text_color="#C7D2FE",
            corner_radius=6,
            padx=8,
            pady=3
        )
        self.detail_badge.pack(side="left")

        self.detail_meta = ctk.CTkLabel(
            self.detail_header,
            text="Today 2:45 PM",
            font=ctk.CTkFont(size=11),
            text_color=TEXT_MUTED
        )
        self.detail_meta.pack(side="right")

        # Inspector Content Box
        self.detail_text = ctk.CTkTextbox(
            self.detail_card,
            fg_color="#0D121D",
            border_color=BORDER_CARD,
            border_width=1,
            corner_radius=8,
            font=ctk.CTkFont(size=12),
            text_color=TEXT_PRIMARY,
            wrap="word"
        )
        self.detail_text.grid(row=1, column=0, sticky="nsew", padx=16, pady=4)

        # Inspector AI Banner
        self.ai_clean_banner = ctk.CTkFrame(self.detail_card, fg_color="#182033", corner_radius=8, border_color="#2D3A54", border_width=1)
        self.ai_clean_banner.grid(row=2, column=0, sticky="ew", padx=16, pady=10)
        
        ctk.CTkLabel(
            self.ai_clean_banner,
            text="✨ AI cleaned this for you",
            font=ctk.CTkFont(size=11, weight="bold"),
            text_color=ACCENT_SKY
        ).pack(anchor="w", padx=12, pady=(6, 2))
        
        ctk.CTkLabel(
            self.ai_clean_banner,
            text="Auto-removed filler words, repeated phrases, and applied smart punctuation.",
            font=ctk.CTkFont(size=10),
            text_color=TEXT_MUTED
        ).pack(anchor="w", padx=12, pady=(0, 6))

        # Inspector Actions Footer
        actions_row = ctk.CTkFrame(self.detail_card, fg_color="transparent")
        actions_row.grid(row=3, column=0, sticky="ew", padx=16, pady=(0, 14))

        self.copy_btn = ctk.CTkButton(
            actions_row,
            text="📋 Copy",
            width=80,
            height=32,
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color=ACCENT_PRIMARY,
            hover_color=ACCENT_PRIMARY_HOVER,
            command=self._copy_selected
        )
        self.copy_btn.pack(side="left", padx=(0, 8))

        self.save_edit_btn = ctk.CTkButton(
            actions_row,
            text="💾 Save Edit",
            width=90,
            height=32,
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="#334155",
            hover_color="#475569",
            command=self._save_inline_edit
        )
        self.save_edit_btn.pack(side="left", padx=(0, 8))

        self.delete_btn = ctk.CTkButton(
            actions_row,
            text="🗑️ Delete",
            width=80,
            height=32,
            font=ctk.CTkFont(size=12, weight="bold"),
            fg_color="#7F1D1D",
            hover_color=ACCENT_CRIMSON,
            command=self._delete_selected
        )
        self.delete_btn.pack(side="right")

    def _infer_category(self, text: str, mode: str) -> str:
        t = (text or "").lower()
        m = (mode or "").lower()
        if "code" in m or "def " in t or "function" in t or "import " in t:
            return "Code"
        elif "email" in m or "dear " in t or "regards" in t or "best," in t:
            return "Email"
        elif "meeting" in t or "sync" in t or "call" in t or "standup" in t:
            return "Meeting"
        elif "note" in m or len(text) > 150:
            return "Notes"
        elif "search" in m:
            return "Search"
        return "Work"

    def _load_items(self):
        for w in self.list_scroll.winfo_children():
            w.destroy()

        history = self.config.load_history()
        # Merge with note_store if available
        all_items = []
        for h in history:
            all_items.append({
                "source": "history",
                "id": h.get("timestamp"),
                "timestamp": h.get("timestamp", ""),
                "text": h.get("text", ""),
                "mode": h.get("mode", "smart_flow"),
                "category": self._infer_category(h.get("text", ""), h.get("mode", ""))
            })

        if self.note_store:
            try:
                notes = self.note_store.get_all_notes(limit=100)
                for n in notes:
                    txt = n.get("structured_content") or n.get("raw_transcript") or ""
                    all_items.append({
                        "source": "note_store",
                        "id": n.get("id"),
                        "timestamp": n.get("created_at", "")[:19],
                        "text": txt,
                        "mode": "notes",
                        "category": "Notes"
                    })
            except Exception:
                pass

        # Filter by category pill
        if self.active_filter != "All":
            all_items = [
                it for it in all_items
                if it["category"].lower() == self.active_filter.lower() or
                   (self.active_filter == "Others" and it["category"] not in ["Notes", "Emails", "Messages", "Code", "Documents"])
            ]

        # Filter by search query
        if self.search_query:
            all_items = [
                it for it in all_items
                if self.search_query in it["text"].lower() or self.search_query in it["category"].lower()
            ]

        if not all_items:
            empty_box = ctk.CTkFrame(self.list_scroll, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
            empty_box.pack(fill="x", pady=20)
            ctk.CTkLabel(
                empty_box,
                text="No dictations match this filter.",
                font=ctk.CTkFont(size=13),
                text_color=TEXT_MUTED
            ).pack(padx=20, pady=30)
            self._display_selected(None)
            return

        # Render cards
        self._rendered_cards = {}
        for idx, item in enumerate(all_items):
            self._render_item_card(item, is_first=(idx == 0))

        # Auto-select first item if none currently selected
        if self.selected_item is None or self.selected_item not in all_items:
            self._select_item(all_items[0])

    def _render_item_card(self, item: dict, is_first: bool = False):
        cat = item.get("category", "Work")
        badge_meta = get_badge_colors(cat)

        card = ctk.CTkFrame(self.list_scroll, fg_color=BG_CARD, corner_radius=8, border_color=BORDER_CARD, border_width=1)
        card.pack(fill="x", pady=4)
        self._rendered_cards[item["id"]] = card

        # Top row: Badge + Timestamp
        top = ctk.CTkFrame(card, fg_color="transparent")
        top.pack(fill="x", padx=12, pady=(8, 4))

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

        ts = item.get("timestamp", "")
        if len(ts) > 10:
            ts_display = ts[11:16] if " " in ts else ts
        else:
            ts_display = ts

        ctk.CTkLabel(top, text=ts_display, font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(side="right")

        # Text preview
        txt = item.get("text", "")
        preview = (txt[:110] + "...") if len(txt) > 110 else txt
        preview_lbl = ctk.CTkLabel(
            card,
            text=preview,
            font=ctk.CTkFont(size=12),
            text_color=TEXT_SECONDARY,
            wraplength=380,
            justify="left"
        )
        preview_lbl.pack(anchor="w", padx=12, pady=(0, 10))

        # Bind clicks to select
        for widget in [card, top, badge, preview_lbl]:
            widget.bind("<Button-1>", lambda e, it=item: self._select_item(it))

    def _select_item(self, item: dict):
        self.selected_item = item
        # Update card highlights
        for item_id, card_widget in self._rendered_cards.items():
            if item and item_id == item.get("id"):
                card_widget.configure(border_color=ACCENT_PRIMARY, fg_color=BG_CARD_HOVER)
            else:
                card_widget.configure(border_color=BORDER_CARD, fg_color=BG_CARD)

        self._display_selected(item)

    def _display_selected(self, item: dict):
        if not item:
            self.detail_badge.configure(text="None", fg_color="#1E293B", text_color=TEXT_MUTED)
            self.detail_meta.configure(text="")
            self.detail_text.delete("1.0", "end")
            self.detail_text.insert("1.0", "Select a dictation from the list to inspect details.")
            return

        cat = item.get("category", "Work")
        badge_meta = get_badge_colors(cat)
        self.detail_badge.configure(text=cat, fg_color=badge_meta["bg"], text_color=badge_meta["fg"])
        
        ts = item.get("timestamp", "")
        wc = len(item.get("text", "").split())
        self.detail_meta.configure(text=f"{ts} • {wc} words")

        self.detail_text.delete("1.0", "end")
        self.detail_text.insert("1.0", item.get("text", ""))

    def _copy_selected(self):
        if not self.selected_item:
            return
        text = self.detail_text.get("1.0", "end").strip()
        self.clipboard_clear()
        self.clipboard_append(text)
        self.copy_btn.configure(text="✓ Copied!", fg_color="#059669")
        self.after(1500, lambda: self.copy_btn.configure(text="📋 Copy", fg_color=ACCENT_PRIMARY))

    def _save_inline_edit(self):
        if not self.selected_item:
            return
        new_text = self.detail_text.get("1.0", "end").strip()
        item_id = self.selected_item.get("id")

        if self.selected_item.get("source") == "history":
            history = self.config.load_history()
            for h in history:
                if h.get("timestamp") == item_id:
                    h["text"] = new_text
                    break
            import json
            from config import HISTORY_FILE
            try:
                with open(HISTORY_FILE, "w", encoding="utf-8") as f:
                    json.dump(history, f, indent=2, ensure_ascii=False)
            except Exception:
                pass
        elif self.selected_item.get("source") == "note_store" and self.note_store:
            try:
                self.note_store.update_note(item_id, structured_content=new_text)
            except Exception:
                pass

        self.save_edit_btn.configure(text="✓ Saved!", fg_color="#059669")
        self.after(1500, lambda: self.save_edit_btn.configure(text="💾 Save Edit", fg_color="#334155"))
        self._load_items()

    def _delete_selected(self):
        if not self.selected_item:
            return
        item_id = self.selected_item.get("id")
        if self.selected_item.get("source") == "history":
            history = self.config.load_history()
            history = [h for h in history if h.get("timestamp") != item_id]
            import json
            from config import HISTORY_FILE
            try:
                with open(HISTORY_FILE, "w", encoding="utf-8") as f:
                    json.dump(history, f, indent=2, ensure_ascii=False)
            except Exception:
                pass
        elif self.selected_item.get("source") == "note_store" and self.note_store:
            try:
                self.note_store.delete_note(item_id)
            except Exception:
                pass

        self.selected_item = None
        self._load_items()
