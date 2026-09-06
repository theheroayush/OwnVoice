import threading
import queue

try:
    import winsound
except ImportError:
    winsound = None


class SoundEffects:
    def __init__(self, enabled=True):
        self.enabled = enabled
        self._queue = queue.Queue(maxsize=64)
        self._worker_thread = None
        self._lock = threading.Lock()

    def _ensure_worker(self):
        with self._lock:
            if self._worker_thread is None or not self._worker_thread.is_alive():
                self._worker_thread = threading.Thread(target=self._worker, daemon=True)
                self._worker_thread.start()

    def _worker(self):
        while True:
            try:
                item = self._queue.get()
                if item is None:
                    break
                freq, duration = item
                if winsound:
                    try:
                        winsound.Beep(freq, duration)
                    except Exception:
                        pass
                self._queue.task_done()
            except Exception:
                pass

    def _play(self, freq: int, duration: int):
        if not self.enabled or not winsound:
            return
        self._ensure_worker()
        try:
            self._queue.put_nowait((freq, duration))
        except queue.Full:
            pass

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

