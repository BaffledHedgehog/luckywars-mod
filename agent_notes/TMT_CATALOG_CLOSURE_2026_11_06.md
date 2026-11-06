# Full TMT catalog and routing closure audit — 6 November 2026

Read-only audit of current Java/resources and the full legacy datapack; this new
note is the only output. No build, JVM, server, indexing, source-server or author-RP
mutation. This is an implementation map, **not a completion claim**.

The target is full useful finished-content composition, seven draws with replacement,
private ability version/mode/type and Tiny enabled. The existing 103 common-wand
profiles and 12 self/jump profiles are components, not the full catalog.

## Exact source activation matrix

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

| Actual entry | Source group order after setting player `tmticked=1` |
| --- | --- |
| `function/items/tmt_item.mcfunction` USING_ITEM | `items/swords`, `items/armor`, `items/crossbow_charging` |
| `items/bows.mcfunction` real bow continuation, unless already marked | swords, armor, crossbow_charging, crossbows, spells |
| `items/crossbows.mcfunction` real crossbow continuation, unless already marked | swords, armor, bows, crossbow_charging, spells |
| `items/damaged.mcfunction` with TMT anywhere in inventory | **mark only**, no additional groups |

These are literal `drops/tmt/trigger_smth{,_not_bow,_not_crossbow,_not_damaged}.mcfunction`.
`players_tick.mcfunction` resets the marker every tick. The damaged entry is not
guarded by `unless tmticked` at its caller. Its purpose still includes preventing
another complementary fanout later that tick. A per-stack reentry scope alone does
not reproduce the player/tick guard. Once-only fanout must not suppress unrelated
ordinary native hit, inventory or shot owners.

SelectedItem means main hand even if offhand TMT caused USING_ITEM. Swords has real
victim branches; do not manufacture accepted damage or search nearby HurtTime to
run those. The already extracted `sourceSelf` methods are the proper no-hit bodies.
Spells checks both hands under source admission; it is absent from ordinary USING
fanout. Bow/crossbow complement groups must not fabricate an ordinary projectile
volley or award a second skill/trinket proc. The old draft's inventory-presence
infinite-arrow grant is obsolete: only the actual selected infinite-arrow ammo is free.

Five source carrier choices are carrot-on-a-stick, bow, snowball, crossbow and
golden apple; `set_basics` provides category markers, not actual native Item class
behavior. Apple generation replaces food/use metadata as recorded in the kernel.

## Catalog unit and efficient reuse

The literal archive `drafts/tmt/tmt_source.json` contains 1,049 rows but **599
distinct keys**. Repeated-key domains include `spellid` 1..241, `pill` 0..129,
`cooltype` 0..16, `cunny` 1..16, `mystical` and `mystical_old` 1..4, `type` 1..8,
`mode` 0/1, `multiply` 2..5 and 20 `mana` values. A row is not generally an ability.
For example, `type=3` has no independent native effect; `spellid` needs its category
and a supported spell owner. The 1,049-domain is evidence to reconcile, not the new
sampling domain or a denominator for finished implementation.

Use a finite bootstrap manifest of **semantic profiles**, assembled from canonical
prototype payloads plus explicit native owner/event/location adapters. Recommended
fields: family ID, variant ID, source definition, parameter domain, category/owned
keys, events, valid location/carrier, native owner and adapter, admission, state
capture requirements. Deduplicate acquisition aliases, counts, display-only variants
and Academy aliases when behavior is identical. Preserve genuine current/old,
upgrade/mode, Tiny and outcome differences inside that family's parameter domain.
Do not let a 241-entry spell selector or 130 pill IDs accidentally weight unrelated
families unless the chosen catalog distribution explicitly intends that weighting.

Existing reuse points:

* `LbcUtilityItemPrototypes`: one canonical loot reader, deferred holder-dependent
  components, inherited native defaults and explicit component-removal support.
* `LbcMeleeItems.Kind`, `LbcAccessoryItems.Kind`, `LbcProjectileWeaponItems.Kind`,
  `LbcImbaCrossbows.Kind`, other `Lbc*Items` definitions: native ID → source resource
  → current payload. These are stronger evidence than guessing flags by item names.
