package me.skywars2.game.items.phone;

import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Native private stereo tones; old envelope fallback remains usable during local cache migration. */
final class TikTokAudio {
    private TikTokAudio(){}
    private static final List<Holder<SoundEvent>> TONES=tones();
    private static List<Holder<SoundEvent>> tones(){
        var result=new ArrayList<Holder<SoundEvent>>(111*16*2);
        for(int base=0;base<111;base++)for(int phase=0;phase<16;phase++)for(int side=0;side<2;side++)
            result.add(Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("skywars2:tiktok.v3.tone_"+base+"_"+phase+"_"+side))));
        return List.copyOf(result);
    }
    static List<Packet<? super ClientGamePacketListener>> spectralPackets(ServerPlayer player,byte[] record,long serial){
        return spectralPackets(player,record,serial,TikTokMedia.SPECTRAL_PARTIALS);
    }
    private record Tone(int key,int gain){
        int side(){return key&1;}
        int bin(){return key>>>5;}
        double priority(){return gain*Math.pow(Math.max(.15,bin()/100.),.45);}
    }
    static List<Packet<? super ClientGamePacketListener>> spectralPackets(ServerPlayer player,byte[] record,long serial,int limit){
        if(limit<1||limit>TikTokMedia.SPECTRAL_PARTIALS)throw new IllegalArgumentException("spectral_limit");
        if(record.length!=TikTokMedia.SPECTRAL_BYTES)throw new IllegalArgumentException("spectral_size");
        var bytes=ByteBuffer.wrap(record);int count=Short.toUnsignedInt(bytes.getShort());
        if(count>TikTokMedia.SPECTRAL_PARTIALS)throw new IllegalArgumentException("spectral_count");
        var tones=new ArrayList<Tone>(count);
        for(int i=0;i<count;i++){
            int key=Short.toUnsignedInt(bytes.getShort()),gain=Short.toUnsignedInt(bytes.getShort());
            int bin=key>>>5,phase=(key>>>1)&15,side=key&1;
            if(bin<2||bin>=1150)throw new IllegalArgumentException("spectral_frequency");
            if(gain<2)continue;
            tones.add(new Tone(key,gain));
        }
        if(tones.size()>limit){
            var selected=new ArrayList<Tone>(limit);
            int sides=(int)tones.stream().filter(t->t.side()==1).count();
            int sideBudget=Math.min(sides,Math.min(limit,Math.max(sides>0?4:0,Math.round((float)limit*sides/tones.size()))));
            for(int channel=0;channel<2;channel++){
                int side=channel,budget=channel==1?sideBudget:limit-sideBudget;
                var candidates=tones.stream().filter(t->t.side()==side).sorted(java.util.Comparator.comparingDouble(Tone::priority).reversed()).toList();
                var chosen=new java.util.LinkedHashSet<Tone>();
                int[] edges={2,25,50,100,200,400,800,1150};
                for(int band=0;band<edges.length-1&&chosen.size()<budget;band++){
                    int lo=edges[band],hi=edges[band+1];
                    candidates.stream().filter(t->t.bin()>=lo&&t.bin()<hi).findFirst().ifPresent(chosen::add);
                }
                for(var tone:candidates){if(chosen.size()>=budget)break;chosen.add(tone);}
                selected.addAll(chosen);
            }
            tones=selected;
        }
        var result=new ArrayList<Packet<? super ClientGamePacketListener>>(tones.size());
        for(var tone:tones){
            int key=tone.key,gain=tone.gain,bin=tone.bin(),phase=(key>>>1)&15,side=tone.side();
            double frequency=bin*10.;int base=(int)Math.round(12*Math.log(frequency/20)/Math.log(2));
            float pitch=(float)(frequency/(20*Math.pow(2,base/12.)));
            var sound=TONES.get((base*16+phase)*2+side);
            result.add(new ClientboundSoundEntityPacket(sound,SoundSource.RECORDS,player,gain/65535f,pitch,serial^key));
        }
        return result;
    }
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
