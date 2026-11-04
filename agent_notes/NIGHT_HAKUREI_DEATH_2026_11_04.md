# Hakurei victim-bound death publication integration ? 4 November 2026

Status: source integration and native scheduled coverage prepared; no compile, fixture, server operation, commit, install, shared root documentation or resource-pack mutation by this agent. Root owns runtime qualification. `git diff --check` passed for the owned Java paths.

## Contract

`LbcCombatRewards` reuses the existing indirect-attributed `beforeDeath` admission and nested `withDeathLoot` stack. Existing victim role/map/ordinary-dimension policy remains intact. Skill28 eligibility is sampled into its death record alongside skill16/18. At successful death-scope completion, current epoch and actual owner resolve before handing each captured body to `LbcHakureiSkill.reward(owner, level, publicationPosition, 1)` (four native table rolls).

`CombatRewardMixin.Published` already invokes `published` after successful native `ServerLevel.addFreshEntity`; no mixin change or new registration is required. The collector accepts ItemEntity and ExperienceOrb only, requires a nonremoved body at publication, deduplicates by object identity within the top victim scope, and stores publication positions in arrival order. A body published successfully and subsequently merged still counted as one actual publication. Native stack count and orb XP value do not multiply rewards.

Skill18 copies (`publishingCopies`) and Hakurei descendants (`hakureiSkill.publishing()`) are excluded even when inner death completion publishes into a still-open outer scope. Inner real deaths own their top frame; same victim nested die/dropAllDeathLoot wrappers reuse the existing outer frame. The existing death ledger prevents repeated completion. Skill18 continues to clone only surviving original item stacks; XP remains entirely native and never duplicated.

No loot reroll of the victim, age selector, global production entity scan, periodic newborn discovery, new XP ledger, XP value edit or XP spawn interception was added. Captured candidates exist only for the synchronous death call. Accepted deferred Hakurei work belongs to its existing typed queue and checkpoint owner.

## Actual 26.3 XP publication semantics

Inspected the local Loom `minecraft-merged-d6f9bb6878-26.3-sources.jar`: LivingEntity.dropAllDeathLoot calls dropExperience synchronously. ExperienceOrb.awardWithDirection normally partitions XP and calls tryMergeToExisting before addFreshEntity; successful vanilla merge increments resident count and publishes no new orb. Existing CoreEntityMergeMixin substitutes one award body when core merging is enabled; its ServerLevel HEAD hook can absorb/discard the incoming orb into a resident and return true before real publication. Existing Nexus conversion can likewise consume an orb before admission.

Therefore successful addFreshEntity alone is insufficient: the collector also requires the orb to be nonremoved. A merged resident value increment creates zero newly published bodies and zero additional Hakurei rolls while preserving all normal XP. Root explicitly accepted this publication policy on 4 November 2026 as part of the victim-bound correction to source global Age0/1 heuristics. No new hook is needed for resident merges because they intentionally contribute zero new bodies.

## Prepared native coverage

HakureiSkillChecks now includes genuine credited native cow deaths, two kills in one server tick, nested death scopes, a preexisting native101XP orb absorbing real cow XP with zero new orb bodies/zero XP-body rolls, and inner skill18 original/copy pairs completing inside an outer skill28 scope without leaking synthetic children. Snapshot comparisons are fixture-only, synchronous, use object identity and never drive production collection. Existing unrelated newborn exclusion, TimeStop and accepted nonempty whole-coordinator queue restore remain.

The final cow assertion now compares the immediate per-death body delta instead of all fixture newborns near one victim. Cleanup remains within the existing fixture owner. Required root qualification: compile the final shared source and execute `hakurei-skill` plus existing `combat-reward-skills` (use actual registered group names from root harness), including normal source artifact/resource verification. No PASS/runtime claim is made here.

Files owned/released: `src/main/java/me/skywars2/game/LbcCombatRewards.java`, `src/main/java/me/skywars2/game/HakureiSkillChecks.java`, this note. No mixin files changed.
