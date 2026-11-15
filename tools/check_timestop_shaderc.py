"""Compile active TimeStop/end-of-frame shaders with Minecraft 26.3's shaderc.

Reads the author pack and prepared patch; only writes ignored verification output.
Uses the game's native compiler directly, without starting Java, Gradle or Minecraft.
"""
import argparse
import ctypes
import hashlib
import json
import os
from pathlib import Path
import re
import zipfile


def limit_resources():
    """Keep this quick native check at idle CPU/low IO priority on two logical cores."""
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    native = ctypes.WinDLL("ntdll")
    handle, number = ctypes.c_void_p, ctypes.c_ulong
    kernel.GetCurrentProcess.restype = handle
    kernel.SetPriorityClass.argtypes = [handle, number]
    kernel.SetProcessAffinityMask.argtypes = [handle, ctypes.c_size_t]
    kernel.SetProcessInformation.argtypes = [handle, ctypes.c_int, handle, number]
    native.NtSetInformationProcess.argtypes = [handle, ctypes.c_int, handle, number]
    native.NtSetInformationProcess.restype = ctypes.c_long
    process = kernel.GetCurrentProcess()
    cores = os.cpu_count() or 1
    affinity = (1 << (cores - 1)) | (1 << max(0, cores - 3))
    memory, io = number(4), number(1)
    if not (kernel.SetPriorityClass(process, 0x40) and kernel.SetProcessAffinityMask(process, affinity)
            and kernel.SetProcessInformation(process, 0, ctypes.byref(memory), ctypes.sizeof(memory))
            and native.NtSetInformationProcess(process, 33, ctypes.byref(io), ctypes.sizeof(io)) == 0):
        raise OSError("Could not apply CPU/IO/memory limits")