* `LbcLibrarySpells.Definition`, elemental/Danmaku owners and `LbcPills.Definition`:
  typed supported outcomes; use these, not arbitrary numeric spell/pill fabrication.
* `LbcNamedFoodItems.Entry`: source flag/definition/carrier/native item mapping.
* `LbcTmtAbilities.write/invoke`: bounded seven-slot persistence and scoped payload
  projection against the actual stack; repeated family writes overwrite, not replay.

Resolve and validate the manifest once per registry generation. Read JSON and
enumerate registry domains at bootstrap/reload, never on a cast, tick or random draw.
Generate a source-reconciliation report at development time: every old row maps to
a supported profile/parameter or a concrete exclusion reason; every finished modern
item family maps to a profile or a concrete non-ability exclusion. Automatic scans
can seed this report but cannot certify behavior.

## Meaningful families by event and current native owner

The following is the concrete owner worklist. A listed owner exists in current
`LbcItems` or item classes; inclusion here does not assert TMT routing exists.
Enumerate its canonical supported definitions/outcomes, not an invented engine.

| Event/location | Meaningful profile families and current owner |
| --- | --- |
| Selected-main self continuation | Feeding/Vampire/Sea Guardian/Evolution: `LbcClassicMelee`; Triple Golden/Blazefire: `LbcChargedSwords`; already extracted and verified |
| Actual melee accepted damage | Epic/Fragment current+old and fragment types/Ender/antimagic: `LbcPendingSwords`; Chaos/Gravity/Piston: `LbcPendingCombat`; Devil: `LbcDevilSword`; Mortis/Banhammer/Scythe: `LbcSpecialTools`; Metal Pipe/Divider: `LbcMetalPipe`; Electro, Terra, Vex, Fevgalea, Kara, charged swords: their existing owners |
| Inventory attack reaction | Current/old mystical=2 outcomes: `LbcReactiveArtifacts`; Sand Amulet: `LbcIncomingArtifacts`/existing amulet owner; use the real attack victim/context |
| Actual incoming accepted damage | Current/old mystical=4 six outcomes: `LbcIncomingArtifacts`; Gaara pot: `LbcGaaraPot`; armor reactions: `LbcArmorSets`, Chaos/Shulker/Glass/Nanobot/Berserk/Phantom/Nyauwu owners; retain actual slots/set requirements |
| Actual jump/source armor continuation | Current/old mystical=3 three outcomes: `LbcJumpArtifacts`; Halloween/Soulhunter: respective owners; overload: `mana().overload().armor()`; Shulker, Techno, Chaos: seven existing jump/armor owners |
| Inventory/passive/equipment | Current mystical=1 outcomes: existing legacy/passive/magic owner; Save/Magnet/Devil Stomach: `LbcOrdinaryAccessories`; regeneration belt: `LbcRegenerationBelt`; mana belt: `LbcMana`; Infinite Quiver: `LbcQuiver`; Moisei: `LbcMoiseiBoots`; Bucket Head/drop/pickup: existing native helmet owner; Holy/Hylian shield: `LbcHolyShield`/native blocking; armor set families: `LbcArmorSets` and subowners |
| Common wand use | Existing 103 profiles/58 families in `LbcWands`; maintain all private mode/version/Tiny state and source order |
| Other spell/library use | `LbcLibrarySpells`, `LbcElementalGrimoires`, `LbcDanmaku`, `LbcUniwand`, `LbcDefensiveArcanums`, `LbcVoidArcanum`, `LbcBabylon`, `LbcSummons`, `LbcRewardTools`, `LbcImbaRunes`; select only typed supported outcomes with complete prerequisite flags |
| Genuine projectile launch | Explosive/double/old payloads: `LbcBalance`; Shulker current+old: `LbcShulkerBow`; Madoka: `LbcMadokaBow`; Railgun/Shish: `LbcChargedBows`; Minigun/old: `LbcMinigunCrossbow`; five IMBA crossbow kinds: `LbcImbaCrossbows`; Harpoon/Sea Legend: existing native trident owner |
| Ammo/projectile payload | Ordinary/IMBA/ownerless Quiver arrows: `LbcQuiver`, `LbcHomingArrows`, native typed arrows and existing attributed child owners; Babylon and trinket payloads retain their own family origin. Profile needs real ammo/launch semantics, not just `standard_round=1` on any carrier |
| Throw/snowball/projectile collision | Ruchka/Honey/Metal Pipe/Bucket/Pumpkin: `LbcThrownUtilities`; special fireballs: `LbcSpecialFireballs`; TNT/projectile gadget: existing `LbcItems` owner; special eggs: `LbcSpecialEggs`; Touhou bomb: `LbcDanmaku`; hedgehog/spike variants: `LbcHedgehog`/`LbcHedgehogSpikes`; ant/acid behavior retains WILD rules |
| Finish consume | Named golden/diamond/netherite foods: `LbcNamedFoods`; effects/explosions/transform/refund: `LbcFoodLifecycle`, `LbcFoodStatuses`, `LbcAttributeFood`, `LbcEdibleSpells`, `LbcKfcFood`, `LbcPills`, `LbcCannedFood`; must preserve private state before decrement/replacement and one real consumption |
| Death protection | Devil/infinite/broken totem: `LbcTotems` + `LbcItems.usedTotem`; require actual native death protection/equipped location, not an incoming-hit emulation |
| Use/mining/block interaction | IMBA shovel/hoe/pickaxe/axe: corresponding native owners; Steampunk breaker: `LbcSteampunkBreaker`; crowbar/reward tools: existing tool owners; ordinary tools' mining/strip/till/path/ignite/SilkX are native tool/component behaviors and need an applicable carrier/interaction adapter |
| Modern gadget/continuous use | `LbcShooters`, `LbcLaserMinigun`, `LbcAk47`, `LbcMagnum`, `LbcGovnomet`, `LbcMobgun`, `LbcStand`, `LbcTimeStop`, `LbcReactive`, `LbcTransformator`, `LbcStalin`, `LbcHohlopitek`, `LbcFpvDrone`, `LbcSteampunkHook`, `LbcTeleportGadgets`, `LbcRasengan`, `LbcPopit` |
| Modern dimension/technology | `LbcNexusTools`, `LbcNexusCombat`, `LbcNexusCompass`, `LbcNexusWires`, `LbcSupernova`, `LbcNovemberLightsabers`, `LbcPozdmanBot`, `LbcCursedPumpkin`, `LbcShishEncounter`; retain their current location/owner admission |
| Modern utility/collectibles | `LbcBridgeBuilder`, `LbcBadApple`, `LbcMellstroy`, `LbcCunny`/terrain, `LbcImbaTerrainUtilities`, `LbcWhiteArrows`, `LbcDiamondifier`, `LbcKapusta`, `LbcPumpkinator`, `LbcGodDice`, `LbcMalevich`, `LbcDeathCompass`, `LbcTouhouCollectibles`, `LbcWitchIndolence`/fingers; use their real input, inventory and target owners |

