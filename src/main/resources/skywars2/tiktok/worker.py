"""One-shot, single-threaded media worker. Credentials never appear in argv or output.

The server owns the queue and starts at most one worker. This program publishes one
complete clip atomically; interrupted work is confined to the supplied staging dir.
Requires Python 3.11+, numpy, Pillow, Playwright (system Edge/Chrome), yt-dlp, ffmpeg.
"""
from __future__ import annotations

import argparse
import asyncio
import io
import json
import math
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import time
import urllib.parse

# Set before numpy imports so preparation cannot occupy every CPU core.
for _name in ("OPENBLAS_NUM_THREADS", "OMP_NUM_THREADS", "MKL_NUM_THREADS", "NUMEXPR_NUM_THREADS"):
    os.environ[_name] = "1"

FPS, WIDTH, HEIGHT, RATE = 20, 64, 112, 24000
SAMPLES = RATE // FPS
MAX_SECONDS, MAX_ORIGINAL, MAX_JPEG = 300, 64 * 1024 * 1024, 48000
HEADER = struct.Struct(">4sIIIIII")  # TKP1, frames, fps, map width/height, rate, bands
BANDS = 24


class MediaError(Exception):
    """Only a fixed code crosses the worker/server boundary."""


def atomic_json(path: Path, value) -> None:
    temp = path.with_suffix(path.suffix + ".tmp")
    with temp.open("w", encoding="utf-8") as out:
        json.dump(value, out, ensure_ascii=False, separators=(",", ":"))
    os.replace(temp, path)


def cookies_for_browser(path: Path) -> list[dict]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(raw, dict):
        raw = raw.get("cookies")
    if not isinstance(raw, list) or not raw:
        raise MediaError("cookies_invalid")
    out = []
    for c in raw:
        domain = str(c.get("domain", "")).lower().lstrip(".")
        if domain != "tiktok.com" and not domain.endswith(".tiktok.com"):
            continue
        item = {"name": str(c["name"]), "value": str(c["value"]),
                "domain": str(c["domain"]), "path": str(c.get("path", "/")),
                "secure": bool(c.get("secure", True)), "httpOnly": bool(c.get("httpOnly", False))}
        expires = c.get("expires", c.get("expirationDate", -1))
        if isinstance(expires, (int, float)) and math.isfinite(expires) and 0 < expires <= time.time():
            continue  # An expired persistent cookie must not become a live session cookie.
        if isinstance(expires, (int, float)) and math.isfinite(expires) and expires > time.time():
            item["expires"] = float(expires)
        if str(c.get("sameSite", "")).lower() in ("strict", "lax", "none", "no_restriction"):
            item["sameSite"] = {"strict": "Strict", "lax": "Lax"}.get(str(c["sameSite"]).lower(), "None")
        out.append(item)
    if not out:
        raise MediaError("cookies_invalid")
    return out


async def recommendations(cookies: Path, excluded: set[str]) -> dict:
    """Let TikTok's own page perform/sign its authenticated For You request.

    No invented public trending endpoint or unrelated account/profile fallback.
    A fresh isolated browser context has only explicitly imported TikTok cookies.
    """
    from playwright.async_api import async_playwright
    async with async_playwright() as api:
        configured = os.environ.get("SKYWARS_TIKTOK_BROWSER")
        channels = [configured] if configured else (["msedge", "chrome", None] if sys.platform == "win32" else ["chrome", None])
        browser = None
        for channel in channels:
            try:
                options = {"channel": channel} if channel else {}
                browser = await api.chromium.launch(**options, headless=True,
                                                    args=["--disable-background-networking", "--mute-audio"])
                break
            except Exception:
                pass
        if browser is None:
            raise MediaError("browser_missing")
        try:
            context = await browser.new_context(viewport={"width": 480, "height": 800})
            await context.add_cookies(cookies_for_browser(cookies))
            page = await context.new_page()
            candidates: list[dict] = []

            async def response(res):
                host = urllib.parse.urlparse(res.url).hostname or ""
                if (host == "www.tiktok.com" or host.endswith(".tiktok.com")) and "/api/recommend/item_list" in res.url:
                    try:
                        data = await res.json()
                        for item in data.get("itemList", []):
                            ident = str(item.get("id", ""))
                            if re.fullmatch(r"[0-9]{8,24}", ident) and ident not in excluded and item.get("video"):
                                author = str(item.get("author", {}).get("uniqueId", ""))
                                if re.fullmatch(r"[A-Za-z0-9_.]{1,64}", author):
                                    candidates.append({"id": ident, "url": f"https://www.tiktok.com/@{author}/video/{ident}"})
                    except Exception:
                        pass  # Response bodies and headers must never become log messages.

            page.on("response", response)
            await page.goto("https://www.tiktok.com/foryou", wait_until="domcontentloaded", timeout=45000)
            for attempt in range(16):
                if candidates:
                    return candidates[0]
                if attempt and attempt % 4 == 0:
                    await page.keyboard.press("ArrowDown")
                await page.wait_for_timeout(1000)
            raise MediaError("feed_unavailable")
        finally:
            await browser.close()


