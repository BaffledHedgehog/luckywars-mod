# Academy Consuming: Silence erasure — 21 October 2026

User contract from AGENTS.md, preserved verbatim:

> Consuming, уточнение 20 октября 2026 (user): Silence сохраняет массовое удаление
> обычных сущностей в радиусе 48. «В радиусе 48 просто также должны пропасть все
> блоки»: восемь legacy-шаблонов заменяются настоящей сферой удаления R48.

Ordinary native entities now use the existing `LbcMobgun.dispose`, without
`hurtServer`/`kill`, native death, loot, XP or death callbacks. Its
AbstractCubeMob path uses `setRemoved(DISCARDED)`, bypassing the native remove
method that can split even an already-dead slime. Existing service/observer
guards and lightweight `transformSpellOrigins` remain; existing item drops
are included.

Terrain still uses BlockAreaWork and the shared LbcStonekivi budgeted queue.
An erasure sphere accepts native entity-load events while its terrain and
entity jobs run, then for 40 active ticks after both finish. Delayed falling
blocks, drops and other ordinary entities are discarded on entry. There is
no repeated global entity scan. Pause preserves the window. Owner lifecycle,
epoch, stop and job cancellation retire it. LbcItems calls this guard before
other item services adopt an entity.

Players retain the previous separate 20 outside-border damage and existing
ally/observer/void-resistance protection, confirmed by the user. Players
are never discarded. The user explicitly preserves god_hedgehog immunity
and its 10-billion outside-border retaliation against the caster. Every
initial, queued and entity-load erasure path excludes that tag.

The Silence part of `checkConsumingAcademy` now verifies no new damage/death
state on cow/dragon, disposal of an already-dead size2 slime without split
children, native falling-block tick before landing, delayed falling block/drop
native entity-load cleanup, actual hedgehog immunity and retaliation, survival of
a late service Interaction, retirement after 40 active ticks, and survival of
a subsequent unrelated item. Combined build and two native matches each of
Duplicating and Consuming passed on a1d43d2e (58.88 and 43.96 seconds).
The actual player20 damage and native god retaliation are exercised;
retaliation includes vanilla difficulty scaling for a living non-player source.
Fake-player teleports use the existing dimension-change acknowledgement.
No GPU or maximum-load/MSPT result is claimed; see ../SERVER_TESTS.md.
