# Entity limits audit (native 26.3)

Read-only review of local minecraft-merged-d6f9bb6878-26.3-sources.jar; this note covers movement validation, attribute value sanitation, and runtime/NBT entity velocity only. Packet codecs and entity-motion packets are intentionally outside this audit.

## Player and vehicle movement validation

Source: net/minecraft/server/network/ServerGamePacketListenerImpl.java.

- handlePlayerPositionChange(...) performs the player “moved too quickly” rejection. It compares squared requested displacement from firstGood* against current velocity squared and a per-packet threshold: 100 normally, 300 while fall-flying, multiplied by the packet count. The branch runs only when the player's level tick manager runs normally.
- This speed check is gated by shouldCheckPlayerMovement(isFallFlying). It returns false for the singleplayer owner, dimension changes, GameRules.PLAYER_MOVEMENT_CHECK false, or fall-flying when GameRules.ELYTRA_MOVEMENT_CHECK is false. Both rules exist in net/minecraft/world/level/gamerules/GameRules.java, identifiers player_movement_check and elytra_movement_check, default true. There is no DISABLE_PLAYER_MOVEMENT_CHECK symbol in this version.
- handleMoveVehicle(...) has its own independent “vehicle moved too quickly” check: squared displacement minus current vehicle velocity squared > 100, except singleplayer owner. It has no corresponding vehicle movement gamerule in GameRules; disabling player movement check does not disable this vehicle rejection.
- Separate “moved wrongly” checks remain in both handlers. They validate post-collision mismatch (movedDist > 0.0625) and may snap/teleport the entity back if collision validation fails. The player variant excludes several states (dimension change, sleeping, creative, spectator, post-impulse grace), but the vehicle variant has no movement gamerule. These are collision/reconciliation checks, not the “moved too quickly” speed threshold. Hiding/removing the warning alone does not remove either rejection path.

Implication: player speed validation is configurable with the two vanilla gamerules; vehicle speed validation requires a server-side code/mixin change. Fully disabling position correction also means handling the separate collision-based “moved wrongly” paths; a single gamerule is insufficient.

## Attribute hard caps and health

Sources: net/minecraft/world/entity/ai/attributes/RangedAttribute.java, AttributeInstance.java, Attributes.java, net/minecraft/world/entity/LivingEntity.java.

- Global effective-value clamp: RangedAttribute.sanitizeValue(value) returns the attribute minimum for NaN; otherwise Mth.clamp(value, minValue, maxValue). AttributeInstance.calculateValue() applies base value and modifiers, then calls sanitizeValue, so raising the base or modifier alone cannot exceed a registered attribute's max.
- The registered limits differ per attribute (examples from Attributes): max health is [1, 1024], movement speed [0, 1024], armor [0, 30], armor toughness [0, 20], scale [0.0625, 16], movement efficiency [0, 1], and gravity [-1, 1]. RangedAttribute stores both bounds as private final fields. A blanket no-cap behavior therefore needs to change sanitation semantics (while deciding what NaN should do), not merely set larger values for selected attributes.
- Extra health clamp is LivingEntity.setHealth(health): writes Mth.clamp(health, 0, getMaxHealth()). getMaxHealth() reads effective Attributes.MAX_HEALTH. onAttributeUpdated(MAX_HEALTH) also lowers current health when the effective max-health value decreases. ServerPlayer does not define a separate setHealth override in the native source; it inherits this clamp. To permit current health above the attribute maximum, both the attribute sanitation ceiling and this health setter clamp must be addressed; retain a deliberate lower bound if zero/negative health is not a desired behavior.

## Runtime and saved entity velocity

Source: net/minecraft/world/entity/Entity.java.

- Runtime setDeltaMovement(Vec3) has no magnitude ceiling: it accepts the vector unchanged iff Vec3.isFinite(). The double overload delegates to it. addDeltaMovement(Vec3) also checks only finiteness before adding. Thus arbitrary finite velocity can be set in server code; game physics (gravity, drag, collision response, fluid effects, and vehicle-specific speed behavior) may still change it each tick, but those are motion mechanics, not a universal hard cap in these setters.
- Entity load hard cap: Entity.load(ValueInput) reads the Motion vector and, independently per axis, replaces any component with absolute value greater than 10.0 by 0.0 before calling setDeltaMovement. This is discard-to-zero, not clamp-to-10, and it prevents restored velocity components above 10 even though runtime setter accepts larger finite values.
- Scope distinction: lifting the NBT guard enables oversized saved velocity to load. It does not change gravity/drag, collision, or vehicle movement behavior, and it does not affect the network encodings reviewed by the separate packet audit.

References: merged 26.3 sources listed above; no project Java/resources were changed and no build/server was run.

