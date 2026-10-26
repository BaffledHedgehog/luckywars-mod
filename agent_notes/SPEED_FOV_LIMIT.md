# Speed FOV, equipment transitions and rifle hands

## User requirements, 27 October 2026

Preserve the previously selected speed contribution multiplier 0.05, then clamp
that contribution to +/-15% of ordinary FOV. Rifle scopes retain their independent
0.25 factor. Native flight/bow factors and the client's own FOV Effects preference
remain separate.

Reimu gohei hand swapping and Laser Minigun slot changes must not expose a frame
with a new movement attribute and an old ability walking-speed denominator, or
vice versa. The outgoing projection now retains the last movement value sent to
each player. A self MOVEMENT_SPEED snapshot and its matching denominator travel
in one native bundle, calculated from the actual snapshot's base and modifiers.
Raw ability resends use that wire value rather than an as-yet-unsent live value.
Already bundled updates are flattened without projecting their children twice.
Login starts from the fresh client movement base0.1. Respawn follows the native
KEEP_ATTRIBUTE_MODIFIERS flag: effective speed is retained with that flag,
otherwise only the last transmitted base survives. Reset and matching abilities
are bundled. This also avoids injecting the old removed player's modifiers
before native PlayerList.respawn rebinds its connection.
Other entities and unrelated attributes are unchanged. Off-thread sends use
immutable published inputs, and no authoritative movement ability/attribute is
changed to obtain the visual result.

SLR/SLG-88 and Magnum fire, scope and mode changes are admitted only from the
main hand. Magnum's paid queued fire stops if the exact gun leaves that hand.
The common offhand-wands option cannot bypass this. AK and minigun hand behavior
is retained. Scope validity is checked against the live main-hand stack while
projecting native packets; ordinary FOV is used immediately after a hand swap.

## TPS feasibility side question

TPS compensation is possible for server-side mining progress and attack charge:
measure elapsed real time rather than counting every server tick as exactly50ms.
This need not change synchronized speed attributes, which would also affect
normal-rate client prediction. Native actions still resolve at server tick
boundaries, so compensation cannot remove pauses caused by a stalled tick.
This user message asks about feasibility; compensation itself is not introduced
by this change.

## Validation

Qualified build72ea985d passed38 recorded results in two actual mode1 matches
(54.28s), including both firearm hands/admission, real source modifiers, pending
and stale movement snapshots, packet codec/client arithmetic, flat bundles,
base-only and keep-modifiers reset flags, offthread sends, scopes, queued fire,
accuracy, sounds, Polymer presentation and cleanup. Independent effect commands,
codecs/save, signed formulas/shadow HP and shared player/projectile/Time Stop
regressions passed on the same artifact. Exact449 Java sources,1236 classes and
3140 resources match the release. Evidence:build/verification/speed-fov-limit/.

The native reset checks send real Respawn packets using common spawn info; they
do not exercise a full PlayerList replacement/reconnect. Packet checks cannot
establish physical Minecraft client pixels, interpolation or real-network latency.
Arbitrary concurrent multi-thread packet producers are not serialized by these
snapshot maps; ordinary native server-thread ordering is covered.

Installer11424 adopted the qualified artifact and waits for normal main shutdown;
main remains1a8a3825. Automatic installation is authorized; no forced stop/restart.
All175 protected author paths and31 live loot overrides remain unchanged. No RP
update required. Three ready archives remain; obsoletecc460bf8 was retired.
