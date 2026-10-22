# Five cage special drops — 22 October 2026

Status: build and two isolated native match cycles **QUALIFIED** on09a1cf09.
The checked JAR is ready locally; the main server remains unchanged.

Shared-helper update: [HOLE_DROPS_PORT.md](HOLE_DROPS_PORT.md) extracts native fill
into `LbcDropBlocks`, skips unused strict-fill allocations/old-state reads and
adds the native per-cuboid `MAX_BLOCK_MODIFICATIONS` gate. Two cage regression
matches passed on2478ebdc alongside holes and the earlier drop families. The
original cage qualification below remains historical evidence for its batch.

## Author decisions (user)

> близжайшего игрока (не спектатора) любого (даже себя или союзника)

The target is the nearest eligible nonspectator player in the token's dimension,
including the thrower and allies. There is no radius cap or Creative/developer
exception. Observer and service-role exclusions still apply.

> Да, ловушки опасны для всех

Cage hazards are WILD: their damage can harm the author and allies. Causal kill
credit remains attached to the actual thrower. This exception does not authorize
damage to spectators or protected service objects.

> Да, очищать только вверх

Anvil, fireball and potion cages clear the center column from the selected
player's floored feet upward through the dimension's inclusive maximum build Y.
The source's absolute255/319 fills are deliberately replaced: they could clear
downward and erase the floor when activated above those endpoints. No blocks
below the player's feet are cleared by the shaft.

## Source and native tokens

[Source audit](CAGE_DROPS_SOURCE.md) records the original LBC function paths,
ordinary and alternative loot definitions, exact payloads and existing RP keys.
This port exposes the five ordinary decorated tokens below. It does not qualify
the enclosing Lucky Block random pools or invent an empty-cage token.

| Item ID | Public loot ID | Custom-data flag | Name key |
|---|---|---|---|
| `skywars2:cage_drop_anvil` | `lbc:ported/cage_drop_anvil` | `lookupanvil:1` | `trap_anvil` |
| `skywars2:cage_drop_fireball` | `lbc:ported/cage_drop_fireball` | `lookupfireball:1` | `trap_fireball` |
| `skywars2:cage_drop_potions` | `lbc:ported/cage_drop_potions` | `lookuppotions:1` | `trap_potion` |
| `skywars2:cage_drop_lava` | `lbc:ported/cage_drop_lava` | `lookuplava:1` | `trap_lava` |
| `skywars2:cage_drop_obsidian` | `lbc:ported/cage_drop_obsidian` | `obsidiantrap:1` | `trap_obsidian` |

Each token also has `drop:1`; each public table emits one native registered item.
Polymer uses the source repeating-command-block carrier, model `lbc:drop_trap`,
tooltip `lbc:tier_ff`, yellow nonitalic translated name and one white nonitalic
`trap_desc` lore line. `trap_potion` is singular; `trap_potions` belongs to another
item. Existing translations and visual assets are reused unchanged. The original
resource pack and `resourcepack_template` are unchanged by this port.

## Shared dispatch and geometry

`LbcSpecialDrops` owns admission and the single stack debit. Every matching flag
executes in source order: ordinary creepers, old creepers; bastion, end-city,
treasure, village and bonus chests; thirteen-TNT drop; then anvil, fireball,
potions, lava and obsidian cages. A token with several flags executes the combined
activation and loses one unit, at most once per server tick. No eligible player
means no cage is placed, while the source debit still occurs. Held items and
temporary fake `/give` visuals do not activate.

The cage target is selected once before the combined debit. All matching cage
footprints and their native-neighbour border must have resident chunks before
dispatch; failure defers the activation without forcing chunk loads or consuming
the token. Cages use the selected player's position, not the token's position.
There is no player teleport or recentering.

The four bar cages fill a 3×3 stone-brick floor at feetY−1 and a solid 3×4×3 iron-bar
volume at feetY through feetY+3. Block coordinates floor the player's position.
Anvil, fireball and potion cages then clear the center column upward as approved
above, leaving9 floor bricks and32 bars for an ordinary valid placement. Their
native entity spawn X/Z is floored then centered at+.5; spawn Y preserves the
selected player's fractional feet Y.

| Cage | Exact native payload |
|---|---|
| Anvil | Two `minecraft:falling_block` anvils at Y+40 and Y+42; `Time:1`, `DropItem:1b`, `HurtEntities:1b`, `FallHurtMax:35`, `FallHurtAmount:2.0f`, zero initial Motion. |
| Fireball | One `minecraft:fireball` at Y+70; `ExplosionPower:4`, Motion `(0,-0.05,0)`. The legacy filename `tntcage` does not mean a TNT payload. No custom constructor acceleration is injected. |
| Potions | Exactly40 real `minecraft:splash_potion` actors created immediately, at Y+50,+100,…,+2000. Each carries one splash potion with custom color7543569 and one instant-damage effect of duration20. |
| Lava | Clear the center at feetY through feetY+2; replace the center top bar at feetY+3 with one native lava source. No tall shaft or entity payload. |

Potion amplifier0 appears at offsets+50 through+450 and+1050 through+1450:
18 potions. Amplifier1 appears at+500 through+1000 and+1500 through+2000:
22 potions. The source's simultaneous spawn and high altitude are retained;
there is no timed emission loop or invented lifetime to make all40 land.

