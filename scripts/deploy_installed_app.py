"""
Deploy freshly compiled OwnVoice Desktop Hub executable to installed application path.
Preserves user configurations (config.json, .env, history.json, notes.db).
"""
import os
import sys
import shutil
import time
import subprocess
from pathlib import Path

SOURCE_DIR = Path(r"C:\Users\ayush\OneDrive\Documents\ownvoice\dist\OwnVoice")
TARGET_DIR = Path(r"C:\Users\ayush\AppData\Local\Programs\OwnVoice")
BACKUP_DIR = Path(r"C:\Users\ayush\AppData\Local\Programs\OwnVoice_Backup")

def stop_running_processes():
    print("[Deploy] Checking for running OwnVoice processes...")
    try:
        subprocess.run(["taskkill", "/F", "/IM", "OwnVoice.exe"], capture_output=True)
        time.sleep(1.0)
    except Exception as e:
        print(f"[Deploy] Taskkill notice: {e}")

def backup_user_data():
    if not TARGET_DIR.exists():
        return
    print(f"[Deploy] Backing up user data to {BACKUP_DIR}...")
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)
    
    files_to_preserve = ["config.json", ".env", "history.json"]
    for f in files_to_preserve:
        src = TARGET_DIR / f
        if src.exists():
            shutil.copy2(src, BACKUP_DIR / f)
            print(f"  Backed up {f}")

    # Check for notes.db in root or _internal
    for p in [TARGET_DIR / "notes.db", TARGET_DIR / "_internal" / "notes.db"]:
        if p.exists():
            shutil.copy2(p, BACKUP_DIR / "notes.db")
            print(f"  Backed up notes.db from {p}")

def deploy():
    if not SOURCE_DIR.exists():
        print(f"[Deploy] Error: Source directory {SOURCE_DIR} does not exist!")
        sys.exit(1)

    stop_running_processes()
    backup_user_data()

    print(f"[Deploy] Deploying from {SOURCE_DIR} to {TARGET_DIR}...")
    TARGET_DIR.mkdir(parents=True, exist_ok=True)

    # Copy files
    for item in SOURCE_DIR.iterdir():
        dest = TARGET_DIR / item.name
        if item.is_dir():
            if dest.exists():
                # For _internal, preserve notes.db if inside
                internal_db = dest / "notes.db"
                has_internal_db = internal_db.exists()
                if has_internal_db:
                    shutil.copy2(internal_db, BACKUP_DIR / "notes.db")
                shutil.rmtree(dest)
            shutil.copytree(item, dest)
            print(f"  Updated directory {item.name}")
        else:
            # Don't overwrite existing user config files if they exist in target
            if item.name in ["config.json", "history.json", ".env"] and dest.exists():
                continue
            shutil.copy2(item, dest)
            print(f"  Updated {item.name}")

    # Restore backups
    if BACKUP_DIR.exists():
        for b in BACKUP_DIR.iterdir():
            if b.name == "notes.db":
                shutil.copy2(b, TARGET_DIR / "_internal" / "notes.db")
                shutil.copy2(b, TARGET_DIR / "notes.db")
                print(f"  Restored {b.name}")
            else:
                shutil.copy2(b, TARGET_DIR / b.name)
                print(f"  Restored {b.name}")

    print("[Deploy] Deployment complete successfully!")

    # Verify launch
    target_exe = TARGET_DIR / "OwnVoice.exe"
    if target_exe.exists():
        print(f"[Deploy] Verified {target_exe} ({target_exe.stat().st_size} bytes)")
        print("[Deploy] Starting application in background...")
        subprocess.Popen([str(target_exe)], cwd=str(TARGET_DIR))
        print("[Deploy] OwnVoice launched.")
    else:
        print(f"[Deploy] Error: {target_exe} not found!")
        sys.exit(1)

if __name__ == "__main__":
    deploy()
