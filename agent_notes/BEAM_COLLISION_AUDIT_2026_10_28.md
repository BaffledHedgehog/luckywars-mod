# Beam collision audit — 28 October 2026

Read-only Java audit. No gameplay code changed; no native runtime qualification claimed. Scope: shared ray service and neighboring laser call sites, with Academy prism geometry as a comparison. Root investigation owns the meditation fix.

## Concrete candidate

- `src/main/java/me/skywars2/game/LbcGaycasterAcademy.java:225-228`: meditation prism samples its 27-block segment every 0.3 blocks, then damages entities whose native box intersects `new AABB(point, point.add(1,1,1))`. Every sample is the MIN corner, so the damaging volume extends a full block toward positive world X/Y/Z, independent of beam direction. This is asymmetric relative to the warning/particle centerline (`:221`, `:225-226`), and exceeds its apparent narrow width. A player with 0.6-block width can be hit while its center is almost 1.3 blocks on the positive side. The candidate is independent of shared raycasts.
- Academy's candidate collection `:164-168` expands the union of helper positions/segments by 16 blocks. This is broadphase only: each hazard applies its own later geometry. There is no evidence that the entire 16-block box directly damages players.
- Separate potential feedback issue `:296-300`: `hurt` calls `failed` regardless of the boolean returned by `hurtServer`. A geometrically overlapping but rejected damage event can still fail another active meditation. This affects perceived hits but does not create remote geometric overlap.

## Shared ray service

- `LbcExasperato.java:300-313` normalizes the direction and enqueues a separate `Search`; `:333-354` processes finite straight segments with block, shield/suppression boundaries and terminal hit handling.
- Entity query `:346-347` calls native `ProjectileUtil.getEntityHitResult(level,owner,from,end,new AABB(from,end).inflate(1),filter,0)`. The inflated box selects candidates; the final zero is native narrowphase padding. This is not an unconditional hit on every entity in the candidate box. No shared broadphase-only false-hit bug identified here.
- `LbcGaycaster.java:61-74` and Academy colored casts `LbcGaycasterAcademy.java:70-74` both use this queue. Meditation prism hazards do not call it.
- `LbcWands.java:856-864,867-882` likewise use native entity clipping with padding zero after resident wall clipping. `LbcLaserMinigun.java:202-217` uses `firstHit`, damages only its returned entity, and draws to that same endpoint. No oversized direct-hit box identified in these laser paths.
- `LbcWands.java:1022-1024` initial electro ray and `LbcBolts.java:408-410` item-target ray also use padding zero. Their one-block broadphase margins should not be reduced as a purported collision fix.

## Payload areas, not direct-ray widths

- Ordinary Gaycaster block impact `LbcGaycaster.java:86-88` deliberately selects target feet positions inside radius 1.5 around impact; electricity `:114-116` uses radius 2 around its offset impact point. Direct entity impacts select only that entity. Acid emission at `:112` subsequently uses its physical projectile service.
- Academy colored impact has equivalent radius-1.5 block impact selection at `LbcGaycasterAcademy.java:78-80`. These can legitimately affect an entity beside the ray endpoint and must be distinguished from meditation prism intersection.
- Academy electric walls have explicit roughly 2x2x2 collision per lane (`:263`), separate from prism and shared ray search; phase-5 flooding uses radius 12 and floor equality (`:274`). Neither is an accidental inflated ray candidate box.

## Test entrypoints and gaps

- `GameChecks.java:941-943`: check names `gaycaster`, `gaycaster-stop-probe`, `gaycaster-cleanup`; implementation `GaycasterChecks.run/stopProbe/cleanup` at `GaycasterChecks.java:418,422,433`.
- Existing queue checks `GaycasterChecks.java:330-355` cover queued firing, Time Stop and finite 125-block empty-sky rendering. Hazard visuals/counts occur at `:253-255,375-389` and electric lane probes around `:168-190`.
- Add targeted positive/negative-side prism misses/hits, horizontal axes, diagonal rays, vertical separation, endpoints, and native player-box edge contact. Assert both HP and trial retention. Reuse the shared queue fixture to demonstrate that a nearby parallel off-ray native box misses despite being inside the inflated candidate box; include a genuine edge hit and an intervening wall. Existing queue/render/lifecycle checks do not establish prism side symmetry.
