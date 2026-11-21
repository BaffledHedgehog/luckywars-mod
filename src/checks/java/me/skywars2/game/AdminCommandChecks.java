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
    private static void dropRequests(GameController game, net.minecraft.server.level.ServerPlayer online,
                                     String offlineName, UUID uuid) throws Exception {
        var server = game.server();
        var dispatcher = server.getCommands().getDispatcher();
        var output = new java.util.ArrayList<String>();
        var source = server.createCommandSourceStack().withSource(new net.minecraft.commands.CommandSource() {
            public void sendSystemMessage(net.minecraft.network.chat.Component message) { output.add(message.getString()); }
            public boolean acceptsSuccess() { return true; }
            public boolean acceptsFailure() { return true; }
            public boolean shouldInformAdmins() { return false; }
        });
        String earned = LbcDropRequests.earnedKey(3), fulfilled = LbcDropRequests.fulfilledKey(3);
        var earnedAdv = server.getAdvancements().get(Identifier.parse(earned));
        var fulfilledAdv = server.getAdvancements().get(Identifier.parse(fulfilled));
        require(earnedAdv != null && fulfilledAdv != null, "exact source drop request ids");
        var files = SavedAdvancements.of(server);
        var originalFile = files.legacy().resolve(uuid + ".json");
        var editedFile = files.current().resolve(uuid + ".json");
        require(!Files.exists(originalFile) && !Files.exists(editedFile), "offline fixture owns both native files exclusively");
        boolean createdLegacyDirectory = !Files.exists(files.legacy());
        Files.createDirectories(files.legacy());
        var nativeBefore = java.util.Map.of(earnedAdv, online.getAdvancements().getOrStartProgress(earnedAdv).isDone(),
                fulfilledAdv, online.getAdvancements().getOrStartProgress(fulfilledAdv).isDone());
        try {
            // One real legacy completed criterion, one bogus 'done' flag, and an unrelated saved entry.
            byte[] original = ("""
                    {
                      "swrg:rewards/drop_3": {"criteria": {"s": "2026-11-20 12:00:00 +0300"}, "done": true},
                      "swrg:rewards/drop_2": {"criteria": {"other": "2026-11-20 12:00:00 +0300"}, "done": true},
                      "fixture:unrelated": {"criteria": {"x": "2026-11-20 12:00:00 +0300"}, "done": true},
                      "DataVersion": 4325
                    }
                    """).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            Files.write(originalFile, original);
            String query = "advancement query " + offlineName + " ";
            require(dispatcher.execute(query + earned, source) == 1
                            && dispatcher.execute(query + LbcDropRequests.earnedKey(2), source) == 0,
                    "query decodes legacy completed requirements, ignoring a bogus 'done' field");
            String[] prefixes = {"advancement query qaAd", "advancement grant qaAd", "advancement revoke qaAd", query + "swrg:rewards/drop_3_"};
            for (String prefix : prefixes) {
                String suggestion = prefix.equals(prefixes[3]) ? fulfilled : offlineName;
                require(dispatcher.getCompletionSuggestions(dispatcher.parse(prefix, source)).join().getList().stream()
                        .anyMatch(entry -> entry.getText().equals(suggestion)), "real Brigadier suggestion for " + prefix);
            }

            var noPermission = net.minecraft.server.permissions.PermissionSet.NO_PERMISSIONS;
            require(!dispatcher.getRoot().getChild("droprequests").canUse(source.withPermission(noPermission))
                            && !dispatcher.getRoot().getChild("advancement").canUse(source.withPermission(noPermission)),
                    "legacy listing and native advancement queries retain op admission");
            require(dispatcher.execute("droprequests", source) == 1, "offline listing scheduled through shown command");
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            server.managedBlock(() -> output.stream().anyMatch(line -> line.startsWith("Unfulfilled manual prizes:"))
                    || System.nanoTime() >= deadline);
            var profile = game.players().knownProfiles().get(uuid);
            require(output.stream().anyMatch(line -> line.startsWith(offlineName + " [") && line.contains(" drop_3 "))
                            && LbcDropRequests.pending(profile).equals(List.of(3)), "list includes only the exact offline earned request");
            require(dispatcher.execute("skywars droprequest made " + offlineName + " 2", source) == 0,
                    "attempt to fulfill an unearned request does not fabricate it");
            require(dispatcher.execute("skywars droprequest made " + offlineName + " 3", source) == 1
                            && dispatcher.execute(query + earned, source) == 1 && dispatcher.execute(query + fulfilled, source) == 1
                            && LbcDropRequests.pending(profile).isEmpty()
                            && files.read(uuid).progress(fulfilled, fulfilledAdv.value().requirements()).isDone(), "offline marking preserves earned and records the matching completion");

            String revoke = "advancement revoke " + offlineName + " only ";
            require(dispatcher.execute(revoke + fulfilled + " s", source) == 1
                            && dispatcher.execute(query + fulfilled, source) == 0 && dispatcher.execute(query + earned, source) == 1,
                    "offline revoke alters only the chosen criterion and matching canonical completion");
            String grant = "advancement grant " + offlineName.toLowerCase(java.util.Locale.ROOT) + " only ";
            require(dispatcher.execute(grant + fulfilled, source) == 1 && dispatcher.execute(query + fulfilled, source) == 1
                            && files.read(uuid).progress(fulfilled, fulfilledAdv.value().requirements()).isDone(),
                    "offline case-insensitive grant is saved to the native current progress file");
            require(java.util.Arrays.equals(Files.readAllBytes(originalFile), original)
                            && files.read(uuid).data().has("fixture:unrelated"), "legacy file and unrelated native progress survive editing");

            String root = "lbc:true_advancements/root";
            String branch = "lbc:true_advancements/accesories";
            String leaf = "lbc:true_advancements/accesories/save_platform";
            String editPrefix = "advancement grant " + offlineName + " ";
            String revokePrefix = "advancement revoke " + offlineName + " ";
            require(dispatcher.execute(editPrefix + "until " + branch, source) > 0
                            && dispatcher.execute(query + root, source) == 1 && dispatcher.execute(query + branch, source) == 1
                            && dispatcher.execute(query + leaf, source) == 0, "offline until changes the parent branch only");
            require(dispatcher.execute(editPrefix + "from " + branch, source) > 0 && dispatcher.execute(query + leaf, source) == 1
                            && dispatcher.execute(revokePrefix + "through " + branch, source) > 0
                            && dispatcher.execute(query + root, source) == 0 && dispatcher.execute(query + leaf, source) == 0,
                    "offline from and through use the native advancement hierarchy");
            require(dispatcher.execute(editPrefix + "everything", source) > 0 && dispatcher.execute(query + leaf, source) == 1
                            && dispatcher.execute(revokePrefix + "everything", source) > 0 && dispatcher.execute(query + leaf, source) == 0
                            && files.read(uuid).data().has("fixture:unrelated"), "everything respects the registered advancement set");
            require(dispatcher.execute(editPrefix + "only " + earned, source) == 1
                            && dispatcher.execute(editPrefix + "only " + fulfilled, source) == 1, "restore the targeted request for rollback");
            var beforeFailure = Files.readAllBytes(editedFile);
            var temp = Path.of("config","luckywars","players.json.tmp");
            require(!Files.exists(temp), "atomic-save failure fixture owns the temporary path");
            Files.createDirectory(temp);
            var sentinel = temp.resolve("owned");
            try {
                Files.writeString(sentinel, "offline advancement save failure");
                require(dispatcher.execute(revoke + fulfilled, source) == 0
                                && java.util.Arrays.equals(beforeFailure, Files.readAllBytes(editedFile))
                                && dispatcher.execute(query + fulfilled, source) == 1,
                        "failed profile transaction restores the actual native saved file and the in-memory entitlement");
            } finally {
                Files.deleteIfExists(sentinel);
                Files.deleteIfExists(temp);
            }

            String player = online.getScoreboardName();
            require(dispatcher.execute("advancement query " + player + " " + earned, source) == (nativeBefore.get(earnedAdv) ? 1 : 0),
                    "query reads genuinely live advancement state");
            if (!nativeBefore.get(earnedAdv)) require(dispatcher.execute("advancement grant " + player + " only " + earned, source) == 1,
                    "ordinary live grant still uses the vanilla command");
            require(dispatcher.execute("skywars droprequest made " + player + " 3", source) == 1
                            && online.getAdvancements().getOrStartProgress(earnedAdv).isDone()
                            && online.getAdvancements().getOrStartProgress(fulfilledAdv).isDone(),
                    "actual online fulfillment awards the matching advancement and preserves the earned one");
        } finally {
            for (var entry : nativeBefore.entrySet())
                for (String criterion : entry.getKey().value().criteria().keySet()) {
                    if (entry.getValue()) online.getAdvancements().award(entry.getKey(), criterion);
                    else online.getAdvancements().revoke(entry.getKey(), criterion);
                }
            Files.deleteIfExists(editedFile);
            Files.deleteIfExists(originalFile);
            if (createdLegacyDirectory) Files.deleteIfExists(files.legacy());
        }
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
            dropRequests(game,owner,offlineName,offline);
            var temporary=Path.of("config","luckywars","players.json.tmp");var sentinel=temporary.resolve("admin-io-sentinel");
            require(!Files.exists(temporary),"exclusive absent durable-save temporary path");
            try{Files.createDirectory(temporary);Files.writeString(sentinel,"owned admin command failure fixture");require(command(game,"coins "+offlineName+" add 10")==0&&row.coins==123&&PlayerStore.load().knownProfiles().get(offline).coins==123,"failed durable save restores memory and leaves disk unchanged");}
            finally{Files.deleteIfExists(sentinel);Files.deleteIfExists(temporary);}
        }catch(Exception error){throw new IllegalStateException("Native administration fixture: "+error.getMessage(),error);}
        finally{saved.restore(game);saved.restoreAttributes();saved.restoreUse();game.players().restoreProfiles(profiles);try{game.players().saveAtomic();}catch(java.io.IOException error){throw new IllegalStateException("Restore admin profiles",error);}}
        SkyWars2.LOGGER.info("[check] PASS admin-commands (actual permission/dispatcher, offline currency and five key slots, overflow/unknown controls, source native grants/revokes/selections, stale projection denial, disk reload, re-earned case ownership, saved drop requests and offline advancement commands/completions, failed-save rollback)");
    }
}
