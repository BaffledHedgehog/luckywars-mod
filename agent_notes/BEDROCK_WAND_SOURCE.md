# Bedrock Wand source notes

Legacy source is read-only: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

## Author decisions — 17 October 2026 (user)

Room recall (user): «вообще - я хочу чтоб при установке комнаты сервак
запоминал что было на месте бедрока, и при отзыве комнаты возвращал блоки
обратно. Этого не было из-за тех. ограничений старого датапака.»

Admission threshold (user): «Допуск от 2500, исправить границу».

Academy trial with a solid 21×21×21 cube and immediate upgrade readiness:
«Да, сохранить».

Anti-magic beacon (user): «вообще антимагический маяк даже разово установленный
в области комнаты должен отменять её дальнейшие преобразования (т.е. маяк не
убирает комнату, а убирает воздействие жезла на неё. И пользователь жезла далее
на ПКМ будет создавать новую комнату у себя, ведь даже не сможет убрать предыдущую.)
Маяк стирает 'метку' комнаты навсегда».

Shared cube removal (user): «Да, можно убирать и чужие кубы».

Target decisions: charge threshold is inclusive (`>=2500`) with a 2500 debit;
recall and Academy modes 3–5 are free. Recall restores original block states,
full BlockEntity data (including container contents), and structure light cells.
Overlapping rooms can be recalled in any order; shared cube removal remains
allowed without an owner filter. Academy trial checks the item in the hand used
for the click, fills the solid 21³ bedrock cube, and grants readiness immediately.
An anti-magic beacon permanently cancels room control and leaves existing blocks
in place; this applies to normal and regenerative rooms, including when the
beacon is removed later.

## Legacy source behavior

- Recipe/output: `loot_table/tuntijaa_stone_wall.json`; crafting dispatch in
  `function/other/craft_ground.mcfunction`, `function/other/craft/inventory_craft_check.mcfunction`,
  and `function/other/sauvojen_tuntija/crafted_wallstone{,_inv}.mcfunction`.
  Inputs are one `ststone` and one `wall` wand. Ground crafting removes matching
  item entities wholesale; inventory crafting reduces matching slots. The new
  native recipe deliberately consumes one of each.
- Entry dispatch: `function/workingitems/spell1_default{,_left}.mcfunction` and
  `spell1_upgraded{,_left}.mcfunction`; both hands are routed. `stopper_magic`
  within 20 blocks blocks casts. `function/config.mcfunction` configures 2500;
  legacy room-creation functions test strict `mana > bedrock_wand_mana_use`,
  so exactly 2500 fails in the old datapack. The target behavior above fixes
  that boundary.
- Ordinary click toggles the owner-marked room. Academy modes 1 and 2 toggle
  ordinary and regenerative rooms; mode 3 uses `structure_void` in a 3³ bedrock
  subset, mode 4 places an unowned 5³ bedrock cube marker, and mode 5 clears
  marked cubes within seven blocks of the ray hit. Mode 5 can therefore remove
  another player's cube; the user explicitly retained shared removal.
- The room template `structure/bedrock_room.nbt` is 21×11×21 and has 3726
  bedrock blocks, 210 light blocks, and no air entries. Regen fills template
  shell air only: floor y −5..−3; ceiling y 3..5; and perimeter walls, with the
  middle interior left open. These template counts and the placed shape remain
  relevant to the port.
- The legacy trial is reached from `function/other/magic_academy/interacted{,_upgrade}.mcfunction`
  and `can_upgrade.mcfunction`. The old offhand path ultimately checks
  `SelectedItem` (main hand), a source bug; target behavior uses the actual
  interacting hand as decided above. Legacy test fills bedrock at offsets
  −10..10 on all axes and performs readiness through the Academy path.
- `function/tick.mcfunction` skips `tick_not_timestopped.mcfunction` during the
  global time stop; that dispatch owns regenerative-room ticking. Legacy room
  creation/cast dispatch checks nearby anti-magic, but the old active regen tick
  lacks that check. The target beacon behavior is permanent room cancellation,
  not a temporary pause.

## Port implementation comparison

`src/main/java/me/skywars2/game/LbcBedrock.java` is the native implementation;
`LbcAcademy.java` owns the trial and `LbcMana.java` the inclusive spend gate.
Current code uses a 2500 cost as both required mana and debit, with Overload
allowing the cast while still debiting real mana. Its Academy trial uses the
passed `InteractionHand` and stack. See [BEDROCK_WAND_PORT.md](BEDROCK_WAND_PORT.md)
for concrete mechanics and the current test boundary.

## Source index

`loot_table/tuntijaa_stone_wall.json`; `function/other/craft_ground.mcfunction`;
`function/other/craft/inventory_craft_check.mcfunction`;
`function/other/sauvojen_tuntija/crafted_wallstone{,_inv}.mcfunction`;
`function/workingitems/spell1_default{,_left}.mcfunction`;
`spell1_upgraded{,_left}.mcfunction`; all
`function/workingitems/awaken_tuntija_wands/bedrock/*.mcfunction`;
`function/other/magic_academy/can_upgrade.mcfunction`;
`function/other/magic_academy/interacted{,_upgrade}.mcfunction`;
`function/config.mcfunction`; `function/tick.mcfunction`;
`tick_not_timestopped.mcfunction`; `time_stopped_tick{,entity}.mcfunction`;
`raycastselector_other.mcfunction`; `structure/bedrock_room.nbt`.