`LbcItems`' owner fields and tick list are the modern-coverage inventory, not a
catalog to execute wholesale. Skill/class-only owners (e.g. Planetoid/GodClaw/
Psychblind skills), encounter state and infrastructure are not automatically item
abilities merely because they are ported. A native item that grants/calls one can
contribute its supported item-facing operation; do not grant an entire player class.
Finished drop-token mechanics likewise belong only when they supply a meaningful
item-facing effect with the proper real loose/drop event; acquisition token routing
must not be silently confused with the generated TMT's held ability.

## Concrete exclusions and parameter ownership

Exclude standalone service/category flags already supplied by `set_basics`: weapon/
tool categories (`sword`, `hoe`, `axe`, `pickaxe`, `shovel`, `bow`, `crossbow`,
`trident`, `shield`), equipment/accessory categories, `gadget`, `shooter`, `summoner`,
`raycastspell`, `magicwand`, `libspell`, `spell`, arcanum categories, `instantitem`,
`craftusable`, `transformable`, cooldown categories and `projegadget`. A profile can
own required category flags but selecting only one of them is not a useful ability.

Exclude standalone `mode`, `type`, `upgraded`, `mana`, `spellid`, `cooltype`,
`spellcount`, `phd`, `multiply` selector values; attach them to the proper semantic
family. Some selectors (pill/cunny/mystical) represent real outcomes only when their
full native classification/admission is present. Do not exclude their effects.

