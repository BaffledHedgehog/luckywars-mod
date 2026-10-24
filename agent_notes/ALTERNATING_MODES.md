# Academy alternating modes — userfix6, 25 October 2026

The user-requested final selector choice is `Чередующийся` / `Alternating`.
Academy Confuse and Corruption add mode5 after their existing four modes;
Academy Evoker adds mode4 after its three formations. Ordinary random casts,
ordinary trials, Blood, Bass and Acid Corruption retain their existing behavior.

Each used stack owns its `CUSTOM_DATA.alternating_next` cursor (default1),
separate from selected `mode`. Successful paid casts use the existing explicit
action and then advance1..4..1 or1..3..1. Explicit mode casts and free Shift
selection leave the cursor unchanged, including when leaving/reentering the
alternating choice. A cast with no target still spends/advances under the
existing spell contract; rejected admission never advances. Stack component
updates preserve all other current data, including any trial readiness changes.
There is no shared player/global alternating field and no entity physics change.

Configured mana costs, actual two-hand dispatch, native effects/formations,
target/role/team checks, antimagic, personal nomagic and Time Stop remain on the
existing paths. Shift selection remains before mana debit at negative balances.

## Presentation

The read-only author pack was searched for an existing usable translation;
none was found. The one new key `mode_alternating` is supplied in the existing
template `assets/minecraft/lang/{ru_ru,en_us}.json`, with all other template
entries preserved. Merge this key into the matching author language files;
do not replace those files with these partial templates. The author pack was
not changed. Existing selector keys, item names/lore/styles/models are retained.
All three new messages include the fallback `Чередующийся`, so an older author
pack displays the mode name instead of a raw missing key before that merge.
See [resource pack rules](RESOURCE_PACK.md), [Confuse](BLOOD_CONFUSE_PORT.md),
[Corruption](CORRUPTION_PORT.md), [Evoker](EVOKER_BASS_PORT.md).

## Checks and qualification boundary

Existing BloodConfuseChecks, CorruptionChecks and EvokerBassChecks now include
actual item input fixtures for complete sequential wrap, exact source effects
or fang counts, normal cost per successful cast, failed mana admission, Time
Stop cursor preservation, free selector reach/wrap, and independent two-hand
cursors/costs. Existing ordinary/trial, target protection and antimagic checks
remain. Corruption's old selector expectation is updated to include new mode5;
Acid Corruption's four-mode expectation remains unchanged.

These checks are wired into the existing three suite entry points. They were
prepared and statically reviewed only in this task: no build or server was
started, and no runtime pass is claimed. The parent integration must compile
and run the three native suites when authorized by the main task.

## Combined integration qualification

Root built `17daf0d4` and ran114 recorded suite results in two actual mode1
matches, including these native checks and actual stop cleanup. See
[combined qualification](GAMEPLAY_FIXES_2026_10_25.md) for artifact identity,
ready-only deployment state and verification limits.
