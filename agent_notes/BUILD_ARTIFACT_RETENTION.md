# Build artifact retention (25 October 2026)

## User request (user)

> и ещё - пожалуйста, чисти ненужные файлы оставшиеся после билда, я тут заметил что там несколько десяток старых версий мода, которые в целом то и не нужны уже

## Retention rule

After a new build is qualified, keep its ready JAR, the preceding qualified
ready JAR, and one exact copy of the installed main JAR for rollback. Current
build/libs and the isolated test server's installed mod are active build/test
inputs and remain separate. Other third-party mods, runtime, libraries, Gradle
caches, worlds, profiles and configurations are outside this cleanup policy.

Remove obsolete named skywars2 JARs directly inside build/verification and its
main-rollback directory. Before deletion resolve every literal path, require its
parent to match one of these exact project directories, reject symlinks, compare
size and modification time with the inventory, and exclude every retained path.
No recursive deletion is needed. Preserve small qualification logs/manifests;
older historical notes may name JARs subsequently removed by this retention.

## Cleanup performed

Deleted285 obsolete JAR files (10,040,160,976 bytes,9.35 GiB), including247 old
ready/staged JARs and38 old main-rollback copies. Kept the Acid and Soulless
qualified JARs and chef-skin2558af6b, an exact installed-version fallback.
Main mods/skywars2.jar hash stayed2558af6b570287dd32d692c5305644b3804fe052695e0aa453c4718e421b5206.
Inventory and receipt are in ignored build/verification/build-cleanup-candidates.json
and build/verification/danmaku-final/build-cleanup-receipt.json. Retention will
advance once Danmaku qualifies; counts above describe this cleanup operation.

## Retention advanced after Danmaku qualification

Danmaku passed both actual matches. The preceding Soulless JAR was then removed
after verifying its exact hash and the new ready artifact. Final cleanup totals:
286 JARs,10,078,304,331 bytes (9.39 GiB). Kept the new Danmaku ready JAR, the
previous Acid ready JAR, and chef-skin2558af6b as the installed-version fallback.
Current build/libs stays available; the main installed JAR is unchanged.

## Retention advanced after Sheep qualification

Sheep passed 26 recorded results in two actual matches. After hash verification,
the obsolete Acid ready archive was removed. Cumulative cleanup:287 JARs,
10,116,474,266 bytes (9.42 GiB). Kept Sheep ready, preceding Danmaku ready and
chef-skin2558af6b rollback. Main disk JAR was externally updated to04396ca4
(Danmaku); this task preserved it and did not install or restart the server.

## Chef rework ready checkpoint

Qualified Chef4bf4649e is now ready. No extra historical version series was
created. Keep preceding Sheep65a39f13, current main disk Danmaku04396ca4 and
former main2558af6b fallback until the next supervised main replacement; this
session did not verify which JAR an already-running main JVM originally loaded.
Cumulative cleanup remains287 obsolete JARs/9.42 GiB after Sheep advancement.
Current build/libs contains the active binary and source JAR. Main, worlds and
caches are preserved. Temporary diagnostic scripts/evidence live under ignored
build/verification rather than the project/server root.

## Hole erosion ready checkpoint

After build54941d0e passed28 results in two matches, the older Sheep65a39f13
archive was verified against its qualification SHA and deleted (38,281,250bytes).
Kept new Hole Erosion, preceding Chef4bf4649e, main disk Danmaku04396ca4 and
former main2558af6b fallback while the running main JVM's original build remains
unverified. No main file was replaced. Cumulative removal:288 JARs,
10,154,755,516bytes/9.46GiB. Receipt:
`build/verification/hole-erosion-final/build-cleanup-receipt.json`.

## Weapon sound ready checkpoint

After593a39df passed28 results in two matches, verified obsolete Chef4bf4649e
archive was removed (38,322,316bytes). Keep new Weapon Sound, previous Hole
Erosion54941d0e, main disk Danmaku04396ca4 and former main2558af6b fallback.
Main remains unchanged. Cumulative cleanup:289 JARs/10,193,077,832bytes/9.49GiB.
Receipt: `build/verification/weapon-sound-final/build-cleanup-receipt.json`.

## All-item sounds and grimoire ready checkpoint

Afterccb40b6c passed46 results in two matches, the qualified older Hole
Erosion54941d0e archive was verified and removed (38,336,771bytes). Keep current
All-item Sounds/Grimoire, previous Weapon Sound593a39df, main disk04396ca4 and
former-main2558af6b fallback. Main was not replaced/restarted. Cumulative removal:
290 JARs/10,231,414,603bytes/9.53GiB. Receipt:
`build/verification/all-item-sounds-final/build-cleanup-receipt.json`.

## Grimoire consumption correction checkpoint

After95c3db2f passed14 results in two matches, verified obsolete Weapon
Sound593a39df was removed (38,350,217bytes). Keep new Grimoire, previous
All-item Soundsccb40b6c and main04396ca4/former-main2558af6b fallbacks.
Cumulative cleanup291 JARs/10,269,764,820bytes/9.56GiB. Main unchanged.
Receipt: `build/verification/grimoire-consumption-final/build-cleanup-receipt.json`.

## Hedgehog qualification and automatic installation

Build4cd2ef8f passed32 recorded results in two actual matches. After verifying
new/old archive hashes, removed obsoleteccb40b6c (38,391,489 bytes).
Cumulative removal:292 JARs,10,308,156,309 bytes (~9.60GiB). Keep latest
4cd2ef8f, precedingb2f50a60, current main95c3db2f and old main fallbacks until the
running JVM's original artifact is known. Installer reuses the matching
qualified95c3db2f archive as rollback instead of making another duplicate.
User now requests automatic qualified installs; install_when_stopped waits
for normal shutdown, and newer qualified requests supersede pending builds.
Live receipt: build/verification/hedgehog-final/main-install.json.
