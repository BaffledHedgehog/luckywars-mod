# Gaycaster / native Prism + Omega craft

## Academy continuation - 24 October 2026

Source: `workingitems/gaycaster/meditate` and its six phase/tick helpers;
`ray/cast_upgraded*`, `ray/cast2_<colour>*`; Academy upgrade modifier (tier S);
awakened `acidacid/click2`, `tick`, `tick_spawn`, `explode`, and
`lbcr/*acidbig*very` helpers. These are read-only legacy inputs.

Author clarification (user):

> Сохранить старое: голубой — электричество, синий — вода

The Academy leaf is `lbc:ported/academy_gaycaster`. Seven selected colours and
one sequential palette retain source125 reach and1500 mana per shot. Shift+PKM
cycles modes before admission/debit, even with negative mana. The palette colour
is stored in the actual used stack; the first sequential shot is red, then wraps
after purple. Red uses the accepted-damage-only healing ruling above; yellow
sets200 ticks on a direct target and100 for a block-area target. Orange creates
a5x5x5 lava cube in source AIRS; blue creates water directly, not Conduit status.
Purple preserves strict air replacement without block-item drops.
Water/lava/support changes capture the ray's projectile cause through the existing
terrain attribution context: friendly mobs remain affected; allied players keep
their normal protection. Native drowning/lava calls verify both admission and
caster credit on a friendly mob.

Green uses a500-tick awakened seed at0.2 block/tick, emitting ordinary acid with
the source40% chance. Its first native block collision releases40 owned500-tick
acid children and removes the seed. Native projectile ownership/reflection and
the existing acid reaction/poison/attachment service remain shared. The seed's
source flight stays straight; reactive children retain shared acid physics.
This dependency does not open acquisition of the unported awakened acid staff.

Meditation keeps source115 five-tick units, with no mana debit/cast hook after
the normal1500 admission. The six phases retain eleven prism warnings/shots,
eleven delayed concrete fire circles, eleven falling acid hazards, the random
five-lane electric walls,120-tick rising water plane, and16 rising black displays
per five-tick unit from35 through8. Trial hits are WILD as in source, excluding
technical roles/observers; the participant fails on contact with a trial attack.
The actual initiating hand must keep the same trial stack. Readiness then uses
the existing Academy interaction/promotion system, including single-use tokens.
Continuing trial terrain paints retain the original cast's causal order, so they
cannot silently overtake a later real player hit. Trial particles use source
normal-distance delivery; only the red flash is forced-distance. Selected colour
ray particles retain their original forced-distance delivery.

Every participant owns its helpers; failure/unequipping/restart affects that
participant's session. Native display load/unload reattaches by UUID only to a
live session, otherwise discards orphaned displays. Death/disconnect/match stop
remove the trial. Time Stop pauses it. No arbitrary projectile/queue population
cap or alternate generic actor framework was added. Existing translated
names/lore/model assets were checked in the author's actual pack, read-only.
The template changes only the old mainhand-only failure wording to the actual
initiating hand, retaining its translation key and formatting.

Qualified build6559f2b8:24 recorded suite results/two real mode1 matches (124.9s); exact281sources/753classes/2767resources. Installed stopped main, rollbacke7b6df3a retained. Evidence:build/verification/academy-gaycaster-final/. Full trial uses normal server time with native teleport dodging, not counter fast-forward. No real-client pixels, actual disk restart/chunk reload/disconnect or peak-load claim.

The fixture latches chunk readiness once; that warm-up limit is not a limit on
the full575-tick trial. Explicit fake-player `doTick` heal checks set food below
the vanilla regeneration threshold, restoring it with the existing player-state
snapshot. This isolates the expected8HP from ordinary saturation regeneration.

Source: legacy LBC lbc/function/workingitems/gaycaster/ray/ (cast, cast2,
loop, check, impact/block and impact/entity), other/craft/gaycaster,
other/craft_ground, loot_table/gaycaster and lbcr/raycast_preloop10magnum/11.
Read-only RP: existing gaycaster, eight lore keys, lbc:gaycaster, tier A.
No resource-pack writes or new assets.

## Scope

Ready leaf: lbc:ported/gaycaster. Original lbc:gaycaster was already bundled
as an Ultra Reforge dependency; it is not newly enabled here. Both identities
use the ordinary native behaviour. No broad loot pool added.
Academy entry, six-phase meditation and upgraded palette remain unopened.
An externally supplied upgraded/trial+Shift stack is rejected without payment;
it does not silently fall through to an ordinary paid cast.

Native shapeless skywars2:gaycaster_combine accepts one Prism and one Omega
(ordinary/Academy identities or source donor flags), exactly two occupied slots,
in either2x2 or3x3. Native output pickup consumes one of each, one result.
Preview has no effects/random decisions; extra ingredients are rejected.

## Ordinary ray

