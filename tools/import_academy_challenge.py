"""Prepare the one internal Academy challenge advancement for native completion."""
import json
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import write_table


def main():
    path = SOURCE / 'advancement/challenges/no_knowledge.json'
    advancement = json.loads(path.read_text(encoding='utf-8-sig'))
    assert advancement['rewards'] == {'function': 'lbc:swrg_kit_integration/gain/challenge_completed'}
    # The reviewed reward only emits title/subtitle; LbcLoadouts performs it after native award.
    del advancement['rewards']
    write_table(ROOT / 'src/main/resources/data/lbc/advancement/challenges/no_knowledge.json', advancement)
    print('Prepared native no_knowledge criterion; source completion message handled in Java')


if __name__ == '__main__':
    main()
