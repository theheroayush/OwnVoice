import threading
import pystray
from PIL import Image, ImageDraw

class TrayIcon:
    def __init__(self, on_open_settings, on_toggle_dictation, on_toggle_overlay, on_exit):
        self.on_open_settings = on_open_settings
        self.on_toggle_dictation = on_toggle_dictation
        self.on_toggle_overlay = on_toggle_overlay
        self.on_exit = on_exit
        self.icon = None

    def _create_image(self):
        width, height = 64, 64
        img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        draw.ellipse([4, 4, width - 4, height - 4], fill=(15, 23, 42, 255), outline=(56, 189, 248, 255), width=3)
        draw.rounded_rectangle([24, 14, 40, 36], radius=7, fill=(244, 244, 245, 255))
        draw.arc([18, 22, 46, 42], start=0, end=180, fill=(244, 244, 245, 255), width=3)
        draw.line([32, 42, 32, 50], fill=(244, 244, 245, 255), width=3)
        draw.line([22, 50, 42, 50], fill=(244, 244, 245, 255), width=3)
        draw.ellipse([44, 10, 56, 22], fill=(16, 185, 129, 255))
        return img

    def start(self):
        menu = pystray.Menu(
            pystray.MenuItem("🎙️ OwnVoice (F8)", None, enabled=False),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem("⚡ Toggle Dictation (F8)", lambda: self.on_toggle_dictation()),
            pystray.MenuItem("👁️ Show/Hide Pill", lambda: self.on_toggle_overlay()),
            pystray.MenuItem("⚙️ Settings", lambda: self.on_open_settings()),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem("❌ Exit", lambda: self.on_exit())
        )
        self.icon = pystray.Icon("OwnVoice", self._create_image(), "OwnVoice — Voice Dictation", menu)
        threading.Thread(target=self.icon.run, daemon=True).start()

    def stop(self):
        if self.icon:
            self.icon.stop()
