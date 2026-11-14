package me.skywars2.game;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.resources.Identifier;

/** Real dispatcher, native ownership projections, offline file reload and failed durable commits. */
public final class AdminCommandChecks {
    private AdminCommandChecks() {}
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("Admin commands: "+detail);}
    private static int command(GameController game,String suffix){
        try{return game.server().getCommands().getDispatcher().execute("skywars admin "+suffix,game.server().createCommandSourceStack());}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException("Native admin command "+suffix,error);}
    }
    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var ordinary=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(owner!=null&&ordinary!=null,"two actual native players");
        var saved=new RegenerationBeltChecks.Snapshot(game,owner);var profiles=game.players().checkpointProfiles();
        String offlineName="qaAdminOffline";UUID offline=GameChecks.offlineUuid(offlineName);
        require(game.server().getPlayerList().getPlayer(offline)==null,"offline target has no fabricated player body");
        try{
            var node=game.server().getCommands().getDispatcher().getRoot().getChild("skywars").getChild("admin");
            require(node!=null&&node.canUse(game.server().createCommandSourceStack())&&!node.canUse(ordinary.createCommandSourceStack().withPermission(net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS)),"operator-only native command tree");
            game.players().get(offline,offlineName);var row=game.players().get(offline);
            for(String resource:List.of("coins","memcoins")){
                require(command(game,resource+" "+offlineName+" set 100")==1&&command(game,resource+" "+offlineName+" add 200")==1&&command(game,resource+" "+offlineName+" remove 50")==1,"native offline currency operations");
                require((resource.equals("coins")?row.coins:row.memcoins)==250,"exact offline balance");
                require(command(game,resource+" "+offlineName+" set 2147483647")==1&&command(game,resource+" "+offlineName+" add 1")==0,"overflow rejected");
                require((resource.equals("coins")?row.coins:row.memcoins)==Integer.MAX_VALUE,"overflow cannot wrap or silently clamp money");
                require(command(game,resource+" "+offlineName+" remove 2147483647")==1&&command(game,resource+" "+offlineName+" remove 5")==1,"removal stops at zero");
                require((resource.equals("coins")?row.coins:row.memcoins)==0,"no negative balance");
                require(command(game,resource+" "+offlineName+" add 123")==1,"known persisted final balance");
            }
            for(int kind=0;kind<5;kind++){
                require(command(game,"keys "+offlineName+" "+kind+" set 3")==1&&command(game,"keys "+offlineName+" "+kind+" add 2")==1&&command(game,"keys "+offlineName+" "+kind+" remove 1")==1,"all five actual key slots");
                require(row.keys[kind]==4,"exact selected case key count");
            }
            int known=game.players().knownProfiles().size();
            require(command(game,"coins qaAdminUnknown add 1")==0&&game.players().knownProfiles().size()==known,"unknown names never create accidental profiles");
            require(LbcSourceUnlocks.administrativeIds().size()==94,"all manual kit/skill/sound/cage/suffix/tournament flags, excluding drop requests");
            var online=game.players().get(owner.getUUID());online.kit=20;online.skill=11;online.killSound=13;online.cage=20;online.suffix=26;online.winEffect=2;online.killEffect=3;
            for(String id:List.of("swrg.kit20","swrg.skill11","swrg.sound13","swrg.bars20","swrg:rewards/suffix_26","swrg:rewards/turnir/trophy1","swrg:rewards/turnir/killer")){
                require(command(game,"unlock "+owner.getName().getString()+" give "+id)==1&&LbcSourceUnlocks.has(owner,online,id),"native ownership granted: "+id);
                require(command(game,"unlock "+owner.getName().getString()+" take "+id)==1&&!LbcSourceUnlocks.has(owner,online,id)&&online.revokedLegacyUnlocks.contains(id),"native ownership revoked durably: "+id);
                if(!id.contains(":"))require(!owner.entityTags().contains(id),"native tag projection removed");
                else require(!owner.getAdvancements().getOrStartProgress(game.server().getAdvancements().get(Identifier.parse(id))).isDone(),"native impossible-advancement projection removed");
            }
            require(online.kit==0&&online.skill==0&&online.killSound==0&&online.cage==0&&online.suffix==0&&online.winEffect==0&&online.killEffect==0,"revoking access clears only corresponding selected perks/cosmetics");
            owner.entityTags().add("swrg.kit20");LbcSourceUnlocks.importLegacy(owner,online);
            require(!online.legacyUnlocks.contains("swrg.kit20")&&!LbcSourceUnlocks.has(owner,online,"swrg.kit20"),"stale native tag cannot reimport revoked ownership");
            LbcSourceUnlocks.publish(owner,online);require(!owner.entityTags().contains("swrg.kit20"),"rejoin projection repair removes stale tag");
            require(command(game,"unlock "+offlineName+" give swrg.sound13")==1&&command(game,"unlock "+offlineName+" take swrg.sound13")==1,"offline grant/revoke needs no entity");
            var disk=PlayerStore.load().knownProfiles().get(offline);
            require(disk!=null&&disk.coins==123&&disk.memcoins==123&&java.util.Arrays.stream(disk.keys).allMatch(n->n==4)&&disk.revokedLegacyUnlocks.contains("swrg.sound13")&&!disk.legacyUnlocks.contains("swrg.sound13"),"real file reload retains currencies, five key slots and offline revocation");
            require(command(game,"unlock "+offlineName+" give lbc:true_advancements/armor/randchanted")==0,"ordinary gameplay achievements are outside the manual unlock command");
            var payout=LbcCases.class.getDeclaredMethod("apply",PlayerStore.Profile.class,LbcCases.Payout.class);payout.setAccessible(true);
            payout.invoke(null,online,new LbcCases.Payout("swrg.kit20",0,0,"",false));
            require(online.legacyUnlocks.contains("swrg.kit20")&&!online.revokedLegacyUnlocks.contains("swrg.kit20"),"new canonical case award reopens a previously revoked unlock");
            var temporary=Path.of("config","luckywars","players.json.tmp");var sentinel=temporary.resolve("admin-io-sentinel");
            require(!Files.exists(temporary),"exclusive absent durable-save temporary path");
            try{Files.createDirectory(temporary);Files.writeString(sentinel,"owned admin command failure fixture");require(command(game,"coins "+offlineName+" add 10")==0&&row.coins==123&&PlayerStore.load().knownProfiles().get(offline).coins==123,"failed durable save restores memory and leaves disk unchanged");}
            finally{Files.deleteIfExists(sentinel);Files.deleteIfExists(temporary);}
        }catch(Exception error){throw new IllegalStateException("Native administration fixture: "+error.getMessage(),error);}
        finally{saved.restore(game);saved.restoreAttributes();saved.restoreUse();game.players().restoreProfiles(profiles);try{game.players().saveAtomic();}catch(java.io.IOException error){throw new IllegalStateException("Restore admin profiles",error);}}
        SkyWars2.LOGGER.info("[check] PASS admin-commands (actual permission/dispatcher, offline currency and five key slots, overflow/unknown controls, source native grants/revokes/selections, stale projection denial, disk reload, re-earned case ownership and failed-save rollback)");
    }
}
