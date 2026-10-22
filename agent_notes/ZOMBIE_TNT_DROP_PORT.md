# Zombie TNT special drop — 22 October 2026

Status: build and two isolated native match cycles **QUALIFIED** on `9929b971`.
The checked JAR is ready locally. The main server, original resource pack and
resource-pack template remain unchanged.

## Author's decisions (user)

On the source's zero-damage melee attacks:

> Полностью отключить ближние удары

On whether the trap is dangerous to its thrower and allied players:

> Да, опасна для всех

These answers override the original native zero-damage melee behavior and
ordinary allied-player protection. Carriers retain ordinary hostile movement
and targeting but cannot execute melee hits, knockback or melee ignition.
Native TNT remains WILD, with observer and protected service-role exclusions.

## Item and source payload

[Source audit](ZOMBIE_TNT_DROP_SOURCE.md) records exact function/loot/RP evidence.
The public leaf `lbc:ported/zombie_tnt_drop` emits one registered Polymer item
`skywars2:zombie_tnt_drop`, with `{zombietnt:1,drop:1}`, repeating-command-block
fallback, `lbc:drop_trap` model, `lbc:tier_ff` tooltip, yellow nonitalic
`trap_tnt_zombies` name and one white nonitalic `trap_desc` lore row. Existing
assets are reused; the zero-weight bare IMBA placeholder is not newly exposed.

At the actual fractional token position, four native baby zombies appear at
offsets `(+1,0,0)`, `(0,0,+1)`, `(-1,0,0)`, `(0,0,-1)`. Token yaw and pitch do
not rotate these offsets or transfer to the carriers. Each zombie carries one
actual native TNT passenger with fuse 200 ticks. The entity-type TNT constructor
retains summoned TNT's zero initial motion, avoiding the coordinate constructor's
priming impulse. Native baby passenger attachments establish the initial
position before insertion; there is no per-tick teleport or custom riding loop.

The source base attack attribute -10 remains, with no attribute-limit exception.
The explicit melee guard prevents even native zero-damage hit side effects, as
requested. Native gravity, AI, sunlight, drowning conversion, despawn, ordinary
death loot and XP remain. No randomized spawn equipment, persistence override,
custom reward, owner lifetime or extra explosion on carrier death is introduced.
TNT detached by native death/ejection keeps its fuse and can still explode.

## Admission, ownership and native hooks

`LbcSpecialDrops` remains the single token dispatcher and debit owner. This
payload is admitted after corrosion and before the later pit/cage branches.
The complete four-position footprint must be resident before any pair is emitted.
There is no synchronous chunk load. Inactive/ending matches, wrong arena,
fake-age5999 visuals, stale epochs and stopped time cannot activate the token.
Held items do not activate. Matching causal tokens retain separate throwers
only at native merge attempts, without a world scan.

`LbcZombieTntDrops` tracks carriers and mounted/detached TNT with the actual
thrower, independently of pickup target or nearest player. Native TNT ownership
and serialization, reflection precedence, chain TNT and indirect terrain
consequences reuse established hooks. Native conversion stamps the replacement
before entity-load handling and preserves its native attributes/passenger
transfer. It does not turn hostile carriers into friendly summons.

Target acquisition, direct damage and native explosion target selection exclude
spectators and protected server NPCs/roles. WILD permission bypasses allied-player
checks for both native damage and knockback. Shared TNT configuration protection
still applies once, alongside ordinary native armor/effects. Reflection changes
the effective cause before chain explosions or saves.

Time stop freezes the native carrier, passenger movement and fuse together.
Unloaded actor UUIDs retain same-match causes in memory. Owner death/disconnect
does not remove already emitted actors. Match stop/epoch replacement discards
living carriers and all tracked TNT quietly, including detached charges; it does
not kill them for loot or manufacture explosions. Real restart persistence of
carrier attribution requires separate qualification.

## Qualification

The selected no-melee policy prevents native carrier hits and ignition from
executing; the preliminary zero-hit displacement/ignition hooks were removed.
The native initial-rotation check found that `LivingEntity`'s constructor
randomizes yaw, whereas `/summon` loading absent Rotation resets it to zero.
Carriers now explicitly reset yaw, pitch, head and body before insertion.

`tools/smoke_zombie_tnt_drop.py` passed two actual mode-1 fixture matches:
42 suite results, 62.26 seconds including boot, loading and previous corrosion,
pit, cage, Creeper, TNT and chest regressions. The final run has no failed suites.
The first failed candidate and the later pre-final passing candidate are retained
as evidence; the final candidate additionally preserves WILD from the direct
trap when opening the explosion terrain write scope. The same scope correction
also preserves the existing cage trap's WILD permission.

Qualified native fixtures cover complete give/loot components; exact fractional
cardinal coordinates; baby/default motion/orientation; actual passenger
attachment and serialized passenger/fuse; ordinary gravity and mounted fuse
progress over server ticks; time-stop position/fuse/motion pause and resume;
fully disabled melee; source/reflected TNT kill credit and native mob-kill
statistics; native TNT save/load and real chained TNT; role protection; allied
and self/reflected damage; all six TNT-protection levels applied once; actual
Drowned conversion retaining passenger and cause; ordinary carrier death loot
and release of live TNT; dropped stack debit; fake/epoch/unloaded/residency
admission; departed retained mob reference; and quiet actual match-stop cleanup.

The runner independently releases its own locks and records cleanup failures,
while restoring backups only after verifying the server is stopped. A mocked
early-refusal test preserved the original error, left dummy server data untouched,
released both locks and wrote failure results without accessing a running server.

Ready artifact: `build/verification/skywars2-zombie-tnt-drop-9929b971.jar`.
SHA-256: `9929b9710f3f0c86940d9112e0667b18155a31b3815070c6b4be41207f70ac94`.
Evidence: `build/verification/zombie-tnt-drop-final/` (`build.log`, `native.log`,
`server.log`, `results.json`, `qualification.json`, and earlier candidate logs).
All 161 Java files match the source JAR; 2662 processed resource files match the
release. CRC, transformed Fabric metadata and bundled JAR entries passed.
The isolated server stopped normally, released locks and restored its previous
mod/configuration/profile/scoreboard backups. Main JAR remains `69de8601`; this
artifact is ready, not installed. Resource pack and template are unchanged.

Limits: native fake-player fixtures do not qualify real-client graphics or
throw packets. TNT serialization and a retained removed-mob reference do not
prove actual player-disconnect, chunk-unload or restart persistence. Native
final-fuse detonation is checked, rather than a complete 200-tick natural journey.
No worst-case simultaneous mob/explosion capacity claim is made. WILD terrain
scope is reviewed and regression-tested, without a dedicated native
TNT-generated environmental-hazard fixture. The preexisting Polymer fake-player
chunk-packet-context diagnostic remains.

Next bounded work: remaining special drops, starting with sethome/falling-block
source contracts. Whole Lucky Block pools and unfinished modes remain gated.
