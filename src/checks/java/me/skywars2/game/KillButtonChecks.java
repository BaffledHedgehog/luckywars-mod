package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.phys.Vec3;

/** Actual native immediate inputs, deterministic native lottery and owned replacement-player deaths. */
public final class KillButtonChecks {
    private KillButtonChecks(){}
    private static Scene running;
    private static boolean hooked;
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Kill button: "+why);}
    public static void run(GameController game){
        require(running==null,"exclusive scheduled scene");
        if(!hooked){ServerTickEvents.START_LEVEL_TICK.register(level->{var scene=running;if(scene!=null&&scene.f.level==level)try{scene.input();}catch(Throwable error){failed(scene,error);}});hooked=true;}
        var scene=new Scene(game);running=scene;
        try{scene.begin();}catch(Throwable error){running=null;try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
    }
    public static void tick(GameController game){var scene=running;if(scene==null)return;try{
        require(scene.game==game,"same controller");scene.acknowledge();require(game.server().getTickCount()-scene.started<160,"bounded native scene phase="+scene.phase);
        if(!scene.done)return;running=null;scene.close();
        SkyWars2.LOGGER.info("[check] PASS kill-button (real immediate Item.use and both-hand carrot/snowball TMT inputs; all six self rolls and seventh other; TECH24 spent cost/no RNG, MAG, Greed, void, protected roles, Nexus chosen-before-guard/no reroll, empty-target advancement, TimeStop; owned Gamma generic-kill/replacement and actual OTHER credit; lethal TMT SELF full outer death drop/private sibling preservation; original core/body/stats cleanup)");
    }catch(Throwable error){failed(scene,error);}}
    private static void failed(Scene scene,Throwable error){if(running!=scene)return;running=null;error.addSuppressed(new IllegalStateException("native Kill Button phase="+scene.phase));try{scene.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}SkyWars2.LOGGER.error("[check] FAIL kill-button",error);}

    private static final class Scene implements AutoCloseable {
        final GameController game;
        final WoolBucketChecks.Fixture f;
        final UUID gammaId=GameChecks.offlineUuid("TestGamma");
        final int started;
        final Set<UUID> initial=new HashSet<>();
        final Map<UUID,Map<Stat<?>,Integer>> stats=new LinkedHashMap<>();
        final MatchRandomState levelRandom;
        final LbcTmtAbilities.Ability kill,sibling;
        final ItemStack source;
        MatchCoreCheckpoint image;
        ServerPlayer gamma;
        int phase;
        boolean owned,closed,done;
        Scene(GameController game){
            this.game=game;require(game.server().getPlayerList().getPlayer(gammaId)==null,"never borrow TestGamma session");
            require(game.server().getPlayerList().getPlayers().size()==2,"exclusive Alpha/Beta roster; random selectors cannot endanger an unrelated session");
            f=new WoolBucketChecks.Fixture(game,false);started=game.server().getTickCount();levelRandom=MatchRandomState.capture(f.level.getRandom());
            for(var level:game.server().getAllLevels())for(var actor:level.getAllEntities())initial.add(actor.getUUID());
            saveStats(f.owner);saveStats(f.other);
            String id=BuiltInRegistries.ITEM.getKey(LbcKillButton.TOKEN).toString();
            var canonical=game.items().tmt().useProfiles().families().stream().flatMap(family->family.variants().stream())
                    .filter(property->((ItemData.IntReader)(Object)property.data()).skywars2$readString(LbcTmtAbilities.NATIVE,"").equals(id)).findFirst().orElseThrow();
            source=LbcKillButton.TOKEN.getDefaultInstance();require(LbcKillButton.matches(source),"full native default contains authored kill_rand selector");
            var privateTag=source.get(DataComponents.CUSTOM_DATA).copyTag();privateTag.putString("kill_source_only","private source");source.set(DataComponents.CUSTOM_DATA,CustomData.of(privateTag));
            source.set(DataComponents.CUSTOM_NAME,Component.literal("private native kill source"));
            var payload=canonical.data().copyTag();payload.putString("kill_source_only","private source");LbcTmtComponents.capture(game,source,payload);kill=new LbcTmtAbilities.Ability(canonical.id(),CustomData.of(payload));
            var harmless=new CompoundTag();harmless.putString("sibling_unchanged","independent private property");harmless.putString(LbcTmtAbilities.NATIVE,"minecraft:stick");
            var stick=new ItemStack(Items.STICK);stick.set(DataComponents.CUSTOM_DATA,CustomData.of(harmless));LbcTmtComponents.capture(game,stick,harmless);
            sibling=new LbcTmtAbilities.Ability("check/kill_button_harmless",CustomData.of(harmless));
        }
        void saveStats(ServerPlayer player){var values=new LinkedHashMap<Stat<?>,Integer>();
            for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);values.put(stat,player.getStats().getValue(stat));}
            for(var item:List.of(LbcKillButton.TOKEN,LbcTmtItems.prototype(0).getItem(),LbcTmtItems.prototype(2).getItem(),Items.SNOWBALL,Items.CARROT_ON_A_STICK,Items.STICK,Items.PAPER,Items.AIR)){
                for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_DROPPED)){var stat=type.get(item);values.put(stat,player.getStats().getValue(stat));}}
            var stat=Stats.ENTITY_KILLED.get(EntityTypes.PLAYER);values.put(stat,player.getStats().getValue(stat));stats.put(player.getUUID(),values);
        }
        void begin(){f.prepareParticipants();gamma=GameChecks.createBot(game,"TestGamma");owned=true;saveStats(gamma);
            game.server().getCommands().performPrefixedCommand(game.server().createCommandSourceStack(),"gamemode survival TestGamma");
        }
        void acknowledge(){f.client.acknowledge();f.audience.acknowledge();if(gamma!=null&&gamma.connection instanceof MatchPlayerCheckpoint.SimulatedAcknowledgement ack)ack.skywars2$acknowledgeRestore();}
        void move(ServerPlayer player,ServerLevel level,Vec3 at){require(player.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"real native test participant teleport");player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);acknowledge();}
        void policies(){for(var player:List.of(f.owner,f.other,gamma)){
            player.entityTags().removeAll(List.of("sin_greed_active","strict_map_object","spectator","rcdenyalways"));game.items().mana().score(player,"void_resistance").set(0);
            player.stopUsingItem();player.removeAllEffects();player.getAbilities().invulnerable=false;player.setInvulnerableTime(0);player.damageCooldownTime=0;}
            require(f.owner.isAlive()&&f.other.isAlive(),"borrowed Alpha/Beta survive every lottery");
        }
        void prepareGamma(){move(gamma,f.level,f.home.add(40,0,0));gamma.getInventory().clearContent();gamma.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);gamma.setHealth(20);gamma.setAbsorptionAmount(0);gamma.setNoGravity(true);}
        void clearOutputs(){for(var level:game.server().getAllLevels())for(var actor:com.google.common.collect.Lists.newArrayList(level.getAllEntities()))
            if(!initial.contains(actor.getUUID())&&(actor instanceof ItemEntity||actor instanceof ExperienceOrb||actor instanceof Snowball)){game.items().entityUnloaded(actor);actor.discard();}}
        void revoke(){var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/legends/kill_button"));require(advancement!=null,"real kill-button advancement exists");for(var key:advancement.value().criteria().keySet())f.owner.getAdvancements().revoke(advancement,key);}
        void awarded(){var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/legends/kill_button"));require(advancement!=null&&f.owner.getAdvancements().getOrStartProgress(advancement).isDone(),"seventh branch grants real native advancement even without a kill");}
        // Search native RNG seeds, without replacing the algorithm or owner event. Candidate sets
        // are independently declared by each scenario; order is the real connected roster order.
        long seed(ServerPlayer caster,int roll,List<ServerPlayer> expectedCandidates,ServerPlayer target){
            var ordered=new ArrayList<>(expectedCandidates);ordered.sort(Comparator.comparingInt(player->game.server().getPlayerList().getPlayers().indexOf(player)));
            int index=target==null?-1:ordered.indexOf(target);require(target==null||index>=0,"declared target belongs to test candidate set");
            for(long seed=1;seed<100000;seed++){caster.getRandom().setSeed(seed);if(caster.getRandom().nextInt(7)!=roll)continue;
                if(roll==6&&!ordered.isEmpty()&&caster.getRandom().nextInt(ordered.size())!=index)continue;caster.getRandom().setSeed(seed);return seed;}
            throw new IllegalStateException("No bounded native RNG seed");
        }
        long tail(ServerPlayer caster,long seed,int draws,int candidates){caster.getRandom().setSeed(seed);if(draws>0)caster.getRandom().nextInt(7);if(draws>1)caster.getRandom().nextInt(candidates);long tail=caster.getRandom().nextLong();caster.getRandom().setSeed(seed);return tail;}
        ItemStack ordinary(){return source.copyWithCount(3);}
        ItemStack mixed(int carrier){var stack=LbcTmtItems.prototype(carrier);stack.set(DataComponents.MAX_STACK_SIZE,16);stack.remove(DataComponents.MAX_DAMAGE);stack.setCount(3);
            var data=stack.get(DataComponents.CUSTOM_DATA).copyTag();data.putInt("kill_rand",0);data.putString("outer_only","retain full physical TMT");LbcTmtAbilities.write(data,List.of(sibling,kill));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            stack.set(DataComponents.CUSTOM_NAME,Component.literal("complete outer kill transport"));stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.PAPER,7))));
            stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(0x335577),List.of(),Optional.empty()));var entity=new CompoundTag();entity.putString("outer_entity_only","not the native source");stack.set(DataComponents.ENTITY_DATA,TypedEntityData.of(EntityTypes.PIG,entity));
            var before=stack.copy();require(LbcTmtAbilities.invoke(stack,kill.id(),()->{
                require(LbcKillButton.matches(stack)&&ItemData.intValue(stack,"kill_rand",0)==1&&ItemData.stringValue(stack,"kill_source_only","").equals("private source"),"private native kill selector overrides conflicting outer data");
                require(!stack.has(DataComponents.CONTAINER)&&!stack.has(DataComponents.POTION_CONTENTS)&&!stack.has(DataComponents.ENTITY_DATA),"native source receives no unrelated outer payload");
                require(ItemStack.matches(LbcTmtAbilities.outerCopy(stack),before),"independent complete outer copy inside private factory scope");return true;
            }),"actual private source projection");return stack;
        }
        ItemStack input(ServerPlayer caster,ItemStack stack,InteractionHand hand,boolean nativeGameMode,boolean accepted,int cost){
            var opposite=hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;var other=new ItemStack(Items.STICK,7);caster.setItemInHand(hand,stack);caster.setItemInHand(opposite,other);
            var before=stack.copy();var used=Stats.ITEM_USED.get(stack.getItem());int uses=caster.getStats().getValue(used);
            var result=nativeGameMode?caster.gameMode.useItem(caster,caster.level(),stack,hand):stack.getItem().use(caster.level(),caster,hand);
            require(result.consumesAction()==accepted,"real "+(nativeGameMode?"game-mode":"Item.use")+" admission accepted="+accepted+" phase="+phase);
            require(!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"source and identity scopes leave real native input");
            if(caster.isAlive()){
                require(caster.getItemInHand(hand)==stack&&ItemStack.matches(stack,before.copyWithCount(before.getCount()-cost))&&caster.getItemInHand(opposite)==other&&other.getCount()==7,"exact native debit, full outer/sibling payload and untouched opposite hand");
                require(caster.getStats().getValue(used)==uses+cost,"native use statistics retain accepted property/base costs and rejected-input zero cost");
            }
            return before.copyWithCount(before.getCount()-cost);
        }
        void guarded(int roll,List<ServerPlayer> candidates,ServerPlayer target,int draws){long seed=seed(f.owner,roll,candidates,target);long expected=tail(f.owner,seed,draws,candidates.size());
            input(f.owner,ordinary(),InteractionHand.MAIN_HAND,false,true,1);require(f.owner.getRandom().nextLong()==expected,"exact lottery RNG consumption/no target reroll");
            require(f.owner.isAlive()&&f.other.isAlive()&&gamma.isAlive(),"guarded lottery cannot kill another participant");}
        void controls(){require(game.allowDamage(gamma)&&game.lives(gammaId)==1&&game.alivePlayers().size()==3,"actual operator-enrolled third participant");prepareGamma();policies();
            // Each of the six losing lottery values is self-directed, protected only by void.
            for(var player:List.of(f.owner,f.other,gamma))game.items().mana().score(player,"void_resistance").set(1);for(int roll=0;roll<6;roll++)guarded(roll,List.of(),null,1);
            game.items().mana().score(f.owner,"void_resistance").set(0);f.owner.entityTags().add("sin_greed_active");guarded(0,List.of(),null,1);f.owner.entityTags().remove("sin_greed_active");
            game.items().mana().score(f.owner,"void_resistance").set(1);f.field(true,f.owner.position().add(0,23,0));require(game.items().magic().mechanismSuppressedAt(f.level,f.owner.position(),24)&&!game.items().magic().mechanismSuppressedAt(f.level,f.owner.position(),20),"real TECH field proves source radius24 rather than20");
            long seed=seed(f.owner,0,List.of(),null),expected=tail(f.owner,seed,0,0);input(f.owner,ordinary(),InteractionHand.OFF_HAND,false,true,1);require(f.owner.getRandom().nextLong()==expected,"TECH still spends source item but never rolls lottery");f.removeFields();
            game.items().mana().score(f.other,"void_resistance").set(1);game.items().mana().score(gamma,"void_resistance").set(1);revoke();guarded(6,List.of(f.other,gamma),gamma,2);awarded();
            // A target TECH veto happens after the same one target choice; the caster is outside it.
            game.items().mana().score(gamma,"void_resistance").set(0);f.field(true,gamma.position().add(0,1,0));require(!game.items().magic().mechanismSuppressedAt(f.level,f.owner.position(),24)&&game.items().magic().mechanismSuppressedAt(f.level,gamma.position(),24),"actual distinct target-only TECH field");guarded(6,List.of(f.other,gamma),gamma,2);f.removeFields();
            var nexus=game.server().getLevel(LbcHedgehogDimensions.NEXUS);require(nexus!=null,"real Nexus dimension");move(gamma,nexus,new Vec3(f.home.x,120,f.home.z));guarded(6,List.of(f.other,gamma),gamma,2);move(gamma,f.level,f.home.add(40,0,0));
            gamma.entityTags().add("strict_map_object");guarded(6,List.of(f.other),f.other,2);gamma.entityTags().remove("strict_map_object");
            gamma.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);require(gamma.isSpectator(),"actual native spectator role positive control");guarded(6,List.of(f.other),f.other,2);gamma.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);require(game.allowDamage(gamma),"native role control preserves enrolled Gamma on return");
            f.other.entityTags().add("sin_greed_active");gamma.entityTags().add("sin_greed_active");revoke();guarded(6,List.of(),null,1);awarded();policies();
            game.items().mana().score(f.owner,"void_resistance").set(1);f.owner.entityTags().add("strict_map_object");seed=seed(f.owner,0,List.of(),null);expected=tail(f.owner,seed,0,0);input(f.owner,ordinary(),InteractionHand.MAIN_HAND,false,false,0);require(f.owner.getRandom().nextLong()==expected,"protected-role caster rejected before debit/RNG");f.owner.entityTags().remove("strict_map_object");
            f.pause();try{var mixed=mixed(0);seed=seed(f.owner,0,List.of(),null);expected=tail(f.owner,seed,0,0);input(f.owner,mixed,InteractionHand.OFF_HAND,true,false,0);require(f.owner.getRandom().nextLong()==expected,"actual TimeStop rejects TMT input before debit/lottery");}finally{f.thaw();}
            policies();image=new MatchCoreCheckpoint(game);
        }
        void replacement(){require(gamma.isDeadOrDying(),"only owned Gamma needs replacement");var previous=gamma;
            gamma.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));gamma=game.server().getPlayerList().getPlayer(gammaId);require(gamma!=null&&gamma!=previous&&gamma.isAlive(),"real native replacement body");gamma.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            game.server().tickRateManager().setFrozen(true);try{image.restoreFrozen();}finally{game.server().tickRateManager().setFrozen(false);}require(game.allowDamage(gamma)&&!game.isObserver(gamma)&&game.lives(gammaId)==1,"frozen core restores actual replacement enrollment");prepareGamma();policies();clearOutputs();
        }
        void death(){require(gamma.isDeadOrDying()&&gamma.getLastDamageSource()!=null&&gamma.getLastDamageSource().is(DamageTypes.GENERIC_KILL),"literal native genericKill death, not ordinary large damage");require(game.lives(gammaId)==0&&game.awaitingRespawn(gammaId)&&f.owner.isAlive()&&f.other.isAlive(),"only owned Gamma eliminated; Alpha/Beta remain live participants");}
        void input(){if(done)return;acknowledge();
            if(phase==0){controls();phase++;return;}
            if(phase>=1&&phase<=4){policies();for(var player:List.of(f.owner,f.other,gamma))game.items().mana().score(player,"void_resistance").set(1);int index=phase-1,carrier=index<2?0:2;var hand=InteractionHand.values()[index%2];
                if(phase==1){f.field(false,f.owner.position().add(0,5,0));require(game.items().magic().suppressed(f.owner),"actual MAG positive control");}
                long seed=seed(f.owner,0,List.of(),null),expectedTail=tail(f.owner,seed,1,0);
                input(f.owner,mixed(carrier),hand,true,true,carrier==2?2:1);require(f.owner.isAlive(),"MAG does not suppress accepted protected-self lottery");
                if(carrier==0)require(f.owner.getRandom().nextLong()==expectedTail,"actual carrot lottery still rolls once under MAG; no inferred suppression");
                int after=0;for(var actor:f.level.getAllEntities())if(actor instanceof Snowball ball&&!initial.contains(ball.getUUID())){after++;require(ball.getOwner()==f.owner&&LbcTmtItems.matches(ball.getItem()),"real snowball carrier preserves its native owned base throw");}
                require(after==(carrier==2?1:0),"immediate native factory adds no fake lottery projectile; base transport count");f.removeFields();clearOutputs();phase++;return;}
            if(phase==5){policies();game.items().mana().score(f.owner,"void_resistance").set(1);f.field(true,f.owner.position().add(0,23,0));long seed=seed(f.owner,0,List.of(),null),expected=tail(f.owner,seed,0,0);input(f.owner,mixed(0),InteractionHand.OFF_HAND,true,true,1);require(f.owner.getRandom().nextLong()==expected&&f.owner.isAlive(),"TMT private Kill Button still debits under TECH without RNG/effect");f.removeFields();phase++;return;}
            if(phase==6){policies();prepareGamma();for(var player:List.of(f.owner,f.other))game.items().mana().score(player,"void_resistance").set(1);seed(gamma,0,List.of(),null);input(gamma,ordinary(),InteractionHand.MAIN_HAND,false,true,1);death();phase++;return;}
            if(phase==7){replacement();game.items().mana().score(f.owner,"void_resistance").set(1);f.other.entityTags().add("sin_greed_active");int kills=game.players().get(f.owner.getUUID()).kills,coins=game.players().get(f.owner.getUUID()).coins;seed(f.owner,6,List.of(gamma),gamma);
                input(f.owner,ordinary(),InteractionHand.OFF_HAND,false,true,1);death();require(game.players().get(f.owner.getUUID()).kills==kills+1&&game.players().get(f.owner.getUUID()).coins==coins+50,"actual OTHER genericKill inherits caster kill credit and native reward");awarded();phase++;return;}
            if(phase==8){replacement();for(var player:List.of(f.owner,f.other))game.items().mana().score(player,"void_resistance").set(1);var stack=mixed(0);seed(gamma,0,List.of(),null);var expected=input(gamma,stack,InteractionHand.OFF_HAND,true,true,1);death();
                var drops=f.level.getEntitiesOfClass(ItemEntity.class,gamma.getBoundingBox().inflate(8)).stream().filter(item->!initial.contains(item.getUUID())&&LbcTmtItems.matches(item.getItem())).toList();
                require(drops.size()==1&&ItemStack.matches(drops.getFirst().getItem(),expected),"lethal private SELF callback runs after native completion and drops exactly the complete debited outer item, never the source projection");
                var dropped=drops.getFirst().getItem();require(LbcTmtAbilities.read(dropped).size()==2&&LbcTmtAbilities.read(dropped).stream().anyMatch(property->property.equals(sibling))&&!LbcTmtAbilities.active(dropped),"actual lethal death drop preserves harmless sibling and clean full private payload");phase++;return;}
            replacement();done=true;
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;game.server().tickRateManager().setFrozen(true);
            for(Runnable action:List.<Runnable>of(()->{f.removeFields();if(f.stopped)f.thaw();},()->{
                for(var entry:stats.entrySet()){var player=game.server().getPlayerList().getPlayer(entry.getKey());if(player!=null)entry.getValue().forEach((stat,value)->player.getStats().setValue(player,stat,value));}
                var current=game.server().getPlayerList().getPlayer(gammaId);if(owned&&current!=null)current.connection.onDisconnect(new DisconnectionDetails(Component.literal("Kill Button fixture cleanup")));
            },this::clearOutputs,()->{game.items().tmt().forget(f.owner);if(gamma!=null)game.items().tmt().forget(gamma);levelRandom.restore(f.level.getRandom());},f::close))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Kill Button scoped cleanup",failure);
        }
    }
}
