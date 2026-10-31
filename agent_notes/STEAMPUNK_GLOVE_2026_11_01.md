# Steampunk glove native implementation — 1 November 2026

Added only `LbcSteampunkGlove.java`, `SteampunkGloveChecks.java` and this note.
Integration, registration, builds and native launch belong to the parent agent.
No runtime proof is claimed here.

Read-only source: original server `world/datapacksdisabled/LBC/data/lbc/function/`:
`items/on_sprint.mcfunction`, `players_sprint_end.mcfunction`,
`players_1s.mcfunction`, `players_1s_sprinted.mcfunction`,
`swrg_kit_integration/items/steampunk_sword.mcfunction`,
`steampunk_sword1.mcfunction`, `steampunk_sword_1.mcfunction`.

`sprint_timer` already belongs to native Moisei boots. Integrate glove tick AFTER
Moisei tick; it must never increment the shared clock independently. The glove
owns previously unused `sprint_cooldown`, native `sprinted` tag and source fixed
movement/damage IDs. Existing Vanity native score/tag/attribute capture suffices;
there is no private state/checkpoint adapter.

Ready selected-hand sprint restarts timer1, then adds movement0.05*timer and
damage1.25*timer through200. Last modifiers remain above200. Every refresh writes
cooldown5; only stopped shared global one-second callbacks decrement it. A stop
removes both modifiers/tag without refund. Fresh sprint during cooldown gets no
new bonus. Switching away retains prior bonus until stop, as source does. Source
sneak removes the tag and prevents further updates without removing prior bonus.
User confirmed TECH must cancel the glove. TECH immediately removes both owned
modifiers and the sprinted tag, including after switching away; leaving the field
cannot revive that canceled ramp during its existing cooldown. MAG and artifact
magic suppression do not gate it. No configurable policy remains.
Participant/role/death cleanup resets state; action blocking skips new
updates; ordinary frozen dimensions/global freeze pause both ramp and recharge.

Public integration: `tick`, `tickPlayer`, `clear` (purify, retain cooldown),
`reset` (player lifecycle), `stop`. Constructor accepts GameController; checks
expect `game.items().steampunkGlove()` and leaf `lbc:ported/steampunk_sword`.

Finite checks use existing native snapshot/connection/air fixture. They observe
every actual tick of full200 ramp,25 retained-ceiling ticks and five stopped
global seconds, then test native TECH/MAG markers, artifact tag, action freeze,
sneak, switch-away, ready/cooldown selection, Time Stop, spectator role and
clear/stop. Both native modifier contents and actual native attribute values are
checked, with scoped fixture restoration. They do not manually tick worlds.
