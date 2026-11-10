package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native criterion and scheduled-clock fixtures; isolated leaves prove payloads, not normal acquisition. */
public final class IncomingArtifactChecks {
    private IncomingArtifactChecks(){}
    private record SandHit(float amount,float before,float after,boolean accepted,Vec3 at,int cooldown,net.minecraft.world.damagesource.DamageSource source){}
    private static final class SandWitness extends net.minecraft.world.entity.animal.cow.Cow {
        final List<SandHit> hits=new ArrayList<>();
        SandWitness(net.minecraft.server.level.ServerLevel level){super(EntityTypes.COW,level);}
        @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){float hp=getHealth();int cooldown=damageCooldownTime;var at=position();boolean accepted=super.hurtServer(level,source,amount);hits.add(new SandHit(amount,hp,getHealth(),accepted,at,cooldown,source));return accepted;}
    }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Incoming artifacts: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-4,why+" actual="+actual+" expected="+expected);}
    private static java.lang.reflect.Field field(Object actor,String name)throws NoSuchFieldException{for(Class<?> type=actor.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    /** The real network scheduler owns this connection's native super.tick; the fixture never invokes it. */
    private static final class ScheduledConnection extends net.minecraft.network.Connection {
        final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();
        net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket latestPosition;
        ScheduledConnection(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public boolean isConnecting(){return false;}
        @Override public boolean isConnected(){return true;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);if(packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket position)latestPosition=position;}
        @Override public void flushChannel(){}
    }
    private static final class ScheduledClient implements AutoCloseable {
        final GameController game;final ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous,listener;
        final ScheduledConnection connection=new ScheduledConnection();final java.lang.reflect.Field updates;
        ScheduledClient(GameController game,ServerPlayer player){this.game=game;this.player=player;previous=player.connection;
            try{
                listener=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),connection,player,new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
                // A fresh memory connection has no protocol channel pipeline; bind only its own native dispatch field.
                field(connection,"packetListener").set(connection,listener);updates=field(listener,"tickCount");
                listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
                listener.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
                var teleport=connection.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance).map(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::cast).reduce((a,b)->b).orElseThrow();
                listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));
                require(listener.hasClientLoaded()&&field(listener,"awaitingPositionFromClient").get(listener)==null,"fresh native listener loaded and real teleport acknowledged");
                game.server().getConnection().getConnections().add(connection);
            }catch(ReflectiveOperationException|RuntimeException|Error error){game.server().getConnection().getConnections().remove(connection);player.connection=previous;throw new IllegalStateException("scoped native connection scheduler",error);}
        }
        void acknowledge(){listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());try{if(field(listener,"awaitingPositionFromClient").get(listener)!=null){var teleport=Objects.requireNonNull(connection.latestPosition,"actual emitted teleport packet survives measurement windows");listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));}}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}require(ready(),"native player loaded and latest actual teleport acknowledged");}
        boolean ready(){try{return listener.hasClientLoaded()&&!player.isChangingDimension()&&field(listener,"awaitingPositionFromClient").get(listener)==null;}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        int updates(){try{return updates.getInt(listener);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
        @Override public void close(){try{game.server().getConnection().getConnections().remove(connection);}finally{player.connection=previous;}}
    }
    private static final class Snapshot {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final net.minecraft.nbt.CompoundTag food;
        final boolean ground,sprint,shift,swimming,using;final InteractionHand useHand;final int useRemaining;final ItemStack useStack;final Vec3 knownMovement;final net.minecraft.world.entity.Pose pose;final Map<java.lang.reflect.Field,Object> tickers=new LinkedHashMap<>();
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> combatTracker=new LinkedHashMap<>(),foodFields=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> profile=new LinkedHashMap<>();
        Snapshot(GameController game,ServerPlayer player){this.player=player;for(var holder:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var attr=player.getAttribute(holder);if(attr!=null)attributes.getInstance(holder).replaceFrom(attr);}try{var map=(Map<?,?>)field(player,"lastEquipmentItems").get(player);for(var entry:map.entrySet())equipment.put((EquipmentSlot)entry.getKey(),((ItemStack)entry.getValue()).copy());}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(player,objective);if(score!=null)scores.put(objective,score.value());}state=new GameChecks.WandThirdPlayerState(player,game.items().mana());using=player.isUsingItem();useHand=using?player.getUsedItemHand():InteractionHand.MAIN_HAND;useRemaining=player.getUseItemRemainingTicks();useStack=player.getItemInHand(useHand);ground=player.onGround();sprint=player.isSprinting();shift=player.isShiftKeyDown();swimming=player.isSwimming();knownMovement=player.getKnownMovement();pose=player.getPose();
            try{for(var name:List.of("attackStrengthTicker","itemSwapTicker","xxa","yya","zza","jumping","yHeadRot","yHeadRotO","hurtTime","hurtDuration","lastHurt","lastDamageStamp","lastDamageSource","lastRecordedFoodLevel","lastSentFood","lastFoodSaturationZero","lastSentHealth","lastRecordedHealthAndAbsorption")){var f=field(player,name);tickers.put(f,f.get(player));}}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            try{for(var f:player.getCombatTracker().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);var v=f.get(player.getCombatTracker());if(v instanceof List<?> list)combatTracker.put(f,new ArrayList<>(list));else if(!java.lang.reflect.Modifier.isFinal(f.getModifiers()))combatTracker.put(f,v);}}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess());player.getFoodData().addAdditionalSaveData(output);food=output.buildResult();try{for(var f:player.getFoodData().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);foodFields.put(f,f.get(player.getFoodData()));}}catch(IllegalAccessException e){throw new IllegalStateException(e);}
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,player.getStats().getValue(stat));}
            for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(Items.WOODEN_SWORD,Items.BOW,Items.CARROT_ON_A_STICK,Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS)){var stat=type.get(item);stats.put(stat,player.getStats().getValue(stat));}
            for(var item:List.of(Items.REPEATING_COMMAND_BLOCK,LbcMagic.MYSTICAL_ARTIFACT)){var picked=Stats.ITEM_PICKED_UP.get(item);stats.put(picked,player.getStats().getValue(picked));}for(var type:List.of(EntityTypes.COW,EntityTypes.SKELETON)){var killed=Stats.ENTITY_KILLED.get(type);stats.put(killed,player.getStats().getValue(killed));}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();player.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            try{var p=game.players().get(player.getUUID());for(var f:p.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var v=f.get(p);profile.put(f,v instanceof int[] a?a.clone():v instanceof Set<?> set?new HashSet<>(set):v);}}
            catch(IllegalAccessException error){throw new IllegalStateException(error);}
        }
        @SuppressWarnings({"unchecked","rawtypes"}) void restoreAttributes(){try{for(var entry:combatTracker.entrySet())if(entry.getValue() instanceof List values){var target=(List)entry.getKey().get(player.getCombatTracker());target.clear();target.addAll(values);}else entry.getKey().set(player.getCombatTracker(),entry.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}player.getAttributes().assignAllValues(attributes);try{var map=(Map<EquipmentSlot,ItemStack>)field(player,"lastEquipmentItems").get(player);map.clear();equipment.forEach((slot,stack)->map.put(slot,stack.copy()));}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        void restoreUse(){if(using){player.setItemInHand(useHand,useStack);player.startUsingItem(useHand);try{field(player,"useItemRemaining").setInt(player,useRemaining);}catch(IllegalAccessException|NoSuchFieldException e){throw new IllegalStateException(e);}}}
        void restoreProfile(GameController game){try{for(var e:profile.entrySet()){var value=e.getValue();e.getKey().set(game.players().get(player.getUUID()),value instanceof int[] array?array.clone():value instanceof Set<?> set?new HashSet<>(set):value);}}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
        void restore(GameController game){for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(player,objective,true).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(player,objective);stats.forEach((s,v)->player.getStats().setValue(player,s,v));
            advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});state.restore(game);player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setSwimming(swimming);player.setKnownMovement(knownMovement);player.setPose(pose);try{for(var entry:tickers.entrySet())entry.getKey().set(player,entry.getValue());}catch(IllegalAccessException error){throw new IllegalStateException(error);}player.getFoodData().readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,player.registryAccess(),food));try{for(var entry:foodFields.entrySet())entry.getKey().set(player.getFoodData(),entry.getValue());}catch(IllegalAccessException e){throw new IllegalStateException(e);}restoreProfile(game);}
    }
    private static final class Entry implements AutoCloseable {
        final Map<Object,Object> map;final Object key,previous;final boolean existed;
        @SuppressWarnings("unchecked") Entry(Object service,String name,Object key){this.key=key;try{map=(Map<Object,Object>)field(service,name).get(service);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}existed=map.containsKey(key);previous=map.remove(key);}
        @Override public void close(){map.remove(key);if(existed)map.put(key,previous);}
    }

    private static final class Fixture implements AutoCloseable {
        final GameController game;final ServerPlayer owner,enemy;final Snapshot saved,enemySaved;
        final ElementalGrimoireChecks.Fixture base;final SauvojenMeditationChecks.Attribution attribution;
        final LbcIncomingArtifacts service;final net.minecraft.server.level.ServerLevel level;final Vec3 at;
        final Set<Entity> created=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> blocks=new LinkedHashMap<>();
        ScheduledClient client,otherClient;SandWitness cow;int stage,start,clock,instantStart;
        long beforeCriteria;float beforeHealth;boolean closed,sawPassive;String epoch;
        Fixture(GameController game){
            this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));enemy=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&enemy!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(enemy),"two native active participants");
            saved=new Snapshot(game,owner);enemySaved=new Snapshot(game,enemy);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,enemy));
            base=new ElementalGrimoireChecks.Fixture(game);service=game.items().incomingArtifacts();require(service.count()==0,"exclusive incoming service");
            base.initialize();level=owner.level();at=owner.position();epoch=game.items().epoch();
            for(var p:List.of(owner,enemy)){
                p.stopUsingItem();p.removeAllEffects();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(1000);p.setHealth(500);p.setAbsorptionAmount(0);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                p.getAbilities().invulnerable=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setOnGround(true);
                for(var tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"))p.entityTags().remove(tag);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;profile.lbcChallenge=0;Arrays.fill(profile.trinkets,0);game.items().mana().baseRegen(p,0);
                game.items().magic().inventoryChanged(p);
            }
            enemy.teleportTo(level,at.x,at.y,at.z+3,Set.of(),180,0,true);
            client=new ScheduledClient(game,owner);otherClient=new ScheduledClient(game,enemy);
            for(var pos:BlockPos.betweenClosed(owner.blockPosition().offset(-8,-9,-8),owner.blockPosition().offset(8,8,8)))blocks.put(pos.immutable(),level.getBlockState(pos));
            try{payloads();basic();prepareShield();}catch(Throwable error){try{close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}
        }
        ItemStack loot(String name){var got=game.lbc().rollTable(level,at,owner,"lbc:ported/"+name);require(got.size()==1,"single isolated source leaf "+name);return got.getFirst();}
        void put(boolean old,int type,int slot){owner.getInventory().setItem(slot,loot((old?"legacy_":"")+"incoming_artifact_"+type));}
        void payloads(){
            for(boolean old:new boolean[]{false,true})for(int type=1;type<=6;type++){
                var stack=loot((old?"legacy_":"")+"incoming_artifact_"+type);var data=LbcFartus.data(stack);
                require(stack.is(old?Items.REPEATING_COMMAND_BLOCK:LbcMagic.MYSTICAL_ARTIFACT)&&stack.getCount()==1&&data.getIntOr(old?"mystical_old":"mystical",0)==4&&data.getIntOr("type",0)==type,"exact vanilla family4/type flags");
                require(stack.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:mysticalartifact"))&&stack.get(DataComponents.LORE).lines().size()==(old?1:2),"exact source model/lore shape");
                require(data.getIntOr("transformable",0)==(old?0:1)&&data.getIntOr("accessory",0)==(old?0:1),"source current/old acquisition flags");
            }
            for(boolean old:new boolean[]{false,true}){var stack=loot((old?"legacy_":"")+"sand_amulet");require(stack.is(old?Items.REPEATING_COMMAND_BLOCK:LbcAccessoryItems.Kind.SAND_AMULET.item)&&LbcFartus.data(stack).getIntOr(old?"gaara_old":"gaara",0)==1&&stack.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("lbc:gaara")),"exact current/old amulet payload");}
        }
        void reset(){owner.stopUsingItem();owner.getInventory().clearContent();owner.removeAllEffects();owner.setHealth(500);owner.setAbsorptionAmount(0);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.hurtTime=0;owner.setDeltaMovement(Vec3.ZERO);}
        void environmental(float amount){owner.hurtServer(level,owner.damageSources().generic(),amount);}
        void effect(Holder<MobEffect> type,int amplifier,int ticks){var effect=owner.getEffect(type);require(effect!=null&&effect.getAmplifier()==amplifier&&effect.getDuration()==ticks,"native effect "+type+" amp="+amplifier+" ticks="+ticks+" actual="+effect);}
        void basic(){
            reset();put(false,1,0);float health=owner.getHealth();long count=service.criterionCount();environmental(1);
            require(service.criterionCount()==count+1&&owner.getHealth()<health&&!owner.hasEffect(MobEffects.RESISTANCE),"real accepted environmental criterion excludes attacker shield");
            reset();put(false,5,40);environmental(1);require(!owner.hasEffect(MobEffects.STRENGTH),"offhand-only current does not open container gate");
            put(false,5,0);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;environmental(1);effect(MobEffects.STRENGTH,0,100);require(!owner.hasEffect(MobEffects.SPEED),"first ordered speed helper grants only Strength");
            owner.setInvulnerableTime(0);owner.damageCooldownTime=0;environmental(1);effect(MobEffects.SPEED,2,100);require(!owner.hasEffect(MobEffects.RESISTANCE),"second event Speed arrives after Resistance test");
            owner.setInvulnerableTime(0);owner.damageCooldownTime=0;environmental(1);effect(MobEffects.RESISTANCE,0,100);require(!owner.hasEffect(MobEffects.FIRE_RESISTANCE),"third event Resistance arrives after FireResistance tests");
            owner.setInvulnerableTime(0);owner.damageCooldownTime=0;environmental(1);effect(MobEffects.FIRE_RESISTANCE,1,200);require(!owner.hasEffect(MobEffects.REGENERATION),"fourth event FireResistance arrives after Regeneration test");
            owner.setInvulnerableTime(0);owner.damageCooldownTime=0;environmental(1);effect(MobEffects.REGENERATION,1,200);
            reset();for(int type:new int[]{1,3,4,5,6})put(true,type,type);environmental(1);
            effect(MobEffects.RESISTANCE,3,20);effect(MobEffects.SPEED,1,200);effect(MobEffects.SLOWNESS,2,200);effect(MobEffects.STRENGTH,1,200);effect(MobEffects.WEAKNESS,0,200);
            reset();put(false,5,0);owner.entityTags().add("nomagic_active");try{environmental(1);effect(MobEffects.STRENGTH,0,100);}finally{owner.entityTags().remove("nomagic_active");}
            reset();put(false,2,0);environmental(1);var tnts=new ArrayList<net.minecraft.world.entity.item.PrimedTnt>();for(var e:level.getAllEntities())if(LbcIncomingArtifacts.managed(e)&&e instanceof net.minecraft.world.entity.item.PrimedTnt tnt)tnts.add(tnt);
            require(tnts.size()==1&&tnts.getFirst().getClass()==net.minecraft.world.entity.item.PrimedTnt.class&&tnts.getFirst().getFuse()==0&&tnts.getFirst().getOwner()==owner,"real default-power native TNT with source immediate fuse and owner");
            require(service.canAffect(tnts.getFirst(),owner),"TNT preserves native self-source admission; item shield determines damage");require(!owner.hasEffect(MobEffects.RESISTANCE),"item blast protection does not grant general Resistance");effect(MobEffects.SLOW_FALLING,0,100);observerChild(tnts.getFirst());service.forget(owner);
            reset();put(false,4,0);environmental(1);effect(MobEffects.WEAKNESS,9,60);require(enemy.hasEffect(MobEffects.WEAKNESS)&&enemy.getEffect(MobEffects.WEAKNESS).getAmplifier()==9,"source weakness affects independently eligible nearby player");enemy.removeAllEffects();
            reset();owner.getInventory().setItem(0,loot("legacy_sand_amulet"));enemy.setInvulnerableTime(0);enemy.damageCooldownTime=0;
            enemy.hurtServer(level,new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK),owner),1);
            var oldSand=new ArrayList<net.minecraft.world.entity.item.FallingBlockEntity>();for(var e:level.getAllEntities())if(LbcIncomingArtifacts.managed(e)&&e instanceof net.minecraft.world.entity.item.FallingBlockEntity falling)oldSand.add(falling);
            require(oldSand.size()==16&&oldSand.stream().allMatch(f->f.getDeltaMovement().lengthSqr()==0&&!f.dropItem&&service.owner(f)==owner),"actual accepted old amulet event creates16native zero-motion sand blocks");service.forget(owner);
            reset();put(false,6,0);environmental(1);var wolves=new ArrayList<net.minecraft.world.entity.animal.wolf.Wolf>();for(var e:level.getAllEntities())if(LbcIncomingArtifacts.managed(e)&&e instanceof net.minecraft.world.entity.animal.wolf.Wolf wolf)wolves.add(wolf);
            require(wolves.size()==1&&wolves.getFirst().getOwner()==owner&&service.owner(wolves.getFirst())==owner&&LbcIndirectKills.playerSource(wolves.getFirst())==owner,"real native wolf owner and causal attribution");
            var wolf=wolves.getFirst();require(wolf.getAttributeValue(Attributes.FOLLOW_RANGE)==200&&wolf.getAttributeValue(Attributes.MOVEMENT_SPEED)==.5&&wolf.getAttributeValue(Attributes.ATTACK_DAMAGE)==5,"exact source wolf base attributes");
            require(!service.canAffect(wolf,owner),"native wolf cannot harm its owner");enemy.entityTags().add("strict_map_object");try{wolf.setTarget(enemy);require(wolf.getTarget()==null,"native wolf target callback protects technical role");}finally{enemy.entityTags().remove("strict_map_object");}service.forget(owner);require(wolf.isRemoved(),"source wolf owner cleanup");
        }
        void observerChild(net.minecraft.world.entity.item.PrimedTnt parent){
            var child=new net.minecraft.world.entity.item.PrimedTnt(level,parent.getX(),parent.getY(),parent.getZ(),owner);child.setFuse(80);child.setDeltaMovement(Vec3.ZERO);
            service.inheritChild(parent,child);require(level.addFreshEntity(child)&&service.owner(child)==owner&&game.items().currentEpoch(child),"native TNT child inherits actual current-epoch incoming author");
            var scoped=new Snapshot(game,owner);int protection=game.config().tntProtection;
            try{
                game.spectators().enter(owner);owner.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                require(game.isObserver(owner)&&owner.isSpectator(),"author becomes actual observer with native spectator gamemode");
                game.config().tntProtection=0;enemy.removeAllEffects();enemy.setAbsorptionAmount(0);enemy.setHealth(500);enemy.setDeltaMovement(Vec3.ZERO);enemy.setInvulnerableTime(0);enemy.damageCooldownTime=0;
                var source=level.damageSources().explosion(child,owner);require(game.items().allowDamage(enemy,source),"observer author does not cancel already-produced managed TNT child");
                float hp=enemy.getHealth();require(enemy.hurtServer(level,source,8),"actual native observer-authored child hurt accepted");float lost=hp-enemy.getHealth();var impulse=enemy.getDeltaMovement();require(lost>0,"observer child does positive native explosion damage");
                var control=new net.minecraft.world.entity.item.PrimedTnt(level,child.getX(),child.getY(),child.getZ(),null);
                var cause=EntityTypes.COW.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);require(cause!=null,"independent native damage cause");cause.setPos(owner.position());
                var nativeControl=new net.minecraft.world.damagesource.DamageSource(source.typeHolder(),control,cause);
                enemy.setHealth(500);enemy.setDeltaMovement(Vec3.ZERO);enemy.setInvulnerableTime(0);enemy.damageCooldownTime=0;
                require(enemy.hurtServer(level,nativeControl,8),"independent native same-type/TNT damage accepted");near(500-enemy.getHealth(),lost,"managed observer child damage equals native control");
                require(enemy.getDeltaMovement().distanceToSqr(impulse)<1e-8,"native child impulse equals independent same-position control");
                enemy.entityTags().add("sin_greed_active");try{require(!game.items().allowDamage(enemy,source),"observer-author exception retains Greed target protection");}finally{enemy.entityTags().remove("sin_greed_active");}
                var board=game.server().getScoreboard();require(board.getPlayerTeam("sw2_incoming_child")==null,"exclusive observer-child ally control team");var team=board.addPlayerTeam("sw2_incoming_child");var ownerTeam=owner.getTeam();var enemyTeam=enemy.getTeam();
                try{if(ownerTeam!=null)board.removePlayerFromTeam(owner.getScoreboardName(),ownerTeam);if(enemyTeam!=null)board.removePlayerFromTeam(enemy.getScoreboardName(),enemyTeam);board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(enemy.getScoreboardName(),team);require(!game.items().allowDamage(enemy,source),"observer-author exception retains default other-allied-player protection");}
                finally{board.removePlayerTeam(team);if(ownerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);if(enemyTeam!=null)board.addPlayerToTeam(enemy.getScoreboardName(),enemyTeam);}
                control.discard();cause.discard();
            }finally{
                game.config().tntProtection=protection;game.spectators().remove(owner);scoped.restore(game);scoped.restoreAttributes();scoped.restoreUse();enemy.removeAllEffects();enemy.setHealth(500);enemy.setDeltaMovement(Vec3.ZERO);enemy.setInvulnerableTime(0);enemy.damageCooldownTime=0;
            }
        }
        void prepareShield(){reset();put(false,1,1);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHIELD));owner.setYRot(0);owner.setXRot(0);owner.startUsingItem(InteractionHand.MAIN_HAND);clock=client.updates();start=game.server().getTickCount();stage=0;}
        boolean step(){
            require(game.playing()&&game.canUseGameplayItems(owner)&&epoch.equals(game.items().epoch()),"scoped native fixture active epoch");client.acknowledge();otherClient.acknowledge();
            require(game.server().getTickCount()-start<360,"bounded native scheduler progress");
            switch(stage){
                case 0->{
                    if(client.updates()-clock<7)return false;
                    require(owner.isBlocking(),"real native shield warmup");beforeHealth=owner.getHealth();beforeCriteria=service.criterionCount();owner.setInvulnerableTime(0);owner.damageCooldownTime=0;
                    owner.hurtServer(level,new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK),enemy),4);
                    near(owner.getHealth(),beforeHealth,"fully shield blocked hit loses no health");require(service.criterionCount()==beforeCriteria+1,"fully shield blocked hit reaches real criterion");effect(MobEffects.RESISTANCE,4,40);
                    reset();put(true,2,0);beforeCriteria=service.criterionCount();environmental(1);instantStart=client.updates();beforeHealth=owner.getHealth();effect(MobEffects.INSTANT_DAMAGE,0,1);stage=1;
                }
                case 1->{
                    int elapsed=client.updates()-instantStart;if(elapsed<3)return false;
                    require(service.criterionCount()>=beforeCriteria+2,"scheduled native HarmI reenters actual incoming criterion");require(owner.getHealth()<beforeHealth,"scheduled HarmI causes native damage");
                    require(!owner.hasEffect(MobEffects.INSTANT_DAMAGE),"native cooldown eventually rejects same-or-lower recursive Harm; no blanket proc guard");
                    reset();owner.getInventory().setItem(0,loot("sand_amulet"));clock=client.updates();stage=2;
                }
                case 2->{
                    if(owner.hasEffect(MobEffects.RESISTANCE)){sawPassive=true;require(owner.getEffect(MobEffects.RESISTANCE).getAmplifier()==0,"current-only ResistanceI passive");owner.getInventory().clearContent();clock=client.updates();stage=3;}
                    else require(client.updates()-clock<=22,"source once-per-second passive arrives on native cadence");
                }
                case 3->{
                    if(client.updates()-clock<42)return false;require(sawPassive&&!owner.hasEffect(MobEffects.RESISTANCE),"removing current amulet retains grant until natural40tick expiry");
                    reset();owner.getInventory().setItem(0,loot("legacy_sand_amulet"));clock=client.updates();stage=4;
                }
                case 4->{
                    if(client.updates()-clock<42)return false;require(!owner.hasEffect(MobEffects.RESISTANCE),"old amulet has no inventory passive");
                    reset();enemy.teleportTo(level,at.x+8,at.y,at.z+8,Set.of(),0,0,true);enemy.setDeltaMovement(Vec3.ZERO);
                    var control=new SandWitness(level);control.setPos(at.add(2,0,4));control.noPhysics=true;control.setNoGravity(true);control.setNoAi(true);control.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);control.setHealth(100);require(level.addFreshEntity(control),"independent native falling-block control");created.add(control);
                    try{var source=new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK),owner,owner);boolean allowed=game.items().allowDamage(control,source);boolean accepted=control.hurtServer(level,source,4);SkyWars2.LOGGER.info("[check] incoming-artifacts native sand control pos={} hp={} allowed={} accepted={} hits={}",control.position(),control.getHealth(),allowed,accepted,control.hits);require(allowed&&accepted,"independent native falling_block4 accepted with original owner allowed="+allowed+" accepted="+accepted+" hits="+control.hits);near(control.getHealth(),96,"independent native falling_block4 health control");}finally{game.items().indirectKills().clear(control);control.discard();}
                    cow=new SandWitness(level);cow.setPos(at.add(0,0,4));cow.noPhysics=true;cow.setNoGravity(true);cow.setNoAi(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);cow.setHealth(100);level.addFreshEntity(cow);created.add(cow);
                    service.sand(owner,3);clock=client.updates();stage=5;
                }
                case 5->{
                    var center=cow.blockPosition();if(!level.getBlockState(center.below()).is(net.minecraft.world.level.block.Blocks.CHISELED_RED_SANDSTONE)){require(client.updates()-clock<10,"queued source column writes complete");return false;}
                    require(level.getBlockState(center).is(net.minecraft.world.level.block.Blocks.RED_SAND),"source four-cell red sand column");
                    require(service.count()==1,"single source column; separate enemy is outside10 and cannot add overlapping radius1 shards");
                    SkyWars2.LOGGER.info("[check] incoming-artifacts sand column start owner={} enemy={} witness={} hp={} cooldown={} managed={} hits={}",owner.position(),enemy.position(),cow.position(),cow.getHealth(),cow.damageCooldownTime,service.count(),cow.hits);
                    // A foreign later edit must survive the delayed local clear.
                    level.setBlock(center.above(1),net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK.defaultBlockState(),818);clock=client.updates();stage=6;
                }
                case 6->{
                    if(client.updates()-clock<112)return false;var center=cow.blockPosition();
                    require(level.getBlockState(center.above(1)).is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK),"delayed cleanup preserves foreign block replacement");
                    require(level.getBlockState(center).isAir()&&level.getBlockState(center.above(2)).isAir()&&level.getBlockState(center.above(3)).isAir(),"delayed clear touches only owned local red-sand cells");
                    require(level.getBlockState(center.below()).is(net.minecraft.world.level.block.Blocks.CHISELED_RED_SANDSTONE),"source support remains outside local sand clear");
                    SkyWars2.LOGGER.info("[check] incoming-artifacts sand column end owner={} enemy={} witness={} hp={} cooldown={} greed={} validTarget={} hits={}",owner.position(),enemy.position(),cow.position(),cow.getHealth(),cow.damageCooldownTime,LbcGreed.active(cow),game.items().wands().projectileTarget(owner,cow),cow.hits);
                    require(cow.getHealth()==96,"column shards native4 falling-block damage in radius1 actual="+cow.getHealth()+" pos="+cow.position()+" hits="+cow.hits);
                    reset();service.sand(owner,1);var falls=new ArrayList<net.minecraft.world.entity.item.FallingBlockEntity>();for(var e:level.getAllEntities())if(LbcIncomingArtifacts.managed(e)&&e instanceof net.minecraft.world.entity.item.FallingBlockEntity f)falls.add(f);
                    require(falls.size()==14&&falls.stream().allMatch(f->f.getDeltaMovement().y==-5&&f.getBlockState().is(net.minecraft.world.level.block.Blocks.SAND)&&!f.dropItem&&service.owner(f)==owner),"exact native14 sand formation/motion/drop/owner");
                    service.forget(owner);require(falls.stream().allMatch(Entity::isRemoved)&&service.count()==0,"owner cleanup removes only managed work and actors");return true;
                }
                default->throw new IllegalStateException("fixture stage");
            }
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->service.forget(owner),()->{for(var e:created){if(e instanceof LivingEntity living)game.items().indirectKills().clear(living);e.discard();}},()->blocks.forEach((pos,state)->level.setBlock(pos,state,818)),()->{if(client!=null)client.close();if(otherClient!=null)otherClient.close();},base::close,()->saved.restore(game),()->enemySaved.restore(game),()->{saved.restoreAttributes();enemySaved.restoreAttributes();saved.restoreUse();enemySaved.restoreUse();},attribution::close))try{action.run();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}
            if(failure!=null)throw new IllegalStateException("incoming fixture restoration",failure);
        }
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive incoming fixture");try{running=new Fixture(game);}catch(Throwable error){if(running!=null)running.close();running=null;throw error;}}
    public static void tick(GameController game){if(game==null||running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS incoming-artifacts (14 source operator leaves; actual native environmental/full-shield criterion; ordered current/old effects; native wolf cause; scheduled native Harm reentry/cooldown; current40tick passive/old absence; source local sand cleanup/foreign-edit protection/native shards/14falling actors; scoped restore)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL incoming-artifacts",error);}}
}
