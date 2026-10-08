"""Separate relocated documents from unchanged source snapshot identifiers."""
import json
from pathlib import Path

NOTES = Path(__file__).resolve().parents[1]
DOCUMENT_ROOT = NOTES.parent
CONFIG = json.loads((NOTES/'inventory/source_roots.json').read_text(encoding='utf-8'))
SOURCE_ROOT = Path(CONFIG['datapack_root'])

def document_path(identifier):
    return DOCUMENT_ROOT / identifier
