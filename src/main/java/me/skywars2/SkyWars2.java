package me.skywars2;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Empty entry point for the dedicated server. Add gameplay services here. */
public final class SkyWars2 implements DedicatedServerModInitializer {
    public static final String MOD_ID = "skywars2";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        LOGGER.info("SkyWars 2 initialized. Fabric + Polymer server skeleton is ready.");
    }
}
