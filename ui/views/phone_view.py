"""
OwnVoice Desktop Hub — Phone Link View
Matches Universal Phone Link ecosystem bridge with QR code, PIN pairing, and local Wi-Fi auto-discovery.
"""
import customtkinter as ctk
from ui.design_tokens import (
    BG_APP, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    ACCENT_PRIMARY, ACCENT_EMERALD, ACCENT_SKY, ACCENT_AMBER,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED
)

class PhoneView(ctk.CTkFrame):
    def __init__(self, parent, hub, config_manager, bridge_server=None):
        super().__init__(parent, fg_color=BG_APP)
        self.hub = hub
        self.config = config_manager
        self.bridge_server = bridge_server

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(0, weight=1)

        self.scroll = ctk.CTkScrollableFrame(self, fg_color=BG_APP, corner_radius=0)
        self.scroll.grid(row=0, column=0, sticky="nsew", padx=24, pady=16)
        self.scroll.grid_columnconfigure(0, weight=1)

        self._build_header()
        self._build_status_pill()
        self._build_pairing_container()

    def _build_header(self):
        ctk.CTkLabel(self.scroll, text="📱 Universal Phone Link", font=ctk.CTkFont(family="Segoe UI", size=22, weight="bold"), text_color=TEXT_PRIMARY).pack(anchor="w")
        ctk.CTkLabel(
            self.scroll,
            text="Turn your Android phone into a high-fidelity wireless microphone that types directly into your PC cursor.",
            font=ctk.CTkFont(size=12),
            text_color=TEXT_MUTED
        ).pack(anchor="w", pady=(2, 14))

    def _build_status_pill(self):
        status_frame = ctk.CTkFrame(self.scroll, fg_color=BG_CARD, corner_radius=10, border_color=BORDER_CARD, border_width=1)
        status_frame.pack(fill="x", pady=(0, 16))

        local_ip = self.bridge_server.local_ip if self.bridge_server else "127.0.0.1"
        port = self.bridge_server.port if self.bridge_server else 8765

        ctk.CTkLabel(
            status_frame,
            text=f"🟢 Local Wi-Fi Discovery Active • Server: {local_ip}:{port} • Auto-Discovery: UDP 8766",
            font=ctk.CTkFont(size=12, weight="bold"),
            text_color=ACCENT_EMERALD
        ).pack(anchor="w", padx=16, pady=10)

    def _build_pairing_container(self):
        content_card = ctk.CTkFrame(self.scroll, fg_color=BG_CARD, corner_radius=12, border_color=BORDER_CARD, border_width=1)
        content_card.pack(fill="both", expand=True, pady=(0, 16))

        content_card.grid_columnconfigure(0, weight=2)
        content_card.grid_columnconfigure(1, weight=3)

        # Left Column: QR Code Container
        left_col = ctk.CTkFrame(content_card, fg_color="transparent")
        left_col.grid(row=0, column=0, sticky="nsew", padx=20, pady=20)

        self.qr_label = ctk.CTkLabel(left_col, text="")
        self.qr_label.pack(pady=(0, 10))

        ctk.CTkLabel(left_col, text="📷 Scan with OwnVoice Camera", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_SECONDARY).pack()
        ctk.CTkLabel(left_col, text="Instant pairing with zero typing", font=ctk.CTkFont(size=10), text_color=TEXT_MUTED).pack(pady=(2, 0))

        # Right Column: 3 Connection Methods
        right_col = ctk.CTkFrame(content_card, fg_color="transparent")
        right_col.grid(row=0, column=1, sticky="nsew", padx=(10, 20), pady=20)

        ctk.CTkLabel(right_col, text="3 Zero-Friction Ways to Connect:", font=ctk.CTkFont(size=14, weight="bold"), text_color=ACCENT_SKY).pack(anchor="w", pady=(0, 10))

        # Option 1: 1-Tap Auto-Discover
        opt1 = ctk.CTkFrame(right_col, fg_color="#0D121D", corner_radius=8, border_color=BORDER_CARD, border_width=1)
        opt1.pack(fill="x", pady=4)
        ctk.CTkLabel(opt1, text="⚡ Option 1: 1-Tap Auto-Discover (Recommended)", font=ctk.CTkFont(size=11, weight="bold"), text_color=ACCENT_EMERALD).pack(anchor="w", padx=12, pady=(8, 2))
        ctk.CTkLabel(opt1, text="When phone & PC are on the same Wi-Fi, open OwnVoice on Android and tap '1-Tap Auto-Discover'. Connected in <1 second!", font=ctk.CTkFont(size=10), text_color=TEXT_MUTED, wraplength=340, justify="left").pack(anchor="w", padx=12, pady=(0, 8))

        # Option 2: Scan QR Code
        opt2 = ctk.CTkFrame(right_col, fg_color="#0D121D", corner_radius=8, border_color=BORDER_CARD, border_width=1)
        opt2.pack(fill="x", pady=4)
        ctk.CTkLabel(opt2, text="📷 Option 2: Scan QR Code (WhatsApp Web style)", font=ctk.CTkFont(size=11, weight="bold"), text_color=ACCENT_SKY).pack(anchor="w", padx=12, pady=(8, 2))
        ctk.CTkLabel(opt2, text="In OwnVoice on phone, tap 'Scan QR Code' and point camera at the code on the left.", font=ctk.CTkFont(size=10), text_color=TEXT_MUTED, wraplength=340, justify="left").pack(anchor="w", padx=12, pady=(0, 8))

        # Option 3: 6-Digit PIN
        opt3 = ctk.CTkFrame(right_col, fg_color="#0D121D", corner_radius=8, border_color=BORDER_CARD, border_width=1)
        opt3.pack(fill="x", pady=4)
        ctk.CTkLabel(opt3, text="🔢 Option 3: Enter 6-Digit PIN", font=ctk.CTkFont(size=11, weight="bold"), text_color=ACCENT_AMBER).pack(anchor="w", padx=12, pady=(8, 2))

        pin_row = ctk.CTkFrame(opt3, fg_color="transparent")
        pin_row.pack(anchor="w", padx=12, pady=(2, 8))

        pin_val = self.bridge_server.current_pin if self.bridge_server else "000000"
        self.pin_label = ctk.CTkLabel(
            pin_row,
            text=f"  {pin_val[:3]} {pin_val[3:]}  ",
            font=ctk.CTkFont(family="Consolas", size=18, weight="bold"),
            fg_color=BG_CARD,
            text_color=ACCENT_AMBER,
            corner_radius=6
        )
        self.pin_label.pack(side="left", padx=(0, 10))

        ctk.CTkButton(
            pin_row,
            text="🔄 New PIN",
            width=80,
            height=28,
            font=ctk.CTkFont(size=11, weight="bold"),
            command=self._regenerate_bridge_pin,
            fg_color="#1E293B",
            hover_color=BG_CARD_HOVER
        ).pack(side="left")

        self._refresh_qr_image()

    def _regenerate_bridge_pin(self):
        if self.bridge_server:
            new_pin = self.bridge_server.regenerate_pin()
            if hasattr(self, "pin_label") and self.pin_label:
                self.pin_label.configure(text=f"  {new_pin[:3]} {new_pin[3:]}  ")
            self._refresh_qr_image()

    def _refresh_qr_image(self):
        if self.bridge_server and hasattr(self, "qr_label") and self.qr_label:
            try:
                pil_img = self.bridge_server.generate_qr_image(size=170)
                ctk_img = ctk.CTkImage(light_image=pil_img, dark_image=pil_img, size=(170, 170))
                self.qr_label.configure(image=ctk_img)
            except Exception as e:
                print(f"[PhoneView] QR render notice: {e}")
