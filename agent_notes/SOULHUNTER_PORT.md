# Soulhunter and ordinary Soulless family

Source contracts and boundaries: [source audit](SOULHUNTER_SOURCE.md),
[Soulless service](SOULLESS_PORT.md), [presentation](SOULHUNTER_RESOURCES.md),
[native checks](SOULHUNTER_CHECKS.md).

## Author rulings (user, 25 October 2026)

Asked whether to separate the malformed source posture branches into RMB dash
(5 seconds) and Shift+RMB splash (20 seconds):

> Да, разделить способности и кулдауны

Asked whether to give each metasola its full configured HP:

> Да, полное здоровье каждого вида

The question's initial claim that every source pig necessarily began at 10 HP
was withdrawn after inspection of native attributes-before-health loading. Full
25/50/20/15/500/40 HP is now also an explicit user requirement.

Asked whether each vortex should use its own center rather than searching other
nearby vortex displays (user):

> в целом разницы не будет, че производительнее то и сделай

Each field uses its own center directly, avoiding a second nearby-display lookup
for every attracted entity and avoiding cross-instance coupling.

Deployment decision after the tested build was ready (user):

> Пока оставить сборку готовой

## Native implementation

[LbcSoulhunter.java](../src/main/java/me/skywars2/game/LbcSoulhunter.java) uses a
native MaceItem with Polymer vanilla-mace presentation, retaining native damage,
Density/Wind Burst and fall smash behavior. Source components, model, red name,
SS tier and lore are retained by its scoped loot leaf. RMB remains maintained
native item use; each posture dispatches only its own cooldown, including while
held. Taking the item out of the active hand ends native use naturally.

RMB sets look velocity times3 and dash cooldown5. A real downward-looking native
jump adds1.2Y after the base jump, preserving XZ, with its separate5-second
cooldown. While sprinting airborne, either held hand permits look-direction
acceleration0.2 and one stamina debit per tick. Exhaustion at0 blocks further
acceleration until grounded nonsprinting recharge reaches60. Recharge does not
require the item still held. Source lack of mana/antimagic admission for this mace
is preserved; gameplay roles, action blocking, Greed, frost and Time Stop remain
authoritative. Three positive cooldowns debit once per20 active player ticks.

Shift+RMB deals15 native player_attack to eligible enemies within10. This is an
area attack, with the source ally/self exclusion; the global friendly-mob physical
projectile contact rule does not turn the area attack into a projectile. The seven
source destructive air fills are applied as one union of their exact stepped XZ
mask, Y0..5, writing each resident nonair cell once with native container/block
drops. Support removal and native hurt receive captured causes. The four source
HUD rows retain E197/E201 stamina, E198 dash, E199 jump, E200 splash.

Ordinary Soulless costs the configured1500 default and applies source WILD
ghosting within25 from eyes, including caster/allies. Its source lore still says
15; the actual source radius is retained. Ghosting adds5 seconds per cast and
retains its five named temporary modifiers, invisibility and airborne sprint
behavior. The duration is shown with source E196, without creating a new key.
Native HurtTime10 emits an owned hostile Vex; converting a Vex rolls one complete
metasola payload and retains source downward/no-gravity drop motion. Its existing
wind-charge death loot is source behavior rather than a second metasola reward.

All six metasolas are functional native items with paired native physical pig
hosts and bright item displays. Each pair has independent identity/owner, native
launch/gravity/vulnerability, full HP and source activation delay. Types1/2 retain
both tick-age and second-age paths; other types retain their ordinary delay.
Fields persist after activation until destroyed, suppressed or lifecycle cleanup.
Source per-type damage/healing/pull/mana/fire/resistance behavior is in the linked
contract. Type5 uses the actual fractional fire/lava/void_air template, rather than
an inferred light-only sphere. Native hazard/support changes retain author credit.

The player void-resistance consequence is integrated into the existing mana-second
pass: while positive, a player belowY0 returns toY320, then loses one second.
Time Stop pauses that consequence. Existing void-score checks remain in their
original damage paths; no universal new damage immunity is introduced.

## Acquisition and lifecycle

The ordinary wand and original Halloween mace produce one Soulhunter via the
shared native shapeless recipe in both2x2 and3x3, with exactly two occupied slots,
one item per donor per actual pickup, no consumption or world effects on preview.
The current two-hand use dispatcher supports ordinary wand/material combinations
and a Soulhunter plus another supported item. Original crafting sound/particles
occur only after result pickup. Legacy ground-stack deletion is not reproduced.

Public ready leaves: `lbc:ported/soulcorruption_wand`, `soulhunter_mace`, and
`metasola_1` through`metasola_6`; the narrow mixed table is`lbc:metasola`.
Broad Halloween reward pools stay gated. Academy trials/upgrade/metasola-generation
mode remain a separate next batch; this does not advertise an Academy variant.
Owner departure/death and match stop retire managed fields/Vex and restore owned
ghost modifiers; paired unload retires both actors. Current item epoch rejects
stale or unpaired managed actors. No author RP/template/source-server edits.

Build/runtime/deployment qualification is recorded below after integration.


## Integrated qualification (25 October 2026)

Build `fa036823` passed86 recorded results in two actual mode1 matches (151.33s),
including all14 Soulhunter-family cases twice, the existing shared regressions,
real match-stop probes and cleanup. Exact316 source files/881 classes/2812
resources match the tested ready JAR. No new ERROR lines beyond the existing
fake-bot Polymer packet-context pair. Evidence: `build/verification/soulhunter-final/`.
Main installation was refused while its session.lock was held. The user then
requested leaving this build ready, so the main JAR remains `2558af6b`; no restart
or replacement occurred. This supersedes individual task-agent pending statuses.
Native fixture checks include explicit tick boundaries, not real client pixels,
actual disk restart/disconnect or peak-capacity qualification.
