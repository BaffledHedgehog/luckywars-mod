# Soulless Academy port (25 October 2026)

Extends the native ordinary Soulless wand and its six existing metasolas.
`skywars2:academy_soulcorruption_wand` uses the original item model, full authored
lore, A-tier tooltip, source mode labels and upgrade translation keys.
Test acquisition: `/loot give @s loot lbc:ported/academy_soulcorruption_wand`.
The normal Academy now assigns and promotes the actual used Soulless stack;
a token-built single-use Academy is consumed on successful promotion.

Shift+use on the upgraded wand selects between ordinary WILD ghosting and
creation of one random undeployed metasola at the caster's feet. Selection
is free and independent per hand. Paid casts retain shared gameplay, Time Stop
and antimagic guards, configured cost, cast hooks and offhand configuration.
Generation uses the existing six-entry `lbc:metasola` table.

## User decisions

> сделай допуск и расход 1500, без возвращения 1000 маны получается

Mode2 now admits/spends1500 at the default configured wand cost; the old1000
refund is removed. Ordinary mode and meditation retain their configured spend.

> Сохранить 16 враждебных vex и немедленное открытие

Meditation immediately readies the actual ordinary trial stack and summons16
ordinary hostile native Vex at playerY+1.6. They are untamed and can attack
the caster/allies, without the ghost-hit Vex wind-charge reward. Native ownership
resolves through the shared indirect-kill service. Already summoned ordinary
Vex survive a later antimagic field; initial use is blocked there. Owned helpers
retire quietly on owner invalidation, unload or match stop and are not saved.

## Sources and validation

Source audit/presentation: [SOULLESS_ACADEMY_RESOURCES](SOULLESS_ACADEMY_RESOURCES.md).
Native test contract: [SOULLESS_ACADEMY_CHECKS](SOULLESS_ACADEMY_CHECKS.md).
Ordinary mechanics: [SOULLESS_PORT](SOULLESS_PORT.md).
Build/server qualification follows below after the final tested artifact.

## Integrated qualification

Build `63d98904` passed 26 recorded suite results
in two actual mode1 matches (55.21 seconds), including ordinary
Soulless, all six metasolas, Soulhunter and actual retained-trial match-stop cleanup.
Exact 343 Java sources / 939 classes /
2813 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-soulless-academy-63d98904.jar`. Evidence: `build/verification/soulless-academy-final/`.
The main JAR remains unchanged at2558af6b; READY ONLY is the user's existing choice.
No author RP edits or translation fragments are required. Native fixtures do not
qualify real client pixels, actual disk restart/disconnect or peak-load performance.
