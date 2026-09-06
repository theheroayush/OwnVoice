#!/usr/bin/env bash
# OwnVoice Linux Launcher
echo "======================================================"
echo "  Starting OwnVoice for Linux (v2.5.0 Universal)"
echo "  Display Server: ${XDG_SESSION_TYPE:-X11/Wayland}"
echo "======================================================"
python3 app.py "$@"
