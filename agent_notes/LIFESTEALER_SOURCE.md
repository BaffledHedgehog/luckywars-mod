# Lifestealer source audit — 2026-10-21

Read-only audit; no Java/resources/server changes or runtime verification. Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`. Paths below relative to this root. Read project AGENTS/ARCHITECTURE and agent_notes/AGENTS/RESOURCE_PACK.

## Item and input

## Уточнение при переносе — 21 октября 2026

Исходный аудит сохранён как описание legacy. Код обычного/Academy жезла
теперь перенесён; build и два настоящих матча на `ba2e46e6…` прошли.
Контракт — [LIFESTEALER_PORT.md](LIFESTEALER_PORT.md).

Native `EffectCommands` для `/effect give ... instant_health 1` задаёт
**один тик** мгновенного эффекта. Для Regeneration аргумент `1` означает
**20 тиков**. Формулировка «1s» для Instant Health неточна; усилители прежние.

Решения пользователя, заменяющие описанные ниже ошибки legacy:

- Лечение и испытание считают только цели, принявшие попадание через `hurtServer`.
- Заряд взрывается при первом столкновении, включая столкновение рядом с владельцем;
  самого владельца и союзников защищают общие проверки целей.
- Выбор режима разрешён с любой маной без оплаты и бонусов каста.

Academy требует 15 принятых попаданий за один обычный каст и проверяет
использованную руку; случайная привязка legacy к MAINHAND не воспроизводится.
Отдельного исключения Creative нет. Общие цели/антимагия/щит, native owner,
отражение и эпоха матча используют существующие службы. Исходные cactus/generic,
урон 1/5/10, ступени лечения, скорость 4,5, срок 40 активных тиков и радиус 7
сохранены. Полные ресурсы/прежние ключи проверены; RP только читался.
Результаты серверного прогона описаны в контракте переноса и SERVER_TESTS;
исторический аудит ниже остаётся описанием исходника.

### Исходные предмет и ввод

`lbc/loot_table/transformables_staff.json` entry: carrot_on_a_stick, count1, model `lbc:vampirewand`, tooltip `lbc:tier_d`, name translate `vampire_wand` (italic false), custom data `{transformable:1,staff:1,magicwand:1,lifestealer:1}`. Lore preserves aqua type/staff, magic_damage 1, vampire_wand_mana_use score/mana_on_activate, radius20/cast_radius, white nonitalic `vampire_wand.lore.1..4`. Related payloads: `legacy_luckyblock.json` uses lifestealer_old; `rofls.json` is another vampire model entry; `spells_library.json` contains spell payloads. Do not conflate old/new flags.

`workingitems/spell1*.mcfunction`: right-click dispatch reads upgraded flag and selected mainhand/offhand. `spell1_default[_left]` and `spell1_upgraded[_left]`: lifestealer1, mana >= vampire_wand_mana_use (config250), no stopper_magic marker within20. Pre functions cast then subtract250 and run mage-set manamax bonus/trinket cast_wand callback. Upgraded sneak changes between modes1/2 with mode_caster/mode_charge messages; no cast/payment in that branch. Legacy erroneously gates switching on mana; project user rule explicitly supersedes that. Offhand adds temporary left tag and reads equipment.offhand mode; pre_left leaves left behind on sneak (legacy bug).

## Active cast

`workingitems/lifestealer.mcfunction` base: hostile mobs within20, count distance0.1..20; damage distance0.01..20, excludes spectator and nomagic_active and same_team. Damage cactus1, or5 with have_stand + stand_clocks_active + overheaven<=0, or10 overheaven>=1. Count is eligible entities, independent of accepted damage. Heal count1..3/4..6/7..9/10+: regeneration1s amps2/3/4, instant_health amp4; active Stand replaces those with instant_health amps0/1/5/20, Overheaven1+ amps1/2/10/40. No count means no healing. Same Stand tests in `lifestealer_no_stand`, `_stand`, `_stand_x2`.

Base sends player_vampirism to nearby non-spectator enemy players, hiss volume1.5 pitch2, 200 damage_indicator particles spread12 at y+1. Base only: vampire advancement checks friendly nonplayer mob team1..32 within20; upgradable selected MAINHAND + count>=15 calls Academy can_upgrade. Academy `upgrade/lifestealer` only applies tier_a modifier; common Academy flow supplies upgraded/mode flags. No item-specific crafting recipe found in source references; acquisition is transformables staff loot and Academy upgrade.

Upgraded mode1 (`lifestealer_upgraded_mode_1`) same radius/heals but generic damage (bypass cooldown via pack tag). Count/message omit nomagic exclusion while damage keeps it: inconsistent legacy behavior requiring decision, not silent reproduction. No base advancement/upgrade callback.

## Mode2 flight / impact

`lifestealer_upgraded_mode_2`: hiss1 pitch2, marker at eyes+1 forward (library/random call sites can spawn at eyes), lifetime40, tags life_steal/slowraycast/magic/other, copies rotation, owner lbcID2 and team_number. `tick_not_timestopped` -> raycastselector -> raycastselector_other -> lifestealer_charge. Each active tick increments Lifetime; charge visits15 samples spaced0.3 (4.5 blocks/tick), increments Distance; last sample resets Distance and teleports marker. Lifetime40 active ticks => nominal180 blocks. Solid/non-air sample or mob overlap calls impact. Collision probe uses mobs dx/dy/dz0 and distance<=15, excludes spectator but not owner/team/nomagic. Continuation after impact is not explicitly returned: killed marker commands may still continue (legacy ambiguity).

`lbcr/function/dmg_bypl_1nocd_vampire` -> dmgwithid_un_playeronly tags non-spectator owner nodamage, same-team PLAYERS nodamage1. Unless owner within3 of impact, invokes kill. kill selects mobs radius7 excluding nodamage/nodamage1/spectator; each gets generic1/5/10 by owner's CURRENT Stand/Overheaven, increments count regardless of damage acceptance. Heals tagged owner anywhere via vampirism, then kills marker; tags cleared globally. No nomagic filter, allied nonplayer mobs are not excluded, missing owner still permits unowned radius7 damage with no heal. No attribution source in legacy damage commands; target migration requires owner attribution per user rule. Close owner suppresses impact without marker removal. These are bugs/ambiguities to resolve before implementation.

## Clocks passive

`players_10t.mcfunction` calls `other/stand_arrow/tick_10t_stand_clocks` every10 ticks for have_stand + have_stand_clocks_active unless stopper_magic within20. Deals owner1 in_fire unless Inventory contains custom_data lifestealer1 anywhere; upgraded status/mana do not matter. lifestealer_old does NOT protect. Inventory covers normal carried slots/offhand, not ender chest. `items/changed_inventory/sel` sets have_stand_clocks_active from container.* or weapon.*; stand_clocks_active is added only from container.* but removed only if both container/weapon absent (offhand-only activation asymmetry). Same passive also tests y>=20000 and overheaven<=0 to call stand_clocks_overheaven_upgrade; separate from Lifestealer protection.

## Resource pack (read only)

Actual inspected root `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8` (different from older RESOURCE_PACK.md 26.3(1) path). `assets/lbc/items/vampirewand.json` -> `lbc:item/vampirewand`; model `assets/lbc/models/item/vampirewand.json` parent minecraft:item/handheld, layer0 minecraft:item/vampirewand. Existing `assets/minecraft/lang/{ru_ru,en_us,rpr}.json` includes vampire_wand, lore1..4, player_vampirism, upgrade_succesful_lifestealer, upgrade_condition_lifestealer. Russian name Жезл вампиризма; upgrade says no damage cooldown + projectile area mode; condition explicitly15+ mobs simultaneously. Existing model and translates need no duplicate template.

## Reuse boundaries

ARCHITECTURE points to LbcWands two-hand/mode routing, LbcMana.trySpendWand, LbcStand current Stand state, LbcTimeStop clocks/lifecycle, LbcIndirectKills ownership and existing target helpers. Preserve active-tick lifetime, shared team/NPC/spectator guards, current match epoch cleanup. This note records source, not a completed port.
