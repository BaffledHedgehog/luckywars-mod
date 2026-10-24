# Label Chef: native mannequin rework

## Author skin (user, 25 October 2026)

> E:\servers_mew\skywars2_last\viktorbarinov1.png - положи куда надо и убери из корня

The exact 64x64 PNG is moved to
[chef.png](../src/main/resources/assets/skywars2/textures/entity/chef.png).
Its bytes remain unchanged: SHA256
`bc1ca79f70a6e01688495db3abb3fccccb60298c40f7b6d049d6dbf46848eb36`.
The original server-root file is removed by that move. Each Chef receives a
static native `ResolvableProfile` before spawning, with the resource skin patch
`texture: skywars2:entity/chef`, `model: wide`. Native profile metadata carries
the patch to vanilla clients; no signed skin upload or remote username lookup is
needed. Existing Polymer mod-assets registration includes the PNG in the generated
server resource pack. Clients must load that server pack to see the skin.
The author's separate resource pack is unchanged.

User ruling: each Chef remains bound to the actual accepted melee victim. Replace the old `lbc:shef` display with a real Minecraft mannequin NPC, with server-only terrain-aware pursuit, tools, an infinite building material supply and a sword. HP is infinite; each 20 accepted damage enters 200 ticks of motionless, harmless stasis. Void falls return the Chef to a nearby actual surface and pursuit continues. No additional client mod is required.

Implementation is in [LbcWtf.java](../src/main/java/me/skywars2/game/LbcWtf.java) and [LbcChefBrain.java](../src/main/java/me/skywars2/game/LbcChefBrain.java). Chef subclasses the native 26.3 `Mannequin`, whose protected `Level` constructor retains `EntityTypes.MANNEQUIN`, native Avatar metadata, ordinary mannequin attributes and ordinary living-entity physics. Ordinary mannequins remain unchanged. There is no fake player, projectile carrier, display follower, custom entity registration or client dependency. The original `shef` name and six source sounds remain; the obsolete repeating-command-block model is no longer rendered.

The accepted melee hook consumes one Label only after a Chef was successfully admitted. Its final target reference is the exact hit entity; there is no nearest-enemy substitution. The service removes it on owner/target death, observer/lifecycle disqualification, target dimension change, unload, disconnect or actual match stop. Temporary target immunity causes waiting. Owner freeze does not delete an already released NPC. Shared target/damage rules still protect technical roles, allied players and active Greed; the common summon-owner resolver excludes the Chef from its owner/allied homing while physical friendly-mob projectile hits remain possible; this physical NPC does not become a magical projectile or reflect into a different victim.

Pursuit uses a bounded independent server implementation of action-aware A*, not the Baritone client implementation or a claim of full Baritone equivalence. Each planning slice polls at most 256 queue entries, with at most 1536 polls across all Chefs in a server tick, including stale entries. When a slice finds only a necessary detour, its frontier, costs and parent chain persist into the next active slice. Once a closer segment or the goal is found, it executes that segment and replans toward the same uncapped-distance victim, using resident chunks only. A retained search is capped at 16384 distinct positions and 32768 queued entries; an exhausted/capped search retries after the normal delay. A moved start, materially moved target, stasis, invalid next step or lifecycle reset invalidates it. A node can walk, climb or descend one block, clear two-block headroom, attach a cobblestone bridge, or jump-pillar upward. Diagonal ascent requires an already supported step; unsupported ascent uses a same-column pillar before a horizontal bridge. Pillar jumping clears horizontal velocity and uses zero horizontal input, retaining native vertical jump physics. A near-reached node is retired only after actual support exists below it: passing through its air coordinate during a jump cannot skip the pending pillar placement. Native input/travel/collision, jump power, gravity, friction, liquids and entity pushing perform movement. Moving/stuck targets invalidate cached segments. A planning slice can yield no path and retry later; loaded terrain and the chosen local action graph determine progress. No chunk is forcibly loaded.

Native netherite pickaxe, axe, shovel, hoe, shears and sword are held internally; the fastest native destroy-speed tool is equipped for mining. Breaking accumulates the standard hardness/speed divisor, with the correct-tool penalty and normal resource-drop eligibility. Stationary mining is exempt from movement-stuck resets, so long breaks such as obsidian can complete; replacing the block state resets progress rather than transferring work to a different block. Work and block breaking stay within four blocks of the eyes; building stays within three blocks. Unbreakable terrain is excluded. Bridges attach to an existing solid face, require unobstructed replaceable cells and respect the world border. A Chef does not drop its own placed blocks when it mines them again. The tool stacks and building supply are never consumed, and player interaction/equipment-slot access is rejected. There is no loot/death callback or equipment extraction path through the NPC.

