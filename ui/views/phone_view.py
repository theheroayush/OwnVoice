"""
OwnVoice Desktop Hub — Connect Your Phone View
Matches media_1789815616649.png with feature pills, glowing QR code canvas with checklist and PIN countdown,
realistic mobile phone viewfinder mockup, and waiting-for-connection animated indicator.
"""
import tkinter as tk
import customtkinter as ctk
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_PRIMARY_HOVER, ACCENT_EMERALD, ACCENT_SKY, ACCENT_AMBER,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED, TEXT_SUBTLE
)

class PhoneView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager, bridge_server=None):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager
        self.bridge_server = bridge_server
        self._anim_running = True
        self._spinner_angle = 0
        self._pin_seconds_remaining = 292  # 4:52

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(0, weight=1)

        self.scroll = ctk.CTkScrollableFrame(self, fg_color=BG_APP, corner_radius=0)
        self.scroll.grid(row=0, column=0, sticky="nsew", padx=24, pady=16)
        self.scroll.grid_columnconfigure(0, weight=1)

        self._build_header()
        self._build_feature_pills()
        self._build_two_steps()
        self._build_need_help()
        self._start_countdown()

    def _build_header(self):
        hdr = ctk.CTkFrame(self.scroll, fg_color="transparent")
        hdr.pack(fill="x", pady=(0, 14))

        ctk.CTkLabel(hdr, text="📱  DEVICES", font=ctk.CTkFont(size=10, weight="bold"), text_color=ACCENT_SKY).pack(anchor="w")

        title_row = ctk.CTkFrame(hdr, fg_color="transparent")
        title_row.pack(anchor="w", pady=(2, 2))
        ctk.CTkLabel(title_row, text="Connect ", font=ctk.CTkFont(family="Segoe UI", size=24, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")
        ctk.CTkLabel(title_row, text="your phone", font=ctk.CTkFont(family="Segoe UI", size=24, weight="bold"), text_color="#818CF8").pack(side="left")

        ctk.CTkLabel(hdr, text="Use your Android phone as a wireless microphone.", font=ctk.CTkFont(size=12), text_color=TEXT_MUTED).pack(anchor="w")

    def _build_feature_pills(self):
        pills_row = ctk.CTkFrame(self.scroll, fg_color="transparent")
        pills_row.pack(fill="x", pady=(0, 16))
        for col in range(3):
            pills_row.grid_columnconfigure(col, weight=1)

        features = [
            ("🎙️", "High quality audio", "Clear voice input"),
            ("📶", "Wireless & easy", "Just scan and connect"),
            ("📱", "Perfect for anywhere", "Dictate from your phone")
        ]

        for col, (icon, title, sub) in enumerate(features):
            card = ctk.CTkFrame(pills_row, fg_color=BG_CARD, corner_radius=8, border_color=BORDER_CARD, border_width=1)
            card.grid(row=0, column=col, sticky="nsew", padx=4 if col == 1 else (0 if col == 0 else 0))

            inner = ctk.CTkFrame(card, fg_color="transparent")
            inner.pack(fill="x", padx=12, pady=8)

            ctk.CTkLabel(inner, text=icon, font=ctk.CTkFont(size=16)).pack(side="left", padx=(0, 10))

            txt_col = ctk.CTkFrame(inner, fg_color="transparent")
            txt_col.pack(side="left")
            ctk.CTkLabel(txt_col, text=title, font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
            ctk.CTkLabel(txt_col, text=sub, font=ctk.CTkFont(size=10), text_color=TEXT_MUTED).pack(anchor="w")

    def _build_two_steps(self):
        steps_grid = ctk.CTkFrame(self.scroll, fg_color="transparent")
        steps_grid.pack(fill="x", pady=(0, 16))
        steps_grid.grid_columnconfigure(0, weight=3)
        steps_grid.grid_columnconfigure(1, weight=2)

        # Step 1: Scan the QR code
        s1 = ctk.CTkFrame(steps_grid, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        s1.grid(row=0, column=0, sticky="nsew", padx=(0, 16))

        s1_top = ctk.CTkFrame(s1, fg_color="transparent")
        s1_top.pack(fill="x", padx=16, pady=(16, 10))

        badge1 = ctk.CTkLabel(s1_top, text="1", font=ctk.CTkFont(size=11, weight="bold"), text_color="#FFFFFF", fg_color="#4F46E5", corner_radius=12, width=24, height=24)
        badge1.pack(side="left", padx=(0, 10))

        t1_col = ctk.CTkFrame(s1_top, fg_color="transparent")
        t1_col.pack(side="left")
        ctk.CTkLabel(t1_col, text="Scan the QR code", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(t1_col, text="Open OwnVoice on your phone and scan this code.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w")

        # QR & Checklist Layout
        qr_row = ctk.CTkFrame(s1, fg_color="transparent")
        qr_row.pack(fill="x", padx=16, pady=(0, 14))

        # Glowing QR Container
        qr_glow_box = ctk.CTkFrame(qr_row, fg_color="#182038", corner_radius=12, border_color="#818CF8", border_width=1.5)
        qr_glow_box.pack(side="left", padx=(0, 18), pady=4)

        self.qr_label = ctk.CTkLabel(qr_glow_box, text="")
        self.qr_label.pack(padx=12, pady=12)

        # Checklist
        chk_box = ctk.CTkFrame(qr_row, fg_color="transparent")
        chk_box.pack(side="left", fill="both", expand=True)

        checklist_items = [
            ("📶", "Make sure both devices\nare on the same Wi-Fi"),
            ("📱", "Open the OwnVoice mobile app"),
            ("📷", "Scan the QR code"),
            ("🔗", "You'll be connected in seconds")
        ]
        for icon, desc in checklist_items:
            item_f = ctk.CTkFrame(chk_box, fg_color="transparent")
            item_f.pack(fill="x", pady=4)
            ctk.CTkLabel(item_f, text=icon, font=ctk.CTkFont(size=13), width=24).pack(side="left", padx=(0, 8))
            ctk.CTkLabel(item_f, text=desc, font=ctk.CTkFont(size=11), text_color=TEXT_SECONDARY, justify="left").pack(side="left")

        # Fallback PIN Row
        pin_box = ctk.CTkFrame(s1, fg_color="transparent")
        pin_box.pack(fill="x", padx=16, pady=(0, 16))

        ctk.CTkLabel(pin_box, text="Can't scan? Enter code instead", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", pady=(0, 4))

        pin_input_row = ctk.CTkFrame(pin_box, fg_color="transparent")
        pin_input_row.pack(anchor="w")

        pin_val = self.bridge_server.current_pin if self.bridge_server else "562330"
        self.pin_display = ctk.CTkLabel(
            pin_input_row,
            text=f"  {pin_val[:3]}  {pin_val[3:]}  ",
            font=ctk.CTkFont(family="Consolas", size=16, weight="bold"),
            fg_color="#0D121D",
            text_color=TEXT_PRIMARY,
            corner_radius=6,
            height=32
        )
        self.pin_display.pack(side="left", padx=(0, 8))

        copy_pin_btn = ctk.CTkButton(
            pin_input_row,
            text="📋",
            width=32,
            height=32,
            fg_color="#1E293B",
            hover_color=BG_CARD_HOVER,
            command=self._copy_pin
        )
        copy_pin_btn.pack(side="left", padx=(0, 12))

        self.refresh_timer_lbl = ctk.CTkLabel(
            pin_input_row,
            text="🕒 This code refreshes in 4:52",
            font=ctk.CTkFont(size=10),
            text_color=TEXT_MUTED
        )
        self.refresh_timer_lbl.pack(side="left")

        self._refresh_qr_image()

        # Step 2: Connect on your phone
        s2 = ctk.CTkFrame(steps_grid, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        s2.grid(row=0, column=1, sticky="nsew")

        s2_top = ctk.CTkFrame(s2, fg_color="transparent")
        s2_top.pack(fill="x", padx=16, pady=(16, 8))

        badge2 = ctk.CTkLabel(s2_top, text="2", font=ctk.CTkFont(size=11, weight="bold"), text_color="#FFFFFF", fg_color="#4F46E5", corner_radius=12, width=24, height=24)
        badge2.pack(side="left", padx=(0, 10))

        t2_col = ctk.CTkFrame(s2_top, fg_color="transparent")
        t2_col.pack(side="left")
        ctk.CTkLabel(t2_col, text="Connect on your phone", font=ctk.CTkFont(size=14, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(t2_col, text="Follow the steps in the mobile app.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w")

        # Phone Frame Mockup
        phone_chassis = ctk.CTkFrame(s2, fg_color="#0D121D", corner_radius=18, border_color="#334155", border_width=2, width=200, height=210)
        phone_chassis.pack(pady=(4, 12), padx=24)
        phone_chassis.pack_propagate(False)

        # Phone Status Bar
        p_status = ctk.CTkFrame(phone_chassis, fg_color="transparent", height=18)
        p_status.pack(fill="x", padx=10, pady=(6, 2))
        ctk.CTkLabel(p_status, text="9:41", font=ctk.CTkFont(size=9, weight="bold"), text_color=TEXT_MUTED).pack(side="left")
        ctk.CTkLabel(p_status, text="📶 🔋", font=ctk.CTkFont(size=8), text_color=TEXT_MUTED).pack(side="right")

        # Phone App Viewfinder
        ctk.CTkLabel(phone_chassis, text="🎙️ OwnVoice", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_PRIMARY).pack(pady=(4, 1))
        ctk.CTkLabel(phone_chassis, text="Scan QR code", font=ctk.CTkFont(size=10, weight="bold"), text_color=ACCENT_SKY).pack()
        ctk.CTkLabel(phone_chassis, text="Scan the code from your computer\nto connect.", font=ctk.CTkFont(size=8), text_color=TEXT_MUTED, justify="center").pack(pady=(0, 6))

        # Viewfinder box
        vf = ctk.CTkFrame(phone_chassis, fg_color="#182033", corner_radius=8, border_color="#818CF8", border_width=1.5, width=90, height=90)
        vf.pack()
        vf.pack_propagate(False)
        ctk.CTkLabel(vf, text="📷\nQR Scan", font=ctk.CTkFont(size=10), text_color="#A5B4FC").pack(expand=True)

        # Bottom Connection Status Pill with Spinner
        status_pill = ctk.CTkFrame(s2, fg_color="#1E2038", corner_radius=8, border_color="#3B3D6A", border_width=1)
        status_pill.pack(fill="x", padx=16, pady=(0, 16))

        sp_inner = ctk.CTkFrame(status_pill, fg_color="transparent")
        sp_inner.pack(fill="x", padx=12, pady=8)

        ctk.CTkLabel(sp_inner, text="🔗", font=ctk.CTkFont(size=14)).pack(side="left", padx=(0, 8))

        sp_text = ctk.CTkFrame(sp_inner, fg_color="transparent")
        sp_text.pack(side="left")
        ctk.CTkLabel(sp_text, text="Waiting for connection...", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(sp_text, text="Keep the app open on your phone.", font=ctk.CTkFont(size=9), text_color=TEXT_MUTED).pack(anchor="w")

        # Circular spinner canvas
        self.spinner_canvas = tk.Canvas(sp_inner, bg="#1E2038", width=20, height=20, highlightthickness=0)
        self.spinner_canvas.pack(side="right")
        self._animate_spinner()

    def _build_need_help(self):
        help_bar = ctk.CTkFrame(self.scroll, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        help_bar.pack(fill="x", pady=(0, 8))

        inner = ctk.CTkFrame(help_bar, fg_color="transparent")
        inner.pack(fill="x", padx=16, pady=10)

        ctk.CTkLabel(inner, text="❔", font=ctk.CTkFont(size=14)).pack(side="left", padx=(0, 8))
        ctk.CTkLabel(inner, text="Need help?", font=ctk.CTkFont(size=12, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left", padx=(0, 8))
        ctk.CTkLabel(inner, text="Make sure your phone and computer are on the same Wi-Fi network.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(side="left")

        guide_btn = ctk.CTkButton(
            inner,
            text="View guide →",
            font=ctk.CTkFont(size=11, weight="bold"),
            fg_color="transparent",
            hover_color=BG_CARD_HOVER,
            text_color=ACCENT_SKY,
            width=80,
            command=lambda: None
        )
        guide_btn.pack(side="right")

    def _copy_pin(self):
        pin_val = self.bridge_server.current_pin if self.bridge_server else "562330"
        self.clipboard_clear()
        self.clipboard_append(pin_val)

    def _refresh_qr_image(self):
        if self.bridge_server and hasattr(self, "qr_label") and self.qr_label:
            try:
                pil_img = self.bridge_server.generate_qr_image(size=140)
                ctk_img = ctk.CTkImage(light_image=pil_img, dark_image=pil_img, size=(140, 140))
                self.qr_label.configure(image=ctk_img)
            except Exception as e:
                print(f"[PhoneView] QR render notice: {e}")

    def _animate_spinner(self):
        if not self._anim_running or not hasattr(self, "spinner_canvas"):
            return
        try:
            if self.spinner_canvas.winfo_exists():
                self.spinner_canvas.delete("all")
                self._spinner_angle = (self._spinner_angle + 30) % 360
                self.spinner_canvas.create_arc(
                    2, 2, 18, 18,
                    start=self._spinner_angle,
                    extent=120,
                    outline=ACCENT_SKY,
                    width=2,
                    style="arc"
                )
                self.after(80, self._animate_spinner)
        except Exception:
            pass

    def _start_countdown(self):
        def tick():
            if not self._anim_running or not self.winfo_exists():
                return
            if self._pin_seconds_remaining > 0:
                self._pin_seconds_remaining -= 1
            else:
                self._pin_seconds_remaining = 300
                if self.bridge_server:
                    new_pin = self.bridge_server.regenerate_pin()
                    self.pin_display.configure(text=f"  {new_pin[:3]}  {new_pin[3:]}  ")
                    self._refresh_qr_image()

            mins = self._pin_seconds_remaining // 60
            secs = self._pin_seconds_remaining % 60
            self.refresh_timer_lbl.configure(text=f"🕒 This code refreshes in {mins}:{secs:02d}")
            self.after(1000, tick)

        self.after(1000, tick)

    def destroy(self):
        self._anim_running = False
        super().destroy()
