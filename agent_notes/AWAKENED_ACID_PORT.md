# Awakened acid staff

Ordinary and Academy `acidacid_wand`, native two-donor recipe, shared acid
projectiles and source player counter. The final evidence section records actual build/test/install status.

## Author decisions

- Rifle: "Сохранить очередь после смены предмета и её накопление" (user).
- Gatling/Rifle: "Одно воздействие на цель за тик" (user).
- Existing global policy: killer inheritance is mandatory for direct and delayed
  damage. Later accepted player influence overrides an older harmful effect.
- Existing craft policy: real 2x2/3x3 recipe, one result and one of each donor;
  reject unrelated occupied slots. Free Academy mode changes require no mana.

## Sources and acquisition

[Presentation/acquisition audit](AWAKENED_ACID_RESOURCES.md) includes exact
source functions, existing RP keys and texture/model identity. Runtime source:
`world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/`
in the old server, read-only. Source player release loop is `players_5t`;
`acid_rifle` is also one of KFC's exact112 source counters and is reset by full
purify. `on_fire` is not a bypass-cooldown damage type in either source or port.

Native ordinary Acid (`stacid`) + ordinary/Academy Caustic (`acidwand`) makes one
awakened staff. Native IDs and equivalent legacy custom-data flags work.
Scoped test leaves: `lbc:ported/awakened_acid_wand` and
`lbc:ported/academy_awakened_acid_wand`. Broad loot pools remain gated.
Ordinary S / Academy SS, original name, model, lore and mana score retained.
No RP/template changes needed. Numeric custom-data `tier` means ignition;
it is independent of tooltip rarity and is absent in a fresh Academy item.

## Runtime contract

Every paid activation costs configured1450 mana. Both hands use the existing
dispatcher and pay independently. Ordinary launches one great seed: speed0.2,
age500, Poison I1200ticks/radius5, burning7 ON_FIRE and source accelerated
burning age. It sheds ordinary downward acid with40% chance per tick, and the
first native block contact creates forty owned acid children, then removes the
seed. Existing Gaycaster shares this seed unchanged.

Academy Shift+PKM cycles four modes. Looking straight down (pitch89..90) toggles
ignition instead; first missing-tier toggle enables it. Changes are free even
at negative mana. Ordinary trial: cast while burning after Academy admission;
the actual used stack/hand receives readiness, then normal Academy promotion.

1. Default: great seed, with local ignition selection.
2. Caust: speed0.2, age500, radius15 Poison XI2400ticks, burning15 ON_FIRE.
3. Gatling: eight charges, yaw±30°/pitch±20°, speed3, age200. Poison XI1000ticks
   plus Wither XI200ticks, or burning25 ON_FIRE.
4. Rifle: adds ten releases to the player's queue. One release every5ticks,
   aimed from the current eye/look; queue survives unequip and adds on repeated
   casts. Last used staff supplies ignition, replacing broken source inventory
   lookup. Shared actionbar E109 displays the raw number of remaining releases.

Caust/Gatling/Rifle use the shared native acid actor, passage through entities,
sticky block contact, causal status effects, reflection/reload and acid's allowed
external physics. Their source tags do not make them shield-blockable. Straight
flight preserves native applied impulses without adding gravity/drag. Block
contact never restores a flying velocity after sticking. Burning caust/burst age
increments normally, unlike ordinary/great source acid.

Fast charges keep the source seven half-block visual samples and swept area
coverage, but deduplicate living targets once per tick (user). Ordinary ON_FIRE
HurtTime is preserved; it is not converted to seven accepted hits per tick.
Friendly mobs are hittable, allied players/caster and technical roles protected.
Poison/wither retain source player, and later real attacks win attribution.

Antimagic forbids activation and pauses Rifle releases without spending queued
shots. Time Stop pauses both native acid and the release queue. Full purify,
death/disconnect forget and match stop clear Rifle. KFC's raw+1 becomes one live
release, preserving its original counter units. No extra actor framework, world
scanner or artificial queue capacity limit is introduced.

## Verification

Qualified build `322f2e06`: 26 recorded suite results in two actual mode1 matches (115.91s). Native tests cover real menus/PKM, both hands, dual cost, four mode payloads, poison/wither causes and later-hit precedence, exactly one ON_FIRE call per target per fast-projectile tick, source hurt cooldown, sticky native block contact, free negative-mana changes, accumulating Rifle after unequip, live HUD/KFC counters, antimagic/Time Stop, full purify/forget, actual burning offhand trial, SS promotion and actual match stop. Gaycaster, Prism, CausticOmega, AdvancedSnow, Neutral, Mobgun, Academy and KFC regressions passed. Exact 283 sources / 758 classes / 2770 resources verified. Installed stopped main with rollback `6559f2b8`; server not started. Evidence: `build/verification/awakened-acid-final/`. Native fixtures include manual boundary ticks. No client pixels, actual disk restart/chunk reload/disconnect or peak-capacity performance claim.
