# Raw consumable raycast spell admission —29 October2026

Implementation staged, bounded native checks pass in candidate8b38434f; see
[exact integration/proof and limits](CHAOS_CHAIN_INTEGRATION_2026_10_29.md).
All seven inputs are now actual native source table leaves, launched through
the Fabric UseItemCallback and shared two-hand dispatcher. Full Chaos release still
requires the complete original25-entry trap pool and entire necessary reward
chain; no pool filtering or new catalog readiness is introduced here.

Owned edits: LbcLibrarySpells.java and, after root's explicit ownership handoff,
LbcLibraryRandom.java; this new note. No resource/table, Items/Wands, server,
resource pack or commit edits. Read repository AGENTS and exact legacy source.

## Actual source closure

Legacy root is `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Current native `lbc/loot_table/transformables_spells.json` emits raw ordinary
carrot-on-a-stick stacks with raycastspell1, spell1 and one subtype flag. No
libspell/spellid is present. Existing LbcLibrarySpells.kind requires a typed
item or actual libspell/spellid, so packaging alone did not admit these stacks.
No independent raw subtype consumer was found in current native Java.

Actual mainhand chain: lbc:function/items/spells -> workingitems/spell ->
lbcr:function/raycast:3 -> lbc:workingitems/raycast_spells/select -> the original
unstable_spellbook raycast loops and matching terminal helpers. The selector's
full source order is retained for these seven bounded aliases:

|Raw flag|Source loop|Existing shared random-library effect|Actual terminal|
|---|---|---|---|
|tnts|9|16|26 real native TNT, fuse56, original-.4/0/.4 motion combinations|
|wither1|11|18|Wither60seconds amplifier0, source smoke/sound|
|heal1|12|19|Native InstantHealth duration200 amplifier4, heart/sound|
|arrows1|14|21|Source firestorm: radius35 fireballs power3,3per2ticks,500ticks|
|lava1|13|20|Original31cube ordered terrain rules, sparse sourcelava, sourceY1..126 lava column,4ghasts|
|anvils|15|22|Source actually falling stone_bricks,10/tick atY310, original hurt parameters,60ticks|
|skeletons|10|17|Phantom-arrow marker1600ticks with original5/10tick behavior|

These exact payload engines already exist in LbcLibraryRandom. No replacement
TNT, creeping explosion carrier, summon approximation or second ray manager is
added. Source aliases have **no mana price**. Each terminal helper globally
clears one corresponding subtype and sets Distance300; a ray that misses has
no terminal helper and must not consume. The range is the source200 ray extent.

## Implementation and root integration

LbcLibrarySpells adds `public static boolean matchesRawRaycast(ItemStack)` and
`public static boolean useRawRaycast(GameController,ServerPlayer,InteractionHand)`.
Requires both actual raw admission flags and one subtype; excludes libspell1
and upgraded1. Upgraded source dispatch has no ordinary spell-subtype path, so
no invented upgraded alias behavior is exposed. Existing typed kind/use/cast
and definitions remain unchanged.

Root hook in LbcWands.cast, immediately AFTER the existing typed library route:

```java
if (LbcLibrarySpells.matchesRawRaycast(stack))
    return LbcLibrarySpells.useRawRaycast(game,player,hand);
