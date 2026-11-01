## Final user decision and implemented scope (1 November 2026)

The user rejected ANY resource-pack edits. The asset-based option below is research
only and was not delivered. Final implementation is the outgoing packet mixer in
SoundNormalization.java with measured server-only caps. It retains the original
audience. For mono clips originally above volume1 it projects a per-listener
virtual origin, like the server implementation of playsound minVolume; stereo
clips retain their real coordinates. This preserves initial bearing/attenuation,
NOT an exact moving-listener attenuation curve over a long clip. No seek/live
gain or sample-perfect loop support is claimed. See the final sound contract.

# Sound engine limits ? 1 November 2026

Read-only inspection of current Minecraft **26.3**, not historical Paper runtime. Source archive: `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`. All vanilla paths/line numbers below are entries inside that archive. Author RP is read-only under `RESOURCE_PACK.md`; this audit changes no source, build or RP.

## Exact current calculations

* `net/minecraft/sounds/SoundEvent.java:18?50`: optional `fixedRange`; `getRange(v) = fixedRange.orElse(v > 1 ? 16*v : 16)`. This is **server audience selection**, used by `net/minecraft/server/level/ServerLevel.java:1053?1060,1076?1083` before packet broadcast.
* `net/minecraft/network/protocol/game/ClientboundSoundPacket.java:45?64` and `ClientboundSoundEntityPacket.java:34?49`: holder/event, source, position/entity, float volume, float pitch, seed; no separate client attenuation parameter or playback-instance handle. SoundEvent direct codec includes optional range (`SoundEvent.java:26?29`).
* `net/minecraft/client/multiplayer/ClientPacketListener.java:2107?2132` forwards both sound packet types to `ClientLevel.playSeededSound`. `net/minecraft/client/multiplayer/ClientLevel.java:671?699` creates positional/EntityBound instances.
* **fixedRange does not reach client attenuation**: `net/minecraft/client/resources/sounds/SimpleSoundInstance.java:87` delegates with `sound.location()` only; `AbstractSoundInstance.java:30?37` does the same for EntityBound's superclass. Event range is discarded in both paths.
* `AbstractSoundInstance.java:88?89`: effective instance volume `u = packetVolume * selected Sound.volume.sample(random)`. `Sound.java:93?94`: client maximum attenuation distance `D = max(u,1) * selected Sound.attenuationDistance`.
* `net/minecraft/client/resources/sounds/SoundEventRegistrationSerializer.java:54?64` reads file entry volume (default 1) and integer `attenuation_distance` (default 16). Sound references may also contribute volume through resolved selected Sound; use the resolved effective value for this formula.
* `net/minecraft/client/sounds/SoundEngine.java:401?443,487?488`: source gain `clamp(u,0,1) * clamp(options.getFinalSoundSourceVolume(source),0,1) * gainBySource[source]`; D is computed from **unclamped** u. Linear attenuation is installed separately.
* `com/mojang/blaze3d/audio/Channel.java:103?115`: sets OpenAL gain; linear distance model (53251), max distance D, rolloff 1, reference distance 0. For ordinary positional playback its distance factor is linear down to zero at D (subject to OpenAL spatialization/source format). Sample amplitude and source/user gains remain additional factors; a mono/stereo file inspection matters for actual spatial behavior.

Thus reducing packet volume from 10 to 0.5 with default RP volume 1 changes gain from 1 to 0.5 **and D from 160 to 16**. Supplying fixedRange=160 only keeps server recipients; their client still uses D=16. Lowering sounds.json `volume` has the same coupling. Above effective volume 1, lowering volume while remaining >=1 lowers radius but does not lower clamped source gain at all.

## Feasible normalization preserving distance

For transparent normalization with identical sound event, packet volume, category, seed, coordinates, recipient selection and client radius: prepare an author-applied RP template containing gain-adjusted audio files (sample amplitude), leaving sounds.json volume/attenuation metadata and packets intact. A sample reduction does not change u or D. This also reaches already authored volumes >1 and permits the same dynamic distance behavior at all call sites. Perceptual normalization/true-peak limiting needs the separate ffmpeg audit; engine gain alone cannot equalize file loudness.

For a new dedicated RP event with known finite fixed packet volume, lowering its metadata/packet gain and raising attenuation_distance can compensate D, but integer metadata and varying call-site volumes mean one replacement cannot generally preserve every original radius. It also needs correct server audience selection. Do not claim this is an exact global server-only solution.

A server can send new sounds or stop sounds, but cannot seek, change gain of an existing playing instance, or address a unique instance. `ClientPacketListener.java:1699?1701` handles stop by event identifier/category. EntityBound updates position locally; it does not expose network volume changes. SoundEngine tick/category gain APIs are client-local, not packets. A repeat packet starts another playback; stop+restart loses phase and may stop unrelated same-name/category instances. No seamless runtime per-player volume slider can be implemented server-only for an already running vanilla-client track.

## Shared interception coverage

Current custom `minecraft:lbcsounds.*` paths are named RP events carried in **direct SoundEvent holders**, not necessarily registered server SOUND_EVENT entries. `src/main/java/me/skywars2/game/LbcAk47.java:136?150` constructs a variable-range direct event from any string identifier and sends packets after its own audience check; `LbcFeedbackDrops.java:57?76`, `LbcSteampunkGlove.java:18` show more direct custom paths. Vanilla IDs also use direct holders (`LbcAcademy.java:253?254`, `GameMenus.java:343`). `LbcClouds.java:692?703` uses `BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event)`. Matching only holder registry keys misses direct holders; match `packet.getSound().value().location()`.

A common outgoing packet boundary covering **both** ClientboundSoundPacket and ClientboundSoundEntityPacket can observe named registered and direct sounds, including direct per-player sends. A registry-registration hook or only ServerLevel.playSound/LbcAk47.sound cannot cover every path. Preserve holders, source, pitch, seed, quantized coordinates/entity ID, and avoid double-transforming bundle/nested sends. This interception still cannot decouple gain/radius on a vanilla client and does not cover client-originated ambient/UI playback or sound-producing world-event packets. Asset normalization avoids most of those interception limitations when the asset itself is shared.

No custom SOUND_EVENT registry registration was found by searching current src/main/java for `Registries.SOUND_EVENT`/`BuiltInRegistries.SOUND_EVENT`; observed uses are lookup/wrapping. This is a source-search finding, not a claim about all third-party mods.