def main():
    limit_resources()
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pack", type=Path, required=True)
    args = parser.parse_args()
    pack = args.pack.resolve()
    addon = root / "resourcepack_template/updates/timestop_glsl_2026_11_15"
    out = root / "build/verification/timestop-shaderc-2026-11-15"
    out.mkdir(parents=True, exist_ok=True)
    source_jar = next((root / ".gradle/loom-cache/minecraftMaven").rglob("*26.3-sources.jar"))
    jar = source_jar.with_name(source_jar.name.replace("-sources.jar", ".jar"))
    if not jar.is_file():
        raise FileNotFoundError(jar)
    relative = "assets/lbc/shaders/post/time_gate.fsh"
    patched = (addon / relative).read_bytes()
    original = re.sub(rb"\bfreezeActive\b", b"active", patched)
    current = (pack / relative).read_bytes()
    patch_manifest = json.loads((addon / "export-manifest.json").read_text(encoding="utf-8"))
    if hashlib.sha256(original).hexdigest() != patch_manifest["assets"][0]["author_base_sha256"]:
        raise AssertionError("Original shader does not match the recorded failure")
    if current not in (original, patched):
        raise AssertionError("Author shader changed: patch must preserve everything except the identifier")
    files = {}
    archive = zipfile.ZipFile(jar)

    def read(name):
        if name in files:
            return files[name]
        for base in (addon, pack):
            path = base / name
            if path.is_file():
                data = path.read_bytes()
                break
        else:
            data = archive.read(name)
        files[name] = data
        return data

    def expand(text, stack=()):
        # Includes have the same namespace/path lookup as ShaderSource.getInclude().
        def include(match):
            identifier = match.group(1)
            namespace, path = identifier.split(":", 1) if ":" in identifier else ("minecraft", identifier)
            name = f"assets/{namespace}/shaders/include/{path}"
            if name in stack:
                raise AssertionError(f"Recursive shader include: {name}")
            return expand(read(name).decode("utf-8"), (*stack, name))
        return re.sub(r'^\s*#(?:moj_import|include)\s*[<\"]([^>\"]+)[>\"]\s*$', include, text, flags=re.M)

    stages = {}
    for chain in ("assets/minecraft/post_effect/end_of_frame.json", "assets/lbc/post_effect/time_stop_freeze.json"):
        for entry in json.loads(read(chain))["passes"]:
            for key, extension, kind in (("vertex_shader", "vsh", 0), ("fragment_shader", "fsh", 1)):
                identifier = entry[key]
                namespace, path = identifier.split(":", 1)
                name = f"assets/{namespace}/shaders/{path}.{extension}"
                stages[name] = kind
    rows = []
    negative = out / "original-time_gate.fsh"
    negative.write_bytes(original)
    rows.append(("lbc:post/time_gate-original", 1, original, True))
    for i, (name, kind) in enumerate(stages.items()):
        generated = out / f"expanded-{i}.{'fsh' if kind else 'vsh'}"
        generated.write_text(expand(read(name).decode("utf-8")), encoding="utf-8", newline="\n")
        rows.append((name, kind, generated.read_bytes(), False))
    cache = Path.home() / ".gradle/caches/modules-2/files-2.1/org.lwjgl"
    native_jar = next((cache / "lwjgl-shaderc/3.4.3").rglob("lwjgl-shaderc-3.4.3-natives-windows.jar"))
    dll = out / "shaderc.dll"
    with zipfile.ZipFile(native_jar) as native:
        dll.write_bytes(native.read("windows/x64/org/lwjgl/shaderc/shaderc.dll"))
    lib = ctypes.CDLL(str(dll))
    ptr, integer, boolean, size, string = ctypes.c_void_p, ctypes.c_int, ctypes.c_bool, ctypes.c_size_t, ctypes.c_char_p

    def bind(name, result, *arguments):
        method = getattr(lib, "shaderc_" + name)
        method.restype, method.argtypes = result, arguments
        return method

    initialize = bind("compiler_initialize", ptr)
    release = bind("compiler_release", None, ptr)
    options_initialize = bind("compile_options_initialize", ptr)
    options_release = bind("compile_options_release", None, ptr)
    environment = bind("compile_options_set_target_env", None, ptr, integer, integer)
    auto_bind = bind("compile_options_set_auto_bind_uniforms", None, ptr, boolean)
    preserve = bind("compile_options_set_preserve_bindings", None, ptr, boolean)
    debug = bind("compile_options_set_generate_debug_info", None, ptr)
    optimization = bind("compile_options_set_optimization_level", None, ptr, integer)
    macro = bind("compile_options_add_macro_definition", None, ptr, string, size, string, size)
    compile_shader = bind("compile_into_spv", ptr, ptr, string, size, integer, string, string, ptr)
    status = bind("result_get_compilation_status", integer, ptr)
    error_message = bind("result_get_error_message", string, ptr)
    length = bind("result_get_length", size, ptr)
    result_release = bind("result_release", None, ptr)
    compiler = initialize()
    if not compiler:
        raise AssertionError("shaderc initialization failed")
    checks, log = [], []
    try:
        for name, kind, source, negative_control in rows:
            for variant in range(2):
                options = options_initialize()
                result = None
                try:
                    # Exact GlslCompiler.createBaseShaderOptions() in the version used by the game.
                    environment(options, 0, 4202496)
                    auto_bind(options, True)
                    preserve(options, False)
                    debug(options)
                    optimization(options, 0)
                    if variant:
                        for define in (b"RENDERPEARL_DEPTH_IS_ZERO_TO_ONE", b"RENDERPEARL_EXPLICIT_DEPTH_INVARIANCE",
                                       b"RENDERPEARL_INSTANCE_INDEX_INCLUDES_BASE_INSTANCE"):
                            macro(options, define, len(define), b"", 0)
                    result = compile_shader(compiler, source, len(source), kind, name.encode(), b"main", options)
                    if not result:
                        raise AssertionError("shaderc returned no result")
                    error = error_message(result).decode("utf-8")
                    passed = (status(result) != 0 and "'active'" in error and "Reserved word" in error) if negative_control \
                        else status(result) == 0 and length(result) > 0
                    checks.append({"name": name, "variant": variant, "negative_control": negative_control,
                                   "passed": passed, "error": error})
                    log.append(f"{'PASS' if passed else 'FAIL'} {name} variant={variant}")
                    if error:
                        log.append(error.strip())
                finally:
                    if result:
                        result_release(result)
                    options_release(options)
    finally:
        release(compiler)
        archive.close()
    (out / "shaderc.log").write_text("\n".join(log) + "\n", encoding="utf-8")
    sha = lambda data: hashlib.sha256(data).hexdigest()
    with source_jar.open("rb") as stream:
        source_sha = hashlib.file_digest(stream, "sha256").hexdigest()
    report = {"success": all(c["passed"] for c in checks), "compiler": "LWJGL shaderc 3.4.3 native DLL, Minecraft 26.3 GlslCompiler base options",
              "source_jar_sha256": source_sha, "patched_shader_sha256": sha(patched),
              "author_base_sha256": sha(original), "author_current_sha256": sha(current),
              "positive_stages": len(stages), "variants_per_stage": 2,
              "negative_control": "original active identifier must fail with Reserved word in both variants",
              "physical_client_reload_observed": False, "checks": checks, "inputs": {k: sha(v) for k, v in files.items()}}
    (out / "results.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print("\n".join(log))
    print(json.dumps({k: v for k, v in report.items() if k not in ("inputs", "checks")}, indent=2))
    if not report["success"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
