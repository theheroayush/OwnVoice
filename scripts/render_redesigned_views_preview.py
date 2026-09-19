"""
Renders a visual showcase of the 4 newly designed screens in the OwnVoice Desktop Hub:
1. Aa Style (Writing Style & Tone)
2. 📱 Phone (QR Code & Phone Mockup)
3. 📊 Productivity (Metrics & 7-Day Bar Chart)
4. 🕒 History (Date-Grouped Transcripts & Metadata Inspector)
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ARTIFACT_DIR = Path(r"C:\Users\ayush\.gemini\antigravity\brain\5ea4f4e3-15d7-4c21-9504-b5fbba15d970")
OUT_FILE = ARTIFACT_DIR / "desktop_views_preview.png"

def create_preview():
    W = 1280
    H = 960
    img = Image.new("RGBA", (W, H), (11, 15, 23, 255))
    draw = ImageDraw.Draw(img)

    # Header
    draw.rectangle([0, 0, W, 70], fill=(14, 19, 31, 255), outline=(26, 34, 52, 255), width=1)
    draw.text((32, 16), "🎙️ OwnVoice Desktop Hub — Multi-Page Redesign Showcase", fill=(248, 250, 252, 255))
    draw.text((32, 42), "Tier-1 Linear/Figma-Grade Polish • 4 New & Overhauled Screens", fill=(100, 116, 139, 255))

    QUAD_W = (W - 48) // 2
    QUAD_H = (H - 100) // 2

    # Panel 1: Aa Style (Top-Left)
    x1, y1 = 16, 85
    draw.rounded_rectangle([x1, y1, x1 + QUAD_W, y1 + QUAD_H], radius=10, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
    draw.text((x1 + 16, y1 + 14), "Aa Style — Tone & AI Polishing Profiles", fill=(248, 250, 252, 255))
    draw.text((x1 + 16, y1 + 34), "4 default tone profiles + app-specific override matrix", fill=(100, 116, 139, 255))

    # 4 Style cards
    styles = [("Casual", "Conversational", False), ("Natural", "Direct & balanced", True), ("Formal", "Polished & executive", False), ("Concise", "Punchy bulleted", False)]
    sc_w = (QUAD_W - 50) // 4
    for i, (name, desc, is_sel) in enumerate(styles):
        cx = x1 + 16 + i * (sc_w + 6)
        bg_c = (30, 32, 56, 255) if is_sel else (14, 19, 31, 255)
        border_c = (79, 70, 229, 255) if is_sel else (30, 41, 59, 255)
        draw.rounded_rectangle([cx, y1 + 60, cx + sc_w, y1 + 120], radius=6, fill=bg_c, outline=border_c, width=1)
        draw.text((cx + 8, y1 + 68), name, fill=(248, 250, 252, 255))
        draw.text((cx + 8, y1 + 88), desc[:15], fill=(148, 163, 184, 255))

    # App overrides table preview
    draw.rounded_rectangle([x1 + 16, y1 + 135, x1 + QUAD_W - 16, y1 + QUAD_H - 16], radius=6, fill=(14, 19, 31, 255), outline=(30, 41, 59, 255), width=1)
    draw.text((x1 + 24, y1 + 145), "APP-SPECIFIC WRITING STYLES", fill=(71, 85, 105, 255))
    rows = [
        ("WhatsApp", "🟢 WhatsApp", "Casual", "Hey, are you free for a quick call?"),
        ("VS Code", "💻 VS Code", "Concise", "refactor(engine): optimize token buffer"),
        ("Outlook", "✉️ Outlook", "Formal", "Please let me know your availability.")
    ]
    for idx, (app, label, tone, sample) in enumerate(rows):
        ry = y1 + 175 + idx * 45
        draw.text((x1 + 24, ry), label, fill=(248, 250, 252, 255))
        draw.rounded_rectangle([x1 + 160, ry - 4, x1 + 240, ry + 20], radius=4, fill=(30, 41, 59, 255))
        draw.text((x1 + 170, ry), tone, fill=(16, 185, 129, 255))
        draw.text((x1 + 255, ry), f'"{sample}"', fill=(148, 163, 184, 255))

    # Panel 2: 📱 Phone (Top-Right)
    x2, y2 = x1 + QUAD_W + 16, 85
    draw.rounded_rectangle([x2, y2, x2 + QUAD_W, y2 + QUAD_H], radius=10, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
    draw.text((x2 + 16, y2 + 14), "📱 Phone — Seamless Mobile Bridge & Discovery", fill=(248, 250, 252, 255))
    draw.text((x2 + 16, y2 + 34), "Real-time UDP beacon, glowing QR canvas & phone mockup", fill=(100, 116, 139, 255))

    # Feature pills
    pills = ["⚡ Direct PC typing", "🔒 Local Wi-Fi only", "🔁 Real-time streaming"]
    px = x2 + 16
    for p in pills:
        pw = len(p) * 8 + 16
        draw.rounded_rectangle([px, y2 + 56, px + pw, y2 + 78], radius=11, fill=(14, 19, 31, 255), outline=(30, 41, 59, 255), width=1)
        draw.text((px + 8, y2 + 61), p, fill=(56, 189, 248, 255))
        px += pw + 8

    # Left: QR canvas mockup
    draw.rounded_rectangle([x2 + 16, y2 + 90, x2 + 250, y2 + QUAD_H - 16], radius=8, fill=(14, 19, 31, 255), outline=(79, 70, 229, 255), width=1)
    draw.rectangle([x2 + 45, y2 + 105, x2 + 220, y2 + 260], fill=(255, 255, 255, 255))
    draw.text((x2 + 65, y2 + 170), "[ QR CODE ]", fill=(0, 0, 0, 255))
    draw.text((x2 + 30, y2 + 275), "PIN: 5 6 2 3 3 0", fill=(245, 158, 11, 255))
    draw.text((x2 + 30, y2 + 300), "Auto-refreshes in 4:52", fill=(100, 116, 139, 255))

    # Right: Phone Mockup
    phone_x = x2 + 275
    phone_w = QUAD_W - 295
    draw.rounded_rectangle([phone_x, y2 + 90, phone_x + phone_w, y2 + QUAD_H - 16], radius=16, fill=(14, 19, 31, 255), outline=(100, 116, 139, 255), width=2)
    draw.rounded_rectangle([phone_x + (phone_w - 60) // 2, y2 + 96, phone_x + (phone_w + 60) // 2, y2 + 106], radius=5, fill=(30, 41, 59, 255))
    draw.ellipse([phone_x + phone_w // 2 - 25, y2 + 170, phone_x + phone_w // 2 + 25, y2 + 220], outline=(79, 70, 229, 255), width=3)
    draw.text((phone_x + 20, y2 + 240), "Waiting for connection...", fill=(248, 250, 252, 255))
    draw.text((phone_x + 20, y2 + 265), "Open OwnVoice on your phone", fill=(100, 116, 139, 255))

    # Panel 3: 📊 Productivity (Bottom-Left)
    x3, y3 = 16, y1 + QUAD_H + 16
    draw.rounded_rectangle([x3, y3, x3 + QUAD_W, y3 + QUAD_H], radius=10, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
    draw.text((x3 + 16, y3 + 14), "📊 Productivity — Voice Analytics & Insights", fill=(248, 250, 252, 255))
    draw.text((x3 + 16, y3 + 34), "Live 7-day interactive bar chart, top apps, streaks & metrics", fill=(100, 116, 139, 255))

    # 4 Metric cards
    m_cards = [("1,284", "Words created", "+14%"), ("42", "Dictations", "+8%"), ("18 min", "Time saved", "3.2x"), ("4.2 kg", "CO2 offset", "Eco")]
    mc_w = (QUAD_W - 50) // 4
    for i, (val, label, trend) in enumerate(m_cards):
        cx = x3 + 16 + i * (mc_w + 6)
        draw.rounded_rectangle([cx, y3 + 60, cx + mc_w, y3 + 120], radius=6, fill=(14, 19, 31, 255), outline=(30, 41, 59, 255), width=1)
        draw.text((cx + 8, y3 + 68), val, fill=(248, 250, 252, 255))
        draw.text((cx + 8, y3 + 90), label[:12], fill=(100, 116, 139, 255))
        draw.text((cx + mc_w - 40, y3 + 68), trend, fill=(16, 185, 129, 255))

    # Interactive Bar Chart
    chart_y = y3 + 135
    chart_w = QUAD_W - 32
    draw.rounded_rectangle([x3 + 16, chart_y, x3 + 16 + chart_w, y3 + QUAD_H - 16], radius=6, fill=(14, 19, 31, 255), outline=(30, 41, 59, 255), width=1)
    days = [("Sep 12", 420), ("Sep 13", 680), ("Sep 14", 890), ("Sep 15", 510), ("Sep 16", 1284), ("Sep 17", 940), ("Sep 18", 1120)]
    bw = 36
    spacing = (chart_w - 40 - len(days) * bw) // (len(days) - 1)
    max_val = 1400
    for idx, (day_lbl, dval) in enumerate(days):
        bx = x3 + 36 + idx * (bw + spacing)
        bh = int((dval / max_val) * 120)
        by = chart_y + 165 - bh
        is_active = (idx == 4)
        bar_fill = (79, 70, 229, 255) if is_active else (30, 41, 59, 255)
        bar_border = (129, 140, 248, 255) if is_active else (51, 65, 85, 255)
        draw.rounded_rectangle([bx, by, bx + bw, chart_y + 165], radius=4, fill=bar_fill, outline=bar_border, width=1)
        draw.text((bx - 4, chart_y + 172), day_lbl, fill=(148, 163, 184, 255))
        if is_active:
            draw.rounded_rectangle([bx - 10, by - 26, bx + bw + 10, by - 6], radius=4, fill=(15, 23, 42, 255), outline=(79, 70, 229, 255), width=1)
            draw.text((bx - 4, by - 22), f"{dval} w", fill=(248, 250, 252, 255))

    # Panel 4: 🕒 History (Bottom-Right)
    x4, y4 = x3 + QUAD_W + 16, y1 + QUAD_H + 16
    draw.rounded_rectangle([x4, y4, x4 + QUAD_W, y4 + QUAD_H], radius=10, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
    draw.text((x4 + 16, y4 + 14), "🕒 History — Transcripts & Metadata Inspector", fill=(248, 250, 252, 255))
    draw.text((x4 + 16, y4 + 34), "Date headers, app badges, word counts & inspection drawer", fill=(100, 116, 139, 255))

    # Search & filters
    draw.rounded_rectangle([x4 + 16, y4 + 56, x4 + 220, y4 + 80], radius=6, fill=(14, 19, 31, 255), outline=(30, 41, 59, 255), width=1)
    draw.text((x4 + 26, y4 + 61), "🔍 Search dictations...", fill=(100, 116, 139, 255))

    # List items
    h_items = [
        ("TODAY", None, None, None, False),
        ("04:25 PM", "💬 WhatsApp", "Casual", "Hey, let me know if the presentation slides are ready.", True),
        ("02:15 PM", "💻 VS Code", "Concise", "refactor(audio): migrate to 16kHz mono ring buffer", False),
        ("YESTERDAY", None, None, None, False),
        ("11:30 AM", "✉️ Teams", "Formal", "Please find the architectural review document attached.", False)
    ]
    hy = y4 + 90
    for time_s, app_s, tone_s, snippet, is_active in h_items:
        if app_s is None:
            draw.text((x4 + 20, hy + 4), time_s, fill=(71, 85, 105, 255))
            hy += 24
        else:
            item_bg = (30, 32, 56, 255) if is_active else (14, 19, 31, 255)
            item_border = (79, 70, 229, 255) if is_active else (30, 41, 59, 255)
            draw.rounded_rectangle([x4 + 16, hy, x4 + QUAD_W - 16, hy + 38], radius=6, fill=item_bg, outline=item_border, width=1)
            draw.text((x4 + 24, hy + 10), time_s, fill=(100, 116, 139, 255))
            draw.text((x4 + 95, hy + 10), app_s, fill=(56, 189, 248, 255))
            draw.text((x4 + 200, hy + 10), snippet[:38] + "...", fill=(248, 250, 252, 255))
            hy += 44

    img.save(OUT_FILE)
    print(f"Showcase preview saved to {OUT_FILE} ({OUT_FILE.stat().st_size} bytes)")

if __name__ == "__main__":
    create_preview()
