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

import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

/** Native attack/sweep and real scheduler controls; no manual actor/effect/world ticks. */
public final class PendingSwordChecks {
    private PendingSwordChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Pending swords: "+why);}
    private static void near(double actual,double expected,String why){require(Math.abs(actual-expected)<1e-5,why+" actual="+actual+" expected="+expected);}
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
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,player.getStats().getValue(stat));}NativeMeleeChecks.captureStats(player,stats);
            for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:List.of(Items.WOODEN_SWORD,Items.IRON_SWORD,Items.GOLDEN_SWORD,Items.FLINT_AND_STEEL,Items.BOW,Items.CARROT_ON_A_STICK,Items.LEATHER_HELMET,Items.LEATHER_CHESTPLATE,Items.LEATHER_LEGGINGS,Items.LEATHER_BOOTS)){var stat=type.get(item);stats.put(stat,player.getStats().getValue(stat));}
            var picked=Stats.ITEM_PICKED_UP.get(Items.REPEATING_COMMAND_BLOCK);stats.put(picked,player.getStats().getValue(picked));for(var type:List.of(EntityTypes.COW,EntityTypes.SKELETON)){var killed=Stats.ENTITY_KILLED.get(type);stats.put(killed,player.getStats().getValue(killed));}
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

    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner,other;
        final Snapshot savedOwner,savedOther;final SauvojenMeditationChecks.Attribution attribution;
        final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();final List<Cow> cows=new ArrayList<>();
        final net.minecraft.world.scores.PlayerTeam team,ownerTeam,otherTeam;
        final Vec3 origin;ScheduledClient client;int start,phase,fangBaseline;Vec3 stage2,stage3;final List<Vec3> observed=new ArrayList<>();
        Fixture(GameController game){this.game=game;base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(other!=null,"native other participant");savedOwner=new Snapshot(game,owner);savedOther=new Snapshot(game,other);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));base.initialize();origin=owner.position();
            SkyWars2.LOGGER.info("[check] pending-swords baseline owner={} other={} otherUsable={} observer={} alive={} removed={} tags={}",owner.position(),other.position(),game.canUseGameplayItems(other),game.isObserver(other),other.isAlive(),other.isRemoved(),other.entityTags());
            var board=game.server().getScoreboard();ownerTeam=owner.getTeam();otherTeam=other.getTeam();require(board.getPlayerTeam("sw2_pending_check")==null,"exclusive native team fixture");team=board.addPlayerTeam("sw2_pending_check");team.setAllowFriendlyFire(false);board.addPlayerToTeam(owner.getScoreboardName(),team);if(other.getTeam()!=null)board.removePlayerFromTeam(other.getScoreboardName(),other.getTeam());
            for(var p:List.of(owner,other)){p.getInventory().clearContent();p.removeAllEffects();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setInvulnerableTime(0);p.damageCooldownTime=0;p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(1000);var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;profile.lbcChallenge=0;Arrays.fill(profile.trinkets,0);}
            other.teleportTo(other.level(),origin.x+30,origin.y,origin.z,Set.of(),0,0,true);
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(origin.add(-12,-1,-12)),BlockPos.containing(origin.add(12,-1,12)))){var key=pos.immutable();require(owner.level().hasChunkAt(key),"resident native safe teleport platform");blocks.put(key,owner.level().getBlockState(key));owner.level().setBlock(key,Blocks.STONE.defaultBlockState(),2);}
        }
        ItemStack leaf(String id){var items=game.lbc().rollTable(owner.level(),origin,owner,"lbc:ported/"+id);require(items.size()==1,"source singleton "+id);return items.getFirst();}
        void held(String id){owner.setItemInHand(InteractionHand.MAIN_HAND,leaf(id));owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);}
        Cow cow(Vec3 at){var cow=new Cow(EntityTypes.COW,owner.level()){
            @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,DamageSource source,float amount){if(source.getEntity()==owner)observed.add(owner.position());return super.hurtServer(level,source,amount);}
        };cow.setPos(at);cow.setNoAi(true);cow.setNoGravity(true);cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);cow.setHealth(1000);require(owner.level().addFreshEntity(cow),"native sweep target admitted");cows.add(cow);return cow;}
        void charge(){try{field(owner,"attackStrengthTicker").setInt(owner,100);field(owner,"itemSwapTicker").setInt(owner,100);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}owner.setOnGround(true);owner.setSprinting(false);owner.setKnownMovement(Vec3.ZERO);owner.fallDistance=0;}

        void epicVariants(){
            int[] oldFragment={11,7,5,13,3,9,1,2,4,6,14,12,10,16,15,0,8};
            int[] oldRandom={11,7,13,3,9,1,2,4,6,14,12,10,16,15,0,8,5};
            for(String id:List.of("epic_sword","legacy_epic_sword","fragment_sword","legacy_fragment_sword")){
                boolean old=id.startsWith("legacy"),random=!id.contains("fragment");var template=leaf(id);
                require(template.is(old?Items.GOLDEN_SWORD:random?LbcMeleeItems.Kind.EPIC_SWORD.item:LbcMeleeItems.Kind.FRAGMENT_SWORD.item),"source vanilla sword carrier "+id);
                require(template.getMaxDamage()==(old?new ItemStack(Items.GOLDEN_SWORD).getMaxDamage():random?300:600),"source exact durability "+id);
                require(old==(template.get(DataComponents.UNBREAKABLE)!=null),"source old unbreakable presence "+id);
                require(Identifier.parse(random?"lbc:epicsword":"lbc:fragmentsword").equals(template.get(DataComponents.ITEM_MODEL)),"source exact model "+id);
                for(int draw=0;draw<17;draw++){
                    game.items().pendingSwords().forget(owner);game.items().evokerBass().forget(owner);owner.removeAllEffects();owner.setHealth(1000);owner.setAbsorptionAmount(0);owner.teleportTo(owner.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);owner.setDeltaMovement(Vec3.ZERO);
                    var stack=template.copy();if(!random){var flags=LbcFartus.data(stack);flags.putInt("cooltype",draw);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));}owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                    int expected=old?(random?oldRandom[draw]:oldFragment[draw]):random?(draw==16?0:draw+1):draw;
                    var target=cow(origin.add(14,0,0));base.recording.packets.clear();
                    try{var rng=field(owner,"random");var previous=rng.get(owner);int scripted=draw;int[] count={0};if(random)rng.set(owner,java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(17)&&StackWalker.getInstance().walk(frames->frames.anyMatch(frame->frame.getClassName().equals(LbcPendingSwords.class.getName())))){count[0]++;return scripted;}try{return method.invoke(previous,args);}catch(java.lang.reflect.InvocationTargetException error){throw error.getCause();}}));
                        try{require(target.hurtServer(owner.level(),owner.damageSources().playerAttack(owner),1),"actual native accepted Epic input "+id+"/"+draw);if(random)require(count[0]==1,"one source inclusive17way draw");}finally{if(random)rng.set(owner,previous);}
                    }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
                    Holder<MobEffect> kind=switch(expected){case 0->MobEffects.ABSORPTION;case 2->MobEffects.JUMP_BOOST;case 4,11->MobEffects.RESISTANCE;case 5->MobEffects.FIRE_RESISTANCE;case 6->MobEffects.GLOWING;case 7->old?MobEffects.REGENERATION:MobEffects.INSTANT_HEALTH;case 9->MobEffects.INVISIBILITY;case 13->MobEffects.SPEED;case 14->MobEffects.STRENGTH;case 15->MobEffects.CONDUIT_POWER;default->null;};
                    if(kind!=null){var effect=owner.getEffect(kind);require(effect!=null,"full variant self outcome "+id+"/"+draw+" branch"+expected);if(expected==15)require(effect.getDuration()==(old?(random?60:80):200)&&effect.getAmplifier()==(old&&random?2:1),"old cool water distinct from old fragment");if(expected==7)require(effect.getAmplifier()==(old?5:1)&&effect.getDuration()==(old?20:1),"old regen100 versus current native instant2");if(expected==5)require(effect.getDuration()==(old?320:160)&&effect.getAmplifier()==(old?0:1),"flame duration and independent higher layer");}
                    if(expected==1){var arrows=game.items().pendingSwords().arrowSnapshot();require(arrows.size()==8&&arrows.stream().allMatch(a->a.getOwner()==owner),"eight native owned world axis arrows "+id);}
                    if(expected==3)require(game.items().pendingSwords().queuedCasts()==1,"one independent sequence "+id);
                    if(expected==4)require(old?!owner.hasEffect(MobEffects.SLOW_FALLING):owner.hasEffect(MobEffects.SLOW_FALLING),"old explosion omits slow falling");
                    if(expected==13){var flashes=base.recording.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).filter(v->v.particle().getType()==net.minecraft.core.particles.ParticleTypes.FLASH).toList();require(flashes.size()==1&&flashes.getFirst().count()==100&&flashes.getFirst().overrideLimiter()==old,"source white/cyan FLASH packet100 and FORCE difference");}
                    game.items().indirectKills().clear(target);target.discard();cows.remove(target);blocks.forEach((pos,state)->owner.level().setBlock(pos,Blocks.STONE.defaultBlockState(),2));
                }
            }
            game.items().pendingSwords().forget(owner);game.items().evokerBass().forget(owner);owner.removeAllEffects();
        }

        void feedbackAudience(){
            try(var audience=new ScheduledClient(game,other)){
                owner.removeAllEffects();owner.teleportTo(owner.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);
                var stack=leaf("fragment_sword");var data=LbcFartus.data(stack);data.putInt("cooltype",6);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                var c=cow(origin.add(14,0,0));
                for(double distance:new double[]{74.9,75.1}){other.teleportTo(other.level(),origin.x+distance,origin.y,origin.z,Set.of(),0,0,true);audience.acknowledge();audience.connection.packets.clear();c.setInvulnerableTime(0);c.damageCooldownTime=0;require(c.hurtServer(owner.level(),owner.damageSources().playerAttack(owner),1),"actual flash accepted native activation");var nativePlayers=owner.level().getPlayers(v->v==other&&v.distanceToSqr(owner)>=.0001&&v.distanceToSqr(owner)<=5625,Integer.MAX_VALUE);require(nativePlayers.contains(other)==(distance<=75),"native world-limited player-selector distance75 control "+distance);var markers=audience.connection.packets.stream().filter(ClientboundLevelParticlesPacket.class::isInstance).map(ClientboundLevelParticlesPacket.class::cast).filter(v->v.particle().getType()==net.minecraft.core.particles.ParticleTypes.BLOCK_MARKER).toList();SkyWars2.LOGGER.info("[check] pending-swords flash audience configured={} origin={} owner={} viewer={} distance2={} markers={} nativePlayers={} viewerUsable={} observer={} gamemode={} tags={} greed={} allied={} role={} target={} spatial={} resident={} sameLevel={} ready={} held={} ownerGlow={} ownerInputBlocked={} ownerStopped={} beacon={} nomagicViewers={}",distance,origin,owner.position(),other.position(),owner.distanceToSqr(other),markers.size(),nativePlayers.size(),game.canUseGameplayItems(other),game.isObserver(other),other.gameMode.getGameModeForPlayer(),other.entityTags(),LbcGreed.active(other),owner.isAlliedTo(other),game.items().wands().validRoleTarget(other,false),game.items().wands().projectileTarget(owner,other),owner.level().getEntitiesOfClass(ServerPlayer.class,owner.getBoundingBox().inflate(75),v->v==other).size(),owner.level().hasChunkAt(BlockPos.containing(other.position())),owner.level()==other.level(),audience.ready(),LbcFartus.data(owner.getMainHandItem()),owner.hasEffect(MobEffects.GLOWING),game.items().actionsBlocked(owner),game.items().wands().inputTimeStopped(owner),game.items().magic().beaconSuppressed(owner.position()),owner.level().players().stream().filter(v->!v.isSpectator()&&v.entityTags().contains("nomagic_active")&&v.distanceToSqr(owner)<=400).map(v->v.getScoreboardName()+":"+v.position()).toList());require(markers.size()==(distance<=75?1:0),"approved finite75block red marker audience "+distance+" actual="+markers.size());if(distance<=75){var marker=markers.getFirst();require(marker.count()==600&&marker.overrideLimiter()&&marker.particle() instanceof net.minecraft.core.particles.BlockParticleOption block&&block.getState().is(Blocks.RED_MUSHROOM_BLOCK),"source redblock marker600 FORCE preserved");}}
                var old=leaf("legacy_fragment_sword");var flags=LbcFartus.data(old);flags.putInt("cooltype",11);old.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));owner.setItemInHand(InteractionHand.MAIN_HAND,old);
                for(double distance:new double[]{14,16,76}){other.teleportTo(other.level(),origin.x+distance,origin.y,origin.z,Set.of(),0,0,true);audience.acknowledge();audience.connection.packets.clear();c.setInvulnerableTime(0);c.damageCooldownTime=0;require(c.hurtServer(owner.level(),owner.damageSources().playerAttack(owner),1),"actual old screamer activation");var sounds=audience.connection.packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).filter(v->v.getSound().value().location().toString().equals("minecraft:entity.ghast.hurt")).toList();require(sounds.size()==(distance<=15?1:0),"source15block old screamer recipients bounded below approved75 "+distance);if(distance<=15)require(sounds.getFirst().getVolume()==1000000&&sounds.getFirst().getPitch()==0,"original million-volume screamer and pitch0 preserved");}
                c.discard();cows.remove(c);other.teleportTo(other.level(),origin.x+30,origin.y,origin.z,Set.of(),0,0,true);audience.acknowledge();owner.removeAllEffects();
            }
        }
        void nativeSweep(){held("ender_sword");var primary=cow(origin.add(0,0,2));var swept=cow(origin.add(.6,0,2));var unrelated=cow(origin.add(7,0,0));unrelated.hurtTime=10;var unrelatedAt=unrelated.position();charge();base.recording.packets.clear();owner.attack(primary);
            require(primary.getHealth()<1000&&swept.getHealth()<1000,"actual primary and native sweep accepted");require(observed.size()>=2&&observed.stream().allMatch(origin::equals),"attacker stays at original pose across all native accepted callbacks");require(unrelated.position().equals(unrelatedAt),"recent hurt nearby nonvictim excluded");
            require(base.recording.packets.stream().filter(net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.class::isInstance).count()==1,"exactly one owner teleport after whole native attack");for(var target:List.of(primary,swept)){require(Math.floor(target.getX())>=Math.floor(origin.x-10)&&Math.floor(target.getX())<Math.ceil(origin.x+10)&&Math.floor(target.getZ())>=Math.floor(origin.z-10)&&Math.floor(target.getZ())<Math.ceil(origin.z+10),"actual accepted sweep target uses radius10 native sampled columns origin="+origin+" destination="+target.position());near(target.getX(),Math.floor(target.getX())+.5,"native spread X floor then center");near(target.getZ(),Math.floor(target.getZ())+.5,"native spread Z floor then center");require(target.getY()==origin.y,"native integer standing Y without extra half block");}
            for(var c:cows){game.items().indirectKills().clear(c);c.discard();}cows.clear();owner.teleportTo(owner.level(),origin.x,origin.y,origin.z,Set.of(),0,0,true);
        }
        void statuses(){held("antimagic_sword");
            var board=game.server().getScoreboard();board.addPlayerToTeam(other.getScoreboardName(),team);var noMagicBefore=base.mana.noMagic(other);var manaBefore=base.mana.get(other);other.setInvulnerableTime(0);other.damageCooldownTime=0;other.hurtServer(owner.level(),owner.damageSources().playerAttack(owner),1);require(base.mana.noMagic(other)==noMagicBefore&&base.mana.get(other)==manaBefore,"default allied player protected from custom noMagic input");board.removePlayerFromTeam(other.getScoreboardName(),team);
other.teleportTo(other.level(),origin.x+14,origin.y,origin.z,Set.of(),0,0,true);base.mana.set(other,100);other.setInvulnerableTime(0);other.damageCooldownTime=0;var before=base.mana.score(other,"nomagic").get();var indirect=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC),owner);
            require(other.hurtServer(owner.level(),indirect,1),"far owner-attributed magic damage is accepted natively");
            require(base.mana.score(other,"nomagic").get()==before&&base.mana.get(other)==100,"indirect magic cannot trigger a held melee sword");
            other.setInvulnerableTime(0);other.damageCooldownTime=0;
            var damage=owner.damageSources().playerAttack(owner);
            require(other.hurtServer(owner.level(),damage,1),"actual far native melee source accepted target");
            require(base.mana.score(other,"nomagic").get()==before+15&&base.mana.get(other)==0,"shared additive noMagic actual target beyond removed radius7");
            held("enhanced_flint");var c=cow(origin.add(14,0,0));require(c.hurtServer(owner.level(),damage,1),"actual far omen victim");require(base.mana.score(c,"effect_kiuaskivi").get()==400,"shared exact20s owned curse beyond removed radius7");c.discard();cows.clear();
        }
        void scheduled(){owner.removeAllEffects();held("epic_sword");var stack=leaf("epic_sword");var data=LbcFartus.data(stack);data.remove("cool");data.putInt("fragmentsword",1);data.putInt("cooltype",3);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.setItemInHand(InteractionHand.MAIN_HAND,stack);var c=cow(origin.add(0,0,2));for(int n=0;n<2;n++){c.setInvulnerableTime(0);c.damageCooldownTime=0;require(c.hurtServer(owner.level(),owner.damageSources().playerAttack(owner),1),"independent accepted fang activation");}c.discard();cows.clear();require(game.items().pendingSwords().queuedCasts()==2,"two independent three-stage sequences");fangBaseline=game.items().evokerBass().snapshot().size();require(fangBaseline>=2,"both immediate first native fangs exist");owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);client=new ScheduledClient(game,owner);client.acknowledge();start=game.server().getTickCount();phase=0;
        }
        boolean step(){client.acknowledge();int elapsed=game.server().getTickCount()-start;if(phase==0&&elapsed<5){require(game.items().evokerBass().snapshot().size()==fangBaseline,"no fang stage before5native ticks");owner.teleportTo(owner.level(),origin.x+2,origin.y,origin.z,Set.of(),90,0,true);stage2=owner.position().add(-3,.5,0);}
            if(phase==0&&elapsed>=5){var fangs=game.items().evokerBass().snapshot();require(fangs.size()==fangBaseline+2,"independent second fangs at5ticks");require(fangs.stream().filter(f->f.position().distanceToSqr(stage2)<1e-6).count()==2,"second stages read current moved owner pose");phase=1;owner.teleportTo(owner.level(),origin.x-2,origin.y,origin.z,Set.of(),180,0,true);stage3=owner.position().add(0,.5,-4.5);}
            if(phase==1&&elapsed>=10){var fangs=game.items().evokerBass().snapshot();require(fangs.size()==fangBaseline+4,"independent third fangs at10ticks");require(fangs.stream().filter(f->f.position().distanceToSqr(stage3)<1e-6).count()==2,"third stages read latest pose");require(game.items().pendingSwords().queuedCasts()==0,"finished independent queues removed");require(client.updates()>0,"real native connection scheduling drove player");return true;}return false;}
        @Override public void close(){try{if(client!=null)client.close();game.items().pendingSwords().forget(owner);game.items().evokerBass().forget(owner);for(var c:cows){game.items().indirectKills().clear(c);c.discard();}blocks.forEach((p,s)->owner.level().setBlock(p,s,2));var board=game.server().getScoreboard();board.removePlayerTeam(team);if(ownerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);if(otherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),otherTeam);savedOwner.restoreAttributes();savedOther.restoreAttributes();savedOwner.restore(game);savedOther.restore(game);}finally{try{base.close();}finally{attribution.close();}}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);try{running.epicVariants();running.feedbackAudience();running.nativeSweep();running.statuses();running.scheduled();}catch(Throwable error){try{running.close();}finally{running=null;}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS pending-swords (native attack/sweep accepted victims, deferred single owner teleport, far shared status targets, independently scheduled moving-pose fang stages)");}}catch(Throwable error){try{running.close();}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL pending-swords",error);}}
}