def netscape_cookies(source: Path, target: Path) -> None:
    rows = ["# Netscape HTTP Cookie File"]
    for c in cookies_for_browser(source):
        if any("\n" in str(c.get(k, "")) or "\t" in str(c.get(k, "")) for k in ("domain", "path", "name", "value")):
            raise MediaError("cookies_invalid")
        rows.append("\t".join([c["domain"], "TRUE" if c["domain"].startswith(".") else "FALSE", c["path"],
                               "TRUE" if c["secure"] else "FALSE", str(int(c.get("expires", 0))), c["name"], c["value"]]))
    target.write_text("\n".join(rows) + "\n", encoding="utf-8")
    try:
        target.chmod(0o600)
    except OSError:
        pass


def execute(argv: list[str], timeout: int, code: str) -> None:
    try:
        subprocess.run(argv, stdin=subprocess.DEVNULL, stdout=subprocess.DEVNULL,
                       stderr=subprocess.DEVNULL, check=True, timeout=timeout,
                       creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0)
    except (OSError, subprocess.SubprocessError) as error:
        raise MediaError(code) from None


def download(clip: dict, cookies: Path, stage: Path) -> Path:
    cookie_file = stage / "download-cookies.txt"
    netscape_cookies(cookies, cookie_file)
    try:
        execute([sys.executable, "-m", "yt_dlp", "--ignore-config", "--no-playlist", "--quiet", "--no-warnings",
                 "--cookies", str(cookie_file), "--max-filesize", str(MAX_ORIGINAL), "--socket-timeout", "15",
                 "--retries", "2", "--fragment-retries", "2", "--concurrent-fragments", "1",
                 "--restrict-filenames", "--no-write-info-json", "--no-write-thumbnail", "--no-mtime",
                 "-f", "best[ext=mp4]/best", "-o", str(stage / "original.%(ext)s"), clip["url"]], 180, "download_failed")
    finally:
        cookie_file.unlink(missing_ok=True)
    originals = list(stage.glob("original.*"))
    if len(originals) != 1 or not originals[0].is_file() or not 0 < originals[0].stat().st_size <= MAX_ORIGINAL:
        raise MediaError("download_size")
    return originals[0]


def palette_lut(colors):
    import numpy as np
    colors = np.asarray(colors, dtype=np.int32)
    if colors.shape != (256, 3):
        raise MediaError("palette_invalid")
    usable = np.arange(4, 248, dtype=np.uint8)  # Native 26.3 MapColor ids; first four are transparent.
    grid = np.indices((32, 32, 32)).reshape(3, -1).T * 8 + 4
    result = np.empty(32768, dtype=np.uint8)
    for start in range(0, 32768, 512):
        delta = grid[start:start + 512, None, :] - colors[usable][None, :, :]
        # Luma-weighted nearest colour; chunks bound working RAM instead of a 32^3 x palette x RGB tensor.
        metric = delta[:, :, 0] ** 2 * 3 + delta[:, :, 1] ** 2 * 4 + delta[:, :, 2] ** 2 * 2
        result[start:start + 512] = usable[metric.argmin(axis=1)]
    return result


def jpeg_frames(path: Path):
    """MJPEG is a sequence of self-contained JPEG images; never retain the whole video."""
    buffer = bytearray()
    with path.open("rb") as inp:
        while chunk := inp.read(65536):
            buffer.extend(chunk)
            while True:
                end = buffer.find(b"\xff\xd9")
                if end < 0:
                    break
                image = bytes(buffer[:end + 2]); del buffer[:end + 2]
                if not image.startswith(b"\xff\xd8"):
                    raise MediaError("jpeg_invalid")
                yield image
            if len(buffer) > 1024 * 1024:
                raise MediaError("jpeg_size")
    if buffer:
        raise MediaError("jpeg_incomplete")


