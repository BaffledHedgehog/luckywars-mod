"""Offscreen GPU proof for grenade smoke; the author's resource pack is read-only."""
from pathlib import Path
import hashlib, json, math, re, struct, zipfile
import moderngl
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
PACK = Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")
ADDON = ROOT / "resourcepack_template/updates/smoke_volume_2026_11_14"
OUT = ROOT / "build/verification/smoke-volume-2026-11-14"
OUT.mkdir(parents=True, exist_ok=True)
JAR = zipfile.ZipFile(next(p for p in (ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3").glob("*.jar") if not p.name.endswith("-sources.jar")))
BASE = json.loads((ADDON / "AUTHOR_BASE_SHA256.json").read_text())


def source(name, original=False):
    name = str(name).replace("\\", "/")
    if not original and (ADDON / name).is_file():
        return (ADDON / name).read_text(encoding="utf-8")
    if (PACK / name).is_file():
        data = (PACK / name).read_bytes()
        sha = hashlib.sha256(data).hexdigest()
        assert name not in BASE or BASE[name] == sha, "Author dependency changed: " + name
        BASE[name] = sha
        return data.decode()
    return JAR.read(name).decode()


def expand(text, original=False):
    return re.sub(r"#include\s+<([^>]+)>", lambda m: expand(source(
        "assets/" + m.group(1).split(":", 1)[0] + "/shaders/include/" + m.group(1).split(":", 1)[1], original), original), text).replace("gl_VertexIndex", "gl_VertexID")


ctx = moderngl.create_standalone_context(require=330)
result = dict(renderer=ctx.info["GL_RENDERER"], checks=[], numericComparisons=[])


def check(condition, label):
    assert condition, label
    result["checks"].append(label)


core_path = "assets/minecraft/shaders/core/rendertype_outline."
cores = {old: ctx.program(vertex_shader=expand(source(core_path + "vsh", old), old),
                         fragment_shader=expand(source(core_path + "fsh", old), old)) for old in (False, True)}
vsh = expand(source("assets/lbc/shaders/post/smoke_volume.vsh"))
fsh = expand(source("assets/lbc/shaders/post/smoke_volume.fsh"))
final_store = "fragColor = vec4(mix(fog, scene.rgb, transmission), scene.a);"
assert fsh.count(final_store) == 1
diagnostic = fsh.replace(final_store, "fragColor = vec4(opticalDepth, transmission, front, 1);")
programs = {}
for zero in (False, True):
    define = "\n#define RENDERPEARL_DEPTH_IS_ZERO_TO_ONE\n" if zero else "\n"
    for debug in (False, True):
        frag = (diagnostic if debug else fsh).replace("#version 330", "#version 330" + define, 1)
        programs[zero, debug] = ctx.program(vertex_shader=vsh, fragment_shader=frag)
check(True, "original/new core and GL/zero-to-one smoke vertex/fragment shaders compile and link")


def projection(fov, size, zero=False, reverse=False, infinite=False):
    near, far = .05, 512.
    sy = 1 / math.tan(math.radians(fov) / 2)
    p = np.zeros((4, 4), dtype="f4")
    p[0, 0], p[1, 1], p[3, 2] = sy * size[1] / size[0], sy, -1
    if zero:
        p[2, 2] = 0 if reverse and infinite else near / (far - near) if reverse else -1 if infinite else -far / (far - near)
        p[2, 3] = near if reverse and infinite else far * near / (far - near) if reverse else -near if infinite else -far * near / (far - near)
    else:
        p[2, 2] = (1 if infinite else (far + near) / (far - near)) * (1 if reverse else -1)
        p[2, 3] = (2 * near if infinite else 2 * far * near / (far - near)) * (1 if reverse else -1)
    return p


def view_matrix(yaw=0., pitch=0., camera=(0., 0., 0.)):
    y, p = np.radians([yaw, pitch])
    ry = np.array([[math.cos(y), 0, math.sin(y)], [0, 1, 0], [-math.sin(y), 0, math.cos(y)]])
    rx = np.array([[1, 0, 0], [0, math.cos(p), -math.sin(p)], [0, math.sin(p), math.cos(p)]])
    v = np.eye(4, dtype="f4"); v[:3, :3] = rx @ ry; v[:3, 3] = -v[:3, :3] @ camera
    return v


def records(spheres, p, v, size, mirror=False, corrupt=None):
    data = np.zeros((size[1], size[0], 4), dtype="u1")
    for slot, sphere in enumerate(spheres):
        center, radius = sphere[:2]; density = sphere[2] if len(sphere)>2 else 1.
        row = 59 + slot if not mirror else size[1] - 1 - 59 - slot
        references = np.array([center, np.array(center) + [radius, 0, 0], np.array(center) + [0, radius*density, 0], np.array(center) + [0, 0, radius]], dtype="f4")
        values = [*references.flatten(), *np.linalg.inv(p @ v).T.flatten()]
        for i, value in enumerate(values):
            b = struct.pack("<f", value)
            data[row, i * 2, :3] = [b[0], b[1], 201]
            data[row, i * 2 + 1, :3] = [b[2], b[3], 202]
        data[row, 56, :3] = [83, 77, 75]
        if corrupt == "header": data[row, 56, :3] = [66, 72, 100]
        if corrupt == "partial": data[row, 7, 2] = 0
        if corrupt == "nan": data[row, 0, :3], data[row, 1, :3] = [0, 0, 201], [192, 127, 202]
        if corrupt == "matrix": data[row, 24:56, :2] = 0
    return data


def blocks(program, size, p=None, v=None, time=0., world=(0, 0, 0)):
    buffers = []
    for name in program:
        block = program[name]
        if not isinstance(block, moderngl.UniformBlock): continue
        block.binding = len(buffers); data = bytearray(block.size)
        if name == "Globals":
            struct.pack_into("3i", data, 0, *world); struct.pack_into("f", data, 28, time / 1200.)
            struct.pack_into("2f", data, 32, *size)
        if name == "Projection": data[:64] = p.T.astype("f4").tobytes()
        if name == "DynamicTransforms":
            data[:64] = v.T.astype("f4").tobytes(); data[64:128] = np.eye(4, dtype="f4").tobytes()
            struct.pack_into("4f", data, 128, 1, 1, 1, 1)
        buffer = ctx.buffer(data); buffer.bind_to_uniform_block(block.binding); buffers.append(buffer)
    return buffers


def render(data, p, v, distance, zero=False, reverse=False, debug=False, time=0., world=(0, 0, 0), dtype="f4", scene=None):
    size = data.shape[1], data.shape[0]
    yy, xx = np.indices((size[1], size[0]))
    if scene is None:
        scene = np.stack([((xx // 8 + yy // 8) % 2) * .8 + .1, (xx % 17) / 18., (yy % 13) / 14., ((xx + yy) % 19) / 20.], axis=-1).astype("f4")
    z = p @ np.array([0, 0, -distance, 1])
    depth = z[2] / z[3]; depth = depth if zero else depth * .5 + .5
    if dtype == "f2": depth = round(depth * (2**24 - 1)) / (2**24 - 1)
    scene_tex = ctx.texture(size, 4, scene.astype("f4").tobytes(), dtype="f4")
    data_tex = ctx.texture(size, 4, data.tobytes())
    depth_tex = ctx.texture(size, 1, np.full((size[1], size[0]), depth, dtype="f4").tobytes(), dtype="f4")
    for tex in (scene_tex, data_tex, depth_tex): tex.filter = moderngl.NEAREST, moderngl.NEAREST
    scene_tex.use(0); data_tex.use(1); depth_tex.use(2)
    program = programs[zero, debug]
    program["SceneSampler"].value = 0; program["SmokeDataSampler"].value = 1; program["SceneDepthSampler"].value = 2
    buffers = blocks(program, size, time=time, world=world)
    target = ctx.simple_framebuffer(size, components=4, dtype=dtype); target.use()
    vao = ctx.vertex_array(program, []); ctx.disable(moderngl.BLEND | moderngl.DEPTH_TEST | moderngl.CULL_FACE)
    vao.render(vertices=3)
    read_type = {"f1": "u1", "f2": "f2", "f4": "f4"}[dtype]
    image = np.frombuffer(target.read(components=4, dtype=dtype), dtype=read_type).reshape(size[1], size[0], 4).copy()
    for x in [vao, target, scene_tex, data_tex, depth_tex, *buffers]: x.release()
    return image


size = (320, 240)
for zero in (False, True):
    for reverse in (False, True):
        for infinite in (False, True):
            for fov, yaw, pitch, camera in [(70, 0, 0, (0, 0, 0)), (110, 123, -78, (7.25, -2, 13.5)), (45, -170, 89, (-4.5, 3, -1.25))]:
                p = projection(fov, size, zero, reverse, infinite); v = view_matrix(yaw, pitch, camera)
                center = np.array(camera) + v[:3, :3].T @ np.array([0, 0, -12.])
                spheres = [(center, 5.)]; data = records(spheres, p, v, size, mirror=reverse)
                empty = np.zeros_like(data)
                baseline = render(empty, p, v, 3, zero, reverse)
                foreground = render(data, p, v, 3, zero, reverse)
                check(np.array_equal(foreground, baseline), f"foreground exact, {zero=} {reverse=} {infinite=} {fov=} {pitch=}")
                image = render(data, p, v, 30, zero, reverse)
                check(np.array_equal(image[..., 3], baseline[..., 3]), "scene alpha/other effect transport preserved")
                debug = render(data, p, v, 30, zero, reverse, debug=True)
                # Independent dense numerical quadrature along known camera-space rays.
                for x, y in [(160, 120), (170, 110), (150, 130)]:
                    ray = np.array([((x + .5) / size[0] * 2 - 1) / p[0, 0], ((y + .5) / size[1] * 2 - 1) / p[1, 1], -1.])
                    ray /= np.linalg.norm(ray)
                    samples = np.linspace(0, 30 / -ray[2], 4097)
                    r2 = np.sum((samples[:, None] * ray - np.array([0, 0, -12.]))**2, axis=1) / 25
                    density = np.maximum(0, 1 - r2)**2
                    numeric = 3 * np.trapezoid(density, samples)
                    observed = float(debug[y, x, 0]); error = abs(observed - numeric)
                    check(error < .003, "GPU density integral matches independent4097sample quadrature")
                    result["numericComparisons"].append(error)
                check(float(debug[120, 160, 1]) < .0001, "outside observer cannot see through central cloud")

p = projection(70, size); v = view_matrix(); data = records([(np.array([0, 0, -12]), 5.)], p, v, size)
baseline = render(np.zeros_like(data), p, v, 30)
for corrupt in ("header", "partial", "nan", "matrix"):
    check(np.array_equal(render(records([(np.array([0, 0, -12]), 5.)], p, v, size, corrupt=corrupt), p, v, 30), baseline), "invalid record passthrough: " + corrupt)
for center in ([0, 0, 12], [100, 0, -12]):
    check(np.array_equal(render(records([(np.array(center), 5.)], p, v, size), p, v, 30), baseline), "behind camera/off-ray volume leaves scene exact")
inside = records([(np.zeros(3), 5.)], p, v, size)
inside_debug = render(inside, p, v, 30, debug=True)
check(np.max(inside_debug[20:-20, 20:-20, 1]) < .002, "inside cloud suppresses distant scene in every direction in view")
first = render(data, p, v, 30)
for dtype in ("f1", "f2", "f4"):
    fg = render(data, p, v, 3, dtype=dtype)
    clean = render(np.zeros_like(data), p, v, 3, dtype=dtype)
    check(np.array_equal(fg, clean), "foreground exact with native color/depth quantization " + dtype)
red = np.full((size[1], size[0], 4), [1., 0., 0., .5], dtype="f4")
blue = np.full_like(red, [0., 0., 1., .5])
hidden_a = render(data, p, v, 30, scene=red); hidden_b = render(data, p, v, 80, scene=blue)
check(np.max(np.abs(hidden_a[110:130, 150:170] - hidden_b[110:130, 150:170])) < .0001, "hidden color/depth do not leak silhouettes through opaque core")
check(np.max(np.abs(render(data, p, v, 30, time=2.) - first)) > .001, "billows animate continuously in game time")
check(np.array_equal(render(data, p, v, 30, world=(512 * 10000, -512 * 10000, 512 * 10000)), first), "integer camera wrapping remains exact far from origin")
full = records([(np.array([i * .03, 0, -12.]), 5.) for i in range(16)], p, v, size)
check(np.isfinite(render(full, p, v, 30)).all(), "all16 simultaneous cloud slots remain finite")


helper_model=json.loads(source("assets/lbc/models/item/black_hole_small.json"))
helper_item=json.loads(source("assets/lbc/items/black_hole_small.json"))
check(len(helper_model["elements"])==57 and helper_item["model"]["tints"][0]["value"]==0xFCFEFF,"actual authored invisible helper mesh/model contract")


def core_render(program, markers, density=1.):
    vertices, indices = [], []
    for marker in markers:
        references = np.array([[0, 0, -12], [5, 0, -12], [0, 5*density, -12], [0, 0, -7]], dtype="f4")
        for tag, element in enumerate(helper_model["elements"]):
            base = len(vertices)
            lo,hi=np.array(element["from"]),np.array(element["to"])
            uvs=np.array(element["faces"]["up"]["uv"][:2])/16.
            for q in [(lo[0],lo[1],hi[2]),(hi[0],lo[1],hi[2]),(hi[0],lo[1],lo[2]),(lo[0],lo[1],lo[2])]:
                point=(np.array(q)/16.-.5)*np.array([5,5*density,5])+np.array([0,0,-12])
                vertices.append([*point, *[c / 255 for c in marker], 1, *uvs])
            indices.extend([base, base + 1, base + 2, base + 2, base + 3, base])
    buffers = blocks(program, size, p, v)
    tex = ctx.texture((1, 1), 4, bytes([255]*4)); tex.use(0); program["Sampler0"].value = 0
    vb = ctx.buffer(np.array(vertices, dtype="f4").tobytes()); ib = ctx.buffer(np.array(indices, dtype="u4").tobytes())
    vao = ctx.vertex_array(program, [(vb, "3f 4f 2f", "Position", "Color", "UV0")], ib)
    target = ctx.simple_framebuffer(size, components=4); target.use(); target.clear()
    vao.render()
    pixels = np.frombuffer(target.read(components=4), dtype="u1").reshape(size[1], size[0], 4).copy()
    for x in [vao, target, vb, ib, tex, *buffers]: x.release()
    return pixels


old_markers = [(253, 29, 66), (177, 74, 0), (231, 165, 0), (231, 166, 0), (213, 117, 0), (213, 118, 0)]
check(np.array_equal(core_render(cores[False], old_markers), core_render(cores[True], old_markers)), "all existing zone/hole/nuclear/signed-glow/spread carriers unchanged byte-for-byte")
transport = core_render(cores[False], [(213, 119, 0)])
check(np.array_equal(transport[59, 56, :3], [83, 77, 75]) and not np.any(transport[..., 3]), "actual core writes new alpha-zero SMK row without outline/bloom")
image = render(transport, p, v, 30)
check(np.max(np.abs(image - first)) < .0003, "actual core reference-point transport drives smoke post")
# Independent density channel makes departed smoke disperse instead of moving rigidly.
last_transmission=0.
for density in [1.,.75,.5,.25,.05,0.]:
    faded=records([(np.array([0,0,-12]),5.,density)],p,v,size)
    image=render(faded,p,v,30)
    if density:
        diag=render(faded,p,v,30,debug=True)
        check(diag[120,160,1]>=last_transmission,"dissipation transmission increases monotonically")
        last_transmission=float(diag[120,160,1])
        check(abs(float(diag[120,160,0])-density*float(render(data,p,v,30,debug=True)[120,160,0]))<.001,"density scales optical depth independently of radius")
    else:check(np.array_equal(image,baseline),"fully dissipated volume leaves exact original scene")
trail=records([(np.array([0,0,-12]),5.,.7),(np.array([8,0,-12]),3.25,.7)],p,v,size)
body=records([(np.array([8,0,-12]),3.25,.7)],p,v,size)
check(np.max(np.abs(render(trail,p,v,30)[120,160,:3]-baseline[120,160,:3]))>.1,"old plume remains behind after source moves eight blocks")
check(np.array_equal(render(body,p,v,30)[120,160],baseline[120,160]),"departed plume vanishes after dissipation while displaced source remains")
quarter=core_render(cores[False],[(213,119,0)],density=.25)
expected=records([(np.array([0,0,-12]),5.,.25)],p,v,size)
check(np.max(np.abs(render(quarter,p,v,30)-render(expected,p,v,30)))<.0003,"actual core Y reference length carries trail density")
for name, image in [("outside", first), ("inside", render(inside, p, v, 30)), ("foreground", render(data, p, v, 3))]:
    from PIL import Image
    Image.fromarray(np.uint8(np.clip(image[..., :3], 0, 1) * 255)[::-1]).save(OUT / (name + ".png"))
for name, sha in BASE.items(): check(hashlib.sha256((PACK / name).read_bytes()).hexdigest() == sha, "author dependency unchanged: " + name)
(ADDON / "AUTHOR_BASE_SHA256.json").write_text(json.dumps(BASE, indent=2) + "\n")
result["shaderAssetsSha256"]={str(p.relative_to(ADDON)).replace("\\","/"):hashlib.sha256(p.read_bytes()).hexdigest() for p in (ADDON/"assets").rglob("*") if p.is_file()}
result.update(success=True, maximumQuadratureError=max(result["numericComparisons"]), authorAssetsUntouched=True,
              scope="Actual standalone GPU shaders/transport/depth occlusion and independent numeric integration; not a physical Minecraft client session")
(ADDON / "GPU_RESULTS.json").write_text(json.dumps(result, indent=2) + "\n")
(OUT / "results.json").write_text(json.dumps(result, indent=2) + "\n")
print(json.dumps(dict(success=True, renderer=result["renderer"], checks=len(result["checks"]), maximumQuadratureError=result["maximumQuadratureError"])))
