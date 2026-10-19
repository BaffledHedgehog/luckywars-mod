# Tiny Ruby / Tiny Terra: code review

Обновление 15 октября: ниже сохранён первоначальный review старого sampled
попадания. Последующая native-hitbox реализация Tiny Terra прошла отдельный
read-only review и адресный runtime; актуальный алгоритм и границы CPU-замеров
описаны в [PROJECTILE_PERFORMANCE.md](PROJECTILE_PERFORMANCE.md).

Read-only review of `LbcWands.rubyRay`, `terraRay`, and `placeTerraBlock` against `WANDS_THIRD_SOURCE.md`, the two user decisions recorded there, and local Minecraft 26.3 bytecode. This is not runtime or build evidence.

## Исправление, найденное при review

- **Resolved in current code: sound broadcast leaked across dimensions.** Ruby and Terra call `LbcOverload.sound(..., radius=0)`. Earlier, `radius<=0` bypassed the level check and sent packets to every connected player; native `PlaySoundCommand.playSound` filters recipients to `viewer.level() == sourceLevel`. `LbcOverload.sound` now keeps that level check and applies native sound range from `SoundEvent.getRange(volume)`. Native evidence: `PlaySoundCommand.playSound` in the local 26.3 merged jar; current helper at `LbcOverload.sound`.

## Checks that match the contract

- `rubyRay` samples 101 positions at 0.35-block intervals, emits the particle at each sample, passes through any block, and stops before querying an unloaded chunk. It avoids repeated destroy/drop work for consecutive samples in one block. Following the latest user clarification, wand drops do not depend on Creative/developer: destruction always requests native drops. It uses no marker entities or persistent ray state.
- `terraRay` performs one candidate query per cast, remembers attempted targets in a cast-local set, and does not reset native invulnerability. Its per-sample cell `AABB(point, point + 1)` matches the old `dx=0,dy=0,dz=0` selector: native `EntitySelectorParser.createAabb` creates a 0..1 box and `EntitySelector.getAbsoluteAabb` moves it by the exact source `Vec3` without flooring. The user-selected one-hit-per-target contract intentionally differs from the old repeated per-sample damage.
- Terra preserves wall termination and command order: sample particle/sound and target attempts occur before the non-air check; at the first wall it places short grass above, then moss in the wall cell, using flags 258 and explicit neighbor updates. It does not load missing chunks. No separate timer or cleanup state is created.
- Both casts retain existing mana payment, Mage full-set bonus, and trinket cast hook through `cast`; no Creative-mode ability gate was added to the ray methods. Mana costs remain 500/250 as selected.

## CPU note

`rayBounds` now bounds the union of sample cells: it starts one step before the origin, reaches the last sample, then expands the positive sides by one block. Each tested cell is `AABB(point, point + 1)`, so any entity bounding box that can intersect a tested cell must intersect this query bound. Native `Level.getEntities(Entity,AABB,Predicate)` delegates to `LevelEntityGetter.get(AABB)`; `EntitySection.getEntities(AABB)` admits entities by bounding-box intersection, while `EntitySectionStorage.forEachAccessibleNonEmptySection` visits nonempty sections intersecting the query. This removes the former unconditional eight-block padding while preserving candidates that can intersect a sample cell. The query remains an AABB around the ray, and the cast scans each candidate at up to 101 samples; diagonal bounds and dense candidate sets may still cost more. This is geometric/code-path analysis only, not a measured speedup or regression; no benchmark or runtime test was run here.

## Sources

- Project: `src/main/java/me/skywars2/game/LbcWands.java`, `LbcOverload.java`.
- Native 26.3 merged jar: `EntitySelectorParser.createAabb`, `EntitySelector.getAbsoluteAabb`, `PlaySoundCommand.playSound`, `Level.getEntities`, `EntitySection.getEntities(AABB)`, `EntitySectionStorage.forEachAccessibleNonEmptySection`.
- Old datapack reference and user decisions: `agent_notes/WANDS_THIRD_SOURCE.md`.
