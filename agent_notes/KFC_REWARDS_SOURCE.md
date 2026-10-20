# KFC reward abilities — 20 October 2026

Read-only source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
The source functions and resource pack were not edited.

## Fake victory button

`function/projectiles/eggs.mcfunction:31` detects the thrown snowball item
custom data `win_button:1`; it calls `items/win/win`, immediately broadcasting
the stored `title.player_win` component and sending `reward.win` to the nearest
non-spectator. These are cosmetic messages: no match completion or reward
accounting occurs. Four markers are added, and `win_2` is scheduled after
20 ticks. `win_2` uses `spreadplayers 0 0 0 192 false`, spawns fireworks,
calls the existing end fireworks randomization and schedules itself again.
`end_resets.mcfunction:117` cancels this recurring schedule. Reusing the button
adds four markers and replaces the global 20-tick timer.

The owner's explicit clarification says the fireworks continue after the
user dies or leaves and stop at match end. `LbcKfcRewards` therefore keeps its
cosmetic marker/timer state until stop/epoch change, separate from owner
cleanup. It reuses `OriginalGuiDesign.sourceMessage` and
`MatchEvents.spawnVictoryFirework`; its Polymer `SnowballItem` superclass
retains native throwing physics, sound, statistics and item consumption.
The native alias `lbc:win_button` issues `skywars2:win_button`, preserving
model `lbc:win_button`, name key `win_button`, yellow nonitalic name,
tooltip `lbc:tier_x` and `{win_button:1,gadget:1}`.

## Cultus and six instant items

`workingitems/penis_cultus/use` emits ten soul particles at cast eyes +2,
spawns one random variant at every other non-spectator player's feet +5,
grants `true_advancements/summoner/penis_cultus`, subtracts the configured
250 mana and invokes the existing summon trinket hook. Main/offhand
dispatch is in `spell1_default`/`spell1_default_left`.

`spawn_random_penis` chooses uniformly from six variants. Each summon is
a silent native bat tagged `no_penis`, with permanent hidden invisibility
and wither. It carries a native item passenger with Age -32768, Health
10000, pickup delay 3, invulnerability and tag `penis`. The item is a
repeating command block with model `lbc:penis_1` … `lbc:penis_6`, name
key `penis`, and custom data `instantitem:1,penis_bomb:1,penis_num:N`;
variant 2 also has `govnoq:1`. No max-stack-size/tooltip override is present
in this particular summon source; unrelated spell/trinket variants have
extra metadata and were not substituted.

`items_tick_tag` derives `working` and `penis_bomb` entity tags from custom
data. `items_tick_act` → `workingitems/penis_mob` selects the nearest
non-player mob without `no_penis` in radius 1.2. `other/penises/select`
then applies the variant to the exact chosen position and consumes one
item. Player inventory instant processing uses `other/instants/select`
→ `select_old` → `other/penises/player_select`; it clears one matching
item for each encountered variant.

All six `other/penises/*` effect branches were read:

| Variant | Native result (amplifiers are zero-based) | Sound |
| --- | --- | --- |
| 1 | 6 arrow damage; cloud 10 at +1, spread 2, speed .1 | `lbcsounds.suction` |
| 2 | hidden slowness 8s amp5, weakness 4s amp5, nausea 6s amp5; 4 arrow damage; dirt block particles 100 | `lbcsounds.dick_1` |
| 3 | native TNT at victim, fuse 2 ticks | `lbcsounds.spank` |
| 4 | hidden blindness 8s amp1, levitation 1s amp15; native lightning; 100 pale flash particles | `lbcsounds.woo` |
| 5 | hidden slowness 1s amp127; 10 arrow damage; local backward 5 then world upward 5 teleport; redstone block particles 100 | `lbcsounds.shityeah` |
| 6 | .1 arrow damage to the **same victim**, repeated once for every loaded entity without `spectator` tag; white wool particles 100 | `lbcsounds.weewee` |

Every branch broadcasts the source `love_*_penis` translation with its
source color and selected player's display name, only for player victims.
One inherited variant-3 function/key has an offensive legacy identifier;
the translation key is preserved without introducing replacement prose.

The owner explicitly requested preserving variant 6's dependence on entity
count. `LbcKfcRewards` captures that count once on activation and queues
separate .1 native hits through the existing budgeted `LbcStonekivi.Work`
queue. Per-hit armor calculations are preserved; hits are not aggregated.
`skywars2:cultus_arrow` is arrow-style damage with its own
`bypasses_cooldown`/`is_projectile` tags. Vanilla arrow is unchanged.

Native `CultBat`/`CultItem` subclasses retain the vanilla entity IDs and
physics. Item contact overrides native `playerTouch` without a world or
inventory scan; a full inventory still prevents pickup. Only tracked item
neighbourhoods are queried for non-player mob contact. Source particles,
sounds, numeric effects, item metadata and weighted random choice are
preserved. The normal shared target filter protects teammates, technical
NPCs and observers. Bats, items, TNT, lightning, status and displacement
effects retain the caster through the existing attribution paths. Holy
lightning uses the common native bolt lifecycle/fire helper and a small
`KfcLightningMixin` to preserve vanilla lightning damage while attributing
it and filtering targets. It does not become an electric shock ability.

The cultus alias issues `skywars2:penis_cultus`; `tools/import_kfc.py`
reproducibly remaps both reward aliases while retaining source metadata.
Its model/name key remain `lbc:penis_cultus`/`penis_cultus`, with the
original four lore lines, tooltip `lbc:tier_a` and custom data flags.

## Scope and verification boundary

The general legacy instant-item family includes inventory-retained items
and interactions with skill 2. In particular `select2` lacks the penis
handler, so the original skill-2 inventory route can retain a bomb without
activating it. Skill 2 is Researcher / «Исследователь», `lbcskill.2`,
source GUI `lbc:gui/page/10000/2`; its lore describes recognizing mystical
artifacts, sword fragments, grimoire mana prices and Infinity, and an Imba
notification beam. It does not describe penis-bomb immunity. `select2`
handles selected-hand infinity/wires/greedstone only; there is no bomb
branch even for its selected hand. The owner answered on 20 October 2026
(user): fix the missing handling. Researcher therefore has no bomb immunity;
native contact activation covers all six cultus outputs for that skill too.
The separate generic instant-item family remains outside this port.

Validation performed here: inspected native 26.3 source jar signatures,
ran the isolated resource importer and its existing metadata/RP checks;
compared both reward tables with migrated source, allowing only native
item ID changes; checked RU/EN variant translation keys and all eight
existing reward/bomb RP item/model definitions read-only.
No build or server was run by this task; runtime tests belong to the root
task and its KFC checks agent.
