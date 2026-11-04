# Native mode3 BedWars shop - 4 November 2026

Implementation prepared; root owns compilation, exact artifact inspection and actual mode3 qualification. No build/server/commit, shared controller/menu/auxiliary edits or RP/template mutation was performed by this agent. Protected author WIP is unchanged. Source analysis is [NIGHT_BEDWARS_SOURCE_2026_11_04.md](NIGHT_BEDWARS_SOURCE_2026_11_04.md); original server and actual author RP remained read-only. TMT LAST; mode2/4 remain gated. These source defaults are provisional pending the root-owned author question queue, not claimed author approvals.

## Stable integration contract

New `LbcBedwarsShop(Policy)`; `Policy` has:

- `String epoch()` using existing `game.items().epoch()`;
- `boolean canTrade(ServerPlayer, Entity actualShop)` supplied by root for active mode3, enrolled live player, current role/index/arena, actual dimension/distance and shared TimeStop/input policy;
- `int team(ServerPlayer)` in1..16;
- `ItemStack teamChest(ServerPlayer)` / `teamZombie(ServerPlayer)`, pure stack producers forwarded to `LbcBedwarsAuxiliaries`;
- `void beacon(ServerPlayer, LbcBedwarsShop.Beacon, int seconds)` forwarded only after debit acceptance.

`Beacon` enum is exactly SPEED2,JUMP2,FIRE_RES,STRENGTH2,HASTE2,REGEN2,RESISTANCE1. Aux owns durations/effects/identity/lifecycle. Shop selects source durations300,300,900,120,600,180,300 and costs5stars each. No root getter is assumed inside either new class.

`boolean open(ServerPlayer,Entity)` requires the actual living/current Villager object with source `bedwars_shop` tag, same dimension, nonobserver connected player and supplied policy. It returns false on rejection. Root's ordinary actual-villager interaction must invoke this before vanilla trading. Every top click and redirected input checks original body/epoch/team and current policy again. Root calls `stop()` on end/reset/retirement to close menus. No MAG/TECH gate is added; actual player/world input freeze belongs in canTrade.

No tick service or private checkpoint is required for purchases: each is one synchronous native inventory/body publication transaction, no accepted queue/cooldown/RNG/private rewind. Menu state is ephemeral and epoch-gated. Root should close it on whole-scene replacement before resumed input; gameplay inventory/bodies remain native coordinator-owned.

## Complete finite production implementation

`LbcBedwarsShop.java` implements all77 actions across8 source tabs. Category action counts16,15,7,5,8,14,5,7; all source slots/prices/outputs match the audit. Tab6 is empty; no Lucky block/loot table/TMT/function-interpreter edge exists. The finite table is Java typed definitions, not a new parser. All registry lookups occur only while opening/rebuilding a real menu; no inventory NBT/tick or world scan.

GUI preserves54slots, original title/category keys, material icons, ITEM_NAME components, enchant levels, dyed armor/team blocks, source potion types, source price component ordering/style and the separate bare currency keys in upgrade/beacon price lines. Actual separator is PALE_HANGING_MOSS, empty literal ITEM_NAME, hide_tooltip=true (source comment's VINE is stale). Actual shop61-key class includes one Lucky-only key; mode3 reuses the other60 unchanged. Their current actual RU/EN/RPR values and file SHA-256 are recorded in ignored `build/verification/bedwars-native-2026-11-04/shop/static_presentation.json`; no localization was copied into the mod.

Physical copper/iron/gold/diamond/star currency counts every native inventory/equipment slot, accepts any matching material stack regardless components, computes affordable repeats, then plans the entire multi-currency debit before committing. Item output plans source empty-slot armor auto-equip, existing-stack merge, storage insertion and overflow. An overflow ItemEntity is published before committing the inventory plan; rejected publication leaves currency/inventory intact. A reentrant inventory/role change discards that body and leaves the plan unpublished. Native inventory equality uses full components/count. Accepted output/body and inventory commit happen synchronously; root never snapshots a half transaction.

Preserved source quirks: ordinary shift uses ceil(maxStack/gives), then output clamps one maxstack (wool22purchases->64 for22copper;glass7->64 for7iron). Leather chest source gives2 clamps to1 and equips one. Team chest/upgrades/beacons buyonce regardless shift. Empty upgrade still spends currency. Bow UI key labels retain source enchant level mismatch. Upgrade caps from source comments are ignored as actual code does; full positive integer levels saturate at Integer.MAX_VALUE through the existing native ItemEnchantments/EnchantmentRules projection policy. Existing effect ownership remains external to this shop.

Double visits were CONFIRMED in actual source Paper1.21.11 bytecode, not guessed: CraftInventory.getContents delegates to NMS Inventory.getContents, which appends all equipment; source EnchantOps then visits armor and offhand again. Native general upgrade iteration therefore visits all current native slots once and36..40 again, retaining two passes for offhand bows/weapons/KB items. Armor-specific upgrade visits only four equipped armor slots once, as its separate source helper does. Bytecode evidence is `shop/{CraftInventory,Inventory}.bytecode.txt` in the same ignored directory.

Standing current-version full-enumeration instruction is applied to vanilla weapon/tool families: current native minecraft-namespace swords/axes/spears/trident/mace; pickaxes/axes/shovels/hoes/shears. This includes newer copper materials and spear families without arbitrary custom LBC item acquisition. Autoequip uses current vanilla armor tags/equippable slots so new armor materials qualify, while heads/elytra/pumpkins are not treated as the source armor family. Source bows remain actual BOW only, not crossbows. This current-version adaptation is recorded distinctly from source quirks.

SGui intercepts top transfers, forged number-key swap, collection/double click, drag and shift. Ordinary lower-inventory moves are delegated to vanilla; lower shift/double/drag are intercepted. Every actual callback synchronizes the native menu/cursor. No callback grants the displayed item directly. Source success/failure sounds play twice (currency helper plus listener), and category sound uses source click pitch.

## Qualification entry

`BedwarsShopChecks.start(GameController,LbcBedwarsShop,Entity actualShop)` / `tick(GameController)` require an actual admitted mode3 match, enrolled TestAlpha, a real indexed source shop within trade distance and no pre-existing TimeStop/exemption. Root supplies/places that fixture; this helper does not fabricate an engine/NPC or change mode admission. It uses the existing ScheduledClient native connection and real ServerboundContainerClickPacket input, and existing player snapshot restoration. Root should not invoke until real mode3 root ownership exists.

Prepared checks cover finite77/8 source slot counts, hidden Lucky tab, actual separator; mixed-currency atomic failure then successful debit; physical tagged/offhand currency; source wool/glass shift economics; leather chest autoequip; actual full-inventory body output; paid no-op; confirmed offhand bow double upgrade; integer saturation; forged swap/collect/drag and lower transfer interception; lost actual shop tag/observer rejection; positive native MAG/TECH markers with successful ordinary purchase; actual shared TimeStop across real server ticks with no debit; retired GUI rejection. Scoped new item bodies/jammer/player/connection state are restored.

Runtime result is NOT claimed. Root still owns additional family-wide purchase matrix (all77 outputs and exact source prices/components), concurrent role/epoch/death transitions, source shop protection/auxiliary effects, whole-match checkpoint replay and any required fixture corrections, plus ordinary mode1 regression. No physical-client rendering, peakTPS or live installed-artifact claim is made here.
