# Infinite and Devil Totems — 26 October 2026

## Source and author decisions

Read-only source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
The acquisition source is `lbc/loot_table/transformables_totem.json` and
`infinite_totem.json`; `lbc/structure/notsourcelava.nbt` is a complete
30×30×30 cube of falling lava, level8. The existing
`lbc/advancement/{infinite_totem,devil_totem}.json` and reward functions
`lbc/function/other/{infinite_totem,deviltotem,deviltotem0}.mcfunction` supply
activation behavior. All names, lore, styling,
models, rarity weights/quality and source enchantments are retained.

Author decision (user): Infinity above I returns additional identical totems.
The implemented rule is Infinity N → N copies of the consumed stack, including
its actual enchantments, custom name and other components. Normal quantities
appear immediately at activation; large quantities retain a counted refund
queue with bounded CPU/entity creation per pass. There is no 255 or other
artificial level limit. Match stop cancels outstanding match-owned work.

Author decision (user): enchantment levels must support the positive signed
integer range, through 2,147,483,647. The existing server component codec,
mutable enchantments and saturating anvil formula already implement this.
Native survival anvil/storage tests are rerun with this release. Vanilla
clients still receive protocol-safe component values plus the actual numeric
tooltip; the true server stack and Polymer restoration metadata retain the
full level. Removing that client projection would disconnect vanilla clients.
Enchanting-table and loot level selection are unchanged.

## Native behavior

Both items remain real `minecraft:totem_of_undying` stacks. Vanilla chooses and
consumes the correct hand, awards usage and restores health/effects. Native
`used_totem` activation invokes the source rewards before that restoration;
the existing broken-totem handler remains active. Infinity requires both
original transformable/totem flags and a positive actual Infinity level.

Holding a Devil Totem produces four native, untamed Wither Skeletons about
each enemy player within20 blocks on the shared five-second pulse. Both hands
work independently. Native finalized skeletons retain their stone swords,
health and AI; their original author's UUID is persisted on the entity. They
are hostile WILD actors, including toward their author. Native melee and Wither
retain causal ownership. No enemy means no summons or self-damage. Antimagic
does not suppress this source passive; dimension time stop pauses it.

Devil activation applies native Instant DamageII behavior in radius10:
12 magic damage to living targets, or8 healing to undead. Other affected
targets retain WitherII for60,000 seconds. WILD effects can affect allied
players even with team friendly fire disabled; service roles and absolute
active Greed remain protected. Vanilla subsequently clears the user's own
transient source effects and applies ordinary totem protection.

The center3×3×3 becomes source lava immediately. The original15% mask over
the30-cube falling-lava template is reproduced with native block writes and
visible progress through the shared CPU-budgeted terrain queue; queued cells
cannot overwrite that central source-lava fill. No runtime structure placement
or temporary Instant Damage entities/effects are required. Soul-fire/soul
particles and the Wither death sound remain. Lava and ongoing damage retain
the responsible player through the shared indirect-kill tracker.

The five-unit source death counter advances every two ticks, pauses in time
stop and causes an actual native death on tick10, bypassing another totem.
The actual triggering attacker retains kill credit. Source void resistance
and active Greed exempt the final death. Clear/disconnect and match stop
remove pending death state; match stop retires loaded actors, terrain work
and counted refunds. Persisted actors from an earlier match are discarded
when loaded, without force-loading chunks.

## Replacement-player lifecycle correction

The real death test exposed a26.3 callback-order issue: vanilla's packet
handler assigns its replacement `player` only after `PlayerList.respawn`
returns, while Fabric's AFTER_RESPAWN callback prepares gameplay inside that
call. Bind the listener to the replacement before game preparation, so its
spawn teleport, following teleports and client-load confirmation operate on
the new player. The native caller subsequently assigns the same replacement.
Tests assert listener identity and an actual post-respawn teleport; previous
projectile, special-egg and raycast checks run after this real replacement.

## Acquisition, resources and checks

Ready leaves: `lbc:ported/infinite_totem`, `lbc:ported/devil_totem`.
Complete public pools: `lbc:transformables_totem`, `lbc:infinite_totem`.
No resource-pack changes are needed: all17 translation keys and both item
models already exist in the author's pack. The live grimoire override pack
and manually edited resource-pack files are preserved.

`tools/import_lbc_totems.py` imports only these four tables.
`tools/check_totems.py` is read-only and checks exact source migration, the
falling-lava template, RP references and public catalog. `TotemChecks` uses
actual native lethal hits, consumption, advancements, effects and skeleton
attacks; it covers both hands, positive levels through IntMax, WILD allied
damage, lava, cooldowns, native final death/credit and replacement-player
respawn. A retained stop fixture checks cleanup of real hostile actors,
unfinished lava, death debt and an IntMax refund queue.

`tools/smoke_totems.py` runs two actual mode1 matches on isolated port25576,
with advanced-anvil, previous tool, summon/Guardian, Syringe, projectile,
KFC, special-egg and indirect-kill regressions. Evidence is recorded under
`build/verification/totems-final/` only after the final run completes.
Client pixels/audio playback and peak TPS require separate observation.

Final release: `7da14e9683a61936fb392763e1850b8e91263a8c6a00a476d2738e7b1513813f`.
Build and26 recorded groups across two actual mode1 matches passed in59.21s.
Exact398 Java sources,1127 classes and3134 resource files match the release.
Installer16176 installed the qualified JAR on the primary server, preserving
7dc0342b as rollback and without restarting it. Oldest f086522d was retired;
keep the new release and the7dc0342b/da260679 fallbacks. Protected user files,
previous deletions and live grimoire overrides are unchanged. Evidence:
`build/verification/totems-final/qualification-7da14e96.json`, `main-install.json`
and `build-cleanup-receipt.json`. The only ERROR log pair is the previously
known fake-player chunk-packet context diagnostic; no native suites failed.
