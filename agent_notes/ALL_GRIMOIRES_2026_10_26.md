# Complete consumed grimoire library

## Author requirements (user)

> ВСЕ гримуары это просто одноразовые жезлы (совсем у немногих есть минимальные различия)

> продолжай перенос. Раз такие быстрые патчи - перенеси все оставшиеся гримуары одним патчем, не растягивай так сильно

This patch closes the remaining167 direct books together. With the earlier72
elemental/mode books and Danmaku, the complete weighted library has240 unique
playable entries: IDs1..241 except189. The source has no direct189 or242 book.
Random-all preserves the explicitly disabled189 outcome;242 creates a flying
Sauvojen wand and is implemented as a random-only dependency.

## Read-only source and presentation

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Primary evidence is `lbc/loot_table/spells_library.json`,
`lbc/function/items/libspells/select.mcfunction`, `select_wand.mcfunction`,
`lbc/function/workingitems/badwiki_lib.mcfunction`,
`random_spell_lib.mcfunction`, the dispatched wand helpers, and
`workingitems/unstable_spellbook/` plus `workingitems/raycast_spells/`.
Flying Sauvojen helpers are under `workingitems/sauvojen_staffs/`.

`tools/import_lbc_remaining_grimoires.py` imports the167 deterministic
`lbc:ported/*_grimoire` leaves, native defaults in
`src/main/resources/skywars2/remaining_grimoires.json`, and the complete broad
table. The existing72 leaves and Danmaku remain unchanged. Only native item
identities and obsolete schema names change; source pools, weights, models,
tiers, names, lore, blank lines, formatting and custom data are retained.
RU/EN/RPR translations and item models are checked against the current author
resource pack read-only. The source's intentional `?` / `? ` translation keys
on unknown-property books remain intentional. No new resource-pack additions
are needed, and previously applied/deleted templates are not recreated.

| Source IDs | Remaining family |
| --- | --- |
| 1..4 | Evoker: random, spiral, ring, fan |
| 5..9 | Bass: random and four fixed effects |
| 10..14 | Corruption: random and four fixed effects |
| 15..19 | Ender normal/safe; Lifestealer normal/strong/charge |
| 20..27 | Confuse random/four effects; Blood normal/safe; random Badwiki |
| 28..111 | All84 fixed Badwiki effects |
| 112..119 | Wall normal/horizontal/stair/break; advanced Ender four modes |
| 120..128 | Three Falling Stars modes; two Prisms; Blaze/turret head; both random books |
| 129..140 | Five contact Hedgehogs; both Sheep grades; five Psycho modes |
| 141..153 | Three Tesla modes; two Caustic acids; two Omega grades; six Neutral modes |
| 154..158 | Lightning normal/chain/reforge; both Exoperso grades |
| 195..198 | Four Acid+Corruption modes |
| 237..241 | Bedrock normal/cube/clear/regeneration/remove |

Exact ID-to-item and fixed-effect mappings are recorded in the importer and
generated manifest rather than duplicated in another catalog.

## Native shared payloads

`LbcLibrarySpells` registers the imported native items and invokes shared wand
payloads directly. Admission checks role, level, resident origin, offhand
configuration, action restrictions, personal/dimension Time Stop and antimagic.
One successful use consumes one actual held book and increments its native use
stat. Neither zero nor negative mana prevents use; no fake wand or temporary
mana credit is involved. Books do not switch wand modes, claim another held
item's Academy trial or invoke paid-wand cast bonuses. Paid wand wrappers retain
their existing admission, price, mode and bonus behavior.

Badwiki uses the existing effect interception layer, including the three RTP
effects that become internal teleport counters. Caustic acid keeps eight
charges and source ignition. Tesla books use the source four ordinary arcs,
one chain arc, or the shared conducting instant ray. The upgraded safe Omega
uses the existing ally policy. Sheep shares permanent native polymorph,
gender/potion interactions and the shader-hole engine. Bedrock books share
layered room restoration and regeneration. The established Danmaku rule remains:
ordinary cast consumes its book; the free charged Shift bomb does not.

