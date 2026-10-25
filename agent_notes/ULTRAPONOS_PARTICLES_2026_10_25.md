# Ultraponos upward brown-wool shards ? 25 October 2026

User point9 preserves the same brown-wool block-shard appearance and wants the
stream to rise like the emitted items. Item loot, exact item motionY.8 and
independent horizontal uniform[-.2,.2), source280 active ticks and all ownership,
Time Stop, resident-chunk and mechanism guards are unchanged. Particles remain
one native packet of60 per active tick (source300 per5ticks), at the hopper rim.

Native26.3 sources in
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`:
- `net/minecraft/client/particle/ParticleResources.java` binds BLOCK to
  TerrainParticle.Provider and DUST_PILLAR to TerrainParticle.DustPillarProvider.
- `net/minecraft/client/particle/TerrainParticle.java`: both providers instantiate
  exactly the same TerrainParticle class, brown-wool material sprite, random
  quarter-sprite UV, tint0.6, half quad size, gravity1 and native collision.
- `net/minecraft/client/particle/SingleQuadParticle.java` forwards velocities to
  Particle's constructor. `Particle.java` adds random[-.4,.4] components and
  normalizes the vector to magnitude about.06..18 before addingY.1. Consequently
  BLOCK cannot receive item-likeY.8 through a packet, even with oldyMaxSpeed5.
- DustPillarProvider then explicitly overrides the normalized velocity with
  `(Gaussian/30, suppliedY+Gaussian/2, Gaussian/30)`, lifetime20..39ticks.
- `net/minecraft/client/multiplayer/ClientPacketListener.java#handleParticleEvent`:
  ALTERNATIVE passes each packet axis speed directly for count>0, whereas
  DEFAULT multiplies it by a signed Gaussian. No spatial jitter is requested.
- `Particle.tick`: subtract.04 for gravity1, move, multiply motion by.98; these
  are the same free-flight vertical acceleration/drag as native emitted items.

Implementation uses DUST_PILLAR with the same brown-wool state and suppliedY.8.
The native particle protocol identifier changes; the actual shard appearance
stays the same. Native initialY is Gaussian(mean.8,sigma.5), horizontal axes are
independent Gaussian(mean0,sigma1/30), not exact uniform[-.2,.2]. About94.5% of
shards initially rise before gravity and about93.6% after the first.04 gravity
step; rare downward native shards remain. Exact item velocity on every shard is
unavailable with vanilla block-particle providers, and is not claimed. BLOCK_MARKER
and FALLING_DUST both discard packet velocity and change the visible particle.

UltraponosDropChecks captures the actual emitted packet, performs the native
stream-codec round trip, checks the visual material/type/rate/origin/ALTERNATIVE
and meanY payload, then evaluates the source-linked provider/first-tick equations
on a seeded4096-shard cohort. These are packet and source-semantics checks;
they do not execute a client renderer or qualify actual client pixels. Build,
native execution and READY qualification are recorded by the integrating agent.
