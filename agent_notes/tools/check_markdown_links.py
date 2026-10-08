"""Check local Markdown file targets; heading semantics are audited separately."""
from pathlib import Path
import json
import re
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT/'agent_notes'


def main():
    errors = []
    checked = 0
    for document in NOTES.glob('*.md'):
        text = document.read_text(encoding='utf8')
        for match in re.finditer(r'\]\((<[^>]+>|[^\s)]+)\)', text):
            target = match.group(1).strip('<>')
            if target.startswith(('#', 'http:', 'https:', 'mailto:', 'app:', 'skill:')):
                continue
            path = unquote(target.split('#', 1)[0])
            # Markdown exported with /E:/ paths needs a Windows drive path.
            if re.match(r'^/[A-Za-z]:/', path):
                path = path[1:]
            path = re.sub(r':\d+$', '', path)
            candidate = Path(path)
            if not candidate.is_absolute():
                candidate = document.parent/candidate
            checked += 1
            if not candidate.is_file():
                errors.append(dict(document=document.relative_to(ROOT).as_posix(), line=text.count('\n', 0, match.start())+1, target=target))
    report = dict(checked_links=checked, error_count=len(errors), errors=errors,
                  scope='Local file links only; Markdown heading anchors and prose not assessed.')
    (NOTES/'inventory/markdown_link_audit.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    print(json.dumps(dict(checked_links=checked, error_count=len(errors), first_errors=errors[:12]), ensure_ascii=False))


if __name__ == '__main__':
    main()
