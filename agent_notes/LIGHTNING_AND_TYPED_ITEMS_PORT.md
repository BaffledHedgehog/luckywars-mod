# Lightning and typed dropped items - current contract

Status: QUALIFIED. Build and two native matches passed on `39220c8c9cb6f6b4727bf2d6b52a52c4d4b80e973e2a99d21a29424cad06a3c2`. Ready archive: `build/verification/skywars2-lightning-rules-39220c8c.jar`; not installed.

## User behavior

Chain Lightning includes allied players, allied mobs and the caster during initial ray acquisition, initial radius acquisition and later propagation. The ray naturally excludes the immediate caster at its origin. Repeated jumps remain allowed. Antimagic, role/service protection, match stage and lifecycle checks remain in force.

Every actual `minecraft:lightning_bolt` entity uses role-only targeting: friendship and caster identity do not prevent a strike. This covers native weather, unmanaged/cause-bearing lightning, managed spells, lightning eggs, Cultus and other owned native bolts. Service entities, spectators and inactive player roles retain protection. This exception does not broaden ordinary projectile or spell targeting.

Weather and unmanaged lightning retain vanilla damage type and native hurt cooldown. Managed lightning retains its electric damage type and Techno immunity. No lightning damage type is added to bypasses_cooldown, and no invulnerability timer is reset. Minecraft's native no_knockback damage tag already includes lightning_bolt; no new knockback resource patch is needed.

Every attributed bolt retains its actual direct source and authoritative kill owner. Direct ignition and block-fire ownership retain that attribution, and lightning-attributed fire can damage allies and self. Self damage never gives suicide kill credit. Ownerless weather remains ownerless.

Dropped ItemEntity stacks whose top-level minecraft:custom_data contains `type` are immune to all damage except minecraft:out_of_world. Presence is sufficient, including integer and string values; nested keys do not qualify. Protection is evaluated per damage event, with no polling and no persistent/global invulnerable flag. Explicit discard, normal despawn and pickup behavior remain unchanged.

## Implementation boundaries

ElectroNativeMixins centralizes actual native lightning targeting, source rewriting and ignition/block-fire attribution; the former KfcLightningMixin is deleted. Managed electric behavior and vanilla lightning behavior remain distinct while sharing friendship exceptions. Existing owner/service/stage checks remain where relevant. Chain acquisition and propagation use the same inclusive chain eligibility without broadening other Lightning Bolts modes.

Typed item protection belongs to ItemEntity's damage entry point. It does not rewrite stack metadata, resurrect removed entities or prevent scripted removal.

## Verification

New native suites: `lightning-rules`, `lightning-rules-chain`, `typed-items`. Runner: `tools/smoke_lightning_rules.py`.

The runner requests two native matches and existing suites `boltbolt`, `infbolt-egg`, `electro-gear`, `electric-damage`, `kfc-cultus` and `mobgun`, followed by stop probes and cleanup checks. All passed in both matches: 26 recorded suite results in 45.88 seconds, plus stop probes. Evidence: `build/verification/lightning-rules-final/`. Native fire immunity is preserved: Vex takes lightning damage but does not ignite. The existing Polymer chunk-packet context warning from synthetic player setup remains; no check failed.

Review coverage includes initial/retained/propagated chain targets; owner/allied player/allied mob and protected-role controls; weather/unmanaged/managed/egg/Cultus lightning; immediate damage, delayed burning, block fire and kill ownership; typed/untyped item damage including out_of_world, despawn and pickup boundaries.

Bounded source audit: [LIGHTNING_FRIENDLY_FIRE_REVIEW](LIGHTNING_FRIENDLY_FIRE_REVIEW.md). That audit describes pre-change blockers; this document is the current implementation contract. Existing historical user notes are preserved in project documentation.

Package CRC and all 2638 included source resources passed byte comparison; 1977 resources remain intentionally excluded. Fabric version expansion and bundled SGUI were checked, as was removal of the old KfcLightningMixin classes. The isolated server stopped normally and released its world lock. The main JAR remains SHA-256 `332415d6ffac9c5f7dfb02a07f395849b4aa68d31312b512fb5748ce3e47ffe4`; main server and RP/template were not modified. No graphical client test is claimed.
