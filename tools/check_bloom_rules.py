"""GPU regression: actual core material shaders -> masked extraction -> two-pass bloom.

Uses the installed pack read-only. No Minecraft process or author assets are changed.
"""
import argparse
import contextlib
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import runpy
import struct
import sys
import zipfile
import moderngl as gl
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("shader_limits", ROOT / "tools/check_timestop_shaderc.py")
limits = importlib.util.module_from_spec(spec)
spec.loader.exec_module(limits)


def check_music_staff(args, ctx, source, screen_vertex, programs, sample, composite,
                      texture, render, raw, sample_source, results, hashes, width, height):
    """Exercise the actual staff material pixels and the existing additive palette."""
    from PIL import Image
    relative = "assets/minecraft/textures/item/wand_52.png"
    original = (args.pack / relative).read_bytes()
    hashes[relative] = hashlib.sha256(original).hexdigest()
    authored = np.array(Image.open(args.pack / relative).convert("RGBA"))
    marked = np.array(Image.open(args.overlay / relative).convert("RGBA"))
    assert np.array_equal(authored[:, :, :3], marked[:, :, :3]), "Artist RGB was repainted"
    red = np.all(authored[:, :, :3] == [255, 0, 64], axis=2)
    dark = np.all(authored[:, :, :3] == [29, 28, 32], axis=2)
    assert red.sum() == 102 and dark.sum() == 159
    expected = authored.copy();expected[red | dark, 3] = 254
    assert np.array_equal(marked, expected), "Only the two material-alpha sets may change"
    results.append({"case": "artist-rgb-preserved", "redPixels": int(red.sum()),
                    "darkPixels": int(dark.sum()), "passed": True})

    # Verify the current model really maps every lightning face to this material.
    model_path = "assets/lbc/models/item/music_staff/staff.json"
    data = (args.pack / model_path).read_bytes();hashes[model_path] = hashlib.sha256(data).hexdigest()
    model = json.loads(data)
    assert model["textures"]["2"] == "minecraft:item/wand_52"
    group = next(g for g in model["groups"] if g["name"] == "zzzzz")
    for index in group["children"]:
        for face in model["elements"][index]["faces"].values():
            u, v, u1, v1 = face["uv"]
            x, x1 = sorted((round(2*u), round(2*u1)));y, y1 = sorted((round(2*v), round(2*v1)))
            assert np.all(marked[y:y1, x:x1] == [29, 28, 32, 254]), (index, face)
    assert not (args.overlay / model_path).exists(), "The authored geometry must stay original"
    results.append({"case": "actual-lightning-model-uv", "elements": len(group["children"]), "passed": True})

    # The exact same two-pass RGB result must remain for EVERY pre-existing rule.
    old_sample = ctx.program(vertex_shader=screen_vertex, fragment_shader=source("assets/minecraft/shaders/post/bloom_sample.fsh", False))
    old_composite = ctx.program(vertex_shader=screen_vertex, fragment_shader=source("assets/lbc/shaders/post/material_bloom.fsh", False))
    rule_path = "bloom_rules.json"
    rules_bytes = (args.pack / rule_path).read_bytes();hashes[rule_path] = hashlib.sha256(rules_bytes).hexdigest()

    def pipeline(scene, sampler=sample, composer=composite):
        extracted, extracted_pixels = render(sampler, {"DiffuseSampler": scene})
        extracted.filter = (gl.LINEAR, gl.LINEAR)
        horizontal, _ = render(composer, {"DiffuseSampler": extracted}, (1, 0))
        horizontal.filter = (gl.LINEAR, gl.LINEAR)
        final, pixels = render(composer, {"DiffuseSampler": scene, "BloomSampler": horizontal}, (0, 1))
        extracted.release();horizontal.release();final.release()
        return pixels, extracted_pixels

    for rule in json.loads(rules_bytes):
        image = raw(rule["source"], 254)
        scene, _ = render(programs["item"], {"Sampler0": image})
        reference, _ = pipeline(scene, old_sample, old_composite)
        actual, _ = pipeline(scene)
        assert np.array_equal(actual, reference), ("Existing bloom changed", rule["source"])
        results.append({"case": "existing-rule-pixel-identical", "rgb": rule["source"], "passed": True})
        image.release();scene.release()
    old_sample.release();old_composite.release()

    # A black halo must DARKEN the surrounding scene, while its core stays white.
    # Check both native item and entity fragments, not a manufactured post-only tag.
    background = np.array([160, 170, 180], dtype="u1")
    for rgb, dark_halo in (([29, 28, 32], True), ([255, 0, 64], False)):
        for kind in ("item", "entity"):
            raw_pixels = np.full((height, width, 4), 255, dtype="u1")
            raw_pixels[:, :, :3] = background
            raw_pixels[72:88, 120:136] = [*rgb, 254]
            image = texture(raw_pixels.tobytes())
            scene, scene_pixels = render(programs[kind], {"Sampler0": image})
            pixels, extracted = pipeline(scene)
            assert np.all(pixels[72:88, 120:136, :3] == 255), (kind, rgb, "Nonwhite core")
            assert np.array_equal(pixels[:, :, 3], scene_pixels[:, :, 3]), "Scene metadata changed"
            outside = pixels[70, 128, :3]
            if dark_halo:
                assert np.all(outside < background), (kind, "No black halo", outside)
                assert np.any(extracted[:, :, 3]) and not np.any(extracted[:, :, :3])
            else:
                assert outside[0] > background[0] and outside[0]-int(background[0]) > outside[1]-int(background[1])
                assert not np.any(extracted[:, :, 3]), "Colored halo polluted the dark channel"
            assert np.array_equal(pixels[60, 160, :3], background), "Halo became fullscreen"
            Image.fromarray(np.flipud(pixels)).save(args.output / f"staff-{'black' if dark_halo else 'red'}-{kind}.png")
            results.append({"case": "staff-white-core-and-halo", "kind": kind,
                            "dark": dark_halo, "outsideRgb": outside.tolist(), "passed": True})
            image.release();scene.release()

    energy = []
    for strength in (0.0, 0.25, 1.0, 3.0):
        old = "ivec3(29,28,32), ivec3(0,0,0), 2.0, LBC_CORE_WHITE)"
        configured = sample_source.replace(old, f"ivec3(29,28,32), ivec3(0,0,0), {strength}, LBC_CORE_WHITE)")
        assert configured != sample_source
        sampler = ctx.program(vertex_shader=screen_vertex, fragment_shader=configured)
        raw_pixels = np.full((height, width, 4), 255, dtype="u1");raw_pixels[:, :, :3] = background
        raw_pixels[72:88, 120:136] = [29, 28, 32, 254]
        image = texture(raw_pixels.tobytes());scene, _ = render(programs["item"], {"Sampler0": image})
        pixels, _ = pipeline(scene, sampler)
        outside = background.astype("i4") - pixels[:, :, :3].astype("i4")
        outside[72:88, 120:136] = 0
        assert np.all(outside >= 0) and np.all(pixels[72:88, 120:136, :3] == 255)
        energy.append(int(outside.sum()))
        sampler.release();image.release();scene.release()
    assert energy[0] == 0 and energy[0] < energy[1] < energy[2] < energy[3], energy
    results.append({"case": "black-halo-strength", "strengths": [0, .25, 1, 3], "darkeningEnergy": energy, "passed": True})

    actual_texture = np.array(Image.fromarray(marked).resize((width, height), Image.Resampling.NEAREST))
    for kind in ("item", "entity"):
        image = texture(actual_texture.tobytes())
        scene, _ = render(programs[kind], {"Sampler0": image})
        extracted, pixels = render(sample, {"DiffuseSampler": scene})
        red_mask = np.all(actual_texture[:, :, :3] == [255, 0, 64], axis=2)
        dark_mask = np.all(actual_texture[:, :, :3] == [29, 28, 32], axis=2)
        material = np.any(pixels, axis=2)
        assert np.any(material & red_mask) and np.any(material & dark_mask), "Staff material did not emit"
        assert not np.any(material & ~(red_mask | dark_mask)), "Handle or transparent texel emitted"
        results.append({"case": "actual-staff-texture", "kind": kind, "handleNeverEmits": True, "passed": True})
        image.release();scene.release();extracted.release()


