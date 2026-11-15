package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Sixteen native sounds per 16.67 ms maximum, each approximating three independent envelope bands. */
final class TikTokAudio {
    private TikTokAudio(){}
    record Mixture(int index,float volume){}
    static Mixture mixture(int a,int b,int c){
        int total=a+b+c;if(total<6)return null;
        int[] levels={a,b,c},weights=new int[3],remainders=new int[3];int rest=4;
        for(int i=0;i<3;i++){weights[i]=4*levels[i]/total;remainders[i]=4*levels[i]%total;rest-=weights[i];}
        while(rest-->0){int best=0;for(int i=1;i<3;i++)if(remainders[i]>remainders[best])best=i;weights[best]++;remainders[best]=-1;}
        int index=weights[0]*(11-weights[0])/2+weights[1];
        float volume=(float)Math.min(2,Math.sqrt((double)a*a+(double)b*b+(double)c*c)/255*4);
        return new Mixture(index,volume);
    }
    static List<Packet<? super ClientGamePacketListener>> packets(ServerPlayer player,byte[] bands,long serial){
        if(bands.length!=TikTokMedia.BANDS)throw new IllegalArgumentException("audio_bands");
        var result=new ArrayList<Packet<? super ClientGamePacketListener>>(TikTokMedia.BANDS/3);
        for(int group=0;group<TikTokMedia.BANDS/3;group++){
            var mix=mixture(Byte.toUnsignedInt(bands[group*3]),Byte.toUnsignedInt(bands[group*3+1]),Byte.toUnsignedInt(bands[group*3+2]));
            if(mix==null)continue;
            var sound=Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("skywars2:tiktok.v2.noise_"+group+"_"+mix.index)));
            long seed=player.getUUID().getLeastSignificantBits()^(serial*0x9e3779b97f4a7c15L)^group;
            result.add(new ClientboundSoundEntityPacket(sound,SoundSource.RECORDS,player,mix.volume,1,seed));
        }
        return result;
    }
}
