import tkinter as tk
import ctypes
import threading
import time
import math
from typing import Callable

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
    def __init__(self, get_volume_fn: Callable[[], float], on_click_toggle: Callable[[], None] = None, on_open_settings: Callable[[], None] = None, on_cancel: Callable[[], None] = None, on_hide: Callable[[], None] = None, on_exit: Callable[[], None] = None, injector = None, config_manager = None):
        self.get_volume_fn = get_volume_fn
        self.on_click_toggle = on_click_toggle
        self.on_open_settings = on_open_settings
        self.on_cancel = on_cancel
        self.on_hide = on_hide
        self.on_exit = on_exit
        self.injector = injector
        self.config = config_manager
        
        self.root = None
        self.canvas = None
        self.state = "DOCKED"  # DOCKED, RECORDING, PROCESSING, SUCCESS, ERROR
        self.status_text = ""
        self.context_label = ""
        self.is_running = False
        self.anim_thread = None
        self.anim_timer = None
        self.focus_thread = None
        self.pulse_phase = 0.0
        self.hide_timer = None
        
        self.width = 165
        self.height = 34
        self.hwnd = None
        
        self.drag_start_x = 0
        self.drag_start_y = 0
        self.win_start_x = 0
        self.win_start_y = 0
        self.is_dragging = False
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

        # Must call update_idletasks() so Tk creates the underlying Win32 window
        # before querying HWND or applying extended window styles
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

        if event.x >= self.width - 26:
            if self.state in ("RECORDING", "PROCESSING"):
                if self.on_cancel:
                    self.on_cancel()
                self.dock()
            else:
                self.hide()
            return

        if self.injector:
            self.injector.refocus_target()
        if self.on_click_toggle:
            self.on_click_toggle()

    def _on_mouse_hover(self, event):
        was_hovering = self.hovering_close
        self.hovering_close = (event.x >= self.width - 26)
        if was_hovering != self.hovering_close:
            self._draw_pill()

    def _on_mouse_leave(self, event):
        if self.hovering_close:
            self.hovering_close = False
            self._draw_pill()

    def _show_context_menu(self, event):
        menu = tk.Menu(self.root, tearoff=0, bg="#0E131F", fg="#F8FAFC", activebackground="#2563EB", activeforeground="#FFFFFF")
        menu.add_command(label="⚙️ Settings & Snippets", command=self.on_open_settings if self.on_open_settings else None)
        if self.state in ("RECORDING", "PROCESSING"):
            menu.add_command(label="🚫 Cancel Dictation", command=self._cancel_and_dock)
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

    def _draw_pill(self):
        if not self.canvas:
            return
        self.canvas.delete("all")
        
        w, h = self.width, self.height
        r = h // 2

        close_color = "#EF4444" if self.hovering_close else "#64748B"

        if self.state == "DOCKED":
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#0A0D14", outline="#1E2638", width=1.2)
            self.canvas.create_oval(10, h//2 - 3.5, 17, h//2 + 3.5, fill="#38BDF8", outline="")
            self.canvas.create_text(23, h//2, text="OwnVoice", anchor="w", fill="#E2E8F0", font=("Segoe UI", 9, "bold"))
            
            self.canvas.create_rectangle(w - 54, 7, w - 28, h - 7, fill="#161F30", outline="#253248", width=1)
            self.canvas.create_text(w - 41, h//2, text="F8", fill="#38BDF8", font=("Segoe UI", 8, "bold"))
            self.canvas.create_text(w - 15, h//2, text="✕", fill=close_color, font=("Segoe UI", 9, "bold"))

        elif self.state == "WELCOME":
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#0A0D14", outline="#38BDF8", width=1.5)
            self.canvas.create_oval(10, h//2 - 3.5, 17, h//2 + 3.5, fill="#10B981", outline="")
            hk = (self.config.get("hotkey", "F8") if self.config else "F8").upper()
            self.canvas.create_text(24, h//2, text=f"OwnVoice Ready • {hk}", anchor="w", fill="#F8FAFC", font=("Segoe UI", 9, "bold"))
            self.canvas.create_text(w - 15, h//2, text="✕", fill=close_color, font=("Segoe UI", 9, "bold"))

        elif self.state == "RECORDING":
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#080C14", outline="#0EA5E9", width=1.5)
            
            pulse_size = 3.5 + math.sin(self.pulse_phase * 1.5) * 0.8
            self.canvas.create_oval(11 - pulse_size, h//2 - pulse_size, 11 + pulse_size, h//2 + pulse_size, fill="#EF4444", outline="")
            
            vol = self.get_volume_fn()
            bar_start_x = 21
            for i in range(4):
                phase_offset = self.pulse_phase * 2.0 + i * 1.0
                bar_h = max(4, int(vol * 18 + math.sin(phase_offset) * 5))
                bx = bar_start_x + i * 4.5
                self.canvas.create_line(bx, h//2 - bar_h//2, bx, h//2 + bar_h//2, fill="#38BDF8", width=2, capstyle="round")

            ctx = f" • {self.context_label}" if self.context_label else ""
            self.canvas.create_text(44, h//2, text=f"Listening{ctx}", anchor="w", fill="#F8FAFC", font=("Segoe UI", 8, "bold"))
            self.canvas.create_text(w - 15, h//2, text="✕", fill=close_color, font=("Segoe UI", 9, "bold"))

        elif self.state == "PROCESSING":
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#120E24", outline="#8B5CF6", width=1.5)
            dot_start_x = 14
            for i in range(3):
                offset = math.sin(self.pulse_phase * 2.5 + i * 1.2) * 2.5
                self.canvas.create_oval(
                    dot_start_x + i * 6 - 1.5, h//2 + offset - 1.5,
                    dot_start_x + i * 6 + 1.5, h//2 + offset + 1.5,
                    fill="#C084FC", outline=""
                )
            self.canvas.create_text(38, h//2, text="Polishing...", anchor="w", fill="#E9D5FF", font=("Segoe UI", 9, "bold"))
            self.canvas.create_text(w - 15, h//2, text="✕", fill=close_color, font=("Segoe UI", 9, "bold"))

        elif self.state == "SUCCESS":
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#061F17", outline="#10B981", width=1.5)
            self.canvas.create_text(14, h//2, text="✓", fill="#10B981", font=("Segoe UI", 10, "bold"))
            self.canvas.create_text(26, h//2, text="Typed", anchor="w", fill="#ECFDF5", font=("Segoe UI", 9, "bold"))

        elif self.state == "ERROR":
            self._create_rounded_rect(2, 2, w - 3, h - 3, r, fill="#1F1306", outline="#F59E0B", width=1.5)
            self.canvas.create_text(13, h//2, text="•", fill="#F59E0B", font=("Segoe UI", 12, "bold"))
            msg = self.status_text or "No speech"
            self.canvas.create_text(23, h//2, text=msg[:14], anchor="w", fill="#FEF3C7", font=("Segoe UI", 8, "bold"))

    def _create_rounded_rect(self, x1, y1, x2, y2, r=17, **kwargs):
        points = (
            x1 + r, y1, x2 - r, y1, x2, y1, x2, y1 + r,
            x2, y2 - r, x2, y2, x2 - r, y2, x1 + r, y2,
            x1, y2, x1, y2 - r, x1, y1 + r, x1, y1
        )
        return self.canvas.create_polygon(points, smooth=True, **kwargs)

    def _clear_hide_timer(self):
        if self.hide_timer and self.root:
            try:
                self.root.after_cancel(self.hide_timer)
            except Exception:
                pass
            self.hide_timer = None

    def show_recording(self, context_label=""):
        self._clear_hide_timer()
        self.state = "RECORDING"
        self.context_label = context_label
        new_w = 175 if context_label else 155
        self._resize(new_w)

    def show_processing(self):
        self._clear_hide_timer()
        self.state = "PROCESSING"
        self._resize(140)

    def show_success(self, text=""):
        self._clear_hide_timer()
        self.state = "SUCCESS"
        self._resize(110)
        if self.root:
            self.hide_timer = self.root.after(1400, self.dock)

    def show_error(self, message="No speech"):
        self._clear_hide_timer()
        self.state = "ERROR"
        self.status_text = message
        self._resize(135)
        if self.root:
            self.hide_timer = self.root.after(2000, self.dock)

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
        target_w = 165
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
        if self.state in ("RECORDING", "PROCESSING"):
            self.pulse_phase += 0.25
            self._draw_pill()
        try:
            if hasattr(self.root, "after"):
                self.anim_timer = self.root.after(40, self._tick_anim)
        except Exception:
            pass

    def _anim_loop(self):
        """Deprecated: Replaced by native Tkinter-driven _tick_anim timer loop."""
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
