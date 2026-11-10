package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;

/** Real authorized commands against actual native participants and spectator camera state. */
public final class OperatorSettingsChecks {
    private OperatorSettingsChecks() { }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Operator settings: "+message);}
    private static Field field(Class<?> type,String name){try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
    private static Object get(Object object,String name){try{return field(object.getClass(),name).get(object);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object object,String name,Object value){try{field(object.getClass(),name).set(object,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static Object copy(Object value){if(value instanceof int[] a)return a.clone();if(value instanceof Set<?> s)return new HashSet<>(s);return value;}
    private static Map<Field,Object> snapshot(Object target){var saved=new LinkedHashMap<Field,Object>();try{for(var f:target.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers()))saved.put(f,copy(f.get(target)));}catch(IllegalAccessException e){throw new IllegalStateException(e);}return saved;}
    private static void restore(Object target,Map<Field,Object> values){try{for(var e:values.entrySet())e.getKey().set(target,copy(e.getValue()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static int command(GameController game,ServerPlayer player,String command,boolean op){
        try{return game.server().getCommands().getDispatcher().execute(command,player.createCommandSourceStack().withPermission(op?PermissionSet.ALL_PERMISSIONS:PermissionSet.NO_PERMISSIONS).withSuppressedOutput());}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){if(!op)return -1;throw new IllegalStateException(command,e);}
    }
    @SuppressWarnings("unchecked")
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer alpha,beta;final GameChecks.WandThirdPlayerState alphaState,betaState;
        final Map<Field,Object> config,alphaProfile,betaProfile;final Object member;final Map<UUID,Object> members;
        final Map<Field,Object> memberFields=new LinkedHashMap<>(),controllerFields=new LinkedHashMap<>();
        final PlayerTeam alphaTeam,betaTeam;final Object betaRespawn;final List<?> spawns;final String originalMap;
        final MatchScoreboardCheckpoint scoreboardBefore;final LbcTeamColors.Checkpoint colorsBefore;
        Fixture(GameController game){
            this.game=game;alpha=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));beta=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&alpha!=null&&beta!=null&&game.allowDamage(alpha)&&game.allowDamage(beta),"two real active match participants");
            require(!game.items().timeStop().stopped(alpha.level())&&alpha.containerMenu==alpha.inventoryMenu&&beta.containerMenu==beta.inventoryMenu,"clean native fixture boundary");
            alphaState=new GameChecks.WandThirdPlayerState(alpha,game.items().mana());betaState=new GameChecks.WandThirdPlayerState(beta,game.items().mana());
            config=snapshot(game.config());alphaProfile=snapshot(game.players().get(alpha.getUUID()));betaProfile=snapshot(game.players().get(beta.getUUID()));
            alphaTeam=alpha.getTeam();betaTeam=beta.getTeam();betaRespawn=get(beta,"respawnConfig");spawns=List.copyOf(game.maps().spawns());originalMap=game.config().mapId;
            scoreboardBefore=new MatchScoreboardCheckpoint(game.server());colorsBefore=game.teamColors().checkpoint();
            members=(Map<UUID,Object>)get(game,"participants");member=members.get(beta.getUUID());
            try{
                for(String name:List.of("lives","awaitingRespawn","disconnected")){var f=field(member.getClass(),name);memberFields.put(f,f.get(member));}
                for(String name:List.of("stage","readyMap","readyBroken","selectionVersion","lastAliveTicks")){var f=field(GameController.class,name);controllerFields.put(f,f.get(game));}
            }catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        void initialize(){
            for(var p:List.of(alpha,beta)){
                p.closeContainer();p.entityTags().remove("admin");p.removeAllEffects();p.setNoGravity(true);
                var profile=game.players().get(p.getUUID());profile.lbcKit=0;profile.lbcSkill=0;profile.lbcChallengeKit=0;profile.lbcChallenge=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);
            }
            game.config().vanillaKits=false;game.config().vanillaSkills=false;game.config().lbcKits=false;game.config().lbcSkills=false;game.config().lbcLevelups=false;game.config().lbcChallenges=false;
        }
        void settings(){
            // Storage remains signed when live operator controls rebuild effect instances.
            require(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,20,-2).getAmplifier()==-2
                    &&new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,20,Integer.MAX_VALUE).getAmplifier()==Integer.MAX_VALUE,
                    "operator controls preserve approved full signed effect storage");
            var modeRoot=game.server().getCommands().getDispatcher().getRoot().getChild("skywars").getChild("gamemode");
            for(String number:List.of("0","1","2","3"))require(modeRoot.getChild(number) instanceof com.mojang.brigadier.tree.LiteralCommandNode<?>,"numeric alias is a vanilla-client-readable literal: "+number);
            require(command(game,alpha,"skywars settings",true)==1,"operator can read settings during a real match");
            var dispatcher=game.server().getCommands().getDispatcher();
            var source=alpha.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS);
            var names=dispatcher.getCompletionSuggestions(dispatcher.parse("skywars settings ",source)).join()
                    .getList().stream().map(com.mojang.brigadier.suggestion.Suggestion::getText).toList();
            require(names.containsAll(List.of("healthDisplay","imbaDropChance","luckyModifier","offhandWands")),"canonical settings are suggested");
            String[][] removed={{"hp","2"},{"imbadrop_chance","6"},{"cfg_lucky_modifier","7"},{"lefthandWands","true"},
                    {"countdown","6"},{"ending","3"},{"stage1seconds","700"},{"stage2seconds","1400"},
                    {"stage1event","2"},{"stage2event","2"},{"pvp","1"},{"mapid",originalMap}};
            for(var alias:removed){
                require(names.stream().noneMatch(alias[0]::equalsIgnoreCase),"removed duplicate is absent from completion: "+alias[0]);
                require(command(game,alpha,"skywars settings "+alias[0]+" "+alias[1],true)==0,"removed duplicate is not an accepted setting: "+alias[0]);
            }
            for(String denied:List.of("skywars settings","skywars settings health 19","skywars settings event halloween true","skywars gamemode 0","gamemode creative TestBeta"))
                require(command(game,beta,denied,false)==-1,"native permission rejects nonoperator command "+denied);
            String[][] changes={{"lives","4"},{"health","19"},{"countdownSeconds","6"},{"stageOneSeconds","700"},{"stageTwoSeconds","1400"},{"endingSeconds","3"},{"stageOneEvent","2"},{"stageTwoEvent","2"},{"pvpMode","1"},{"tntProtection","3"},{"gameMode","2"},{"bedwarsDirectBedBreaking","false"},{"luckyModifier","5"},{"day","2"},{"ranked","true"},{"vanillaKits","true"},{"vanillaSkills","true"},{"lbcKits","true"},{"lbcSkills","true"},{"lbcLevelups","true"},{"lbcChallenges","true"},{"offhandWands","true"},{"lbcLevelupLimit","7"},{"lbcTrinketLimit","6"},{"noBorders","true"}};
            for(var pair:changes)require(command(game,alpha,"skywars settings "+pair[0]+" "+pair[1],true)==1,"operator can change "+pair[0]+" during match");
            var loaded=GameConfig.load();require(loaded.health==19&&loaded.lives==4&&loaded.lbcKits&&loaded.lbcSkills&&loaded.lbcLevelupLimit==7&&loaded.lbcTrinketLimit==6&&loaded.gameMode==2&&loaded.offhandWands&&!loaded.bedwarsDirectBedBreaking,"actual operator writes survive config reload");
            String next=originalMap.equals("1")?"2":"1";
            require(command(game,alpha,"skywars settings map "+next,true)==1&&game.config().mapId.equals(next)&&!game.maps().busy()&&game.maps().spawns().equals(spawns),"map selection persists without replacing current arena/spawns");
            String brokenMap=game.maps().catalog().stream().filter(m->m.available()&&game.maps().brokenAvailable(m.id())).findFirst().orElseThrow().id();
            require(command(game,alpha,"skywars settings map "+brokenMap,true)==1&&command(game,alpha,"skywars settings broken true",true)==1&&game.config().broken&&!game.maps().busy(),"broken selection also changes during match without a paste");
            require(command(game,alpha,"skywars settings broken false",true)==1&&command(game,alpha,"skywars settings map "+originalMap,true)==1,"restore selection through same native commands");
            for(var stage:GameController.Stage.values()){
                set(game,"stage",stage);
                require(command(game,alpha,"skywars settings offhandWands false",true)==1,"operator settings allowed at stage "+stage);
                for(String event:List.of("halloween","november","tournament"))require(command(game,alpha,"skywars settings event "+event+" true",true)==1,"operator event setting at stage "+stage);
            }
            restoreController();
            int health=game.config().health;require(command(game,alpha,"skywars settings health 61",true)==0&&game.config().health==health,"live setting validation still rejects invalid bounds");
            require(command(game,alpha,"skywars settings event missing true",true)==0,"unknown events still rejected");
            restore(game.config(),config);game.config().save();game.luckyModifiers().syncConfig();game.lbc().syncRanked();initialize();
        }
        void observer(){
            set(member,"lives",0);set(member,"awaitingRespawn",false);set(member,"disconnected",false);
            game.spectators().enter(beta);require(game.isObserver(beta)&&!game.allowDamage(beta),"actual viewer state is excluded from match");
        }
        void returned(GameType expected){
            require(!game.isObserver(beta)&&!beta.isSpectator()&&beta.gameMode()==expected&&game.allowDamage(beta)&&game.canUseGameplayItems(beta)&&game.alivePlayers().contains(beta)&&game.lives(beta.getUUID())==1,"native command restores live roster/mode/life/item admission: "+expected);
            var actualTeam=beta.getTeam();
            boolean teamRestored=betaTeam==null?actualTeam==null:actualTeam!=null
                    &&game.server().getScoreboard().getPlayerTeam(betaTeam.getName())==actualTeam
                    &&sameTeam(actualTeam,betaTeam);
            require(beta.getCamera()==beta&&!beta.isInvisible()&&!beta.noPhysics&&!beta.isNoGravity()&&teamRestored,
                    "self camera, collision, gravity and exact prior team metadata restored: mode="+expected+", camera="+(beta.getCamera()==beta)
                    +", invisible="+beta.isInvisible()+", noPhysics="+beta.noPhysics+", noGravity="+beta.isNoGravity()+", team="+teamRestored);
            if(actualTeam!=null&&actualTeam!=betaTeam)SkyWars2.LOGGER.info("[check] operator-settings native return reconstructed retired team {} with exact metadata",actualTeam.getName());
            if(expected!=GameType.CREATIVE)require(!beta.getAbilities().mayfly&&!beta.getAbilities().flying&&!beta.getAbilities().invulnerable,"viewer flight/immunity removed for survival/adventure");
            game.spectators().tick();require(beta.gameMode()==expected&&!game.isObserver(beta),"next observer tick cannot revert returned player");
        }
        void gamemodes(){
            for(var entry:Map.of("2",GameType.ADVENTURE,"0",GameType.SURVIVAL,"1",GameType.CREATIVE,"adventure",GameType.ADVENTURE,"survival",GameType.SURVIVAL,"creative",GameType.CREATIVE).entrySet()){
                observer();require(command(game,beta,(entry.getKey().matches("[0-3]")?"skywars gamemode ":"gamemode ")+entry.getKey(),true)==1,"actual self command restores "+entry.getKey());returned(entry.getValue());
            }
            observer();beta.setGameMode(GameType.SURVIVAL);require(game.isObserver(beta)&&game.lives(beta.getUUID())==0,"ordinary internal setGameMode never enrolls a viewer");
            require(command(game,beta,"skywars gamemode 0",false)==-1&&game.isObserver(beta),"nonoperator cannot escape observer with command");
            require(command(game,alpha,"skywars gamemode 1 TestBeta",true)==1,"operator can explicitly restore another target");returned(GameType.CREATIVE);
            beta.getInventory().setItem(0,new ItemStack(Items.STONE,13));beta.getInventory().setSelectedSlot(0);int lives=game.lives(beta.getUUID());int games=game.players().get(beta.getUUID()).games;
            require(command(game,beta,"skywars gamemode 0",true)==1&&beta.getMainHandItem().getCount()==13&&game.lives(beta.getUUID())==lives&&game.players().get(beta.getUUID()).games==games,"active mode changes preserve items/lives and do not reenroll or award another match");
            observer();require(command(game,beta,"skywars gamemode 3",true)==1&&game.isObserver(beta)&&game.lives(beta.getUUID())==0,"spectator command does not rejoin");
            require(command(game,beta,"skywars gamemode 2",true)==1,"same-mode Adventure restoration still succeeds");returned(GameType.ADVENTURE);
        }
        void cameraAndReconnect(){
            alpha.getInventory().setItem(0,new ItemStack(Items.DIAMOND,11));observer();
            game.spectators().watch(beta,alpha);require(game.spectators().attached(beta)&&beta.getCamera()==alpha,"real target camera attached before return");
            require(command(game,beta,"skywars gamemode 0",true)==1,"camera viewer can return via real command");returned(GameType.SURVIVAL);
            require(beta.getInventory().countItem(Items.DIAMOND)==0,"mirrored target inventory is never retained on return");
            game.leave(beta);require((boolean)get(member,"disconnected")&&game.lives(beta.getUUID())==0,"actual leave marks participant disconnected");
            game.join(beta);require(game.isObserver(beta),"actual reconnect still joins as viewer until explicit command");
            require(command(game,beta,"skywars gamemode 0",true)==1,"operator explicit return after reconnect");returned(GameType.SURVIVAL);
            members.remove(beta.getUUID());game.spectators().enter(beta);
            require(command(game,beta,"skywars gamemode 0",true)==1&&members.get(beta.getUUID())!=member,"late operator with no participant record can explicitly enroll");returned(GameType.SURVIVAL);
            members.put(beta.getUUID(),member);
        }
        void restoreController(){try{for(var e:controllerFields.entrySet())e.getKey().set(game,e.getValue());}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
        public void close(){
            game.spectators().remove(beta);members.put(beta.getUUID(),member);
            try{for(var e:memberFields.entrySet())e.getKey().set(member,e.getValue());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
            restore(game.config(),config);game.config().save();restoreController();restore(game.players().get(alpha.getUUID()),alphaProfile);restore(game.players().get(beta.getUUID()),betaProfile);
            alphaState.restore(game);betaState.restore(game);set(beta,"respawnConfig",betaRespawn);
            var tickRate=game.server().tickRateManager();boolean wasFrozen=tickRate.isFrozen();
            try{tickRate.setFrozen(true);scoreboardBefore.restore(game.server());game.teamColors().restore(colorsBefore);}
            finally{tickRate.setFrozen(wasFrozen);}
            for(var p:List.of(alpha,beta)){game.items().indirectKills().clear(p);game.items().magic().inventoryChanged(p);}
            game.luckyModifiers().syncConfig();game.lbc().syncRanked();game.items().magic().flushCleansing();game.players().save();
        }
    }
    private static boolean sameTeam(PlayerTeam actual,PlayerTeam previous){
        return actual.getName().equals(previous.getName())&&actual.getColor().equals(previous.getColor())
                &&actual.getDisplayName().equals(previous.getDisplayName())&&actual.getPlayerPrefix().equals(previous.getPlayerPrefix())
                &&actual.getPlayerSuffix().equals(previous.getPlayerSuffix())&&actual.isAllowFriendlyFire()==previous.isAllowFriendlyFire()
                &&actual.canSeeFriendlyInvisibles()==previous.canSeeFriendlyInvisibles()
                &&actual.getDeathMessageVisibility()==previous.getDeathMessageVisibility()&&actual.getNameTagVisibility()==previous.getNameTagVisibility()
                &&actual.getCollisionRule()==previous.getCollisionRule();
    }
    private static void recreatedTeam(ServerPlayer player){
        var board=new net.minecraft.world.scores.Scoreboard();var original=board.addPlayerTeam("old_ops");board.addPlayerToTeam(player.getScoreboardName(),original);
        var occupied=board.addPlayerTeam("1");board.addPlayerToTeam("someone_else",occupied);
        var colors=new LbcTeamColors(board);colors.begin(List.of(player));var assigned=colors.assigned(player);var color=assigned.getColor();
        colors.leave(player);require(board.getPlayerTeam(assigned.getName())==null&&board.getPlayersTeam(player.getScoreboardName())==original,"disconnect removes empty generated team and restores original");
        colors.rejoin(player);var recreated=colors.assigned(player);require(recreated!=null&&recreated!=assigned&&recreated.getName().equals(assigned.getName())&&recreated.getColor().equals(color)&&board.getPlayersTeam(player.getScoreboardName())==recreated,"operator return reconstructs deleted match team and metadata");
        colors.stop();require(board.getPlayersTeam(player.getScoreboardName())==original&&board.getPlayerTeam(recreated.getName())==null&&board.getPlayersTeam("someone_else")==occupied,"actual team stop restores originals and retains unrelated members");
    }
    public static void run(GameController game){try(var f=new Fixture(game)){f.initialize();f.settings();f.gamemodes();f.cameraAndReconnect();recreatedTeam(f.beta);}SkyWars2.LOGGER.info("[check] PASS operator-settings (all native /skywars settings values/stages and permissions, deferred map/broken, numeric/name gamemode return, same Adventure, viewer camera/inventory cleanup, exact teams, actual leave/join, late enrollment and deleted team reconstruction)");}
}
