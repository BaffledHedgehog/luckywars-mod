# Electric wall, black-hole contact and Radiant Metasola ? 28 October 2026

(user) Gaycaster meditation electric walls must not hurt players whose actual hitbox misses the visible strand. Void Arcanum must not be repelled by a victim's body; projectile crowd pushing is unwanted. Radiant Metasola must retain unsupported fire and floating lava placed without block updates. Earlier user requested a full integrity1 pattern.

## Electric wall

Case4 in LbcGaycasterAcademy used one axis-aligned damage box spanning roughly2x2 horizontally per electric column. A standing player could be hit while its hitbox missed the actual strand. Use the existing shared LbcBeamGeometry.clip helper on the full vertical segment, at each column's actual X/Z and Y[-.5,1.5]. The warning plane already uses the same two-block height centered at+.5; particle centers now match it. Seven lanes,4.5spacing,8arrow damage, movement, warning orientation and lifetime remain. New real-player handler probes cover cardinal/diagonal movement, both side edges/corners and vertical endpoints; source trial remains active and native author credit remains.

## Void Arcanum and projectile contact

Native Marker has noPhysics; the hole body already cannot exchange physical crowd impulses. The actual steering bug was a one-block exclusion shell: candidate chase points closer than1 were omitted, allowing a farther target to take over. Select the genuinely closest chase point including overlap. Arrival uses the discrete braking speed sqrt(a*a+2*a*d)-a, capped by distance and the existing legacy tier maximum, while retaining the bounded steering acceleration. No teleport, new speed boost or changed eye+tier offset.

Cancel only Entity.push(Entity) when either side is a Projectile. This also prevents an explicit projectile-initiated contact call from pushing the other creature, even though ordinary projectile isPushable was already false. Keep push(DDD), damage collision, block hits, reflection, explicit pulls and deliberate velocity controls intact. Native pair tests cover Arrow/Snowball/SmallFireball/ShulkerBullet and actual acid/charge/banana classes, both orders; cow/cow and explicit-vector controls retain their normal impulses. Arcanum tests cover close target versus opposite farther decoy, exact overlap, real marker collision and all-tier eye arrival/legacy caps.

## Radiant Metasola

Literal legacy other/event/halloween/soulcorruptwand/metasola/5_tick1 uses integrity0.1 with strict. This differs from the author's requested full integrity1 pattern; the new author requirement wins without rewriting source history. The original NBT and mod NBT are identical:21^3 bounds,4945 stored cells =2229 fire,739 source lava and1977 intentional void_air, with4316 unspecified cells. Preserve the exact sparse mask and source-air clearing.

The shared MapManager strict path already uses knownShape and UPDATE_CLIENTS|UPDATE_SKIP_ALL_SIDEEFFECTS, so no duplicate placement engine or Kiuaskivi support filter is introduced. Type5 now requests integrity1.0 through that path; damage/cause, warmup, host descent and lifetime stay. Update the existing Soulhunter field assertions to full source counts. Dedicated native fixture activates a real item, waits200 normal service ticks, verifies exact mask and no scheduled fire/lava updates, removes the emitter, then checks unsupported fire/floating lava after120 normal ticks without replenishment. Restore the scoped blocks, scheduled ticks, player and owned chunk tickets.

## Direct melee versus credited damage

(user) Chef/summon damage credited to its player must not trigger the player's carried Metal Pipe or other sword effects.

Use the existing LbcDamage.isMeleeAttack identity/type predicate in nine actual held-melee consumers, including the held Ice/Terra/Katana tail of LbcItems.damaged. Both damage and death callbacks are covered. Do not gate the entire shared damage dispatch: victim reactions, player kill credit and the previously approved recursive incoming/reactive artifact interactions remain. MetalPipeChecks includes accepted nonlethal and lethal native MOB_ATTACK sources with a direct cow and credited player, plus real direct-player controls.

## Ultraponos native explosions and lightning

(user) Ultraponos must use native explosions instead of hidden creepers. Vanilla lightning nearby must double their power to compensate for the old charged-creeper interaction.

Replace the four/eight helper actors with logical blast records: original positions, base radii6/7/8/9 and skill9 additional12/14/16/18 first, captured thrower and290 entity-resident active ticks. Keep the finite280tick item/particle emitter. Time Stop and non-entity-ticking chunks pause the blasts without adding tickets or generating chunks. Mechanism suppression permanently cancels only the emitter. Epoch changes and match stop clear pending blasts.

Each expiry calls ServerLevel.explode directly with MOB interaction, preserving mobGriefing and captured WILD attribution. A synchronous source identity filters protected roles before exposure/damage/knockback even without a thrower. Actual chained TNT adopts the existing CreeperDrops child lifecycle, WILD policy, nullable owner, save/unload and Time Stop handling; no fake creeper is needed. The old helper-creeper adoption entry point is removed.

The existing native LightningBolt tick mixin hooks exactly the active non-visual strike branch. Pending logical blast bodies intersect the same vanilla X/Z +/-3, Y[-3,+9] strike volume that charged the former creepers. Strength doubles once, including skill9 blasts; repeated bolts/flashes do not stack and do not reset the delay. Visual-only or distant bolts do nothing. Queued blasts can still charge after the item emitter ends.

Audit of Java and packaged command resources found no other spawned immediate-creeper explosion workaround: CreeperDrops and Sarvatra eggs are actual mobs with AI/loot. Exasperato Cosmos already calls native explode; its unattached in-memory source supplies the existing name/source policy and is never spawned or ticked.

## Qualification

Build7350302f passed36 recorded results in two actual mode1 matches (201.97s), including all production scenarios, Babylon-before-combat, original creeper-mob regression and actual match stop clearing pending native blasts. The owned child stopped normally and exact fixture snapshots were restored. Exact550sources/1561classes/3232resources match the tested JAR; all198preexisting author paths and29live override JSONs remain unchanged. Includes the already qualified vanilla scale0.0625..16 repair. No resource-pack changes. Main installation uses the stopped-only shared worker and supersedes queued8ce5423f once the final artifact check completes. No physical client, actual queue-unload-transition or natural TPS claim.

The first f1fc2c91 native run completed both matches, with only two failures from the Radiant persistence fixture's incorrect sampling prerequisite. Exact NBT topology has no lava directly over air:642 lava cells sit over lava and97 over unsupported fire. The corrected fixture samples those real floating lava cells, preserving all exact-palette, no-scheduled-ticks and120natural-tick persistence assertions. Initial results/proof/log are retained under initial-f1fc2c91-*; this is a fixture correction, not a production placement change.

The first7350302f batch run passed all production scenarios in both matches, including real lightning charging and native TNT child lineage. Only the added Babylon neighbor suite refused its intentional fresh-attribution prerequisite after native damage/knockback suites. Its precondition was preserved and its position moved before combat suites; initial order-dependent results/proof/log are retained under initial-7350302f-order-*. The same immutable JAR is rerun; no production or test predicates were relaxed.
