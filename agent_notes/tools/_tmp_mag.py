from pathlib import Path
for p in ['other/techno/magnum/shoot/uwu.mcfunction','other/techno/magnum/check_shift.mcfunction','other/techno/magnum/check_shift_left.mcfunction','other/techno/magnum/shoot/standard/loop.mcfunction']:
 print('\n',p); print((Path('world/datapacksdisabled/LBC/data/lbc/function')/p).read_text(encoding='utf8'))
