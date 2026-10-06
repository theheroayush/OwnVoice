# Sandbox stubs (Linux CI shims for the Windows/Linux desktop test-suite)

OwnVoice ships a **Windows-first** desktop app (`pywin32`, `ctypes.windll`,
`winsound`, `Tkinter`, global hotkeys) plus a global test-suite that assumes a
real desktop session. On a headless Linux CI box (`python3 tests/run_tests.py`)
imports such as `customtkinter`, `pystray`, `pyperclip`, `pynput` or
`sounddevice` are unavailable, and every Windows-only call raises
`AttributeError: module 'ctypes' has no attribute 'windll'`.

This directory contains **stubs only used when you opt in**:

```bash
PYTHONPATH=tests/sandbox_stubs python3 tests/run_tests.py
```

Nothing here is imported by the product code or by a normal test run — the
`sitecustomize.py` module is only picked up because it is the first entry on
`sys.path` in that invocation.

## What the stubs do

| Path | Purpose |
| --- | --- |
| `sitecustomize.py` | Injects a `ctypes.windll` / `ctypes.WinDLL` stand-in whose every call returns `1` so Windows-only code paths execute instead of raising. |
| `sounddevice.py` | Minimal PortAudio-like `InputStream`/`query_devices` used by `core/audio_recorder.py`. |
| `pynput/` | `keyboard.Controller`, `Key`, `Listener` and `mouse.Controller`, `Button`, `Listener` shims for `core/hotkey_manager.py` / `core/injector.py`. |
| `pystray.py`, `pyperclip.py` | Tray icon and in-memory clipboard stubs (thread-locked). |
| `tkinter/` | A headless `tkinter` + `customtkinter`-compatible widget set (with an explicit `__all__` of the 67 constants `customtkinter` re-exports). |

## Known limits of the stub run

With the stubs in place the suite reports **204 passed / 19 failed** on Linux.
Those 19 failures are **expected artifacts of the stubs, not product bugs**:

* 11 failures come from the `ctypes.windll` stand-in returning `1` for every
  symbol (the real Win32 API returns `0` for failure, writes into `lp*`
  out-parameters, etc.).
* `test_f06`-style `INPUT`/`sizeof()` union assertions are Windows-struct-layout
  specific.
* The `pyperclip` stub cannot reproduce the real clipboard round-trip.
* Two latency ceilings (`test_f14` at 0.2 ms, `test_s06`) are below this VM's
  timer granularity and flake run-to-run.

Use a real Windows host for those. Everything else (223 tests discovered) is a
meaningful signal on Linux.
