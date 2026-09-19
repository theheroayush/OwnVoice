"""
OwnVoice Desktop Hub — Central Desktop Window
Tier-1, consumer-grade desktop application window with obsidian theme, sidebar navigation,
modular views, and backwards compatibility with existing audio/key tests.
"""
import threading
import customtkinter as ctk
from ui.design_tokens import (
    BG_APP, BG_SIDEBAR, BORDER_SIDEBAR, BG_CARD, BG_CARD_HOVER, BORDER_CARD,
    SIDEBAR_ACTIVE_BG, SIDEBAR_ACTIVE_BORDER, SIDEBAR_ACTIVE_TEXT, SIDEBAR_ACTIVE_ICON,
    SIDEBAR_INACTIVE_TEXT, SIDEBAR_INACTIVE_HOVER,
    ACCENT_PRIMARY, ACCENT_EMERALD, ACCENT_SKY, ACCENT_CRIMSON,
    TEXT_PRIMARY, TEXT_SECONDARY, TEXT_MUTED, TEXT_SUBTLE,
    WINDOW_WIDTH, WINDOW_HEIGHT, MIN_WIDTH, MIN_HEIGHT
)
from ui.views.home_view import HomeView
from ui.views.history_view import HistoryView
from ui.views.vocabulary_view import VocabularyView
from ui.views.snippets_view import SnippetsView
from ui.views.style_view import StyleView
from ui.views.phone_view import PhoneView
from ui.views.settings_view import SettingsView
from ui.views.productivity_view import ProductivityView

ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