def prepare(original: Path, stage: Path, palette: Path, ffmpeg="ffmpeg") -> dict:
    import numpy as np
    from PIL import Image
    rgb, mjpeg, pcm = (stage / n for n in ("map.rgb", "video.mjpeg", "audio.pcm"))
    # One bounded native process, one filter thread, one thread per encoder. No server tick waits here.
    filters = ("[0:v:0]fps=20,split=2[lo][hi];"
               "[lo]scale=64:112:force_original_aspect_ratio=decrease,pad=64:112:(ow-iw)/2:(oh-ih)/2:black,format=rgb24[map];"
               "[hi]scale=256:448:force_original_aspect_ratio=decrease,pad=256:448:(ow-iw)/2:(oh-ih)/2:black[hd]")
    probe = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "a:0", "-show_entries", "stream=index",
                            "-of", "csv=p=0", str(original)], stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                           stderr=subprocess.DEVNULL, timeout=20, check=True,
                           creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0)
    audio_args = (["-map", "0:a:0", "-t", str(MAX_SECONDS), "-threads", "1", "-ac", "1", "-ar", str(RATE),
                   "-c:a", "pcm_s16le", "-f", "s16le", str(pcm)] if probe.stdout.strip() else [])
    if not audio_args:
        pcm.touch()
    execute([ffmpeg, "-hide_banner", "-loglevel", "error", "-nostdin", "-y", "-threads", "1", "-i", str(original),
             "-filter_complex_threads", "1", "-filter_complex", filters,
             "-map", "[map]", "-t", str(MAX_SECONDS), "-threads", "1", "-f", "rawvideo", str(rgb),
             "-map", "[hd]", "-t", str(MAX_SECONDS), "-threads", "1", "-c:v", "mjpeg", "-q:v", "5", "-f", "image2pipe", str(mjpeg),
             *audio_args], 240, "transcode_failed")
    frames = rgb.stat().st_size // (WIDTH * HEIGHT * 3)
    if not 1 <= frames <= MAX_SECONDS * FPS or rgb.stat().st_size != frames * WIDTH * HEIGHT * 3:
        raise MediaError("duration_invalid")
    lut = palette_lut(json.loads(palette.read_text(encoding="utf-8")))
    edges = np.geomspace(90, 10000, BANDS + 1)
    frequencies = np.fft.rfftfreq(SAMPLES, 1 / RATE)
    masks = [(frequencies >= edges[i]) & (frequencies < edges[i + 1]) for i in range(BANDS)]
    offsets = []
    with rgb.open("rb") as low, pcm.open("rb") as audio, (stage / "clip.tkp").open("wb") as out:
        out.write(HEADER.pack(b"TKP1", frames, FPS, WIDTH, HEIGHT, RATE, BANDS))
        for frame, jpeg in enumerate(jpeg_frames(mjpeg)):
            if frame >= frames:
                raise MediaError("frame_count")
            offsets.append(out.tell())
            pixels = np.frombuffer(low.read(WIDTH * HEIGHT * 3), np.uint8).reshape(-1, 3).astype(np.uint16)
            indices = ((pixels[:, 0] >> 3) << 10) | ((pixels[:, 1] >> 3) << 5) | (pixels[:, 2] >> 3)
            mapped = lut[indices].tobytes()
            samples = audio.read(SAMPLES * 2).ljust(SAMPLES * 2, b"\0")
            wave = np.frombuffer(samples, dtype="<i2").astype(np.float32) / 32768
            spectrum = np.abs(np.fft.rfft(wave * np.hanning(SAMPLES))) / (SAMPLES / 2)
            levels = bytes(min(255, round(float(np.sqrt(np.square(spectrum[m]).sum())) * 255 * 2)) for m in masks)
            if len(jpeg) > MAX_JPEG:
                image = Image.open(io.BytesIO(jpeg)).convert("RGB")
                for quality in (65, 45, 25):
                    encoded = io.BytesIO(); image.save(encoded, "JPEG", quality=quality)
                    jpeg = encoded.getvalue()
                    if len(jpeg) <= MAX_JPEG:
                        break
            if len(jpeg) > MAX_JPEG:
                raise MediaError("jpeg_size")
            out.write(struct.pack(">I", len(jpeg))); out.write(mapped); out.write(levels); out.write(samples); out.write(jpeg)
    if len(offsets) != frames:
        raise MediaError("frame_count")
    with (stage / "clip.idx").open("wb") as index:
        for offset in offsets:
            index.write(struct.pack(">Q", offset))
    for p in (rgb, mjpeg, pcm):
        p.unlink(missing_ok=True)
    return {"format": 1, "frames": frames, "fps": FPS, "width": WIDTH, "height": HEIGHT,
            "rate": RATE, "bands": BANDS, "original": original.name, "duration": frames / FPS}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", type=Path, required=True)
    parser.add_argument("--palette", type=Path, required=True)
    parser.add_argument("--cookies", type=Path)
    parser.add_argument("--exclude", type=Path)
    parser.add_argument("--local", type=Path, help="Offline qualification input; never downloads or opens an account")
    args = parser.parse_args()
    stage = args.stage.resolve()
    stage.mkdir(parents=True, exist_ok=False)
    try:
        if args.local:
            original = stage / "original.mp4"
            shutil.copyfile(args.local, original)
            clip = {"id": "local", "url": ""}
        else:
            if not args.cookies:
                raise MediaError("cookies_missing")
            excluded = set(json.loads(args.exclude.read_text(encoding="utf-8"))) if args.exclude else set()
            clip = asyncio.run(recommendations(args.cookies, excluded))
            original = download(clip, args.cookies, stage)
        result = prepare(original, stage, args.palette)
        result["id"] = clip["id"]
        atomic_json(stage / "manifest.json", result)
        print(json.dumps({"ok": True, "id": clip["id"]}), flush=True)
    except BaseException as error:
        # Sanitized codes ONLY: Playwright/HTTP errors can embed cookies or signed download URLs.
        code = str(error) if isinstance(error, MediaError) else "worker_failed"
        print(json.dumps({"ok": False, "error": code}), flush=True)
        raise SystemExit(1) from None


if __name__ == "__main__":
    main()