Exclude loot color/tier/rarity labels, `drop`/`tmt`/`tmt_item`, acquisition/currency
markers such as Bucks by themselves, obsolete attribute-category marker names whose
meaning is already native attribute components, crafting-only recipe eligibility,
or a tag with no current reader/effect. Tags such as `random_health` or `random_damage`
are **not** equivalent to those dead attribute labels: `LbcAttributeFood` consumes
them. A failed grep does not establish deadness: flags are also constants/maps,
typed definitions, item IDs and component predicates.

## Routing hazards that must be closed

1. Native identity predicates are sometimes `stack.is(nativeItem)` or carrier/class
   checks. `LbcPills` certificate requires a certificate/repeating-command carrier;
   food classifies via `LbcNamedFoodItems.carrier`; item subclass methods implement
   continuous IMBA crossbows. A flag projection cannot invoke these automatically.
   Add explicit adapter admission to the existing owner or use a dedicated carrier
   dispatch. Do not replace the player's held stack with a synthetic prototype.
2. `TrinketBowMixin.skywars2$shot` returns early for explosive/charged/IMBA-beam paths.
   It then runs IMBA → Balance → Shulker → Madoka → Minigun. Merging flags or rerunning
   this whole pipeline loses siblings or duplicates ammo/trinket/levelup work. Split
   payload augmentation into existing owner adapters around one accepted real shot.
3. `LbcImbaCrossbows.kind` and many typed kind selectors choose the first match.
   The current private scope hides **selected property keys**, but base classifier
   flags and native item identity remain. Audit classification per owner. A base
   `fragmentsword` or category can still be meaningful in the wrong scope.
4. Passive/inventory scans require a union of private profile contributions at the
   real location, as jump masks already demonstrate. Projecting only held stacks
   misses carried/equipped properties; iterating all stacks and rerunning whole
   tick owners duplicates their timers and existing ordinary inventory effects.
5. Real consume, durability break, transformation and stack replacement can empty
   or replace the stack before `invoke`'s finally restores state. Decide explicitly
   which native replacement owns the remaining TMT payload and seed; don't silently
   resurrect consumed items or lose sibling abilities.
6. Producer callbacks/projectiles need the private profile snapshot, mode/version
   and source attribution, not a later read of whichever stack is held. Existing
   clone/TimeStop/rewind capture must include these snapshots and once-guard state.

## Closure checklist and honest release boundary

- Produce a complete legacy599-key/1049-row reconciliation and modern native-item
  profile reconciliation, including explicit exclusions. Counts of known profiles
  or ready acquisition roots cannot replace those reports.
- Complete all applicable owner adapters above; validate per-ability parameters,
  duplicate overwrite, seven draws, Tiny and all legitimate current outcomes.
- Wire five native carriers, token-to-one-output consumption and registry reload,
  plus all four exact entry groups and player/tick fanout guards.
- Exercise actual accepted melee/incoming damage, jump/equipment/inventory, launch/
  reload/continuous use, projectile impacts, consume/replacement and death protection;
  verify siblings together, offhand-main interaction and ordinary-item coexistence.
- Verify team/protected role/MAG/TECH/TimeStop rules in each existing owner, attributed
  delayed effects, clone snapshots and supported rewind restoration. Creation stays
  unjammed independently of an individual ability's admission.
- Carry forward the full sealed 57d1d0c7/f8c38689 component base and qualified entity
  protocol fix; the self note lists exact families/resource. Git status is insufficient.
- Finish the item-atlas procedural visual and physical-client render validation;
  do not claim that old numbered textures or a seed alone complete that requirement.
- Only then qualify mixed/custom/type/all-item pools (one logical TMT item), Bucks
  and dependent reward/mode releases. No full completion/install claim from this audit.

