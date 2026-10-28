# Authored ordinary knockback defaults ? 28 October 2026

(user, task contract) Ordinary migrated item/mob defaults have a combined attack_knockback attribute plus held Knockback enchantment budget of5. Reduce positive attribute contribution first, stopping at0, then reduce the enchantment if needed. Preserve negative attributes. Legacy item custom_data imba:1 exempts that source item; imbadrops retain exact source values. This is source balancing, never a cap on arbitrary runtime commands, enchantment changes, or player-created items.

## Source audit and changed defaults

Read-only source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

The migrated `ported/antimagic_sword.json`, `ported/antimagic_sword_loli.json`, and matching ordinary entry in `transformables_sword_all.json` contain Knockback255 without imba:1. Their authored enchantment defaults are now5; names, lore, source markers, model, tier and other modifiers are unchanged. The complete source entry is inspected before normalization, including custom_data set by a later loot function.

Both ordinary creature-drop skeleton variants in `LbcCreatureDrops` author base100 and hold a KnockbackI bone. Their freshly parsed source presets now become base4 plus unchanged KnockbackI. Literal historical SNBT remains available for audit. `LbcSpecialEggs.Kind.FAT_SKELETON` is explicitly an imba egg; its base100 and held KnockbackI stay exact. Its construction does not enter the ordinary source normalizer.

Other authored Java attribute constructors found by the audit are Hedgehog Tank1 and Psycho Vex0; they need no reduction. Random stat upgrades are dynamic gameplay and remain independent. Summoner presets include nested passengers and native equipment, so `LbcSummons` normalizes the freshly DFU-converted ordinary source copy before constructing actors. The current ordinary Ghostrider skeleton has PunchII; it is unchanged. No numeric source Punch aboveII was found in the migrated resource audit. The author clarified that the source Punch limit should approximate the old huge-knockback behavior after removal of native velocity caps, rather than use the natural enchanting maximumII. Ordinary migrated Punch aboveVI now becomesVI; current source bows remain unchanged because their authored levels are at mostII.

Raw `slabs.json`, `aimbob.json`, and `loliluckyblock.json` retain legacy `function`/`functions` schema, are outside the current processResources allowlist, and are not newly shipped. They contain ordinary10/255 values. Importing these through `migrate_loot_schema` now applies the same budget to the completed migrated tree. Raw unshipped imba/slabs contains31 overlevel entries lacking any individual imba marker; its imba source provenance must be retained explicitly when it is eventually ported. Existing imba food/slab/spawn-egg snapshots were not rewritten.

## PunchVI derivation and precision limits

(user, clarification) Derive a practical old-behavior approximation, rather than reducing ordinary source bows to natural PunchII. The parent selectedVI from the full native hit threshold below.

A locally retained named Paper1.21.4 server binary was inspected read-only with `javap -c -p`: `E:/servers_mew/cringe1_21_1/cringe1_21_1/versions/1.21.4/paper-1.21.4.jar`, SHA256 `8be8194c408428547d467a8bc82d3f0c9615bc2e4e0cab6d3df026bc7ddf147f`.

- `ClientboundSetEntityMotionPacket(int, Vec3)` bytecode offsets13?56 clamp each velocity component independently to[-3.9,3.9], then offsets58?88 multiply by8000 and truncate to integer.
- `AbstractArrow.doKnockback` offsets72?97 normalize the horizontal arrow direction and scale by enchantment knockback*0.6*max(0,1-resistance); offsets109?124 add that push with vertical0.1.
- `LivingEntity.hurtServer` offsets542?566 first call native damage knockback with0.4000000059604645. Its own knockback method halves prior horizontal motion and adds the normalized base impulse; this is distinct from the arrow's later additive Punch push.

For a stationary, zero-resistance victim and an aligned cardinal hit, the full horizontal impulse is approximately0.4+0.6*L. PunchV gives3.4, below the old packet limit3.9; PunchVI gives4.0 and is the first integer level that saturated that ordinary full-hit case. The migration therefore usesVI. Ignoring base damage knockback would instead produceVII; that is not the chosen full-hit comparison.

This does **not** exactly preserve every old outgoing vector. The old clamp was per axis, not a cap on horizontal magnitude: diagonal, resistance, preexisting motion, target physics and non-aligned damage direction change the threshold. No single integer enchantment can recreate all those cases. SourceVI is an intentional conventional-hit approximation after the old caps are removed; it introduces no runtime arrow/velocity clamp. Imba source Punch and arbitrary runtime Punch200/255 remain available.

