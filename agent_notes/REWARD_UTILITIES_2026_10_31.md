# Infinite Lucky Block, Imitator and six native fireball specials - 31 October 2026

This closes eight concrete reward consumers. It does not enable the incomplete
full Chaos reward graph or install a candidate on the main server.

## Source and approved behavior

The author corrected the names to **Infinite Lucky Block** and **Imitator**.
Both are explicitly unaffected by either suppressor.

- `workingitems/spell1_default:45` and `_left:22` roll the original `lbc:luckyblock`
  table at the user for `luckyblockwand:1`. Both raw hands now enter the shared
  dispatcher. There is no invented mana cost, debit or cooldown, and no filtered
  reward pool/retry. Empty source rolls remain valid. The existing native block
  loot tables already own the three physical lucky-block variants.
- `items_tick_tag` / `items_tick:3` / `items_copypaster` select the nearest
  non-Imitator dropped item within one block. Copy the full stack and components,
  preserve the donor, use the Imitator's motion and consume one Imitator per native
  tick. With no donor, preserve it. Only loaded Imitators are visited; no global
  entity scan. Native item state carries the reusable rollback state.
- `projectiles/specials` plus `specials_fire_pernul`, `specials_dragon_pernul`,
  `specials_ender_pernul_rand_2` and `rand_motion_razbros` own six raw snowballs.
  Native snowball use owns sound/stat/debit and actual throw spread. Conversion
  runs in the normal item tick, outside native entity-manager load iteration.

Ordinary small fireball uses quantized Motion x6. Large fireball uses x0.075,
ExplosionPower2; Imba large keeps127 and permanent invulnerability. Dragon uses
x0.05; Imba adds16 native children with source signed integer jitter. Imba small
creates one native TNT carrier and32 invulnerable children, fuse100 and source
local offset / floor(component*10)+uniform(-10..10), then x0.1.

On child landing, source `groundbam` sets the actual TNT fuse2, emits nine ownerless
fangs at each nearby target, fills the original11-cube air-tag volume with fire,
adds real lightning and a separate immediate native strong blast. The carrier
remains alive for its own native fuse. No helper creeper is created. Legacy
`groundbam_fire` is only a fill: particles/sound/kill belong to `groundbam_no` and
are not applied here. This distinction was corrected before final qualification.

Shared physical WILD admission retains self/allies and protected-role/Greed rules,
including the native PvP friendly-fire gate. Dragon-created clouds receive source
metadata before native insertion. Accepted small-fireball ignition and terrain
fire use existing causal ownership. Ownerless fangs keep native magic6 damage
without adding native owner/team immunity. Released actors survive author logout;
landing and child insertion use the actor's level and retain source UUID tags
when no live author is available. Existing native type-based MAG removal applies
to fireballs; the TNT branch keeps its source physical rule.

## Native evidence

Final artifact `reward-utilities-15e7b726.jar`, SHA256
`15e7b726c385020cd9c920ac43be1c40e2a95e6a63d4fd249f2fb9a598b6ee33`.
`loose-rewards` and `special-fireballs` pass in two fresh actual mode1 matches.
The first verifies native two-hand input and exact original loot output, no item/
mana debit, nearest whole-stack copying and one operation per real item-service tick.
The second verifies12 real hand/variant throws with debit/stat, six native types,
source offset/velocity/power/fanout, actual small-fireball contacts and protected
roles, actual allied-player damage from a native dragon cloud, real TNT landing,
retained fuse2/nine ownerless fangs/lightning and released-actor lifetime.

The preceding51abf1d5 run passed Infinity, Chaos candidate, physical self-hits,
Time Stop, production Vanity lifecycle and reward-tool suites in six separate
matches. Its two new fixtures failed on an invalid nonempty-loot assumption and
incomplete fixture attribution cleanup. Only those two fixture classes differ in
the final JAR; all production classes/resources and six regression classes are
byte-identical. Earlier381d0771 and51abf1d5 failures remain in the evidence folder.
The old Chaos fixture now avoids clearing unchanged immutable service collections;
its gameplay expectations are unchanged. Every native child stopped normally,
restored fixture snapshots exactly and left no locks. No physical-client render,
full127-power detonation, natural100-tick expiry or peak-TPS qualification is claimed.

Evidence: `build/verification/reward-utilities`. Protected199 author paths,
live31 overrides and installed main JAR d9087f0a remain exact. No functions,
resource-pack changes, question-file edits, main install/restart or remote push.
The original full25-entry Chaos release gate remains closed pending the rest of
the reward consumers and complete chain qualification.
