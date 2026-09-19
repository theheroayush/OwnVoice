import os
import sys
import time
import datetime
import threading
from pathlib import Path

if getattr(sys, "frozen", False):
    APP_BASE_DIR = Path(sys.executable).resolve().parent
else:
    APP_BASE_DIR = Path(__file__).resolve().parent

os.chdir(APP_BASE_DIR)
sys.path.insert(0, str(APP_BASE_DIR))

from config import config_manager
from core.audio_recorder import AudioRecorder
from core.ai_engine import AIEngine
from core.injector import CursorInjector
from core.hotkey_manager import HotkeyManager
from core.sound_effects import sound_effects
from core.context_detector import ContextDetector
from core.snippet_engine import SnippetEngine
from ui.floating_widget import FloatingWidget
from ui.settings_window import SettingsWindow
from ui.tray_icon import TrayIcon
from core.bridge_server import BridgeServer, get_local_ip
from core.note_store import NoteStore
from core.note_engine import NoteEngine
from core.selection_reader import SelectionReader

LOG_FILE = APP_BASE_DIR / "app.log"


def log_event(msg: str):
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    line = f"[{timestamp}] {msg}\n"
    print(line, end="")
    try:
        with open(LOG_FILE, "a", encoding="utf-8") as f:
            f.write(line)
    except Exception:
        pass

