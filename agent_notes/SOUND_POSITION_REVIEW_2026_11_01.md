# Sound position review — 1 November 2026

Read-only review of native custom-sound delivery, after reading the existing
ALL_ITEM_WAND_SOUND_AUDIT_2026_10_25.md and applicable AGENTS instructions.
No Java, resource pack, build, fixture or main-server changes were made.
This is static evidence, not client playback qualification. LbcPsycho has a
separate owner and is excluded from this review.

## Confirmed normalization bypasses

All file references below are relative to src/main/java/me/skywars2/game/.

| Site | Current behavior | Recommended handling |
| --- | --- | --- |
| LbcSteampunkGlove.java:18,56 | Cached custom ROCKET Holder is sent directly to every connected player at the sprinting player's position, volume .6/pitch .8. It bypasses LbcOverload entirely. | Normalize this cached Holder/direct packet path as well. If routing through the shared helper, consciously retain the intended recipient policy. |
| LbcFeedbackDrops.java:57–62 | Yamete custom event sent directly to nearby level players at dropped token position, 1/1. | Apply normalization at final custom packet volume; preserve token coordinates and local recipients. |
| LbcFeedbackDrops.java:74–77 | Ultrascream custom event sent ONLY to victim at victim coordinates, volume 10,000,000/pitch 1. | Normalize while preserving target-only delivery. Do not replace with the broadcasting sound overload. |
| LbcFartus.java:284–286 | Random oluh_1…6 custom event sent ONLY to hit player at projectile coordinates, 1/1. | Normalize while preserving target-only delivery and actual projectile position. |
| LbcKfcRewards.java:345–352 | Player-owner bomb feedback uses shared helper, but nonplayer owner branch constructs a custom Holder and direct packets at the impact position. | Normalize both owner branches identically; preserve explicit impact level/position. |

Shared delivery is LbcOverload.java:235–271. Its overloads with a player derive
world from that player; explicit-level overloads preserve supplied world. Private
sendSound at 273–280 also directly constructs packets and requires coverage if
its event is custom. Nonplayer LbcAk47.sound at 135–151 is another direct dynamic
custom-capable helper; Stalin calls it with custom shot/rasstrelat sounds. This
helper currently bypasses shared volume adjustment for nonplayer sources.
No ClientboundSoundEntityPacket construction was found in native sources.

## Position and recipient conclusions

No new indisputable wrong custom impact coordinate or zero-vector sound origin
was established outside the separately owned Psycho work. Do not manufacture
position fixes from unusual coordinates alone:

- LbcExoperso.java:126 intentionally uses caster Y+20. Legacy
  workingitems/exoperso.mcfunction:5 and exoperso_upgraded.mcfunction:5 explicitly
  use `execute positioned ~ ~20 ~`; the earlier wand audit already records it.
- LbcHohlopitek.java:104 uses actual ray impact Y+20, rather than caster position;
  the prior audit explicitly records this authored offset.
- LbcWitchGluttony.java:173 deliberately puts whale sound at Y1,000,000 with
  minimum volume 1. Moving this without checking its deliberate remote cue
  would change spatial presentation, not fix an established porting error.
- LbcFartus.java:295 uses target position. The suspected owner-dimension mismatch
  is ruled out by valid(Shot) at 189 requiring owner.level()==shot.level().
- Direct target-only custom packets in FeedbackDrops and Fartus are already
  private. The prior Acid Corruption cave-target correction is already present
  at LbcAcidCorruption.java:202 and needs no repeat fix.

The glove's unconditional all-server packet loop is broader than the shared
helper's same-level/range policy. However, cross-dimension legacy @a semantics
must not be asserted from memory: LbcApocalypseEvents.java:233 explicitly records
an intentional no-dimension-restriction command policy. Therefore cross-world
receipt alone is not classified here as a confirmed legacy position mistake.

Recommended focused packet checks: remote bomb impact retains impact position;
nonplayer Stalin custom sounds receive normalization; Fartus/Ultrascream retain
one victim recipient; glove cached custom sound is normalized; metal_pipe and
Psycho vacuum exclusions remain with their implementation owners.

## Bounded client-origin playback review (same date)

Current production Java and src/main/resources were searched for custom sound
components, consumable/item/entity sound overrides, local sound calls, level
events, jukebox songs and custom UI playback. No confirmed lbcsounds event was
found that plays on the client without an outgoing sound packet.

- Excluding the new skywars2/sound_levels.json measurement table, searching all
  packaged resources for `lbcsounds` returns zero files. There is no packaged
  custom jukebox_song or sounds.json definition.
- LbcPills.java:224 constructs Consumable.builder().consumeSeconds(.2f).build(),
  retaining the default vanilla eating cue. Actual custom good/bad feedback
  explicitly calls LbcOverload.sound(p,pill.sound(),1,1,0) at line273.
- No production getEatingSound/getDrinkingSound/getUseSound or entity custom
  getAmbientSound/getHurtSound/getDeathSound override, custom SoundType, or
  playLocalSound call was found.
- The only production levelEvent call is LbcChefBrain.java:833, vanilla block
  break event2001 with native Block.getId(state), not a custom lbcsounds cue.
- Packaged GUI jukebox_playable references are vanilla songs11/5; reward-sound
  menu entries are ordinary vanilla disc carriers with rewardsound custom data,
  not custom client audio components.

This static result supports the outgoing packet normalization boundary for the
current native lbcsounds implementation. It does not certify arbitrary external
resource-pack overrides of vanilla events or independently observe speakers.
No RP/source/build edits were made for this follow-up.
