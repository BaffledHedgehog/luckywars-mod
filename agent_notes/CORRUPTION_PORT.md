# Corruption and Acid + Corruption

This batch adds the Corruption Wand (`corrupter`) and Mifrak Haolamott
(`acidcorrupt_wand`), including ordinary/Academy variants and their native
two-donor recipe. [Exact presentation audit](CORRUPTION_RESOURCES.md).
The four demonic-dust results are a required dependency, tracked separately in
[the glyph contract](CORRUPTED_GLYPHS_PORT.md). The complete family and glyph dependency are qualified together; see the verification below.

## User rulings, 24 October 2026

- Reverse sacrifice: "Сохранить" (user). All eligible friendly mobs in the
  loaded dimension are sacrificed; players and technical roles are excluded.
  Resistance V duration is floor(sum of floored remaining HP / 10) seconds.
- Titular disease: "режим отложенной смерти - только при прямом попадании.
  WILD." (user). Each direct hit adds 120 seconds to that target; a wall hit
  does not curse surrounding entities. Caster launch overlap is ignored by the
  native outgoing ray, without granting friendship immunity to its targets.
- Plague: "обратной маны тоже WILD, радиус оставь" (user). It adds thirty
  seconds of reverse mana to all eligible players within ten blocks of impact,
  including caster and allied players.
- Meditation: "Сохранить эффекты и немедленное открытие" (user). Resistance V
  lasts thirty seconds; nine negative effects have infinite native duration,
  including Instant Damage C. The actual used stack becomes ready immediately.
  A complete purge clears the curse effects.

## Read-only sources

Old datapack root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Donor functions: `lbc/function/workingitems/corrupter{,_pre,_upgraded,
_upgraded_pre,_upgraded_pre_2,_upgraded_pre_left}.mcfunction`.
Continuation: all ordinary/Academy branches under
`lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/`.
Payload helpers: `lbcr/function/damagewithid9{,_rad}.mcfunction`,
`random_debuff{,_tr}.mcfunction`; lifetime scheduling is the ordinary rare
raycast selector. Delayed death decrements in `lbc:function/effect_death`.
No source server or author resource-pack file is modified.

## Runtime contract

Corruption costs the configured 1800 mana. Ordinary casts choose one of four
source outcomes for eligible enemies within fifteen blocks: Poison I for ten
seconds (Wither II for source-tagged unholy monsters), Wither II for seven
seconds, Hunger CCLVI for two seconds, or eight cactus damage. The source
ordinary powder chance is rolled independently for every target on every
cast; Academy limits powder rolls to selected damage mode. Academy cycles all
four modes for free before the mana admission check. Native effects retain
their caster and ordinary HurtTime applies to cactus damage. The actual used
hand/stack completes its trial when a surviving eligible enemy has Poison,
Wither and Hunger during the damage outcome. Source C presentation stays C
after Academy, with the generic green upgraded line appended.

Acid + Corruption costs 1210 mana. The ordinary/default instant ray stops at
one native first entity/block contact and creates a stationary native acid
ball. Its twenty-second lifetime and six-block pulse are source values. Each
tick, every eligible target has a 35% chance of one random source debuff. A
20% tick roll emits one native falling acid drop at an integer offset in
[-4,4] on each axis. Parent ownership is copied directly, avoiding nearest
marker/caster contamination. Falling drops have a fifteen-second lifetime,
move half a block per tick, apply two cactus damage plus a 20% random debuff
chance at native intersecting samples, and erase the source 0.3-block box on
their first wall contact without drops. Native armor and HurtTime remain.
Instant Damage durations use native instantaneous ticks rather than seconds. Ticked Instant Damage (and native heal/harm inversion) now carries its accepted effect owner through the existing status-damage mixin; real one-tick mob deaths and later-player precedence are covered.

All three instant rays execute inline using resident DDA and native entity
boxes. No work queue slows projectiles or defers their flight. Unloaded chunk
boundaries end the ray/actor without loading more chunks. Released hard acid
is magical and disappears under antimagic. The shared Time Stop service pauses
native actors and custom death counters; reflection uses native projectile
ownership. Epoch, owner/participant, unload and match-stop guards apply.

Delayed death uses the original `delayed_death` seconds counter and E111 HUD.
Only targets with a live curse are visited. Greed pauses it; void resistance
can suppress its terminal kill as in the source. A captured cause travels in
the native generic-kill DamageSource, including through deferred Time Stop
damage. The shared indirect-kill owner gives priority to a later accepted
player hit. Purify/forget/end clear the custom timer. Subsequent direct hits
extend duration and record the new player's action.

Reverse mana retains its existing E118 counter and regen implementation.
Its accepted application records the caster; actual reversed regeneration
records mana debt in the shared overload attribution service. Duration-only
extensions, including KFC's two source `reverse_mana +1` entries, preserve the
existing cause. Purge/expiry clear the active reverse source; recovery clears
remaining overload debt. Hunger attribution also follows native starvation,
including an empty food bar after natural Hunger expiry. Explicit removal,
full purge, food recovery and lifecycle cleanup remove the hunger cause.

The native exact-two-ingredient recipe accepts one `stacid` Acid Wand and one
`corrupter` Corruption Wand, rejects extras, and works in both 2x2 and 3x3.
The result is fresh ordinary continuation loot. Native menu pickup/shift/full
inventory behavior handles debit; legacy ground polling is not reproduced.

## Qualification

Qualified build `58a78a13`: 34 recorded suite results, two actual mode1 matches (149.97s). Exact 292 sources / 785 classes / 2781 resources match the tested artifact. Installed stopped main; rollback `0faca9ac` retained, main not started. Evidence: `build/verification/corruption-final/`. Native fixtures cover both
crafting menus, source presentation, four donor modes and used-hand C trial,
continuation modes, direct-only WILD death, WILD reverse regeneration credit,
global friendly-mob sacrifice, ball/drop payloads and boundary lifetimes,
meditation/SS promotion, shared purify, actual Time Stop, accepted later-hit
precedence on real delayed mob deaths, and actual match stop. Hunger's native
cause boundary is checked without killing a participant; a full natural
starvation/death session is not claimed. Pixel appearance, disk restart and
peak-load performance require separate evidence.
