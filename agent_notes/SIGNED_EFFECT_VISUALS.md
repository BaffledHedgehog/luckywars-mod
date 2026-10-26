Root qualification: build1a8a3825 passed the dedicated native suite and broader
regressions in two actual matches (68 results total); installed by worker19140.
Evidence: build/verification/signed-effects-gameplay/.

# Signed effect native-client presentation (26 October 2026)

Author RP remains read only. Changed files live only in resourcepack_template;
copy those paths into the RP manually. Existing deleted templates are not restored.
No translations or existing marker colors/rows are replaced.

Integration: register SignedEffectVisualMixin$Flags, $Particles, $Living,
$Metadata, $ClientEffect and $Player. Call SignedEffectVisuals.tick(server) every native
server tick; joined(player) after login and replacement-player respawn;
clear(player) on disconnect; stop(server) on actual server stop. Effects callbacks
and entity removal are self-contained in the dedicated mixins and existing
FullInvisibilityEntityMixin. The helpers preserve unrelated native post effects.
Native raw effect packets keep the signed amplifier and duration without an
outgoing recipient context; recipient-context serialization sends duration zero
for nonpositive native binary visual effects so the client cannot activate I
by clamping the signed amplifier. The authoritative effects are untouched.

Level is (long) amplifier + 1, including 2147483648 at IntMax amplifier.
Negative invisibility is glowing, zero invisibility and glowing are inert.
Positive invisibility I uses native rendering; II suppresses effect particles;
III+ retains the existing player Interaction proxy, camera exemptions, pickup
and passenger guards. Negative blindness subtracts its magnitude per viewer,
with retracking at thresholds and restored particle metadata. Glowing is never
used as a reveal channel. Other entities receive the metadata/particle tier
projection; full Interaction replacement still applies only to players, as in
the existing implementation (custom mob Polymer constructors are not replaced).

Native blindness/darkness I remains; II darkens the scene and III reaches black.
Negative darkness increases brightness and contrast. Negative night vision
inverts scene colors with contrast; this approximates inverted lighting because
post effects cannot recover the original unlit material/lightmap separately.
Positive night vision II+ adds contrast. Positive nausea adds animated warp,
edge darkness and a dark red pulse; nonpositive nausea has no active shader.
Visual contrast/warp parameters saturate at16; signed storage never saturates.
Each active family uses one2-pass chain, at most four chains after deduplication.

Individual glowing II+ uses native client-only ItemDisplayElement carriers,
reusing the existing invisible lbc:black_hole_small helper geometry. Each viewer
has eight nearest tracked actors; original native silhouette outlines still
render for all other actors. The carrier follows the actor center, carries
log2(level), native actor height and the original TeamColor palette ID. Fresh
marker colors #D57500..#D57507 write only rows50..57 into the outline target.
Zone row0, black-hole rows2..17 and nuclear main/fade rows18..49 remain intact.
A dedicated pass before native entity_sobel reads the helper records, adds an
independent capsule-shaped halo per actor and preserves scene alpha exactly,
including existing black-hole/nuclear/camera transport. Level growth increases
radius to screen size with fixed shader cost. Existing real entity teamRGB,
name tags, hitboxes, armor, camera and entity IDs are untouched.

Limits: the extra halo is an analytic capsule around each actor, rather than
an exact per-entity silhouette convolution. Overlapping halos add. Only the
eight nearest enhanced glowing actors receive halos; native outlines remain
for the rest. The new record channel requires at least57x116 framebuffer.
This path has no history or stale-fade cache; cleanup destroys only owned
holders. ManualVisualTracking repairs holders after native client-world reset.
Native TeamColor is still an enum of16 values, so arbitrary low-bit RGB cannot
be encoded through scoreboard packets in26.3. See TeamColor.STREAM_CODEC.

Checks: SignedEffectVisualChecks.verify(player) exercises all six native effect
packet codecs at IntMin/-3/-2/-1/0/1/2/255/256/IntMax amplifiers, actual native
invisible/glowing flags and command-glow preservation, post-chain relog/clear
cleanup and negative-blindness per-viewer arithmetic. It restores player
effects/post chains. GPU frames, subjective shader response, driver compile and
physical reconnect frames are not verified by these native checks.

GPU validation: tools/check_signed_effect_shaders.py passed83 checks on a real
NVIDIA RTX4080SUPER OpenGL3.3 context. Receipt: build/verification/signed-effect-visual-gpu/results.json. Combined old+new outline programs link; blackIII pixels, negative brightness/inversion, finiteextremes, independent halo growth, screen-sizedIntMax, alpha preservation and marker/pass ordering pass. This is standalone GPU validation, not an actual Minecraft client-frame capture.

Native blindness admission audit: only Player.isMobilityRestricted reads blindness on the server; its caller gates native critical attacks. The dedicated $Player mixin preserves this restriction only for positive blindness. Native client sprint/fog is covered by recipient-only nonpositive effect duration projection. Nonpositive conduit, jumpboost, slowfalling and dolphinsgrace also use zero client duration while retaining signed server effects.
