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


def main():
    limits.limit_resources()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, required=True)
    parser.add_argument("--overlay", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--native-compiler", action="store_true", help="Also compile core glint/per-face/OIT variants with the game's shaderc")
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
                text = (args.overlay / f"assets/minecraft/shaders/core/{kind}.fsh").read_text(encoding="utf-8")
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
    old = ctx.program(vertex_shader=screen_vertex, fragment_shader=source("assets/minecraft/shaders/post/bloom_sample.fsh", False))
    results = []

    def raw(rgb, alpha):
        pixels = np.zeros((height, width, 4), dtype="u1")
        pixels[72:88, 120:136] = [*rgb, alpha]
        return texture(pixels.tobytes())

    # Same RGB must be harmless on every ordinary renderer, including alpha255 skins.
    for rgb in ((255, 14, 14), (0, 48, 255), (255, 3, 3), (255, 142, 50)):
        for kind in programs:
            for alpha in ((255, 254) if kind in ("terrain", "particle") else (255,)):
                image = raw(rgb, alpha)
                scene, _ = render(programs[kind], {"Sampler0": image})
                extracted, pixels = render(sample, {"DiffuseSampler": scene})
                assert not np.any(pixels[:, :, :3]), ("RGB collision", kind, rgb, alpha)
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

    # The old exact same unmarked red frame DOES emit: guard is a behavioral fix.
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
    assert not np.any(pixels[:, :, :3]), "Transport bytes emitted light"
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
    for name, digest in hashes.items():
        assert hashlib.sha256((args.pack / name).read_bytes()).hexdigest() == digest
    report = {"passed": True, "renderer": ctx.info["GL_RENDERER"], "cases": results,
              "oldRgbOnlyNegativeControlEmits": True, "authorPackUnchanged": True,
              "scope": "Actual opaque/cutout material shaders and RGBA8 post passes on GPU; no physical Minecraft reload or OIT/translucent attachment transport claim."}
    (args.output / "GPU_RESULTS.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report))


if __name__ == "__main__":
    main()
