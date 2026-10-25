# Danmaku source audit — 2026-10-25

Read-only source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

## Contract and unresolved accidents

Ordinary and Academy must operate in both hands; native friendly projectile mobs remain hittable, allied players protected, homing enemies only. New magic projectile external physics requires asking. Source has no free mode selector: sneak is a bomb action, not mode change.

Questions: preserve Academy offhand eyes-height vs other hands eyes minus0.5? Preserve separate hand-authored patterns and source shared cast counter (a single hand holding a staff only fires on its parity)? Preserve enormous uncapped bomb growth from score? Source cleanup only removes player nodmg/nodmg1, leaving bomb-tagged mobs immune to subsequent effects: accidental global state must not be ported silently. Card damage omits attacker; native port must credit owner as required by user attribution contract. Score +3 happens on collision. The user corrected the blocked-hit question: generic already bypasses cooldown through the authoritative tag; preserve that damage type and source score. Bomb source may reassign old displays because it never adds active: use stable owner attachment in native port.

## Exact mechanics

`click`: admission mana >= configured1000 in all four workingitems spell1 default/upgraded main/offhand dispatchers; invokes click_2 then subtract1000 and shared mage bonus/cast hooks. Non-sneak adds20 shared danmaku ticks (queues rather than resets). Sneak refunds1000 unconditionally; score>=50 calls click_bomb, otherwise no effect. Admission still requires1000 even for refunded bomb.

`players_tick.mcfunction:31` only ticks positive counter outside stopper_magic marker20 blocks, so antimagic pauses pending volley. `tick` uses remaining counter modulo20 and parity; main fires phases0,18,...2, left19,17,...1 then decrements. Both held staffs therefore fire alternating ticks. Ordinary main per phase counts9,2,2,2,2,2,9,4,4,4 =40 cards (8 homing); left2 each first9 plus17 at tenth =35 (12 homing). Academy main8 homing each first9 plus17 straight at tenth=89 (72 homing); Academy left6 per phase=60 straight. Source exact geometry is appended below to avoid losing authored decimals.

Cards: item_display repeating_command_block models lbc:card_1 straight/card_2 homing; fixed, scale0.5, interpolation/teleport1, view_range10. Copy owner lbcID2/team_number, mark active. Tick runs detect_rec three times, each moves0.4 =>1.2/tick; target AABB overlaps unit positive xyz selector volume dx=dy=dz=0 with distance cap8; damage0.65 generic then +3 owner score and destroys card. Cooldown bypass is supplied by the generic damage-type tag; no knockback is authored. Solid/non-air block destroys card after movement; any air only, not general replaceable blocks. Homing nearest nonexcluded #minecraft:mobs without range cap; smooth direction uses facing target then0.06 offset and forward1 construction; if no target homing does not advance. Lifetime60 kills, global raycastselector first adds1 each tick. Owner/team excludes players; strict_map_object excluded. No gravity/physics source.

Bomb: two red glass displays positive/negative scale, only act half collision. Copies score to Distance, owner/team act half; grants staff advancement; ordinary main SelectedItem upgradable and score>=1500 calls Academy can_upgrade before reset0. Upgrade file applies tier S (source live tier must be distinguished from other Academy A policies). Lifetime globally increments1 then bomb adds3 =>4/tick, not3! Scale magnitude Lifetime*.2; radius=floor(floor(Lifetime*1.2)*.05), kills after Lifetime>Distance. At1500: about376 ticks, radius90. Hits every tick1 magic credited owner, normal damage invulnerability applies. Deletes enemy vanilla projectiles, magic/mecha raycasters excluding bombs, mob_projectile mobs; skips owner/team/strict_map objects and mob-tagged nonmob raycasters. No terrain destruction. Fast bomb is Touhou kit external dependency (Distance300, yellow glass, damage2), not Academy switch.

## Verification limits

Static source audit only. No build, server, Git, resource pack or Java changed. Source files appended are exact authoritative geometry; lore/model loot registration need separate follow-up before implementation qualification.

## Exact volley source

### shoot_1.mcfunction
```mcfunction
execute rotated ~30 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~15 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-15 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-30 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~70 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-70 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~60 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-60 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_10.mcfunction
```mcfunction
execute positioned ~ ~.1 ~ rotated ~17 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.1 ~ rotated ~7 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.1 ~ rotated ~-13 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.1 ~ rotated ~-23 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_10l.mcfunction
```mcfunction
execute rotated ~30 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~15 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-15 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-30 ~ run function lbc:items/wands/danmaku_staff/shoot

execute rotated ~70 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-70 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~60 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-60 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing

execute rotated ~50 ~-15 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-50 ~-15 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~40 ~-15 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-40 ~-15 run function lbc:items/wands/danmaku_staff/shoot_homing

execute rotated ~30 ~-10 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-30 ~-10 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~20 ~-10 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-20 ~-10 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_10l_upgraded.mcfunction
```mcfunction
execute positioned ^0.443117 ^0.321943 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.500369 ^-0.222779 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.057253 ^-0.544722 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.443117 ^-0.321943 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.500369 ^0.222779 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.057253 ^0.544722 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_10_upgraded.mcfunction
```mcfunction
execute rotated ~40 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~35 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~30 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~25 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~20 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~15 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~10 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~5 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-40 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-35 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-30 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-25 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-20 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-15 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-10 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~-5 ~ run function lbc:items/wands/danmaku_staff/shoot

```