Obsidian fills3×4×3 at offsets−1..+1 X/Z and−1..+2 Y using strict obsidian,
replaces the two center blocks at Y0/Y1 with water, and places four strict glass
blocks at the horizontal cardinal sides of Y1. Nominal final counts are
30 obsidian,4 glass and2 water. It has no title, shaft or spawned actors.
The other four cages send a single-space title followed by translated `look_up`
subtitle, preserving native title timings.

Block writes mirror native `/fill replace`: `BlockInput.place` uses flags258
for regular writes and818 for strict writes. Regular writes defer
`updateNeighboursOnBlockSet` until that fill finishes; strict writes omit those
updates and retain native suppression flags. Native block replacement controls
container/block-entity side effects and inventory ejection where applicable;
there is no separate manual container clear, inventory copy or custom loot roll.
Strict obsidian/glass replacement retains strict native side effects.
In particular it removes a replaced container without ejecting its inventory;
the chest-reward drops' explicit inventory-ejection contract is separate.
Falling-block payloads use26.3's native block-state codec (`"minecraft:anvil"`),
not the legacy `{Name:...}` object that silently falls back to sand.

## Causal ownership, protections and lifecycle

The shared dispatcher captures the real `ItemEntity` thrower before activation;
the nearest trapped player and pickup target never become invented authors.
The native merge guard compares causes only during matching trap-token merge
attempts. Primary anvils, fireballs and potions retain the cause and match epoch.
Current native projectile ownership takes precedence after reflection or other
owner changes; reflected TNT/falling-block ownership retains its existing
precedence. Ownerless tokens acquire no invented player author.

Causal integration covers falling-anvil harm, native potion instant harm,
fireball direct damage/explosion, lava and propagated fire, placed-water drowning,
support removal/falls and new suffocating trap blocks. Native TNT primed by a cage
explosion inherits its cause and epoch before insertion; further chained TNT
inherits them as well. WILD permission follows these causal consequences so
allied/self damage remains permitted while observers and service objects remain
protected. Existing native/global protection and time-stop mechanisms still
apply. The observed runtime coverage and remaining limits are listed below.

Managed actors retain native gravity, collisions, explosion behavior, block
landing, potion mechanics and native despawn rules. Attribution adds no custom
TTL, persistence flag, spawn cap or owner-lifetime requirement. Native projectile
`getOwner` is not overridden with a retained removed-author fallback that would
change native flight/despawn behavior; causal damage hooks retain attribution.

During same-match native chunk unload, loaded tracking is released and actor-UUID
cause references are retained for reattachment on load. No forced chunk load is
introduced. Current-epoch loading restores tracking; stale epochs are quietly
discarded. Time stop freezes managed actors through the shared mechanism. Match
stop quietly discards loaded actors and clears retained causes without causing
explosions or death rewards. Block geometry and native environmental consequences
are not assigned a new restoration timer.

## Runtime qualification

Build and two actual isolated mode1 fixture matches passed on SHA256
09a1cf0967b2bb84d789d1a8da06a6d0e008232cc792dae5147d336ab93dd479.
The runner completed24 suite results in50.07 seconds, including all four drop
families, actual match-stop probes and cleanup. Cage checks cover complete native
give/ported prototypes; all five geometries; exact immediate2-anvil/1-fireball/
40-potion payloads and their NBT; nearest self/allied selection and spectator
exclusion; five flags/43 actors with one shared debit; no-target debit; held/fake
exclusion; residency without chunk admission; per-unit ticks; stopped time;
stale epochs; and quiet cleanup. The Y270 shaft retains all9 floor blocks and
lower terrain while clearing above. Strict container replacement removes its
block entity with no inventory ejection, matching native strict-fill flags.

Real native anvil fall damage, a moving native fireball collision and native
splash-potion harm damage an allied player with the correct thrower source;
splash harm also affects its own author. A real disposable cow in the obsidian
water takes lethal native DROWN damage and credits the thrower without previous
combat credit. This tests causal drowning without waiting for air depletion.
The existing Creeper, TNT and chest regressions also pass. Initial failed native
logs are retained: the first exposed the legacy block-state codec incompatibility;
the second led to fixing the test's same-tick spectator-physics/collision setup.

Ready archive: build/verification/skywars2-cage-drops-09a1cf09.jar.
Evidence: build/verification/cage-drops-final/ (build/native/server logs,
results.json and qualification.json). All150 Java sources match the sources JAR;
2657 processed resource files match the release. Fabric metadata/bundled jars
were checked separately after Loom transformation; CRC passed. The isolated
server stopped normally, configuration and its previous mod JAR were restored,
and runner/world locks were released. Main remains69de8601, not installed;
original RP and resourcepack_template are unchanged.

Actual chunk-unload/reload, native saved-state restoration, disconnected/dead
author scenarios and offline/restart persistence are not qualified by the
presence of UUID cause maps or native serialization code. Real-client visuals,
natural high-altitude potion arrival and normal native despawn also require
observed evidence. New cage chain-TNT ownership and protected-role paths are
implemented but not independently exercised as new cage fixtures. There is no
measured peak-capacity/MSPT claim. The preexisting Polymer chunk-packet-context
diagnostic on fixture bot join remains.

Next: audit the remaining terrain traps (`hole`/`hole_old`) before publishing
their individual leaves. Keep the full Lucky Block pools and unfinished modes
gated until their own contracts are implemented and qualified.
