# Nine direct elemental grimoires

## Source and scope

Read-only legacy `LBC/data/lbc/loot_table/spells_library.json` and
`function/items/libspells/select.mcfunction` route direct IDs160/163/165/167/
171/173/176/181/184 to the ordinary Ruby/Terra/Electro/Air/Acid/Teleport/Stone/
Fire/Blind `workingitems/sauvojen_staffs/*_preloop` helpers. These are consumed
snowball carriers, not mana-paid wand casts. Their stored `mana` values are
library-crafting metadata. No native projectile is needed for the throwaway
carrier. Broad/random library tables remain unqualified and hidden.

| Spell ID | Native item and narrow `lbc:ported/` leaf | Source model suffix | Metadata mana | Tier |
| --- | --- | --- | --- | --- |
| 160 | ruby_grimoire | swarogia | 1200 | C |
| 163 | terra_grimoire | terra | 1200 | C |
| 165 | electro_grimoire | sosuenerugi | 900 | C |
| 167 | air_grimoire | explode | 1000 | C |
| 171 | acid_grimoire | lahazor | 200 | C |
| 173 | teleport_grimoire | arkontas | 2000 | B |
| 176 | stone_grimoire | stone | 2000 | C |
| 181 | fire_grimoire | fire | 200 | C |
| 184 | blind_grimoire | blind | 300 | D |

Models live under `lbc:spelllib/`. The importer preserves every source modifier,
name/lore component/color/italic flag, blank row, Epic rarity row and stack99;
only the item ID and obsolete loot schema field names change. The current
author resource pack `26.3(1)` is audited read-only for RU/EN/RPR translations,
item/model/texture dependencies. No translation fragment or RP edit is needed.
Native `/give` defaults carry matching authored presentation and custom data.

## Native behavior

Each use starts its existing ordinary payload engine and consumes one actual
used stack. Both hands retain the shared parallel-use dispatcher. Zero/negative
mana is admitted and remains unchanged. Shift still casts the ordinary spell;
grimoire flags cannot invoke wand modes or Academy meditation/readiness.
Creative consumption follows the native item rule. Existing native payload
ownership, collision, damage tags, sounds and lifecycle are shared.

Counts per consumed cast: Ruby/Terra/Air/Acid/Teleport/Stone one charge each,
Electro three traces with40/45/50-tick lifetimes, Fire three small fireballs,
Blind five charges. Acid keeps the ordinary spread. Source cast cues are
amethyst-cluster break, rocket twinkle, explosion, zombie-villager cure,
lightning thunder, firecharge use and squid squirt respectively; native snowball
throw feedback/statistics remain without creating a snowball carrier.

Observer/dead/removed/ending, wrong world, unloaded origin, disabled offhand,
blocked input, time stop and antimagic guards reject before consumption.
The wrapper does not call the paid wand admission/debit/bonus path.

## Qualification

Build2159fbe9 passed12 recorded groups in two actual mode1 matches (69.00s).
Exact373 sources/1052 classes/2846 resources match the tested artifact, including
all nine new narrow loot leaves. The explicit
`elemental-grimoires` fixture exercises actual game-mode use of all nine kinds,
both hands and Shift, exact imported/default components, metadata, charge
counts, owner/epoch/motion, source sound packets, unchanged mana, final-stack
parallel use and nonconsuming guards. Separate retained actual-stop fixtures
cover all nine payload engines. Client rendering/audio and peak TPS require
separate client observation.

Evidence: `build/verification/elemental-grimoires-final/qualification.json`,
`main-install.json` and `tools/smoke_elemental_grimoires.py`. The isolated
instance stopped normally; settings, players, scoreboard and its JAR were
restored and fixture locks released. Only the known fake-bot Polymer packet
context diagnostic appeared. Pre-existing user hashes/deletions and author
RP remain unchanged. Installer21172 installed2159fbe9 over22b646b6 after
qualification, retaining22b646b6 as rollback; no restart was performed.
