# Neutral wand and Feather of Greed

Scoped native entrances: `lbc:ported/neutral_wand`, `academy_neutral_wand`,
`feather_greed`. Broad staff/Greed/Elemental reward pools remain gated. Source
craft-with-Elemental lore is retained, but this batch does not qualify the still
unported Elemental branch or the complete Greed encounter.

## Source

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
- `function/workingitems/neutralmage/{select,select_pre,check_upgrade,select_upgraded,
  select_upgraded_pre,select_upgraded_pre_left,select_upgraded_pre_2,1,2,3,4,5}.mcfunction`.
- `function/other/magic_academy/{can_upgrade,upgrade}/neutralmage.mcfunction`.
- `function/config.mcfunction`:neutral_wand_mana_use1200.
- `loot_table/transformables_staff.json`, `item_modifier/add_upgrade_desc.json`,
  `item_modifier/tiers/a.json`, `loot_table/feather_greed.json`, and
  `function/workingitems/feather_greed.mcfunction`.
`tools/import_lbc_neutral.py` preserves the original blue neutral name,11-row
lore, tier B ->A Academy, flags and model; feather retains tier D, vanilla
feather model, glint, name, single lore row and its two original flags.23 reused
RU/EN keys/model were checked against the read-only author RP; no RP edit.
Original firework and potion NBT is extracted verbatim once into
`skywars2/neutral_wand_presets.json`, decoded once and loaded through native APIs.

## Neutral casts and native ownership

Ordinary cast randomly chooses1..5. Academy Shift+PKM chooses modes freely before
mana admission, including negative balances (user); an actual cast costs1200,
then invokes the existing mage/trinket cast hook once. Original caster-only
wither-shoot sound retained. Parallel configured hand dispatch pays separately.
Mode1:three native WitherSkulls. Mode2:three native SmallFireballs.
Mode3:one native LargeFireball, ExplosionPower2. Mode4:three native firework
rockets, LifeTime100 and ShotAtAngle. White/blue4607/red payloads each retain four
original shapes (small/large ball, star, burst), fades, trail and twinkle.
Mode5:three native splash potions with glint/color16719841 and Weakness, Poison,
Wither, Glowing IV for60 native ticks; native splash attenuation remains.

Origins are at eyes+forward1, with lateral0/+.1/-.1 (rockets:-.1/0/+.1).
Motion is the original local marker vector quantized to.001:forward5 and side.5
for skulls/potions, forward3 and side.3 for fireballs, forward1 and side.1 for
rockets. Native acceleration, gravity, impact, explosions, visual and lifespan
remain native; only rotation is initialized before insertion. No new renderer,
particle copy, per-step ray, homing, arbitrary TTL/range or fixed lifetime is added.
Existing LbcItems epoch/load/unload/death/stop and safe deferred native unload
own a small set of these real projectiles. TimeStopNativeMixin pauses native ticks.
No separate entity/actor tick framework is introduced.

Actual ownership follows all direct/deferred damage and deliberate reflection
(user). Generic wand protection excludes caster/allied players/technical roles;
friendly mobs remain physical targets (user). Both area damage and direct hits
consult current native owner. Small fireballs use the existing ignition-cause
hook. Native potion effect sources retain delayed Wither/Poison attribution;
later player attacks still override older item effects. Antimagic handles these
magical native projectiles. Incidental explosion/fluid/projectile redirection is
blocked through existing protected-wand policy (user); explicit reflection remains.

An ownership-specific vanilla difference is deliberately prevented:the source
ownerless skull deals5 magic damage without owner healing or owner enchantment
post-hit effects; owned vanilla skulls would instead deal8 and heal5 on kills.
NeutralSkullMixin changes only tagged neutral skulls to preserve the original5
magic damage/no healing/no newly added enchantment callback, with actual source
owner attached. Unrelated vanilla/cultist skulls keep their native behavior.

## Academy and feather

Neutral trial stores type_combo/score on the actually used stack. First result
starts score0; two repeats increase it to2, so three identical consecutive casts
complete the trial. Changing type resets the count. Each hand/stack is independent;
no temporary block copy, shared marker selector or selected-mainhand mistake.
Existing Academy admission/ready/promotion applies source tier A and added lore.

Feather:PKM gives native visible Slow Falling I60 seconds, original white dust
and dragon flap sound. No mana admission/debit, consumption, cooldown or
mage/trinket cast hook exists in its source function. Negative mana still permits
use; native role/action/antimagic/Time Stop admission applies. Existing native
effect clearing owns its lifetime. No new custom timer/KFC counter is added.

## Verification scope

NeutralWandChecks uses native server item use, complete presentation and serialized
payloads, exact source counts/Motion/origins/initial rotation, native skull
collision/damage/no-heal/friendly-mob/current-reflector credit, native fireball/
firework impacts, splash effects and delayed Wither death. It also checks stack
trial isolation/offhand use, free switching, admission/both-hand costs, feather
at negative mana with a full mage set, real successive-tick Academy promotion,
actual three-tick dimension freeze of all13 projectiles and actual quiet match
stop of all five native families. Fixtures wait for native entity visibility.
Manual native ticks are used for precise collision/effect boundaries; client
pixels, actual disk restart/unload/disconnect, full natural trajectories and
peak-capacity behavior are not claimed. tools/smoke_neutral_wand.py runs two
actual mode1 matches with Blood/Confuse, Evoker/Bass, Lifestealer, Academy and KFC
regressions. Final evidence:build/verification/neutral-wand-final/.

## Qualified release - 23 October 2026

Builda6e485fd passed18 recorded suite results in two actual mode1 matches
(51.53s), exact269sources/726classes/2746resources match tested artifacts. Installed
main while stopped; rollback46db086d retained, main not started. All three
preexisting user source hashes remain unchanged and excluded from the commit.
All final suites passed. The only final ERROR pair is known fake-player Polymer
packet-context output. Evidence:build/verification/neutral-wand-final/. Rendering,
restart/unload/disconnect, full natural trajectories and peak load remain outside
this qualification; native collision/effect boundaries include manual ticks.
