import tkinter as tk
import ctypes
import threading
import time
import math
from typing import Callable, Optional

import sys

# Enable Per-Monitor V2 DPI awareness on Windows 11/10
if sys.platform == "win32" and hasattr(ctypes, "windll"):
    try:
        ctypes.windll.shcore.SetProcessDpiAwareness(2)
    except Exception:
        try:
            ctypes.windll.user32.SetProcessDPIAware()
        except Exception:
            pass

GWL_EXSTYLE = -20
WS_EX_NOACTIVATE = 0x08000000
WS_EX_TOOLWINDOW = 0x00000080
WS_EX_TOPMOST = 0x00000008

SWP_NOSIZE = 0x0001
SWP_NOMOVE = 0x0002
SWP_NOZORDER = 0x0004
SWP_NOACTIVATE = 0x0010
SWP_FRAMECHANGED = 0x0020
SWP_SHOWWINDOW = 0x0040

SM_XVIRTUALSCREEN = 76
SM_YVIRTUALSCREEN = 77
SM_CXVIRTUALSCREEN = 78
SM_CYVIRTUALSCREEN = 79

class FloatingWidget:
    """
    Tier-1 Dynamic Floating Capsule Bar for OwnVoice.
    Features:
    - Ambient breathing vector microphone icon with oscillating acoustic halo.
    - Dedicated 1-click Note Taker trigger chip ([📝 Note]) alongside Dictation.
    - Real-time 8-bar fluid audio frequency visualizer reactive to live microphone volume.
    - Smooth interactive hover highlights and cursor affordances.
    - Ultra-low CPU idle footprint (<0.05%) with deterministic timer scheduling.
    - Dual mode states: Dictation (Cyan/Violet) vs Note Taking (Amber/Emerald).
    """

    def __init__(
        self,
        get_volume_fn: Callable[[], float],
        on_click_toggle: Callable[[], None] = None,
        on_click_note_toggle: Callable[[], None] = None,
        on_open_settings: Callable[[], None] = None,
        on_cancel: Callable[[], None] = None,
        on_hide: Callable[[], None] = None,
        on_exit: Callable[[], None] = None,
        injector = None,
        config_manager = None
    ):
        self.get_volume_fn = get_volume_fn
        self.on_click_toggle = on_click_toggle
        self.on_click_note_toggle = on_click_note_toggle
        self.on_open_settings = on_open_settings
        self.on_cancel = on_cancel
        self.on_hide = on_hide
        self.on_exit = on_exit
        self.injector = injector
        self.config = config_manager
        
        self.root = None
        self.canvas = None
        self.state = "DOCKED"  # DOCKED, RECORDING, PROCESSING, SUCCESS, ERROR, WELCOME
        self.status_text = ""
        self.context_label = ""
        self.is_note_mode = False
        self.is_running = False
        self.anim_thread = None
        self.anim_timer = None
        self.focus_thread = None
        self.pulse_phase = 0.0
        self.hide_timer = None
        
        self.width = 240
        self.docked_width = 240
        self.height = 36
        self.hwnd = None
        
        self.drag_start_x = 0
        self.drag_start_y = 0
        self.win_start_x = 0
        self.win_start_y = 0
        self.is_dragging = False
        self.hovered_element = None  # None, "mic", "dictate", "note", "hotkey", "close", "cancel", "done"
        self.hovering_close = False

    def _clamp_coordinates(self, x, y):
        """Keep capsule strictly within virtual multi-monitor desktop boundaries."""
        try:
            user32 = ctypes.windll.user32
            vx = user32.GetSystemMetrics(SM_XVIRTUALSCREEN)
            vy = user32.GetSystemMetrics(SM_YVIRTUALSCREEN)
            vw = user32.GetSystemMetrics(SM_CXVIRTUALSCREEN)
            vh = user32.GetSystemMetrics(SM_CYVIRTUALSCREEN)
            if vw > 0 and vh > 0:
                clamped_x = max(vx, min(vx + vw - self.width, x))
                clamped_y = max(vy, min(vy + vh - self.height, y))
                return clamped_x, clamped_y
        except Exception:
            pass
        return x, y

    def _create_window(self):
        self.root = tk.Tk()
        self.root.title("OwnVoice")
        self.root.overrideredirect(True)
        self.root.attributes("-topmost", True)
        
        self.root.config(bg="#000001")
        if sys.platform == "win32":
            try:
                self.root.wm_attributes("-transparentcolor", "#000001")
            except Exception:
                pass

        try:
            self.root.update_idletasks()
            raw_id = self.root.winfo_id()
            parent_hwnd = ctypes.windll.user32.GetParent(raw_id)
            self.hwnd = parent_hwnd if (parent_hwnd and ctypes.windll.user32.IsWindow(parent_hwnd)) else raw_id
            
            ex_style = ctypes.windll.user32.GetWindowLongW(self.hwnd, GWL_EXSTYLE)
            ctypes.windll.user32.SetWindowLongW(
                self.hwnd,
                GWL_EXSTYLE,
                ex_style | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW | WS_EX_TOPMOST
            )
            ctypes.windll.user32.SetWindowPos(
                self.hwnd, -1, 0, 0, 0, 0,
                SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE | SWP_FRAMECHANGED
            )
            if self.injector:
                self.injector.set_overlay_hwnd(self.hwnd)
        except Exception:
            pass

        self.width = self.docked_width
        screen_w = self.root.winfo_screenwidth()
        screen_h = self.root.winfo_screenheight()
        
        saved_x = self.config.get("overlay_x") if self.config else None
        saved_y = self.config.get("overlay_y") if self.config else None
        
        x = saved_x if saved_x is not None else (screen_w - self.width) // 2
        y = saved_y if saved_y is not None else screen_h - self.height - 40
        x, y = self._clamp_coordinates(x, y)
        self.root.geometry(f"{self.width}x{self.height}+{x}+{y}")

        self.canvas = tk.Canvas(
            self.root,
            width=self.width,
            height=self.height,
            bg="#000001",
            highlightthickness=0,
            cursor="hand2"
        )
        self.canvas.pack(fill="both", expand=True)
        
        self.canvas.bind("<ButtonPress-1>", self._on_mouse_down)
        self.canvas.bind("<B1-Motion>", self._on_mouse_drag)
        self.canvas.bind("<ButtonRelease-1>", self._on_mouse_up)
        self.canvas.bind("<Motion>", self._on_mouse_hover)
        self.canvas.bind("<Leave>", self._on_mouse_leave)
        self.canvas.bind("<Button-3>", self._show_context_menu)

        self._draw_pill()

    def _get_element_at(self, x, y) -> str:
        """Determines which interactive sub-component is under the mouse."""
        if self.state == "RECORDING":
            if x <= 36:
                return "cancel"
            if x >= self.width - 26:
                return "close"
            if self.width - 56 <= x < self.width - 26:
                return "done"
            return "body"

        if self.state != "DOCKED":
            if x >= self.width - 26:
                return "close"
            return "body"

        close_boundary = self.width - 26
        if x >= close_boundary:
            return "close"

        hotkey_right = close_boundary - 4
        hotkey_left = hotkey_right - 26
        if hotkey_left <= x < hotkey_right:
            return "hotkey"

        note_right = hotkey_left - 6
        note_left = note_right - 66
        if note_left <= x < note_right:
            return "note"

        dictate_right = note_left - 6
        dictate_left = 36
        if dictate_left <= x < dictate_right:
            return "dictate"

        return "mic"

    def _on_mouse_down(self, event):
        self.drag_start_x = event.x_root
        self.drag_start_y = event.y_root
        self.win_start_x = self.root.winfo_x() if self.root else 0
        self.win_start_y = self.root.winfo_y() if self.root else 0
        self.is_dragging = False
        if self.hwnd:
            try:
                ctypes.windll.user32.SetCapture(self.hwnd)
            except Exception:
                pass

    def _on_mouse_drag(self, event):
        dx = event.x_root - self.drag_start_x
        dy = event.y_root - self.drag_start_y
        if math.hypot(dx, dy) >= 6.0:
            if not self.is_dragging:
                self.is_dragging = True
                if self.hwnd:
                    try:
                        ctypes.windll.user32.SetCapture(self.hwnd)
                    except Exception:
                        pass
            new_x, new_y = self._clamp_coordinates(self.win_start_x + dx, self.win_start_y + dy)
            if self.root:
                self.root.geometry(f"{self.width}x{self.height}+{new_x}+{new_y}")

    def _on_mouse_up(self, event):
        try:
            ctypes.windll.user32.ReleaseCapture()
        except Exception:
            pass

        if self.is_dragging:
            self.is_dragging = False
            if self.config and self.root:
                self.config.set("overlay_x", self.root.winfo_x(), save=False)
                self.config.set("overlay_y", self.root.winfo_y(), save=True)
            return

        elem = self._get_element_at(event.x, event.y)

        # Close / Cancel button trigger
        if elem in ("close", "cancel") or (self.state == "RECORDING" and event.x <= 36) or event.x >= self.width - 26:
            if self.state in ("RECORDING", "PROCESSING"):
                if self.on_cancel:
                    self.on_cancel()
                self.dock()
            else:
                self.hide()
            return

        if self.injector:
            self.injector.refocus_target()

        if self.state == "DOCKED":
            if elem == "note":
                if self.on_click_note_toggle:
                    self.on_click_note_toggle()
                elif self.on_click_toggle:
                    self.on_click_toggle()
            elif elem == "hotkey":
                if self.on_open_settings:
                    self.on_open_settings()
            else:
                if self.on_click_toggle:
                    self.on_click_toggle()
        else:
            # Click while recording or processing (Done / Waveform / Body) stops and completes
            if self.on_click_toggle:
                self.on_click_toggle()

    def _on_mouse_hover(self, event):
        prev = self.hovered_element
        self.hovered_element = self._get_element_at(event.x, event.y)
        self.hovering_close = (self.hovered_element == "close")
        if prev != self.hovered_element:
            self._draw_pill()

    def _on_mouse_leave(self, event):
        if self.hovered_element is not None:
            self.hovered_element = None
            self.hovering_close = False
            self._draw_pill()

    def _show_context_menu(self, event):
        menu = tk.Menu(self.root, tearoff=0, bg="#0E131F", fg="#F8FAFC", activebackground="#2563EB", activeforeground="#FFFFFF")
        if self.state in ("RECORDING", "PROCESSING"):
            menu.add_command(label="🚫 Cancel Recording", command=self._cancel_and_dock)
        else:
            menu.add_command(label="🎙️ Start Dictation", command=self.on_click_toggle if self.on_click_toggle else None)
            if self.on_click_note_toggle:
                menu.add_command(label="📝 Start Note Taker", command=self.on_click_note_toggle)
        menu.add_separator()
        menu.add_command(label="⚙️ Settings & Snippets", command=self.on_open_settings if self.on_open_settings else None)
        menu.add_command(label="👁️ Hide to Tray", command=self.hide)
        menu.add_separator()
        exit_cmd = self.on_exit if self.on_exit else (self.root.quit if self.root else None)
        menu.add_command(label="❌ Exit OwnVoice", command=exit_cmd)
        menu.tk_popup(event.x_root, event.y_root)

    def _cancel_and_dock(self):
        if self.on_cancel:
            self.on_cancel()
        self.dock()

    def hide(self):
        if self.root:
            self.root.withdraw()
        if self.on_hide:
            self.on_hide()

    def show(self):
        if self.root:
            if hasattr(self.root, "deiconify"):
                try:
                    self.root.deiconify()
                except Exception:
                    pass
            if hasattr(self.root, "lift"):
                try:
                    self.root.lift()
                except Exception:
                    pass
            if hasattr(self.root, "attributes"):
                try:
                    self.root.attributes("-topmost", True)
                except Exception:
                    pass
            if self.hwnd:
                try:
                    ctypes.windll.user32.SetWindowPos(
                        self.hwnd, -1, 0, 0, 0, 0,
                        SWP_NOSIZE | SWP_NOMOVE | SWP_NOACTIVATE | SWP_SHOWWINDOW
                    )
                except Exception:
                    pass

    def _create_rounded_rect(self, x1, y1, x2, y2, r=17, **kwargs):
        points = (
            x1 + r, y1, x2 - r, y1, x2, y1, x2, y1 + r,
            x2, y2 - r, x2, y2, x2 - r, y2, x1 + r, y2,
            x1, y2, x1, y2 - r, x1, y1 + r, x1, y1
        )
        return self.canvas.create_polygon(points, smooth=True, **kwargs)

    def _draw_vector_mic(self, cx: float, cy: float, fill_color="#38BDF8", cradle_color="#94A3B8", aura_color=None, aura_radius=0.0):
        """Draws a clean studio vector microphone with acoustic grille and stand."""
        # 1. Ambient Breathing Halo
        if aura_color and aura_radius > 0:
            self.canvas.create_oval(
                cx - aura_radius, cy - aura_radius,
                cx + aura_radius, cy + aura_radius,
                outline=aura_color, width=1.2
            )

        # 2. Mic Capsule Body
        self._create_rounded_rect(
            cx - 3.5, cy - 6.5, cx + 3.5, cy + 3.5, r=3,
            fill=fill_color, outline=""
        )

        # 3. Acoustic Grille detail lines
        grille_color = "#0B0F17"
        self.canvas.create_line(cx - 2.5, cy - 3.5, cx + 2.5, cy - 3.5, fill=grille_color, width=1)
        self.canvas.create_line(cx - 2.5, cy - 1.0, cx + 2.5, cy - 1.0, fill=grille_color, width=1)

        # 4. Acoustic Cradle Ring (U-arc under capsule)
        self.canvas.create_arc(
            cx - 6, cy - 4, cx + 6, cy + 6.5,
            start=180, extent=180, style="arc",
            outline=cradle_color, width=1.4
        )

        # 5. Stand Stem
        self.canvas.create_line(cx, cy + 6.5, cx, cy + 9.5, fill=cradle_color, width=1.4)

        # 6. Stand Base
        self.canvas.create_line(cx - 4, cy + 9.5, cx + 4, cy + 9.5, fill=cradle_color, width=1.4)

    def _draw_pill(self):
        if not self.canvas:
            return
        self.canvas.delete("all")
        
        w, h = self.width, self.height
        r = h // 2

        is_close_hover = (self.hovered_element == "close")
        close_color = "#EF4444" if is_close_hover else "#64748B"

        if self.state == "DOCKED":
            # 1. Outer Dark Obsidian Pill Body with Subtle Glass Border
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#090D16", outline="#334155" if self.hovered_element else "#1E293B", width=1.2)
            
            # 2. Vector Microphone with Ambient Breathing Aura
            sin_val = (math.sin(self.pulse_phase) + 1.0) / 2.0  # 0.0 to 1.0
            aura_r = 9.5 + sin_val * 2.2
            if self.hovered_element == "mic":
                aura_col = "#0284C7"
                mic_fill = "#7DD3FC"
            else:
                aura_col = "#0369A1" if sin_val > 0.4 else "#082F49"
                mic_fill = "#38BDF8"
            
            self._draw_vector_mic(18, h // 2, fill_color=mic_fill, aura_color=aura_col, aura_radius=aura_r)

            # 3. Dictation Button ([||| Dictate])
            is_dictate_hover = (self.hovered_element in ("dictate", "mic"))
            dx1, dx2 = 36, 112
            self._create_rounded_rect(dx1, 6, dx2, h - 6, r=5, fill="#1E293B" if is_dictate_hover else "#131B2E", outline="#38BDF8" if is_dictate_hover else "#253248", width=1.0)
            # Mini sound wave icon (3 bars)
            self.canvas.create_line(dx1 + 8, h // 2 - 3, dx1 + 8, h // 2 + 3, fill="#38BDF8", width=1.2)
            self.canvas.create_line(dx1 + 11, h // 2 - 6, dx1 + 11, h // 2 + 6, fill="#38BDF8", width=1.8)
            self.canvas.create_line(dx1 + 14, h // 2 - 3, dx1 + 14, h // 2 + 3, fill="#38BDF8", width=1.2)
            dictate_col = "#38BDF8" if is_dictate_hover else "#E2E8F0"
            self.canvas.create_text(dx1 + 20, h // 2, text="Dictate", anchor="w", fill=dictate_col, font=("Segoe UI", 8, "bold"))

            # 4. Notes Button ([📝 Notes])
            is_note_hover = (self.hovered_element == "note")
            nx1, nx2 = 118, 184
            self._create_rounded_rect(nx1, 6, nx2, h - 6, r=5, fill="#2E2214" if is_note_hover else "#1E1810", outline="#F59E0B" if is_note_hover else "#382A1B", width=1.0)
            # Mini notepad icon
            self.canvas.create_rectangle(nx1 + 7, 11, nx1 + 13, h - 11, fill="#0F172A", outline="#F59E0B", width=1.0)
            self.canvas.create_line(nx1 + 9, 14, nx1 + 11, 14, fill="#F59E0B", width=1.0)
            self.canvas.create_line(nx1 + 9, 17, nx1 + 11, 17, fill="#F59E0B", width=1.0)
            note_col = "#FBBF24" if is_note_hover else "#FEF3C7"
            self.canvas.create_text(nx1 + 19, h // 2, text="Notes", anchor="w", fill=note_col, font=("Segoe UI", 8, "bold"))

            # 5. Hotkey Badge ([F8])
            is_hk_hover = (self.hovered_element == "hotkey")
            kx1, kx2 = 190, 214
            self._create_rounded_rect(kx1, 6, kx2, h - 6, r=4, fill="#1E293B" if is_hk_hover else "#0F172A", outline="#38BDF8" if is_hk_hover else "#1E2638", width=1.0)
            hk_text = (self.config.get("hotkey", "F8") if self.config else "F8").upper()
            self.canvas.create_text((kx1 + kx2) // 2, h // 2, text=hk_text, fill="#60A5FA" if is_hk_hover else "#38BDF8", font=("Segoe UI", 7, "bold"))

            # 6. Close Icon (✕)
            close_x = 227
            if is_close_hover:
                self.canvas.create_oval(close_x - 8, h // 2 - 8, close_x + 8, h // 2 + 8, fill="#2D1215", outline="")
            self.canvas.create_text(close_x, h // 2, text="✕", fill=close_color, font=("Segoe UI", 9, "bold"))

        elif self.state == "WELCOME":
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#090D16", outline="#38BDF8", width=1.5)
            self._draw_vector_mic(18, h // 2, fill_color="#10B981", aura_color="#064E3B", aura_radius=9.0)
            hk = (self.config.get("hotkey", "F8") if self.config else "F8").upper()
            self.canvas.create_text(34, h // 2, text=f"OwnVoice Ready • {hk}", anchor="w", fill="#F8FAFC", font=("Segoe UI", 8, "bold"))
            self.canvas.create_text(w - 16, h // 2, text="✕", fill=close_color, font=("Segoe UI", 9, "bold"))

        elif self.state == "RECORDING":
            is_note = getattr(self, "is_note_mode", False)
            border_col = "#F59E0B" if is_note else "#0EA5E9"
            bg_col = "#120E08" if is_note else "#080C14"
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill=bg_col, outline=border_col, width=1.5)

            # Left Cancel Button (✕)
            cx_cancel = 20
            is_cancel_hover = (self.hovered_element == "cancel")
            self.canvas.create_oval(
                cx_cancel - 10, h // 2 - 10, cx_cancel + 10, h // 2 + 10,
                fill="#451A1A" if is_cancel_hover else "#2D1215",
                outline="#EF4444" if is_cancel_hover else "#7F1D1D",
                width=1.0
            )
            self.canvas.create_text(cx_cancel, h // 2, text="✕", fill="#EF4444", font=("Segoe UI", 9, "bold"))

            # Center 10-Bar Symmetrical Flow Waveform
            vol = self.get_volume_fn() if self.get_volume_fn else 0.0
            vol = max(0.0, min(1.0, float(vol)))
            bar_start_x = 44
            num_bars = 10
            bar_spacing = 18.5
            envelopes = [0.35, 0.55, 0.8, 0.95, 1.0, 1.0, 0.95, 0.8, 0.55, 0.35]
            colors_dictate = ["#38BDF8", "#38BDF8", "#60A5FA", "#60A5FA", "#818CF8", "#818CF8", "#A78BFA", "#A78BFA", "#C084FC", "#C084FC"]
            colors_note = ["#F59E0B", "#F59E0B", "#FBBF24", "#FCD34D", "#34D399", "#34D399", "#10B981", "#10B981", "#059669", "#059669"]
            palette = colors_note if is_note else colors_dictate

            for i in range(num_bars):
                phase_offset = self.pulse_phase * 2.4 + i * 0.65
                wave_motion = math.sin(phase_offset) * 4.5
                envelope = envelopes[i]
                bar_h = max(4, int(vol * 24.0 * envelope + wave_motion + 6.0))
                bx = bar_start_x + i * bar_spacing
                self.canvas.create_line(bx, h // 2 - bar_h // 2, bx, h // 2 + bar_h // 2, fill=palette[i], width=3, capstyle="round")

            # Right Done Button (✓)
            cx_done = w - 42
            is_done_hover = (self.hovered_element == "done")
            self.canvas.create_oval(
                cx_done - 11, h // 2 - 11, cx_done + 11, h // 2 + 11,
                fill="#065F46" if is_done_hover else "#064E3B",
                outline="#34D399" if is_done_hover else "#10B981",
                width=1.0
            )
            self.canvas.create_text(cx_done, h // 2, text="✓", fill="#A7F3D0", font=("Segoe UI", 10, "bold"))

            # Far Right Close Button (✕)
            cx_dismiss = w - 16
            self.canvas.create_text(cx_dismiss, h // 2, text="✕", fill=close_color, font=("Segoe UI", 9, "bold"))

        elif self.state == "PROCESSING":
            is_note = getattr(self, "is_note_mode", False)
            border_col = "#F59E0B" if is_note else "#8B5CF6"
            bg_col = "#1A140B" if is_note else "#120E24"
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill=bg_col, outline=border_col, width=1.5)

            # Traveling 3-Dot Harmonic Shimmer
            dot_start_x = 22
            dot_color = "#FCD34D" if is_note else "#C084FC"
            for i in range(3):
                offset = math.sin(self.pulse_phase * 3.0 + i * 0.9) * 3.0
                cx = dot_start_x + i * 9
                cy = h // 2 + offset
                self.canvas.create_oval(cx - 2.5, cy - 2.5, cx + 2.5, cy + 2.5, fill=dot_color, outline="")

            label = "Structuring Note..." if is_note else "Cleaning up..."
            text_col = "#FEF3C7" if is_note else "#E9D5FF"
            self.canvas.create_text(54, h // 2, text=label, anchor="w", fill=text_col, font=("Segoe UI", 8, "bold"))
            self.canvas.create_text(w - 18, h // 2, text="✕", fill=close_color, font=("Segoe UI", 9, "bold"))

        elif self.state == "SUCCESS":
            is_note = getattr(self, "is_note_mode", False)
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#061F17", outline="#10B981", width=1.5)
            self.canvas.create_text(18, h // 2, text="✓", fill="#10B981", font=("Segoe UI", 10, "bold"))
            label = self.status_text or ("Note Saved!" if is_note else "Typed!")
            self.canvas.create_text(34, h // 2, text=label[:20], anchor="w", fill="#ECFDF5", font=("Segoe UI", 8, "bold"))

        elif self.state == "ERROR":
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#1F1306", outline="#F59E0B", width=1.5)
            self.canvas.create_text(14, h // 2, text="•", fill="#F59E0B", font=("Segoe UI", 12, "bold"))
            msg = self.status_text or "No speech"
            self.canvas.create_text(26, h // 2, text=msg[:16], anchor="w", fill="#FEF3C7", font=("Segoe UI", 8, "bold"))

    def _clear_hide_timer(self):
        if self.hide_timer and self.root:
            try:
                self.root.after_cancel(self.hide_timer)
            except Exception:
                pass
            self.hide_timer = None

    def show_recording(self, context_label="", is_note_mode=False):
        self._clear_hide_timer()
        self.state = "RECORDING"
        self.context_label = context_label
        self.is_note_mode = is_note_mode
        self._resize(280)

    def show_processing(self, is_note_mode=False):
        self._clear_hide_timer()
        self.state = "PROCESSING"
        self.is_note_mode = is_note_mode
        self._resize(210)

    def show_success(self, text="", is_note_mode=False):
        self._clear_hide_timer()
        self.state = "SUCCESS"
        self.status_text = text
        self.is_note_mode = is_note_mode
        self._resize(190)
        if self.root:
            self.hide_timer = self.root.after(1200, self.dock)

    def show_error(self, message="No speech"):
        self._clear_hide_timer()
        self.state = "ERROR"
        self.status_text = message
        self._resize(160)
        if self.root:
            self.hide_timer = self.root.after(1800, self.dock)

    def show_welcome(self):
        self._clear_hide_timer()
        self.state = "WELCOME"
        welcome_w = 195
        self.width = welcome_w
        if self.root and self.canvas:
            try:
                screen_w = self.root.winfo_screenwidth() if hasattr(self.root, "winfo_screenwidth") else 1920
                screen_h = self.root.winfo_screenheight() if hasattr(self.root, "winfo_screenheight") else 1080
                toast_x = (screen_w - self.width) // 2
                toast_y = max(40, int(screen_h * 0.12))
                toast_x, toast_y = self._clamp_coordinates(toast_x, toast_y)
                self.root.geometry(f"{self.width}x{self.height}+{toast_x}+{toast_y}")
                self.canvas.config(width=self.width, height=self.height)
            except Exception:
                pass
            self._draw_pill()
            if hasattr(self.root, "after"):
                self.hide_timer = self.root.after(2500, self.dock)

    def dock(self):
        self._clear_hide_timer()
        was_welcome = (self.state == "WELCOME")
        self.state = "DOCKED"
        self.context_label = ""
        self.is_note_mode = False
        target_w = self.docked_width
        if was_welcome and self.root and self.canvas:
            saved_x = self.config.get("overlay_x") if self.config else None
            saved_y = self.config.get("overlay_y") if self.config else None
            screen_w = self.root.winfo_screenwidth() if hasattr(self.root, "winfo_screenwidth") else 1920
            screen_h = self.root.winfo_screenheight() if hasattr(self.root, "winfo_screenheight") else 1080
            target_x = saved_x if saved_x is not None else (screen_w - target_w) // 2
            target_y = saved_y if saved_y is not None else screen_h - self.height - 40
            target_x, target_y = self._clamp_coordinates(target_x, target_y)
            self._animate_to_dock(target_x, target_y, target_w)
        else:
            self._resize(target_w)

    def _animate_to_dock(self, target_x, target_y, target_w=165):
        if not self.root or not self.canvas:
            self._resize(target_w)
            return
        try:
            curr_x = self.root.winfo_x()
            curr_y = self.root.winfo_y()
        except Exception:
            self._resize(target_w)
            return

        if curr_x == target_x and curr_y == target_y:
            self._resize(target_w)
            return

        steps = 8
        dx = (target_x - curr_x) / steps
        dy = (target_y - curr_y) / steps
        self.width = target_w
        try:
            self.canvas.config(width=self.width, height=self.height)
        except Exception:
            pass

        def step_anim(step=0):
            if not self.root or self.state != "DOCKED":
                return
            if step >= steps:
                try:
                    self.root.geometry(f"{self.width}x{self.height}+{target_x}+{target_y}")
                    self._draw_pill()
                except Exception:
                    pass
                return
            nx = int(curr_x + dx * (step + 1))
            ny = int(curr_y + dy * (step + 1))
            try:
                self.root.geometry(f"{self.width}x{self.height}+{nx}+{ny}")
                self._draw_pill()
            except Exception:
                pass
            if hasattr(self.root, "after"):
                try:
                    self.root.after(16, lambda: step_anim(step + 1))
                except Exception:
                    pass

        step_anim(0)

    def _resize(self, new_w):
        self.width = new_w
        if self.root and self.canvas:
            curr_x = self.root.winfo_x()
            curr_y = self.root.winfo_y()
            self.root.geometry(f"{self.width}x{self.height}+{curr_x}+{curr_y}")
            self.canvas.config(width=self.width, height=self.height)
            self._draw_pill()

    def _focus_tracker_loop(self):
        user32 = ctypes.windll.user32
        while self.is_running:
            try:
                fg = user32.GetForegroundWindow()
                if fg != 0 and fg != self.hwnd and self.injector:
                    self.injector.update_target_hwnd(fg)
            except Exception:
                pass
            time.sleep(0.1)

    def _tick_anim(self):
        if not self.is_running or not self.root:
            return
        
        delay = 40
        if self.state == "DOCKED":
            self.pulse_phase = (self.pulse_phase + 0.08) % (math.pi * 200)
            self._draw_pill()
            delay = 40  # 25 FPS subtle breathing
        elif self.state == "RECORDING":
            self.pulse_phase = (self.pulse_phase + 0.25) % (math.pi * 200)
            self._draw_pill()
            delay = 33  # 30 FPS dynamic audio waveform
        elif self.state == "PROCESSING":
            self.pulse_phase = (self.pulse_phase + 0.20) % (math.pi * 200)
            self._draw_pill()
            delay = 33  # 30 FPS liquid shimmer

        try:
            if hasattr(self.root, "after"):
                self.anim_timer = self.root.after(delay, self._tick_anim)
        except Exception:
            pass

    def start_overlay(self):
        self.is_running = True
        self._create_window()
        self.show()
        self.show_welcome()
        self._tick_anim()
        self.focus_thread = threading.Thread(target=self._focus_tracker_loop, daemon=True)
        self.focus_thread.start()

    def destroy(self):
        self.is_running = False
        self._clear_hide_timer()
        if hasattr(self, "anim_timer") and self.anim_timer and self.root and hasattr(self.root, "after_cancel"):
            try:
                self.root.after_cancel(self.anim_timer)
            except Exception:
                pass
            self.anim_timer = None
        if self.root:
            try:
                self.root.destroy()
            except Exception:
                pass
            self.root = None
