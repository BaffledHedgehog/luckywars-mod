# Native shields and tridents — 26 October 2026

## Source

Read-only legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Acquisition is `lbc/loot_table/transformables_trident.json`,
`transformables_shield.json` and `electro_shield.json`. The Holy ability is
`lbc/advancement/holy_shield.json` →
`lbc/function/items/shields/holyshield.mcfunction`. Its achievement checks a
zombie whose actual death-loot table is `lbc:reactive_shish`, regardless of
whether that particular pulse successfully deals damage. The existing
`true_advancements/tridents/*` and `true_advancements/shields/*` remain native.

This batch adds four ready leaves: `lbc:ported/sea_legend`, `harpoon`,
`hylian_shield` and `holy_shield`. The complete two-outcome trident pool and
three-outcome shield pool are now public in the loot catalog. The original
`lbc:electro_shield` alias now returns the already implemented native Electro
item, rather than an inert vanilla shield with its old flags. Its component
payload is unchanged. Broad LuckyWars mode/outcome activation is separate.

## Behavior

Sea Legend is a real vanilla trident with RiptideX, the original mainhand+5
attack modifier and tierC presentation. Dry/wet admission, charging, launch,
spin, collision, durability and sounds belong to vanilla. Harpoon is a real
trident with LoyaltyI and ImpalingX, tierD and vanilla charged throws, damage,
pickup and return. Native projectiles retain the actual thrower and complete
weapon stack, and participate in the existing projectile/time-stop lifecycle.
No replacement ray, synthetic use animation or extra projectile is introduced.

Hylian Shield is a real unbreakable vanilla shield with UnbreakingX and the
six original banner layers over blue. Native directional blocking, blocking
delay and offhand use remain. Holy Shield is a real vanilla shield with its
three yellow banner layers over light blue, source lore and tierB. All source
translation keys, colors, styles, attributes and hidden tooltip components
are preserved. Both use vanilla shield rendering without new RP textures.

Holy pulses use the same native `UsingItemTrigger` boundary as the source
criterion. They operate only while the actual shield is being used, from
either hand, without mana admission or debit. The origin is eyes+look×3 and
the range is a sphere of radius4 measured to entity positions. Terrain does
not obstruct this source area. Only the original `unholy_monsters` tag is
affected: native Instant HealthI behavior means6 magic damage to undead or
4 healing if a future tag member has ordinary healing behavior. Real magic
HurtTime remains; accepted damage retains the shield user's causal author.
Owned/allied undead mobs are included, while service roles and active Greed
remain protected. Ten yellow dust particles appear at each eligible target's
position+Y1. There is no additional sound in the source to reproduce.

Dimension/personal time stop, blocked actions, inactive participants and the
source Nexus/Imprinted exclusions prevent pulses. Source antimagic does not
block this blessing. Releasing the shield ends pulses immediately. There is
no surrogate cooldown, timer map or extra per-player tick scanner.

Every in-range native demonic powder field is purified by removing its display
and releasing its AI leases/duels. This is the source's direct display removal,
without invoking a terminal Bafomet curse or Ahriman attribute penalty. Outside
fields survive. The shared glyph service exposes this bounded cleanup method;
ordinary glyph expiration/contact/destruction behavior is unchanged.

## Resources and verification

`tools/import_lbc_native_gear.py` writes only the four narrow leaves, both
complete original pools and the original Electro alias. Its checker
`tools/check_native_gear.py` is read-only: seven exact migrated tables,
all12 existing author RP translation keys and original banner components.
No new RP assets or translation fragments are needed. Do not regenerate or
write the live grimoire override pack or author-applied RP files.

`NativeGearChecks` exercises actual native input in both hands, source pool
closure and native Electro identity, Holy eye-relative sphere boundaries,
terrain passage, owned undead/roles/Greed, ordinary magic cooldown and expiry,
free use, particles, powder purification and the real Holy achievement.
It covers Hylian frontal/rear blocking and unbreakability, charged native
Harpoon throws/impact/Loyalty pickup and preserved weapon components, exact
time-stop velocity restoration and native dry/wet RiptideX use and spin.
The stop fixture retains a thrown trident and an out-of-range powder field
while holding a shield; actual match stop must remove the native actors.

`tools/smoke_native_gear.py` uses isolated port25576 and two actual mode1
matches. It includes glyph AI, existing Electro equipment, real totem death
and replacement-player respawn, integer anvil/persistence, previous tools,
summon/Guardian, Syringe, friendly projectiles, KFC, eggs and indirect kills.
Artifact/source/class/resource equality and protected author-file hashes are
checked separately before installation. Client pixels/audio and peak TPS are
not inferred from native packet/behavior checks.

The combined run exposed an older Electro equipment fixture leaving its
reflected virtual Electro trace alive after returning. Its cleanup now removes
only newly created fixture traces and retains earlier trace objects without
advancing their simulation. Production reflection and projectile behavior are
unchanged; later fixtures can again require empty independent payload services.

Final release: `a82498d09869e6c30a1c5da96dfdcbed7718700452b54161f9492f7e394f77e6`.
Build and30 recorded groups across two actual mode1 matches passed in57.88s.
Exact401 Java sources,1130 classes and3138 resources match the release.
The only ERROR log pair is the established fake-player chunk-packet context
diagnostic; no final native suites failed. The primary server is running on
port24566 with RCON disabled. Installer13420 is waiting for normal shutdown
and the world lock, then will install automatically with7da14e96 as rollback.
It never kills/restarts main or overwrites a running JAR. Actual installer
status is in `build/verification/native-gear-final/main-install.json`.

Evidence: `build/verification/native-gear-final/qualification-a82498d0.json`
and `build-cleanup-receipt.json`. Protected user files/deletions and the live
grimoire override pack retain their original hashes. Oldest da260679 archive
was retired; retain the qualified new release, current7da14e96 and previous
7dc0342b. Historical focus runs and the pre-cleanup regression failure are
not release evidence; the final artifact is independently qualified.
