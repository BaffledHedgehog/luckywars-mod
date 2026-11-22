package me.skywars2.media.client;

import io.netty.buffer.Unpooled;
import me.skywars2.media.MediaPackets;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;

/** Local preference changes the native lerp input before sprint prediction, without an RTT. */
public final class PersonalFovChecks {
    private static void close(float actual,float expected) {
        if(Math.abs(actual-expected)>1e-6f)throw new AssertionError(actual+" != "+expected);
    }
    public static void main(String[] args) {
        ClientFovPreferences.reset();close(ClientFovPreferences.scale(.42f),.42f);
        for(int percent:new int[]{0,5,100}) {
            var preference=new MediaPackets.FovPreference(percent,false);
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
            try {
                MediaPackets.FovPreference.CODEC.encode(buffer,preference);
                if(!preference.equals(MediaPackets.FovPreference.CODEC.decode(buffer))||buffer.isReadable())
                    throw new AssertionError("exact FOV protocol roundtrip");
            } finally {buffer.release();}
            ClientFovPreferences.accept(preference);
            for(float nativeScale:new float[]{0,.05f,1}) {
                // Same final interpolation used by AbstractClientPlayer.getFieldOfViewModifier.
                // Its movement input already contains local sprint on this first frame.
                for(float nativeModifier:new float[]{1,1.15f,.625f,1.1f,.85f,4})
                    close(Mth.lerp(ClientFovPreferences.scale(nativeScale),1,nativeModifier),
                            1+(nativeModifier-1)*percent/100f);
            }
            ClientFovPreferences.accept(new MediaPackets.FovPreference(percent,true));
            close(Mth.lerp(ClientFovPreferences.scale(.05f),1,.25f),.25f);
        }
        for(int bad:new int[]{-1,101}) {
            boolean rejected=false;
            try {new MediaPackets.FovPreference(bad,false);}catch(IllegalArgumentException expected){rejected=true;}
            if(!rejected)throw new AssertionError("corrupt preference bounds");
        }
        ClientFovPreferences.reset();close(ClientFovPreferences.scale(.42f),.42f);
        System.out.println("PASS local FOV 0/5/100: first predicted sprint, native speed/flight/bow lerp, full scope, disconnect reset and protocol bounds");
    }
}