Source evidence: `TMT_SELF_2026_11_06.md`, `TMT_PROPERTIES_2026_11_06.md`,
`TMT_REWARD_SOURCE_2026_11_01.md`; current `LbcTmtAbilities`,
`LbcUtilityItemPrototypes`, `LbcItems`, `LbcMeleeItems`, `LbcAccessoryItems`,
`LbcProjectileWeaponItems`, `LbcImbaCrossbows`, `LbcLibrarySpells`, `LbcPills`,
`LbcNamedFoodItems`, `LbcFoodLifecycle`, `TrinketBowMixin`, `LbcEquipmentMixin`,
`ConsumableSkillMixin`; full legacy `items/{bows,crossbows,damaged,swords,armor,spells}`
and `drops/tmt/{set_basics,set_rc,set_bow,set_snowball,set_crossbow,trigger_smth*}`.

## Second-pass closure of the 29 unmatched raw keys

The accompanying `TMT_CATALOG_2026_11_06.json` now records every matching nonstorage
line across **both** `lbc` and `lbcr` functions/advancements/loot, including exact
relative paths and line numbers. The following resolves the earlier blanket
"unresolved" classification into evidence-based dispositions. A live source gap
is not a useless ability and remains a full-completion blocker.

| Key(s) | Exact source route / disposition | Current native consequence |
| --- | --- | --- |
| `blood_slab` | `advancement/blood_slab` → `other/imba/blood_slab/place`; `raycastselector_imba:9` → tick | Real placed-block engine; native owner not located |
| `cum_slab` | `advancement/cum_slab` → `other/imba/place_cum_slab`; requires **birch slab** in match_tool; selector line8 | Real placed-block engine; native owner not located |
| `bucks` | `projectiles/eggs:29` → `other/bucks/rc` | Real snowball effect; currency label alone is not it; native owner not located |
| `clocks` | `workingitems/spell1_default:70` / `_left:55` → `clock_fragment/select{,_left}` | Requires magicwand and **string** `type`, e.g. `1t`, `1s`, `2s`; distinct from `stand_clocks`; native owner not located |
| `double` | `items/bows:4` → `workingitems/doublebow` | Real current bow payload; native `double_old`/Balance is a different source variant; current owner not located |
| `fastexplode` | `items/crossbows:16` → `workingitems/fastghast` | Real rocket reload/replacement; `LbcMinigunCrossbow` only implements fast/fast_old, so no alias proof |
| `fastfired` | `items/crossbows:13` → `workingitems/fastfiredbow` | Real arrow volley/replacement; current owner not located |
| `penisgun` | `items/crossbows:19` → `workingitems/penispenispenis` | Real crossbow payload; current owner not located |
| `gayradar` | `projectiles/eggs:28` → `other/gayradar/click` | Real snowball payload; current owner not located |
| `impulse` | `projectiles/specials:19` → arrow tagged edited3 | Real special transport; do not equate it with `spike_impulse` without source equivalence; native owner not located |
| `kill_rand` | `projectiles/eggs:23` → `other/kill_rand_hihihiha`, TECH gate | Real snowball payload; current owner not located |
| `luckyspawner` | `drops/specialdrops:26` sets an authored lucky-block item spawner | Real loose-drop creation; native owner not located |
| `steampunk_rocket` | `projectiles/eggs:34` → `swrg_kit_integration/items/steampunk_rocket` | Real snowball payload; current owner not located |
| `trollge` | `projectiles/eggs:21` → `other/troll_box/select` | Real random snowball event family; native owner not located |
| `imbaxe` | `advancement/imbaxe` → `other/imba/imbaxe_use` | **Located:** `LbcImbaAxe.recognized:53` accepts only `LbcImbaAxeItem.TOKEN`; events BLOCK_BREAK/REAL_MELEE. Canonical source read in `LbcImbaAxeItem`; TMT needs an explicit identity adapter |
| `luck_key` | `advancement/key_luck_used` → durability clear; `loot_table/nekoluckyblock` mainhand predicates | **Located:** current native loot predicates, `LuckyBlockChecks:194` and `ZaikaPassiveChecks:164`. Acquisition eligibility, not an independent random cast |
| `gluttony_cooldown` | Only behavioral reader `ui/cooldown:20–21` selects readiness/no-sin glyph | Exclude standalone UI marker; actual whale/rabbit/snake mechanics remain meaningful |
| `crowbar` | Only loot/identity/achievement/crafting references, no effect reader in either namespace | Exclude raw marker; preserve original native tool/component mechanics separately |
| `gvozd`, `op` | Potion identity/achievement labels; no effect reader in either namespace | Exclude raw markers; potion effects are native components |
| `randomgovno` | Extra label in four govno loot outcomes; no function/advancement consumer | Exclude raw label; `govnoq` / `eat_shit` retain actual behavior |
| `slimeball` | Sword identity and achievement predicate; no function effect reader | Exclude raw label; original knockback/enchant components are separate meaningful properties |
| `glowingmobs` | No nonstorage occurrence in either namespace | Exclude ineffective generator key |
| six `wisp_*` keys | Source has create_wisp, passive, delete and ownership engines; matching key uses are loot and **display entity Tags**, not held custom_data readers | Do not infer held casting from entity-tag spelling. Actual wisp-book input/native owner remains a distinct unresolved family; source engine is not declared dead |

