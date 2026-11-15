"""Real bounded ffmpeg preparation and binary consumer contract; no account/network access."""
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import struct
import zlib
import shutil
import subprocess
import sys
import tempfile
import zipfile

sys.dont_write_bytecode = True

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("worker", ROOT / "src/main/resources/skywars2/tiktok/worker.py")
worker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(worker)


def main():
    target = ROOT / "build/verification/tiktok"
    target.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="offline-", dir=target) as temporary:
        root = Path(temporary)
        native = next((ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft").rglob("minecraft-merged-d6f9bb6878-26.3-sources.jar"))
        with zipfile.ZipFile(native) as jar:
            source = jar.read("net/minecraft/world/level/material/MapColor.java").decode()
        colors = {int(i): int(rgb) for i, rgb in re.findall(r"new MapColor\((\d+), (\d+)\)", source)}
        palette = [[((colors.get(i // 4, 0) >> shift) & 255) * [180, 220, 255, 135][i % 4] // 255
                    for shift in (16, 8, 0)] for i in range(256)]
        palette_path = root / "palette.json"
        palette_path.write_text(json.dumps(palette))
        reports = []
        for silent in (False, True):
            original = root / ("silent.mp4" if silent else "audio.mp4")
            audio = [] if silent else ["-f", "lavfi", "-i", "sine=frequency=1000:sample_rate=24000"]
            subprocess.run(["ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y", "-threads", "1",
                            "-f", "lavfi", "-i", "testsrc2=size=256x448:rate=20", *audio, "-t", "2",
                            "-c:v", "libx264", "-threads", "1", "-preset", "ultrafast", "-pix_fmt", "yuv420p",
                            *([] if silent else ["-c:a", "aac"]), str(original)], check=True, timeout=30)
            before = hashlib.sha256(original.read_bytes()).hexdigest()
            stage = root / ("silent" if silent else "audio"); stage.mkdir()
            report = worker.prepare(original, stage, palette_path)
            assert report["frames"] == 40 and report["fps"] == 20
            assert hashlib.sha256(original.read_bytes()).hexdigest() == before
            offsets = [v[0] for v in struct.iter_unpack(">Q", (stage / "clip.idx").read_bytes())]
            assert len(offsets) == 40 and offsets[0] == worker.HEADER.size
            maximum, nonzero, video, envelope = 0, False, set(), False
            with (stage / "clip.tkp").open("rb") as inp:
                assert worker.HEADER.unpack(inp.read(worker.HEADER.size)) == (b"TKP3", 40, 20, 480, 854, 48000, 48)
                for offset in offsets:
                    assert inp.tell() == offset
                    compressed,length = struct.unpack(">II", inp.read(8))
                    assert 0 < length <= worker.MAX_JPEG
                    low = zlib.decompress(inp.read(compressed))
                    assert len(low) == 409920 and min(low) >= 4 and max(low) < 248
                    video.add(hashlib.sha256(low).hexdigest())
                    assert max(max(palette[code]) for code in set(low)) >= 240, "bright source colors must survive palette conversion"
                    bands = inp.read(144); envelope |= any(bands)
                    pcm = inp.read(9600); nonzero |= any(pcm)
                    jpeg = inp.read(length)
                    assert jpeg.startswith(b"\xff\xd8") and jpeg.endswith(b"\xff\xd9")
                    import io
                    from PIL import Image
                    assert Image.open(io.BytesIO(jpeg)).size == (720, 1280)
                    maximum = max(maximum, length)
                assert inp.read() == b""
            assert len(video) > 30 and nonzero != silent and envelope != silent
            spectral = worker.spectral_module()
            sidecar = (stage/'clip.tka').read_bytes()
            assert spectral.HEADER.unpack_from(sidecar) == (b'TKA1',40,20,88)
            assert len(sidecar)==16+40*spectral.RECORD_BYTES
            counts=[struct.unpack_from('>H',sidecar,16+i*spectral.RECORD_BYTES)[0] for i in range(40)]
            assert max(counts)<=88 and (any(counts) != silent)
            # Migration works from prepared PCM and does not touch video or the
            # original; no cookies/account/ffmpeg/video decode is needed.
            upgrade=root/('upgrade-silent' if silent else 'upgrade-audio');upgrade.mkdir()
            guarded={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (original,stage/'clip.tkp',stage/'clip.idx')}
            spectral.migrate(stage,upgrade)
            assert (upgrade/'clip.tka').read_bytes()==sidecar
            assert guarded=={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in (original,stage/'clip.tkp',stage/'clip.idx')}
            assert not (upgrade/'audio.pcm').exists()
            assert not any((stage / p).exists() for p in ("map.rgb", "audio.pcm", "video.mjpeg"))
            if not silent:
                reference=target/'worker-reference';reference.mkdir(exist_ok=True)
                for name in ['clip.tkp','clip.idx','clip.tka']:shutil.copyfile(stage/name,reference/name)
            reports.append({"silent": silent, "frames": 40, "differentFrames": len(video), "maxJpeg": maximum,
                            "audio": nonzero, "envelope": envelope, "originalSha256": before})
        cookies = root / "cookies.json"
        cookies.write_text(json.dumps([{"domain": ".tiktok.com", "name": "sessionid", "value": "synthetic-not-a-credential"},
                                       {"domain": ".tiktok.com", "name": "expired", "value": "discarded", "expires": 1},
                                       {"domain": "tiktok.com.evil.test", "name": "bad", "value": "ignored"}]))
        assert len(worker.cookies_for_browser(cookies)) == 1
        worker.netscape_cookies(cookies, root / "cookies.txt")
        assert "evil.test" not in (root / "cookies.txt").read_text()
        assert "expired" not in (root / "cookies.txt").read_text()
        candidates=worker.candidates_from_response({"itemList":[{"id":"12345678","video":{"ok":True},"author":None},
                {"id":"12345679","video":{"ok":True},"author":{"uniqueId":"\u043f\u0440\u0438\u043c\u0435\u0440"}}]},set())
        assert len(candidates)==1 and candidates[0]["id"]=="12345679" and "%" in candidates[0]["url"]
        result = {"ok": True, "networkAccess": False, "cases": reports,
                  "workerSha256": hashlib.sha256(Path(spec.origin).read_bytes()).hexdigest()}
        (target / "worker-results.json").write_text(json.dumps(result, indent=2))
        print(json.dumps(result))


if __name__ == "__main__":
    main()
