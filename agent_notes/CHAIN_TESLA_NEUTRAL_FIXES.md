# Chain effects, Tesla conduction and Neutral alternating — current batch

## Source and requested distinction

Read-only source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Two different authored chain mechanisms must remain distinct:

- Academy Thunder uses `lbcr/function/raycast_lightning_mode_chain.mcfunction`
  and `raycast_lightnings_chan.mcfunction`: initial native lightning, radius3
  `lightningist` tag, then `lbc/function/tick2smobs.mcfunction` and
  `other/lightning.mcfunction` every40 active ticks. A strike transfers the
  status to neighbors at0.001..16 and creates native `lightning_bolt` entities.
  LbcBolts already used WILD targeting for all chain acquisitions/propagation;
  caster/allied identities are included. Role, Greed and magic guards remain.
- Generic Raid Omen124 converts into `effect_chain_lightning`, displayed with
  green E156. Sources: `projectiles/arrows2.mcfunction`,
  `other/attributes/pills/raid_omen_124.mcfunction`, `effects/tick.mcfunction`,
  `effects/effect_chain_lightning.mcfunction`, `other/imba/chain_lightning*`.
  One counter is spent each active tick. This branch uses16 native ARROW damage
  and three upward ten-tick electric arcs per contact, with next targets within100.
  It does not turn this effect into a native lightning-bolt spell.

The user requests WILD propagation for both. The generic legacy owner/team
`nodmg` filters are removed. Its malformed multi-target `facing entity ... limit=10`
recursion becomes a finite nearest-next walk, at most ten fresh candidates per
visited vertex, with an identity visited set per activation. Connected chains
can extend beyond the original100 radius, as the source recurses from each hit.
The emitter is the initial visited vertex to avoid a zero-length self ray;
the caster remains eligible when the status is carried by another entity.
Loaded eligible mobs/players only; no chunk loads or global entity markers.

LbcChainLightningStatus owns FIFO contributions and original captured causes,
the native scoreboard counter and a persistent same-epoch attachment. Actual
Raid124 ingestion uses the effect mixin before ordinary projectile friendship
filtering, preserving the actual projectile owner. Mana also adopts loaded
Raid124 and positive external player counters, supplies E156, and shares cleanse.
Existing counters pause under Greed/Time Stop; new incoming Greed effects are
rejected. Protected roles, skill19 immunity, native effect admission, owner leave,
entity unload/reload, match stop and epoch cleanup are integrated. Native ARROW
hurt cooldown is retained; no timer reset or new bypass tag is added.
The upward arcs reuse the existing Electro engine and captured ownership.

## Thunder Reforge particles

The source does contain a ray particle. Exact file:
`lbcr/function/raycast_lightning_mode_reforge.mcfunction`, line2:
`dust{color:[0.0,0.765,1.0],scale:0.3f}`, count1, zero spread, normal visibility,
with0.35 step. The previous native END_ROD ray is replaced by cyan dust0x00C3FF,
scale0.3 and zero spread. Default/chain Thunder particles are preserved.

## Academy Neutral

LbcNeutral adds final selector mode6, `mode_alternating` with the Russian fallback
`Чередующийся`. The existing template EN/RU translations are reused. Every paid
cast uses existing modes1..5 sequentially and wraps; stack-local
`alternating_next` advances after casting. Free Shift selection, failed mana,
Time Stop and antimagic never advance it. Each hand retains its own cursor and
normal1200 mana cost. Ordinary random casts/trial and all native projectile
payloads, physics, ownership and lifecycle are preserved. Sources remain the
Neutral contract and original presets; no source pack or language edits.

## Academy Tesla instant

Original source `lbcr/function/raycast_preloop10cd_upgraded_mode_instant*`
conducts only on water and terminates on non-air. The user explicitly changes
this: instant rays now share `minecraft:electroprovod` with ordinary arcs,
continue through tagged conductors, and call existing teslaConduction once per
sample segment (up to eight quarter-block samples). Native damage1, accepted
hit accounting, original cause, WILD conductor damage, technical guards and
electric damage policy come from that common code. Nonconducting solids stop
the beam. Tesla chain homing/direct allied-player and caster protection remains
unchanged; it is separate from WILD conductivity and Raid124 electricity.

## Verification boundaries

NeutralWandChecks adds full mode6 wrap, exact native types/counts, independent
hands/costs and rejected admission cursor checks. TeslaChecks adds actual Instant
work over iron/water/stone, source WILD common conduction and rejected damage.
ChainLightningFixChecks is asynchronous: natural controller ticks verify Thunder
mob -> caster/allied player -> native bolt at caster, then actual Raid124 intake,
Greed/Time Stop pauses, native16 ARROW attempts, player E156 HUD, actual tipped-arrow
intake and cleanse. Existing lightning-rules-chain covers repeated Thunder visits.

Root integration: register ChainLightningFixChecks.run/tick; add Raid124 mixin
ingress and LbcWands.chainStatusElectro using the common native arc engine.
Those shared-file changes are owned by the root. No build/server/git operation
was performed by this agent. Native suites were prepared and statically reviewed;
no runtime PASS, client particles/pixels or disk-restart qualification is claimed.

The asynchronous chain fixture now requires clean participant attribution maps
and empty Thunder/Raid124 baselines before mutation, and rejects unrelated
eligible recipients in its isolated chain region. Both participants snapshot
all profile fields, advancements, native combat-credit fields, CombatTracker
entries/flags, food internals, custom statistics and ordinary player state,
following PlayerRuleChecks.Saved. The fixture stabilizes positions/velocity
during the natural Thunder observation and disables both profiles' passives.
Cleanup removes fixture attribution before restoring native combat state,
retains preexisting entities/guards, and retires only ElectroTraces created
after the baseline without ticking or stopping the shared electric service.

Source identity follow-up: `other/imba/chain_lightning_hit_at.mcfunction` uses
`damage @s 16 arrow by @a[tag=nodmg,limit=1]`, without a `from` carrier argument.
Cached26.3 DamageCommand constructs `new DamageSource(holder, caster)` for this
form; that constructor sets both direct and causing entity to the caster.
The status carrier supplies geometry only. This matters for managed LowVex,
ants and other creatures: using the carrier as direct damage entity would
invoke its unrelated friendship rules before the captured WILD cause.

ChainLightningFixChecks retains the natural eighty-tick Thunder observation and
adds one explicitly bounded service pulse: an actual ally-thrown old Vex drop
creates five managed LowVex; four extras are discarded before damage, and the
remaining carrier receives native Raid124 from the allied spellcaster. Its own
attack policy demonstrably rejects that caster, while the status must damage
both players and expose native ARROW direct/cause identity equal to the actual
spellcaster. The boundary requires no unrelated bolt work before its manual
tick. All resulting new traces/entities are covered by the existing cleanup.

Final root qualification: build `b4cdfa8d`,148 recorded results across two actual
mode1 matches and actual stop cleanup. See [combined review](GAMEPLAY_REVIEW_2026_10_25.md)
for the tested artifact, deployment state and verification boundaries.