### shoot_1l.mcfunction
```mcfunction
execute positioned ^.3 ^ ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-.3 ^ ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_1l_upgraded.mcfunction
```mcfunction
execute positioned ^ ^0.547723 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.474342 ^0.273861 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.474342 ^-0.273861 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^ ^-0.547723 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.474342 ^-0.273861 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.474342 ^0.273861 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_1_upgraded.mcfunction
```mcfunction
execute rotated ~ ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~90 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-90 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~180 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~45 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-45 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~135 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-135 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_2.mcfunction
```mcfunction
execute positioned ~ ~.35 ~ rotated ~-20 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.35 ~ rotated ~-15 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_2l.mcfunction
```mcfunction
execute positioned ^.3 ^ ^ rotated ~2 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-.3 ^ ^ rotated ~2 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_2l_upgraded.mcfunction
```mcfunction
execute positioned ^0.057253 ^0.544722 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.500369 ^0.222779 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.443117 ^-0.321943 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.057253 ^-0.544722 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.500369 ^-0.222779 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.443117 ^0.321943 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_2_upgraded.mcfunction
```mcfunction
execute rotated ~10 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~100 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-80 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-170 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~35 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-55 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~125 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-145 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_3.mcfunction
```mcfunction
execute positioned ~ ~.35 ~ rotated ~-10 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.35 ~ rotated ~-5 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_3l.mcfunction
```mcfunction
execute positioned ^.3 ^ ^ rotated ~4 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-.3 ^ ^ rotated ~4 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_3l_upgraded.mcfunction
```mcfunction
execute positioned ^0.113878 ^0.535754 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.520915 ^0.169256 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.407037 ^-0.366498 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.113878 ^-0.535754 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.520915 ^-0.169256 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.407037 ^0.366498 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_3_upgraded.mcfunction
```mcfunction
execute rotated ~20 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~110 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-70 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-160 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~25 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-65 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~115 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-155 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_4.mcfunction
```mcfunction
execute positioned ~ ~.35 ~ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.35 ~ rotated ~5 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_4l.mcfunction
```mcfunction
execute positioned ^.3 ^ ^ rotated ~2 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-.3 ^ ^ rotated ~2 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_4l_upgraded.mcfunction
```mcfunction
execute positioned ^0.169256 ^0.520915 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.535754 ^0.113878 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.366498 ^-0.407037 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.169256 ^-0.520915 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.535754 ^-0.113878 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.366498 ^0.407037 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_4_upgraded.mcfunction
```mcfunction
execute rotated ~30 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~120 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-60 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-150 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~15 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-75 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~105 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-165 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_5.mcfunction
```mcfunction
execute positioned ~ ~.35 ~ rotated ~10 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.35 ~ rotated ~15 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_5l.mcfunction
```mcfunction
execute positioned ^.3 ^ ^ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-.3 ^ ^ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_5l_upgraded.mcfunction
```mcfunction
execute positioned ^0.222779 ^0.500369 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.544722 ^0.057253 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.321943 ^-0.443117 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.222779 ^-0.500369 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.544722 ^-0.057253 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.321943 ^0.443117 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_5_upgraded.mcfunction
```mcfunction
execute rotated ~40 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~130 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-50 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-140 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~5 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-85 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~95 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-175 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_6.mcfunction
```mcfunction
execute positioned ~ ~.35 ~ rotated ~20 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.35 ~ rotated ~25 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_6l.mcfunction
```mcfunction
execute positioned ^.3 ^ ^ rotated ~-2 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-.3 ^ ^ rotated ~-2 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_6l_upgraded.mcfunction
```mcfunction
execute positioned ^0.547723 ^ ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.273861 ^0.474342 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.273861 ^0.474342 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.547723 ^ ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.273861 ^-0.474342 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.273861 ^-0.474342 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_6_upgraded.mcfunction
```mcfunction
execute rotated ~50 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~140 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-40 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-130 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-5 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-95 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~85 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~175 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_7.mcfunction
```mcfunction
execute positioned ~ ~.2 ~ rotated ~30 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.2 ~ rotated ~15 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.2 ~ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.2 ~ rotated ~-15 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.2 ~ rotated ~-30 ~ run function lbc:items/wands/danmaku_staff/shoot
execute rotated ~70 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-70 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~60 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-60 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_7l.mcfunction
```mcfunction
execute positioned ^.3 ^ ^ rotated ~-4 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-.3 ^ ^ rotated ~-4 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_7l_upgraded.mcfunction
```mcfunction
execute positioned ^0.321943 ^0.443117 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.544722 ^-0.057253 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.222779 ^-0.500369 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.321943 ^-0.443117 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.544722 ^0.057253 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.222779 ^0.500369 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_7_upgraded.mcfunction
```mcfunction
execute rotated ~60 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~150 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-30 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-120 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-15 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-105 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~75 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~165 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_8.mcfunction
```mcfunction
execute positioned ~ ~.1 ~ rotated ~23 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.1 ~ rotated ~13 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.1 ~ rotated ~-7 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.1 ~ rotated ~-17 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_8l.mcfunction
```mcfunction
execute positioned ^.3 ^ ^ rotated ~-2 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-.3 ^ ^ rotated ~-2 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_8l_upgraded.mcfunction
```mcfunction
execute positioned ^0.366498 ^0.407037 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.535754 ^-0.113878 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.169256 ^-0.520915 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.366498 ^-0.407037 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.535754 ^0.113878 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.169256 ^0.520915 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_8_upgraded.mcfunction
```mcfunction
execute rotated ~70 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~160 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-20 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-110 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-25 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-115 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~65 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~155 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

### shoot_9.mcfunction
```mcfunction
execute positioned ~ ~.1 ~ rotated ~20 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.1 ~ rotated ~10 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.1 ~ rotated ~-10 ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ~ ~.1 ~ rotated ~-20 ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_9l.mcfunction
```mcfunction
execute positioned ^.3 ^ ^ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-.3 ^ ^ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_9l_upgraded.mcfunction
```mcfunction
execute positioned ^0.407037 ^0.366498 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.520915 ^-0.169256 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^0.113878 ^-0.535754 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.407037 ^-0.366498 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.520915 ^0.169256 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
execute positioned ^-0.113878 ^0.535754 ^ rotated ~ ~ run function lbc:items/wands/danmaku_staff/shoot
```

### shoot_9_upgraded.mcfunction
```mcfunction
execute rotated ~80 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~170 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-10 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-100 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-35 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~-125 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~55 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
execute rotated ~145 ~-20 run function lbc:items/wands/danmaku_staff/shoot_homing
```

## Independent recount and exact steering correction

Parsed each shoot function call independently: ordinary main per phase `[9,2,2,2,2,2,9,4,4,4]` =32 straight+8 homing=40. Ordinary left `[2,2,2,2,2,2,2,2,2,17]` =23 straight+12 homing=35. The tenth left phase has5 straight plus12 homing, not13 total. Academy main `[8,8,8,8,8,8,8,8,8,17]` =17 straight+72 homing=89. Academy left6 each=60 straight. Previous31-left total was an audit arithmetic error, corrected above.

At each of the three substeps let P be current projectile origin, d its current unit forward direction, and u=normalize(targetEyes-P). The command context first moves to P+0.06u, restores projectile orientation, moves another d, faces projectile eyes at P, then reverses that facing with local `^-1` and restores position P. Thus new direction is normalize(d+0.06u); teleport moves0.4 along this new direction and adopts it as projectile rotation. Reacquires nearest valid target on every substep. This is vector interpolation, not a constant angle turn. Homing has no fallback forward teleport if no target.

Master source `function/tick.mcfunction:1` invokes tick_not_timestopped only unless time_stopped>=1; `tick_not_timestopped.mcfunction:3` dispatches all slowraycast entities to raycastselector excluding nexus/imprinted dimensions. `raycastselector.mcfunction:1` increments Lifetime1 before player dispatcher, confirming card age+1/tick and ordinary bomb+4/tick (1 master+3 bomb). Ordinary bomb checks distance only after radius damage; final tick can overshoot stored score by up to4. Source fast bomb has master+1 plus conditional+10 until past Distance, then master still+1, empty increments to60. These source clocks pause with global Time Stop.

Parent recorded later user decisions on shared timer, free bomb and current-hand-only behavior; implementation must follow parent's exact decision record over source admission above. The later user correction about the generic damage-type tag is preserved below and in DANMAKU_DECISIONS.md; no accepted-hit-only scoring change was requested.

## Full damage-tag context verified by parent

Legacy data/minecraft/tags/damage_type/bypasses_cooldown.json and the native
import both contain minecraft:generic. Therefore card generic damage bypasses
HurtTime without local timer resets. Earlier local-helper observations about
absence of an explicit bypass do not imply ordinary cooldown behavior.

## Integrated qualification

Build `04396ca4` passed 24 recorded suite results
in two actual mode1 matches (62.1 seconds), including all six actual-tick hand patterns, bombs, source cooldown bypass, Curse/electro bridges and actual match-stop cleanup.
Exact 346 Java sources / 952 classes /
2816 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-danmaku-04396ca4.jar`. Evidence: `build/verification/danmaku-final/`.
The main JAR remains unchanged at2558af6b; READY ONLY is the user's existing choice.
No author RP edits or translation fragments are required. Native fixtures do not
qualify real client pixels, actual disk restart/disconnect or peak-load performance.
