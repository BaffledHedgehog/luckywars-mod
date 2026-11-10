"""Exercise the portable builder against real files, animations and repeat builds."""
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "resourcepack_template/updates/tmt_equipment_2026_11_07/build_equipment_atlas.py"
spec = importlib.util.spec_from_file_location("equipment_builder", SCRIPT)
builder = importlib.util.module_from_spec(spec)
spec.loader.exec_module(builder)
Image = builder.Image


def png(color, size=(16, 16)):
    out = io.BytesIO()
    with Image.new("RGBA", size, color) as image:
        image.save(out, format="PNG")
    return out.getvalue()


class AtlasTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="tmt-atlas-test-")
        self.launcher = Path(self.temporary.name)
        instance = self.launcher / "instances/test"
        instance.mkdir(parents=True)
        (instance / "mmc-pack.json").write_text(json.dumps({"components": [{"uid": "net.minecraft", "version": "test"}]}))
        self.pack = instance / "minecraft/resourcepacks/Pack with spaces"
        self.pack.mkdir(parents=True)
        (self.pack / "pack.mcmeta").write_text(json.dumps({"pack": {"description": "test", "pack_format": 1}, "overlays": {"entries": [{"directory": "extra"}]}}))
        self.output = self.pack / "assets/lbc/textures/entity/equipment"
        self.jar = self.launcher / "libraries/com/mojang/minecraft/test/minecraft-test-client.jar"
        self.jar.parent.mkdir(parents=True)
        with zipfile.ZipFile(self.jar, "w") as archive:
            archive.writestr("assets/minecraft/textures/item/shared.png", png("red"))
            archive.writestr("assets/minecraft/textures/block/water.png", png("cyan"))
        self.put("assets/minecraft/textures/item/shared.png", png("blue"))
        self.put("assets/lbc/textures/block/duplicate.png", png("red"))
        self.put("assets/lbc/textures/font/empty.png", png((200, 50, 12, 0)))
        self.put("assets/lbc/textures/entity/equipment/humanoid/original.png", png("orange"))
        self.put("extra/assets/lbc/textures/gui/overlay.png", png("magenta"))
        with Image.new("RGBA", (16, 48)) as image:
            image.paste((0, 128, 0, 255), (0, 0, 16, 16))
            image.paste((255, 255, 0, 255), (0, 16, 16, 32))
            out = io.BytesIO()
            image.save(out, format="PNG")
        self.put("assets/lbc/textures/particle/animated.png", out.getvalue())
        self.put("assets/lbc/textures/particle/animated.png.mcmeta", b'{"animation":{"frames":[0]}}')

    def tearDown(self):
        self.temporary.cleanup()

    def put(self, name, data):
        target = self.pack / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)

    def run_build(self, **kwargs):
        return builder.build(self.pack, self.jar, self.output, seed=42, **kwargs)

    def test_discovery_all_categories_frames_dedup_and_repeat(self):
        self.output.mkdir(parents=True, exist_ok=True)
        self.assertEqual(builder.find_pack(self.output), self.pack)
        self.assertEqual(builder.vanilla_jar(self.pack), self.jar)
        original = {p: p.read_bytes() for p in self.pack.rglob("*") if p.is_file()}
        first = self.run_build()
        self.assertEqual(first["statistics"]["inputTextures"], 8)
        self.assertEqual(first["uniqueTiles"], 7)
        self.assertEqual(first["statistics"]["duplicateTiles"], 1)
        self.assertEqual(first["statistics"]["transparentTextures"], 1)
        self.assertEqual(first["statistics"]["transparentFrames"], 2)
        self.assertEqual(first["size"], [48, 48])
        self.assertEqual(first["paddingTiles"], 2)
        self.assertEqual(first["rgbaGpuBytesBothLayers"], 48 * 48 * 8)
        a, b = [self.output / name for name in builder.OUTPUTS]
        self.assertEqual(a.read_bytes(), b.read_bytes())
        with Image.open(a) as image:
            self.assertEqual(image.getpixel((0, 0)), builder.MARKER)
            for y in range(0, image.height, 16):
                for x in range(0, image.width, 16):
                    self.assertIsNotNone(image.crop((x, y, x + 16, y + 16)).getchannel("A").getbbox())
        second = self.run_build()
        self.assertEqual(first, second)
        for path, data in original.items():
            self.assertEqual(path.read_bytes(), data)
        changed = builder.build(self.pack, self.jar, self.output, seed=43)
        self.assertNotEqual(changed["pngSha256"], first["pngSha256"])

    def test_failure_does_not_replace_existing_atlas(self):
        first = self.run_build()
        with self.assertRaisesRegex(ValueError, "exceed"):
            self.run_build(max_side=16)
        self.assertEqual(hashlib.sha256((self.output / builder.OUTPUTS[0]).read_bytes()).hexdigest(), first["pngSha256"])
        self.put("assets/lbc/textures/item/broken.png", b"not a PNG")
        with self.assertRaisesRegex(ValueError, "broken.png"):
            self.run_build()
        self.assertEqual(hashlib.sha256((self.output / builder.OUTPUTS[1]).read_bytes()).hexdigest(), first["pngSha256"])

    def test_transparent_rgb_is_not_a_distinct_source(self):
        a = Image.new("RGBA", (16, 16), "blue")
        b = a.copy()
        a.putpixel((2, 2), (255, 0, 0, 0))
        b.putpixel((2, 2), (0, 255, 0, 0))
        self.assertEqual(builder.normalize(a, 16), builder.normalize(b, 16))

    def test_sparse_art_and_invalid_animation(self):
        with Image.new("RGBA", (2048, 2048)) as image:
            image.putpixel((0, 0), (255, 255, 255, 255))
            image.putpixel((2047, 2047), (255, 255, 255, 255))
            out = io.BytesIO()
            image.save(out, format="PNG")
        self.put("assets/lbc/textures/misc/sparse.png", out.getvalue())
        report = self.run_build()
        self.assertEqual(report["statistics"]["sparseFallbacks"], 1)
        self.assertEqual(report["uniqueTiles"], 8)
        self.put("assets/lbc/textures/particle/animated.png.mcmeta", b'{"animation":{"width":15}}')
        with self.assertRaisesRegex(ValueError, "frame dimensions"):
            self.run_build()

    def test_wrong_jar_is_not_silently_accepted(self):
        with zipfile.ZipFile(self.jar, "w") as archive:
            archive.writestr("server.txt", "no client textures")
        with self.assertRaisesRegex(ValueError, "client JAR"):
            self.run_build()

    def test_copied_script_runs_from_an_unrelated_working_directory(self):
        self.output.mkdir(parents=True, exist_ok=True)
        script = self.output / SCRIPT.name
        script.write_bytes(SCRIPT.read_bytes())
        result = subprocess.run([sys.executable, str(script), "--seed", "42"], cwd=self.launcher,
                                capture_output=True, text=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr)
        report = json.loads((self.output / builder.REPORT).read_text(encoding="utf-8"))
        self.assertEqual(report["uniqueTiles"], 7)
        self.assertEqual(report["vanillaJar"], str(self.jar.resolve()))


if __name__ == "__main__":
    unittest.main()
