# Shared boss rewards, Crocodildo and remaining projectile owners

Component verified internally; **not installed**. Hold all fixes until the next stable
release. Full TMT migration remains active.

Full candidate: test `608c5159bf429ec2d6de6f819625e77d4dfa1ad05978df69cc842879b738deb0`,
production `c659daf563bdd7ac1198fe73fac7650d76db7fa396592d99a8907357ec2b597c`.
Full parent: `6c24e5c5` / `5a872f29`, current-content batch. All other parent archive
entries are retained byte for byte. Twenty explicit Java families, five resources.

## Author decisions and implementation

- Crocodildo has **200 real HP**. Its death from its own nuke is intentional.
- Its pig bombs invoke **the existing normal nuclear engine**, including its terrain,
  waves, suppression, visual and final cleanup. There is no separate Crocodildo blast
  queue or old integrity-air-template implementation. Native explosion cause remains
  the actual boss, including after its death; no attribution to the token dropper.
- TECH causes ordinary rewarded death. The whole encounter and bomb cadence pause
  under the shared TimeStop service. Native body/visual, active age, pending tokens
  and retained bomb source join encounter checkpoint reconstruction. Clone presentation
  uses the existing encounter helpers. Loaded targets are indexed; no full-world NBT
  scans, per-tick NBT serialization or new pathfinding engine.
- Original models, scale, bossbar/name keys, movement blend, attack/death/start sounds
  and particles are retained. Global music is sent once per listener at their own
  position, rather than using the source's distant Y=100000/huge-volume workaround.
- `LbcBossLoot` protects **actual reward outputs before publication**: invulnerable,
  no gravity, glowing, unlimited lifetime and zero velocity. Explicit nuclear entity
  cleanup also excludes these rewards. Foreign nearby drops remain vulnerable; no
  radius search decorates unrelated items. Ordinary match cleanup still removes them.
- Sauvojen Tuntija's `100024 -> 99900` threshold means **124 effective HP**. It now
  uses real 124 HP and the ordinary native death callback, emits Infinity/cloud rewards
  once at its actual position, and has no teleport-down/secondary death-token reward.
  Infinity and cloud wand outputs use the same protected-loot helper. Bounded native
  boss/encounter search found no other HP-sentinel threshold needing this correction.
- Crocodildo's reward is **64 usable native timed nuclear eggs and one upgraded native
  Exasperato**. `64_nukes` was not previously packaged. Its legacy `functions` entries
  must use native26.3 `modifier`/`type`, otherwise count and wand properties are ignored.
  Native checks caught count1 before this correction. The wand reference now uses the
  existing `ported/exasperato_wand` table with the original upgraded/lore/style overlays.
- Halloween Jack is a typed mode of **LbcKapusta**, retaining that owner's fair queue,
  native firework, persistent attachment, clone and rewind machinery. Source Jack
  presentation, smooth deterministic homing, soul-fire trail and arrow damage remain;
  it does not generate cabbage offspring. Its canonical leaf is a native Polymer item.
- Raw `lightning` is the eighth source raycast alias. It reuses the existing queued
  ray terminal and library chain-lightning owner, with one terminal debit and no mana
  charge. Scalar inventory/debit reads use `ItemData` rather than whole-tag copies.

## Qualification

`build/verification/tmt-boss-projectiles-2026-11-07/component-verification.json`.

Seven native suites passed on `5b6f8f3e`: boss-projectiles, sauvojen-boss,
raw-raycast-spells, produce-gadgets, produce-gadgets-rewind, tmt-catalog, tmt-block-use.
Final `608c5159` changes **only BossProjectileChecks$Scene.class**, to restore the
actual TMT carrot's item-used statistic too. Its fresh boss-projectiles scene passes;
all product and six other suite entries are byte-exact to the seven-suite run.

Real TMT Jack input, cloning and TimeStop; Crocodildo's ordinary dropped trigger,
200HP, paused bombing, whole capture/discard/restore of canonical body and passenger;
actual pig landing and nuclear self-kill; immediate64 eggs/upgraded native wand surviving
every later wave **and explicit final cleanup**; foreign item vulnerability; TECH death;
native reward egg use-on/debit/timed nuclear owner; immediate protected Sauvojen reward
without teleport or duplicate; all eight raw spell native inputs/terminals were checked.

10483 guarded inputs and282 author paths preserved. Four author WIP Java files and
author HUD files unchanged; WIP files were not compiled. Normal child shutdown, exact
fixture restoration, no retained locks/listeners. One512MiB compiler followed by one
1280MiB fixture. Main remains `83b0ea27`; no installer, Main restart or RP edits.
Physical client rendering and arbitrary-map/peak-TPS behavior are not claimed.

## Remaining work

Genuine composable one-debit TMT DROP routing for Academy, helicopter and Crocodildo
still needs integration; standalone encounter support does not close those TMT domains.
Do not fabricate held-use callbacks or remove them from the sampler audit as inert.
Keep protected Academy WIP integration separate. Retain the current-content capability
audit, firework ammunition, fully unloaded steampunk rocket, heavy800-active-tick Troll
throughput and final acquisition/atlas/stable-release chain from the parent batch.
