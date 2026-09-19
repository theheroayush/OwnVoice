"""
OwnVoice v3.0 Universal Zero-Knowledge Cloud Relay Server.
Enables instant cross-network pairing and control between Mobile and PC across 5G, NAT, and VPNs.
"""

import argparse
import json
import logging
from collections import defaultdict
from typing import Dict, Set
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Query
from fastapi.responses import JSONResponse
import uvicorn

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("OwnVoiceCloudRelay")

app = FastAPI(
    title="OwnVoice Cloud Relay",
    version="3.0.0",
    description="Zero-Knowledge WebSocket Relay for Cross-Device Air Control & Dictation"
)

# In-memory rooms: room_id -> {"pc": Set[WebSocket], "phone": Set[WebSocket]}
class RelayHub:
    def __init__(self):
        self.rooms: Dict[str, Dict[str, Set[WebSocket]]] = defaultdict(lambda: {"pc": set(), "phone": set()})

    async def connect(self, room: str, role: str, websocket: WebSocket):
        await websocket.accept()
        normalized_role = "pc" if role.lower() == "pc" else "phone"
        self.rooms[room][normalized_role].add(websocket)
        logger.info(f"Client connected: room='{room}', role='{normalized_role}'. Total in room: PC={len(self.rooms[room]['pc'])}, Phone={len(self.rooms[room]['phone'])}")

    def disconnect(self, room: str, role: str, websocket: WebSocket):
        normalized_role = "pc" if role.lower() == "pc" else "phone"
        if room in self.rooms:
            self.rooms[room][normalized_role].discard(websocket)
            if not self.rooms[room]["pc"] and not self.rooms[room]["phone"]:
                del self.rooms[room]
        logger.info(f"Client disconnected: room='{room}', role='{normalized_role}'")

    async def forward_message(self, room: str, sender_role: str, message: str):
        normalized_role = "pc" if sender_role.lower() == "pc" else "phone"
        target_role = "phone" if normalized_role == "pc" else "pc"

        if room not in self.rooms:
            return

        targets = list(self.rooms[room][target_role])
        dead_targets = []
        for ws in targets:
            try:
                await ws.send_text(message)
            except Exception as e:
                logger.warning(f"Failed to forward message to {target_role} in room {room}: {e}")
                dead_targets.append(ws)

        for dead in dead_targets:
            self.disconnect(room, target_role, dead)

hub = RelayHub()

@app.get("/")
@app.get("/health")
async def health():
    return {
        "status": "healthy",
        "service": "ownvoice-cloud-relay",
        "version": "3.0.0",
        "active_rooms": len(hub.rooms)
    }

@app.get("/rooms/{room_id}/status")
async def room_status(room_id: str):
    if room_id in hub.rooms:
        r = hub.rooms[room_id]
        return {
            "room": room_id,
            "pc_connected": len(r["pc"]) > 0,
            "phone_connected": len(r["phone"]) > 0,
            "pc_count": len(r["pc"]),
            "phone_count": len(r["phone"])
        }
    return {
        "room": room_id,
        "pc_connected": False,
        "phone_connected": False,
        "pc_count": 0,
        "phone_count": 0
    }

@app.websocket("/ws")
async def websocket_endpoint(
    websocket: WebSocket,
    room: str = Query(..., description="Unique device pairing room ID / token"),
    role: str = Query("phone", description="'pc' or 'phone'")
):
    await hub.connect(room, role, websocket)
    try:
        while True:
            data = await websocket.receive_text()
            await hub.forward_message(room, role, data)
    except WebSocketDisconnect:
        hub.disconnect(room, role, websocket)
    except Exception as e:
        logger.error(f"WebSocket error in room {room}: {e}")
        hub.disconnect(room, role, websocket)

def run_server(host: str = "0.0.0.0", port: int = 8767):
    logger.info(f"Starting OwnVoice Cloud Relay on {host}:{port}")
    uvicorn.run(app, host=host, port=port, log_level="info")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="OwnVoice Cloud Relay Server")
    parser.add_argument("--host", default="0.0.0.0", help="Host address to bind")
    parser.add_argument("--port", type=int, default=8767, help="Port to listen on")
    args = parser.parse_args()
    run_server(host=args.host, port=args.port)