def ensure_default_desktop() -> bool:
    """
    Guarantees the process and calling thread are bound to the user's interactive
    desktop ('WinSta0' / 'Default') on Windows. On non-Windows platforms, returns True.
    """
    if sys.platform != "win32":
        return True
    try:
        import ctypes
        from ctypes import wintypes

        if not hasattr(ctypes, "WinDLL"):
            return True

        u32 = ctypes.WinDLL("user32", use_last_error=True)

        ACCESS_MAX = 0x02000000  # MAXIMUM_ALLOWED

        u32.OpenWindowStationW.argtypes = [wintypes.LPCWSTR, wintypes.BOOL, wintypes.DWORD]
        u32.OpenWindowStationW.restype = wintypes.HANDLE
        u32.SetProcessWindowStation.argtypes = [wintypes.HANDLE]
        u32.SetProcessWindowStation.restype = wintypes.BOOL
        u32.OpenDesktopW.argtypes = [wintypes.LPCWSTR, wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
        u32.OpenDesktopW.restype = wintypes.HANDLE
        u32.SetThreadDesktop.argtypes = [wintypes.HANDLE]
        u32.SetThreadDesktop.restype = wintypes.BOOL

        h_winsta = u32.OpenWindowStationW("WinSta0", False, ACCESS_MAX)
        if h_winsta:
            u32.SetProcessWindowStation(h_winsta)

        h_desk = u32.OpenDesktopW("Default", 0, False, ACCESS_MAX)
        if h_desk:
            u32.SetThreadDesktop(h_desk)

        log_event("Interactive desktop bound successfully (WinSta0 / Default).")
        return bool(h_winsta and h_desk)
    except Exception as e:
        log_event(f"ensure_default_desktop notice: {e}")
        return False

_single_instance_mutex = None

def check_single_instance() -> bool:
    """
    Guarantees only one instance of OwnVoice runs at any given time.
    If an existing instance is active, prevents duplicate process and port collisions.
    """
    global _single_instance_mutex
    if sys.platform != "win32":
        return True
    try:
        import ctypes
        from ctypes import wintypes
        k32 = ctypes.windll.kernel32
        ERROR_ALREADY_EXISTS = 183
        MUTEX_NAME = "Global\\OwnVoice_SingleInstance_Mutex_Ayush"
        k32.CreateMutexW.argtypes = [wintypes.LPVOID, wintypes.BOOL, wintypes.LPCWSTR]
        k32.CreateMutexW.restype = wintypes.HANDLE
        _single_instance_mutex = k32.CreateMutexW(None, False, MUTEX_NAME)
        if k32.GetLastError() == ERROR_ALREADY_EXISTS:
            current_pid = os.getpid()
            log_event(f"OwnVoice mutex exists. Checking for active instance (current PID: {current_pid})...")
            # 1. Check if existing instance is responding on port 8765
            for _ in range(3):
                try:
                    import urllib.request
                    urllib.request.urlopen("http://127.0.0.1:8765/status", timeout=1.5)
                    log_event("Existing instance confirmed responsive on port 8765. Exiting duplicate process.")
                    return False
                except Exception:
                    time.sleep(0.4)
            # 2. Check if another OwnVoice process is alive
            try:
                import psutil
                other_procs = [
                    p for p in psutil.process_iter(['name', 'pid'])
                    if p.info['name'] and 'ownvoice' in p.info['name'].lower() and p.info['pid'] != current_pid
                ]
                if other_procs:
                    log_event(f"Another OwnVoice process is actively running (PID {other_procs[0].info['pid']}). Exiting duplicate process.")
                    return False
            except Exception as pe:
                log_event(f"Process inspection notice: {pe}")
            log_event("No other responsive OwnVoice instance found. Taking over...")
            return True
        return True
    except Exception as e:
        log_event(f"Single instance check notice: {e}")
        return True

class OwnVoiceApp:
    def __init__(self):
        ensure_default_desktop()
        log_event("Starting OwnVoice Engine...")
        self.config = config_manager
        self.audio_recorder = AudioRecorder()
        self.ai_engine = AIEngine(self.config)
        self.injector = CursorInjector(self.config)
        self.snippet_engine = SnippetEngine(self.config)
        self.note_store = NoteStore()
        self.note_engine = NoteEngine(self.config)
        self.selection_reader = SelectionReader()
        self.current_selection = None
        self.overlay_visible = True
        sound_effects.enabled = self.config.get("sound_effects", True)
        self.active_context_mode = "smart_flow"
        self.active_context_label = ""
        self.bridge_server = BridgeServer(
            port=8765,
            on_inject=self.injector.inject_text,
            api_key=self.config.get("google_api_key", ""),
            on_open_link=self.open_phone_link,
            note_store=self.note_store,
            note_engine=self.note_engine,
            selection_reader=self.selection_reader
        )
        self.bridge_server.start()

        self.is_note_session = False
        self.overlay = FloatingWidget(
            get_volume_fn=self.audio_recorder.get_current_volume,
            on_click_toggle=self.toggle_dictation,
            on_click_note_toggle=self.toggle_note_taker,
            on_open_settings=self.open_settings,
            on_cancel=self.cancel_dictation,
            on_hide=self.hide_overlay,
            on_exit=self.exit_app,
            injector=self.injector,
            config_manager=self.config
        )

        self.settings_ui = SettingsWindow(
            config_manager=self.config,
            ai_engine=self.ai_engine,
            audio_recorder=self.audio_recorder,
            snippet_engine=self.snippet_engine,
            on_settings_changed=self.reload_settings,
            bridge_server=self.bridge_server
        )

        self.hotkey_manager = HotkeyManager(
            on_start=self.on_recording_start,
            on_stop=self.on_recording_stop,
            config_manager=self.config
        )

        self.tray = TrayIcon(
            on_open_settings=self.open_settings,
            on_toggle_dictation=self.toggle_dictation,
            on_toggle_overlay=self.toggle_overlay,
            on_exit=self.exit_app,
            config_manager=self.config,
            on_open_phone_link=self.open_phone_link
        )

    def reload_settings(self):
        self.hotkey_manager.stop()
        self.hotkey_manager.start()

    def on_recording_start(self, is_note_mode: bool = False):
        self.is_note_session = is_note_mode
        # 1. Capture target window
        self.injector.update_target_hwnd()

        # 2. Check for highlighted on-screen text selection non-destructively
        try:
            self.current_selection = self.selection_reader.get_selected_text()
            if self.current_selection:
                log_event(f"Screen selection context captured ({len(self.current_selection)} chars): '{self.current_selection[:50]}...'")
        except Exception:
            self.current_selection = None

        # 3. Yield Settings VU monitor to prevent hardware contention
        if self.settings_ui:
            self.settings_ui.stop_vu_monitor()

        # 4. Mode Selection & App-Aware Context Detection
        if self.is_note_session:
            self.active_context_mode = "smart_note"
            self.active_context_label = "Note Mode"
        elif self.config.get("auto_context", True):
            self.active_context_mode, self.active_context_label = ContextDetector.detect_tone(self.injector.last_target_hwnd)
        else:
            self.active_context_mode = self.config.get("dictation_mode", "smart_flow")
            self.active_context_label = ""

        log_event(f"Recording started ({'Note Taker' if self.is_note_session else 'Dictation'}) [Context: {self.active_context_label} -> {self.active_context_mode}]")

        if self.config.get("sound_effects", False):
            sound_effects.play_start()
        
        if self.overlay.root:
            self.overlay.root.after(0, self.overlay.show)
            self.overlay.root.after(0, lambda: self.overlay.show_recording(self.active_context_label, is_note_mode=self.is_note_session))
            
        try:
            dev_idx = self.config.get("input_device_index", None)
            self.audio_recorder.start_recording(device_index=dev_idx)
            log_event(f"Audio stream started on Device {self.audio_recorder.active_device_index} ({self.audio_recorder.sample_rate}Hz)")
        except Exception as e:
            log_event(f"Microphone error: {e}")
            if self.overlay.root:
                self.overlay.root.after(0, lambda: self.overlay.show_error("Mic Error"))

    def on_recording_stop(self):
        log_event("Recording stopped. Processing audio...")
        if self.settings_ui and hasattr(self.settings_ui, "resume_vu_monitor"):
            self.settings_ui.resume_vu_monitor()
        if self.config.get("sound_effects", False):
            sound_effects.play_stop()

        is_note = getattr(self, "is_note_session", False)
        if self.overlay.root:
            self.overlay.root.after(0, lambda: self.overlay.show_processing(is_note_mode=is_note))

        def process():
            try:
                audio_bytes = self.audio_recorder.stop_recording()
                log_event(f"Captured audio: {len(audio_bytes)} WAV bytes")
                
                if not audio_bytes or len(audio_bytes) < 1000:
                    log_event("Audio too short — returning to dock")
                    if self.overlay.root:
                        self.overlay.root.after(0, self.overlay.dock)
                    self.is_note_session = False
                    return

                # If dedicated Note Taker session, compile structured note
                if is_note:
                    log_event("Compiling structured note with NoteEngine...")
                    note_dict = self.note_engine.structure_note(
                        audio_wav_bytes=audio_bytes,
                        selection_context=self.current_selection,
                        source="desktop_note_bar"
                    )
                    structured_text = note_dict.get("structured_content") or note_dict.get("summary") or ""
                    title = note_dict.get("title", "Voice Note")
                    nid = self.note_store.save_note(
                        title=title,
                        category=note_dict.get("category", "general"),
                        structured_content=structured_text,
                        raw_transcript=note_dict.get("raw_transcript", ""),
                        tags=note_dict.get("tags", []),
                        source="desktop_note_bar"
                    )
                    log_event(f"Structured note compiled and saved to SQLite (ID: {nid}): '{title}'")
                    self.injector.inject_text(structured_text)
                    if self.config.get("sound_effects", False):
                        sound_effects.play_success()
                    if self.overlay.root:
                        self.overlay.root.after(0, lambda: self.overlay.show_success("Note Saved!", is_note_mode=True))
                    self.is_note_session = False
                    return

                # AI Transcription with App-Aware Tone and Selection Context
                text, latency = self.ai_engine.transcribe_audio(
                    audio_bytes,
                    mode=self.active_context_mode,
                    selection_context=self.current_selection
                )
                log_event(f"Gemini transcription ({int(latency*1000)}ms): '{text}'")

                if not text:
                    log_event("No speech recognized")
                    if self.overlay.root:
                        self.overlay.root.after(0, lambda: self.overlay.show_error("No speech"))
                    self.is_note_session = False
                    return

                raw_trimmed = text.strip()
                norm_cmd = raw_trimmed.lower().rstrip(".!")

                # Intercept Hands-Free Vocal Erase Commands
                if raw_trimmed == "[COMMAND:DELETE_LAST]" or norm_cmd in ["scratch that", "delete that", "undo that", "erase that", "cancel that"]:
                    self.injector.erase_last()
                    log_event("Vocal edit executed: Erased last injection")
                    if self.config.get("sound_effects", False):
                        sound_effects.play_success()
                    if self.overlay.root:
                        self.overlay.root.after(0, lambda: self.overlay.show_success("Erased last entry"))
                    self.is_note_session = False
                    return

                if raw_trimmed == "[COMMAND:CLEAR_ALL]" or norm_cmd in ["clear all", "delete line", "clear text"]:
                    self.injector.erase_all()
                    log_event("Vocal edit executed: Cleared text")
                    if self.config.get("sound_effects", False):
                        sound_effects.play_success()
                    if self.overlay.root:
                        self.overlay.root.after(0, lambda: self.overlay.show_success("Cleared line"))
                    self.is_note_session = False
                    return

                # Voice Snippets Expansion
                expanded_text = self.snippet_engine.expand(text)
                if expanded_text != text:
                    log_event(f"Snippets expanded: '{expanded_text}'")

                self.config.add_history_entry({
                    "timestamp": datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "text": expanded_text,
                    "mode": self.active_context_mode,
                    "latency": latency
                })

                # Auto-save structured note if in note mode or multi-line document
                if self.active_context_mode in ("bullet_notes", "formal_document") or "\n" in expanded_text:
                    try:
                        title_line = expanded_text.splitlines()[0].lstrip("# -*").strip()[:50] or "Voice Note"
                        self.note_store.save_note(
                            title=title_line,
                            category="bullet_notes" if self.active_context_mode == "bullet_notes" else "general",
                            structured_content=expanded_text,
                            raw_transcript=text,
                            tags=["desktop", self.active_context_mode],
                            source="desktop_hotkey"
                        )
                    except Exception as ne:
                        log_event(f"Note auto-save notice: {ne}")

                # Inject text into active search box / document
                self.injector.inject_text(expanded_text)
                log_event(f"Successfully injected: '{expanded_text[:40]}'")
                
                if self.config.get("sound_effects", False):
                    sound_effects.play_success()

                if self.overlay.root:
                    self.overlay.root.after(0, lambda: self.overlay.show_success(expanded_text))
                self.is_note_session = False

            except Exception as e:
                log_event(f"Error during transcription: {e}")
                err_str = str(e)
                if "429" in err_str or "Quota" in err_str:
                    toast_msg = "Gemini Quota Exceeded (429)"
                elif "401" in err_str or "403" in err_str or "API Key" in err_str:
                    toast_msg = "Invalid Gemini API Key"
                elif "503" in err_str or "busy" in err_str.lower():
                    toast_msg = "Gemini Busy (503)"
                elif "timeout" in err_str.lower():
                    toast_msg = "Network Timeout"
                elif "404" in err_str:
                    toast_msg = "Model Unavailable (404)"
                else:
                    toast_msg = f"Error: {err_str[:20]}"

                if self.overlay.root:
                    self.overlay.root.after(0, lambda: self.overlay.show_error(toast_msg))
                if self.config.get("sound_effects", False):
                    sound_effects.play_error()
                self.is_note_session = False

        threading.Thread(target=process, daemon=True).start()

    def cancel_dictation(self):
        log_event("Recording cancelled by user via ✕ button.")
        self.audio_recorder.stop_recording()
        self.is_note_session = False
        if self.settings_ui and hasattr(self.settings_ui, "resume_vu_monitor"):
            self.settings_ui.resume_vu_monitor()
        if self.overlay.root:
            self.overlay.root.after(0, self.overlay.dock)

    def toggle_dictation(self):
        if self.audio_recorder.is_recording:
            self.on_recording_stop()
        else:
            self.on_recording_start(is_note_mode=False)

    def toggle_note_taker(self):
        if self.audio_recorder.is_recording:
            self.on_recording_stop()
        else:
            self.on_recording_start(is_note_mode=True)

    def hide_overlay(self):
        self.overlay_visible = False
        if self.overlay.root:
            self.overlay.root.after(0, self.overlay.hide)

    def toggle_overlay(self):
        if self.overlay_visible:
            self.hide_overlay()
        else:
            self.overlay_visible = True
            if self.overlay.root:
                self.overlay.root.after(0, self.overlay.show)

    def open_settings(self, tab=None):
        if self.overlay.root:
            self.overlay.root.after(0, lambda: self.settings_ui.show(initial_tab=tab))
        elif self.settings_ui:
            self.settings_ui.show(initial_tab=tab)

    def open_phone_link(self):
        self.open_settings(tab="📱 Phone Link")

    def exit_app(self):
        log_event("Exiting OwnVoice...")
        try:
            if hasattr(self, "bridge_server") and self.bridge_server:
                self.bridge_server.stop()
        except Exception:
            pass
        try:
            self.hotkey_manager.stop()
        except Exception:
            pass
        try:
            self.tray.stop()
        except Exception:
            pass
        try:
            if self.settings_ui:
                self.settings_ui._on_close()
        except Exception:
            pass
        try:
            if self.audio_recorder:
                self.audio_recorder.stop_monitoring()
        except Exception:
            pass
        try:
            if self.overlay:
                self.overlay.destroy()
        except Exception:
            pass
        os._exit(0)

    def run(self, open_link: bool = False):
        try:
            ensure_default_desktop()
            log_event("Starting HotkeyManager...")
            self.hotkey_manager.start()
            log_event("Starting Tray Icon...")
            self.tray.start()
            log_event("Starting Overlay...")
            self.overlay.start_overlay()

            if open_link or "--link" in sys.argv:
                log_event("Auto-opening Phone Link pairing window on startup...")
                if self.overlay.root:
                    self.overlay.root.after(350, self.open_phone_link)

            log_event("Entering mainloop...")
            self.overlay.root.mainloop()
        except Exception as e:
            import traceback
            err = traceback.format_exc()
            log_event(f"FATAL ERROR: {err}")
            try:
                (APP_BASE_DIR / "crash.log").write_text(err, encoding="utf-8")
            except Exception:
                pass
            raise

def run_bridge_only():
    """Runs the OwnVoice Ecosystem Bridge as a lightweight, cross-platform daemon."""
    if hasattr(sys.stdout, "reconfigure"):
        try:
            sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        except Exception:
            pass
    ensure_default_desktop()
    log_event("Starting OwnVoice in Headless Ecosystem Bridge Mode...")
    injector = CursorInjector(config_manager)
    server = BridgeServer(
        port=8765,
        on_inject=injector.inject_text,
        api_key=config_manager.get("google_api_key", "")
    )
    server.start()
    print("\n" + "=" * 60)
    print("  [*] OwnVoice Ecosystem Bridge Active (v2.5.0 Universal)")
    print(f"  Platform: {sys.platform.upper()}")
    print(f"  Local IP: {server.local_ip}:{server.port}")
    print(f"  UDP Auto-Discovery Beacon: Port 8766 Active")
    print(f"  Session Pairing PIN: {server.current_pin}")
    print("  Ready to stream text from your Android phone!")
    print("  Press Ctrl+C to terminate.")
    print("=" * 60 + "\n")
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopping bridge server...")
        server.stop()
        sys.exit(0)

if __name__ == "__main__":
    if "--bridge-only" in sys.argv or "--headless" in sys.argv:
        run_bridge_only()
    else:
        if not check_single_instance():
            if "--link" in sys.argv:
                try:
                    import urllib.request
                    urllib.request.urlopen("http://127.0.0.1:8765/open_link", timeout=1.5)
                except Exception:
                    pass
            sys.exit(0)
        try:
            ensure_default_desktop()
            app = OwnVoiceApp()
            app.run()
        except Exception as e:
            import traceback
            err = traceback.format_exc()
            log_event(f"FATAL ERROR: {err}")
            try:
                (APP_BASE_DIR / "crash.log").write_text(err, encoding="utf-8")
            except Exception:
                pass
