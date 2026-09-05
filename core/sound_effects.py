import threading

try:
    import winsound
except ImportError:
    winsound = None


class SoundEffects:
    def __init__(self, enabled=True):
        self.enabled = enabled

    def _play(self, freq: int, duration: int):
        if not self.enabled or not winsound:
            return

        def _run():
            try:
                winsound.Beep(freq, duration)
            except Exception:
                pass

        threading.Thread(target=_run, daemon=True).start()

    def play_start(self):
        """Crisp high tone on dictation start."""
        self._play(880, 90)

    def play_stop(self):
        """Soft lower tone on dictation stop."""
        self._play(587, 100)

    def play_success(self):
        """High affirmative chirp on successful text injection."""
        self._play(1046, 80)

    def play_error(self):
        """Low warning tone on error."""
        self._play(330, 160)


sound_effects = SoundEffects()
