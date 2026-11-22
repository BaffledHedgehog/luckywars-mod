package me.skywars2.media.client;

import me.skywars2.media.MediaPackets;

/** Native client calculation sees sprint prediction in the same frame as movement. */
public final class ClientFovPreferences {
    private ClientFovPreferences(){}
    private static volatile MediaPackets.FovPreference preference;
    public static void accept(MediaPackets.FovPreference value){preference=value;}
    public static void reset(){preference=null;}
    public static float scale(float vanilla){var value=preference;return value==null?vanilla:value.scoped()?1:value.percent()/100f;}
}
