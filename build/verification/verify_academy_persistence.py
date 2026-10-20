"""Archive the unfinished-progress session test without repeating unchanged gameplay checks."""
import hashlib
import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "build/verification"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    artifact = ROOT / "build/libs/skywars2-0.1.0.jar"
    digest = sha(artifact)
    previous = json.loads((OUT / "academy-challenge-verification.json").read_text(encoding="utf-8"))
    reference = Path(previous["archive"])
    assert sha(reference) == previous["sha256"]
    runtime = json.loads((OUT / "academy-challenge-results.json").read_text(encoding="utf-8"))
    assert runtime["passed"] and runtime["artifactSha256"] == digest
    assert len(runtime["completed"]) == 8
    assert "BUILD SUCCESSFUL" in (OUT / "academy-persistence-build.log").read_text(encoding="utf-16")
    changed = []
    with zipfile.ZipFile(reference) as old, zipfile.ZipFile(artifact) as current:
        assert current.testzip() is None
        assert set(old.namelist()) == set(current.namelist())
        for name in current.namelist():
            if old.read(name) != current.read(name):
                assert name.startswith("me/skywars2/game/GameChecks") and name.endswith(".class"), name
                changed.append(name)
    assert changed
    source = "me/skywars2/game/GameChecks.java"
    with zipfile.ZipFile(ROOT / "build/libs/skywars2-0.1.0-sources.jar") as archive:
        assert archive.read(source) == (ROOT / "src/main/java" / source).read_bytes()
    assert sha(ROOT.parent / "mods/skywars2.jar") == previous["unchangedMainSha256"]
    target = OUT / f"skywars2-academy-persistence-{digest[:8]}.jar"
    shutil.copyfile(artifact, target)
    report = {
        "sha256": digest, "archive": str(target), "runtime": runtime,
        "unchangedMainSha256": previous["unchangedMainSha256"],
        "unchangedProductionReferenceSha256": previous["sha256"],
        "changedJarEntries": changed,
        "regressionEvidence": "academy-challenge-verification.json; all production classes/resources byte-identical",
        "limits": ["Native fake players; no real-client visual test", "No mass-load MSPT benchmark"],
    }
    (OUT / "academy-persistence-verification.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"sha256": digest, "archive": str(target), "completed": len(runtime["completed"])}))


if __name__ == "__main__":
    main()
