#!/usr/bin/env python3
"""Check Java source layout and class names used by the server's Fabric metadata."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
import json
from pathlib import Path, PurePosixPath
import re
from typing import NamedTuple


SOURCE_ROOTS = (
    ("src/main/java", False),
    ("src/checks/java", True),
    ("media-protocol/src/main/java", False),
)
TYPE_KINDS = {"class", "interface", "enum", "record"}
OWN_PACKAGE = "me.skywars2."
TEST_TYPE = re.compile(r"MapAuthoring.*|MapDeletion.*|.*Checks|Vanity.*Scene|NativeCheckBackend|TikTokCheckFields")
JAVA_NAME = r"(?:[^\W\d]|\$)(?:\w|\$)*"
TOKEN = re.compile(
    r'(?P<ignored>\s+|//[^\r\n]*|/\*.*?\*/)'
    r'|(?P<text_block>"""(?:\\.|(?!""")[^\\])*""")'
    r'|(?P<string>"(?:\\.|[^"\\\r\n])*"|\'(?:\\.|[^\'\\\r\n])*\')'
    rf'|(?P<name>{JAVA_NAME})|(?P<symbol>.)',
    re.DOTALL,
)


class Token(NamedTuple):
    text: str
    kind: str
    offset: int

    @property
    def literal(self) -> str | None:
        return self.text[1:-1] if self.kind == "string" else None


@dataclass(frozen=True)
class Source:
    path: Path
    checks: bool


def tokens(text: str) -> list[Token]:
    return [Token(match[0], match.lastgroup, match.start()) for match in TOKEN.finditer(text)
            if match.lastgroup != "ignored"]


def words(parts: list[Token]) -> tuple[str, ...]:
    return tuple(part.text for part in parts)


def group_end(parts: list[Token], start: int, left: str, right: str) -> int:
    depth = 0
    for i in range(start, len(parts)):
        depth += (parts[i].text == left) - (parts[i].text == right)
        if depth == 0:
            return i
    raise ValueError(f"Unclosed {left} group")


def arguments(parts: list[Token]) -> list[list[Token]]:
    result = [[]]
    depth = 0
    for part in parts:
        if part.text == "," and depth == 0:
            result.append([])
        else:
            result[-1].append(part)
        depth += (part.text in {"(", "[", "{"}) - (part.text in {")", "]", "}"})
    return result


def declared_types(parts: list[Token]) -> tuple[str, list[str]]:
    package = ""
    types = []
    enclosing = []
    pending = None
    braces = parens = 0
    for i, part in enumerate(parts):
        word = part.text
        if word == "package" and braces == 0 and parens == 0:
            end = next(j for j in range(i + 1, len(parts)) if parts[j].text == ";")
            package = "".join(words(parts[i + 1:end]))
        if (word in TYPE_KINDS and i + 1 < len(parts) and parts[i + 1].kind == "name"
                and (i == 0 or parts[i - 1].text != ".")
                and parens == 0 and braces == (enclosing[-1][0] if enclosing else 0)):
            pending = parts[i + 1].text
        if word == "{":
            braces += 1
            if pending is not None and parens == 0:
                enclosing.append((braces, pending))
                types.append("$".join(name for _, name in enclosing))
                pending = None
        elif word == "}":
            if enclosing and enclosing[-1][0] == braces:
                enclosing.pop()
            braces -= 1
        parens += (word == "(") - (word == ")")
    return package, types


def mixin_targets(parts: list[Token]):
    for i, part in enumerate(parts):
        if part.text != "@":
            continue
        j = i + 1
        while j + 1 < len(parts) and parts[j + 1].text == ".":
            j += 2
        annotation = "".join(words(parts[i + 1:j + 1]))
        if annotation not in {"Mixin", "org.spongepowered.asm.mixin.Mixin"}:
            continue
        if j + 1 < len(parts) and parts[j + 1].text == "(":
            end = group_end(parts, j + 1, "(", ")")
            for arg in arguments(parts[j + 2:end]):
                if words(arg[:2]) == ("targets", "="):
                    yield from (token for token in arg[2:] if token.literal is not None)


def plugin_classes(parts: list[Token]):
    # These parameters receive the active mixin FQN; the other strings are targets or diagnostics.
    callbacks = {"shouldApplyMixin": 1, "preApply": 2, "postApply": 2}
    for i, part in enumerate(parts[:-1]):
        if part.text not in callbacks or parts[i + 1].text != "(":
            continue
        end = group_end(parts, i + 1, "(", ")")
        args = arguments(parts[i + 2:end])
        position = callbacks[part.text]
        if len(args) <= position or not args[position] or end + 1 == len(parts) or parts[end + 1].text != "{":
            continue
        if words(args[position][:-1]) not in {("String",), ("final", "String")}:
            continue
        parameter = args[position][-1].text
        body = parts[end + 2:group_end(parts, end + 1, "{", "}")]
        for j in range(len(body) - 5):
            comparison = words(body[j:j + 6])
            if comparison[:4] == (parameter, ".", "equals", "(") and comparison[5] == ")":
                yield body[j + 4]
            elif comparison[1:] == (".", "equals", "(", parameter, ")"):
                yield body[j]


