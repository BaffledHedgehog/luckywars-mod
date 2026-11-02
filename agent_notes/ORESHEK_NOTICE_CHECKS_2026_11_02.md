# Oreshek notice checks - 2 November 2026

OreshekChecks now expects the public registered LbcOreshek.ITEM identity from the
canonical nut loot leaf and snapshots that item's ITEM_USED/ITEM_BROKEN statistics.
The actual finishUsingItem path, exact consumption debit, harmful-debuff selection,
real native15 isolated blast, repeated consumption, source Nexus transfer, producer47
and role/TimeStop guards remain covered.

Prior assertions expected a persistent post-survival marker and an E202 broadcast on
reset; those expectations contradicted the new death-only contract. A high-health
survivor and a second actual consumed explosion must leave no pending announcement.
The fixture seeds an obsolete persistent tag, calls the reset boundary twice and asserts
that both calls return false, the tag clears, and no E202 system-chat packet is sent.
The real Nexus transfer also leaves no pending death notice.

The source Choices random fixture is package-visible for a separate genuine native
player death/replacement fixture. No synthetic died() call was introduced as evidence
of a real death. Such a fixture remains for integration: it must capture core state,
observe actual nut explosion lethal death, assert exactly one E202 per audience,
respawn via PlayerList into a different ServerPlayer, and rebind player snapshots and
listeners before restoration. The old Oreshek fixture deliberately remains a survivor
fixture, because its original final-body snapshots cannot safely restore a replacement
without that explicit lifecycle work. No build/native tests were run by this agent.

Follow-up: OreshekDeathChecks implements run/tick/active hooks as a separate bounded
native fixture. It requires an ordinary three-life baseline, performs a real highHP
nut-explosion survival, then a later actual void death with zero symbol packets. After
registered native replacement, lowHP actual consumption chooses the real15blast and
must enter native death with one symbol packet for each independently recorded audience.
It respawns again and observes twenty server ticks with no repeated announcement.
The fixture never calls died() directly. It restores core participation/lives through the
supported checkpoint, rebinds only its own snapshot player references, restores native
statistics/attributes/profile/inventory and removes only newly-created nonplayer actors.
No reflection writes to production maps were added. Compilation/native qualification
remains with integration after the source freeze.

Retained first native run6715b079: survivor, native unrelated death and its zero-symbol
assertions passed; the following tick failed in the fixture's unconditional Shish Client
loaded-listener ACK before its respawn stage. The fixture was trying to acknowledge a
native-dead retired body. The step now ACKs only its live stages0/3, verifies their listener
is bound to the canonical player, and leaves dead stages1/2 to the supported PlayerList
respawn plus SimulatedAcknowledgement lifecycle. Captured dead-body wire packets remain
available for exact notice assertions. Production behavior was not changed.
