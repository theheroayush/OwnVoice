import tkinter as tk
import ctypes
import threading
import time
import math
from typing import Callable

GWL_EXSTYLE = -20
WS_EX_NOACTIVATE = 0x08000000
WS_EX_TOOLWINDOW = 0x00000080
WS_EX_TOPMOST = 0x00000008

class FloatingWidget:
    def __init__(self, get_volume_fn: Callable[[], float], on_click_toggle: Callable[[], None] = None, on_open_settings: Callable[[], None] = None, on_cancel: Callable[[], None] = None, on_hide: Callable[[], None] = None, injector = None, config_manager = None):
        self.get_volume_fn = get_volume_fn
        self.on_click_toggle = on_click_toggle
        self.on_open_settings = on_open_settings
        self.on_cancel = on_cancel
        self.on_hide = on_hide
        self.injector = injector
        self.config = config_manager
        
        self.root = None
        self.canvas = None
        self.state = "DOCKED"  # DOCKED, RECORDING, PROCESSING, SUCCESS, ERROR
        self.status_text = ""
        self.context_label = ""
        self.is_running = False
        self.anim_thread = None
        self.focus_thread = None
        self.pulse_phase = 0.0
        self.hide_timer = None
        
        self.width = 165
        self.height = 34
        self.hwnd = None
        
        self.drag_start_x = 0
        self.drag_start_y = 0
        self.is_dragging = False
        self.hovering_close = False

    def _create_window(self):
        self.root = tk.Tk()
        self.root.title("OwnVoice")
        self.root.overrideredirect(True)
        self.root.attributes("-topmost", True)
        
        self.root.config(bg="#000001")
        self.root.wm_attributes("-transparentcolor", "#000001")

        try:
            self.hwnd = ctypes.windll.user32.GetParent(self.root.winfo_id())
            ex_style = ctypes.windll.user32.GetWindowLongW(self.hwnd, GWL_EXSTYLE)
            ctypes.windll.user32.SetWindowLongW(self.hwnd, GWL_EXSTYLE, ex_style | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW | WS_EX_TOPMOST)
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
        self.win_start_x = self.root.winfo_x()
        self.win_start_y = self.root.winfo_y()
        self.is_dragging = False

    def _on_mouse_drag(self, event):
        dx = event.x_root - self.drag_start_x
        dy = event.y_root - self.drag_start_y
        if abs(dx) > 3 or abs(dy) > 3:
            self.is_dragging = True
            new_x = self.win_start_x + dx
            new_y = self.win_start_y + dy
            self.root.geometry(f"{self.width}x{self.height}+{new_x}+{new_y}")

    def _on_mouse_up(self, event):
        if self.is_dragging:
            self.is_dragging = False
            if self.config:
                self.config.set("overlay_x", self.root.winfo_x(), save=False)
                self.config.set("overlay_y", self.root.winfo_y(), save=True)
            return

        if event.x >= self.width - 26:
            if self.state == "RECORDING":
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
        if self.state == "RECORDING":
            menu.add_command(label="🚫 Cancel Recording", command=self._cancel_and_dock)
        menu.add_command(label="👁️ Hide to Tray", command=self.hide)
        menu.add_separator()
        menu.add_command(label="❌ Exit OwnVoice", command=self.root.quit)
        menu.tk_popup(event.x_root, event.y_root)

    def _cancel_and_dock(self):
        if self.on_cancel:
            self.on_cancel()
        self.dock()

    def hide(self):
        if self.root:
            self.root.withdraw()

    def show(self):
        if self.root:
            self.root.deiconify()
            self.root.lift()

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

    def show_recording(self, context_label=""):
        self.state = "RECORDING"
        self.context_label = context_label
        new_w = 175 if context_label else 155
        self._resize(new_w)

    def show_processing(self):
        self.state = "PROCESSING"
        self._resize(140)

    def show_success(self, text=""):
        self.state = "SUCCESS"
        self._resize(110)
        if self.root:
            self.hide_timer = self.root.after(1400, self.dock)

    def show_error(self, message="No speech"):
        self.state = "ERROR"
        self.status_text = message
        self._resize(135)
        if self.root:
            self.hide_timer = self.root.after(2000, self.dock)

    def dock(self):
        self.state = "DOCKED"
        self.context_label = ""
        self._resize(165)

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

    def _anim_loop(self):
        while self.is_running:
            if self.state in ("RECORDING", "PROCESSING"):
                self.pulse_phase += 0.25
                if self.root and self.canvas:
                    try:
                        self.root.after(0, self._draw_pill)
                    except Exception:
                        pass
            time.sleep(0.04)

    def start_overlay(self):
        self.is_running = True
        self._create_window()
        self.anim_thread = threading.Thread(target=self._anim_loop, daemon=True)
        self.anim_thread.start()
        self.focus_thread = threading.Thread(target=self._focus_tracker_loop, daemon=True)
        self.focus_thread.start()
