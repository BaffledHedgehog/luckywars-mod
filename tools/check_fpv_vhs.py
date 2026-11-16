"""Render the actual FPV VHS fragment shader; preserve alpha and transport bytes."""
from pathlib import Path
import hashlib
import json
import os
import struct
import ctypes

import moderngl as gl
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ADDON = ROOT / "resourcepack_template/updates/fpv_vhs_2026_11_16"
OUT = ROOT / "build/verification/fpv-vhs-2026-11-16"
PACK = Path("C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8")
RELATIVE = "assets/lbc/shaders/post/fpv_drone.fsh"


def main():
    if os.name == "nt":
        kernel = ctypes.WinDLL("kernel32", use_last_error=True)
        kernel.GetCurrentProcess.restype = ctypes.c_void_p
        kernel.SetPriorityClass.argtypes = [ctypes.c_void_p, ctypes.c_uint32]
        if not kernel.SetPriorityClass(kernel.GetCurrentProcess(), 0x40):
            raise ctypes.WinError(ctypes.get_last_error())
    OUT.mkdir(parents=True, exist_ok=True)
    before = hashlib.sha256((PACK / RELATIVE).read_bytes()).hexdigest()
    fragment = (ADDON / RELATIVE).read_text(encoding="utf-8")
    # The fragment needs only GameTime. Native shaderc separately checks the real Globals UBO.
    fragment = fragment.replace("#include <minecraft:globals.glsl>", "uniform float GameTime;")
    vertex = """#version 330
    void main(){vec2 p=vec2((gl_VertexID<<1)&2, gl_VertexID&2);gl_Position=vec4(p*2.0-1.0,0,1);}
    """
    context = gl.create_standalone_context(require=330)
    program = context.program(vertex_shader=vertex, fragment_shader=fragment)
    program["InSampler"].value = 0
    vao = context.vertex_array(program, [])
    context.disable(gl.BLEND | gl.DEPTH_TEST | gl.CULL_FACE)
    cases = []
    for width, height in ((640, 360), (853, 481), (1280, 720)):
        yy, xx = np.indices((height, width))
        source = np.empty((height, width, 4), dtype=np.uint8)
        source[..., 0] = 35 + xx * 120 // width
        source[..., 1] = 45 + yy * 130 // height
        source[..., 2] = 80 + xx * 70 // width
        source[..., 3] = (xx * 13 + yy * 7) % 256
        incoming = context.texture((width, height), 4, source.tobytes())
        incoming.filter = (gl.NEAREST, gl.NEAREST)
        outgoing = context.texture((width, height), 4)
        target = context.framebuffer(color_attachments=[outgoing])
        target.use(); incoming.use(0)
        previous = None
        protected = (xx < 171) & (np.minimum(yy, height - 1 - yy) < 50)
        center = (xx >= width * .25) & (xx < width * .75) & ~protected
        for seconds in (0, .05, 1, 2, 12.5, 49):
            program["GameTime"].value = seconds / 1200
            vao.render(vertices=3)
            result = np.frombuffer(target.read(components=4), dtype=np.uint8).reshape(source.shape).copy()
            assert np.array_equal(source[..., 3], result[..., 3]), "all alpha bytes must survive"
            assert np.array_equal(source[protected], result[protected]), "transport RGB/alpha must survive"
            delta = np.abs(result[..., :3].astype(np.int16) - source[..., :3].astype(np.int16))
            mean = float(delta[center].mean())
            assert .2 < mean < 7, ("subtle visible VHS, clear central camera image", mean)
            if previous is not None:
                assert np.any(result[center, :3] != previous[center, :3]), "noise must animate"
            previous = result
            cases.append({"width": width, "height": height, "seconds": seconds,
                          "central_mean_difference_bytes": mean, "passed": True})
            if width == 1280 and seconds == 12.5:
                Image.fromarray(result[::-1, :, :3]).save(OUT / "preview.png")
        for value in (target, outgoing, incoming):
            value.release()
    assert hashlib.sha256((PACK / RELATIVE).read_bytes()).hexdigest() == before, "author pack is read-only"
    report = {"passed": True, "renderer": context.info["GL_RENDERER"], "cases": cases,
              "author_source_sha256": before, "physical_client_render": False,
              "adapter": "Only Globals.GameTime storage replaced by a standalone uniform"}
    (OUT / "GPU_RESULTS.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"passed": True, "cases": len(cases), "output": str(OUT)}))


if __name__ == "__main__":
    main()
