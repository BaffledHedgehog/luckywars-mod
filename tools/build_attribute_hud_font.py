#!/usr/bin/env python3
"""Build the AttributeHud font resources in resourcepack_template only."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import BinaryIO

from fontTools.fontBuilder import FontBuilder
from fontTools.pens.ttGlyphPen import TTGlyphPen

PROJECT_ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = PROJECT_ROOT / "resourcepack_template" / "assets" / "lbc" / "font" / "attribute_hud"
DEFAULT_RESOURCEPACK = Path(
    r"C:\Users\yarik_temp\AppData\Roaming\FreesmLauncher\instances\26.3(1)"
    r"\minecraft\resourcepacks\all_in_one_1.21.8"
)
UPM = 1000
ADVANCE = 600
CELL = 100
SLOT_RANGE = range(-40, 41)

# 5x7 monochrome pixel forms authored here; no third-party fonts or images.
PATTERNS: dict[str, tuple[str, ...]] = {
    "0": ("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
    "1": ("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
    "2": ("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
    "3": ("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
    "4": ("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
    "5": ("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
    "6": ("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
    "7": ("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
    "8": ("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
    "9": ("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
    ".": ("00000", "00000", "00000", "00000", "00000", "00000", "10000"),
    "-": ("00000", "00000", "00000", "01110", "00000", "00000", "00000"),
    "+": ("00000", "00100", "00100", "11111", "00100", "00100", "00000"),
    "N": ("10001", "11001", "11001", "10101", "10011", "10011", "10001"),
    "a": ("00000", "01110", "00001", "01111", "10001", "10011", "01101"),
    "n": ("00000", "10110", "11001", "10001", "10001", "10001", "10001"),
    "I": ("01110", "00100", "00100", "00100", "00100", "00100", "01110"),
    "f": ("00110", "01001", "01000", "11100", "01000", "01000", "01000"),
    "i": ("00100", "00000", "01100", "00100", "00100", "00100", "01110"),
    "t": ("01000", "01000", "11100", "01000", "01000", "01001", "00110"),
    "y": ("00000", "10001", "10001", "10001", "01111", "00001", "01110"),
    " ": ("00000",) * 7,
    "♥": ("00000", "01010", "11111", "11111", "01110", "00100", "00000"),
    "◆": ("00100", "01110", "11111", "11111", "01110", "00100", "00000"),
    "⚔": ("10001", "01010", "00100", "11111", "00100", "01010", "10001"),
    "✦": ("00100", "10101", "01110", "11111", "01110", "10101", "00100"),
    "↗": ("00001", "00011", "00101", "01001", "10000", "11111", "00000"),
    "↑": ("00100", "01110", "10101", "00100", "00100", "00100", "00100"),
    "◈": ("00100", "01110", "11111", "11011", "11111", "01110", "00100"),
    "↓": ("00100", "00100", "00100", "00100", "10101", "01110", "00100"),
    "☘": ("00100", "01110", "11111", "01110", "00100", "00100", "01100"),
    "◇": ("00100", "01010", "10001", "10001", "01010", "00100", "00000"),
    "·": ("00000", "00000", "00000", "00100", "00000", "00000", "00000"),
}


def glyph_name(char: str) -> str:
    return f"uni{ord(char):04X}"


def make_glyph(pattern: tuple[str, ...]):
    pen = TTGlyphPen(None)
    for row_index, row in enumerate(pattern):
        for col_index, bit in enumerate(row):
            if bit != "1":
                continue
            x0 = col_index * CELL
            y1 = (7 - row_index) * CELL
            x1 = x0 + CELL
            y0 = y1 - CELL
            pen.moveTo((x0, y0))
            pen.lineTo((x1, y0))
            pen.lineTo((x1, y1))
            pen.lineTo((x0, y1))
            pen.closePath()
    return pen.glyph()


def build_ttf(destination: Path | BinaryIO) -> None:
    order = [".notdef", *(glyph_name(c) for c in PATTERNS)]
    fb = FontBuilder(UPM, isTTF=True)
    fb.setupGlyphOrder(order)
    fb.setupCharacterMap({ord(c): glyph_name(c) for c in PATTERNS})
    glyphs = {".notdef": TTGlyphPen(None).glyph()}
    glyphs.update({glyph_name(c): make_glyph(p) for c, p in PATTERNS.items()})
    fb.setupGlyf(glyphs)
    fb.setupHorizontalMetrics({name: (200 if name == glyph_name(".") else ADVANCE, 0) for name in order})
    fb.setupHorizontalHeader(ascent=800, descent=-200)
    fb.setupNameTable({
        "familyName": "SkyWars2 Attribute HUD Pixel",
        "styleName": "Regular",
        "uniqueFontIdentifier": "SkyWars2AttributeHudPixel-Regular-v1",
        "fullName": "SkyWars2 Attribute HUD Pixel Regular",
        "psName": "SkyWars2AttributeHudPixel-Regular",
        "version": "Version 1.0",
    })
    fb.setupOS2(sTypoAscender=800, sTypoDescender=-200, usWinAscent=800, usWinDescent=200)
    fb.setupPost()
    fb.setupMaxp()
    # Stable metadata keeps repeated delta generation from recreating the same font.
    fb.font.recalcTimestamp = False
    fb.font["head"].created = fb.font["head"].modified = 3_800_000_000
    fb.save(destination)


def font_json(slot: int) -> dict:
    return {
        "providers": [{
            "type": "ttf",
            "file": "lbc:attribute_hud/glyphs.ttf",
            "shift": [0, slot * 9],
            "size": 10,
            "oversample": 2,
        }]
    }


def verify_ttf(path: Path | BinaryIO) -> None:
    from fontTools.ttLib import TTFont

    font = TTFont(path)
    mapping = font.getBestCmap()
    expected = {ord(c) for c in PATTERNS}
    if set(mapping) != expected:
        missing = sorted(expected - set(mapping))
        extra = sorted(set(mapping) - expected)
        raise RuntimeError(f"TTF cmap mismatch: missing={missing}, extra={extra}")
    metrics = font["hmtx"].metrics
    bad = {c: metrics[mapping[ord(c)]][0] for c in PATTERNS
           if metrics[mapping[ord(c)]][0] != (200 if c == "." else ADVANCE)}
    if bad:
        raise RuntimeError(f"TTF advance mismatch (digits {ADVANCE}, decimal point 200 units): {bad}")


def check_applied_pack(pack_root: Path) -> None:
    applied_font_dir = pack_root / "assets" / "lbc" / "font" / "attribute_hud"
    if applied_font_dir.exists():
        raise RuntimeError(
            f"Refusing to generate: resource pack already has assets/lbc/font/attribute_hud: {applied_font_dir}. "
            "Do not recreate an applied template."
        )


def import_attribute_icons(pack_root: Path, vanilla_jar: Path | None, force: bool) -> None:
    """Reference the author's existing PNGs; write only pending JSON/TTF, never images."""
    import hashlib
    import math
    import re
    import zipfile
    from io import BytesIO
    from PIL import Image

    template = PROJECT_ROOT / "resourcepack_template"
    source = (PROJECT_ROOT / "src/main/java/me/skywars2/game/AttributeHud.java").read_text(encoding="utf-8-sig")
    icons = {int(code, 16): name for name, code in
             re.findall(r'case "([^"]+)" -> "\\u([0-9A-Fa-f]{4})";', source)}
    if not icons:
        raise RuntimeError("No explicit attribute icon mappings found")
    source_hashes = {}

    def read_pack(relative: str) -> bytes:
        data = (pack_root / relative).read_bytes()
        source_hashes[relative] = hashlib.sha256(data).hexdigest()
        return data

    def texture(relative: str) -> bytes:
        if (pack_root / relative).is_file():
            return read_pack(relative)
        candidates = [vanilla_jar] if vanilla_jar else sorted(
            (PROJECT_ROOT / ".gradle/loom-cache/minecraftMaven").rglob("*-26.3.jar"))
        for candidate in candidates:
            with zipfile.ZipFile(candidate) as archive:
                if relative in archive.namelist():
                    return archive.read(relative)
        raise RuntimeError(f"Missing existing texture {relative}; supply --vanilla-jar")

    providers = json.loads(read_pack("assets/minecraft/font/default.json").decode("utf-8-sig"))["providers"]
    bitmaps, padding, origin = [], {}, {}
    for provider in providers:
        if provider.get("type") != "bitmap":
            continue
        grid = provider["chars"]
        matching = [(row, col, ord(char)) for row, line in enumerate(grid)
                    for col, char in enumerate(line) if ord(char) in icons]
        if not matching:
            continue
        namespace, path = provider["file"].split(":", 1)
        raw = texture(f"assets/{namespace}/textures/{path}")
        with Image.open(BytesIO(raw)) as image:
            image = image.convert("RGBA")
        columns, rows = len(grid[0]), len(grid)
        if image.width % columns or image.height % rows:
            raise RuntimeError(f"Invalid original bitmap grid: {provider['file']}")
        width, height = image.width // columns, image.height // rows
        if max(width, height) + 1 > 256:
            raise RuntimeError(f"Original glyph exceeds native atlas cell: {provider['file']}")
        for row, col, cp in matching:
            if cp in origin:
                raise RuntimeError(f"Duplicate default bitmap for U+{cp:04X}")
            glyph = image.crop((col * width, row * height, (col + 1) * width, (row + 1) * height))
            bounds = glyph.getchannel("A").getbbox()
            if bounds is None:
                raise RuntimeError(f"Empty attribute icon U+{cp:04X}")
            advance = math.floor(bounds[2] * 9 / height + .5) + 1
            padding[chr(0xF0000 + cp)] = 10 - advance
            origin[cp] = {"attribute": icons[cp], "texture": provider["file"],
                          "sourceSize": [width, height], "advance": advance,
                          "pixelSha256": hashlib.sha256(glyph.tobytes()).hexdigest()}
        bitmap = dict(provider)
        bitmap.update(height=9, chars=["".join(c if ord(c) in icons else "\0" for c in line) for line in grid])
        bitmaps.append(bitmap)
    if set(origin) != set(icons):
        raise RuntimeError(f"Missing original attribute bitmaps: {sorted(set(icons) - set(origin))}")

    files = {}
    # The installed mod uses row_m6..row_p11. Translate the whole panel by 54 GUI px.
    # All bitmap ascents now satisfy ascent <= height without padding or resampling a PNG.
    slots = range(-6, 12)
    for slot in slots:
        offset = slot + 6
        tag = f"m{-slot}" if slot < 0 else f"p{slot}" if slot else "0"
        row_bitmaps = [dict(provider, ascent=8 - offset * 9) for provider in bitmaps]
        ttf = dict(font_json(offset)["providers"][0])
        config = {"providers": [*row_bitmaps, {"type": "space", "advances": padding}, ttf]}
        assert all(p["ascent"] <= p["height"] for p in row_bitmaps)
        relative = f"assets/lbc/font/attribute_hud/row_{tag}.json"
        files[relative] = (json.dumps(config, ensure_ascii=True, indent=2) + "\n").encode("utf-8")

    font_relative = "assets/lbc/font/attribute_hud/glyphs.ttf"
    if not (pack_root / font_relative).exists():
        buffer = BytesIO()
        build_ttf(buffer)
        buffer.seek(0)
        verify_ttf(buffer)
        files[font_relative] = buffer.getvalue()
    spacing_relative = "assets/lbc/font/attribute_hud/spacing.json"
    files[spacing_relative] = (json.dumps({"providers": [{"type": "space", "advances": {"\uE000": 1}}]},
                                        indent=2, ensure_ascii=True) + "\n").encode("utf-8")

    def differs(relative: str, data: bytes) -> bool:
        existing = pack_root / relative
        if not existing.is_file():
            return True
        old = read_pack(relative)
        return json.loads(old) != json.loads(data) if relative.endswith(".json") else old != data

    pending = {path: data for path, data in files.items() if differs(path, data)}
    for relative, data in pending.items():
        target = template / relative
        if target.exists() and target.read_bytes() != data and not force:
            raise RuntimeError(f"Existing template differs: {target}; inspect it or use --force")
    for relative, data in pending.items():
        target = template / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    for relative, digest in source_hashes.items():
        assert hashlib.sha256((pack_root / relative).read_bytes()).hexdigest() == digest
    report = PROJECT_ROOT / "build/verification/attribute-hud-icons.json"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps({"icons": {f"{cp:04X}": value for cp, value in origin.items()},
                                 "guiSize": 9, "slotAdvance": 10, "rows": list(slots),
                                 "rowsPerColumn": 18, "panelOffsetY": 54, "writesImages": False,
                                 "pendingFiles": list(pending), "sourceSha256": source_hashes,
                                 "sourceUnchanged": True}, indent=2) + "\n", encoding="utf-8")
    print(f"Verified {len(icons)} existing PNG references and {len(slots)} rows; {len(pending)} pending JSON/TTF files")
    print("No PNG files created or moved. Source resource pack unchanged. Report:", report)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true",
                        help="overwrite generated target files in resourcepack_template")
    parser.add_argument("--resourcepack-root", type=Path, default=DEFAULT_RESOURCEPACK,
                        help="read-only resource pack root used to prevent recreating an applied template")
    parser.add_argument("--import-icons", action="store_true",
                        help="prepare a delta importing the current AttributeHud switch icons into applied row fonts")
    parser.add_argument("--vanilla-jar", type=Path, help="vanilla client/merged JAR for textures inherited from Minecraft")
    args = parser.parse_args()

    try:
        if args.import_icons:
            import_attribute_icons(args.resourcepack_root, args.vanilla_jar, args.force)
            return 0
        check_applied_pack(args.resourcepack_root)
        OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
        targets = [OUTPUT_DIR / "glyphs.ttf", OUTPUT_DIR / "spacing.json"]
        targets.extend(OUTPUT_DIR / f"row_{'m' + str(abs(slot)) if slot < 0 else 'p' + str(slot) if slot > 0 else '0'}.json"
                       for slot in SLOT_RANGE)
        existing = [p for p in targets if p.exists()]
        unexpected = [p for p in OUTPUT_DIR.iterdir() if p not in targets]
        if (existing or unexpected) and not args.force:
            paths = [*existing, *unexpected]
            raise RuntimeError("Refusing to overwrite or ignore existing output files; inspect them or rerun with --force: "
                               + ", ".join(str(p) for p in paths))

        ttf_path = OUTPUT_DIR / "glyphs.ttf"
        build_ttf(ttf_path)
        verify_ttf(ttf_path)
        spacing_path = OUTPUT_DIR / "spacing.json"
        spacing_path.write_text(json.dumps({
            "providers": [{"type": "space", "advances": {"\uE000": 1}}]
        }, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")
        for slot in SLOT_RANGE:
            tag = f"m{abs(slot)}" if slot < 0 else f"p{slot}" if slot > 0 else "0"
            (OUTPUT_DIR / f"row_{tag}.json").write_text(
                json.dumps(font_json(slot), indent=2, ensure_ascii=True) + "\n", encoding="utf-8"
            )

        # Re-read every generated font JSON to verify provider ids and offsets.
        for slot in SLOT_RANGE:
            tag = f"m{abs(slot)}" if slot < 0 else f"p{slot}" if slot > 0 else "0"
            config = json.loads((OUTPUT_DIR / f"row_{tag}.json").read_text(encoding="utf-8"))
            provider = config["providers"][0]
            if provider != {
                "type": "ttf", "file": "lbc:attribute_hud/glyphs.ttf",
                "shift": [0, slot * 9], "size": 10, "oversample": 2,
            }:
                raise RuntimeError(f"Unexpected provider in row_{tag}.json: {provider}")
        spacing = json.loads(spacing_path.read_text(encoding="utf-8"))
        if spacing["providers"][0]["advances"] != {"\ue000": 1}:
            raise RuntimeError("Spacing provider does not contain U+E000 advance +1")
        print(f"Generated and verified {len(targets)} template files in {OUTPUT_DIR}")
        print(f"Verified {len(PATTERNS)} TTF cmap codepoints; digits advance={ADVANCE}, decimal point=200 units")
        return 0
    except Exception as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())