PKM,1500 mana, two source sounds, mage/trinket cast hook. Existing two-hand
dispatcher applies the independent cost to each hand. Local cast colour
replaces shared player/storage/caster tags, preventing cross-caster interference.
125-block source reach remains; first native entity hitbox/block intersection
replaces the old positive unit-cell selector. Shared Exasperato resident DDA
queue provides its cooperative3ms budget, loaded-chunk limits and epoch/owner/
antimagic/Time Stop cleanup. Unbounded callers retain their previous reach.
No new projectile entity, queue cap, incidental impulse or actor framework.

Forced-distance particles sample each half-block at source scale1.5; impact
dust retains scale2. Hue starts -20 and advances5 per half-block;
negative initial impact hues retain the source no-payload behaviour. Original
block overlaps (red320..329 with purple; blue/purple278) remain.
Entity impact has the original red330..359 band.

Seven source payloads reuse existing services:

- Red:6 native magic damage; one native Instant Health II only when at least
  one red hit is accepted, for both entity and block impact (user,24 October).
  Every target is processed; a later rejected hit cannot erase earlier success.
  This supersedes source unconditional entity healing and last-target block healing.
- Orange:100 source ticks of Kiuaskivi; block impact also fills the exact
  +/-0.5 cube with fire on airs_to_fire. Native Raid Omen111 is transport only.
- Yellow: existing Psycho piss counter set to100, rather than added.
- Green: six shared native acid charges aimed upward, original block-origin
  offset2 behind impact, normal lifetime300 and existing ownership/physics.
- Cyan: four shared short electric arcs, lifetimes5/7/9/11. Staff activation
  requires magic; released arcs stay nonmagical (user). Magnum retains its
  separate mechanism suppression policy. Source entity
  helper launches from the caster's eye minus one forward block.
- Blue: native Conduit Power II,20ticks, existing indexed water behaviour.
- Purple:5 native out_of_world damage unless void_resistance; block impact
  destroys the +/-0.5 cube with native block/container drops.

Source particles/sounds and seven advancement criteria remain. Criteria use
source hue bands and presence of an eligible target, without requiring damage
acceptance. Native target/role rules replace the old loose global selectors.
Green/Cyan later damage uses their existing projectile owners.
Physical effects hit friendly mobs; caster/allied players, spectators,
technical roles and antimagic remain protected by existing policy (user).

## Shared Kiuaskivi status and causal terrain

LbcKiuas owns the existing effect_kiuaskivi raw tick counter and affected-entity
index, reusing held Kiuaskivi's kiuResult tags. Active ticks handle only the
source3x2x3 neighbourhood. Unchanged states are filtered before writes;
no world scan or markers. Greed/native freeze pause it; antimagic prevents
writes while its timer expires. Full/negative cleanse, forget, epoch and stop
remove it; source actionbar glyph E146 displays the counter.
KFC's exact112-source score list is unchanged: this counter was absent there.

Native Raid Omen111 ingress (including existing Bad Wiki outcomes) converts
to this status instead of starting a raid. An entity attachment retains epoch
and causal UUID/order; the existing scoreboard retains its counter. Unload
detaches the index; load reconstructs it and rejects stale epochs. Continuing
terrain writes keep their original influence order, so a newer accepted player
hit stays authoritative (user). Fire spread uses existing hazard inheritance.
Existing cause token primitives also used by Frost are reused.

Enhanced Conduit water writes use its captured native effect cause through
LbcIndirectKills, retaining effect influence order for drowning/support removal.
Unowned colour-artifact water stays unowned. Generic native status attribution
still uses the existing in-memory service; disk restoration of the generic
Conduit cause is not claimed.

## Validation

Red healing clarification (user,24 October 2026):

> Да, лечить только за принятый урон

Native regression cases cover a rejected direct hit, all rejected block targets,
accepted/rejected targets in either query order, and multiple accepted targets.
Only one8HP heal occurs; all eligible targets still receive their own damage call.

GaycasterChecks / tools/smoke_gaycaster.py: actual native PKM for seven colour
bands/both hands; native2x2/3x3 menus, extra ingredient rejection, full inventory
shift-click, one-pair output pickup; presentation/admission; native Raid111
conversion, manual same-entity index unload/load, fire credit and later-hit
precedence; actual Time Stop/resume and real match-stop cleanup.
Prism/CausticOmega/AdvancedSnow/Neutral/Mobgun/Academy/KFC regressions run together
in two actual mode1 matches. Finite125 sky rendering and actual dual-hand
PKM costs are also covered. Fake players use an explicit native doTick for
Instant Health; waiting alone does not tick their local player effects.

Qualified builde7b6df3a:24 recorded suite results in two actual mode1 matches (85.03s); exact280sources/749classes/2766resources. Installed on stopped main, rollback0f4337b6 retained. Evidence:build/verification/gaycaster-healing-final/. Three user sources unchanged/excluded. Only the known fake-player Polymer packet-context pair appeared. Fixtures include
direct native boundary calls; no real-client pixels, actual disk restart/unload/
disconnect or peak-capacity performance claim.
