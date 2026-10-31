# Ten Imba runes — implementation batch, native validation pending

This note records source closure and the new Java owner. It is not a release or
native PASS receipt. No build, server, main installation, resource-pack write or
commit was performed by this implementation worker.

## Source and decisions

Read-only legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
`lbcr/function/raycast.mcfunction:1-3` and `raycast_left.mcfunction:1-3` enter
`lbc/function/workingitems/raycast_spells/select.mcfunction:48-66`, which selects
all ten `lbc/function/other/imba/runes/*_cast.mcfunction` files. Native admission
uses the actual used hand, fixing the legacy offhand SelectedItem mistake.
No name/lore/material-only recognition is introduced: raw integer-one `spell`,
`raycastspell`, and the corresponding `rune_*` leaf are required. Existing raw
loot definitions, models, names, formatting and weights remain untouched.

User decision relayed by root: **the stronger antimagic artifact suppresses every
rune cast too**. Root constructs the owner with `artifactSuppressesAll=true`.
The source beacon20 admission, ordinary dimensions, current native participant,
role and Time Stop owners remain shared. The source immediate bodies themselves
did not test nomagic; this explicit user decision supersedes that omission.

|Rune|Source behavior implemented|
|---|---|
|Fehu|201 original sample points, step1, crying-obsidian dust; unit-cell mob selector/range15 or non-air terminal; nomagic16 and next-point beacon20 continuation; one native token debit at terminal; original `lbc:item_gallery` template offset(-19,0,-19), including entities, through MapManager native placement and indirect terrain cause. Finite miss/cancellation leaves token. Commented itemkiller marker remains inactive.|
|Gifu|201 sample points step0.33; happy-villager particles, unit-cell mob selector/range7; block terminal retreats0.33; nomagic7/next-point beacon20; one terminal debit and twenty original `lbc:random_glack_1` native loot rolls.|
|Uruz|Attack-damage base exactly1e100 (legacy literal has101 digits), native infinite ResistanceV.|
|Thurisaz|18 permanent positive attribute modifiers with exact `minecraft:rune_thurisaz` identity;11 hostile negative modifiers with `minecraft:rune_thurisaz_negative`; positive attack-knockback alone ADD_VALUE, all other entries ADD_MULTIPLIED_TOTAL. Original values/tags `gigantic`/`submissive`; cast preserves source same-team/strict/spectator filtering without a new Greed gate. Native falling-anvil100 within1 every gameplay tick is WILD for the already-submissive cohort, without reapplying the cast's team filter. Repeated casts retain native modifier identity without stacking.|
|Ansuz|Native Creative mode.|
|Raido|Canonical fakeholder `bottom_nexus` in `lbc.math`; native existing Nexus transfer once per20ticks below/equal-100; original integer RTP[-4800,4800], y2332 and antigravity modifier removal shared with Hedgehog dimensions.|
|Kenaz|Native same-coordinate/yaw/pitch Nether transition for source-world entities outside caster position, excluding core/strict objects and shared protected roles. Source has no team or Greed filter; native transfer and its shared indirect cause are WILD.|
|Wunjo|Shared actual participant lives+16, native keepInventory=true, original green title.|
|Hagalaz|Root MapManager/controller clear hook, native loaded source-world item purge, original dark-red title.|
|Nautiz|Original green title, online-player ResistanceV200ticks, fallDamage=false, source-world(0,1000,0) transfer, root shared controller restart intent and four original native nonplayer/nonmarker kill sweeps.|

There are no delayed rune attribute removals in the audited source casts. Their
modifiers are permanent until source `lbc/function/resets.mcfunction:158-200`
or match lifecycle reset. Native attributes/tags/scores are the authoritative
world/player checkpoint state. No parallel timer, ray queue, effect stack,
dimension engine or ability snapshot was created.

## Shared hooks and remaining root integration

`LbcImbaRunes(GameController, GlobalActions, boolean)` exposes static `matches`,
`kind`, and instance `use(player,hand)`, `tick()`, `stop()`. `GlobalActions`
requires `clearMap(author)`, `restart(author)`, `addLives(author,amount)`.
Root integrates actual raw UseItem selection, gameplay tick and match stop.
`stop()` removes both rune modifier identities/native tags and resets the source
bottom-Nexus score; normal player lifecycle resets retain canonical base values.

