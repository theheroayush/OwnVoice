import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUTPUT_DIR = os.path.dirname(os.path.abspath(__file__))
GRAPHICS_DIR = os.path.join(OUTPUT_DIR, "graphics")
SCREENSHOTS_DIR = os.path.join(GRAPHICS_DIR, "screenshots")

os.makedirs(SCREENSHOTS_DIR, exist_ok=True)

# Color Palette (Linear / Stripe Obsidian Dark)
COLOR_BG_DARK = (11, 15, 23)        # #0B0F17
COLOR_SURFACE = (18, 24, 38)        # #121826
COLOR_CARD = (26, 34, 53)           # #1A2235
COLOR_BORDER = (45, 55, 78)         # #2D374E
COLOR_PRIMARY_BLUE = (37, 99, 235)  # #2563EB
COLOR_CYAN = (56, 189, 248)         # #38BDF8
COLOR_EMERALD = (16, 185, 129)      # #10B981
COLOR_TEXT_PRIMARY = (255, 255, 255)
COLOR_TEXT_SECONDARY = (148, 163, 184) # #94A3B8
COLOR_TEXT_MUTED = (100, 116, 139)    # #64748B

def get_font(size, bold=False):
    # Try Windows system fonts
    font_names = [
        "C:\\Windows\\Fonts\\segoeuib.ttf" if bold else "C:\\Windows\\Fonts\\segoeui.ttf",
        "C:\\Windows\\Fonts\\arialbd.ttf" if bold else "C:\\Windows\\Fonts\\arial.ttf",
    ]
    for fn in font_names:
        if os.path.exists(fn):
            try:
                return ImageFont.truetype(fn, size)
            except Exception:
                pass
    return ImageFont.load_default()