```

That retains the existing ordinary-dimension, dual-hand, input and offhand config
framework. The new route also checks normal role/participant, MAG, Time Stop,
map and resident-eye admission. TECH does not block these magical spells. No
libspell tags or typed replacement items are injected into raw authored stacks.

LbcLibraryRandom adds a package-level overload
`ray(player,type,BooleanSupplier terminalAdmitted,Runnable afterImpact)`.
Its original private random-library ray delegates with always-true/no-op
callbacks, preserving typed behavior and its existing page-turn sound. Both
routes use the same Exasperato.searchSpell CPU-budgeted resident/epoch/MAG ray,
particles, range200, block-hit backstep1, WILD role-eligible living target and
same shared impact. No new timer or lifetime owner is created. Raw path adds
no snowball-throw or book-page-turn sound absent from its original entrypoint.

At a real hit, raw terminal admission first checks that a matching item still
exists in native inventory/equipment/crafting. This prevents two queued casts
from producing two payloads from a single item. Shared impact then runs and
the source global native clear removes one matching subtype, updates inventory
notifications and awards the actual used native-item stat. Miss, jammer cutoff,
epoch/owner cancellation or exhausted pending ammo does not debit. No explicit
mana debit or refill is added. Native output lifetimes and delayed causal
attribution stay owned by the existing shared services.

## Source ambiguities and audit corrections

Source offhand raycast_left gates offhand spell1 but invokes the same selector
which reads SelectedItem/mainhand subtype flags. The native route follows the
established configured hand-aware Wands contract, rather than duplicating a
mainhand cast from offhand. This is recorded for author review; the user is
asleep and no chat question is sent. Shared normal ray geometry/MAG policy is
reused: literal legacy loops probe integer points with dx0/dy0/dz0 and a16block
player-MAG continuation gate, while existing native random-library ray uses
the common exact search and common MAG policy. No private discrete ray engine
is introduced solely for the aliases.

**Correction to earlier LBC-only trap graph triage:** the source namespace
`lbcr` is an actual live dependency. The earlier conclusions that krazha and
shetka lacked consumers, or drone_pult was only an unreferenced clear helper,
were incorrect. Actual `lbcr:function/raycast.mcfunction` dispatches:

* line8 krazha -> lbcr:raycast_loop6;
* line20 shetka -> lbcr:raycast_shetka;
* line31 lightning_bolt -> lbcr:raycast_lightning_bolt;
* line32 cunny_slab -> lbc:other/imba/cunny_slab/cast;
* line33 drone_pult -> lbc:items/misc/drone/click.

Corresponding offhand lbcr:raycast_left routes exist for krazha, shetka,
lightning_bolt and drone_pult. These are real remaining mechanics, not inert
metadata. The earlier221-flag classification artifact is only a preliminary
LBC-namespace triage and must not serve as a complete source closure. This
bounded task implements only the seven normal consumable aliases above.

### Native fixture follow-up after child46ea51fc

The preserved child log reports raw-raycast-spells PASS, then Infinity and
Aimbob PASS, followed by all-grimoires-random failing its native wild phantom
self-hit assertion. Read-only inspection did **not** establish a Raw fixture
invulnerable-ability leak: State captures WandThirdPlayerState before
preparePlayer, and that shared snapshot explicitly saves and restores
abilities.invulnerable after restoring game mode. Raw's later attribute,
profile and attribution restoration does not write the abilities object.
No production fix or unrelated fixture edit is justified by that evidence.

Raw teardown now asserts exact original native ability booleans (including
invulnerable), mode, noPhysics/noGravity, damage immunity timers and tags after
all player/attribution restoration. It preserves a borrowed true value rather
than forcing false. These assertions are newly staged and not runtime proven.

All raw inputs now come from the actual registered native
lbc:transformables_spells table. The fixture expands all ten original leaves
with a fixed native loot context and seed17, checks one singleton output per
leaf, and retains the seven matching authored aliases. It checks source flags,
count1, max stack64, presentation components and absence of invented libspell
metadata. Each callback receives a component-identical copy with count2; no
synthetic custom-data substitute remains. This deterministic leaf expansion
does not consume the world's random stream or alter packaged tables.

The later AllGrimoire fixture uses a separate borrowed snapshot, sets Survival
and resets damage timers but does not explicitly normalize its ability flag.
Its phantom is created successfully and then ticked ten times; the failure
alone does not distinguish collision rejection from native damage rejection.
The next diagnostic needs the ability value and native arrow position/removal,
collision and damage-rejection observations at that fixture. No new build or
server run was performed by this agent.

Required qualification: real raw authored stacks through Wands, seven actual
terminal payloads and native one-item clear, no mana debit, no-hit retention,
MAG versus TECH, role/epoch/Time Stop cancellation, configured offhand and
unchanged typed grimoires. No build, runtime or completion claim is made here.
