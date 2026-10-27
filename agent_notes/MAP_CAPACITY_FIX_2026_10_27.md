# Map selection and unused template references, 27 October 2026

## Author clarification (user)

> так и должно быть! это называется ГЛЭКОКОД - так как игра не ругалась когда каких то частей не было, я просто переиспользовал шаблон не удаляя несуществующие template строки, все карты там есть

Absent template references in these source map functions are intentional unused
lines, not evidence that the intended map geometry is incomplete. This explicit
clarification supersedes historical notes calling the source snapshot incomplete.
Do not reconstruct missing pieces or disable these maps because of those lines.

## Corrections

`GameController.requestStart` now obtains its quick-game pool from
`MapManager.quickStartCandidates`: playable numeric SkyWars maps whose actual
capacity fits the current player count, filtered by broken-variant availability.
No fixed player-count tiers or hardcoded map-number arrays remain.

The stale arrays excluded map39 (10 spawns) at9/10players and map54 (16 spawns)
at13..16players. The latter left the previous13..16pool entirely empty.

`tools/import_game_resources.py --maps-only` skips absent unused source template
commands and retains every existing placement in original order with original
coordinates/integrity/seed. Omitted references remain in diagnostics. An invalid
namespace or unparsed command remains a real import error. A playable map needs
existing placements and at least two actual spawn markers; broken variants must
retain the same capacity. The maps-only refresh does not copy or rewrite NBT or
loot tables and preserves existing non-map diagnostics.

The refreshed catalog admits all63 visible SkyWars cards, including21previously
blocked maps, plus existing11BedWars geometry manifests. BedWars gameplay remains
unsupported; this changes geometry availability, not game-mode implementation.
The unused63 entry has no card, placements or spawns and remains unavailable;
Dragonball is map64 with24spawns. Empty future BedWars placeholders also remain
unavailable.

The menu now sends the specific existing unavailability reason if a card becomes
unavailable, instead of silently ignoring its click. No RP translations changed.

## Capacity pools

| Players | Eligible normal SkyWars maps |
| --- | --- |
|9–10|39,54,55,56,57,59,60,61,62,64|
|12|54,55,56,57,59,60,61,62,64|
|16|54,59,60,61,64|
|20|59,61,64|
|24|64|

The actual catalog is authoritative, so future capacity changes need no new
hardcoded array. `manifest-audit.json` verifies unchanged capacities, exact source
placement filtering/order, existing resources and individual/catalog agreement.

## Native verification

Build8624ee1c passed13 recorded native results in two actual mode1 matches
(119.06s):16players on normal map59 and24players on broken map64. Each player had
a distinct native Participant spawn, remained an active participant and fit the
chosen map. Native chest generation passed in both matches (92/56containers).
The actual menu selected previously blocked23 and map54; a separate full map23
preparation completed successfully. Candidate checks cover counts1..25 in both
variants, specific39/54regressions and excluded unsupported/empty entries.

The initial harness run completed the menu and16-player match on54, but waited
for the wrong24-player setup log suffix. That runner-only acknowledgement was
corrected without Java/resource changes. The initial result is retained as
non-qualified diagnostic evidence; the final complete run above is authoritative.
Both owned children stopped normally and restored their exact snapshots, including
all24 UUID playerdata/stats/advancement files.

Exact artifact: `build/verification/skywars2-map-capacity-8624ee1c.jar`.
SHA256: `8624ee1c973730ac346fe1735025bb2fae45b67e04aedf55c010f963ec9bea69`.
All506Java sources,1443classes and3215resources match the tested JAR. All175
protected author paths and31live grimoire override files are unchanged. No RP edit.
Manifest: `build/verification/map-capacity/qualification.json`.
Native log/results: `build/verification/map-capacity-gameplay*`.
Static source/catalog audit: `build/verification/map-capacity/manifest-audit.json`.

The safe automatic installer adopts this build instead of the Sauvojen-only
request; all prior fixes are included. Main735d9905 is running, so installation
waits for normal shutdown/world-lock release, without forced stop or restart.
Receipt: `build/verification/map-capacity/main-install.json`. Retain this ready
build, preceding qualified0c952772 and exact installed735d9905 rollback. Obsolete
16502e73 and25d7466b are retired only after verified installation and exact
parent/non-reparse/hash/size/mtime checks. Small evidence is retained.

No physical-client or peakTPS claim; unused map placeholder63 and BedWars gameplay
remain unavailable. Intentional omitted references are not incomplete-map warnings.