Sword attacks use native mob-attack damage, 8 damage, a 20-active-tick attack interval, sight and a 2.5-block reach to the bound victim. They retain native target armor, damage cooldown and common guards. Every real attack captures a fresh owner cause. Mining captures its cause when work starts; placement captures the write cause. Shared `withCause`, `trapChanges` and `supportRemoved` preserve downstream credit and later-hit precedence.

Damage uses the vanilla `hurtServer` pipeline for cooldown, armor, resistance, native hurt sound/animation and attacker memory. The overridden final damage application accumulates accepted damage without reducing HP or entering death. A larger hit during native cooldown contributes only the accepted damage difference. Direct health-reduction attempts are also prevented and counted; explicit death callbacks do nothing. The mannequin uses ordinary health attributes, not an artificially huge max-health value.

At 20 accumulated damage, the accumulator resets and the NPC enters 200 active service ticks of stasis; excess damage does not carry over. Input, velocity, native movement and position writes are rejected, its native tick is paused, and incoming damage is rejected. No attacks, mining or building occur. Dimension Time Stop pauses the stasis clock. Shared cold/stun stops ordinary actions separately. On release the fixed target is pursued again.

Void rescue probes only loaded heightmap columns, within 24 horizontal blocks of the fall. It selects the nearest valid two-block-clear supported surface among these probes, then uses a still-valid last grounded point or a target-near surface if necessary. It clears velocity/fall distance and replans. This is a bounded rescue search, not a whole-world nearest-surface proof, and it never creates a rescue platform. The larger search occurs only for rescue; ordinary pursuit does not scan terrain columns each tick. Spawning uses the same surface checks in radius 12.

The existing Fartus crafting fountain retains its source 60-active-tick particles and sounds. Its epoch, Time Stop and stop cleanup share the same service.

Validation status: implementation and native source/API inspection completed by the implementation agent; no build, server launch or client-pixel verification was performed by that agent. Parent integration owns compilation, boundary fixtures and live qualification. Manual step/physics fixtures must be described as explicit tick boundaries rather than elapsed natural server time.

## Exact author requirement (user, 24 October 2026)

> Да, к цели удара. Ещё - нужен реворк шефа - надо, чтобы это был манекен с ИИшкой (какой нибудь крутой барритон), который будет преследовать цель. У ИИшки должны быть все инструменты, бесконечный блок если надо строиться, и меч в качестве оружия. У неё должно быть бесконечное хп НО система nock-а - если нанести ей 20 урона, она на 10 секунд уходит в стазис (не получает урона, ничего не делает, не двигается). При падении в пустоту - телепортируется на любую поверхность неподалёку и продолжает таргетить цель.


## Integrated runtime qualification (24 October 2026)

Build `b0dfd5ca` passed74 recorded suite results in two actual mode1 matches (168.90s), including Fartus/Chef and the existing shared regressions. Exact 311 sources/857 classes/2797 resources match the tested installed JAR. Main installed while stopped, rollback `0fb48f1a` retained, server not started. Evidence: `build/verification/fartus-final/`. Native synchronous tick boundaries are explicit simulations; no elapsed ten-second wait, real client pixels, actual disk restart/disconnect or general-world/load guarantee is claimed. This integrated result supersedes the earlier pending-status statements by the individual source/check agents.

Runtime log retains only the existing fake-bot Polymer packet-context ERROR pair during bot admission. No new error lines occurred; exact lines are recorded in the qualification manifest.


## Author skin runtime qualification (25 October 2026)

Build `2558af6b` passed 10 recorded suite results in two actual mode1 matches
(56.58s), including the existing Chef fixtures with static native wide skin
profile and native stream-codec round-trip assertions, and actual stop cleanup.
The original PNG bytes occur unchanged in both the built JAR and the generated
Polymer server resource pack. Exact 313 sources/866 classes/2801 resources match
the tested artifact. Main installed while stopped; rollback `07ff2ecd` retained;
server not started. Evidence: `build/verification/chef-skin-final/`.
No real client pixel verification; clients need the Polymer server pack loaded.