Authorized narrow shared change in `LbcExasperato`: default replay protocol
`sampleStep/sample/sampleContinues` plus `replayableSamples`. A nonzero sample
step uses the same CPU-budgeted, epoch-owned Search queue, position/remaining,
typed terminal and existing Vanity checkpoint. Default DDA, shield, suppression,
technology and other spell contracts remain on their original path. Native
sample contact runs before the original recursion suppression gate.

Global legacy dependencies live under
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/`:

- `swrg/function/maploader/clear.mcfunction`:64 temporary marker origins x/z
  -192..144 step48, inclusive fills spanning(-192,0,-192)..(192,319,192),
  then source maploader/lobby predicate cleanup and #mapstat=1. Root owns bounded
  native region clearing and original predicate-tag classification.
- `swrg/function/game/quickstart/1.mcfunction`, `1a.mcfunction`, player-count
  branches and `2.mcfunction`: prepare/reload map, select original capacity
  group, load Overworld/Imprinted/White, remove the f8hw83fhfjd3 gravity modifier,
  schedule start/0 after5seconds. Root's shared-controller adaptation waits for
  actual native lobby/map readiness and starts a real new match/epoch. This
  deliberately uses existing match lifecycle instead of a duplicate quickstart
  engine or an arbitrary private5second timer. Epoch invalidation prevents using
  an old Vanity point to restore across this full restart.

## Native check entry points (authored, NOT RUN)

- `ImbaRuneChecks.run(game)`: ten actual imported leaves/raw guards; both hands
  under native beacon and stronger artifact; actual Uruz/Thurisaz/Ansuz uses;
  independent201-point queue geometry controls, both ray finite misses, Gifu
  actual terminal loot/debit and switched physical stack cancellation. Borrowed
  attributes/components/terrain/player state and carrier stats are restored.
- `startGlobals/tickGlobals/cancelGlobals`: five actual native global payloads
  Wunjo/Raido/Kenaz/Fehu/Hagalaz, with the existing full-world Vanity coordinator
  restoring world/core/players/actors between each. Original core/player images
  use existing shared checkpoint classes, never a parallel implementation.
- `startRestart/tickRestart/cancelRestart`: **destructive final-match suite** for
  Nautiz, checking actual raw debit, new epoch, native prepared map, active
  participant and reset lives. The disposable native harness must own restoring
  its original server snapshot; this suite makes no cross-epoch rewind claim.

Root must register/tick these check suites and run compilation/native evidence.
All failures must retain their original receipts; no ready/released claim follows
from the existence of these files.

Final targeted review: artifact suppression uses the common full20 radius at each
sample and its next point, retaining raw legacy16/7 tags as additional compatibility;
Nautiz player coordinate edits use `TeleportMomentum.positionFlags`. Kenaz native
entity transitions explicitly retain their current velocity. The global Hagalaz
fixture waits for the actual probe to clear and shared Stonekivi accepted work to
settle before rewinding. These fixes are authored and have not been run.

## Root integration and native evidence (31 October 2026)

Runtime/check build passed. `imba-runes` passed on `de145976`; actual controller
`imba-runes-restart` passed again on `869880d5`. Final `0794b535` passes all five
global rune payloads and complete production rewinds between them. Its production
classes/data are byte-identical to `869880d5`. The original gallery is now
packaged; native KENAZ transitions retain velocity and place destination tickets.
Vanity prepares/includes native actor destinations before creating its image.

The global fixture acknowledges every affected simulated client's real native
teleport/load listener, rather than only Alpha. No target, gallery, life, native
restart or rewind assertion was removed. Original failed receipts are retained.
Hagalaz versus Chef is still awaiting the user's answer; ordinary inherited kill
is provisional and this interaction is not qualified by the global fixture.
See [batch evidence](DAMAGE_REACTIONS_AND_REWARD_CHAIN_2026_10_31.md) for scope.
Earlier authored/not-run statements describe worker handoff, not current status.
