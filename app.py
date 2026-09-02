import os
import sys
import time
import datetime
import threading
from pathlib import Path
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

LOG_FILE = Path(__file__).resolve().parent / "app.log"

def log_event(msg: str):
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    line = f"[{timestamp}] {msg}\n"
    print(line, end="")
    try:
        with open(LOG_FILE, "a", encoding="utf-8") as f:
            f.write(line)
    except Exception:
        pass

class OwnVoiceApp:
    def __init__(self):
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
            on_exit=self.exit_app
        )

    def reload_settings(self):
        self.hotkey_manager.stop()
        self.hotkey_manager.start()

    def on_recording_start(self):
        # 1. Capture target window
        self.injector.update_target_hwnd()

        # 2. App-Aware Context Detection
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
            dev_idx = self.config.get("input_device_index", 9)
            self.audio_recorder.start_recording(device_index=dev_idx)
        except Exception as e:
            log_event(f"Microphone error: {e}")
            if self.overlay.root:
                self.overlay.root.after(0, lambda: self.overlay.show_error("Mic Error"))

    def on_recording_stop(self):
        log_event("Recording stopped. Processing audio...")
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
        self.hotkey_manager.stop()
        self.tray.stop()
        if self.overlay.root:
            self.overlay.root.after(0, self.overlay.root.destroy)
        sys.exit(0)

    def run(self):
        self.hotkey_manager.start()
        self.tray.start()
        self.overlay.start_overlay()
        self.overlay.root.mainloop()

if __name__ == "__main__":
    app = OwnVoiceApp()
    app.run()
