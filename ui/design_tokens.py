"""
OwnVoice Desktop Hub — Design System Tokens
Strict tokens derived from the consumer Figma/Linear-grade design specifications.
"""

# Window Dimensions
WINDOW_WIDTH = 1080
WINDOW_HEIGHT = 720
MIN_WIDTH = 960
MIN_HEIGHT = 640

# Background Palette
BG_APP = "#0B0F17"           # Obsidian Dark base
BG_SIDEBAR = "#0E131F"       # Deep slate sidebar
BORDER_SIDEBAR = "#1A2234"   # Subtle partition border
BG_CARD = "#131926"          # Standard elevated surface
BG_CARD_HOVER = "#182133"    # Elevated hover surface
BORDER_CARD = "#1E293B"      # Card border

# Active / Selection Highlights
SIDEBAR_ACTIVE_BG = "#1E2038"
SIDEBAR_ACTIVE_BORDER = "#3B3D6A"
SIDEBAR_ACTIVE_TEXT = "#FFFFFF"
SIDEBAR_ACTIVE_ICON = "#818CF8"

SIDEBAR_INACTIVE_TEXT = "#94A3B8"
SIDEBAR_INACTIVE_HOVER = "#161D2E"

# Accent Colors
ACCENT_PRIMARY = "#4F46E5"        # Indigo primary button
ACCENT_PRIMARY_HOVER = "#4338CA"
ACCENT_VIOLET = "#6366F1"
ACCENT_SKY = "#38BDF8"
ACCENT_EMERALD = "#10B981"
ACCENT_AMBER = "#F59E0B"
ACCENT_CRIMSON = "#EF4444"
ACCENT_CRIMSON_BG = "#7F1D1D"

# Typography Colors
TEXT_PRIMARY = "#F8FAFC"
TEXT_SECONDARY = "#CBD5E1"
TEXT_MUTED = "#64748B"
TEXT_SUBTLE = "#475569"

# Badges & Tag Colors (Background, Text)
BADGE_COLORS = {
    "Name": {"bg": "#312E81", "fg": "#C7D2FE"},
    "Place": {"bg": "#064E3B", "fg": "#6EE7B7"},
    "Technology": {"bg": "#0C4A6E", "fg": "#7DD3FC"},
    "Company": {"bg": "#78350F", "fg": "#FCD34D"},
    "Jargon": {"bg": "#701A75", "fg": "#F5D0FE"},
    "Meeting": {"bg": "#3730A3", "fg": "#C7D2FE"},
    "Email": {"bg": "#1E293B", "fg": "#94A3B8"},
    "Work": {"bg": "#1E3A8A", "fg": "#93C5FD"},
    "Personal": {"bg": "#334155", "fg": "#CBD5E1"},
    "Link": {"bg": "#1E293B", "fg": "#60A5FA"},
    "Contact": {"bg": "#064E3B", "fg": "#34D399"},
    "Code": {"bg": "#1E1E2E", "fg": "#93C5FD"},
    "Text": {"bg": "#312E81", "fg": "#C7D2FE"},
    "Notes": {"bg": "#3730A3", "fg": "#C7D2FE"},
    "Others": {"bg": "#1E293B", "fg": "#94A3B8"},
}

def get_badge_colors(category_or_type: str):
    key = str(category_or_type).strip().capitalize()
    return BADGE_COLORS.get(key, {"bg": "#1E293B", "fg": "#94A3B8"})
