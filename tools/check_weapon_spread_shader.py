"""Standalone GPU proof; author resource pack is read-only. No game/server process."""
import copy
import json
import math
import re
import struct
import zipfile
from pathlib import Path

import moderngl
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
TEMPLATE = ROOT / "resourcepack_template"
RP = Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")
OUT = ROOT / "build/verification/weapon-spread-crosshair"
OUT.mkdir(parents=True, exist_ok=True)
JAR = zipfile.ZipFile(next(p for p in (ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3").glob("*.jar") if not p.name.endswith("-sources.jar")))


def source(path):
    for base in (TEMPLATE, RP):
        p = base / path
        if p.is_file():
            return p.read_text(encoding="utf-8")
    return JAR.read(str(path).replace("\\", "/")).decode()


def expand(s):
    def include(m):
        ns, name = m.group(1).split(":", 1)
        return expand(source(Path("assets") / ns / "shaders/include" / name))
    return re.sub(r"#include\s+<([^>]+)>", include, s).replace("gl_VertexIndex", "gl_VertexID")


def shader(path):
    return expand(source(Path(path)))


ctx = moderngl.create_standalone_context(require=330)
results = {"renderer": ctx.info["GL_RENDERER"], "checks": [], "geometry": []}


def check(condition, message):
    assert condition, message
    results["checks"].append(message)


core = ctx.program(vertex_shader=shader("assets/minecraft/shaders/core/rendertype_outline.vsh"),
                   fragment_shader=shader("assets/minecraft/shaders/core/rendertype_outline.fsh"))
check(True, "combined core outline shaders compile/link")
vsh = shader("assets/lbc/shaders/post/weapon_spread.vsh")
fsh = shader("assets/lbc/shaders/post/weapon_spread.fsh")
post = ctx.program(vertex_shader=vsh, fragment_shader=fsh)
check(True, "spread post shaders compile/link")
# Render bounds into float pixels through the ACTUAL post vertex shader.
bounds_program = ctx.program(vertex_shader=vsh, fragment_shader="""#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location=0) flat in vec4 spreadBounds;
layout(location=0) out vec4 fragColor;
void main(){fragColor=spreadBounds;}""")
post["SceneSampler"].value = 0
post["SpreadDataSampler"].value = 1
bounds_program["SpreadDataSampler"].value = 1
vao = ctx.vertex_array(post, [])
bounds_vao = ctx.vertex_array(bounds_program, [])


def camera(pitch, yaw=0.0):
    p, y = np.radians([pitch, yaw])
    forward = np.array([-math.sin(y)*math.cos(p), -math.sin(p), math.cos(y)*math.cos(p)])
    right = np.array([-math.cos(y), 0., -math.sin(y)])
    up = np.cross(right, forward)
    return forward, right, up


def fields(angle, mode, pitch, fov, size, yaw=0.):
    forward, right, up = camera(pitch, yaw)
    sy = 1 / math.tan(math.radians(fov)*.5)
    sx = sy * size[1] / size[0]
    return [0., 0., 0., angle, 0., 0., 0., float(mode), 0., 0., 0., 1., sx, sy, *forward, *right, *up]


def record(values, size, mirror=False, header=(83, 80, 82)):
    data = np.zeros((size[1], size[0], 4), dtype=np.uint8)
    row = size[1]-1-58 if mirror else 58
    for i, value in enumerate(values):
        b = struct.pack("<f", value)
        data[row, i*2, :3] = [b[0], b[1], 201]
        data[row, i*2+1, :3] = [b[2], b[3], 202]
    data[row, 56, :3] = header
    return data


def render(data, size):
    # Vary all RGB channels and alpha; inversion must preserve exact per-pixel alpha.
    yy, xx = np.indices((size[1], size[0]))
    scene = np.stack([(xx % 93)+11, (yy % 79)+29, ((xx+yy) % 111)+37, (xx*3+yy*5) % 256], axis=-1).astype("u1")
    scene_tex = ctx.texture(size, 4, scene.tobytes())
    mask = ctx.texture(size, 4, data.tobytes())
    scene_tex.use(0); mask.use(1)
    target = ctx.simple_framebuffer(size, components=4)
    target.use(); vao.render(vertices=3)
    image = np.frombuffer(target.read(components=4), dtype="u1").reshape(size[1], size[0], 4).copy()
    btarget = ctx.simple_framebuffer((1, 1), components=4, dtype="f4")
    btarget.use(); bounds_vao.render(vertices=3)
    bounds = np.frombuffer(btarget.read(components=4, dtype="f4"), dtype="f4").copy()
    scene_tex.release(); mask.release(); target.release(); btarget.release()
    return image, scene, bounds


def brute_euler(angle, pitch, size, fov, yaw=0.):
    # Independent dense CPU evaluation of the actual Cartesian product of native
    # yaw/pitch offsets. No shader extrema/stationary-point formulas are reused.
    forward, right, up = camera(pitch, yaw)
    q, t = np.meshgrid(np.linspace(pitch-angle, pitch+angle, 601), np.linspace(yaw-angle, yaw+angle, 601))
    q = np.radians(q); t = np.radians(t)
    direction = np.stack([-np.sin(t)*np.cos(q), -np.sin(q), np.cos(t)*np.cos(q)], axis=-1)
    den = direction @ forward
    x = (direction @ right) / den
    y = (direction @ up) / den
    sy = 1 / math.tan(math.radians(fov)*.5)
    scale = sy*size[1]*.5
    return np.array([-x.min(), x.max(), -y.min(), y.max()])*scale


def verify_pixels(image, scene, bounds, size, enabled=True):
    yy, xx = np.indices((size[1], size[0]), dtype="f4")
    dx, dy = xx+.5-size[0]*.5, yy+.5-size[1]*.5
    thickness = max(1., math.floor(min(size[0]/640., size[1]/360.))) if enabled else 0.
    length = thickness*4
    left, right, bottom, top = bounds
    horizontal = (dy >= -thickness/2) & (dy < thickness/2) & (((dx < 0) & (-dx <= left) & (-dx >= max(0, left-length))) | ((dx >= 0) & (dx <= right) & (dx >= max(0, right-length))))
    vertical = (dx >= -thickness/2) & (dx < thickness/2) & (((dy < 0) & (-dy <= bottom) & (-dy >= max(0, bottom-length))) | ((dy >= 0) & (dy <= top) & (dy >= max(0, top-length))))
    wanted = scene.copy()
    wanted[horizontal | vertical, :3] = 255-wanted[horizontal | vertical, :3]
    check(np.array_equal(image, wanted), "exact four-bar inversion and alpha " + str((size, bounds.tolist())))


for size in [(512, 256), (640, 480), (1280, 720)]:
    for fov in (45, 90, 110):
        for angle in (0, 5, 20):
            data = record(fields(angle, 1, 0, fov, size), size)
            image, scene, bounds = render(data, size)
            if angle:
                sy = 1/math.tan(math.radians(fov)*.5)
                expected = np.repeat(math.tan(math.radians(angle))*sy*size[1]*.5, 4)
                # Bounds beyond the viewport are safely clipped outside the image.
                half = np.array([size[0]/2]*2+[size[1]/2]*2)+8
                expected = np.minimum(expected, half)
                check(np.allclose(bounds, expected, atol=.002), f"cone angular projection {angle=} {fov=} {size=}")
            else:
                check(np.array_equal(image, scene), "zero spread leaves scene unchanged")
            verify_pixels(image, scene, bounds, size, angle > 0)
        for pitch in (0, 60, 89, -60, -89, 90, -90):
            for yaw in (0, 127):
                angle = 12
                data = record(fields(angle, 2, pitch, fov, size, yaw), size, mirror=(pitch == 89))
                image, scene, bounds = render(data, size)
                expected = brute_euler(angle, pitch, size, fov, yaw)
                check(np.allclose(bounds, expected, atol=.008), f"Euler independent CPU envelope {pitch=} {yaw=} {fov=} {size=}")
                verify_pixels(image, scene, bounds, size)
                results["geometry"].append({"pitch": pitch, "yaw": yaw, "fov": fov, "size": size, "gpu": bounds.tolist(), "cpu": expected.tolist()})

size = (512, 256)
good = fields(5, 1, 0, 90, size)
for name, data in [("absent", np.zeros((size[1], size[0], 4), dtype="u1")),
                   ("wrong header", record(good, size, header=(71,76,87)))]:
    image, scene, bounds = render(data, size)
    check(np.array_equal(image, scene), name + " leaves scene byte-identical")
for index, value in [(3, float("nan")), (3, float("inf")), (3, 1e30), (12, float("nan")), (12, 0), (14, 50), (7, 3)]:
    values = good.copy(); values[index] = value
    image, scene, bounds = render(record(values, size), size)
    check(np.array_equal(image, scene), f"invalid field {index}/{value} leaves scene byte-identical")
bad = record(good, size); bad[58, 9, 2] = 0
image, scene, bounds = render(bad, size)
check(np.array_equal(image, scene), "malformed encoded float sentinel rejected")
for header in [(90,66,83), (66,72,100), (66,72,101), (78,85,75), (78,85,76), (71,76,87)]:
    image, scene, bounds = render(record(good, size, header=header), size)
    check(np.array_equal(image, scene), f"legacy header {header} never claimed")

baseline = json.loads((RP / "assets/minecraft/post_effect/entity_outline.json").read_text())
patched = json.loads(source(Path("assets/minecraft/post_effect/entity_outline.json")))
restored = copy.deepcopy(patched)
del restored["targets"]["spread_scene"]
restored["passes"] = [p for p in restored["passes"] if p.get("output") != "spread_scene" and not any(i["target"] == "spread_scene" for i in p.get("inputs", []))]
check(restored == baseline, "existing entire post chain unchanged after removing only two spread passes/target")
names = [p["fragment_shader"] for p in patched["passes"]]
check(names.index("lbc:post/signed_glow") < names.index("lbc:post/weapon_spread") < names.index("minecraft:post/entity_sobel"), "spread reads raw data after glow and before Sobel")
include = source(Path("assets/lbc/shaders/include/weapon_spread_data.glsl"))
check("ROW=58" in include and "ivec3(213,118,0)" in include, "new marker/row distinct from all legacy channels")
for token in ("lbc_bh_slot(Color.rgb)", "lbc_nuclear_slot(Color.rgb)", "lbc_signed_glow_slot(Color.rgb)"):
    check(token in source(Path("assets/minecraft/shaders/core/rendertype_outline.vsh")), "legacy recognition retained " + token)

# Exercise the real core producer too, including its client-view matrix inversion,
# byte encoding, transparent raw row, and protection from subsequent normal glow.
baseline_core = ctx.program(vertex_shader=expand((RP / "assets/minecraft/shaders/core/rendertype_outline.vsh").read_text()),
                            fragment_shader=expand((RP / "assets/minecraft/shaders/core/rendertype_outline.fsh").read_text()))
forward, right, up = camera(60, 27)
view = np.eye(4, dtype="f4")
view[:3, :3] = np.stack([right, up, -forward])
projection = np.diag([1.2, 1.7, .1, 1]).astype("f4")
references = np.array([[2., 3., 4.], [7., 3., 4.], [2., 5., 4.], [2., 3., 5.]], dtype="f4")


def core_render(program, markers, normal_overwrite=False):
    blocks = []
    for name in program:
        member = program[name]
        if not isinstance(member, moderngl.UniformBlock):
            continue
        member.binding = len(blocks)
        data = bytearray(member.size)
        if name == "DynamicTransforms":
            data[:64] = view.T.tobytes()
            data[64:128] = np.eye(4, dtype="f4").tobytes()
            struct.pack_into("4f", data, 128, 1, 1, 1, 1)
        elif name == "Projection":
            data[:64] = projection.T.tobytes()
        elif name == "Globals":
            struct.pack_into("2f", data, 32, *size)
        buffer = ctx.buffer(data); buffer.bind_to_uniform_block(member.binding)
        blocks.append(buffer)
    tex = ctx.texture((1, 1), 4, bytes([255]*4)); tex.use(0)
    program["Sampler0"].value = 0
    vertices, indices = [], []
    for color, disk in markers:
        for tag in range(57):
            base = len(vertices)
            position = references[min(tag//6, 3)] if tag < 24 else references[0]
            for corner in range(4):
                vertices.append([*position, *[c/255 for c in color], 1, (tag+.5)/64, .75 if disk else .25])
            indices.extend([base, base+1, base+2, base+2, base+3, base])
    buf = ctx.buffer(np.array(vertices, dtype="f4").tobytes())
    idx = ctx.buffer(np.array(indices, dtype="u4").tobytes())
    vertices_vao = ctx.vertex_array(program, [(buf, "3f 4f 2f", "Position", "Color", "UV0")], idx)
    target = ctx.simple_framebuffer(size, components=4)
    target.use(); target.clear()
    ctx.disable(moderngl.BLEND | moderngl.DEPTH_TEST | moderngl.CULL_FACE)
    vertices_vao.render()
    pixels = np.frombuffer(target.read(components=4), dtype="u1").reshape(size[1],size[0],4).copy()
    if normal_overwrite:
        quad = []
        inverse = np.linalg.inv(projection @ view)
        for x,y in [(-1,-1),(1,-1),(1,1),(-1,1)]:
            point = inverse @ np.array([x,y,0,1])
            quad.append([*(point[:3]/point[3]), 1,1,1,1, 0,0])
        obuf = ctx.buffer(np.array(quad,dtype="f4").tobytes())
        oidx = ctx.buffer(np.array([0,1,2,2,3,0],dtype="u4").tobytes())
        ovao = ctx.vertex_array(program,[(obuf,"3f 4f 2f","Position","Color","UV0")],oidx)
        ovao.render()
        after = np.frombuffer(target.read(components=4),dtype="u1").reshape(size[1],size[0],4).copy()
        check(np.array_equal(after[58,:57],pixels[58,:57]), "actual core row58 protected against ordinary glowing overdraw")
        check(np.all(after[58,200] == 255), "ordinary glow outside reserved strip still renders")
        obuf.release();oidx.release();ovao.release()
    for b in blocks: b.release()
    tex.release();buf.release();idx.release();vertices_vao.release();target.release()
    return pixels


raw = core_render(core, [((213,118,0), False)], normal_overwrite=True)
check(tuple(raw[58,56,:3]) == (83,80,82), "actual core marker writes SPR header at row58")
check(np.all(raw[:,:,3] == 0), "actual raw marker has zero alpha: no Sobel silhouette")
decoded = []
for i in range(28):
    lo,hi = raw[58,i*2],raw[58,i*2+1]
    check(lo[2] == 201 and hi[2] == 202, f"actual core field{i} byte sentinels")
    decoded.append(struct.unpack("<f", bytes([lo[0],lo[1],hi[0],hi[1]]))[0])
expected = [*references.flatten(), 1.2,1.7,*forward,*right,*up,*([0]*5)]
check(np.allclose(decoded,expected,atol=1e-6), "actual core refs/projection/client forward-right-up agree with native contract")
legacy = [((253,29,66),False)]
legacy += [((177,74,i),False) for i in range(16)]
legacy += [((231,164,i),False) for i in range(16)]
legacy += [((231,165,i),False) for i in range(16)]
legacy += [((213,117,i),False) for i in range(8)]
old_pixels = core_render(baseline_core,legacy)
new_pixels = core_render(core,legacy)
check(np.array_equal(old_pixels,new_pixels), "all57 existing zone/hole/nuclear/fade/signed-glow raw records GPU byte-identical to author RP")
check(np.array_equal(core_render(baseline_core,[((177,74,0),True)]),core_render(core,[((177,74,0),True)])), "legacy black-hole disk kind1 GPU byte-identical")
(OUT / "results.json").write_text(json.dumps(results, indent=2)+"\n", encoding="utf-8")
print(json.dumps({"renderer": results["renderer"], "checks": len(results["checks"]), "geometry_cases": len(results["geometry"]), "success": True}))
