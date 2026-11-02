# Skill 7 armor reserve implementation — 2 November 2026

Final author policy and native qualification supersede this historical implementation/source note: [completed port](ARMOR_ZEUS_SKILLS_PORT_2026_11_02.md). Do not repeat resolved questions.

This note describes the new component helper only. Native damage integration,
catalog admission and runtime qualification belong to the root integration task;
the existence of this helper is not evidence that those are complete.

Author policy supplied to this implementation: skill7 is entirely unjammed;
toughness has2.5times armor's reserve value; consume toughness first, then armor;
share payment between worn pieces; the terminal hit is completely blocked even
when its damage exceeds the remaining reserve. Grant no Resistance effect.
Ordinary native mitigation and existing Resistance continue to apply.

The helper uses2HP per armor point and5HP per toughness point. Only actual
positive `ADD_VALUE` item armor/toughness POINTS form the reserve. Root resolved
the percentage interpretation explicitly: percentage coefficients are not point
quantities. All `ADD_MULTIPLIED_BASE` / `ADD_MULTIPLIED_TOTAL` coefficients and
negative/zero entries remain exact and continue to affect native mitigation.
Native player base attributes and non-item modifiers are not reserve donors.

For each accepted hit the helper reads just HEAD/CHEST/LEGS/FEET and each actual
stack's effective `ATTRIBUTE_MODIFIERS` component, including native item defaults.
Every matching positive additive entry contributes when its slot group accepts
the actual worn slot; no7-entry assumption or historical armor lookup exists.
It drains each attribute through deterministic equal-share water filling between
eligible pieces. If a piece runs out, the remaining debt is shared between the
others; this fixes the source's absent-slot redistribution bug. Within a piece,
all eligible positive entries pay proportionally. Fully exhausted entries become
exact0. Modifier IDs, operations, slot groups and displays are preserved. All
unrelated entries and all other stack components (name, enchantments, durability,
custom data, etc.) survive; no replacement stack or temporary container is used.

`LbcArmorReserveSkill.absorb(ServerPlayer, DamageSource, float)` returns the
original amount without mutation for a missing player/source, nonpositive or
nonfinite damage, checkpoint restoration, bypass-invulnerability sources, or no
positive reserve. Otherwise it drains the reserve and returns0, including terminal
overflow. It does not resolve skill selection, inspect inventories, poll on ticks,
scan/copy NBT, grant/clear effects, or own a standalone Resistance lease.

The root caller must own live `hasSkill(player,7)` admission, config/match/role/
pause restrictions, and source-specific upstream rejection. Call once per
genuinely accepted positive hit AFTER native mitigation and BEFORE absorption/HP
debit. Do not turn the zero return into cancellation of the outer native damage
call: actual hurt reaction/tilt, knockback, criteria and invulnerability bookkeeping
must continue through the native accepted-hit path. Bypassing sources (including
the native void/outside-border family when tagged) must ignore this defense.
The helper independently checks `DamageTypeTags.BYPASSES_INVULNERABILITY`.
No admission or catalog changes are made in this implementation-only subtask.

Read-only legacy source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/armored*.mcfunction`.
The inventory routine granted/cleared global ResistanceV; damage routines consumed
`damage_resisted`, scaled armor20/toughness40, and redistributed four shares with
scoreboards and a temporary container. Those legacy mechanics are source facts,
superseded here by the supplied author policy. Earlier source audit:
[COMBAT_REWARD_SKILLS_SOURCE_2026_11_02.md](COMBAT_REWARD_SKILLS_SOURCE_2026_11_02.md).

No build or tests were run in this subtask, as requested by the root owner.
