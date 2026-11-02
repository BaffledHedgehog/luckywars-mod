# Movement/combat feedback ? 2 November 2026

## Scope and freeze

Agent owns feedback2/3/7/8/11/17/18/21/22. Prepared changes below are source-reviewed, **not built or run on a server by this agent**. Root owns integration, compilation, native fixture qualification and deployment. No main server action, build, original resource-pack write, git commit or native test launch was performed.

Frozen files: `LbcAdvancedSnow.java`, `LbcTeleportGadgets.java`, `LbcOrdinaryAccessories.java`, `LbcDeus.java`, `DeusWhipChecks.java`, `TeleportGadgetChecks.java`, `AttributePolicyChecks.java`, new `WeaknessAttributeMixin.java`, new `RiptideVelocityMixin.java`, new `RiptideVelocityChecks.java`, new `BlockCollisionHeightMixin.java`, new `BlockCollisionHeightChecks.java`. Narrow `LbcItems.java` rocket sections were frozen and handed back to root before its unrelated changes. No change to `LbcFrost`, `LbcMana`, `LbcWands`, initializer, main mixins JSON, build files or GameChecks.

Required root integration: register `WeaknessAttributeMixin`, `RiptideVelocityMixin`, `BlockCollisionHeightMixin`; expose/call `RiptideVelocityChecks.run(game)` and `BlockCollisionHeightChecks.run(game)` in the authoring harness. Existing checks to run: `advanced-snow`, `teleport-gadgets`, `deus-whip`, `attribute-policy`, rocket checks/feedback-models; root removes cold_cd **only from HUD**, per current author clarification.

## Snow Wand

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

`function/workingitems/snow_wand/upgraded/check.mcfunction:2` positions one block above the impact before `summon.mcfunction:1` adds another two. Academy aimed cloud must therefore be impactY+3. The initial worker edit incorrectly added +3 before the shared summon added +2 again; the native fixture caught that regression. Root restores the aimed pre-summon +1 (total +3) and corrects ordinary cast origin to the player eye position, because spell1_default and spell1_default_left explicitly use anchored eyes positioned before summon+2. Existing AdvancedSnowChecks already requires aimedY244.62 at source impact241.62; this is an actual regression assertion, not a newly guessed height.

## Swapper

Both service tick and completed movement-packet hook now compare total displacement squared from the initial origin against0.0001 (0.01block radius), replacing1e-8. This tolerates tiny inertia without accumulating a fresh allowance per packet/tick. Exactly0.01 remains allowed. Existing finite real-scheduler test now moves0.009 and back during charge and rejects0.011.

## Weakness

Native MobEffects defines `minecraft:effect.weakness` as flat attack damage deduction-4/level. Attribute limits intentionally allow other negative attack damage. New AttributeInstance calculation interception runs **only** when this exact modifier is negative and belongs to ATTACK_DAMAGE. Sum base and all other flat modifiers first, bound only Weakness's deduction to the positive remainder, then apply the original base/total percentage operations and native sanitize. Negative bases, unrelated negative additions, unrelated negative percentages, and reversed beneficial Weakness keep their signed behavior. No global attack_damage clamp.

AttributePolicyChecks adds live native AttributeInstance assertions for zero floor caused by Weakness alone, independently negative base/additions and negative percentage. New mixin needs root registration.

## Vex Sword ultimate ? investigated, no damage change justified

Datapack `function/other/phantomarrows/1..8.mcfunction` explicitly authors base damage2.5 and cardinal speed1 / diagonal components0.7. Current `LbcVexSword.PHANTOM` and `phantomShot` preserve these values. Native26.3 AbstractArrow.onHitEntity computes `ceil(length(velocity)*baseDamage)` before mitigation: an initial unit-speed phantom arrow yields3HP, then drag/armor/cooldown may lower accepted damage. Base damage2.5 does NOT mean2.5 raw HP per native arrow contact.

