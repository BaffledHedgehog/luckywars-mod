# Match-mode ownership and startup — qualified, 6 November2026

Author decision: editing gamemode during a match selects the NEXT match. It must
not reinterpret the current map, participants, beds or abilities immediately.

GameController now owns one activeGameMode value. begin captures the requested
configuration before countdown. The existing stop path first clears abilities,
BedWars, team assignments, events and participants, then releases the value;
the lobby exposes the next configured mode. Configuration remains persistent
and editable at every stage. No new timer, worker, polling pass or mode engine.

The explicit GameController.Checkpoint captures/restores the active value before
dependent owners restore. MatchCoreCheckpoint already owns configuration and
the remaining native state, so a captured current mode and different pending
configuration remain distinct through that same coordinator. No private rewind
implementation or alternate player body was introduced.

## Shared source policy and gameplay readers

The arena chest supplier, Lucky modifier start, BedWars family, Boba, corrupted
glyphs and Wrath now read gameMode(), not mutable future configuration. Cages
also use the captured mode. Public new-match/restart admission and requested map
selection continue to read configuration because they start a NEW match. Lobby
menus display the future selection. BedWars recognizes source family3/4 while
public4 remains gated. Balance retains its separate legacy overlay.

| Source mode | Solo teams | Initial source XP | Family |
| --- | --- | --- | --- |
|1 | distinct native teams |0 | SkyWars |
|2 | distinct native teams |20 levels, before kits | SkyWars |
|3/4 | BedWars owner, no solo reassignment |0 | BedWars |

Source SWRG game/start/0 assigns solo teams for1/2 and copies configured lives.
LBC start grants20levels only for2. Shared phase helpers now implement these
source distinctions in the actual begin path; native kit11 may independently
add20levels afterwards. The fixture explicitly uses the existing ranked access
bypass to make that kit available and separately checks disabled kits; no
production access rule is relaxed. Configured lives remain unchanged, including additional
lives. LbcLoadouts, mana/Nulla ordering, retained Stand eligibility/120active-tick
grant and all other existing ability owners remain shared and unchanged.
Receiving XP never manufactures have_stand entitlement or a Stand item.

Requested map family is validated at selection, preparation, cached readiness
and next-map configuration. A wrong-family selection cannot paste or replace
the pending map. The new single translation key is provided as merge-only
RU/EN additions in resourcepack_template/updates/match_mode_2026_11_06. The author
pack was read-only and lacked this key. The embedded Russian fallback works
before merging; existing deleted template/language files are not restored.

## Actual native proof and release limits

Exact test `b8d1066e`, production `256dc67b`. Twelve isolated matches pass on this
test hash: seven actual SkyWars1 and five actual BedWars3 matches. Supplemental
lobby checks cover all four requested families, cached readiness, wrong-family
selection rejection without paste/config mutation, and preserved public2/4
admission gates. Actual persisted setting changes preserve the live family and
real chest behavior. Native player XP/kit ordering, real distinct solo teams,
life preservation and typed core replay are checked. The final probe in BOTH
cohorts leaves a different next setting through the actual stop command and
verifies lobby handoff, cleared BedWars/team ownership and exact file restoration.

Existing Lucky chest acquisition, Lucky modifier resolver/service, corrupted
glyph family policies, mana/Nulla, team colors and BedWars basic/lifecycle checks
also pass. Source-policy fixtures that deliberately exercise several families
now enter the same explicit mode phase and restore the actual old mode, rather
than depending on midmatch configuration edits. Independent modifier-service
checks use its explicit startup-context overload. Assertions are retained.

Only13 fresh owned Java families and0 source resources are overlaid on qualified
parent test/production archives. 10290 other inputs and282
author paths remain byte-exact; protected Academy work is not shipped. Three
new RP handoff files are separate from the archives. Retained binary linkage,
all archive bytes, normal child exits and exact fixture/auxiliary restoration
are checked. Temporary BedWars maps are removed. Both build outputs and stopped
main JAR are installed with exact hashes and rollback; no main restart or push.
Gradle512MiB/one worker/no persistent daemon and2GiB native fixture run sequentially
with RAM checks. Other user Minecraft/Velocity/editor heaps are untouched.

The four-mode phase checks are synchronous inside admitted1/3 matches; they do
NOT claim a full public2/4 startup or mode4 economy qualification. Those gates,
TMT-last/full reward graphs/custom-mixed-type pools/dependent kits/skill2,
protected Academy and general rewind/release validation remain separate. No
physical-client, peakTPS or disk-restart qualification is claimed.

Evidence: build/verification/match-mode-start-2026-11-06/qualification.json, main-install.json,
template-proof.json, package-proof.json, linkage-proof.json, the sealed native
logs/results, remaining-catalog-proof.json and local-commit.json.
