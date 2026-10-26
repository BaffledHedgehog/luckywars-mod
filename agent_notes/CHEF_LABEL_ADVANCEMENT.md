# Applying a label to the Chef

Author request (user): award a GOAL advancement for applying `wtf_item` to the
Chef. Its parent is the Stand glove advancement, and its icon is the label.
Provide title/description translation keys in the resource-pack template.

New ID: `lbc:true_advancements/legends/chef_label`.
Parent: `lbc:true_advancements/legends/stand` (ORAORA glove, acquired via Stand Arrow).
Frame: `goal`; ordinary toast/chat announcement. Its impossible criterion is
awarded only after successful label application creates a new Chef. The icon
uses the actual label's vanilla backing, repeating command block, `lbc:banan`
model and `wtf_item:1` custom data, following existing advancement icon practice.
New client keys are `lbc.adv.legends.chef_label` and its `.lore` key. Merge
`resourcepack_template/updates/chef_label.{ru_ru,en_us}.json` into the respective
author language files. No source resource pack or existing author language
template is modified, and no new texture is needed.

The infinite-HP Chef accounts accepted damage inside its own `actuallyHurt`,
without reducing HP. Label handling now uses that accepted-hit boundary;
generic Fabric label callbacks skip Chef to avoid duplicate application. A
deliberate label strike can target one's own Chef, following physical-hit
eligibility for this specific target while leaving automatic homing protection
unchanged. The new Chef remains bound to exactly the struck Chef, with the
applying player's ownership. Ordinary target eligibility remains as before.
Indirect damage, blocked/rejected hits, knock, Greed, role restrictions and
failed spawns do not consume a label or award this goal.

`ChefChecks` adds one native fixture: loaded parent/frame/icon; ordinary mob
application without the goal; actual accepted strikes on one's own and another
player's Chef; exactly one consumed label and one bound new Chef; correct award
recipient; and no grant/consumption on indirect damage or rejected knocked hits.
Existing advancement progress is restored for both players by the fixture.
Full-family movement, combat, ownership, survival and real-stop regressions
remain in the same two-match verification.

Build `6e648580845ff631891cf333f28f8188e8a4859dd044cb27ac00f16c207a0228`
passed 22 recorded suite results in two fresh mode1 matches (92.95 seconds).
Each passed all 24 Chef fixtures, naturally ticking pursuit, sheep/Danmaku/
player-rule/Time-Stop regressions and real-stop cleanup. Exact 401 sources,
1130 classes and 3139 resources match the tested artifact. RU/EN fragments
and advancement metadata were checked for matching keys and the requested
parent/frame/icon. Protected author files/deletions and live grimoire overrides
remain unchanged; the author resource pack was read only.

Ready artifact: `build/verification/skywars2-chef-label-6e648580.jar`.
Evidence and current installation status: `build/verification/chef-label-final/`,
including immutable `qualification-6e648580.json` and `main-install.json`.
Installer 24628 confirmed normal shutdown/world unlock and installed this build
on main, retaining rifle build `1265875f` as rollback. It did not restart main.
