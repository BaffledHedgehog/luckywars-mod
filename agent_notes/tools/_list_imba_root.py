from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba')
for p in sorted(base.glob('*.mcfunction')): print(f'{p.stem:50} {len(p.read_text(encoding="utf-8").splitlines()):6} {p.stat().st_size:8}')