def generated_test_metadata(path: Path) -> dict:
    if not path.is_file():
        return {}
    parts = tokens(path.read_text(encoding="utf-8-sig"))
    metadata = {"entrypoints": {}, "mixins": []}
    for i in range(len(parts) - 6):
        if words(parts[i:i + 4]) == ("mod", ".", "entrypoints", "["):
            name = parts[i + 4].literal
            if name and words(parts[i + 5:i + 8]) == ("]", "=", "["):
                end = group_end(parts, i + 7, "[", "]")
                metadata["entrypoints"][name] = [token.literal for token in parts[i + 8:end]
                                                  if token.literal is not None]
        elif words(parts[i:i + 6]) == ("mod", ".", "mixins", ".", "add", "("):
            if parts[i + 6].literal is not None:
                metadata["mixins"].append(parts[i + 6].literal)
    if any(part.literal == "generateTestServerMetadata" for part in parts):
        required = {"skywars2:map-editing", "skywars2:test-checks"}
        if not required <= metadata["entrypoints"].keys() or not metadata["mixins"]:
            raise ValueError(f"{path}: could not read generated test-server metadata")
    return metadata


class Layout:
    def __init__(self, repo: Path):
        self.repo = repo
        self.files = 0
        self.types = {}
        self.targets = []
        self.errors = []

    def read_sources(self):
        for source_root, checks in SOURCE_ROOTS:
            root = self.repo / source_root
            for path in sorted(root.rglob("*.java")):
                relative = path.relative_to(self.repo)
                content = path.read_text(encoding="utf-8-sig")
                parts = tokens(content)
                package, names = declared_types(parts)
                directory = ".".join(path.relative_to(root).parts[:-1])
                if directory != package:
                    self.errors.append(f"{relative}: package {package!r} does not match directory {directory!r}")
                self.files += 1
                test_package = any(package == prefix or package.startswith(prefix + ".")
                                   for prefix in ("me.skywars2.validation", "me.skywars2.checks"))
                if not checks and (test_package
                                   or any(TEST_TYPE.fullmatch(name.partition("$")[0]) for name in names)):
                    self.errors.append(f"{relative}: test-only Java source belongs under src/checks/java")
                for name in names:
                    qualified = f"{package}.{name}" if package else name
                    if qualified in self.types:
                        self.errors.append(f"{relative}: duplicate Java type {qualified} in {self.types[qualified].path}")
                    self.types[qualified] = Source(relative, checks)
                for token in mixin_targets(parts):
                    where = f"{relative}:{content.count(chr(10), 0, token.offset) + 1}"
                    self.targets.append((token.literal, where, checks))

    def resolve(self, name: str, where: str, checks: bool) -> Source | None:
        name = name.replace("/", ".").partition("::")[0]
        source = self.types.get(name)
        if source is None:
            self.errors.append(f"{where}: missing Java type {name}")
        elif source.checks and not checks:
            self.errors.append(f"{where}: production references test-only type {name}")
            return None
        return source

    def metadata(self, metadata: dict, origin: Path, checks: bool):
        resources = self.repo / ("src/checks/resources" if checks else "src/main/resources")
        for key, entries in metadata.get("entrypoints", {}).items():
            for entry in entries:
                value = entry["value"] if isinstance(entry, dict) else entry
                self.resolve(value, f"{origin} entrypoint {key}", checks)
        for mixin in metadata.get("mixins", []):
            name = mixin["config"] if isinstance(mixin, dict) else mixin
            relative = PurePosixPath(name)
            if relative.is_absolute() or ".." in relative.parts or "\\" in name or ":" in name:
                raise ValueError(f"{origin}: invalid Mixin resource path {name!r}")
            config_path = resources / relative
            if not config_path.is_file():
                self.errors.append(f"{origin}: missing Mixin config {name}")
                continue
            where = str(config_path.relative_to(self.repo))
            config = json.loads(config_path.read_bytes())
            registered = set()
            for side in ("mixins", "server", "client"):
                for entry in config.get(side, []):
                    full_name = f"{config['package']}.{entry}"
                    registered.add(full_name)
                    self.resolve(full_name, where, checks)
            plugin = config.get("plugin")
            source = self.resolve(plugin, where, checks) if plugin else None
            if source is not None:
                content = (self.repo / source.path).read_text(encoding="utf-8-sig")
                for token in plugin_classes(tokens(content)):
                    value = token.literal
                    if value is not None and value.startswith(OWN_PACKAGE):
                        location = f"{source.path}:{content.count(chr(10), 0, token.offset) + 1}"
                        self.resolve(value, location, checks)
                        if value not in registered:
                            self.errors.append(f"{location}: {value} is not registered in {where}")

    def check(self):
        self.read_sources()
        fabric = Path("src/main/resources/fabric.mod.json")
        self.metadata(json.loads((self.repo / fabric).read_bytes()), fabric, checks=False)
        gradle = Path("build.gradle")
        self.metadata(generated_test_metadata(self.repo / gradle), gradle, checks=True)
        for target, where, checks in self.targets:
            if target.replace("/", ".").startswith(OWN_PACKAGE):
                self.resolve(target, where, checks)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[1],
                        help="LuckyWars checkout to inspect")
    args = parser.parse_args()
    layout = Layout(args.repo.resolve())
    try:
        layout.check()
    except (OSError, ValueError, KeyError) as error:
        parser.exit(1, f"Java layout check failed: {error}\n")
    if layout.errors:
        parser.exit(1, "\n".join(layout.errors) + "\n")
    print(f"Java layout verified: {layout.files} files, {len(layout.types)} named types")


if __name__ == "__main__":
    main()
