"""Report JVM reflection calls authored in the mod classes of a built JAR."""

import argparse
import io
import json
import struct
import zipfile

CLASS_METHODS = {
    "forName", "newInstance", "getClasses", "getDeclaredClasses", "getConstructor",
    "getConstructors", "getDeclaredConstructor", "getDeclaredConstructors", "getField",
    "getFields", "getDeclaredField", "getDeclaredFields", "getMethod", "getMethods",
    "getDeclaredMethod", "getDeclaredMethods", "getEnclosingConstructor", "getEnclosingMethod",
    "getGenericSuperclass", "getGenericInterfaces", "isRecord", "getRecordComponents",
    "getNestHost", "getNestMembers", "getPermittedSubclasses",
    "getAnnotation", "getAnnotations", "getDeclaredAnnotation", "getDeclaredAnnotations",
    "getAnnotationsByType", "getDeclaredAnnotationsByType",
}


def read_u2(stream):
    return struct.unpack(">H", stream.read(2))[0]


def constants(data):
    stream = io.BytesIO(data)
    if stream.read(4) != b"\xca\xfe\xba\xbe":
        raise ValueError("Invalid class magic")
    stream.read(4)
    pool = [None] * read_u2(stream)
    index = 1
    while index < len(pool):
        tag = stream.read(1)[0]
        if tag == 1:
            pool[index] = stream.read(read_u2(stream)).decode("utf-8", errors="replace")
        elif tag in (3, 4):
            stream.read(4)
        elif tag in (5, 6):
            stream.read(8)
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = read_u2(stream)
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[index] = (tag, read_u2(stream), read_u2(stream))
        elif tag == 15:
            stream.read(3)
        else:
            raise ValueError(f"Unsupported constant tag {tag}")
        index += 1
    return pool


def reflection_calls(data):
    pool = constants(data)
    for entry in pool:
        if not isinstance(entry, tuple) or entry[0] not in (10, 11):
            continue
        _, owner_index, name_and_type = entry
        owner = pool[pool[owner_index]]
        _, name_index, descriptor_index = pool[name_and_type]
        name = pool[name_index]
        if (owner.startswith(("java/lang/reflect/", "jdk/internal/reflect/"))
                or owner == "java/lang/Class" and name in CLASS_METHODS
                or owner in ("java/lang/invoke/MethodHandles", "java/lang/invoke/MethodHandles$Lookup",
                             "java/lang/invoke/MethodHandle", "java/lang/invoke/VarHandle")):
            yield f"{owner.replace('/', '.')}.{name}{pool[descriptor_index]}"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar")
    parser.add_argument("--fail-on-use", action="store_true")
    args = parser.parse_args()
    findings = {}
    inspected = 0
    with zipfile.ZipFile(args.jar) as jar:
        for path in jar.namelist():
            if not path.startswith("me/skywars2/") or not path.endswith(".class"):
                continue
            inspected += 1
            calls = sorted(set(reflection_calls(jar.read(path))))
            if calls:
                findings[path.removesuffix(".class").replace("/", ".")] = calls
    print(json.dumps({"jar": args.jar, "inspected": inspected, "classesWithUse": len(findings),
                      "calls": dict(sorted(findings.items()))}, indent=2))
    return int(args.fail_on_use and bool(findings))


if __name__ == "__main__":
    raise SystemExit(main())
