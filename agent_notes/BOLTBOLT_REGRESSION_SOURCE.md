# Boltbolt regression source audit — 21 October 2026

Read-only source comparison; no runtime or visual qualification. Root AGENTS.md:338–341 preserves the user requirement: all five ordinary charges live 200 full ticks. The latest task asks for half as many ordinary lightning strikes and restoration of legacy cloud/snowball feel. Do not restore the old double lifetime counter.

Source roots: `L` = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`; `B` = `workingitems/awaken_tuntija_wands/boltbolt/` below L. Native `J` = `src/main/java/me/skywars2/game/LbcBolts.java`.

## Ordinary bolts / cloud behavior

- L+B+`end.mcfunction`:1–6 summons one initial lightning and five markers at +/-10 X/Z and +10 Y. J:46,449–451 preserves these positions/counts. J:524,536,538 currently yields ten strikes per marker (20,40,...200), fifty scheduled strikes plus initial lightning. Halving scheduled frequency to 40 ticks yields five per marker while preserving the user-approved duration and five charges. The initial cast strike needs an explicit count convention; it is separate from the fifty repeats.
- L+B+`charge_tick.mcfunction`:1–6 emits cloud/sculk at the OLD marker position, then if no eligible mob within 15 moves 0.5 toward nearest eyes; otherwise teleports to random eligible mob within 25. J:526–535 has the same distance/steering intent, but J:539–540 emits at the NEW position, making the trail visually different. L+`tick_not_timestopped.mcfunction`:3 and `raycastselector.mcfunction`:1,9 run steering every tick. Native steering also runs every tick.
- L+`raycasters_1s.mcfunction`:1,17 adds 20 lifetime and strikes each second; `raycastselector.mcfunction`:1 also adds one every tick. This explains old ~5-second lifetime and fewer strikes; the author explicitly rejected restoring that double count. Reduce repeat frequency independently.
- Native `targets` are living targets; legacy uses `#minecraft:mobs`. Target domain differences require checking tag membership/shared filters before claiming exact parity. Preserve author/team/service-entity protection.

## North pole snowball: substantial speed regression

- IMPORTANT command semantics: a function retains its execution position after `tp @s`; subsequent relative teleports are anchored to the original callback position unless explicitly re-anchored with `at @s`.
- L+B+`click_upgraded_mode_snow_meteor_tick_2.mcfunction`:1–6 therefore has FINAL displacement per 2-tick update .5,1,1.5,2,2.5,3 at Lifetime thresholds 0,5,10,15,20,25. They are replacement destinations, NOT additive fall steps. J:583 sums them into .5,1.5,3,5,7.5,10.5. At mature age native falls 10.5 blocks/2t instead of 3: the principal concrete mismatch.
- L+`markers_2t.mcfunction`:1 increments Lifetime by2; `tick2t.mcfunction`:1–2 schedules every2t. L+B+`click_upgraded_mode_snow_meteor_tick.mcfunction`:1–2 and `_first.mcfunction`:1–7 initialize on the FIRST callback only, then move starting the next callback. J:175,475–476 initializes immediately and J:582 moves at age2, eliminating one initialization callback.
- Spawn offset +50, scale3, rotation[-90,-90], teleport/interpolation2 match numerically: L+B+`click_upgraded_mode_snow_meteor_end.mcfunction`:1, `_first.mcfunction`:1–4 versus J:175,284–286. Spin +6 yaw/pitch every2t matches L+B+`...tick_2.mcfunction`:8–13 and J:592. Do not increase visual size/spin to compensate for the speed bug.
- Legacy particle source and five collision samples remain anchored to OLD callback position: L+B+`...tick_2.mcfunction`:7,14–18. Native J:587–593 instead sweeps old position down through fall+5 in half-block samples, snaps center to blockY+5, and emits particles at NEW position. This changes impact altitude, particle trail, and obstacle response. A faithful bounded correction should retain old origin for effects/impact center; any tunneling prevention must preserve that center and body semantics.
- Legacy impact function inherits callback execution origin, so explosion/template center is OLD origin, not teleported display location. L+B+`...explode.mcfunction`:1–8 uses offsets air(-5,-6,-5), snow(-3,-7,-3). J:599,606,613 uses meteor.position after snap. This affects the entire crater/snow structure placement.
- Raymarch differs: legacy B+`click_upgraded_mode_snow_meteor.mcfunction`:1–5 steps .75 to Distance200 (~150 blocks), coarse mob/block probes; native J:284,295–320 uses exact shared first-hit and .5 particle spacing. Shared safety/filter behavior must remain; exact terminal coordinates can differ.

## Watering/cloud-mode ambiguity

If “cloud mode” means watering (mode4), legacy B+`click_upgraded_mode_watering.mcfunction`:1–7 and `watering_tick.mcfunction`:1–2 create five random-walking water volumes, 200 ticks, 2-tick cadence, strengthIII/water breathing240 seconds. J:467–471,563–573 matches those broad values. Legacy `universal_pos_razb_7.mcfunction`:1–12 uses NBT integer conversion (floor) and independent offsets -7..7; native floors through BlockPos and uses the same independent range. Its block edits are queued rather than synchronous. No source evidence supports replacing this with a new cloud visual.

No Java, resources, source server, or installed JAR was changed by this audit.

## Implementation findings reported by parent during audit

- Parent verified native Transformation.CODEC requires `translation`, `left_rotation`, `scale`, and `right_rotation`. Current J:175 supplies only scale, so display read falls back to identity: actual rendered native scale is1, despite requested scale3. This is a separate principal visual regression; replace with a complete transform. The numerical scale comparison above describes requested NBT, not successfully decoded display state.
- User confirmed keeping five charges and changing ordinary repeat interval20 ->40 ticks. Keep Field of Lightning's shared Bolt scheduling independently configurable.
- Exact meteor callback schedule: Lifetime2 initializes without movement; Lifetime4 first moves .5; Lifetime6/8 moves1; Lifetime10/12/14 moves1.5; Lifetime16/18 moves2; Lifetime20/22/24 moves2.5; Lifetime26+ moves3. This follows threshold evaluation after marker Lifetime +=2.
- A contact correction based on actual rendered geometry is a deliberate improvement over the source's old-anchor probes; do not label it literal parity. Parent is assessing geometry to prevent explosions visibly above ground.
