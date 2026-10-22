# Library drop port — qualified

22 October 2026. Normal and old Library drops are implemented and qualified in
two actual isolated mode1 matches. The tested build is ready, not installed.

## Source contract

Read-only source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
`loot_table/barriersnottraps.json:50–92` defines one repeating command block,
weight 10 / quality 5, `lbc:tier_d`, `lbc:drop_not_trap`, yellow nonitalic
`drop_library`, one white nonitalic `trap_desc` lore line, `{library:1,drop:1}`.
There are no conditions on this entry. Pool weight/quality are acquisition pool
metadata; the standalone leaf returns one native token without activating pools.
`loot_table/legacy_luckyblock.json:1185–1203` defines one plain barrier, empty
set_components and the identical flags; no custom name, lore or tier/model is
authored. Its own native prototype preserves that distinct presentation.

`function/drops/specialdrops.mcfunction:39` selects nearest any nonspectator at
the token, then runs `drops/library` at that player's fractional position. Both
tokens carry the same library trigger and reach that same function. The payload
has 16 ordered strict block commands, followed by native lapis32, one XP orb
Value1395 and one ordinary XP bottle at +(1,1,0); no rotation or general interior
clearing. See [source audit](NEXT_DROPS_AFTER_ENZAN_SOURCE.md).

## Native items and public leaves

`LbcLibraryDropItem.Kind.NORMAL.item()` is `skywars2:library_drop`;
`Kind.OLD.item()` is `skywars2:library_drop_old`. `register()` installs the
DefaultItemComponentEvents model override, essential for Polymer defaults.
`matches(stack)` requires a nonempty stack and both integer custom-data flags,
matching source dispatch and existing drop compatibility. Copied source flags on
other item types remain accepted; unflagged vanilla lookalikes are rejected.
`kind(stack)` selects OLD only for the native old item, otherwise NORMAL for a
matching stack; `matches(stack,kind)` compares that result. Both kinds expose
`flag()` as `library`.

Public leaves: `lbc:ported/library_drop`, `lbc:ported/library_drop_old`. No full
Lucky Block, legacy, imba or trap pool is enabled by these resources.

## Resource-pack audit (read-only)

Current pack: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`.
`assets/minecraft/lang/{ru_ru,rpr,en_us}.json:2609,2611` already contains:

| Key | RU and RPR | EN |
| --- | --- | --- |
| drop_library | Дроп: Комната зачарвоаний | Drop: Enchantment Room |
| trap_desc | Активный Дроп. Для активации выбросте предмет. | Active Drop. Throw the item to activate. |

Literal author spelling is retained by translatable components. Existing
`assets/lbc/items/drop_not_trap.json` points to `lbc:item/drop_not_trap`;
tier_d frame/background sprites and their metadata exist. No translations,
models, template files or pack files were written. Old presentation uses only
its vanilla barrier proxy/model plus source flags.

## Native implementation

`LbcLibraryDrop` executes all 16 commands in order. The enclosing block positions
come from the recipient's fractional coordinates; rewards keep the fractional
offset. Only the four authored doorways are cleared. Strict writes preserve
native container deletion without ejection. Each fill retains its individual
height and modification-limit gate; a rejected command does not suppress later
commands. The new state-aware `LbcDropBlocks.setBlock` follows native setblock,
which has no fill-volume gamerule check, and preserves the downward end rod.
The existing fill helper's behavior is unchanged.

Nearest selection reuses common role guards; self, allied and Creative players
remain eligible. Actual thrower terrain attribution uses the shared WILD write
scope. The dispatcher performs loaded-chunk preflight, source-order dispatch
before Lucky Pigs/Enzan, and one shared token debit per active tick. It neither
loads chunks synchronously nor adds mana, cooldown or antimagic requirements.
No eligible player still consumes one token, as in the source.

Reward creation uses native COMMAND entity creation and source NBT loading:
one lapis32 stack, one orb Value1395, one ordinary XP bottle with zero initial
motion. No direct XP grant or orb splitting replaces the summon. Managed rewards
use active handles, persisted epoch tags, load/unload callbacks and quiet discard
at match stop; Stand pauses native entities and pending activation. Native bottle
offspring and orb merging retain vanilla behavior and ordinary map reset, without
an invented ownership/lineage or persistence guarantee.

## Qualification

Build and two actual isolated mode1 matches passed on `0e005c40`: 78 recorded
suite results, 83.52 seconds including startup, maps and regressions. Native
checks cover both public leaves/prototypes and Polymer roundtrip, exact final
geometry and untouched sentinel cells, fractional rewards/defaults, container
replacement, per-command limits0/1/2/3/8/9/24/25, recipient roles, legacy dispatch,
multi-flag source order/debit, actual dropped-stack ticks, epoch/unload callbacks,
time-stop pause and actual match cleanup. All preceding special-drop suites and
their stop probes/cleanup run in both matches.

The stop fixture preserves prior probes' pending tokens and falling actors.
An observed pre-existing Cage fixture cleanup iterator mutation was corrected
by collecting entities before discarding them; gameplay cage behavior is unchanged.
An initial synchronous Library stop-probe admission failure did not reproduce
after diagnostics were added; its cause is not established. Evidence is retained
in `build/verification/library-drop-final/attempt1/`, without claiming a production
fix for that failure. Another failed run records the diagnosed Cage fixture issue.

Ready artifact: `build/verification/skywars2-library-drop-0e005c40.jar`, SHA256
`0e005c401798ac018e4d0ce92dbf884b96dc4268b42a77ce4dd9ec444ad53a92`.
181 Java sources, 531 compiled classes and 2673 processed resources match the
source/release JARs. Both public leaves are explicitly whitelisted in the build.
Evidence: `build/verification/library-drop-final/qualification.json`, results and
server log. Hardened runner restored isolated files and released both locks.
Main JAR96ee50a7 and user Reroll source6ef80947 are unchanged; RP/template unchanged.
Only the established fake-bot Polymer packet-context ERROR pair remains.

Real client pixels/packets, actual disk unload/restart persistence and peak
capacity are not qualified. Height-limit behavior and terrain causality are
source-reviewed/reuse shared hooks, rather than separate Library death fixtures.
Next bounded acquisition audit: [Bees](NEXT_DROPS_AFTER_LIBRARY_SOURCE.md).