def draw_rounded_rect(draw, bbox, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(bbox, radius=radius, fill=fill, outline=outline, width=width)

# ==============================================================================
# 1. APP ICON (512 x 512)
# ==============================================================================
def create_app_icon():
    size = (512, 512)
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Base rounded rect with smooth gradient feel
    padding = 16
    draw_rounded_rect(draw, [padding, padding, 512 - padding, 512 - padding], radius=110, fill=COLOR_BG_DARK, outline=COLOR_PRIMARY_BLUE, width=4)

    # Inner subtle glow circle
    cx, cy = 256, 256
    for r in range(160, 40, -10):
        alpha = int(25 * (1.0 - (r / 160.0)))
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(37, 99, 235, alpha))

    # Soundwave bars + Microphone silhouette in center
    wave_heights = [45, 90, 160, 230, 180, 120, 60]
    bar_width = 16
    gap = 18
    start_x = cx - ((len(wave_heights) * bar_width + (len(wave_heights) - 1) * gap) // 2)

    for i, h in enumerate(wave_heights):
        x = start_x + i * (bar_width + gap)
        top_y = cy - (h // 2)
        bot_y = cy + (h // 2)
        # Gradient colors across waves
        ratio = i / (len(wave_heights) - 1)
        r = int(COLOR_PRIMARY_BLUE[0] * (1 - ratio) + COLOR_CYAN[0] * ratio)
        g = int(COLOR_PRIMARY_BLUE[1] * (1 - ratio) + COLOR_CYAN[1] * ratio)
        b = int(COLOR_PRIMARY_BLUE[2] * (1 - ratio) + COLOR_CYAN[2] * ratio)
        draw_rounded_rect(draw, [x, top_y, x + bar_width, bot_y], radius=8, fill=(r, g, b, 255))

    # Glowing center spark
    draw.ellipse([cx - 8, cy - 8, cx + 8, cy + 8], fill=(255, 255, 255, 240))

    # Save 512x512 icon
    icon_path = os.path.join(GRAPHICS_DIR, "icon_512x512.png")
    # Convert RGBA to RGB with dark background for strict Play Store compliance (no transparency on app icon)
    final_icon = Image.new("RGB", size, COLOR_BG_DARK)
    final_icon.paste(img, (0, 0), img)
    final_icon.save(icon_path, "PNG")
    print(f"[+] Saved App Icon: {icon_path}")

# ==============================================================================
# 2. FEATURE GRAPHIC (1024 x 500)
# ==============================================================================
def create_feature_graphic():
    width, height = 1024, 500
    img = Image.new("RGB", (width, height), COLOR_BG_DARK)
    draw = ImageDraw.Draw(img)

    # Ambient gradient glow
    for r in range(400, 50, -25):
        alpha = int(22 * (1.0 - (r / 400.0)))
        draw.ellipse([width - 250 - r, height // 2 - r, width - 250 + r, height // 2 + r], fill=(37, 99, 235))

    # Subtle horizontal grid lines
    for y in range(0, height, 40):
        draw.line([(0, y), (width, y)], fill=(20, 26, 40), width=1)
    for x in range(0, width, 60):
        draw.line([(x, 0), (x, height)], fill=(20, 26, 40), width=1)

    # Left typography section
    font_tag = get_font(18, bold=True)
    font_h1 = get_font(52, bold=True)
    font_sub = get_font(22, bold=False)
    font_pill = get_font(16, bold=True)

    # Category Pill
    draw_rounded_rect(draw, [60, 60, 290, 96], radius=18, fill=COLOR_SURFACE, outline=COLOR_PRIMARY_BLUE, width=2)
    draw.text((80, 68), "AI VOICE PRODUCTIVITY", font=font_tag, fill=COLOR_CYAN)

    # Main Brand Name & Headline
    draw.text((60, 115), "OwnVoice", font=font_h1, fill=COLOR_TEXT_PRIMARY)
    draw.text((60, 185), "Whisper-Fast Voice Typing & PC Air-Typing", font=font_sub, fill=COLOR_TEXT_SECONDARY)
    draw.text((60, 220), "The executive-grade voice keyboard built for speed and privacy.", font=get_font(18), fill=COLOR_TEXT_MUTED)

    # Feature badges
    badges = [
        ("⚡ 48kHz Studio Mic", COLOR_PRIMARY_BLUE),
        ("💻 Air-Typing to PC", COLOR_CYAN),
        ("📝 Meeting Mode", COLOR_EMERALD),
        ("🔒 Zero Data Retention", (168, 85, 247))
    ]

    bx = 60
    by = 280
    for text, col in badges:
        text_bbox = draw.textbbox((0, 0), text, font=font_pill)
        pw = (text_bbox[2] - text_bbox[0]) + 30
        draw_rounded_rect(draw, [bx, by, bx + pw, by + 40], radius=20, fill=COLOR_SURFACE, outline=col, width=1)
        draw.text((bx + 15, by + 10), text, font=font_pill, fill=COLOR_TEXT_PRIMARY)
        bx += pw + 16
        if bx > 500:
            bx = 60
            by += 52

    # Right side: Visual representation of Keyboard + PC link
    card_x, card_y = 660, 70
    card_w, card_h = 300, 360
    draw_rounded_rect(draw, [card_x, card_y, card_x + card_w, card_y + card_h], radius=24, fill=COLOR_SURFACE, outline=COLOR_BORDER, width=2)

    # Card titlebar
    draw.line([(card_x, card_y + 44), (card_x + card_w, card_y + 44)], fill=COLOR_BORDER, width=1)
    draw.ellipse([card_x + 16, card_y + 16, card_x + 28, card_y + 28], fill=COLOR_PRIMARY_BLUE)
    draw.text((card_x + 36, card_y + 14), "Live Desktop Link", font=get_font(15, bold=True), fill=COLOR_TEXT_PRIMARY)

    # Card active typing demo
    draw_rounded_rect(draw, [card_x + 16, card_y + 60, card_x + card_w - 16, card_y + 150], radius=12, fill=COLOR_BG_DARK, outline=COLOR_PRIMARY_BLUE, width=1)
    draw.text((card_x + 26, card_y + 70), "Transcribing to VS Code...", font=get_font(13), fill=COLOR_CYAN)
    draw.text((card_x + 26, card_y + 94), "\"Refactor voice engine to", font=get_font(15, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((card_x + 26, card_y + 118), "native 48kHz stereo stream|\"", font=get_font(15, bold=True), fill=COLOR_TEXT_PRIMARY)

    # Metrics row
    draw_rounded_rect(draw, [card_x + 16, card_y + 170, card_x + 140, card_y + 240], radius=12, fill=COLOR_CARD)
    draw.text((card_x + 26, card_y + 180), "LATENCY", font=get_font(11, bold=True), fill=COLOR_TEXT_MUTED)
    draw.text((card_x + 26, card_y + 200), "< 0.2ms", font=get_font(20, bold=True), fill=COLOR_EMERALD)

    draw_rounded_rect(draw, [card_x + 155, card_y + 170, card_x + card_w - 16, card_y + 240], radius=12, fill=COLOR_CARD)
    draw.text((card_x + 165, card_y + 180), "ACCURACY", font=get_font(11, bold=True), fill=COLOR_TEXT_MUTED)
    draw.text((card_x + 165, card_y + 200), "99.4%", font=get_font(20, bold=True), fill=COLOR_CYAN)

    # Security badge
    draw_rounded_rect(draw, [card_x + 16, card_y + 260, card_x + card_w - 16, card_y + 335], radius=12, fill=COLOR_CARD, outline=COLOR_BORDER)
    draw.text((card_x + 26, card_y + 275), "🛡️ Local Wi-Fi Subnet Only", font=get_font(13, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((card_x + 26, card_y + 300), "Hardware-backed PIN authentication", font=get_font(11), fill=COLOR_TEXT_SECONDARY)

    feature_path = os.path.join(GRAPHICS_DIR, "feature_graphic_1024x500.png")
    img.save(feature_path, "PNG")
    print(f"[+] Saved Feature Graphic: {feature_path}")

# ==============================================================================
# 3. HIGH-FIDELITY PHONE SCREENSHOTS (1080 x 2400)
# ==============================================================================
def create_screenshot(filename, tag, title, subtitle, draw_mockup_func):
    width, height = 1080, 2400
    img = Image.new("RGB", (width, height), COLOR_BG_DARK)
    draw = ImageDraw.Draw(img)

    # Subtle ambient gradient
    for r in range(600, 100, -50):
        draw.ellipse([width//2 - r, 1200 - r, width//2 + r, 1200 + r], fill=(16, 22, 36))

    # Top Header Copy
    font_tag = get_font(32, bold=True)
    font_title = get_font(72, bold=True)
    font_sub = get_font(36, bold=False)

    # Tag Badge
    tag_bbox = draw.textbbox((0, 0), tag, font=font_tag)
    tag_w = tag_bbox[2] - tag_bbox[0] + 48
    draw_rounded_rect(draw, [width//2 - tag_w//2, 120, width//2 + tag_w//2, 185], radius=30, fill=COLOR_SURFACE, outline=COLOR_PRIMARY_BLUE, width=3)
    draw.text((width//2 - tag_w//2 + 24, 134), tag, font=font_tag, fill=COLOR_CYAN)

    # Big Headline
    draw.text((width//2, 230), title, font=font_title, fill=COLOR_TEXT_PRIMARY, anchor="mt")

    # Subtitle
    draw.text((width//2, 330), subtitle, font=font_sub, fill=COLOR_TEXT_SECONDARY, anchor="mt")

    # Phone Frame Container (Centering phone mock)
    phone_x = 90
    phone_y = 440
    phone_w = 900
    phone_h = 1860
    draw_rounded_rect(draw, [phone_x, phone_y, phone_x + phone_w, phone_y + phone_h], radius=70, fill=COLOR_SURFACE, outline=(60, 75, 105), width=8)

    # Phone Inner Screen
    screen_padding = 18
    sx = phone_x + screen_padding
    sy = phone_y + screen_padding
    sw = phone_w - 2 * screen_padding
    sh = phone_h - 2 * screen_padding
    draw_rounded_rect(draw, [sx, sy, sx + sw, sy + sh], radius=56, fill=COLOR_BG_DARK)

    # Speaker notch & camera
    draw.ellipse([width//2 - 14, sy + 25, width//2 + 14, sy + 53], fill=(20, 25, 38))

    # Draw specific screen content
    draw_mockup_func(draw, sx, sy, sw, sh)

    out_path = os.path.join(SCREENSHOTS_DIR, filename)
    img.save(out_path, "PNG")
    print(f"[+] Saved Screenshot: {out_path}")

# Mockup 1: Voice Keyboard
def draw_keyboard_mockup(draw, sx, sy, sw, sh):
    # App Bar
    draw.text((sx + 50, sy + 90), "Messages", font=get_font(42, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 50, sy + 145), "Rahul Sharma • Active now", font=get_font(24), fill=COLOR_EMERALD)

    # Chat Bubbles
    draw_rounded_rect(draw, [sx + 50, sy + 230, sx + 580, sy + 350], radius=24, fill=COLOR_CARD)
    draw.text((sx + 80, sy + 260), "Hey Ayush! Are we still moving", font=get_font(28), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 80, sy + 300), "forward with the live demo today?", font=get_font(28), fill=COLOR_TEXT_PRIMARY)

    # Voice Transcribed Bubble (User)
    draw_rounded_rect(draw, [sx + 240, sy + 390, sx + sw - 50, sy + 570], radius=24, fill=COLOR_PRIMARY_BLUE)
    draw.text((sx + 270, sy + 420), "Yes! Just finished calibrating the", font=get_font(28, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 270, sy + 460), "sound engine to 48kHz. Ready", font=get_font(28, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 270, sy + 500), "whenever you are! 🎙️", font=get_font(28, bold=True), fill=COLOR_TEXT_PRIMARY)

    # Active Input Box
    draw_rounded_rect(draw, [sx + 40, sy + 620, sx + sw - 40, sy + 740], radius=20, fill=COLOR_SURFACE, outline=COLOR_PRIMARY_BLUE, width=2)
    draw.text((sx + 70, sy + 660), "Speaking... \"Let's sync up at 5 PM|\"", font=get_font(30), fill=COLOR_CYAN)

    # --- KEYBOARD SECTION ---
    ky = sy + sh - 980
    draw_rounded_rect(draw, [sx, ky, sx + sw, sy + sh], radius=40, fill=(14, 19, 29))

    # Keyboard Toolbar
    draw_rounded_rect(draw, [sx + 40, ky + 25, sx + 250, ky + 85], radius=30, fill=COLOR_PRIMARY_BLUE)
    draw.text((sx + 75, ky + 40), "🎙️ Voice Mode", font=get_font(24, bold=True), fill=COLOR_TEXT_PRIMARY)

    draw_rounded_rect(draw, [sx + 270, ky + 25, sx + 480, ky + 85], radius=30, fill=COLOR_CARD)
    draw.text((sx + 300, ky + 40), "💻 Air-Typing", font=get_font(24, bold=True), fill=COLOR_TEXT_SECONDARY)

    draw_rounded_rect(draw, [sx + 500, ky + 25, sx + 680, ky + 85], radius=30, fill=COLOR_CARD)
    draw.text((sx + 535, ky + 40), "📋 Clipboard", font=get_font(24, bold=True), fill=COLOR_TEXT_SECONDARY)

    # Keys Row 1 (Q - P)
    keys_r1 = ["Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"]
    kw = (sw - 80) // 10
    kh = 120
    for i, k in enumerate(keys_r1):
        kx = sx + 40 + i * kw
        draw_rounded_rect(draw, [kx + 4, ky + 115, kx + kw - 4, ky + 115 + kh], radius=14, fill=COLOR_CARD)
        draw.text((kx + kw//2, ky + 115 + kh//2), k, font=get_font(36, bold=True), fill=COLOR_TEXT_PRIMARY, anchor="mm")

    # Keys Row 2 (A - L)
    keys_r2 = ["A", "S", "D", "F", "G", "H", "J", "K", "L"]
    r2_offset = kw // 2
    for i, k in enumerate(keys_r2):
        kx = sx + 40 + r2_offset + i * kw
        draw_rounded_rect(draw, [kx + 4, ky + 255, kx + kw - 4, ky + 255 + kh], radius=14, fill=COLOR_CARD)
        draw.text((kx + kw//2, ky + 255 + kh//2), k, font=get_font(36, bold=True), fill=COLOR_TEXT_PRIMARY, anchor="mm")

    # Keys Row 3 (Shift, Z-M, Backspace)
    draw_rounded_rect(draw, [sx + 40, ky + 395, sx + 40 + int(kw * 1.5) - 4, ky + 395 + kh], radius=14, fill=COLOR_SURFACE)
    draw.text((sx + 40 + int(kw * 0.75), ky + 395 + kh//2), "⇧", font=get_font(42), fill=COLOR_TEXT_PRIMARY, anchor="mm")

    keys_r3 = ["Z", "X", "C", "V", "B", "N", "M"]
    for i, k in enumerate(keys_r3):
        kx = sx + 40 + int(kw * 1.5) + i * kw
        draw_rounded_rect(draw, [kx + 4, ky + 395, kx + kw - 4, ky + 395 + kh], radius=14, fill=COLOR_CARD)
        draw.text((kx + kw//2, ky + 395 + kh//2), k, font=get_font(36, bold=True), fill=COLOR_TEXT_PRIMARY, anchor="mm")

    draw_rounded_rect(draw, [sx + sw - 40 - int(kw * 1.5) + 4, ky + 395, sx + sw - 40, ky + 395 + kh], radius=14, fill=COLOR_SURFACE)
    draw.text((sx + sw - 40 - int(kw * 0.75), ky + 395 + kh//2), "⌫", font=get_font(42), fill=COLOR_TEXT_PRIMARY, anchor="mm")

    # Keys Row 4 (Spacebar & Dictation Orb)
    draw_rounded_rect(draw, [sx + 40, ky + 535, sx + 220, ky + 535 + kh], radius=14, fill=COLOR_SURFACE)
    draw.text((sx + 130, ky + 535 + kh//2), "?123", font=get_font(30, bold=True), fill=COLOR_TEXT_PRIMARY, anchor="mm")

    draw_rounded_rect(draw, [sx + 240, ky + 535, sx + sw - 240, ky + 535 + kh], radius=14, fill=COLOR_CARD)
    draw.text((sx + sw//2, ky + 535 + kh//2), "English (US) • OwnVoice", font=get_font(26), fill=COLOR_TEXT_MUTED, anchor="mm")

    draw_rounded_rect(draw, [sx + sw - 220, ky + 535, sx + sw - 40, ky + 535 + kh], radius=14, fill=COLOR_PRIMARY_BLUE)
    draw.text((sx + sw - 130, ky + 535 + kh//2), "🎙️", font=get_font(36), fill=COLOR_TEXT_PRIMARY, anchor="mm")

# Mockup 2: Air-Typing to PC
def draw_airtyping_mockup(draw, sx, sy, sw, sh):
    draw.text((sx + 50, sy + 90), "Air-Typing Studio", font=get_font(42, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 50, sy + 145), "Connected to laptop • 192.168.29.53:8765", font=get_font(24), fill=COLOR_CYAN)

    # Active Link Status Card
    draw_rounded_rect(draw, [sx + 40, sy + 210, sx + sw - 40, sy + 430], radius=24, fill=COLOR_SURFACE, outline=COLOR_PRIMARY_BLUE, width=2)
    draw.text((sx + 70, sy + 245), "🟢 PAIRING ACTIVE (UDP 8766)", font=get_font(22, bold=True), fill=COLOR_EMERALD)
    draw.text((sx + 70, sy + 285), "Ayush's Laptop (Windows 11)", font=get_font(34, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 70, sy + 335), "Target Application: VS Code (main.py)", font=get_font(24), fill=COLOR_TEXT_SECONDARY)
    draw.text((sx + 70, sy + 375), "PIN: 211296 • Token: 00a853b843733798", font=get_font(22), fill=COLOR_TEXT_MUTED)

    # Big Microphone Dictation Capsule
    draw_rounded_rect(draw, [sx + 60, sy + 480, sx + sw - 60, sy + 880], radius=40, fill=COLOR_CARD, outline=COLOR_PRIMARY_BLUE, width=3)
    draw.ellipse([sx + sw//2 - 90, sy + 540, sx + sw//2 + 90, sy + 720], fill=COLOR_PRIMARY_BLUE)
    draw.text((sx + sw//2, sy + 630), "🎙️", font=get_font(72), fill=COLOR_TEXT_PRIMARY, anchor="mm")
    draw.text((sx + sw//2, sy + 760), "LISTENING...", font=get_font(32, bold=True), fill=COLOR_CYAN, anchor="mm")
    draw.text((sx + sw//2, sy + 810), "Speaking transmits directly to PC cursor", font=get_font(24), fill=COLOR_TEXT_SECONDARY, anchor="mm")

    # Real-time Telemetry Stats
    draw_rounded_rect(draw, [sx + 40, sy + 930, sx + sw//2 - 20, sy + 1150], radius=24, fill=COLOR_SURFACE)
    draw.text((sx + 70, sy + 965), "LATENCY", font=get_font(20, bold=True), fill=COLOR_TEXT_MUTED)
    draw.text((sx + 70, sy + 1015), "0.18ms", font=get_font(48, bold=True), fill=COLOR_EMERALD)
    draw.text((sx + 70, sy + 1085), "Instant injection", font=get_font(22), fill=COLOR_TEXT_SECONDARY)

    draw_rounded_rect(draw, [sx + sw//2 + 20, sy + 930, sx + sw - 40, sy + 1150], radius=24, fill=COLOR_SURFACE)
    draw.text((sx + sw//2 + 50, sy + 965), "AUDIO SAMPLING", font=get_font(20, bold=True), fill=COLOR_TEXT_MUTED)
    draw.text((sx + sw//2 + 50, sy + 1015), "48 kHz", font=get_font(48, bold=True), fill=COLOR_CYAN)
    draw.text((sx + sw//2 + 50, sy + 1085), "Studio quality", font=get_font(22), fill=COLOR_TEXT_SECONDARY)

    # Remote Keystrokes & Control Bar
    draw_rounded_rect(draw, [sx + 40, sy + 1200, sx + sw - 40, sy + 1440], radius=24, fill=COLOR_SURFACE)
    draw.text((sx + 70, sy + 1235), "QUICK REMOTE ACTIONS", font=get_font(20, bold=True), fill=COLOR_TEXT_MUTED)

    actions = [("⌫ Backspace", sx + 70), ("⏎ Enter", sx + 320), ("📋 Push Clip", sx + 540)]
    for act, ax in actions:
        draw_rounded_rect(draw, [ax, sy + 1290, ax + 200, sy + 1380], radius=16, fill=COLOR_CARD, outline=COLOR_BORDER)
        draw.text((ax + 100, sy + 1335), act, font=get_font(24, bold=True), fill=COLOR_TEXT_PRIMARY, anchor="mm")

# Mockup 3: Meeting Mode
def draw_meeting_mockup(draw, sx, sy, sw, sh):
    draw.text((sx + 50, sy + 90), "Meeting Studio", font=get_font(42, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 50, sy + 145), "Live Audio Synthesis & Action Items", font=get_font(24), fill=COLOR_EMERALD)

    # Meeting Info Card
    draw_rounded_rect(draw, [sx + 40, sy + 210, sx + sw - 40, sy + 380], radius=24, fill=COLOR_SURFACE)
    draw.text((sx + 70, sy + 245), "Q4 Strategy & Architecture Session", font=get_font(34, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 70, sy + 295), "Duration: 42m 18s • Participants: 4", font=get_font(24), fill=COLOR_TEXT_SECONDARY)
    draw.text((sx + 70, sy + 335), "Auto-sync target: PC Documents/OwnVoice/Meetings", font=get_font(20), fill=COLOR_CYAN)

    # Key Decisions Card
    draw_rounded_rect(draw, [sx + 40, sy + 420, sx + sw - 40, sy + 820], radius=24, fill=COLOR_CARD, outline=COLOR_PRIMARY_BLUE, width=2)
    draw.text((sx + 70, sy + 455), "📌 KEY DECISIONS", font=get_font(22, bold=True), fill=COLOR_CYAN)
    draw.text((sx + 70, sy + 510), "1. Eliminate 2.0s WebSocket latency via gemini-2.0-flash", font=get_font(26, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 70, sy + 555), "   Fallback directly to REST in < 0.15s if connection drops.", font=get_font(22), fill=COLOR_TEXT_SECONDARY)
    draw.text((sx + 70, sy + 620), "2. Calibrate two-thumb IME keyboard hitboxes", font=get_font(26, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 70, sy + 665), "   Isolated pointer tracking with 20dp touch slop.", font=get_font(22), fill=COLOR_TEXT_SECONDARY)
    draw.text((sx + 70, sy + 730), "3. Multi-threaded bridge server for concurrent typing", font=get_font(26, bold=True), fill=COLOR_TEXT_PRIMARY)

    # Prioritized Action Items Card
    draw_rounded_rect(draw, [sx + 40, sy + 860, sx + sw - 40, sy + 1280], radius=24, fill=COLOR_SURFACE)
    draw.text((sx + 70, sy + 895), "✅ ACTION ITEMS", font=get_font(22, bold=True), fill=COLOR_EMERALD)

    items = [
        ("Ayush", "Publish Play Store App Bundle & 512x512 icon", "HIGH"),
        ("Frontend", "Verify all 45 routes compile cleanly in Next.js", "DONE"),
        ("Android", "Purge legacy Purple80 palette for Obsidian theme", "DONE")
    ]
    iy = sy + 950
    for owner, task, status in items:
        draw_rounded_rect(draw, [sx + 70, iy, sx + sw - 70, iy + 90], radius=16, fill=COLOR_CARD)
        draw.text((sx + 95, iy + 25), f"[{owner}] {task}", font=get_font(24, bold=True), fill=COLOR_TEXT_PRIMARY)
        col = COLOR_EMERALD if status == "DONE" else COLOR_PRIMARY_BLUE
        draw.text((sx + sw - 190, iy + 25), status, font=get_font(22, bold=True), fill=col)
        iy += 105

    # Export & Sync Button
    draw_rounded_rect(draw, [sx + 40, sy + 1320, sx + sw - 40, sy + 1430], radius=24, fill=COLOR_PRIMARY_BLUE)
    draw.text((sx + sw//2, sy + 1375), "📥 Sync Markdown File to Laptop PC", font=get_font(30, bold=True), fill=COLOR_TEXT_PRIMARY, anchor="mm")

# Mockup 4: Context Tone
def draw_context_mockup(draw, sx, sy, sw, sh):
    draw.text((sx + 50, sy + 90), "Context-Aware AI Tone", font=get_font(42, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 50, sy + 145), "Automatic formatting for any app", font=get_font(24), fill=COLOR_CYAN)

    tones = [
        ("✨ Clean Polish", "Removes 'ums', stutters, and fixes grammar while keeping your natural voice intact.", True, COLOR_PRIMARY_BLUE),
        ("👔 Professional Email", "Formats bulleted status reports, polite salutations, and clear call-to-actions.", False, COLOR_CARD),
        ("💬 Casual Chat", "Short, snappy, conversational tone optimized for Slack, Discord, and WhatsApp.", False, COLOR_CARD),
        ("💻 Developer Code", "Converts spoken logic into clean docstrings, Git commit messages, or inline code.", False, COLOR_CARD)
    ]

    ty = sy + 220
    for name, desc, active, fill_col in tones:
        outline = COLOR_CYAN if active else COLOR_BORDER
        draw_rounded_rect(draw, [sx + 40, ty, sx + sw - 40, ty + 240], radius=24, fill=fill_col, outline=outline, width=3 if active else 1)
        draw.text((sx + 75, ty + 40), name, font=get_font(32, bold=True), fill=COLOR_TEXT_PRIMARY)
        draw.text((sx + 75, ty + 95), desc, font=get_font(24), fill=COLOR_TEXT_SECONDARY)
        if active:
            draw_rounded_rect(draw, [sx + sw - 230, ty + 35, sx + sw - 70, ty + 85], radius=16, fill=COLOR_CYAN)
            draw.text((sx + sw - 150, ty + 60), "ACTIVE", font=get_font(20, bold=True), fill=COLOR_BG_DARK, anchor="mm")
        ty += 265

    # Live Comparison Card
    draw_rounded_rect(draw, [sx + 40, ty + 30, sx + sw - 40, ty + 370], radius=24, fill=COLOR_SURFACE, outline=COLOR_BORDER)
    draw.text((sx + 70, ty + 60), "🎙️ Raw Spoken Input:", font=get_font(22, bold=True), fill=COLOR_TEXT_MUTED)
    draw.text((sx + 70, ty + 100), "\"uh hey rahul we need to like reschedule our demo", font=get_font(24), fill=COLOR_TEXT_SECONDARY)
    draw.text((sx + 70, ty + 135), "cause the audio engine isn't ready till tomorrow\"", font=get_font(24), fill=COLOR_TEXT_SECONDARY)

    draw.line([(sx + 70, ty + 190), (sx + sw - 70, ty + 190)], fill=COLOR_BORDER, width=1)

    draw.text((sx + 70, ty + 215), "⚡ Transformed Output:", font=get_font(22, bold=True), fill=COLOR_EMERALD)
    draw.text((sx + 70, ty + 255), "\"Hi Rahul, our demo is rescheduled for tomorrow", font=get_font(26, bold=True), fill=COLOR_TEXT_PRIMARY)
    draw.text((sx + 70, ty + 295), "after the audio engine completes final testing.\"", font=get_font(26, bold=True), fill=COLOR_TEXT_PRIMARY)

# ==============================================================================
# MAIN EXECUTION
# ==============================================================================
if __name__ == "__main__":
    print("[*] Generating Play Store Graphic Assets...")
    create_app_icon()
    create_feature_graphic()

    create_screenshot(
        "screenshot_1_voice_keyboard.png",
        "WHISPER-FAST TYPING",
        "AI Voice Keyboard",
        "Linear-grade obsidian design with zero missed keystrokes",
        draw_keyboard_mockup
    )

    create_screenshot(
        "screenshot_2_air_typing_pc.png",
        "STUDIO PC LINK",
        "Air-Typing to Laptop",
        "Turn your phone into a wireless studio mic for your computer",
        draw_airtyping_mockup
    )

    create_screenshot(
        "screenshot_3_meeting_mode.png",
        "EXECUTIVE NOTES",
        "Autonomous Meeting Studio",
        "Live audio transcription, key decisions & action items",
        draw_meeting_mockup
    )

    create_screenshot(
        "screenshot_4_context_tone.png",
        "CONTEXT-AWARE AI",
        "Adaptive Writing Tones",
        "Instant formatting for emails, Slack, notes, and code",
        draw_context_mockup
    )
    print("[SUCCESS] All Play Store graphic assets generated successfully!")
