"""Validate source hashes and review manifests; does not assess semantic completeness."""
import hashlib
import json
from pathlib import Path

NOTES = Path(__file__).resolve().parents[1]
INVENTORY = NOTES / "inventory"
ROOT = Path(json.loads((INVENTORY / "source_roots.json").read_text(encoding="utf-8"))["plugin_root"]).parents[1]


def main():
    errors = []
    documents_changed_since_review = []
    snapshot = json.loads((INVENTORY / "java_extension_sources.json").read_text(encoding="utf-8"))
    inventoried = set()
    reviewed = set()
    summaries = []

    def validate(row, origin):
        path = Path(row.get("source", row.get("path", "")))
        if not path.is_absolute():
            path = ROOT / path
        if not path.is_file():
            errors.append({"manifest": origin, "source": str(path), "error": "missing file"})
        elif hashlib.sha256(path.read_bytes()).hexdigest().lower() != row["sha256"].lower():
            errors.append({"manifest": origin, "source": str(path), "error": "SHA mismatch"})
        return path.resolve().as_posix().lower()

    for row in snapshot["files"]:
        inventoried.add(validate(row, "java_extension_sources.json"))
    for name in ["plugin_commands_review.json", "plugin_items_review.json", "plugin_runtime_review.json"]:
        data = json.loads((INVENTORY / name).read_text(encoding="utf-8-sig"))
        rows = data.get("reviewed_sources", data.get("files", []))
        sources = {validate(row, name) for row in rows}
        reviewed.update(sources)
        document = Path(data.get("documentation", data.get("review_document", data.get("generated_for", ""))))
        if not document.is_absolute():
            document = NOTES / document
        if not document.is_file():
            errors.append({"manifest": name, "document": str(document), "error": "missing document"})
        elif data.get("review_document_sha256") and hashlib.sha256(document.read_bytes()).hexdigest() != data["review_document_sha256"]:
            documents_changed_since_review.append({"manifest": name, "document": str(document), "note": "Editable documentation changed since the review snapshot; user edits are allowed."})
        summaries.append({"manifest": name, "reviewed_file_count": len(rows), "document": document.name})

    report = {
        "source_root": ROOT.as_posix(),
        "inventoried_source_count": len(inventoried),
        "reviewed_inventoried_source_count": len(inventoried & reviewed),
        "reviewed_unique_source_count": len(reviewed),
        "inventoried_sources_missing_review": sorted(inventoried - reviewed),
        "additional_reviewed_sources": sorted(reviewed - inventoried),
        "reviews": summaries,
        "documents_changed_since_review": documents_changed_since_review,
        "error_count": len(errors),
        "errors": errors,
        "scope": "Manifest membership and current SHA only. Review records are source-level findings; neither semantic completeness nor live runtime is validated.",
    }
    (INVENTORY / "java_extension_review.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: report[k] for k in ["inventoried_source_count", "reviewed_inventoried_source_count", "reviewed_unique_source_count", "error_count"]}))
    raise SystemExit(bool(errors or inventoried - reviewed))


if __name__ == "__main__":
    main()