## Boundaries and future ports

`tools/normalize_source_knockback.py` runs once after schema migration of a complete source loot tree. It changes only authored ordinary item defaults. `migrate_loot_schema(..., preserve_imba=True)` explicitly preserves imbadrops provenance even when the original entry has no imba item marker. A future importer of imba/imbadrops material must pass that flag and retain the exception when extracting a standalone leaf outside the original directory. A marker on a contained item/entity cannot exempt its parent item.

`SourceKnockbackBalance.entity` is only for freshly parsed or DFU-converted ordinary authored presets. It recursively handles passengers and equipment, lowering authored mob base before held additive modifiers and then enchantment levels. It is called from `LbcCreatureDrops.preset` and `LbcSummons` preparation. Do not call it from tick, inventory, command, attack, conversion, or entity-load-wide hooks: those would rebalance player/runtime changes and erase the source boundary. Do not pass imba egg/drop entity payloads into this ordinary helper.

Current ordinary authored attack_knockback modifiers are additive. Both normalizers reject unsupported non-additive authored modifiers for explicit review; they do not silently guess multiplication semantics. Ordinary source Punch is capped atVI at these same explicit boundaries; no global enchantment/arrow hook is installed. Source negatives are preserved, including the case -2 + KnockbackIX -> -2 + KnockbackVII rather than an unnecessary reduction toV. Modifier IDs, slots and operations remain intact.

## Verification and limits

`tools/check_source_knockback_balance.py --self-test-only` passes priority, negative remainder, exact source imba exemption, nested-marker isolation, idempotence, explicit unmarked imbadrops provenance, unsupported-operation review, source Punch255->VI, source PunchII preservation and imba Punch255 preservation cases. `--resources build/resources/main` audits the actual processed loot trees for remaining ordinary reductions; explicit imba/imbadrops source directories retain their exception. This check does not parse embedded SNBT; native source tests cover that boundary.

Processed-resource verification passed against the parent's current `build/resources/main`:1300 loot tables plus all migration edge cases. This is an artifact source audit, not a server execution claim.

`SourceKnockbackBalanceChecks.run` verifies actual ordinary loot rolls, a native skeleton loaded from balanced serialized equipment, priority and source-marker preservation, negative source values, and native runtime Knockback200 remaining available. Parent integration owns compilation and actual server checks, including real ordinary creature drops and imba encounter eggs. No build or server was started by this scoped worker.

The broad design-source check currently stops at a preexisting blazefire kit mismatch: legacy source item `minecraft:carrot_on_a_stick` versus already ported `skywars2:blazefire_sword`. Source knockback normalization makes no change to that source table. No unrelated source-check exception was added.

No author resource-pack paths, live datapack overrides, or the198 preexisting dirty paths were edited by this worker. Installation and qualification status must be supplied by the parent run; this note does not claim server installation or physical-client verification.

## Integrated qualification - root, 28 October 2026

Final build31b84a20 (SHA25631b84a206f81a967e74a7812b7cbb0613e69eb84d3841d402afd489d0e3f5fec)
passed38recorded suite results in2actual isolated mode1matches plus2scoped participant setup/cleanup results,60.0s.
Real Carpet attacker/victim and resistance-100, complete base/sprint/enchant direction, signed/extreme attributes,
native melee/magic/projectile controls, source loot/mob/bow/imba/runtime behavior and neighboring combat/stop checks pass.
The direct native spear source is also covered; owner-attributed magic and arrows retain their existing geometry.
Exact544sources/1544classes/3230resources match the selected artifact.1300processed loot tables and Python migration
edge cases pass.198preexisting author paths and29live override JSONs are byte-identical. Native child stopped
normally and exact snapshots were restored, including TestKnockback playerdata/stats/advancements and usercache.
Initial439d25c8 attribute-only direction checks passed in both matches; its full qualification was rejected for
an obsolete pre-WILD arrow assertion and incompatible simultaneously retained TimeStop stop probes. Initial log,
results and artifact proof remain under build/verification/knockback-repair/initial-*. Final harness retains the
changed creature source's stop fixture, preserves all its assertions, and verifies current WILD arrow behavior.
Installer9360 is authorized to replace main b3c13ce9 only after normal shutdown/world unlock; consult main-install.json
for installed versus waiting status. No main force-stop/restart, physical-client rendering or peakTPS claim.
No RP change. Runtime commands/enchants remain unrestricted; ordinary source balancing does not rewrite live inventories.

Installation receipt confirms31b84a20installed overb3c13ce9, no restart, worker9360.
