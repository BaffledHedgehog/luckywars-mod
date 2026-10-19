import difflib
import json
import pathlib
import re

baseline = json.loads(pathlib.Path('build/verification/comments-before.json').read_text(encoding='utf-8'))
lexer = re.compile(
    r'(?P<comment>//[^\r\n]*|/\*[\s\S]*?\*/)'
    r'|(?P<literal>"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\')'
    r'|(?P<token>[\w$]+|>>>=|>>=|<<=|>>>|>>|<<|\.\.\.|->|::|\+\+|--|&&|\|\||==|!=|<=|>=|[+*/%&|^\-]=|[^\s])'
)

def tokens(source):
    return [m.group() for m in lexer.finditer(source) if m.lastgroup != 'comment']

changed = []
failed = []
diff = []
for name, before in baseline.items():
    after = pathlib.Path(name).read_text(encoding='utf-8')
    if after == before:
        continue
    changed.append(name)
    if tokens(before) != tokens(after):
        failed.append(name)
    diff.extend(difflib.unified_diff(before.splitlines(True), after.splitlines(True), fromfile=name, tofile=name))

pathlib.Path('build/verification/comments.diff').write_text(''.join(diff), encoding='utf-8')
print(json.dumps({'changed': changed, 'non_comment_changes': failed}, ensure_ascii=False, indent=2))
if failed:
    raise SystemExit(1)
