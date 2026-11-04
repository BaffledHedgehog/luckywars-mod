# Mana Vampirism29 / Champion33 native implementation — 4 November 2026

Parent integration, author decisions and actual executed qualification: [ADVANCED_SKILLS_PORT_2026_11_04.md](ADVANCED_SKILLS_PORT_2026_11_04.md). Worker handoff below is historical; its unrun tests are not runtime evidence.

Status: implementation and authoring fixture only; not compiled or runtime qualified
by this worker. Root owns integration, final author decisions, build, fixture,
artifact verification, release and commit. No resource pack files changed.

Source closure is `REMAINING_SKILLS_SOURCE_2026_11_04.md`, sections29 and33.
Both new services take `(GameController, Predicate<ServerPlayer> ownerAdmission,
BiPredicate<ServerPlayer,LivingEntity> targetAdmission)`. Shared `hasSkill` supplies
the catalog/unlock/live role policy, and each service checks native world/player
TimeStop and tick manager state. Constructor predicates own author jammer policy.
No private skill catalog, scheduled legacy functions or damage wrapper added.

## Integration

Call `manaVampireSkill.damaged(victim,source)` and
`championSkill.damaged(victim,source)` exactly once from existing accepted native
`LbcItems.damaged`. Call Champion
`incoming(player,source,original,actual,blocked)` from existing native
EntityHurtPlayerTrigger mixin. Do not require positive damage or unblocked hits for
that incoming route: source accepts shield and Resistance criteria.

Call Champion `clear(player)` for actual switch/death/reset/unload and `stop()` at
match cleanup. Clear respects MatchPlayerCheckpoint.restoring. There is no timer,
refresh loop, owner map or private checkpoint image: exact source-named native
transient attribute modifiers hold all stance state, and native player images
capture them. No Mana Vampire service state requires checkpoint integration.

Outgoing source owner uses shared current source/projectile/summon attribution and
Babylon cause; never previous kill credit. Mana accepts only native damage tag
`minecraft:acceptable_for_manavampirism`; Champion excludes only native
`minecraft:arrow_only`, preserving broad nonarrow owner-attributed source hits.

Mana uses pre-reward mana against maximum+5000, retaining final addition overshoot,
negative victim mana and ordinary queued vanilla Heal I. Monster bonus is a single
15 magic damage attempt on actual classified magical target, with actual owner and
native invulnerability behavior; recursion guard is scoped to that call. Shared
`magic().magical` owns magic_mobs/summactivated/native managed classification.

Champion native IDs are `minecraft:7374yg374g347g37gh34` (+20 armor),
`minecraft:7374yg3274g347g37gh34` (+5 toughness), and
`minecraft:g8wu4g8k4wug84khu84hu48h` (+6 attack damage). Each event replaces the
opposite modifiers, duplicate events do not stack, no timeout exists.

## Qualification entry

Register `CombatStanceSkillChecks.run(game)` under `combat-stance-skills` and its
`tick(game)` on the existing authoring native server tick. It requires root's
`LbcItems.championSkill()` getter. The fixture uses two actual TestAlpha/TestBeta
participants, shared snapshots, native scheduled network clients, actual native
player attack/arrow/magic damage and accepted damage criteria. It checks source
mana overshoot/negative debit, exact single15 magic call/actual owner/no unrelated
HurtTime collateral, ordinary mob exclusion, no magic recursion, outgoing
nonarrow magic stance, idempotent modifiers, incoming Resistance and real shield
warmup/criterion, native player modifier rewind, player TimeStop and queued Heal I
through actual native player updates. It does not manually tick player/effect code.

Fixture has not yet run; PASS text is printed only after all scheduled stages.
Final author jammer matrix, terminal accepted-hit role handling and full coordinator
replay must be qualified by root after integration.

## Pending author/source choices

1. Source29 scans all loaded HurtTime10 magical/summoned bodies. Current requested
   native interpretation scopes extra15 to the actual target; author confirmation
   remains necessary for this source bug correction.
2. Source29 victim advancement uses persistent manavampire source tag independently
   of global skill toggle/legacy/outgoing reward admission. Root selected coherent
   active hasSkill29 admission for reward and debit: never recreate stale source
   tag behavior after disable or swap.
3. MAG/TECH/NONE admission for29/33 is injected and not presumed in these services.
4. Root supplies accepted-hit target policy allowing terminal hits while preserving
   roles/allies. Constructor targetAdmission is the sole role/alliance authority;
   services do not prefilter via wands.target, which rejects dying targets.
