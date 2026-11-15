package me.skywars2.game;

import net.minecraft.util.RandomSource;

/** Per-evaluation random state: descendants share their parent's stream, never a second ticket. */
public interface SynchroLootContext {
    boolean skywars2$synchroPrepared();
    void skywars2$synchroPrepare(RandomSource random);
    RandomSource skywars2$synchroRandom();
    boolean skywars2$synchroActive();
    Integer skywars2$synchroSlab();
    void skywars2$synchroSlab(Integer value);
    void skywars2$synchroEnter();
    void skywars2$synchroExit();
}
