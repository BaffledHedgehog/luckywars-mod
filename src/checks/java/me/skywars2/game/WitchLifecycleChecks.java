package me.skywars2.game;

import com.mojang.authlib.GameProfile;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.nbt.*;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.scores.ScoreHolder;

/** Actual saved-player disconnect/login and native replacement-player boundaries, never a synthetic tag replay. */
public final class WitchLifecycleChecks {
    private WitchLifecycleChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Witch lifecycle: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ChefChecks.Fixture base;final LbcWitchClaims service;
        final Map<ServerPlayer,Set<String>> tags=new IdentityHashMap<>();
        final Map<ServerPlayer,Map<String,Integer>> scores=new IdentityHashMap<>();
        final UUID id=GameChecks.offlineUuid("TestGamma");
        final PlayerStore.Profile gammaProfile;
        final int savedGammaWins;
        ServerPlayer gamma;ItemStack root;int phase,start;boolean closed,owned;
        Fixture(GameController game){this.game=game;base=new ChefChecks.Fixture(game);service=game.items().witchClaims();
            require(game.server().getPlayerList().getPlayer(id)==null&&service.ownership().isEmpty()&&game.items().witchPassives().count()==0,"exclusive third session and no borrowed canonical authorities");
            gammaProfile=game.players().get(id);savedGammaWins=gammaProfile.wins;
            for(var p:List.of(base.owner,base.other)){tags.put(p,new HashSet<>(p.entityTags()));var saved=new HashMap<String,Integer>();for(var n:List.of("gens_type","dmg_dealt","greed_prestige"))saved.put(n,new LbcSourceCounters(game).read(p,n));scores.put(p,saved);}
        }
        void init(){base.init();base.move(base.owner,base.at);base.move(base.other,base.at.add(12,0,0));
            for(var p:List.of(base.owner,base.other))for(var mode:LbcWitchClaims.Mode.values())p.entityTags().remove(mode.tag());
            seedOfflineMatchState();
            gamma=connectSaved("have_mom","skywars2.reset_fixture.keep");owned=true;
            require(gamma!=null&&game.isObserver(gamma),"native late join stays a spectator");
            require(!gamma.entityTags().contains("have_mom")&&game.items().mana().score(gamma,"greed_prestige").get()==0,
                    "first login after an offline match boundary removes old Witch eligibility and scores");
            require(gamma.entityTags().contains("skywars2.reset_fixture.keep"),"late match reset preserves the saved unrelated tag");
            require(game.players().get(id).wins==37,"late match reset preserves the permanent profile");
            gamma.entityTags().add("have_mom");
            game.items().mana().score(gamma,"greed_prestige").set(29);
            disconnect();
            require(savedTags().contains("have_mom"),"native logout really saves the new spectator state");
            start=game.server().getTickCount();phase=-3;
        }
        private void seedOfflineMatchState(){
            var profile=new GameProfile(id,"TestGamma");
            var offline=new ServerPlayer(game.server(),base.owner.level(),profile,ClientInformation.createDefault());
            offline.entityTags().addAll(Set.of("have_mom","skywars2.reset_fixture.keep"));
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,offline.registryAccess());
            offline.saveWithoutId(output);
            var path=savedPlayerPath();
            require(java.nio.file.Files.isDirectory(path.getParent()),"native player data directory exists before seeding");
            try{
                NbtIo.writeCompressed(output.buildResult(),path);
            }catch(java.io.IOException error){throw new IllegalStateException(error);}
            require(savedTags().contains("have_mom"),"previous-match transient tag exists in offline player data");
            game.items().mana().score(ScoreHolder.forNameOnly(profile.name()),"greed_prestige").set(71);
            gammaProfile.wins=37;
        }
        void ready(){require(game.allowDamage(gamma)&&game.alivePlayers().size()==3,"real authorized command enrolls the native third participant");
            gamma.setNoGravity(true);gamma.getInventory().clearContent();gamma.setShiftKeyDown(false);gamma.removeAllEffects();game.items().mana().noMagic(gamma,0);
            root=game.lbc().rollTable(gamma.level(),gamma.position(),gamma,"lbc:witch_gens").stream().filter(LbcWitchClaims::matches).findFirst().orElseThrow();
            claim(gamma,2,"dmg_dealt",5000);claim(gamma,3,"lolidefecation",64);greed(gamma);var heart=base.cow(base.at.add(10,0,0));heart.entityTags().add("sin_greed_have_heart");
            require(gamma.entityTags().containsAll(Set.of("sin_wrath","sin_lust"))&&service.ownership().get(LbcWitchClaims.Mode.WRATH).equals(id),"actual source claims publish two independent UUID owners");
            start=game.server().getTickCount();
        }
        void handoff(){require(gamma.entityTags().contains("sin_greed_active")&&greedModifier(gamma),"actual native Greed heartbeat installed its permanent source modifiers");
            game.server().getPlayerList().saveAll();var stale=savedData();
            require(stale.toString().contains("e916adfe-014b-4ff5-82f1-af6ec9226d21"),"native active autosave contains the real Greed modifier");
            disconnect();require(savedTags().containsAll(Set.of("sin_wrath","sin_lust","sin_greed")),"real native player file contains old authority tags");
            // Reproduce loading an earlier native autosave after an unclean departure, using its exact bytes/data.
            try{NbtIo.writeCompressed(stale,savedPlayerPath());}catch(java.io.IOException error){throw new IllegalStateException(error);}
            claim(base.owner,2,"dmg_dealt",5000);greed(base.owner);
            require(base.owner.entityTags().contains("sin_wrath")&&service.ownership().get(LbcWitchClaims.Mode.WRATH).equals(base.owner.getUUID()),"offline incumbent is not an added qualification obstacle: source minimum still succeeds");
            start=game.server().getTickCount();
        }
        void claim(ServerPlayer p,int mode,String counter,int value){p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,root.copyWithCount(1));
            game.items().mana().score(p,"gens_type").set(mode);game.items().mana().score(p,counter).set(value);
            require(net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.invoker().interact(p,p.level(),InteractionHand.MAIN_HAND).consumesAction()&&p.getMainHandItem().isEmpty(),"actual root qualification/debit, mode "+mode);
        }
        void greed(ServerPlayer p){var token=game.lbc().rollTable(p.level(),p.position(),p,"lbc:ported/divider").stream().filter(s->LbcFartus.data(s).getIntOr("greed_greed",0)==1).findFirst().orElseThrow();p.getInventory().setItem(9,token);p.entityTags().add("greed_cursed");claim(p,4,"greed_prestige",0);}
        boolean greedModifier(ServerPlayer p){var key=net.minecraft.resources.Identifier.parse("minecraft:e916adfe-014b-4ff5-82f1-af6ec9226d21");return p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).hasModifier(key)||p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE).hasModifier(key);}
        private java.nio.file.Path savedPlayerPath(){return game.server().getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(id+".dat");}
        CompoundTag savedData(){try{return NbtIo.readCompressed(savedPlayerPath(),NbtAccounter.unlimitedHeap());}catch(java.io.IOException error){throw new IllegalStateException(error);}}
        Set<String> savedTags(){var nbt=savedData();var result=new HashSet<String>();
            for(var tag:nbt.getListOrEmpty("Tags"))tag.asString().ifPresent(result::add);return result;}
        void disconnect(){var previous=gamma;previous.connection.onDisconnect(new DisconnectionDetails(Component.literal("Witch lifecycle fixture")));require(game.server().getPlayerList().getPlayer(id)==null,"real listener disconnect removes native player");}
        private ServerPlayer connectSaved(String... expectedTags){
            var playerList=game.server().getPlayerList();
            require(playerList.getPlayer(id)==null,"fixture login starts with an offline player");
            var profile=new GameProfile(id,"TestGamma");
            var body=new ServerPlayer(game.server(),game.maps().level(),profile,ClientInformation.createDefault());
            // The normal PrepareSpawnTask loads player data before PlayerList.placeNewPlayer.
            var data=playerList.loadPlayerData(body.nameAndId()).orElseThrow();
            body.load(TagValueInput.create(ProblemReporter.DISCARDING,body.registryAccess(),data));
            require(body.entityTags().containsAll(List.of(expectedTags)),"native pre-join body contains the saved test tags");
            try{
                var factory=GameChecks.class.getDeclaredMethod("carpetConnection");
                factory.setAccessible(true);
                var connection=(Connection)factory.invoke(null);
                playerList.placeNewPlayer(connection,body,new CommonListenerCookie(profile,0,body.clientInformation(),false));
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Could not open fixture connection",error);}
            body.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            return playerList.getPlayer(id);
        }
        void reconnect(){var previous=gamma;gamma=connectSaved();require(gamma!=null&&gamma!=previous&&gamma.getUUID().equals(id),"native login loads a fresh session with the same saved UUID");}
        boolean step(){int now=game.server().getTickCount();require(now-start<(phase==3?600:60),"bounded native lifecycle scenario");
            if(phase==-3){
                if(now-start<2)return false;
                reconnect();
                require(game.isObserver(gamma)&&gamma.entityTags().contains("have_mom")
                                &&game.items().mana().score(gamma,"greed_prestige").get()==29,
                        "same-match spectator reconnect does not repeat the raw match reset");
                gamma.entityTags().remove("have_mom");
                game.items().mana().score(gamma,"greed_prestige").set(0);
                game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
                phase=-1;
                return false;
            }
            if(phase==-1){ready();phase=-2;return false;}
            if(phase==-2){if(now-start<2)return false;handoff();phase=0;return false;}
            if(phase==0){if(now-start<2)return false;reconnect();
                require(!gamma.entityTags().contains("sin_wrath")&&!gamma.entityTags().contains("sin_greed")&&!gamma.entityTags().contains("sin_greed_active")&&!greedModifier(gamma)&&gamma.entityTags().contains("sin_lust")&&base.owner.entityTags().contains("sin_wrath"),"old login NBT cannot reclaim transferred Wrath, while untouched Lust remains its own");
                require(game.isObserver(gamma)&&!game.canUseGameplayItems(gamma),"reconciliation never bypasses late-join spectator/game rules");
                game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
                require(game.allowDamage(gamma)&&gamma.entityTags().contains("sin_lust")&&!gamma.entityTags().contains("sin_wrath"),"operator reentry preserves only actual current authorities");
                base.owner.entityTags().remove("sin_wrath");phase++;return false;}
            if(phase==1){require(!service.ownership().containsKey(LbcWitchClaims.Mode.WRATH),"removing a live native authority tag also revokes its reconnect claim");
                var previous=gamma;gamma.setInvulnerableTime(0);gamma.damageCooldownTime=0;
                require(gamma.hurtServer(gamma.level(),gamma.damageSources().genericKill(),10000)&&gamma.isDeadOrDying(),"actual native participant death");
                gamma.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
                gamma=game.server().getPlayerList().getPlayer(id);gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                require(gamma!=previous&&gamma.isAlive()&&gamma.entityTags().contains("sin_lust")&&service.ownership().get(LbcWitchClaims.Mode.LUST).equals(id),"native death/replacement retains the untouched UUID authority");
                // This participant had one life: its replacement must remain eliminated, not silently rejoin.
                require(game.isObserver(gamma)&&!game.canUseGameplayItems(gamma)&&game.alivePlayers().size()==2,"authority restoration never resurrects an eliminated participant");
                gamma.entityTags().addAll(Set.of("sin_greed_active","indolence_active","kill_one_skeleton"));disconnect();require(savedTags().contains("sin_lust"),"offline source file really retains the still-owned authority");
                service.stop();phase++;return false;}
            if(phase==2){reconnect();
                require(Arrays.stream(LbcWitchClaims.Mode.values()).noneMatch(mode->gamma.entityTags().contains(mode.tag()))&&!gamma.entityTags().contains("sin_greed_active")&&!gamma.entityTags().contains("indolence_active")&&!gamma.entityTags().contains("kill_one_skeleton"),"match-end invalidation cleans offline authority and active flags at native next login");
                require(service.ownership().isEmpty()&&game.playing()&&game.alivePlayers().size()==2,"source progress and continuing match stay owned by the existing core");
                game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
                for(var p:List.of(base.owner,base.other))p.entityTags().add("have_mom");
                claim(gamma,7,"gens_type",7);
                require(gamma.entityTags().contains("sin_envy")&&id.equals(service.owner(LbcWitchClaims.Mode.ENVY)),"actual Envy qualification owns the raw tag before whole-match stop");
                gamma.entityTags().addAll(Set.of("greed_cursed","sin_greed_have_heart","skywars2.reset_fixture.keep"));
                game.items().mana().score(gamma,"sleep_time").set(712);game.items().mana().score(gamma,"coldown8").set(19);
                game.stopMatch();
                require(service.ownership().isEmpty()&&!gamma.entityTags().contains("sin_envy")
                        &&!gamma.entityTags().contains("greed_cursed")&&!gamma.entityTags().contains("sin_greed_have_heart")
                        &&!base.owner.entityTags().contains("have_mom")&&gamma.entityTags().contains("skywars2.reset_fixture.keep")
                        &&game.items().mana().hudValue(gamma,"sleep_time")==0&&game.items().mana().hudValue(gamma,"coldown8")==0,
                        "real controller stop removes legacy authority/progress/cooldown tags and scores, preserving unrelated tags");
                start=now;phase=3;return false;}
            if(phase==3){if(game.stage()!=GameController.Stage.LOBBY)return false;
                for(var p:game.server().getPlayerList().getPlayers())require(Arrays.stream(LbcWitchClaims.Mode.values()).noneMatch(mode->p.entityTags().contains(mode.tag()))
                        &&!p.entityTags().contains("have_mom"),"actual lobby handoff cannot restore previous-match authority or Witch qualification tags");
                gamma.entityTags().remove("skywars2.reset_fixture.keep");return true;}
            return false;
        }
        @SuppressWarnings("unchecked") public void close(){if(closed)return;closed=true;gammaProfile.wins=savedGammaWins;service.stop();game.items().witchPassives().stop();try{
            var live=game.server().getPlayerList().getPlayer(id);if(owned&&live!=null){gamma=live;disconnect();}
            // Only the fixture-created participant is removed; the two original native participants remain intact.
            if(owned){var field=GameController.class.getDeclaredField("participants");field.setAccessible(true);((Map<UUID,?>)field.get(game)).remove(id);}
            base.close();for(var p:List.of(base.owner,base.other)){p.entityTags().clear();p.entityTags().addAll(tags.get(p));for(var entry:scores.get(p).entrySet())if(entry.getValue()!=null)game.items().mana().score(p,entry.getKey()).set(entry.getValue());else{var objective=game.server().getScoreboard().getObjective(entry.getKey());if(objective!=null)game.server().getScoreboard().resetSinglePlayerScore(p,objective);}}
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive lifecycle fixture");running=new Fixture(game);try{running.init();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}SkyWars2.LOGGER.info("[check] START witch-lifecycle");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS witch-lifecycle (actual raw claims/native offline files, late-match reset, same-match spectator reconnect, current UUID owner versus stale tags, untransferred authority and native death/respawn, operator/spectator rules, explicit live revoke, offline match-end invalidation)");}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL witch-lifecycle",error);}}
}
