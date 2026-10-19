# Looper viewer projection checks

`GameChecks.checkLooper(game)` exercises the native Polymer conversion with
two independent recipient contexts. The authoritative artifact remains an
unknown-description stack with its original components; the recipient with
LBC skill 3 gets the source `artifact_1`…`artifact_23` lore, and the other
recipient gets `mystical_artifact.lore`. The check verifies the original lore
row, translation key, yellow/red color, non-italic style, exact Polymer
roundtrip, and unchanged server stack for every source state.

The same conversion check covers a spawned native `ItemEntity`'s
`getItem()` stack for both recipients, then calls the native viewer refresh
hook and verifies that ground stacks remain unchanged. Fake connections do
not acknowledge chunks and may not enter the native tracking set; this is not
a verified network delivery test. It also covers perk and config changes
followed by loadout capability refresh, and stacks already resolved by the
former mutating implementation. Invalid state, an
unmarked artifact with no source state, and artifact-like custom data on an
ordinary item are covered as inert cases. The observer recipient is checked
while in the observer role, since display eligibility is based on configured
skill 3 rather than gameplay-item eligibility.

These are server-side projection checks. The ground-item case asks Polymer to
convert the native entity's real item stack for each recipient; it does not
capture or simulate a network packet. A live Minecraft client is still needed
to verify rendered tooltip appearance and delivery of refresh packets for
already tracked items/equipment. The first iteration's tracking assertion failed
because the bots lack chunk acknowledgements; its log is archived separately.

The fixture is wired into the existing `magic` runner group. Final build passes;
magic focus passed in 62.93 s, full regression in 260.82 s (14 matches, all
39 groups), using SHA-256
`200896407820742227cfde745a90216cecb5a86cd87929b6c2383c3ec647b11f`.
Deployment evidence and limits: [SERVER_TESTS.md](../SERVER_TESTS.md).
