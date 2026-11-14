package me.skywars2.game;

import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.input.TextInput;

/** Native dialog/packet limits, durable credential normalization and bounded seekable media/cache. */
public final class TikTokAccountChecks {
    private TikTokAccountChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TikTok account: "+why);}
    public static void run(GameController game){
        Path temporary=null;
        try{
            temporary=Files.createTempDirectory(game.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT),"tiktok-check-");
            String input="[{\"domain\":\".tiktok.com\",\"name\":\"sessionid\",\"value\":\"synthetic-fixture-value\",\"path\":\"/\",\"sameSite\":null,\"secure\":null,\"httpOnly\":null,\"expirationDate\":2000000000},"
                    +"{\"domain\":\"tiktok.com.evil.test\",\"name\":\"bad\",\"value\":\"never-import\"}]";
            String normalized=TikTokCookies.normalize(input);
            require(!normalized.contains("sameSite"),"nullable Cookie-Editor fields are omitted rather than rejected");
            require(normalized.contains("sessionid")&&!normalized.contains("evil.test")&&!normalized.contains("never-import"),"only real TikTok cookie domains retained");
            require(TikTokCookies.normalize("{\"cookies\":"+input+"}").equals(normalized),"array and browser-export wrapper accepted identically");
            String large=input+" ".repeat(TikTokCookies.MAX_INPUT-input.length());
            require(TikTokCookies.normalize(large).equals(normalized),"full four MiB export accepted without minifying");
            Path export=temporary.resolve("large cookie export.json");
            Files.writeString(export,"\ufeff"+large.substring(0,large.length()-3),java.nio.charset.StandardCharsets.UTF_8);
            require(Files.size(export)==TikTokCookies.MAX_INPUT&&TikTokCookies.fromFile(export).equals(normalized),"four MiB UTF8 BOM file accepted at exact byte boundary");
            Files.writeString(export," ",java.nio.charset.StandardCharsets.UTF_8,java.nio.file.StandardOpenOption.APPEND);
            boolean oversizedFile=false;try{TikTokCookies.fromFile(export);}catch(IOException expected){oversizedFile=true;}
            require(oversizedFile,"oversize export rejected by bounded file read");
            large=null;
            Path cookies=temporary.resolve("private/cookies.json");TikTokCookies.save(cookies,normalized);
            require(Files.readString(cookies).equals(normalized),"atomic disk persistence across reload");
            for(String invalid:List.of("", "{}", "[]", "null", "[{}]", "[", input+" ".repeat(TikTokCookies.MAX_INPUT),
                    "[{\"domain\":\"tiktok.com\",\"name\":\"n\",\"value\":\"line\\nsecret\"}]")){
                boolean rejected=false;try{TikTokCookies.normalize(invalid);}catch(IllegalArgumentException expected){rejected=true;require(!expected.toString().contains("secret"),"credential-free failure");}
                require(rejected,"malformed/oversize input denied");require(Files.readString(cookies).equals(normalized),"invalid input never mutates durable credentials");
            }
            var packet=new ClientboundShowDialogPacket(TikTokAccounts.dialog("native-fixture"));
            var bytes=Unpooled.buffer();
            try{
                ClientboundShowDialogPacket.CONTEXT_FREE_STREAM_CODEC.encode(bytes,packet);
                var decoded=(MultiActionDialog)ClientboundShowDialogPacket.CONTEXT_FREE_STREAM_CODEC.decode(bytes).dialog().value();
                require(decoded.common().inputs().size()==1&&decoded.common().inputs().getFirst().key().equals("cookies"),"native client cookie input");
                var control=(TextInput)decoded.common().inputs().getFirst().control();
                require(control.maxLength()==20000&&control.initial().isEmpty(),"20k character input, no existing credential ever returned to client dialog");
            }finally{bytes.release();}
            bytes=Unpooled.buffer();
            try{
                var payload=new CompoundTag();payload.putString("cookies","\u6c34".repeat(20000));
                var reply=new ServerboundCustomClickActionPacket(Identifier.parse("skywars2:tiktok/import/native-fixture"),Optional.of(payload));
                ServerboundCustomClickActionPacket.STREAM_CODEC.encode(bytes,reply);
                require(bytes.readableBytes()<65536,"UTF8 native reply remains below wire cap");
                var decoded=ServerboundCustomClickActionPacket.STREAM_CODEC.decode(bytes);
                require(decoded.payload().orElseThrow().equals(payload),"scoped64KiB NBT accounting accepts maximum three-byte UTF8 input using original native encoder");
                bytes.clear();
                ServerboundCustomClickActionPacket.STREAM_CODEC.encode(bytes,new ServerboundCustomClickActionPacket(Identifier.parse("skywars2:build/native-fixture"),Optional.of(payload)));
                boolean ordinaryRejected=false;try{ServerboundCustomClickActionPacket.STREAM_CODEC.decode(bytes);}catch(RuntimeException expected){ordinaryRejected=true;}
                require(ordinaryRejected,"ordinary dialogs retain original32KiB NBT accounting");
            }finally{bytes.release();}
            Path root=temporary.resolve("cache"),videos=root.resolve("videos");Files.createDirectories(videos);
            for(int i=0;i<6;i++)clip(videos.resolve("1234567"+i));
            Path absentCookies=temporary.resolve("not-configured.json");
            try(var cache=new TikTokCache(root,absentCookies)){
                require(cache.sizeReady()==5&&!cache.configured(),"exactly five retained clips, no anonymous feed fallback");
                require(cache.stateKey().equals("tiktok.waiting_for_import"),"unconfigured account status does not pretend to prepare queue");
                require(!Files.exists(videos.resolve("12345675")),"stale sixth original and derivatives removed together");
                var first=cache.next("");var next=cache.next(first.id);require(!first.id.equals(next.id),"next clip sequence");
                var frame=first.frame(1,true);require(frame.map().length==7168&&frame.pcm().length==2400&&frame.bands().length==24&&frame.jpeg().length==4,"bounded per-frame disk read");
                require(first.frame(0,false).jpeg().length==0,"vanilla read does not allocate/decode high-quality image");
                cache.watched(first.id,Set.of(first.id));require(cache.sizeReady()==5,"active viewers pin original and derivatives");
                cache.watched(first.id,Set.of());require(cache.sizeReady()==4&&!Files.exists(first.directory),"irrelevant clip retired after last viewer releases it");
                cache.tick(1,Set.of());require(cache.sizeReady()==4,"unconfigured cache never launches browser/download process");
                cache.importCookies(input);
                require(cache.configured()&&cache.sizeReady()==0&&Files.readString(absentCookies).equals(normalized),"new account atomically persists and retires old account's complete media queue");
            }
            nativeDialog(game);
        }catch(IOException error){throw new IllegalStateException("TikTok account fixture I/O",error);}
        finally{if(temporary!=null)try(var files=Files.walk(temporary)){for(var p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}catch(IOException failure){throw new IllegalStateException("TikTok fixture cleanup",failure);}}
        SkyWars2.LOGGER.info("[check] PASS tiktok-account (actual operator command/import dialog and authenticated nonce/foreign/replay denial, native reply max Unicode accounting, credential-free invalid input/atomic persistence, five-clip eviction/pin/release/account replacement and bounded frame reads; offline, no account/network)");
    }
    private static void nativeDialog(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var players=game.server().getPlayerList();
            boolean ownerOp=players.isOp(f.owner.nameAndId()),otherOp=players.isOp(f.other.nameAndId());
            Path credentials=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("skywars2/tiktok/cookies.json");
            require(!Files.exists(credentials),"actual account stays unconfigured in offline fixture");
            try{
                var dispatcher=game.server().getCommands().getDispatcher();
                var node=dispatcher.getRoot().getChild("skywars").getChild("tiktok");
                require(node!=null&&node.canUse(game.server().createCommandSourceStack())
                        &&!node.canUse(f.other.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS)),"actual operator-only namespace/permission tree");
                if(!ownerOp)players.op(f.owner.nameAndId());if(!otherOp)players.op(f.other.nameAndId());
                dispatcher.execute("skywars tiktok import",f.owner.createCommandSourceStack());
                var packet=f.client.packets().stream().filter(ClientboundShowDialogPacket.class::isInstance).map(ClientboundShowDialogPacket.class::cast).toList().getLast();
                var dialog=(MultiActionDialog)packet.dialog().value();
                var id=((net.minecraft.server.dialog.action.CustomAll)dialog.actions().getFirst().action().orElseThrow()).id();
                var invalid=new CompoundTag();invalid.putString("cookies","invalid fixture JSON");
                var reply=new ServerboundCustomClickActionPacket(id,Optional.of(invalid));
                int ownerPackets=f.client.packets().size();
                f.audience.listener.handleCustomClickAction(reply);
                require(f.client.packets().size()==ownerPackets&&!Files.exists(credentials),"another authorized operator cannot consume owner's personal nonce");
                f.client.listener.handleCustomClickAction(reply);
                require(f.client.packets().size()>ownerPackets&&!Files.exists(credentials),"real owner reply reaches native handler and rejects malformed JSON without saving");
                var otherwiseValid=new CompoundTag();otherwiseValid.putString("cookies","[{\"domain\":\"tiktok.com\",\"name\":\"sessionid\",\"value\":\"synthetic-never-saved\"}]");
                f.client.listener.handleCustomClickAction(new ServerboundCustomClickActionPacket(id,Optional.of(otherwiseValid)));
                require(!Files.exists(credentials),"consumed native reply cannot replay a valid cookie import");
            }finally{
                if(!ownerOp)players.deop(f.owner.nameAndId());if(!otherOp)players.deop(f.other.nameAndId());
            }
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException("TikTok native command",error);}
    }
    private static void clip(Path folder)throws IOException{
        Files.createDirectories(folder);Files.write(folder.resolve("original.mp4"),new byte[]{1});
        int record=4+TikTokMedia.PIXELS+TikTokMedia.BANDS+TikTokMedia.AUDIO_BYTES+4;
        var data=ByteBuffer.allocate(28+record*2);data.putInt(0x544b5031).putInt(2).putInt(20).putInt(64).putInt(112).putInt(24000).putInt(24);
        for(int i=0;i<2;i++){data.putInt(4);data.put(new byte[TikTokMedia.PIXELS+TikTokMedia.BANDS+TikTokMedia.AUDIO_BYTES]);data.put(new byte[]{(byte)255,(byte)216,(byte)255,(byte)217});}
        Files.write(folder.resolve("clip.tkp"),data.array());
        Files.write(folder.resolve("clip.idx"),ByteBuffer.allocate(16).putLong(28).putLong(28+record).array());
    }
}
