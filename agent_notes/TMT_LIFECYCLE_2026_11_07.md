# TMT native unloaded rocket and full Troll lifetime

Component verified, **not installed**. Full TMT goal remains active. Complete parent:
`tmt-shared-drops-2026-11-07/component-verification.json` (`f3b70523`/`954a85bb`).

Current complete internal candidates:

- Test `112e2dedf81089f902df44b71db0822759e4a610cb057fce8505c5ed7c4fab21`.
- Production `3f32154c814fb4b89720a4312fbe42b09ad3976f280d9cd3b5bf5e130bff410b`.

Four Java families, zero resources. All six suites pass on this same final artifact:
`tmt-rocket-unload`, `troll-box-load`, `troll-box-native`, `troll-box-rewind`,
`tmt-gadgets`, `boss-projectiles`. Qualification and exact source guard:
`build/verification/tmt-lifecycle-2026-11-07/component-verification.json`.

The actual native chunk test found a production bug: Fabric ENTITY_UNLOAD is called
at ServerLevel.EntityCallbacks.onTrackingEnd, including visibility changes with no
removal reason. Steampunk rocket used to retire its flight during that transition.
It now keeps that live flight; real removal and unload retain their existing policies.
No replacement physics, copied launch action or synthetic owner tick was introduced.

Actual private TMT input mounts the real Arrow. After two native ticks, the test
dismounts, pauses time and moves it to an exclusively owned remote chunk. Vanilla
saves and unloads the chunk/entity; actual reload creates a different Arrow object
with the same UUID, position, source, true owner and age. Thawing uses exactly the
remaining native flight lifetime, with one terminal removal. Remote entities/region/poi
files and forced-ticket files are restored to their exact initial bytes after normal
child shutdown. The first diagnostic candidate had a resume failure; the next exposed
the earlier tracking-end failure. Only the final six-suite artifact is qualified.

Troll Box uses one real global source emitter, at its genuine cadence of twenty ticks.
It reaches steady39..41 actors; its first power expires after800 active native ticks.
A five-tick TimeStop holds age, motion and emission. Existing narrow RNG fixture is
reused only for source coordinates/lottery; no fabricated actors or manual gameplay
ticks. There is no evidence here for arbitrary stress, peakTPS, client rendering or
random-distributed cold-cache workload. No speculative budget/scheduler change was made.

Existing native Troll/whole-coordinator rewind and rocket/gadget suites also pass.
The boss suite reconfirms real200HP Crocodildo, shared nuclear self-kill, protected
immediate64nukes/upgraded wand rewards, TECH rewarded death and whole-cycle TimeStop.

10485 guarded inputs and281 author paths exact, including the three unrelated source
WIP files and author HUD/RP. One512MiB compiler, then one1280MiB fixture; normal exit,
exact snapshots/tickets restoration and no retained locks. Installed Main stays
`83b0ea27`; no installer, server restart, author resource-pack edits or separate hotfix.

Remaining: meaningful current class/component/service-exclusion coverage, native
private firework ammo, final acquisition/atlas/stable-release chain and remaining
protected source integration. Inventory zero-pending is not full-goal completion.

Firework audit: existing ImbaNativeVariantsMixin already owns held selection,
Crossbow native factory/wear and ChargedProjectiles power for ordinary native
fireworks. Its helper currently recognizes only ordinary/native identities, not a
private TMT firework property. Reuse that path rather than add another ammo factory;
the factory must receive that property's FIREWORKS payload without leaking sibling
components or replacing the physical carrier. Native inventory ARROW_ONLY selection
should remain unchanged, as vanilla only chooses fireworks from a held hand.
