# Native KFC test plan

Implemented operator suites in `GameChecks.java`: `kfc-menu`, `kfc`, `kfc-food`, `kfc-rewards`, `kfc-stop-probe`, `kfc-cleanup`.

These tests require an actual match with normal TestAlpha/TestBeta ServerPlayer fixtures. They reuse WandThirdPlayerState, native item use/finishUsingItem, native accepted hurtServer and live server entities. No copied outcome switch substitutes for production behavior.

- Native gun loot and food components; exact model, custom flags and names.
- Burning baby chicken: HP/maxHP30, native eye spawn, velocity6, owner, shared20-tick hand cooldown, no mana and empty-hand rejection.
- Native clearFire yields exactly one native edible with no death loot. Native accepted damage at HP20 produces three owned native small fireballs at Y+1, speed1.6, then expires.
- Forget and actual match stop remove tracked chickens; old object load cannot revive owner binding.
- Food targets XP1/5/15, healing, source native effect durations/amplifiers, attribute arithmetic, three mana resources, loot names/counts, ordinary chicken and empty outcome. Outcome4 executes the actual random branch until all38 native slots have been reached, including previously empty slots, while protecting other armor.
- Outcome14 produces exactly400 separate native item entities over400 service ticks and none afterward; shared cleanser clears/rejects the custom timer.
- Actual native food finishUsingItem must consume a stack, restore nutrition and award the KFC advancement through ConsumableSkillMixin; creative participant accepted, observer rejected.

Run after the root-coordinated build:

```powershell
python tools/smoke_kfc.py --server-dir build/verification/key-reuse-server
```

The runner refuses any other server directory or port than25576, backs up/restores game/player config, records artifact SHA256 and runs two complete real matches with cleanup checks between them. It never builds or touches the main/legacy server.

Status: implemented; build and live execution are owned by the root coordinator. Outcome7 awaits the final semantic decision. Native reward use checks cover snowball consumption, four markers, repeated fake celebration without actual match/profile changes, cultus mana admission and owned bat/passenger placement/cleanup. Service tick boundary probes are synchronous; actual match lifecycle is exercised by real server start/stop.