## Random-only dependencies

All53 branches of ordinary random are dispatched explicitly. Existing consumed
payloads are reused where equivalent. Special branches use owned native finite
emitters:26 genuinely delayed TNT entities with source velocities/fuse56;
the80-second phantom-arrow field;60-second Wither I;200-tick Instant Health V;
the lava conversion;25-second firestorm; and60-tick falling stone-brick rain.
These are genuine delayed actors, not instant-fuse explosion substitutes.
The random lightning terminal starts the existing repeating WILD native-bolt
chain across its radius16, distinct from Raid Omen124's damage-chain status.
Ordinary Psychoblind releases three charges. Random phantom arrows can target
the caster/allies and retain WILD damage attribution; ordinary Vex Sword
targeting is unchanged. Source ray particles and terminal sounds/particles are
retained. The lava transform preserves ordered tag replacements, the sparse
31-cube source-lava scatter at probability0.0001 per cell, absolute Y1..126
flowing-lava column and four ghasts; terrain runs through existing native work.

Random-all retains the1..242 range and at most five self-rerolls. The flying
Sauvojen dependency has nine authored models,200-tick life, source fire periods,
own origin/direction and nearest-enemy aim. It calls existing Ruby/Terra/
Electro/Air/Acid/Teleport/Stone/Fire/Blind factories without moving the caster.
Its child source lifetimes are captured locally at launch. The four flying
electrical arcs retain their separate source lifetimes. Time Stop pauses the finite emitters. Antimagic removes the magical phantom
field/flying wand; the source firestorm/falling-block generators are physical
after placement. Owner/epoch invalidation and match stop clean up owned work.

## Verification

Full offline Gradle build succeeded. Build `0d3733b6` passed54 recorded groups in
two actual mode1 matches (58.95s): all167 new books through actual final-stack
item use in both hands/Shift at zero/negative mana; exact imported/default
components; all84 fixed effects including native RTP counters; complete broad
ready-only rolls; all53 ordinary random branches and nine flying factories;
26 delayed TNT velocities/fuse/author;600 native stone-brick falling actors
from60 active emitter ticks; and actual phantom-arrow collisions against the
caster and an allied player on a team with friendly fire disabled.

The existing shared wand regressions cover Evoker/Bass, Corruption, Ender/
Badwiki, Blood/Confuse, Neutral, advanced Ender/Snow, Prism, Tesla, Bedrock,
Lifestealer, Hedgehog, Sheep, Ruby, Stone, Air, Teleport and the Vex Sword family.
Earlier ordinary/corrosive/terrain consumed suites also passed. Retained new
actors coexist with the primitive wand fixtures; actual match stops then clear
both, including explicit native Ruby/Stone cleanup checks. The isolated runner
orders virgin-terrain fixtures before destructive regressions and clears their
loose loot/XP between independent suites.

Diagnostic runs found and resolved outdated fixture expectations for native
RTP interception and the four/one Tesla arc counts, and independent test-region
collisions. Additional native execution caught an old falling-block state
schema and the two distinct native team gates for WILD phantom arrows. The
Stone producer now prunes discarded charges even when the unload callback
has not fired yet. The older Ruby assertion now follows the author's existing
friendly-mob impact rule; its gameplay filter was not changed.

Exact artifact closure: 381 Java sources, 1079 classes and
3076 resources. Main baselineb4aa411f stayed unchanged throughout
qualification. Installer21564 then installed0d3733b6 overb4aa411f with a verified
rollback and no server restart. Protected user files/deletions and author RP
are unchanged. The isolated server stopped and restored its settings/profile/
scoreboard/JAR fixtures. Only the known fake-bot packet-context diagnostics
occurred in the successful run. Client pixels/audio playback and peak TPS
remain outside these native fixtures.

Evidence: `build/verification/all-grimoires-final/qualification.json`, immutable
installer input `qualification-0d3733b6.json`, `main-install.json`, build/runtime
logs and results JSON. Full SHA256: `0d3733b6e2bf78f596ad51cfeb6c8508fdd66d64c69193e21c0922d53f2716ae`.
