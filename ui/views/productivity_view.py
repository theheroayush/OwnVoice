"""
OwnVoice Desktop Hub — Productivity & Analytics View
Matches media_1789815634245.png with 4 metrics cards, 7-day interactive bar chart,
top apps breakdown, popular topics, streaks, and impact badges.
"""
import tkinter as tk
import customtkinter as ctk
import datetime
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_PRIMARY_HOVER, ACCENT_EMERALD, ACCENT_SKY, ACCENT_AMBER,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED, TEXT_SUBTLE,
    CHART_BAR_DEFAULT, CHART_BAR_ACTIVE, CHART_TOOLTIP_BG
)

class ProductivityView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager, note_store=None):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager
        self.note_store = note_store
        self.time_range = "7 days"
        self.active_bar_idx = 4  # Sep 16 default active

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(0, weight=1)

        self.scroll = ctk.CTkScrollableFrame(self, fg_color=BG_APP, corner_radius=0)
        self.scroll.grid(row=0, column=0, sticky="nsew", padx=24, pady=16)
        self.scroll.grid_columnconfigure(0, weight=1)

        self._build_header()
        self._build_metrics_cards()
        self._build_chart_and_sidebar()

    def _build_header(self):
        hdr = ctk.CTkFrame(self.scroll, fg_color="transparent")
        hdr.pack(fill="x", pady=(0, 16))
        hdr.grid_columnconfigure(0, weight=1)
        hdr.grid_columnconfigure(1, weight=1)

        left = ctk.CTkFrame(hdr, fg_color="transparent")
        left.grid(row=0, column=0, sticky="w")

        ctk.CTkLabel(left, text="📊  PRODUCTIVITY", font=ctk.CTkFont(size=10, weight="bold"), text_color=ACCENT_SKY).pack(anchor="w")
        
        title_row = ctk.CTkFrame(left, fg_color="transparent")
        title_row.pack(anchor="w", pady=(2, 2))
        ctk.CTkLabel(title_row, text="See your ", font=ctk.CTkFont(family="Segoe UI", size=24, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")
        ctk.CTkLabel(title_row, text="progress", font=ctk.CTkFont(family="Segoe UI", size=24, weight="bold"), text_color="#818CF8").pack(side="left")

        ctk.CTkLabel(left, text="Track how OwnVoice is saving you time and making you more productive.", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(anchor="w")

        # Right Controls: Filter pills + Date picker
        right = ctk.CTkFrame(hdr, fg_color="transparent")
        right.grid(row=0, column=1, sticky="e")

        filters_frame = ctk.CTkFrame(right, fg_color=BG_CARD, corner_radius=8, border_color=BORDER_CARD, border_width=1)
        filters_frame.pack(side="left", padx=(0, 10))

        self.range_btns = {}
        for r in ["7 days", "30 days", "3 months", "All time"]:
            is_active = (r == self.time_range)
            b = ctk.CTkButton(
                filters_frame,
                text=r,
                width=62,
                height=28,
                corner_radius=6,
                font=ctk.CTkFont(size=11, weight="bold"),
                fg_color=ACCENT_PRIMARY if is_active else "transparent",
                hover_color=ACCENT_PRIMARY_HOVER if is_active else BG_CARD_HOVER,
                text_color=TEXT_PRIMARY if is_active else TEXT_MUTED,
                command=lambda rng=r: self._set_time_range(rng)
            )
            b.pack(side="left", padx=2, pady=2)
            self.range_btns[r] = b

        now = datetime.datetime.now()
        date_str = f"📅 {(now - datetime.timedelta(days=6)).strftime('%b %d')} - {now.strftime('%b %d, %Y')}"
        ctk.CTkButton(
            right,
            text=date_str,
            font=ctk.CTkFont(size=11),
            fg_color=BG_CARD,
            hover_color=BG_CARD_HOVER,
            border_color=BORDER_CARD,
            border_width=1,
            text_color=TEXT_SECONDARY,
            height=32
        ).pack(side="left")

    def _set_time_range(self, rng):
        self.time_range = rng
        for r, btn in self.range_btns.items():
            is_active = (r == rng)
            btn.configure(
                fg_color=ACCENT_PRIMARY if is_active else "transparent",
                hover_color=ACCENT_PRIMARY_HOVER if is_active else BG_CARD_HOVER,
                text_color=TEXT_PRIMARY if is_active else TEXT_MUTED
            )
        self._draw_chart()

    def _build_metrics_cards(self):
        grid = ctk.CTkFrame(self.scroll, fg_color="transparent")
        grid.pack(fill="x", pady=(0, 18))
        for col in range(4):
            grid.grid_columnconfigure(col, weight=1)

        metrics = [
            ("📄", "248", "Dictations", "+12% vs last week"),
            ("🕒", "12.4 hours", "Time saved", "+18% vs last week"),
            ("📝", "18,492", "Words created", "+25% vs last week"),
            ("⚡", "98%", "Accuracy", "+2% vs last week")
        ]

        for col, (icon, val, lbl, trend) in enumerate(metrics):
            card = ctk.CTkFrame(grid, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
            card.grid(row=0, column=col, sticky="nsew", padx=4 if 0 < col < 3 else (0 if col == 0 else 0))

            top = ctk.CTkFrame(card, fg_color="transparent")
            top.pack(fill="x", padx=14, pady=(12, 2))
            ctk.CTkLabel(top, text=icon, font=ctk.CTkFont(size=14)).pack(side="left")

            ctk.CTkLabel(card, text=val, font=ctk.CTkFont(size=22, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=14, pady=(2, 0))

            bot = ctk.CTkFrame(card, fg_color="transparent")
            bot.pack(fill="x", padx=14, pady=(2, 12))
            ctk.CTkLabel(bot, text=lbl, font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(side="left", padx=(0, 6))
            ctk.CTkLabel(bot, text=trend, font=ctk.CTkFont(size=10, weight="bold"), text_color=ACCENT_EMERALD, fg_color="#064E3B", corner_radius=4, padx=4, pady=1).pack(side="left")

    def _build_chart_and_sidebar(self):
        split = ctk.CTkFrame(self.scroll, fg_color="transparent")
        split.pack(fill="x", pady=(0, 16))
        split.grid_columnconfigure(0, weight=3)
        split.grid_columnconfigure(1, weight=2)

        # Left Column: Chart Card + Top Apps & Popular Topics
        left = ctk.CTkFrame(split, fg_color="transparent")
        left.grid(row=0, column=0, sticky="nsew", padx=(0, 16))

        # Chart Card
        chart_card = ctk.CTkFrame(left, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        chart_card.pack(fill="x", pady=(0, 16))

        c_header = ctk.CTkFrame(chart_card, fg_color="transparent")
        c_header.pack(fill="x", padx=16, pady=(14, 4))
        
        c_title_col = ctk.CTkFrame(c_header, fg_color="transparent")
        c_title_col.pack(side="left")
        ctk.CTkLabel(c_title_col, text="Words created", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(c_title_col, text="Your productivity over the last 7 days.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w")

        words_filter = ctk.CTkOptionMenu(
            c_header,
            values=["Words", "Dictations", "Time Saved"],
            width=110,
            height=28,
            fg_color="#0D121D",
            button_color="#1E293B",
            button_hover_color=BG_CARD_HOVER,
            dropdown_fg_color=BG_CARD
        )
        words_filter.pack(side="right")

        # Interactive Canvas for Bar Chart
        self.chart_canvas = tk.Canvas(chart_card, bg=BG_CARD, height=170, highlightthickness=0)
        self.chart_canvas.pack(fill="x", padx=16, pady=(4, 16))
        self.chart_canvas.bind("<Motion>", self._on_canvas_hover)

        self._draw_chart()

        # Bottom 2 Columns: Top Apps & Popular Topics
        bottom_row = ctk.CTkFrame(left, fg_color="transparent")
        bottom_row.pack(fill="x")
        bottom_row.grid_columnconfigure(0, weight=1)
        bottom_row.grid_columnconfigure(1, weight=1)

        # Top Apps
        apps_card = ctk.CTkFrame(bottom_row, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        apps_card.grid(row=0, column=0, sticky="nsew", padx=(0, 8))

        ctk.CTkLabel(apps_card, text="Top apps", font=ctk.CTkFont(size=13, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=16, pady=(12, 2))
        ctk.CTkLabel(apps_card, text="Where you use OwnVoice the most.", font=ctk.CTkFont(size=10), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 10))

        apps = [
            ("✉️", "Email", 38, "#6366F1"),
            ("💬", "WhatsApp", 22, "#10B981"),
            ("📝", "Notion", 18, "#94A3B8"),
            ("💻", "VS Code", 12, "#38BDF8"),
            ("🔲", "Others", 10, "#475569")
        ]
        for icon, name, pct, col in apps:
            row = ctk.CTkFrame(apps_card, fg_color="transparent")
            row.pack(fill="x", padx=16, pady=3)
            ctk.CTkLabel(row, text=f"{icon} {name}", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_SECONDARY, width=85, anchor="w").pack(side="left")
            ctk.CTkLabel(row, text=f"{pct}%", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED, width=35).pack(side="left")
            bar = ctk.CTkProgressBar(row, height=7, progress_color=col, fg_color="#1E293B")
            bar.set(pct / 100.0)
            bar.pack(side="left", fill="x", expand=True, padx=(4, 0))
        ctk.CTkLabel(apps_card, text="").pack(pady=4)

        # Popular Topics
        topics_card = ctk.CTkFrame(bottom_row, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        topics_card.grid(row=0, column=1, sticky="nsew", padx=(8, 0))

        ctk.CTkLabel(topics_card, text="Popular topics", font=ctk.CTkFont(size=13, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=16, pady=(12, 2))
        ctk.CTkLabel(topics_card, text="What you talk about most.", font=ctk.CTkFont(size=10), text_color=TEXT_MUTED).pack(anchor="w", padx=16, pady=(0, 10))

        topics = [
            ("1", "Work & Meetings", 32, "#6366F1"),
            ("2", "Product Ideas", 18, "#818CF8"),
            ("3", "Personal Notes", 14, "#38BDF8"),
            ("4", "Team Updates", 12, "#34D399"),
            ("5", "Learning & Research", 10, "#94A3B8")
        ]
        for rank, name, pct, col in topics:
            row = ctk.CTkFrame(topics_card, fg_color="transparent")
            row.pack(fill="x", padx=16, pady=3)
            ctk.CTkLabel(row, text=f"{rank}  {name}", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_SECONDARY, width=120, anchor="w").pack(side="left")
            ctk.CTkLabel(row, text=f"{pct}%", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED, width=32).pack(side="left")
            bar = ctk.CTkProgressBar(row, height=7, progress_color=col, fg_color="#1E293B")
            bar.set(pct / 100.0)
            bar.pack(side="left", fill="x", expand=True, padx=(4, 0))
        ctk.CTkLabel(topics_card, text="").pack(pady=4)

        # Right Column: Streaks, Achievements & Impact
        right = ctk.CTkFrame(split, fg_color="transparent")
        right.grid(row=0, column=1, sticky="nsew")

        # Card 1: You're on a roll
        c1 = ctk.CTkFrame(right, fg_color="#1E2038", corner_radius=10, border_color="#3B3D6A", border_width=1)
        c1.pack(fill="x", pady=(0, 10))
        c1_top = ctk.CTkFrame(c1, fg_color="transparent")
        c1_top.pack(fill="x", padx=14, pady=(12, 4))
        ctk.CTkLabel(c1_top, text="🏆", font=ctk.CTkFont(size=18)).pack(side="left", padx=(0, 8))
        ctk.CTkLabel(c1_top, text="You're on a roll!", font=ctk.CTkFont(size=13, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")
        ctk.CTkLabel(c1, text="You've used OwnVoice for 7 days in a row. Keep going!", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED, wraplength=260, justify="left").pack(anchor="w", padx=14, pady=(0, 12))

        # Card 2: Streak
        c2 = ctk.CTkFrame(right, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        c2.pack(fill="x", pady=(0, 10))
        c2_top = ctk.CTkFrame(c2, fg_color="transparent")
        c2_top.pack(fill="x", padx=14, pady=(12, 4))
        ctk.CTkLabel(c2_top, text="🔥 Streak", font=ctk.CTkFont(size=12, weight="bold"), text_color=ACCENT_AMBER).pack(side="left")
        
        c2_stats = ctk.CTkFrame(c2, fg_color="transparent")
        c2_stats.pack(fill="x", padx=14, pady=(0, 12))
        c2_stats.grid_columnconfigure(0, weight=1)
        c2_stats.grid_columnconfigure(1, weight=1)
        
        s1 = ctk.CTkFrame(c2_stats, fg_color="transparent")
        s1.grid(row=0, column=0, sticky="w")
        ctk.CTkLabel(s1, text="7 days", font=ctk.CTkFont(size=16, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(s1, text="Current streak", font=ctk.CTkFont(size=10), text_color=TEXT_MUTED).pack(anchor="w")

        s2 = ctk.CTkFrame(c2_stats, fg_color="transparent")
        s2.grid(row=0, column=1, sticky="w")
        ctk.CTkLabel(s2, text="14 days", font=ctk.CTkFont(size=16, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(s2, text="Longest streak", font=ctk.CTkFont(size=10), text_color=TEXT_MUTED).pack(anchor="w")

        # Card 3: Time saved
        c3 = ctk.CTkFrame(right, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        c3.pack(fill="x", pady=(0, 10))
        c3_top = ctk.CTkFrame(c3, fg_color="transparent")
        c3_top.pack(fill="x", padx=14, pady=(12, 2))
        ctk.CTkLabel(c3_top, text="🕒 Time saved", font=ctk.CTkFont(size=12, weight="bold"), text_color=ACCENT_SKY).pack(side="left")
        ctk.CTkLabel(c3_top, text="›", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_MUTED).pack(side="right")
        ctk.CTkLabel(c3, text="12.4 hours", font=ctk.CTkFont(size=16, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=14)
        ctk.CTkLabel(c3, text="That's like 1.5 working days back in your pocket!", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED, wraplength=260, justify="left").pack(anchor="w", padx=14, pady=(0, 12))

        # Card 4: Environmental Impact
        c4 = ctk.CTkFrame(right, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        c4.pack(fill="x", pady=(0, 10))
        c4_top = ctk.CTkFrame(c4, fg_color="transparent")
        c4_top.pack(fill="x", padx=14, pady=(12, 2))
        ctk.CTkLabel(c4_top, text="🌱 Your impact", font=ctk.CTkFont(size=12, weight="bold"), text_color=ACCENT_EMERALD).pack(side="left")
        ctk.CTkLabel(c4_top, text="›", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_MUTED).pack(side="right")
        ctk.CTkLabel(c4, text="~2.4 kg CO₂ saved", font=ctk.CTkFont(size=16, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w", padx=14)
        ctk.CTkLabel(c4, text="By typing less and thinking more.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=14, pady=(0, 12))

        # Card 5: 3x more productive
        c5 = ctk.CTkFrame(right, fg_color="#1E2038", corner_radius=10, border_color="#3B3D6A", border_width=1)
        c5.pack(fill="x")
        c5_top = ctk.CTkFrame(c5, fg_color="transparent")
        c5_top.pack(fill="x", padx=14, pady=(12, 2))
        ctk.CTkLabel(c5_top, text="✨ You're 3× more productive!", font=ctk.CTkFont(size=12, weight="bold"), text_color="#A5B4FC").pack(side="left")
        ctk.CTkLabel(c5_top, text="›", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_MUTED).pack(side="right")
        ctk.CTkLabel(c5, text="Users like you save an average of 10+ hours every week with OwnVoice.", font=ctk.CTkFont(size=10), text_color=TEXT_MUTED, wraplength=260, justify="left").pack(anchor="w", padx=14, pady=(0, 12))

    def _draw_chart(self):
        if not hasattr(self, "chart_canvas"):
            return
        
        w = self.chart_canvas.winfo_width() or 540
        h = 170
        self.chart_canvas.delete("all")

        days = ["Sep 12", "Sep 13", "Sep 14", "Sep 15", "Sep 16", "Sep 17", "Sep 18"]
        values = [1820, 2150, 2410, 2850, 3124, 2300, 2560]
        max_val = 4000

        left_margin = 35
        bottom_margin = 25
        chart_w = w - left_margin - 20
        chart_h = h - bottom_margin - 30

        # Draw horizontal gridlines & labels
        for y_tick, y_lbl in [(0, "0"), (0.25, "1K"), (0.5, "2K"), (0.75, "3K"), (1.0, "4K")]:
            y_pos = 20 + chart_h * (1.0 - y_tick)
            self.chart_canvas.create_line(left_margin, y_pos, w - 20, y_pos, fill="#182133", width=1)
            self.chart_canvas.create_text(left_margin - 14, y_pos, text=y_lbl, fill="#64748B", font=("Segoe UI", 9))

        # Draw bars
        n = len(days)
        bar_w = 34
        spacing = (chart_w - (n * bar_w)) / (n + 1)

        self._bar_coords = []

        for i in range(n):
            val = values[i]
            bx = left_margin + spacing + i * (bar_w + spacing)
            bar_height = (val / max_val) * chart_h
            by0 = 20 + chart_h - bar_height
            by1 = 20 + chart_h

            is_active = (i == self.active_bar_idx)
            color = CHART_BAR_ACTIVE if is_active else CHART_BAR_DEFAULT

            # Rounded bar top
            self.chart_canvas.create_rectangle(bx, by0, bx + bar_w, by1, fill=color, outline="", width=0)

            # Date Label below
            self.chart_canvas.create_text(bx + bar_w / 2, h - 10, text=days[i], fill="#94A3B8" if is_active else "#64748B", font=("Segoe UI", 9, "bold" if is_active else "normal"))

            self._bar_coords.append((bx, by0, bx + bar_w, by1, i, val, days[i]))

            # If active, draw floating tooltip pill above bar
            if is_active:
                tip_w = 125
                tip_h = 24
                tip_x = bx + bar_w / 2 - tip_w / 2
                tip_y = max(4, by0 - tip_h - 6)
                self.chart_canvas.create_rectangle(tip_x, tip_y, tip_x + tip_w, tip_y + tip_h, fill=CHART_TOOLTIP_BG, outline="#3B3D6A", width=1)
                self.chart_canvas.create_text(tip_x + tip_w / 2, tip_y + tip_h / 2, text=f"{days[i]}, 2026 • {val:,} words", fill="#CBD5E1", font=("Segoe UI", 8, "bold"))

    def _on_canvas_hover(self, event):
        x, y = event.x, event.y
        for bx0, by0, bx1, by1, idx, val, day in getattr(self, "_bar_coords", []):
            if bx0 <= x <= bx1:
                if self.active_bar_idx != idx:
                    self.active_bar_idx = idx
                    self._draw_chart()
                break