def main():
    limits.limit_resources()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, required=True)
    parser.add_argument("--overlay", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--native-compiler", action="store_true", help="Also compile core glint/per-face/OIT variants with the game's shaderc")
    parser.add_argument("--music-staff", action="store_true", help="Check the staff texture, dark halo and all existing material rules")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    if args.native_compiler:
        compiler_overlay = args.output / "compiler-variants"
        shader_args = []
        oit = ["OIT", "OIT_COEFF_COUNT 8", "OIT_COEFF_ATTACHMENT_COUNT 2", "OIT_WAVELET_RANK 2"]
        profiles = {"glint": ["GLINT"], "per-face": ["PER_FACE_LIGHTING"],
                    "oit-depth": oit + ["OIT_ALPHA_ONLY", "OIT_DEPTH_BOUNDS"],
                    "oit-transmittance": oit + ["OIT_ALPHA_ONLY", "OIT_TRANSMITTANCE"],
                    "oit-accumulate": oit + ["OIT_ACCUMULATE"]}
        for kind in ("item", "entity", "terrain", "particle"):
            shader_args += ["--shader", f"assets/minecraft/shaders/core/{kind}.fsh"]
            for profile, defines in profiles.items():
                if profile == "glint" and kind not in ("item", "entity"): continue
                if profile == "per-face" and kind != "entity": continue
                relative = f"assets/verification/shaders/core/{kind}-{profile}.fsh"
                path = compiler_overlay / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                relative_source = f"assets/minecraft/shaders/core/{kind}.fsh"
                source_path = args.overlay / relative_source
                if not source_path.is_file(): source_path = args.pack / relative_source
                text = source_path.read_text(encoding="utf-8")
                text = text.replace("#version 330", "#version 330\n#define ALPHA_CUTOUT 0.1\n" + "\n".join("#define " + value for value in defines))
                path.write_text(text, encoding="utf-8")
                shader_args += ["--shader", relative]
        previous_args = sys.argv
        try:
            sys.argv = ["tools/check_timestop_shaderc.py", "--pack", str(args.pack), "--overlay", str(args.overlay),
                        "--overlay", str(compiler_overlay), "--output", str(args.output / "shaderc"), *shader_args]
            with (args.output / "shaderc-run.log").open("w", encoding="utf-8") as log, contextlib.redirect_stdout(log):
                runpy.run_path(str(ROOT / "tools/check_timestop_shaderc.py"), run_name="__main__")
        finally:
            sys.argv = previous_args
    jar = zipfile.ZipFile(next(p for p in (ROOT / ".gradle/loom-cache/minecraftMaven").rglob("*26.3.jar") if not p.name.endswith("-sources.jar")))
    hashes = {}

    def source(name, patched=True):
        for base in ((args.overlay, args.pack) if patched else (args.pack,)):
            path = base / name
            if path.is_file():
                data = path.read_bytes()
                if base == args.pack:
                    hashes[name] = hashlib.sha256(data).hexdigest()
                break
        else:
            data = jar.read(name)
        def include(match):
            namespace, relative = match[1].split(":", 1) if ":" in match[1] else ("minecraft", match[1])
            return source(f"assets/{namespace}/shaders/include/{relative}", patched)
        return re.sub(r'^\s*#(?:include|moj_import)\s*[<"]([^>"]+)[>"]\s*$', include, data.decode(), flags=re.M).replace("gl_VertexIndex", "gl_VertexID")

    ctx = gl.create_standalone_context(require=330)
    width, height = 256, 160
    vertex_head = "#version 330\n#extension GL_ARB_separate_shader_objects : require\n"
    triangle = "vec2 p=vec2((gl_VertexID<<1)&2,gl_VertexID&2);gl_Position=vec4(p*2.-1.,0.,1.);"
    screen_vertex = vertex_head + "out vec2 texCoord;void main(){" + triangle + "texCoord=p;}"

    def uniforms(program, direction=None, opacity=1.0):
        buffers = []
        for name in program:
            block = program[name]
            if not isinstance(block, gl.UniformBlock):
                continue
            data = bytearray(block.size)
            identity = np.eye(4, dtype="f4").tobytes()
            if name == "DynamicTransforms":
                data[:64] = identity
                data[64:128] = identity
                struct.pack_into("4f", data, 128, 1, 1, 1, opacity)
            elif name == "Globals":
                struct.pack_into("2f", data, 32, width, height)
            elif name == "TerrainUniform":
                data[:64] = identity
                struct.pack_into("2i", data, 64, 1, 1)
            elif name == "Fog":
                for offset in range(16, len(data) - 3, 4):
                    struct.pack_into("f", data, offset, 1_000_000 + offset * 100)
            elif name == "BlitConfig":
                struct.pack_into("4f", data, 0, 1, 1, 1, 1)
                if direction is not None:
                    struct.pack_into("2f", data, 16, *direction)
            block.binding = len(buffers)
            buffers.append(ctx.buffer(data))
        return buffers

    def texture(data):
        result = ctx.texture((width, height), 4, data)
        result.filter = (gl.NEAREST, gl.NEAREST)
        result.repeat_x = result.repeat_y = False
        return result

    def render(program, inputs, direction=None, opacity=1.0):
        target = texture(None)
        fbo = ctx.framebuffer([target])
        fbo.use()
        ctx.viewport = (0, 0, width, height)
        fbo.clear()
        blocks = uniforms(program, direction, opacity)
        for i, buffer in enumerate(blocks):
            buffer.bind_to_uniform_block(i)
        for i, (name, image) in enumerate(inputs.items()):
            if name in program:
                program[name].value = i
                image.use(i)
        vao = ctx.vertex_array(program, [])
        vao.render(vertices=3)
        vao.release()
        pixels = np.frombuffer(fbo.read(components=4), dtype="u1").reshape(height, width, 4).copy()
        for buffer in blocks:
            buffer.release()
        fbo.release()
        return target, pixels

    programs = {}
    inputs = {
        "sphericalVertexDistance": ("float", "0."), "cylindricalVertexDistance": ("float", "0."),
        "vertexColor": ("vec4", "vec4(1)"), "lightMapColor": ("vec4", "vec4(1)"),
        "overlayColor": ("vec4", "vec4(1)"), "texCoord0": ("vec2", "p"),
        "texCoordGlint": ("vec2", "p"), "chunkVisibility": ("float", "1."),
        "zoneMaterial": ("flat int", "0"), "zoneRelativePosition": ("vec3", "vec3(0)"),
        "meditationWarningFade": ("float", "0."), "meditationWarningProgress": ("float", "0."),
        "meditationWarningQuadCoord": ("vec2", "p"), "tmtTintSeed": ("flat uvec3", "uvec3(0)"),
        "tmtVertexLight": ("vec4", "vec4(1)"), "tmtSourceCells": ("flat uvec4", "uvec4(0)"),
        "atlasGlitchCode": ("flat float", "0."), "tmtEquipmentSeed": ("flat uvec2", "uvec2(0)"),
        "tiktokPixels": ("flat uvec4", "uvec4(0)"), "tiktokU": ("float", "0."),
        "customParticle": ("flat int", "0"), "particleCoord": ("vec2", "p")}
    for kind in ("item", "entity", "terrain", "particle"):
        fragment = source(f"assets/minecraft/shaders/core/{kind}.fsh")
        # Test the actual cutout/opaque branch; native shaderc separately covers variants.
        fragment = fragment.replace("#version 330", "#version 330\n#define ALPHA_CUTOUT 0.1")
        declarations = "".join(f"{typ.split()[0]+' ' if typ.startswith('flat ') else ''}out {typ.removeprefix('flat ')} {name};\n" for name, (typ, value) in inputs.items())
        values = "".join(f"{name}={value};" for name, (typ, value) in inputs.items())
        vertex = vertex_head + declarations + "void main(){" + triangle + values + "}"
        programs[kind] = ctx.program(vertex_shader=vertex, fragment_shader=fragment)
    sample_source = source("assets/minecraft/shaders/post/bloom_sample.fsh")
    composite_source = source("assets/lbc/shaders/post/material_bloom.fsh")
    sample = ctx.program(vertex_shader=screen_vertex, fragment_shader=sample_source)
    composite = ctx.program(vertex_shader=screen_vertex, fragment_shader=composite_source)
    # Explicit negative control also works after the author has applied the fix:
    # remove only the alpha admission gate and prove that RGB alone leaks bloom.
    guard = "if (!lbc_bloom_marked(scene)) return;"
    assert sample_source.count(guard) == 1
    old = ctx.program(vertex_shader=screen_vertex, fragment_shader=sample_source.replace(guard, ""))
    results = []

    def raw(rgb, alpha):
        pixels = np.zeros((height, width, 4), dtype="u1")
        pixels[72:88, 120:136] = [*rgb, alpha]
        return texture(pixels.tobytes())

    # Same RGB must be harmless on every ordinary renderer, including alpha255 skins.
    for rgb in ((255, 14, 14), (0, 48, 255), (255, 3, 3), (255, 142, 50), (255, 0, 64), (29, 28, 32)):
        for kind in programs:
            for alpha in ((255, 254) if kind in ("terrain", "particle") else (255,)):
                image = raw(rgb, alpha)
                scene, _ = render(programs[kind], {"Sampler0": image})
                extracted, pixels = render(sample, {"DiffuseSampler": scene})
                assert not np.any(pixels), ("RGB collision", kind, rgb, alpha)
                results.append({"case": "ordinary-rgb-collision", "kind": kind, "rgb": rgb, "alpha": alpha, "passed": True})
                image.release();scene.release();extracted.release()
    for kind in ("item", "entity"):
        image = raw((255, 14, 14), 255)
        scene, _ = render(programs[kind], {"Sampler0": image}, opacity=254 / 255)
        extracted, pixels = render(sample, {"DiffuseSampler": scene})
        assert not np.any(pixels[:, :, :3]), ("opacity masquerades as marker", kind)
        results.append({"case": "ordinary-opacity254", "kind": kind, "passed": True})
        image.release();scene.release();extracted.release()

    for rgb in ((255, 14, 14), (0, 48, 255), (255, 3, 3)):
        image = raw(rgb, 254)
        scene, pixels = render(programs["item"], {"Sampler0": image})
        assert np.all(pixels[72:88, 120:136, 3] == 254)
        extracted, pixels = render(sample, {"DiffuseSampler": scene})
        assert np.count_nonzero(np.any(pixels[:, :, :3], axis=2)) == 256
        extracted.filter = (gl.LINEAR, gl.LINEAR)
        horizontal, _ = render(composite, {"DiffuseSampler": extracted}, (1, 0))
        horizontal.filter = (gl.LINEAR, gl.LINEAR)
        final, pixels = render(composite, {"DiffuseSampler": scene, "BloomSampler": horizontal}, (0, 1))
        if rgb == (255, 3, 3):
            assert np.all(pixels[72:88, 120:136, :3] == 255), "White core not filled"
        else:
            assert np.all(pixels[76:84, 124:132, :3][:, :, list(rgb).index(255)] == 255)
        assert np.any(pixels[68:72, 124:132, :3]), "No halo outside geometry"
        results.append({"case": "marked-item-and-halo", "rgb": rgb, "whiteCore": rgb == (255, 3, 3), "passed": True})
        for image in (image, scene, extracted, horizontal, final):image.release()

    # The same unmarked red frame DOES emit without the explicit alpha gate.
    image = raw((255, 14, 14), 255)
    negative, pixels = render(old, {"DiffuseSampler": image})
    assert np.any(pixels[:, :, :3]), "Negative control unexpectedly rejects RGB-only"
    image.release();negative.release()

    # Artist edits must change halo strength monotonically without changing the core.
    energy = []
    for strength in (0.25, 1.0, 3.0):
        configured = sample_source.replace(
            "ivec3(255,3,3), 1.0, LBC_CORE_WHITE)",
            f"ivec3(255,3,3), {strength}, LBC_CORE_WHITE)")
        assert configured != sample_source or strength == 1.0
        configured_program = ctx.program(vertex_shader=screen_vertex, fragment_shader=configured)
        image = raw((255, 3, 3), 254)
        scene, _ = render(programs["item"], {"Sampler0": image})
        extracted, _ = render(configured_program, {"DiffuseSampler": scene})
        extracted.filter = (gl.LINEAR, gl.LINEAR)
        horizontal, _ = render(composite, {"DiffuseSampler": extracted}, (1, 0))
        horizontal.filter = (gl.LINEAR, gl.LINEAR)
        final, pixels = render(composite, {"DiffuseSampler": scene, "BloomSampler": horizontal}, (0, 1))
        outside = pixels[:, :, 0].copy();outside[72:88, 120:136] = 0
        energy.append(int(outside.sum()))
        assert np.all(pixels[72:88, 120:136, :3] == 255)
        for image in (image, scene, extracted, horizontal, final):image.release()
        configured_program.release()
    assert 0 < energy[0] < energy[1] < energy[2], energy
    results.append({"case": "weak-normal-strong", "strengths": [0.25, 1.0, 3.0], "haloEnergy": energy, "passed": True})

    configured = composite_source.replace(
        "ivec3(255,3,3), 1.0, LBC_CORE_WHITE)",
        "ivec3(0,255,0), 0.0, LBC_CORE_HALO)")
    assert configured != composite_source
    tinted = ctx.program(vertex_shader=screen_vertex, fragment_shader=configured)
    image = raw((255, 3, 3), 254)
    black = texture(bytes(width * height * 4))
    scene, _ = render(programs["item"], {"Sampler0": image})
    final, pixels = render(tinted, {"DiffuseSampler": scene, "BloomSampler": black}, (0, 1))
    assert np.all(pixels[72:88, 120:136, :3] == [0, 255, 0]), "Halo-color core not applied"
    results.append({"case": "custom-halo-colored-core", "passed": True})
    for image in (image, black, scene, final):image.release()
    tinted.release()
    transport = np.zeros((height, width, 4), dtype="u1")
    transport[:50, :171] = [255, 3, 3, 254]
    transport[-50:, :171] = [255, 3, 3, 254]
    image = texture(transport.tobytes())
    extracted, pixels = render(sample, {"DiffuseSampler": image})
    assert not np.any(pixels), "Transport bytes emitted a light or dark halo"
    black = texture(bytes(width * height * 4))
    final, pixels = render(composite, {"DiffuseSampler": image, "BloomSampler": black}, (0, 1))
    assert np.array_equal(pixels[:, :, :3], transport[:, :, :3]), "White core corrupted service strip"
    results.append({"case": "service-alpha-is-not-material", "passed": True})
    for image in (image, extracted, black, final):image.release()
    from PIL import Image
    for name in ("lightsaber_sith", "lightsaber_jedi"):
        relative = f"assets/minecraft/textures/item/{name}.png"
        data = (args.pack / relative).read_bytes()
        hashes[relative] = hashlib.sha256(data).hexdigest()
        with Image.open(args.pack / relative) as png:
            original_pixels = np.array(png.convert("RGBA").resize((width, height), Image.Resampling.NEAREST))
        image = texture(original_pixels.tobytes())
        scene, _ = render(programs["item"], {"Sampler0": image})
        extracted, pixels = render(sample, {"DiffuseSampler": scene})
        emitting = np.any(pixels[:, :, :3], axis=2)
        assert np.count_nonzero(emitting) > 0, (name, "blade stopped emitting")
        assert not np.any(emitting & (original_pixels[:, :, 3] != 254)), (name, "hilt emitted")
        results.append({"case": "actual-saber-texture", "texture": name, "emittingPixels": int(emitting.sum()), "hiltNeverEmits": True, "passed": True})
        for image in (image, scene, extracted):image.release()
    if args.music_staff:
        check_music_staff(args, ctx, source, screen_vertex, programs, sample, composite,
                          texture, render, raw, sample_source, results, hashes, width, height)
    for name, digest in hashes.items():
        assert hashlib.sha256((args.pack / name).read_bytes()).hexdigest() == digest
    report = {"passed": True, "renderer": ctx.info["GL_RENDERER"], "cases": results,
              "oldRgbOnlyNegativeControlEmits": True, "authorPackUnchanged": True,
              "scope": "Actual opaque/cutout material shaders and RGBA8 post passes on GPU; no physical Minecraft reload or OIT/translucent attachment transport claim."}
    (args.output / "GPU_RESULTS.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report))


if __name__ == "__main__":
    main()