Paper source `plugins/luckywars/src/main/java` has no `groundbreaker`, `phantom_arrow`, or `vex_arrow` handler. Its only `killedarrow` publication found is helicopter bullet generation in `SwrgNativeService:9053`, which sets its own base damage1, unrelated to this ultimate. Ignite source likewise supplies no Vex damage override. Thus no authoritative special reduced-damage multiplier was found, and current values already match the datapack. Prior author clarification in `VEX_FAMILY_SOURCE.md` explicitly sets all eight ultimate forms to PierceLevel100; the source's inconsistent piercing is intentionally not restored.

## Rocket Boots

Existing native handler/identity/acquisition retained. Direct registered defaults previously had chainmail asset/durability and no GLIDER; the complete loot leaf supplies lbc:rocket/280/GLIDER but direct native acquisition did not. Registration now supplies these exact authored defaults. Identity and transferable legacy custom_data/PDC recognition remain in the existing handler.

More importantly, vanilla LocalPlayer predicts `startFallFlying` before sending START_FALL_FLYING. Previous server hook canceled that start without dirtying the server flag, leaving the client in predicted glide and unable to request subsequent boosts. Existing `rocketGlide` now uses native `stopFallFlying()` on recognized non-Elytra attempts, including suppressed/exhausted attempts. Pinned LivingEntity.stopFallFlying deliberately writes flag7true thenfalse, making the authoritative false state dirty even if the server never entered glide. Native Elytra behavior retained.

Read-only actual RP: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/equipment/rocket.json` exists and references `lbc:rocket`; matching `textures/entity/equipment/humanoid/rocket.png` exists. No RP patch is required or created.

## Cold / Runic Ice Shard

Source `workingitems/iceshard.mcfunction:7-8` increments cold only while below10 but refreshes cold_cd2 while thawed, including at10. `other/freeze/cooldown` checks cold effects, decrements cold_cd, then decreases cold when cooldown reaches0; `other/freeze/1` grants hidden Slowness2seconds. `ui/get_data.mcfunction:12` independently appends cold_cd when positive. Current Frost duplicates this source state/timing, including10cold/cold_cd2. No invented timer/state change was made.

Current author clarification overrides historical HUD source: cold_cd must not be displayed at all. Root/mana owner removes just that HUD entry; internal cooldown, cold progression, Slowness duration and global-second phase remain intact. Existing FrostTimingChecks covers20tick global phases, cooldown5/2 and same-pulse decay.

## Rescue platform

Source `workingitems/platform.mcfunction` teleports to absoluteY2 and fills planeY1. Existing shared TeleportMomentumMixin intentionally retains client-local downward Y, which can immediately defeat this rescue on a delayed client. Current author request permits this rescue-specific exception: create the resident guarded plane first, teleport above it, retain XZ and positive Y, clear only negative Y, reset fall distance, then explicitly send final motion. No broad teleport behavior change or chunk load.

## Deus Tonitrus

Removed sample/age phase oscillations. Each link uses one deterministic cubic defined by current endpoints, incoming tangent and distance-derived fixed control points. Adaptive bounded sampling keeps actual segment length<=0.2. Real segment/AABB contacts inspect a single deterministic nearest128 resident candidate pool. Maximum160segments, three unique hits, roughly one particle per three segments plus contacts. A rejected native damage hit stops damage-pulse chaining; target selection for the next link happens after actual current contact. Native2HP every10active ticks, tangent pull, signed knockback response, TECH/strong MAG artifact suppression, TimeStop and scoreboard clocks retained.

Updated real-scheduler DeusWhipChecks proves stationary path equality across successive frames, segment/sample bounds and a sideways first target actually reached before a second link. Old test expected this sideways target to be unreachable because the former steering barely turned; that obsolete expectation was replaced.

Pure numeric sampling audit (no server): sideways19 target reached112samples, path22.148426, maxsegment0.199974; forward20 reached103samples, max0.199997; near4 reached23samples. Root must still qualify actual native damage and suppression fixtures.

## Riptide ? true velocity synchronization prepared; extreme traversal remains unresolved

Pinned authoritative archive: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar` and matching class jar.

**Proven desynchronization**: own `EnchantmentRules.sanitize` projects client item enchantments to255 while server keeps the actual int level. Vanilla ItemEnchantments.STREAM_CODEC uses VAR_INT but the unmodified client constructor rejects levels>255. Removing this projection would disconnect the client. Native TridentItem.releaseUsing uses the local item's level to calculate and push velocity on both sides, so wide levels previously produced different predictions.

