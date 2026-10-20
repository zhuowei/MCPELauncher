import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / "samples" / "bedrock-1211321-test-pack"


class BedrockTestPackTests(unittest.TestCase):
    def test_manifest_has_required_header_and_module_fields(self):
        manifest = json.loads((PACK / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(manifest["format_version"], 2)
        header = manifest["header"]
        self.assertEqual(header["min_engine_version"], [1, 21, 130])
        self.assertTrue(header["name"].strip())
        self.assertRegex(header["uuid"], re.compile(
            r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", re.I))
        self.assertGreaterEqual(len(manifest["modules"]), 1)
        module = manifest["modules"][0]
        self.assertEqual(module["type"], "script")
        self.assertEqual(module["language"], "javascript")
        self.assertEqual(module["entry"], "scripts/main.js")
        self.assertTrue((PACK / module["entry"]).is_file())
        self.assertRegex(module["uuid"], re.compile(
            r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", re.I))

    def test_header_and_module_uuids_are_unique(self):
        manifest = json.loads((PACK / "manifest.json").read_text(encoding="utf-8"))
        uuids = [manifest["header"]["uuid"]] + [
            module["uuid"] for module in manifest["modules"]
        ]
        self.assertEqual(len(uuids), len(set(uuids)))

    def test_script_contains_smoke_test_callbacks(self):
        script = (PACK / "scripts" / "main.js").read_text(encoding="utf-8")
        self.assertIn("world.afterEvents.worldLoad", script)
        self.assertIn("world.afterEvents.playerSpawn", script)
        self.assertIn("world.afterEvents.itemUse", script)


if __name__ == "__main__":
    unittest.main()
