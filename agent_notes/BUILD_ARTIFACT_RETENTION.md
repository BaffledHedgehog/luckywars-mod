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
