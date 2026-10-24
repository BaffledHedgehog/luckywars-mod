# Corrupted dust glyph dependency

Source: read-only `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/corrupted_dust.json`
and `lbc/function/workingitems/corrupted_pentagram/*.mcfunction` beneath that data root.

Four equal-weight source entries retain their original names, lore, tooltip styles and dust models.
The parent integration generates their native loot entries. All four use `skywars2:corrupted_dust`,
with lowercase `custom_data.glyphkind` (`bafomet`, `lamashtu`, `belial`, `ahriman`).
`SimplePolymerItem` uses a panda spawn egg proxy and implements actual native block use.
One accepted placement consumes one in survival. The owner is the actual placer, fixing the
source's nearest-player ownership ambiguity. Native successful publication awards the original
`lbc:true_advancements/staffs/corruption` advancement.

The item display is stationary at the vanilla egg placement center. Each original model remains
distinct: `lbc:corrupted_pentagram`, `lbc:lamashtu`, `lbc:belial`, `lbc:ahriman`.
The complete authored matrix, fixed transform, zero shadow, and both 20-tick interpolation
durations are initialized before publication. The source rotates ten degrees per one-second pulse.
There is no age expiry. Native dimension Time Stop pauses its cadence. Antimagic deletes it.
Match epoch, owner eligibility, map dimension, unload, forget and stop remove stale actors.
Reloaded unowned displays are discarded by the narrow `loaded` hook.
Ownership is installed before native entity publication, including synchronous load callbacks;
failed publication rolls back its record and never consumes the dust.

Bafomet applies the source quantized vector to nonplayer mobs: existing velocity plus facing
unit, shared-axis recursive 3/4 attenuation when any axis exceeds 1.5. Unholy mobs inside1.5
are excluded. Technical roles remain protected. Pull displacement registers the placer with
the shared indirect-kill service for fall/void credit. Source contact chance20%, unsupported ground
or obstructed upper block terminates the glyph; its explosion grants100 ticks WitherV within1
to nonunholy eligible targets, retaining placer attribution. Author ruling (user):
> Да, сохранить

The explosion is WILD and includes the caster and allied players, preserving the source selection.

Lamashtu grants infinite WitherI within16 to nonunholy eligible targets. Friendly mobs remain
eligible; owner/allied players and protected roles remain safe. On source termination it clears
all Wither within16 from eligible entities, including owner/allied players, preserving source
cleanup breadth. Source particle colours, range, purr volume1/pitch0 and twenty particles remain.

Belial author ruling (user):
> да такая механика есть конечно и надо сохранить, если я верно понял про что ты

Belial removes all eligible nonplayer mob teams within16, including friendly mobs. In modes1/2
(SkyWars/LuckyWars) it also removes foreign nonallied player teams; modes3/4
(BedWars/LuckyBedWars) preserve player teams. The caster, allied players and technical roles
remain protected from team removal. Each eligible nonplayer mob has an independent5% chance
to receive0.001 native arrow damage by another random eligible living entity within30 of the
glyph, excluding the caster. Native revenge AI retains that actual forced attacker. A narrow
weak-victim/UUID-attacker symmetric pair map carries the original causal order into eventual
mob retaliation through the existing indirect-kill service. It never changes global mob ownership.
An explicit forced source retains the cause even for immediate lethal damage; rejected force
does not install a duel. Ordinary real later player attacks override the original cause. Forget,
unload/dead cleanup and match stop clear corresponding records.

Ahriman author ruling (user):
> Сохранить всё

Each active second independently samples six centers with integer offsets-25..25 on every axis.
Centers within2.5 of the glyph are skipped. Each other resident3x3x3 cube performs native destroy
with drops and air replacement, then four black dust particles(scale4), and4 native outside_border
damage within2. This damage is WILD, including caster/allied players, and credits the placer.
The shared terrain cause also owns support removal and ensuing hazards. Unloaded cubes are
skipped without loading chunks. The explosion permanently updates eight base attributes with
eight independent50% choices using the source floor/scale arithmetic: armor-1, toughness-1,
knockback resistance-1 (source get scale10/debit10), luck-1, max health-2, attack damage-1,
attack speed-.2 and movement speed-.02. No synthetic clamping or extra feedback is added.

`CorruptedGlyphChecks.run(GameController)` is a bounded main-thread fixture in an existing
match with active TestAlpha/TestBeta. Sixteen real loot-table rolls verify native dust decoding,
count, variant name/model and registry identity without requiring stochastic coverage of all four.
It exercises actual native item use for all four glyphs, one-unit
consumption, placer ownership, Bafomet pull/roles/no artificial expiry, Lamashtu infinite
Wither/friendly mobs/owner/role rules and broad cleanup, unsupported/obstructed termination,
native Time Stop, native antimagic and owner forget. Dependent fixtures cover all four source
game-mode team rules, real mob revenge identity, native lethal retaliation credit and later-player
precedence, WILD caster/allied-player Wither and cube damage, native block drops, permanent
attribute changes and protected roles. The cube fixture calls one actual cube helper against27
snapshotted blocks; it never runs unrestricted random erosion over the match map. It restores
both participants' state, all eight base attributes, block snapshots, custom/cow-kill stats,
all advancement progress and full profiles, and removes new item/XP entities. Its long-life boundary
uses manual service ticks; this does not claim elapsed-time lifecycle, rendered client pixels,
real disk unload/restart or peak-load testing. Parent owns compilation, suite integration,
resources and final server evidence. Qualified with the complete corruption family on build `58a78a13`: 34 recorded suite results in two actual matches, plus actual live glyph match-stop cancellation. Artifact/source/resource identity and protected user files verified. Installed stopped main with rollback `0faca9ac`; main not started. Evidence: `build/verification/corruption-final/`.
