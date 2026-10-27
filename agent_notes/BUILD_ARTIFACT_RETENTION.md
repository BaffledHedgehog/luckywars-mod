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

## Ordinary Sauvojen qualification (26 October2026)

Qualified c9e36258 passed46 results in two actual matches. Preserve its ready
archive, installed signed-effects1a8a3825 and operator-controlsb2c91dcf rollback.
Main remains running on1a8a3825; installer11424 waits for normal shutdown.
After exact old/new qualification hashes, resolved parent, symlink, inventory
size and timestamp checks, obsolete875fed3a was removed (39174864 bytes).
Latest cumulative receipt supersedes the historical checkpoints above:
323 JARs /11510486943 bytes. Evidence and receipt:
`build/verification/sauvojen/build-cleanup-receipt.json`.

## Late swords ready checkpoint (27 October2026)

Build8a94c831 passed72 suite results plus8 preparation results in two actual
matches. Verified obsolete Academy Sauvojen archivec6815dec was removed
(39,595,426bytes). Keep new Late Swords8a94c831, preceding Fevgaleab8140165 and
exact installed signed-effects1a8a3825 fallback. Installer11424 adopted the new
qualified request and waits for normal main shutdown; main disk remains1a8a3825.
Cumulative removal:332JARs /11,865,118,822bytes. Exact qualification, installer
and cleanup receipt: `build/verification/late-swords/`. Current build/libs,
native fixture inputs, small evidence logs and protected author files remain.

## Classic melee ready checkpoint (27 October2026)

Buildd039db36 passed74 suite results,8 preparation results and2 scoped setup/
cleanup results in two actual matches. Verified obsolete Fevgalea archive
b8140165 was removed (39,639,008bytes). Keep new Classic Meleed039db36,
preceding Late Swords8a94c831 and exact installed signed-effects1a8a3825 fallback.
Installer11424 adopted the new qualified request and waits for normal main
shutdown; main disk remains1a8a3825. Cumulative removal:333JARs /
11,904,757,830bytes. Exact qualification, installer and cleanup receipt:
`build/verification/classic-melee/`. Current build/libs and protected author
files remain; no running main JAR was replaced and no restart was forced.

## Devil Sword ready checkpoint (27 October2026)

Build5b5b67e3 passed76 suite results,8 preparation results and2 scoped setup/
cleanup results in two actual matches. Verified obsolete Late Swords archive
8a94c831 was removed (39,710,836bytes). Keep new Devil Sword5b5b67e3,
preceding Classic Meleed039db36 and exact installed signed-effects1a8a3825
fallback. Installer11424 adopted the new qualified request and waits for normal
main shutdown; main disk remains1a8a3825. Cumulative removal:334JARs /
11,944,468,666bytes. Exact qualification, installer and cleanup receipt:
`build/verification/devil-sword/`. Current build/libs, small failure evidence,
native audit tools and protected author files remain; no main restart was forced.

## Jump artifacts ready checkpoint (27 October2026)

Build435f3498 passed78 suite results,8 preparation results and2 scoped setup/
cleanup results in two actual matches. Verified obsolete Classic Melee archive
d039db36 was removed (39,745,913bytes). Keep new jump artifacts435f3498,
preceding Devil Sword5b5b67e3 and exact installed signed-effects1a8a3825 fallback.
Installer11424 adopted the new qualified request and waits for normal main
shutdown; main disk remains1a8a3825. Cumulative removal:335JARs /
11,984,214,579bytes. Exact qualification, installer and cleanup receipt:
`build/verification/jump-artifacts/`. Current build/libs, the earlier successful
build log, native audit tools and protected author files remain; no main restart
was forced. The auxiliary Polymer audit JAR is not a retained mod version.
