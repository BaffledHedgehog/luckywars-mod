# Single-use Academy token (2026-10-23)

User explicitly nerfed only the Academy summoned by academy_token. The base
magic_academy_drop remains unlimited. Both acquisitions reuse LbcAcademy and
the existing native template/trials; no duplicate NPC or inventory tick service.

The token's native TextDisplay uses welcome_to_single_use_academy with its
original style (aqua/nonitalic), dimensions and position. User authors RP text;
code provides an English fallback. Original RP/template assets are untouched.
Trial admission, unmet conditions, trial readiness, unsupported/already-upgraded
items and rejected roles/offhand/Time Stop never spend an instance. Only the
native successful base-to-Academy item replacement commits its spent flag before
callbacks. That instance's text and interaction are quietly discarded; another
click or player's request cannot promote a second item. The building remains.

Only eight source BLUE concrete cells (x4..7,y2..3,z10 relative to template)
become WHITE, and only while still blue and owned by that placement. Later
academies claim each background row when written, including unlimited base academies;
external block edits survive. Instances are independent. No nearby cube scan.
Missing resident chunks defer repaint until the native chunk-load callback;
Time Stop defers it until resume, without forced loads. UUID ownership survives
same-match native entity reconstruction; spent actors are rejected on reload.
Epoch changes and stop clear all records and quietly discard remaining actors.

AcademySingleUseChecks uses actual native template placement and interaction,
trial readiness and native item promotion across distinct real gameTime ticks.
It checks two-token isolation, style/key, exact background, edits, duplicate/race
requests, observer/offhand/frozen guards, repeated base promotions, overlapping
base ownership and real stop cleanup. No private-map or world-clock mutation.
Terrain, native players, scheduled ticks and changed tickets are restored.

Build42ad616b passed two actual isolated mode1 matches:16 recorded suite results
in42.05s across the Academy/Stalin/shared AK batch. It was installed while main
was stopped; rollback112316bc retained and main not started. Exact253sources /
690classes /2716resources match tested archives; three user files were preserved.
The native Academy fixture passed every runtime attempt, including final actual
stop cleanup. No real-client pixels or actual disk unload / restart / disconnect /
peak-capacity claim. Final evidence:build/verification/stalin-academy-final/.
