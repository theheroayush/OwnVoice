import unittest
import socket
import json
import time
from core.bridge_server import BridgeServer, get_local_ip

class TestBridgeDiscovery(unittest.TestCase):
    def setUp(self):
        self.server = BridgeServer(port=8769)
        self.server.start()
        time.sleep(0.15)

    def tearDown(self):
        self.server.stop()
        time.sleep(0.1)

    def test_pin_and_token_generated(self):
        self.assertEqual(len(self.server.current_pin), 6)
        self.assertTrue(self.server.current_pin.isdigit())
        self.assertGreater(len(self.server.pairing_token), 8)

    def test_qr_code_image_generation(self):
        img = self.server.generate_qr_image(size=120)
        self.assertEqual(img.size, (120, 120))
        self.assertTrue(self.server.get_pairing_uri().startswith("ownvoice://pair?ip="))

    def test_udp_auto_discovery_probe(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(2.0)
        s.sendto(b"DISCOVER_OWNVOICE_PC", ("127.0.0.1", 8766))
        data, addr = s.recvfrom(2048)
        resp = json.loads(data.decode("utf-8"))
        self.assertEqual(resp.get("service"), "ownvoice-bridge")
        self.assertEqual(resp.get("pin"), self.server.current_pin)
        self.assertEqual(resp.get("token"), self.server.pairing_token)
        s.close()

if __name__ == '__main__':
    unittest.main()