This is 14 demonstrated live legacy/native-owner gaps, six wisp-family input gaps,
one native identity adapter, one native loot eligibility key and seven excluded raw
labels/UI markers. No unmatched key was silently promoted to a working profile.
Both-namespace scans matter: arbitrary `spellid` handlers in lbcr are not covered
by searching the primary spell dispatcher alone.

## Modern inventory beyond the 776 ready roots

The JSON now includes all 1,092 direct native-item leaf definitions found anywhere
under current `data/lbc/loot_table`, plus 420 literal skywars2 ID candidates, 195
literal custom-data write keys and dynamic enum-key construction evidence from
non-check Java sources. These are complete **static candidate inventories**, not
counts of abilities or all actually registered types. One pre-existing resource,
`reroll_9999/material_block.json`, is empty/unparseable and is explicitly recorded;
it was not repaired or interpreted as an item.

Another 129 Java-referenced canonical prototype/support resources are captured with
their raw custom-data payloads and exact Java references. This catches definitions
whose resource still names the original **vanilla** carrier while a native item
reads its default components, including `LbcImbaAxeItem`'s golden-axe definition.
Direct native leaf enumeration alone would miss this important path. Dynamic
resource concatenation and enum registrations still require the registry snapshot.

`staff`, `upgradable` and `upgrade_available` have been removed from effect keys;
they remain in each exact prototype payload as eligibility/metadata. The additional
raw-marker exclusions above and parameter exclusions likewise no longer create
semantic signatures. The resulting 459 signatures are still **not** executable
sampler families: many combine flags or represent selector outcomes. Actual family
IDs must be assigned by owner and event, with aliases/variants attached to one unit.

The exact exhaustive automation path is registry-backed bootstrap, after all items
and holder-dependent components finalize:

1. Enumerate `BuiltInRegistries.ITEM.entrySet()` once for namespace skywars2; capture
   every `item.getDefaultInstance()`, native identity/class, CUSTOM_DATA copy and
   gameplay components. This catches enum/JSON registrations and identity-only items
   that static string scans and ready roots omit.
2. Decode all canonical SNBT with `TagParser.parseCompoundFully`; retain **string**,
   boolean, list and compound values. The JSON's numeric flags are an advisory view;
   `rawCustomData` is the source truth. Combine native defaults and actual canonical
   loot modifiers using their real codecs/functions, inner reference before outer
   modifiers. Never reconstruct an item solely from the numeric view.
3. Reconcile every native registered item and supported canonical outcome against an
   explicit semantic owner/event family or a concrete nonability/service exclusion.
   Preserve current/old/upgrade/mode/Tiny/outcome domains inside that family's private
   state. Count/style/model aliases and acquisition weights do not add sampler weights.
4. Construct/cache the finite catalog per registry generation and rebuild on reload;
   draw/use/tick paths must not scan registries, parse files or reflect over owners.

The audit performs no JVM bootstrap, so it cannot honestly supply the resulting
default-component snapshot or assert all modern registrations are closed. This is
an exact implementation/qualification step, not a request to fabricate prototypes.
