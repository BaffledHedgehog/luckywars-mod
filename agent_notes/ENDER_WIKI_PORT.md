# Ender wand / Bad Wiki

Source contract for the ordinary and Academy families. Qualification is recorded
below from the native server run. The original server and author resource pack
are read only.

## Source and design

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Relevant functions: `workingitems/endcast`, `workingitems/badwiki`,
`workingitems/badwiki_upgraded*`, `other/magic_academy/change_tier_3_*`,
`other/magic_academy/change_mode_28_*`, `effects/rtp_1..3`,
`projectiles/arrows2`, `players_tick`, `items/on_falling`, `items/on_ground`.
Source presentation comes from `loot_table/transformables_staff.json`, with the
original `add_upgrade_desc` and `tiers/a` modifiers. The generator
[import_lbc_ender_wiki.py](../tools/import_lbc_ender_wiki.py) extracts four scoped
leaves and all 84 random/28 selectable effect definitions, checking 44 existing
RU/EN keys and both models in the author's pack. No resource-pack writes or
duplicate translations are required.

Qualified ready entrances: `lbc:ported/ender_wand`,
`lbc:ported/academy_ender_wand`, `lbc:ported/bad_wiki`,
`lbc:ported/academy_bad_wiki`. Broad staff/reward pools remain gated.

## Ender

2500 configured mana; one successful wand-cast hook. Random safe surface within
the source square of radius100 around the caster with noBorders, around X/Z=0
otherwise. Nether uses `under 125`; the caster retains rotation and lands half a
block above the spread position. Source sounds/portal particles retain their
original execution origin. The Academy variant adds hidden Slow Falling I for
60 ticks, including when cast from the second hand. Landing within8 blocks of
another nonspectator player awards the original advancement. Source lore says60
despite the actual command's100; the source text is preserved.

Trial: at least100 airborne ticks when the ordinary wand is used. The existing
raw `falltimer` score now has a native tick owner: reset on ground, increment
otherwise; this includes flight/swimming exactly as the original OnGround
selector did. Existing KFC adds one raw tick. Start/end/death/forget clear the
counter. The actually used stack receives readiness, including the second hand.

## Bad Wiki

500 configured mana; ordinary casts choose one of84 source outcomes. The preset
generator preserves anomalies rather than extrapolating by level: e.g. the third
blindness result lasts100 seconds, first saturation uses amplifier3, the next
two use1/2. Instant effects retain native one-tick duration. Instant Damage III
awards the source advancement. Trial requires all24 noninstant source effects
simultaneously on the caster; readiness belongs to the used book.

Academy:28 selectable effects and3 tiers, including three RTP strengths. Native
effects own health/attribute changes, timing, cleansing and ordinary persistence.
Shift+right-click looks up/down to cycle modes; looking straight up/down cycles
strength. Disjoint pitch ranges fix double activation at the source boundaries
and the exact horizon. Mode/tier switches spend no mana and require none (user).
Normal item admission, antimagic, Time Stop, offhand configuration and independent
parallel hand costs use the existing wand entry points. Ordinary and Academy
legacy flag matching explicitly differ by `upgraded:1`.

## Shared teleport effects

The existing `LbcColors` RTP owner now handles all three raw source counters:
`effect_rtp_1`, `_2`, `_3`. Raid Omen40/41/42 transports duration directly into
the corresponding cumulative timer, without starting native raid behavior or
replacing another Raid Omen variant. Native effect admission and projectile
policy still run first. Each positive timer teleports every5 ticks within a
square of radius10/20/30 and loses5 ticks; concurrent strengths act in source
order. Greed and Time Stop pause them. All3 actionbar entries retain glyph E142
and white raw values; KFC adds1 raw tick to each active counter (user). Purify,
death, stop and native visibility/load indexing reuse the existing service.

The shared finite surface picker visits resident columns in a randomized
permutation and stops at a safe column. It uses native two-air/ground-tag/liquid/
ceiling tests and original centered-block destinations. It neither requests
chunks nor retries random void columns thousands of times. No safe resident
surface means no movement; casting still spends its source mana. Random numbers
and exact spread distribution may differ for performance (user). Confuse's
already qualified small-radius distribution remains unchanged.

Player attribution resolves native players, projectiles/clouds and existing
causal services. Teleport curses, including the purple artifact, adopt that
player through the existing displacement/fall owner. Later accepted player hits
still supersede the older curse (user); self-casts do not overwrite an attacker.

## Verification

Build `3f17da51` passed24 recorded suite results in two actual mode1 matches
(50.02s), including previous Neutral/Blood/Confuse/Evoker/Bass, Academy, KFC and
all16 color branches. The isolated runner is
[smoke_ender_wiki.py](../tools/smoke_ender_wiki.py). It saved/restored the isolated
configuration/profiles/scoreboard/JAR and stopped its server successfully.

The new native fixture casts all84 random outcomes and all28 x3 Academy choices
through native item use, checks complete source models/tiers/lore, damage24 for
Instant Damage III, source anomalies, both actual Academy promotions, used-hand
trials/independent costs/free mode changes/antimagic/Time Stop admission, safe and
unloaded-column search, ceiling rejection, raw airborne threshold/ground reset,
three RTP durations/HUD/KFC/Greed/purify/index restoration, native fall credit and
actual epoch/quiet match stop cleanup. Precise effect/fall boundaries use manual
native calls; this is not a real client rendering, disk restart/unload/disconnect,
full natural trajectory or peak-capacity test. Radius100's exact RNG distribution
is deliberately not claimed. The fixture's first failure exposed missing Academy
flag discrimination; that production dispatch was fixed before qualification.

Exact tested artifact:272 Java sources,731 classes,2751 resources; ready, built
and installed JAR hashes match. Full SHA256:
`3f17da515aa89bc717cbe029db2ae9a064008d4907aee76629800e15f69d6f74`.
The known fake-player Polymer missing packet-context error pair was the only
final ERROR output; no final suite failed. Evidence and guarded install manifests
are in `build/verification/ender-wiki-final/` (ignored local output).
Installed to stopped main, rollback
`build/verification/main-rollback/skywars2-before-ender-wiki-a6e485fd.jar` retained;
main was not started. Three preexisting user sources retain their original hashes
and are excluded from this commit.
