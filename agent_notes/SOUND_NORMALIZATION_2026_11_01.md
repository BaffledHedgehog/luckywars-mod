# Custom sound normalization without resource-pack changes

User decision, 1 November 2026: do not change the RP. This supersedes the earlier
asset-copy proposal. No sound files, sounds.json entries or template resources
are delivered by this batch. Metal Pipe and the Psycho vacuum beam remain loud.

## Measurement and playback

`tools/audit_sound_loudness.py` reads the actual author pack and local vanilla
assets. The author pack's 26.3(1) path resolves to the same 26.3 directory. Both
paths match every measured OGG and sounds.json hash. It measures 112 custom events
and 50 vanilla files representing 32 event types. The vanilla event-equal median
is -21.3425 LUFS; very short clips use a -21.88265465 dBFS RMS ceiling. True-peak
ceiling is -1 dBTP. The measurement models the client's float-to-signed16 sample
conversion BEFORE playback gain: several authored files exceed full scale and
are already clipped by the vanilla decoder. Raw float peaks would exaggerate the
required reduction. No correction can recover that already clipped waveform.

The generated SERVER data `skywars2/sound_levels.json` has 112 measured profiles;
83 have a cap below1. Existing quieter playback is retained, never amplified or
unnecessarily multiplied again. Metal Pipe is exempt globally; the vacuum beam
alone uses scoped synchronous exemption, not every other use of `scream`.
Caps apply per voice, not as a compressor across multiple simultaneous sounds.
Pitch, seed, category, event identity and original recipient selection survive.

`SoundNormalizationMixin` intercepts the common outgoing native send boundary,
including direct packets, world broadcasts, entity sounds and bundles. It does
not mutate a shared packet. Reapplying normalization is idempotent. Viewer poses
are published on the server thread; asynchronous sends never query entities or
world state. Custom packet cameras are tracked as well as normal native cameras.

## Distance and vanilla protocol limits

`playsound minVolume` is server logic, not an extra packet field. It can move the
virtual source near a distant listener. There is no independent per-instance
network attenuation radius, seek offset, loop flag or live volume update.
A newly sent sound starts at its beginning. StopSound addresses event/category,
not an individual playing instance.

For a mono sound with original volume P>1, the client originally uses radius16P
but clamps its gain to1. To reduce gain below1 without shrinking initial audible
distance, send each existing recipient the virtual source L+(S-L)/P and the
measured cap. L is the actual camera eye and S the original quantized source.
This retains bearing and original attenuation AT THE INITIAL LISTENER POSE,
within native eighth-block wire quantization. P<=1 leaves the source unchanged.
Stereo assets keep their actual source/binding because vanilla OpenAL does not
normally spatially attenuate those buffers. No extra listeners are introduced.

A listener moving during a long mono clip will not retain exactly the old
attenuation curve; projecting a loud entity-bound mono clip also cannot retain
continuous entity tracking. This is an explicit server-only approximation, not
an exact independent-radius feature. No physical client output or sample-perfect
continuity claim is made by packet tests. The research note records why an asset
change would avoid this limitation, but the user rejected that approach.

## Psycho beam and positional audit

The beam sends its original loud scream ONLY to the actually hit player, bound
to that player so walking does not leave the sound behind. Overlapping casters
share one target cadence. Renewal is independent of remaining ray counters and
recasting; the 4.036333-second original clip at pitch2 is renewed every40 active
ticks, about18ms before its natural end at20TPS. Network delay and low TPS can
still affect joins. No new loop asset/event is required.

Contact loss, target clearing, stop, Time Stop and rewind presentation cleanup
retire future renewals. The last clip finishes naturally within about2.018s.
No event-wide StopSound is sent, as it would also stop unrelated screams.
Original status/damage/ray clocks are unchanged.

The sound-position review found Steampunk Glove was sending its stereo rocket
cue to ALL server players without range/dimension checks. It now uses the shared
native-range helper (original volume.6, pitch.8, no minVolume; radius16).
This fixes the accidental unlimited audience. Other distant source positions in
nuclear/encounter effects were checked against their source rather than guessed.
The local/client-origin coverage audit is in SOUND_POSITION_REVIEW_2026_11_01.md.

## Reproducing profiles

Run the audit against the read-only author pack, the FreesmLauncher assets root,
asset index34.json and a report path under build/verification, optionally adding
`--server-profiles src/main/resources/skywars2/sound_levels.json`. Both audit output
and generated profiles are forbidden inside the actual author pack. Soundfile,
NumPy and ffmpeg are measurement dependencies only; runtime uses cached Java data.
Current evidence is under build/verification/sound-normalization-2026-11-01/;
`profiles-reproduced.json` is byte-identical to the packaged server profiles.

## Verification

The first candidate0dd6a675 passed the packet mixer, feedback and full vanity
rollback. Its old sound audits expected pre-normalization volumes and were
updated to measured caps while preserving audience/pitch/seed/position checks.
The mono gun source expectation independently computes the initial projection.
Its Psycho fixture also allowed random fear teleports to move the target after
choosing next-frame aim. The fixed fixture verifies all real hit statuses then
clears ONLY scared between observations, leaving ray, lookback, piss and audio
intact. No production hit geometry or scheduler clock was changed to satisfy it.

Candidate `5b79a46f` passes all7 scoped groups in7 actual mode1 matches (87.38s):
sound-normalization, psycho-sound, weapon-sounds, item-sounds, steampunk-glove,
feedback-regressions and full vanity-rollback. Exact9762 frozen inputs,
2427compiled classes and7342processed resources match the candidate. All226
protected author paths,31live grimoire overrides,112source sound files and
sounds.json remain exact. Profiles regenerate byte-for-byte. Child shut down
normally; snapshots, extra level/ticket files and fixture state restored, no locks.
The original failed candidate is retained with its failure explanations above.

Qualification: `build/verification/sound-normalization-2026-11-01/qualification.json`.
The shared normal-stop installer installed5b79a46f, retained b1a21c55 rollback,
and did NOT stop/restart the main server. Installation hash verified. Receipt:
`main-install.json` in the same directory. No push. Full kit6/LuckyWars gates and
TMT deferral remain unchanged.
