# Big Bob original consumer — implemented; staged native checks passed

Read-only legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
No Gradle build, native check, server run, resource-pack edit or commit was performed
by this implementation worker. Root integration and native qualification remain.

## Exact behavior

`lbc/function/workingitems/big_bob/click.mcfunction` has exactly two commands:

```mcfunction
loot spawn ~ ~ ~ loot lbc:oreshki_10
playsound ui.cartography_table.take_result master @a ~ ~ ~ 1 1
```

Both `workingitems/spell1_default.mcfunction:109` and
`workingitems/spell1_default_left.mcfunction:95` invoke it at player position +Y1.
Neither admission has a MAG/TECH marker condition. There is no mana charge,
cooldown, item debit, boss, helper creeper, explosion, kill event or boss reward.
The separate Bob encounter must not be inferred from this item's name.

`lbc/loot_table/big_bob.json` retains its raw carrot-on-a-stick identity and
`{magicwand:1,big_bob:1}` components. `LbcBigBob` reads the raw flag and does not
rewrite the source stack. It rolls the existing original `lbc:oreshki_10` at the
source position and inserts native item entities with current thrower/epoch.
Normal role, dimension, stage and Time Stop admission remains shared.

`oreshki_10.json` has ten rolls of max-stack-one repeating command blocks with
`eat_oreshek:1`, original presentation and native Instant Health III consumption.
`LbcOreshek` already owns the independent random-debuff, explosion, Nexus,
Zelensky/nuclear/drone and return/death-marker continuations. `OreshekChecks`
already tests that owner. Big Bob deliberately adds no competing food lifecycle.
Native rewards remain canonical actors for the existing Vanity world snapshot;
the stateless dispenser has no separate checkpoint or tick state.

## Integration and checks

In `LbcWands.cast`, before generic spell matching:

```java
if(LbcBigBob.matches(stack))return LbcBigBob.use(game,player,hand);
```

Expose an immediate `big-bob` native check calling `BigBobChecks.run(game)`.
There are no item registrations, tick, stop, entity, kill or Vanity hooks to add.
The fixture uses actual native game-mode input for both hands twice in the same
frame; compares ten source-table rewards including all components; checks source
position, thrower, unchanged balance/stack, disabled offhand and frozen admission;
restores player/world RNG and native item-use stats as well as the shared fixture.

## Additional audited consumer gaps

Production Java searches (excluding checks) found no consumer for the following
actual source admissions. These are findings, not implementations or release
qualification. A reachable table alone does not establish complete full25 closure.

|Flag|Concrete original loot path|Original missing action|
|---|---|---|
|`unstable_spellbook`|`luckyblock -> lucky_items -> transformables_staff`|Paid right/offhand `workingitems/random_spell`|
|`anal_whip`|`luckyblock -> lucky_items -> transformables_sword -> anal_whip`|Right/offhand `ender_whip_teleport`, item `teleportid` matched to target UUID first word|
|`boba_eblan`|`luckyblock -> random_glack -> random_glack_1 -> loliluckyblock -> boba_eblan`|Consume one, invoke `rotatemap` macro; separate bedwars branch|
|`uniwand`|`grimoire_boss -> uniwand`|Source custom-mana cast; full25 reachability to this boss table still needs root confirmation|

`banana_palm` has a simple source dispenser/debit consumer but no loot-table raw
flag leaf was found, so it is not asserted to be a reachable release blocker.
Other absent dispatcher flags include kit/admin/event and old-wand families;
they require reachability qualification before adding them to the release scope.

## Root integration and native evidence (31 October 2026)

Integrated through native raw-use admission and the existing food/reward owners.
Actual `big-bob` suite passed on `d05b18dc`; this component is unchanged in final
`0794b535`. See [batch evidence](DAMAGE_REACTIONS_AND_REWARD_CHAIN_2026_10_31.md)
for staged runs, preservation and release limits. The earlier worker paragraph
records who performed the checks; it is not the current verification status.

Source-audit correction: the leaf dispatchers have no local MAG/TECH condition,
but upstream `items/spells.mcfunction` artifact-gated the magicwand route. The
initial leaf-only inference missed that upstream condition. The user explicitly
chose Big Bob unjammed, so the implemented behavior remains unjammed. This is an
authorized adaptation, not a claim that every original entry path was unjammed.
The previously listed unstable-book/whip gaps are implemented in this batch;
Boba rotation remains missing and uniwand reachability still needs qualification.
