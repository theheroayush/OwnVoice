"""
Renders a visual preview showcase of the new Tier-1 OwnVoice Desktop Hub
to the artifacts directory so the user can verify the design fidelity.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ARTIFACT_DIR = Path(r"C:\Users\ayush\.gemini\antigravity\brain\5ea4f4e3-15d7-4c21-9504-b5fbba15d970")
OUT_FILE = ARTIFACT_DIR / "desktop_hub_preview.png"

def render():
    WIDTH = 1080
    HEIGHT = 720
    img = Image.new("RGBA", (WIDTH, HEIGHT), (11, 15, 23, 255)) # #0B0F17
    draw = ImageDraw.Draw(img)

    # 1. Sidebar
    SIDEBAR_W = 220
    draw.rectangle([0, 0, SIDEBAR_W, HEIGHT], fill=(14, 19, 31, 255), outline=(26, 34, 52, 255), width=1)

    # Sidebar Brand
    draw.text((24, 24), "🎙️  OwnVoice", fill=(248, 250, 252, 255))
    draw.text((50, 48), "Speak. It writes.", fill=(100, 116, 139, 255))

    # Nav Items
    navs = [
        ("🏠 Home", True),
        ("🕒 History", False),
        ("--- YOUR VOICE ---", None),
        ("📖 Vocabulary", False),
        ("⚡ Snippets", False),
        ("Aa Style", False),
        ("--- DEVICES ---", None),
        ("📱 Phone", False),
        ("⚙️ Settings", False)
    ]
    y_nav = 90
    for name, is_active in navs:
        if is_active is None:
            draw.text((24, y_nav + 4), name.replace("---", "").strip(), fill=(71, 85, 105, 255))
            y_nav += 26
        else:
            if is_active:
                draw.rounded_rectangle([12, y_nav, SIDEBAR_W - 12, y_nav + 34], radius=6, fill=(30, 32, 56, 255), outline=(59, 61, 106, 255), width=1)
                draw.text((24, y_nav + 9), name, fill=(255, 255, 255, 255))
            else:
                draw.text((24, y_nav + 9), name, fill=(148, 163, 184, 255))
            y_nav += 40

    # Sidebar Bottom Status
    draw.rounded_rectangle([12, HEIGHT - 70, SIDEBAR_W - 12, HEIGHT - 24], radius=8, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
    draw.ellipse([24, HEIGHT - 52, 34, HEIGHT - 42], fill=(16, 185, 129, 255))
    draw.text((40, HEIGHT - 54), "Ready to listen", fill=(248, 250, 252, 255))
    draw.text((24, HEIGHT - 20), "OwnVoice v2.1.0 • Pro Edition", fill=(71, 85, 105, 255))

    # 2. Main Content Area
    X0 = SIDEBAR_W + 30
    
    # Top Bar
    draw.rounded_rectangle([X0, 16, X0 + 200, 44], radius=6, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
    draw.text((X0 + 12, 23), "⚡ Press F8 to speak anywhere", fill=(56, 189, 248, 255))
    draw.text((WIDTH - 240, 23), "Gemini Flash 3.6 • Latency <500ms", fill=(100, 116, 139, 255))

    # Greeting & Hero
    draw.text((X0, 64), "☀️ Good evening", fill=(56, 189, 248, 255))
    draw.text((X0, 84), "OwnVoice is ready.", fill=(248, 250, 252, 255))
    draw.text((X0, 112), "Press F8 and start speaking anywhere. Release to inject.", fill=(100, 116, 139, 255))

    # Hero Buttons
    draw.rounded_rectangle([X0, 140, X0 + 170, 178], radius=8, fill=(79, 70, 229, 255)) # Indigo CTA
    draw.text((X0 + 20, 150), "🎙️ Press F8 to speak", fill=(255, 255, 255, 255))
    draw.rounded_rectangle([X0 + 180, 140, X0 + 300, 178], radius=8, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
    draw.text((X0 + 200, 150), "▶️ How it works", fill=(203, 213, 225, 255))

    # Right Hero Listening Card
    HERO_RX = WIDTH - 340
    draw.rounded_rectangle([HERO_RX, 64, WIDTH - 30, 178], radius=12, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
    draw.text((HERO_RX + 16, 76), "((•)) Ready to Listen", fill=(16, 185, 129, 255))
    # Soundwave bars
    for i in range(24):
        bx = HERO_RX + 20 + i * 11
        bh = 8 + (i % 6) * 5
        by = 120 - bh // 2
        color = (79, 70, 229, 255) if i % 2 == 0 else (56, 189, 248, 255)
        draw.rounded_rectangle([bx, by, bx + 5, by + bh], radius=2, fill=color)
    draw.text((HERO_RX + 16, 150), "Tap F8 to start • Auto AI polishing", fill=(100, 116, 139, 255))

    # 3. 4 Metrics Cards
    Y_METRICS = 200
    CARD_W = (WIDTH - X0 - 30 - 36) // 4
    cards = [
        ("1,284", "Words today", "↑ 12%", (6, 78, 59, 255), (110, 231, 183, 255)),
        ("42", "Dictations", "↑ 8%", (6, 78, 59, 255), (110, 231, 183, 255)),
        ("18 min", "Time saved vs typing", "", None, None),
        ("🎯 Impact", "3.2 hrs saved this week", "", None, None),
    ]
    for idx, (val, lbl, trend, tbg, tfg) in enumerate(cards):
        cx = X0 + idx * (CARD_W + 12)
        draw.rounded_rectangle([cx, Y_METRICS, cx + CARD_W, Y_METRICS + 82], radius=10, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
        draw.text((cx + 14, Y_METRICS + 12), val, fill=(248, 250, 252, 255))
        draw.text((cx + 14, Y_METRICS + 46), lbl, fill=(100, 116, 139, 255))
        if trend and tbg:
            draw.rounded_rectangle([cx + 90, Y_METRICS + 44, cx + 134, Y_METRICS + 62], radius=4, fill=tbg)
            draw.text((cx + 95, Y_METRICS + 48), trend, fill=tfg)

    # 4. Recent Dictations (Left) & Quick Actions (Right)
    Y_BOTTOM = 306
    COL_L_W = (WIDTH - X0 - 30) * 3 // 5
    COL_R_W = (WIDTH - X0 - 30) * 2 // 5 - 16
    COL_R_X = X0 + COL_L_W + 16

    # Recent Dictations Header
    draw.text((X0, Y_BOTTOM), "Recent Dictations", fill=(248, 250, 252, 255))
    draw.text((X0 + COL_L_W - 70, Y_BOTTOM), "View all →", fill=(56, 189, 248, 255))

    recent_entries = [
        ("Today 2:45 PM • Smart Flow", "Let's move the architecture sync to 10 AM tomorrow to review the SQLite note store schemas."),
        ("Today 1:15 PM • Email", "Dear team, the release pipeline has completed with 100% test pass rate."),
        ("Yesterday 6:30 PM • Code", "def test_vu_monitor_lifecycle(self): self.assertTrue(settings.is_open)")
    ]
    y_rec = Y_BOTTOM + 28
    for ts, txt in recent_entries:
        draw.rounded_rectangle([X0, y_rec, X0 + COL_L_W, y_rec + 74], radius=10, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
        draw.text((X0 + 14, y_rec + 10), f"📄 {ts}", fill=(100, 116, 139, 255))
        draw.text((X0 + 14, y_rec + 34), txt[:68] + "...", fill=(203, 213, 225, 255))
        y_rec += 84

    # Quick Actions 2x2
    draw.text((COL_R_X, Y_BOTTOM), "Quick Actions", fill=(248, 250, 252, 255))
    actions = [
        ("📖 Add a word", "Save names & jargon"),
        ("⚡ New snippet", "Expand triggers"),
        ("📱 Connect phone", "Pair Android mic"),
        ("Aa Change style", "Smart flow / email / code")
    ]
    QA_W = (COL_R_W - 8) // 2
    QA_H = 68
    for idx, (title, sub) in enumerate(actions):
        r = idx // 2
        c = idx % 2
        ax = COL_R_X + c * (QA_W + 8)
        ay = Y_BOTTOM + 28 + r * (QA_H + 8)
        draw.rounded_rectangle([ax, ay, ax + QA_W, ay + QA_H], radius=8, fill=(19, 25, 38, 255), outline=(30, 41, 59, 255), width=1)
        draw.text((ax + 10, ay + 12), f"{title}  ›", fill=(248, 250, 252, 255))
        draw.text((ax + 10, ay + 36), sub, fill=(100, 116, 139, 255))

    # Pro Tip Banner
    TIP_Y = Y_BOTTOM + 28 + 2 * (QA_H + 8) + 8
    draw.rounded_rectangle([COL_R_X, TIP_Y, COL_R_X + COL_R_W, TIP_Y + 70], radius=8, fill=(19, 25, 38, 255), outline=(59, 61, 106, 255), width=1)
    draw.text((COL_R_X + 12, TIP_Y + 10), "💡 Pro Tip: Style adapts to your app", fill=(56, 189, 248, 255))
    draw.text((COL_R_X + 12, TIP_Y + 34), "OwnVoice auto-switches between email, code,\nand notes based on where you're typing.", fill=(100, 116, 139, 255))

    # Save artifact
    img.save(OUT_FILE)
    print(f"Generated preview at {OUT_FILE}")

if __name__ == "__main__":
    render()