class SettingsWindow:
    def __init__(self, config_manager, ai_engine, audio_recorder=None, snippet_engine=None, on_settings_changed=None, bridge_server=None, note_store=None, on_toggle_dictation=None):
        self.config = config_manager
        self.ai_engine = ai_engine
        self.audio_recorder = audio_recorder
        self.snippet_engine = snippet_engine
        self.on_settings_changed = on_settings_changed
        self.bridge_server = bridge_server
        self.note_store = note_store
        self.on_toggle_dictation = on_toggle_dictation

        self.window = None
        self.is_open = False
        self.test_running = False
        self._vu_paused = False
        self.dev_map = {}

        # Views and Navigation
        self.views = {}
        self.active_tab_name = "🏠 Home"
        self.nav_buttons = {}

        # Compatibility proxies for legacy test suites
        self.test_key_btn = None
        self.key_status_label = None
        self.api_entry = None
        self.api_status = None
        self.vu_progress = None

    def _normalize_tab(self, tab: str) -> str:
        if not tab:
            return "🏠 Home"
        t = str(tab).lower().strip()
        if "phone" in t or "link" in t:
            return "📱 Phone"
        elif "snippet" in t:
            return "⚡ Snippets"
        elif "vocab" in t or "word" in t:
            return "📖 Vocabulary"
        elif "style" in t or "tone" in t:
            return "Aa Style"
        elif "setting" in t or "general" in t or "audio" in t or "gemini" in t or "ai engine" in t:
            return "⚙️ Settings"
        elif "hist" in t or "dictat" in t:
            return "🕒 History"
        elif "productiv" in t or "progress" in t or "stat" in t:
            return "📊 Productivity"
        elif "home" in t:
            return "🏠 Home"
        return "🏠 Home"

    def show(self, initial_tab=None):
        target_tab = self._normalize_tab(initial_tab)
        if self.is_open and self.window:
            self.window.lift()
            self.window.focus_force()
            self.navigate_to(target_tab)
            return

        self.is_open = True
        self.window = ctk.CTkToplevel()
        self.window.title("OwnVoice Desktop Hub")
        self.window.geometry(f"{WINDOW_WIDTH}x{WINDOW_HEIGHT}")
        self.window.minsize(MIN_WIDTH, MIN_HEIGHT)
        self.window.configure(fg_color=BG_APP)
        self.window.protocol("WM_DELETE_WINDOW", self._on_close)

        # Global keyboard shortcuts
        self.window.bind("<Control-k>", lambda e: self.navigate_to("🕒 History"))
        self.window.bind("<Control-K>", lambda e: self.navigate_to("🕒 History"))

        # Main 2-column layout: Sidebar (col 0) + Content Area (col 1)
        self.window.grid_columnconfigure(0, weight=0, minsize=240)
        self.window.grid_columnconfigure(1, weight=1)
        self.window.grid_rowconfigure(0, weight=1)

        self._build_sidebar()
        self._build_content_area()

        # Initialize core views
        self._init_views()
        self.navigate_to(target_tab)

        # Start audio monitoring and VU meter thread
        if self.audio_recorder:
            dev_idx = self.config.get("input_device_index", None)
            self.audio_recorder.start_monitoring(device_index=dev_idx)
        self._start_vu_monitor()

    def _build_sidebar(self):
        self.sidebar = ctk.CTkFrame(
            self.window,
            fg_color=BG_SIDEBAR,
            corner_radius=0,
            border_color=BORDER_SIDEBAR,
            border_width=1,
            width=240
        )
        self.sidebar.grid(row=0, column=0, sticky="nsew")
        self.sidebar.grid_propagate(False)
        self.sidebar.grid_columnconfigure(0, weight=1)
        self.sidebar.grid_rowconfigure(1, weight=1)

        # Brand / Logo Header
        brand_frame = ctk.CTkFrame(self.sidebar, fg_color="transparent")
        brand_frame.grid(row=0, column=0, sticky="ew", padx=16, pady=(18, 14))

        logo_row = ctk.CTkFrame(brand_frame, fg_color="transparent")
        logo_row.pack(anchor="w")

        ctk.CTkLabel(logo_row, text="🎙️", font=ctk.CTkFont(size=20)).pack(side="left", padx=(0, 8))
        ctk.CTkLabel(logo_row, text="OwnVoice", font=ctk.CTkFont(family="Segoe UI", size=18, weight="bold"), text_color=TEXT_PRIMARY).pack(side="left")

        ctk.CTkLabel(brand_frame, text="Speak. It writes.", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(anchor="w", padx=(28, 0), pady=(0, 0))

        # Nav Scrollable Body
        nav_scroll = ctk.CTkScrollableFrame(self.sidebar, fg_color="transparent", corner_radius=0)
        nav_scroll.grid(row=1, column=0, sticky="nsew", padx=8)
        nav_scroll.grid_columnconfigure(0, weight=1)

        nav_items = [
            ("🏠 Home", None),
            ("🕒 History", None),
            ("📊 Productivity", None),
            ("YOUR VOICE", "header"),
            ("📖 Vocabulary", None),
            ("⚡ Snippets", None),
            ("Aa Style", None),
            ("DEVICES", "header"),
            ("📱 Phone", None),
            ("⚙️ Settings", None),
        ]

        self.nav_buttons = {}
        for name, item_type in nav_items:
            if item_type == "header":
                hdr = ctk.CTkLabel(
                    nav_scroll,
                    text=name,
                    font=ctk.CTkFont(size=10, weight="bold"),
                    text_color=TEXT_SUBTLE
                )
                hdr.pack(anchor="w", padx=12, pady=(14, 4))
            else:
                btn = ctk.CTkButton(
                    nav_scroll,
                    text=f"  {name}",
                    font=ctk.CTkFont(size=12, weight="bold"),
                    fg_color="transparent",
                    hover_color=SIDEBAR_INACTIVE_HOVER,
                    text_color=SIDEBAR_INACTIVE_TEXT,
                    anchor="w",
                    height=36,
                    corner_radius=8,
                    border_spacing=6,
                    command=lambda n=name: self.navigate_to(n)
                )
                btn.pack(fill="x", pady=2)
                self.nav_buttons[name] = btn

        # Bottom Card: Ready status + Version
        bottom_frame = ctk.CTkFrame(self.sidebar, fg_color="transparent")
        bottom_frame.grid(row=2, column=0, sticky="ew", padx=12, pady=(0, 16))

        status_box = ctk.CTkFrame(bottom_frame, fg_color=BG_CARD, corner_radius=8, border_color=BORDER_CARD, border_width=1)
        status_box.pack(fill="x", pady=(0, 8))

        status_inner = ctk.CTkFrame(status_box, fg_color="transparent")
        status_inner.pack(fill="x", padx=10, pady=8)

        self.live_indicator = ctk.CTkLabel(status_inner, text="🟢", font=ctk.CTkFont(size=10), text_color=ACCENT_EMERALD)
        self.live_indicator.pack(side="left", padx=(0, 6))

        self.ready_label = ctk.CTkLabel(status_inner, text="Ready to listen", font=ctk.CTkFont(size=11, weight="bold"), text_color=TEXT_PRIMARY)
        self.ready_label.pack(side="left")

        ctk.CTkLabel(bottom_frame, text="OwnVoice v2.1.0 • Pro Edition", font=ctk.CTkFont(size=10), text_color=TEXT_SUBTLE).pack(anchor="w", padx=4)

    def _build_content_area(self):
        self.main_container = ctk.CTkFrame(self.window, fg_color=BG_APP, corner_radius=0)
        self.main_container.grid(row=0, column=1, sticky="nsew")
        self.main_container.grid_columnconfigure(0, weight=1)
        self.main_container.grid_rowconfigure(1, weight=1)

        # Top Bar
        top_bar = ctk.CTkFrame(self.main_container, fg_color=BG_APP, height=44, corner_radius=0)
        top_bar.grid(row=0, column=0, sticky="ew", padx=24, pady=(8, 0))
        top_bar.grid_columnconfigure(0, weight=1)
        top_bar.grid_columnconfigure(1, weight=1)

        hk = self.config.get("hotkey", "f8").upper()
        hotkey_pill = ctk.CTkFrame(top_bar, fg_color=BG_CARD, corner_radius=6, border_color=BORDER_CARD, border_width=1)
        hotkey_pill.grid(row=0, column=0, sticky="w")
        ctk.CTkLabel(hotkey_pill, text=f"⚡ Press {hk} to speak anywhere", font=ctk.CTkFont(size=11, weight="bold"), text_color=ACCENT_SKY).pack(padx=10, pady=4)

        right_status = ctk.CTkFrame(top_bar, fg_color="transparent")
        right_status.grid(row=0, column=1, sticky="e")
        ctk.CTkLabel(right_status, text="Gemini Flash • Sub-second Latency", font=ctk.CTkFont(size=11), text_color=TEXT_MUTED).pack(side="right")

        # View Host Frame
        self.view_host = ctk.CTkFrame(self.main_container, fg_color=BG_APP, corner_radius=0)
        self.view_host.grid(row=1, column=0, sticky="nsew")
        self.view_host.grid_columnconfigure(0, weight=1)
        self.view_host.grid_rowconfigure(0, weight=1)

    def _init_views(self):
        # Instantiate SettingsView first so attributes are available for backwards-compatibility tests
        self.settings_view = SettingsView(
            parent=self.view_host,
            hub=self,
            config_manager=self.config,
            ai_engine=self.ai_engine,
            audio_recorder=self.audio_recorder,
            on_settings_changed=self.on_settings_changed
        )
        self.views["⚙️ Settings"] = self.settings_view

        # Wire backwards-compatibility references
        self.api_entry = self.settings_view.api_entry
        self.test_key_btn = self.settings_view.test_key_btn
        self.key_status_label = self.settings_view.key_status_label
        self.api_status = self.settings_view.key_status_label
        self.vu_progress = self.settings_view.vu_progress
        self.dev_map = self.settings_view.dev_map

        # Other Modular Views
        self.views["🏠 Home"] = HomeView(
            parent=self.view_host,
            hub=self,
            config_manager=self.config,
            note_store=self.note_store,
            audio_recorder=self.audio_recorder,
            on_toggle_dictation=self.on_toggle_dictation
        )

        self.views["🕒 History"] = HistoryView(
            parent=self.view_host,
            hub=self,
            config_manager=self.config,
            note_store=self.note_store
        )

        self.views["📊 Productivity"] = ProductivityView(
            parent=self.view_host,
            hub=self,
            config_manager=self.config,
            note_store=self.note_store
        )

        self.views["📖 Vocabulary"] = VocabularyView(
            parent=self.view_host,
            hub=self,
            config_manager=self.config
        )

        self.views["⚡ Snippets"] = SnippetsView(
            parent=self.view_host,
            hub=self,
            config_manager=self.config,
            snippet_engine=self.snippet_engine
        )

        self.views["Aa Style"] = StyleView(
            parent=self.view_host,
            hub=self,
            config_manager=self.config
        )

        self.views["📱 Phone"] = PhoneView(
            parent=self.view_host,
            hub=self,
            config_manager=self.config,
            bridge_server=self.bridge_server
        )

    def navigate_to(self, tab_name: str):
        target = self._normalize_tab(tab_name)
        self.active_tab_name = target

        # Highlight active sidebar button
        for name, btn in self.nav_buttons.items():
            is_active = (name == target)
            if is_active:
                btn.configure(
                    fg_color=SIDEBAR_ACTIVE_BG,
                    text_color=SIDEBAR_ACTIVE_TEXT,
                    border_color=SIDEBAR_ACTIVE_BORDER,
                    border_width=1
                )
            else:
                btn.configure(
                    fg_color="transparent",
                    text_color=SIDEBAR_INACTIVE_TEXT,
                    border_width=0
                )

        # Show target view, hide others
        for name, view in self.views.items():
            if name == target:
                view.grid(row=0, column=0, sticky="nsew")
                # If history view, auto-focus search if desired
                if name == "🕒 History" and hasattr(view, "search_entry"):
                    self.window.after(50, lambda: view.search_entry.focus_set())
            else:
                view.grid_forget()

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
        if hasattr(self, "settings_view") and self.settings_view:
            self.settings_view._on_device_changed(choice)
            self.dev_map = self.settings_view.dev_map

    def _test_key(self):
        if hasattr(self, "settings_view") and self.settings_view:
            self.settings_view._test_key()

    def _test_connection(self):
        if hasattr(self, "settings_view") and self.settings_view:
            self.settings_view._test_speed()

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
