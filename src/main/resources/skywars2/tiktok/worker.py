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

FPS, WIDTH, HEIGHT, RATE = 20, 480, 854, 48000
JPEG_WIDTH, JPEG_HEIGHT, CHANNELS = 720, 1280, 2
ANALYSIS_RATE = 24000
AUDIO_FPS, SLICES, WINDOW = 60, 3, 2400
SAMPLES = RATE // FPS
MAX_SECONDS, MAX_ORIGINAL, MAX_JPEG = 300, 64 * 1024 * 1024, 131072
HEADER = struct.Struct(">4sIIIIII")  # TKP3, frames, fps, palette width/height, rate, bands
BANDS = 48


def spectral_module():
    import importlib.util
    spec = importlib.util.spec_from_file_location('skywars_spectral', Path(__file__).with_name('spectral.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


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


def candidates_from_response(data, excluded: set[str]) -> list[dict]:
    result = []
    items = data.get("itemList", []) if isinstance(data, dict) else []
    if not isinstance(items, list):
        return result
    for item in items:
        if not isinstance(item, dict) or not isinstance(item.get("video"), dict) or not item["video"]:
            continue
        ident = str(item.get("id", ""))
        author = item.get("author")
        if not re.fullmatch(r"[0-9]{8,24}", ident) or ident in excluded or not isinstance(author, dict):
            continue
        author = author.get("uniqueId", "")
        # Usernames can contain non-Latin letters. An ad/malformed item must not discard the rest of a feed response.
        if not isinstance(author, str) or not 1 <= len(author) <= 64 or not all(c.isalnum() or c in "_." for c in author):
            continue
        candidate = {"id": ident, "url": "https://www.tiktok.com/@" + urllib.parse.quote(author, safe="_.") + "/video/" + ident}
        video = item.get("video")
        media = []
        if isinstance(video, dict):
            rates = video.get("bitrateInfo") or []
            if not isinstance(rates, list):
                rates = []
            for bitrate in sorted((b for b in rates if isinstance(b, dict)),
                                  key=lambda b: b.get("Bitrate", 0) if isinstance(b.get("Bitrate", 0), (int, float)) else 0, reverse=True):
                addr = bitrate.get("PlayAddr", {})
                if isinstance(addr, dict):
                    media.extend(u for u in addr.get("UrlList", []) if isinstance(u, str))
            for key in ("playAddr", "downloadAddr"):
                value = video.get(key)
                if isinstance(value, str):
                    media.append(value)
            if media:
                candidate["media"] = list(dict.fromkeys(media))[:6]
        result.append(candidate)
    return result


async def recommendations(cookies: Path, excluded: set[str], diagnostics: dict | None = None) -> dict:
    """Let TikTok's own page perform/sign its authenticated For You request.

    No invented public trending endpoint or unrelated account/profile fallback.
    A fresh isolated browser context has only explicitly imported TikTok cookies.
    """
    from playwright.async_api import async_playwright
    diagnostics = diagnostics if diagnostics is not None else {}
    diagnostics.update({"feedResponses": 0, "responseItems": 0, "candidates": 0, "parseErrorTypes": {}, "ssrCandidates": 0})
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
            context = await browser.new_context(viewport={"width": 1280, "height": 800})
            await context.add_cookies(cookies_for_browser(cookies))
            page = await context.new_page()
            candidates: list[dict] = []

            async def response(res):
                host = urllib.parse.urlparse(res.url).hostname or ""
                if (host == "www.tiktok.com" or host.endswith(".tiktok.com")) and "/api/recommend/item_list" in res.url:
                    diagnostics["feedResponses"] += 1
                    try:
                        data = await res.json()
                        items = data.get("itemList", []) if isinstance(data, dict) else []
                        diagnostics["responseItems"] += len(items) if isinstance(items, list) else 0
                        parsed = candidates_from_response(data, excluded)
                        candidates.extend(parsed); diagnostics["candidates"] += len(parsed)
                    except Exception as error:
                        # Exception types/counts are safe; messages can contain signed addresses or account data.
                        kind = type(error).__name__; diagnostics["parseErrorTypes"][kind] = diagnostics["parseErrorTypes"].get(kind, 0) + 1

            page.on("response", response)
            await page.goto("https://www.tiktok.com/foryou", wait_until="domcontentloaded", timeout=45000)
            for attempt in range(16):
                if candidates:
                    candidate = candidates[0]
                    candidate["user_agent"] = await page.evaluate("navigator.userAgent")
                    candidate["cookies"] = await context.cookies()
                    return candidate
                # Modern For You also hydrates initial recommendations from its own page JSON.
                # This is the same account/page, not a public trending/profile fallback.
                if attempt in (1, 5, 10):
                    try:
                        hydrated = await page.evaluate("""() => {
                            const node=document.getElementById('__UNIVERSAL_DATA_FOR_REHYDRATION__');
                            const scope=JSON.parse(node?.textContent||'{}').__DEFAULT_SCOPE__||{};
                            const found=[]; const walk=(x,depth)=>{if(!x||typeof x!=='object'||depth>12)return;
                              if(x.id&&x.video&&x.author)found.push(x);for(const v of Object.values(x))walk(v,depth+1)};
                            walk(scope,0);return found;
                        }""")
                        parsed = candidates_from_response({"itemList": hydrated}, excluded)
                        diagnostics["ssrCandidates"] += len(parsed); candidates.extend(parsed)
                    except Exception as error:
                        kind = type(error).__name__; diagnostics["parseErrorTypes"][kind] = diagnostics["parseErrorTypes"].get(kind, 0) + 1
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


def download(clip: dict, cookies: Path, stage: Path, diagnostics: dict | None = None) -> Path:
    # Use the signed media addresses from the actual authenticated feed when available.
    # Stream in bounded chunks: no full video body in RAM and no signed URL in a process command line/log.
    diagnostics = diagnostics if diagnostics is not None else {}
    diagnostics["mediaAddressCount"] = len(clip.get("media", []))
    diagnostics["directAttempts"] = []
    if clip.get("media"):
        import requests
        original = stage / "original.mp4"
        partial = stage / "original.mp4.part"
        with requests.Session() as session:
            for cookie in clip.get("cookies", cookies_for_browser(cookies)):
                expires = cookie.get("expires", -1)
                session.cookies.set(cookie["name"], cookie["value"], domain=cookie["domain"], path=cookie["path"],
                                    secure=cookie["secure"], expires=int(expires) if isinstance(expires, (int, float)) and expires > 0 else None)
            headers = {"Referer": "https://www.tiktok.com/", "Range": "bytes=0-",
                       "User-Agent": clip.get("user_agent", "Mozilla/5.0")}
            for url in clip["media"]:
                parsed = urllib.parse.urlparse(url)
                host = parsed.hostname or ""
                if parsed.scheme != "https" or not any(host == suffix or host.endswith("." + suffix)
                        for suffix in ("tiktok.com", "tiktokcdn.com", "tiktokcdn-us.com", "tiktokv.com", "muscdn.com")):
                    continue
                attempt = {"host": host}; diagnostics["directAttempts"].append(attempt)
                try:
                    started = time.monotonic()
                    with session.get(url, headers=headers, stream=True, timeout=(15, 20)) as response:
                        attempt["httpStatus"] = response.status_code
                        if response.status_code not in (200, 206):
                            continue
                        if int(response.headers.get("Content-Length", "0")) > MAX_ORIGINAL:
                            raise MediaError("download_size")
                        count = 0
                        with partial.open("wb") as out:
                            for block in response.iter_content(65536):
                                count += len(block)
                                if count > MAX_ORIGINAL or time.monotonic() - started > 120:
                                    raise MediaError("download_size")
                                if count == len(block) and b"ftyp" not in block[:64]:
                                    raise MediaError("download_failed")
                                out.write(block)
                        if count:
                            attempt["downloadedBytes"] = count
                            os.replace(partial, original)
                            return original
                except Exception as error:
                    attempt["errorType"] = type(error).__name__  # Messages may contain signed addresses.
                finally:
                    partial.unlink(missing_ok=True)
    cookie_file = stage / "download-cookies.txt"
    netscape_cookies(cookies, cookie_file)
    try:
        execute([sys.executable, "-m", "yt_dlp", "--ignore-config", "--no-playlist", "--quiet", "--no-warnings",
                 "--cookies", str(cookie_file), "--max-filesize", str(MAX_ORIGINAL), "--socket-timeout", "15",
                 "--retries", "2", "--fragment-retries", "2", "--concurrent-fragments", "1",
                 "--restrict-filenames", "--no-write-info-json", "--no-write-thumbnail", "--no-mtime",
                 "-f", "best[ext=mp4]/best", "-o", str(stage / "original.%(ext)s"), urllib.parse.unquote(clip["url"])], 180, "download_failed")
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
    import zlib
    mjpeg, pcm = (stage / n for n in ("video.mjpeg", "audio.pcm"))
    # One JPEG stream bounds staging disk instead of an uncompressed multi-gigabyte RGB movie.
    filters = f"fps={FPS},scale={JPEG_WIDTH}:{JPEG_HEIGHT}:force_original_aspect_ratio=decrease,pad={JPEG_WIDTH}:{JPEG_HEIGHT}:(ow-iw)/2:(oh-ih)/2:black"
    probe = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "a:0", "-show_entries", "stream=index",
                            "-of", "csv=p=0", str(original)], stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                           stderr=subprocess.DEVNULL, timeout=20, check=True,
                           creationflags=subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0)
    audio_args = (["-map", "0:a:0", "-t", str(MAX_SECONDS), "-threads", "1", "-ac", str(CHANNELS), "-ar", str(RATE),
                   "-c:a", "pcm_s16le", "-f", "s16le", str(pcm)] if probe.stdout.strip() else [])
    if not audio_args:
        pcm.touch()
    execute([ffmpeg, "-hide_banner", "-loglevel", "error", "-nostdin", "-y", "-threads", "1", "-i", str(original),
             "-filter_threads", "1", "-vf", filters, "-map", "0:v:0", "-t", str(MAX_SECONDS), "-threads", "1",
             "-c:v", "mjpeg", "-q:v", "4", "-f", "image2pipe", str(mjpeg), *audio_args], 240, "transcode_failed")
    lut = palette_lut(json.loads(palette.read_text(encoding="utf-8")))
    edges = np.geomspace(90, 10000, BANDS + 1)
    frequencies = np.fft.rfftfreq(WINDOW, 1 / ANALYSIS_RATE)
    masks = [(frequencies >= edges[i]) & (frequencies < edges[i + 1]) for i in range(BANDS)]
    window = np.hanning(WINDOW)
    offsets = []
    with pcm.open("rb") as audio, (stage / "clip.tkp").open("w+b") as out:
        out.write(HEADER.pack(b"TKP3", 0, FPS, WIDTH, HEIGHT, RATE, BANDS))
        for frame, jpeg in enumerate(jpeg_frames(mjpeg)):
            if frame >= MAX_SECONDS * FPS: raise MediaError("duration_invalid")
            offsets.append(out.tell())
            image = Image.open(io.BytesIO(jpeg)).convert("RGB")
            if image.size != (JPEG_WIDTH, JPEG_HEIGHT): raise MediaError("frame_size")
            pixels = np.asarray(image.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS), dtype=np.uint8).reshape(-1, 3).astype(np.uint16)
            indices = ((pixels[:, 0] >> 3) << 10) | ((pixels[:, 1] >> 3) << 5) | (pixels[:, 2] >> 3)
            mapped = zlib.compress(lut[indices].tobytes(), 3)
            levels = bytearray()
            for part in range(SLICES):
                begin = frame * SAMPLES + part * (RATE // AUDIO_FPS) - WINDOW
                audio.seek(max(0, begin) * 4)
                missing = max(0, -begin) * 4
                segment = (b"\0" * missing + audio.read(WINDOW * 8 - missing)).ljust(WINDOW * 8, b"\0")
                # Analyze channels independently: opposite stereo phases must not cancel the vanilla fallback.
                wave = np.frombuffer(segment, dtype="<i2").reshape(-1, 2)[::2].astype(np.float32) / 32768
                power = np.square(np.abs(np.fft.rfft(wave * window[:, None], axis=0)) / (WINDOW / 2)).mean(axis=1)
                levels.extend(min(255, round(float(np.sqrt(power[m].sum())) * 255 * 2)) for m in masks)
            audio.seek(frame * SAMPLES * 4);samples = audio.read(SAMPLES * 4).ljust(SAMPLES * 4, b"\0")
            if len(jpeg) > MAX_JPEG:
                for quality in (65, 45, 25, 15):
                    encoded = io.BytesIO();image.save(encoded, "JPEG", quality=quality);jpeg = encoded.getvalue()
                    if len(jpeg) <= MAX_JPEG: break
            if len(jpeg) > MAX_JPEG: raise MediaError("jpeg_size")
            out.write(struct.pack(">II", len(mapped), len(jpeg)));out.write(mapped);out.write(levels);out.write(samples);out.write(jpeg)
            if out.tell() > 640 * 1024 * 1024: raise MediaError("cache_full")
        frames = len(offsets)
        if not frames: raise MediaError("duration_invalid")
        out.seek(0);out.write(HEADER.pack(b"TKP3", frames, FPS, WIDTH, HEIGHT, RATE, BANDS))
    with (stage / "clip.idx").open("wb") as index:
        for offset in offsets:
            index.write(struct.pack(">Q", offset))
    spectral = spectral_module().prepare_pcm(pcm, frames, stage / "clip.tka")
    for p in (mjpeg, pcm):
        p.unlink(missing_ok=True)
    return {"format": 3, "frames": frames, "fps": FPS, "width": WIDTH, "height": HEIGHT,
            "rate": RATE, "channels": CHANNELS, "jpeg_width": JPEG_WIDTH, "jpeg_height": JPEG_HEIGHT, "bands": BANDS, "audio_fps": AUDIO_FPS, "spectral": spectral, "original": original.name, "duration": frames / FPS}


def main():
    # A20-video queue must not turn background preparation into a CPU/IO spike.
    # Children (ffmpeg/browser) inherit the worker's CPU affinity and priority.
    if os.name == "nt":
        import ctypes
        k=ctypes.WinDLL("kernel32",use_last_error=True);nt=ctypes.WinDLL("ntdll")
        handle,number=ctypes.c_void_p,ctypes.c_ulong
        k.GetCurrentProcess.restype=handle
        k.SetPriorityClass.argtypes=[handle,number];k.SetProcessAffinityMask.argtypes=[handle,ctypes.c_size_t]
        k.SetProcessInformation.argtypes=[handle,ctypes.c_int,handle,number]
        nt.NtSetInformationProcess.argtypes=[handle,ctypes.c_int,handle,number]
        process=k.GetCurrentProcess();cores=os.cpu_count() or 1
        affinity=(1<<(cores-1))|(1<<max(0,cores-3));memory,io=number(4),number(1)
        if not (k.SetPriorityClass(process,0x40) and k.SetProcessAffinityMask(process,affinity)
                and k.SetProcessInformation(process,0,ctypes.byref(memory),ctypes.sizeof(memory))
                and nt.NtSetInformationProcess(process,33,ctypes.byref(io),ctypes.sizeof(io))==0):
            raise MediaError("resource_limits")
    elif hasattr(os,"sched_getaffinity"):
        os.nice(10);available=sorted(os.sched_getaffinity(0));os.sched_setaffinity(0,available[-2:])
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage", type=Path, required=True)
    parser.add_argument("--palette", type=Path)
    parser.add_argument("--spectral-source", type=Path, help="Upgrade prepared audio locally without account or video conversion")
    parser.add_argument("--cookies", type=Path)
    parser.add_argument("--exclude", type=Path)
    parser.add_argument("--local", type=Path, help="Offline qualification input; never downloads or opens an account")
    args = parser.parse_args()
    stage = args.stage.resolve()
    stage.mkdir(parents=True, exist_ok=False)
    diagnostics = {}
    try:
        if args.spectral_source:
            result = spectral_module().migrate(args.spectral_source, stage)
            atomic_json(stage / "manifest.json", result)
            print(json.dumps({"ok": True, "audioOnly": True}), flush=True)
            return
        if not args.palette:
            raise MediaError("palette_missing")
        if args.local:
            original = stage / "original.mp4"
            shutil.copyfile(args.local, original)
            clip = {"id": "local", "url": ""}
        else:
            if not args.cookies:
                raise MediaError("cookies_missing")
            excluded = set(json.loads(args.exclude.read_text(encoding="utf-8"))) if args.exclude else set()
            clip = asyncio.run(recommendations(args.cookies, excluded, diagnostics))
            original = download(clip, args.cookies, stage, diagnostics)
        result = prepare(original, stage, args.palette)
        result["id"] = clip["id"]
        atomic_json(stage / "manifest.json", result)
        print(json.dumps({"ok": True, "id": clip["id"]}), flush=True)
    except BaseException as error:
        # Sanitized codes ONLY: Playwright/HTTP errors can embed cookies or signed download URLs.
        code = str(error) if isinstance(error, MediaError) else "worker_failed"
        import traceback
        diagnostics["failureType"] = type(error).__name__
        diagnostics["failureLocations"] = [{"file": Path(frame.filename).name, "line": frame.lineno, "function": frame.name}
                                            for frame in traceback.extract_tb(error.__traceback__)][-6:]
        diagnostics["networkCodes"] = list(dict.fromkeys(re.findall(r"net::[A-Z_0-9]+", str(error))))
        if diagnostics:
            atomic_json(stage / "diagnostics.json", diagnostics)
        print(json.dumps({"ok": False, "error": code}), flush=True)
        raise SystemExit(1) from None


if __name__ == "__main__":
    main()
