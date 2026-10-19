# Zone barrier operator checks

`/sw check zone-barrier` runs after phase-two selection 6 has started the real
event. It checks the live zone center and fixed Y=-60, the 384-to-4 linear
radius over 4000 match ticks (including the radius 1000 ticks after the
current match time, clamped to 4, and the endpoint), and the Polymer item/display fields. The fixture reads the
item model, NONE item context, FIXED billboard, zero display dimensions, view
range 64, glow color 16588098, one-tick interpolation, transform scale,
translation, and identity rotations from the virtual display itself. It also
checks the manual attachment viewer count against every connected arena
player.

The same case checks horizontal circle geometry at cardinal and diagonal
boundaries with floating-point tolerance, fractional excess distance,
arbitrary height, and large coordinates. It creates a dedicated TestZoneObserver
as a late join and confirms that observer remains excluded while TestAlpha and
TestBeta stay alive. It then starts a constant radius-10 test barrier and advances the real
`GameController.tick()` path for two pulse intervals. TestBeta at distance 12.5
must lose exactly 2.5 HP each interval (5 HP total) while creative mode,
`developer`, netherite armor, Resistance IV, absorption, invulnerability and a
shield are set; the fixture temporarily raises and restores native maximum
absorption to allow the 100-point absorption value. TestAlpha at the center takes no damage. A 19-tick prefix must
not damage Beta; an observer outside the circle is excluded. The fixture
restores player inventory, effects, mode, position, velocity, health,
absorption and damage timers in `finally`, and restores `noBorders`.

During a third pulse Beta remains outside and loses another 2.5 HP (7.5
total), while the dedicated late-join observer keeps its original health.

`/sw check entity-limits` also tests all 40 registered attributes with positive
and negative base values and modifiers ±2,000,000; a native cow; and native
NBT Motion save/load for cow, arrow and item. A temporary recording connection
acknowledges actual native teleport packets, including the extra packet sent
on mounting. Native player and boat movement handlers accept 40-block steps;
a subsequent step into a temporary stone block is still rejected by collision.
The fixture restores the original connection and player state and removes
the temporary boat/block. It does not change production packet encodings.

`/sw check zone-barrier-lethal` exercises the native death and respawn paths with TestAlpha
at 40 HP in the center and TestBeta at 2 HP, 13 blocks away from a radius-10
center. After 20 ticks, TestBeta must be dead, awaiting ordinary respawn, and
have exactly one additional profile death. The case sends the native respawn
packet and checks the replacement player and remaining life through a deferred
server-tick check, then restores the temporary profile death count. This pulse
removes exactly 3 HP.

After the lethal deferred respawn check, the runner calls `stopMatch()` to
restore the lobby. `/sw check zone-barrier-cleanup` then checks the lifecycle's
inactive state, zero radius, null center/display, zero viewers, and native
world-border size. The cases are ordered `zone-barrier`,
`zone-barrier-lethal`, `zone-barrier-cleanup`.
