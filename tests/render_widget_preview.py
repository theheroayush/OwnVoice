import os
import sys
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

APP_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(APP_DIR))

OUT_PATH = APP_DIR / "assets" / "screenshots" / "dynamic_bar_preview.png"
OUT_PATH.parent.mkdir(parents=True, exist_ok=True)

# Generate a high-resolution preview canvas of all 4 bar states:
# 1. DOCKED (Ambient Vector Mic, Branding, [📝 Note], [F8], ✕)
# 2. DOCKED (Hovering over Note Taker chip)
# 3. RECORDING (Active 8-bar audio waveform, live volume, cyan/violet)
# 4. NOTE TAKER RECORDING (Active 8-bar amber/emerald waveform, "Taking Notes...")

W = 200
H = 36
PADDING = 18
TOTAL_W = W + PADDING * 2
TOTAL_H = (H + PADDING) * 4 + PADDING

img = Image.new("RGBA", (TOTAL_W, TOTAL_H), (10, 14, 20, 255))
draw = ImageDraw.Draw(img)

def draw_pill_state(y_top, state="DOCKED", hovered="none", vol=0.6, is_note=False):
    x1, y1 = PADDING, y_top
    x2, y2 = x1 + W, y1 + H
    r = H // 2

    # Draw rounded rect body
    bg_col = (14, 18, 27, 255) if not is_note else (17, 20, 30, 255)
    border_col = (30, 41, 59, 255)
    if state == "RECORDING":
        border_col = (245, 158, 11, 255) if is_note else (14, 165, 233, 255)
        bg_col = (15, 18, 28, 255) if is_note else (8, 12, 20, 255)

    draw.rounded_rectangle([x1, y1, x2, y2], radius=r, fill=bg_col, outline=border_col, width=1)

    cy = y1 + H // 2

    if state == "DOCKED":
        # 1. Vector Mic Icon with Ambient Breathing Halo
        cx = x1 + 16
        # Halo
        draw.ellipse([cx - 10, cy - 10, cx + 10, cy + 10], outline=(2, 132, 199, 140), width=1)
        # Mic Capsule
        draw.rounded_rectangle([cx - 3.5, cy - 6, cx + 3.5, cy + 3], radius=3, fill=(56, 189, 248, 255))
        # Grille
        draw.line([cx - 2.5, cy - 3, cx + 2.5, cy - 3], fill=(11, 15, 23, 255), width=1)
        draw.line([cx - 2.5, cy - 1, cx + 2.5, cy - 1], fill=(11, 15, 23, 255), width=1)
        # Cradle arc
        draw.arc([cx - 6, cy - 4, cx + 6, cy + 6], start=180, end=360, fill=(148, 163, 184, 255), width=1)
        # Stem & Base
        draw.line([cx, cy + 6, cx, cy + 9], fill=(148, 163, 184, 255), width=1)
        draw.line([cx - 4, cy + 9, cx + 4, cy + 9], fill=(148, 163, 184, 255), width=1)

        # 2. Branding Text
        draw.text((x1 + 30, cy - 6), "OwnVoice", fill=(226, 232, 240, 255))

        # 3. Note Taker Chip
        nx1, ny1 = x1 + 96, y1 + 7
        nx2, ny2 = nx1 + 42, ny1 + 20
        note_border = (245, 158, 11, 255) if hovered == "note" else (31, 41, 61, 255)
        note_bg = (30, 41, 59, 255) if hovered == "note" else (17, 24, 39, 255)
        draw.rounded_rectangle([nx1, ny1, nx2, ny2], radius=4, fill=note_bg, outline=note_border, width=1)
        # Notepad icon & text
        draw.rectangle([nx1 + 5, ny1 + 4, nx1 + 11, ny2 - 4], fill=(15, 23, 42, 255), outline=(245, 158, 11, 255) if hovered == "note" else (100, 116, 139, 255))
        draw.text((nx1 + 15, cy - 5), "Note", fill=(253, 230, 138, 255) if hovered == "note" else (148, 163, 184, 255))

        # 4. Hotkey Chip
        hx1, hy1 = nx2 + 4, y1 + 7
        hx2, hy2 = hx1 + 24, hy1 + 20
        draw.rounded_rectangle([hx1, hy1, hx2, hy2], radius=4, fill=(22, 31, 48, 255), outline=(37, 50, 72, 255))
        draw.text((hx1 + 5, cy - 5), "F8", fill=(56, 189, 248, 255))

        # 5. Close Button
        draw.text((x2 - 16, cy - 6), "✕", fill=(239, 68, 68, 255) if hovered == "close" else (100, 116, 139, 255))

    elif state == "RECORDING":
        cx = x1 + 16
        if is_note:
            draw.ellipse([cx - 7, cy - 7, cx + 7, cy + 7], outline=(253, 230, 138, 180), width=1)
            draw.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(245, 158, 11, 255))
        else:
            draw.ellipse([cx - 7, cy - 7, cx + 7, cy + 7], outline=(252, 165, 165, 180), width=1)
            draw.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(239, 68, 68, 255))

        # 8-Bar Waveform
        bar_x = x1 + 30
        envelopes = [0.4, 0.7, 1.0, 0.85, 0.95, 0.75, 0.6, 0.35]
        palette_note = [(245, 158, 11), (251, 191, 36), (252, 211, 77), (52, 211, 153), (16, 185, 129), (16, 185, 129), (5, 150, 105), (5, 150, 105)]
        palette_dictate = [(56, 189, 248), (56, 189, 248), (96, 165, 250), (129, 140, 248), (129, 140, 248), (167, 139, 250), (192, 132, 252), (192, 132, 252)]
        palette = palette_note if is_note else palette_dictate

        for i in range(8):
            bh = max(4, int(vol * 20 * envelopes[i] + math.sin(i * 0.8) * 4 + 4))
            bx = bar_x + i * 5
            draw.line([bx, cy - bh // 2, bx, cy + bh // 2], fill=palette[i], width=2)

        # Label
        lbl = "Taking Notes..." if is_note else "Listening • Code"
        lbl_col = (254, 243, 199, 255) if is_note else (248, 250, 252, 255)
        draw.text((bar_x + 46, cy - 6), lbl, fill=lbl_col)
        draw.text((x2 - 16, cy - 6), "✕", fill=(100, 116, 139, 255))

# Draw 4 previews
draw_pill_state(PADDING, state="DOCKED", hovered="none")
draw_pill_state(PADDING + (H + PADDING), state="DOCKED", hovered="note")
draw_pill_state(PADDING + (H + PADDING) * 2, state="RECORDING", is_note=False)
draw_pill_state(PADDING + (H + PADDING) * 3, state="RECORDING", is_note=True)

img.save(str(OUT_PATH))
print(f"Rendered dynamic preview to {OUT_PATH}")