**No fictional3.9 limit**: ClientboundSetEntityMotionPacket uses Vec3.LP_STREAM_CODEC -> LpVec3, representable absolute component maximum1.7179869183E10. ClientPacketListener.handleSetEntityMotion directly calls entity.lerpMotion. Entity.setDeltaMovement checks finite, no small cap. LocalPlayer.move calls super.move. Server movement packet positions clamp to +/-30millionXZ and +/-20millionY; these are coordinate bounds, not a usable velocity clamp.

New RiptideVelocityMixin sends the final native server velocity immediately AFTER the actual native Player.push. It replaces predicted client motion and never adds a second impulse. No enchantment/strength/velocity cap, no physics rewrite. `javap -c` against the pinned jar confirms the exact injection target is Player.push:(DDD)V at bytecode353 in releaseUsing.

New RiptideVelocityChecks intentionally qualifies ONLY the actual release/packet boundary: levels10/255/10000/INT_MAX; actual recipient Polymer projection255 with true server level intact; one final-velocity packet; native LP codec encode/decode retaining huge values within quantization; no double impulse. It explicitly resets velocity before any following physics tick and restores runtime/spin flags. Root must compile/integrate/run it.

**Unresolved, do not claim fixed**: Player.getMoveSimulationType returns AUTHORITATIVE_SIDE_AND_SERVER, isEffectiveAi is true server-side; LivingEntity.aiStep calls native travel. Entity.collide queries entity collisions over the entire expanded swept AABB, then collects block colliders over that AABB. Native BlockCollisions enumerates Cursor3D block volume before null chunk checks; huge diagonal/vertical vectors can therefore hang BOTH server and unmodified client. Sending a true huge motion packet fixes prediction but cannot remove that physical traversal. Avoiding it without an effective speed bound needs a separately agreed movement/collision architecture or client changes. The author rejected small caps and effective255; this agent did not introduce either, nor silently substitute unloaded-terrain noclip/custom movement. INT_MAX server survival and physical-client survival remain unqualified requirements.

## Verification performed here

Targeted git diff --check passed. Pinned source and javap inspection completed; native source constants/packet/injection descriptors checked. Pure cubic numeric audit passed. No compile, native server check, client launch or performance benchmark was performed. Existing author dirty files and original RP were preserved.

## Latest author-approved height optimization

After the initial freeze, author explicitly permitted skipping absent block collisions above/below build height. Pinned26.3 BlockCollisions constructor has NO Y clamp: it creates Cursor3D from floor(boxmin)-1 to floor(boxmax)+1. `LevelHeightAccessor.getMaxY()` is INCLUSIVE in26.3. Cursor3D multiplies width*height*depth in int; huge bounds are particularly unsafe.

New BlockCollisionHeightMixin changes ONLY Cursor3D construction inside the CollisionContext overload. It intersects the Y cursor range with minY-1 through maxY+1, keeping one air sentinel on each side so native boundary-face classification still admits large block shapes. The original AABB, collision shapes/context, entity collision query and world-border paths are retained. Empty intersection returns an immediately empty cursor, even when XZ is enormous. Y endpoints are computed with Math.floor in double before narrowing: original Mth.floor(hugeY)+1 can itself overflow before a later clamp. This handles above/below/crossing queries without iterating nonexistent world heights. It does not load terrain.

New BlockCollisionHeightChecks exercises actual native iterator: hugeXZ box entirely above/below has no iteration, a +/-10b narrow vertical column still detects both bottom and roof blocks, top-world fence protrusion remains collidable. Root needs to register/integrate/compile/run these changes. These are prepared assertions, not claimed runtime results.

This approved optimization improves the SERVER block cursor only. It does not patch the vanilla client's same cursor; it also does not bound a huge XZ footprint while intersecting world height, nor enormous entity section queries. True final Riptide motion packets can therefore still expose extreme client traversal. No blanket velocity limit or custom sparse physics was introduced.
