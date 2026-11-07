# TMT native passive-domain additions, 7 November 2026

Component verified, not installed; full TMT remains incomplete.

The sampler now admits existing native One Punch, Hylian/Ice Sword, Runic Ice
Shard, Neutral Ball, Clown Mask, Mage/Overload armor and Rocket Boots properties.
Their normal held, inventory, equipment and accepted-hit owners consume the
existing aggregate flags. Native formulas, cadence, actual equipment slots,
resource costs, suppression and native debuff prevention remain unchanged.
No new passive scheduler, parallel mechanic, NBT cache or owner state was added.

The scheduled TMT scene verifies a genuinely mixed One Punch/Hylian main hand:
current HP plus absorption updates only its native damage modifier, retains
both private slots/component and also grants the ordinary nearby-monster aura.
The shard's selected-container plus hand pulse composes with Neutral Ball's
native mana/regeneration exchange. Mixed equipped TMTs satisfy the existing
Mage/Overload masks together; a head Clown property prevents real blindness and
darkness additions, and a feet Rocket property satisfies the native boot check.
This does not claim a new Rocket flight test or a physical-client visual test.

Internal test `f60e4685df1154278e107244dc369dac45dbf6be23c48f883e545db98cfc361d`
and production `4673d2d8db2bc2fce71ddc9656dac2a9129c90954f034c76a998ad6c59c6c360`
carry complete e85bb138 / cbebeaa0. Three Java families, no resources; 10,340
other source inputs and all 282 protected author paths preserved. Main/libs,
author RP and concurrent HUD unchanged. Evidence is sealed in
`build/verification/tmt-passives-2026-11-07/component-verification.json`.

Five actual mode-1 matches pass: tmt-passives, tmt-catalog, one-punch-glove,
ice-materials and clown-kit. Reports verify normal shutdown, exact fixture
restoration and no failures, cleanup errors, live child or retained locks.
The earlier diagnostic test omitted the native maximum-absorption attribute;
its corrected positive control verifies actual HP and absorption before testing
the existing owner. That failed report is retained and is not counted as a pass.

822 families / 1,578 variants is not full coverage. Remaining native domains,
independent owners and acquisition/release gates stay open.
