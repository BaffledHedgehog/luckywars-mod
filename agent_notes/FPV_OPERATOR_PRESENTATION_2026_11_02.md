# FPV operator and third person presentation ? 2 November 2026

Scope: `LbcFpvDrone`, `FpvDroneMixin$Presentation`, `FpvDroneChecks`.

The drone ItemDisplay explicitly rejected its own controller in `broadcastToPlayer`.
That filter is removed. The locally controlled player must remain the camera so vanilla
continues sending unconstrained mouse input; the physical drone still turns at 3 degrees
per tick. A camera switch to the drone would regress free look.

The native server player never tracks itself, so its own connection does not receive a
second representation of its stationary body. Attachment now publishes an owner-only,
packet-only native Mannequin at the real player's position. It uses the resolved actual
GameProfile, model layers, arm, pose, facing and visible native equipment. Eight fixed
visible slots use native ItemStack equality and copied changed stacks; no inventory/NBT
scan, inventory shell, world actor, damage target, loot source or save entry is created.
The full inventory and vulnerable player remain the single real server player. Public
viewers continue receiving that native player; they never receive the Mannequin.

The owner's self entity receives invisible/non-glowing flags and empty equipment and
inventory presentation. A private client team prevents same-team friendly-invisibility
rendering of the local body in F5. Native team additions rejoin that private team for this
recipient only. Later self metadata, equipment and inventory packets pass through the
same narrow recipient transformation, including bundle contents. No authoritative player
flags, equipment, inventory, abilities, mode or scoreboard team are mutated. Detachment
removes the Mannequin/team, restores current self flags/equipment/full menu and real team;
existing Shift/death/respawn/forget/stop/checkpoint boundaries all use this detachment.

Sound source comparison: legacy `data/lbc/function/items/misc/drone/tick_fly.mcfunction`
uses `lbcsounds.drone_tick hostile`, volume .15, pitch1, radius35, minimum .04 every flight
tick. Current Java already matches exactly with the default namespace `minecraft`.
The actual read-only RP `all_in_one_1.21.8/assets/minecraft/sounds.json` maps that event
to `lbcsounds/drone_tick`; its Ogg exists (10203 bytes), stereo44.1kHz, duration0.106757s.
The existing repeated sample and native remote-camera audience are retained. No new or
renamed sound asset was fabricated. Native packet receipt can qualify dispatch, but an
actual client/mixer is required to qualify audibility if the symptom persists.

Native source inspection used pinned26.3 sources for Mannequin profile/metadata,
Entity.isInvisibleTo, player/Avatar renderer, packet constructors and listener send.
`FpvDroneChecks` now checks owner-only native Mannequin/profile/equipment, public drone
visibility, self invisibility, unchanged authoritative inventory and other-viewer packets.
Build/runtime qualification is performed by the integrating agent after all peers freeze;
this scoped agent does not start builds or native fixtures concurrently with peer edits.

Follow-up audio listener repair: SoundNormalization independently resolved its listener
through ServerPlayer.getCamera()/last camera-packet entity. FPV deliberately keeps both
on the stationary native body. Its mono gain/range projection and immutable network-thread
VIEWS snapshots consequently used the wrong origin. It now takes the attached FPV drone's
eye first; LbcFpvDrone.viewpoint publishes that same precise eye to shared sound audience
and min-volume calculations. Native checks assert projected mono coordinates, captured
listener snapshots and actual repeated buzz gain/pitch/category/source near the drone.
This fixes a demonstrated listener-propagation defect. The stereo buzz has maxVolume1
and bypasses normalization, so this defect alone does not prove the cause of its reported
inaudibility; real-client qualification remains necessary rather than claiming an unheard
packet was audible.
