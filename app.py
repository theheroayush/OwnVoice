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
    desktop ('WinSta0' / 'Default'). Prevents UIPI / session isolation failures
    when launched from background tasks, services, or shortcuts.
    """
    try:
        import ctypes
        from ctypes import wintypes

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

class OwnVoiceApp:
    def __init__(self):
        ensure_default_desktop()
        log_event("Starting OwnVoice Engine...")
        self.config = config_manager
        self.audio_recorder = AudioRecorder()
        self.ai_engine = AIEngine(self.config)
        self.injector = CursorInjector(self.config)
        self.snippet_engine = SnippetEngine(self.config)
        self.overlay_visible = True
        self.active_context_mode = "smart_flow"
        self.active_context_label = ""

        self.overlay = FloatingWidget(
            get_volume_fn=self.audio_recorder.get_current_volume,
            on_click_toggle=self.toggle_dictation,
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
            on_settings_changed=self.reload_settings
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
            config_manager=self.config
        )

    def reload_settings(self):
        self.hotkey_manager.stop()
        self.hotkey_manager.start()

    def on_recording_start(self):
        # 1. Capture target window
        self.injector.update_target_hwnd()

        # 2. Yield Settings VU monitor to prevent hardware contention
        if self.settings_ui:
            self.settings_ui.stop_vu_monitor()

        # 3. App-Aware Context Detection
        if self.config.get("auto_context", True):
            self.active_context_mode, self.active_context_label = ContextDetector.detect_tone(self.injector.last_target_hwnd)
        else:
            self.active_context_mode = self.config.get("dictation_mode", "smart_flow")
            self.active_context_label = ""

        log_event(f"Recording started (F8/click) [Context: {self.active_context_label} -> {self.active_context_mode}]")

        if self.config.get("sound_effects", False):
            sound_effects.play_start()
        
        if self.overlay.root:
            self.overlay.root.after(0, self.overlay.show)
            self.overlay.root.after(0, lambda: self.overlay.show_recording(self.active_context_label))
            
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

        if self.overlay.root:
            self.overlay.root.after(0, self.overlay.show_processing)

        def process():
            try:
                audio_bytes = self.audio_recorder.stop_recording()
                log_event(f"Captured audio: {len(audio_bytes)} WAV bytes")
                
                if not audio_bytes or len(audio_bytes) < 1000:
                    log_event("Audio too short — returning to dock")
                    if self.overlay.root:
                        self.overlay.root.after(0, self.overlay.dock)
                    return

                # AI Transcription with App-Aware Tone
                text, latency = self.ai_engine.transcribe_audio(audio_bytes, mode=self.active_context_mode)
                log_event(f"Gemini transcription ({int(latency*1000)}ms): '{text}'")

                if not text:
                    log_event("No speech recognized")
                    if self.overlay.root:
                        self.overlay.root.after(0, lambda: self.overlay.show_error("No speech"))
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

                # Inject text into active search box / document
                self.injector.inject_text(expanded_text)
                log_event(f"Successfully injected: '{expanded_text[:40]}'")
                
                if self.config.get("sound_effects", False):
                    sound_effects.play_success()

                if self.overlay.root:
                    self.overlay.root.after(0, lambda: self.overlay.show_success(expanded_text))

            except Exception as e:
                log_event(f"Error during transcription: {e}")
                if self.overlay.root:
                    self.overlay.root.after(0, lambda: self.overlay.show_error(str(e)))
                if self.config.get("sound_effects", False):
                    sound_effects.play_error()

        threading.Thread(target=process, daemon=True).start()

    def cancel_dictation(self):
        log_event("Recording cancelled by user via ✕ button.")
        self.audio_recorder.stop_recording()
        if self.settings_ui and hasattr(self.settings_ui, "resume_vu_monitor"):
            self.settings_ui.resume_vu_monitor()
        if self.overlay.root:
            self.overlay.root.after(0, self.overlay.dock)

    def toggle_dictation(self):
        if self.audio_recorder.is_recording:
            self.on_recording_stop()
        else:
            self.on_recording_start()

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

    def open_settings(self):
        if self.overlay.root:
            self.overlay.root.after(0, self.settings_ui.show)

    def exit_app(self):
        log_event("Exiting OwnVoice...")
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

    def run(self):
        try:
            ensure_default_desktop()
            log_event("Starting HotkeyManager...")
            self.hotkey_manager.start()
            log_event("Starting Tray Icon...")
            self.tray.start()
            log_event("Starting Overlay...")
            self.overlay.start_overlay()
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

if __name__ == "__main__":
    try:
        ensure_default_desktop()
        app = OwnVoiceApp()
        app.run()
    except Exception as e:
        import traceback
        err = traceback.format_exc()
        try:
            (APP_BASE_DIR / "crash.log").write_text(err, encoding="utf-8")
        except Exception:
            pass
