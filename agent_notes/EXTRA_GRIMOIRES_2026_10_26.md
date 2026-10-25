# Missing-family grimoires — 26 October 2026

## Request (user)

> добавь гримуары ещё для тех жезлов, у которых сейчас нет своего гримуара
> та же тема - по гримуару за каждый жезл и для последних двух за каждый его режим после улучшения академии
> если обычный режим академии эквивалентен неулучшенному жезлу для него не надо отдельный гримуар
> tier ставь у гримуаров такой же как у соответствующего жезла
> И в достижение за все гримуары не забудь новые докинуть.

## Implementation

21 books, contiguous IDs243..263, extend the existing manifest/registered item
dispatcher and weighted library without altering the original240 entries.
Native item defaults and deterministic loot components agree. New grimoire
rarity follows its wand family; effective tooltip tier is read from the final
merged source-wand components, including Academy overrides. Tiny Acid/BlindB,
other TinyA; SoullessB/MetasolaA; WindB/AcademyA; FartusS, GaycasterA,
IceS, ShishB, VexSoulA, PsychoVexS. Full manifest is in the RP template.

Tiny books use the exact existing Tiny payloads; no surrogate held wand. Soulless
uses the shared ghosting producer or actual metasola loot generator. Wind uses
the same four WILD movement/charge modes. Fartus shares its five-projectile
random banana/label geometry; Gaycaster and Ice share their ordinary producers.
Shish retains posture-based loaded-chunk ray targeting. Summoners share native
Vex/cultist spawn producers and ownership, with paid admission/hooks outside the
shared payload. Books consume exactly one actual stack item, use no mana and
cannot alter unrelated Academy trials. Existing wand wrappers remain paid.

The two Academy default modes equal their ordinary versions, so no duplicate
books. Other newly discovered wand families receive one ordinary book each.
Magical tools, passive arcanums and technology weapons are outside this scope.

The collection advancement retains existing criterion IDs, removes nonexistent
spell189 and adds21 actual custom-data predicates. Now261 criteria/books;
random-only flying wand242 has no collectible book. The random-all spell can
also choose new payloads. Existing weighted table entry weights are unchanged.

Generator `tools/generate_extra_grimoires.py` is deterministic and called after
the original remaining-grimoire importer. It reads the author RP, emits only
new model definitions/generated models/unchanged placeholder PNG copies and42
new translation keys per language into template. It never writes the author
pack or the protected user-edited template language files.

## Verification

Evidence directory: `build/verification/extra-grimoires-final/`.
Qualification is recorded only after native checks and artifact closure pass.
New checks exercise actual registered final-stack use for all21 books in both
hands/Shift with zero/negative mana; native payload counts and ownership;
antimagic/time-stop/offhand guards; actual inventory advancement predicates;
and lifecycle at a real match stop. Prior library and shared wand regressions
run alongside these checks. RP model/texture/key/tier closure is checked offline.
Client pixels/audio playback and peak-capacity TPS are not directly observed.

Qualified and installed: `081a2f7a9b16483c8664244b18ad9b80ea3aa0b27dcb34e7df39ddfabc428b21`.
All28 groups passed in two actual mode1 matches (114.93s). Exact383 sources,
1081 classes and3097 resources match the built artifacts; all5 protected user
file hashes and4 prior deletions remain unchanged. Native inventory predicates
complete the collection only after the final new book. Seven affected wand
families and the earlier library/random/ordinary-elemental checks passed.
Strict virgin-combat fixtures run before damage-heavy catalog fixtures; the
catalog reset also clears newly reachable families from random-all outcomes.
This ordering/isolation change affects checks only.

Installer25680 installed over0d3733b6 with rollback and no restart. Installed
SHA verified. The obsolete753bcbe3 archive was removed after metadata/hash and
directory checks; current081a2f7a, preceding0d3733b6 andb4aa411f fallback remain.
Cumulative obsolete archive cleanup:308 JARs,10,924,762,717 bytes.
