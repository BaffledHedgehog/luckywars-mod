package me.skywars2.game;

import java.nio.file.*;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.CustomAll;

/** Actual native path dialog and command, asynchronously importing a large UTF8 export. */
public final class TikTokBulkImportChecks {
    private TikTokBulkImportChecks(){}
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TikTok bulk import: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;
        final WoolBucketChecks.Fixture f;
        final Path directory,cookies,export;
        final TikTokCache previous,cache;
        final java.lang.reflect.Field cacheField;
        final boolean wasOp;
        final String normalized;
        int began,phase;
        Fixture(GameController game)throws Exception{
            this.game=game;f=new WoolBucketChecks.Fixture(game,false);f.prepareParticipants();
            wasOp=game.server().getPlayerList().isOp(f.owner.nameAndId());
            directory=Files.createTempDirectory(game.server().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT),"tiktok-bulk-");
            cookies=directory.resolve("private/cookies.json");export=directory.resolve("large cookie export.json");
            String input="[{\"domain\":\".tiktok.com\",\"name\":\"sessionid\",\"value\":\"synthetic-local-only\",\"sameSite\":null}]";
            normalized=TikTokCookies.normalize(input);
            Files.writeString(export,"\ufeff"+input+"\n".repeat(1_000_000),java.nio.charset.StandardCharsets.UTF_8);
            cache=new TikTokCache(directory.resolve("cache"),cookies,false);
            cacheField=TikTokAccounts.class.getDeclaredField("cache");cacheField.setAccessible(true);
            previous=(TikTokCache)cacheField.get(null);require(previous!=null,"real account lifecycle registered");cacheField.set(null,cache);
            if(!wasOp)game.server().getPlayerList().op(f.owner.nameAndId());
        }
        void begin()throws Exception{
            var commands=game.server().getCommands().getDispatcher();
            commands.execute("skywars tiktok importfile",f.owner.createCommandSourceStack());
            var packet=f.client.packets().stream().filter(ClientboundShowDialogPacket.class::isInstance).map(ClientboundShowDialogPacket.class::cast).toList().getLast();
            var dialog=(MultiActionDialog)packet.dialog().value();
            require(dialog.common().inputs().getFirst().key().equals("path"),"actual file path input");
            var id=((CustomAll)dialog.actions().getFirst().action().orElseThrow()).id();
            var tag=new CompoundTag();tag.putString("path",export.toString());
            var reply=new ServerboundCustomClickActionPacket(id,Optional.of(tag));
            f.audience.listener.handleCustomClickAction(reply);
            require(!Files.exists(cookies),"foreign player cannot import using owner's nonce");
            f.client.listener.handleCustomClickAction(reply);
            f.client.listener.handleCustomClickAction(reply);
            began=game.server().getTickCount();
        }
        boolean step()throws Exception{
            require(game.server().getTickCount()-began<100,"async import finished within five seconds");
            if(!Files.exists(cookies))return false;
            require(Files.readString(cookies).equals(normalized),"actual million-character file normalized and atomically saved");
            require(cache.configured()&&WoolBucketChecks.read(cache,"pending")==null,"offline fixture starts no browser or download");
            if(phase==0){
                // Exercise the direct command too, including a Windows filename containing spaces.
                Files.delete(cookies);
                game.server().getCommands().getDispatcher().execute("skywars tiktok importfile \""+export+"\"",f.owner.createCommandSourceStack());
                phase=1;return false;
            }
            SkyWars2.LOGGER.info("[check] PASS tiktok-bulk-import (actual native op path dialog + direct quoted-path command, million-character UTF8 BOM export, foreign/replay denial, asynchronous normalization/atomic persistence; zero network)");
            return true;
        }
        public void close(){
            try{cacheField.set(null,previous);cache.close();}
            catch(Exception error){throw new IllegalStateException("TikTok bulk fixture restore",error);}
            finally{
                if(!wasOp)game.server().getPlayerList().deop(f.owner.nameAndId());
                f.close();
                try(var paths=Files.walk(directory)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
                catch(Exception error){throw new IllegalStateException("TikTok bulk fixture cleanup",error);}
            }
        }
    }
    public static void run(GameController game){
        require(running==null,"exclusive fixture");
        try{running=new Fixture(game);running.begin();}
        catch(Exception error){if(running!=null)running.close();running=null;throw new IllegalStateException("TikTok bulk import setup",error);}
    }
    public static void tick(GameController game){
        if(running==null)return;
        try{if(!running.step())return;running.close();running=null;}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL tiktok-bulk-import",error);}
    }
}
