package me.skywars2.game;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;

/** Larger cookie imports retain the native 64 KiB wire/depth caps; every other dialog uses its original codec. */
public final class TikTokDialogPayloads {
    private static final StreamCodec<ByteBuf,Optional<Tag>> IMPORT_TAG=ByteBufCodecs
            .optionalTagCodec(()->new NbtAccounter(65536L,16)).apply(ByteBufCodecs.lengthPrefixed(65536));
    private TikTokDialogPayloads(){}
    public static StreamCodec<ByteBuf,ServerboundCustomClickActionPacket> wrap(StreamCodec<ByteBuf,ServerboundCustomClickActionPacket> original){
        return new StreamCodec<>(){
            @Override public ServerboundCustomClickActionPacket decode(ByteBuf buffer){
                int position=buffer.readerIndex();var id=Identifier.STREAM_CODEC.decode(buffer);
                if(!id.getNamespace().equals("skywars2")||!id.getPath().startsWith("tiktok/import/")){
                    buffer.readerIndex(position);return original.decode(buffer);
                }
                return new ServerboundCustomClickActionPacket(id,IMPORT_TAG.decode(buffer));
            }
            @Override public void encode(ByteBuf buffer,ServerboundCustomClickActionPacket packet){original.encode(buffer,packet);}
        };
    }
}
