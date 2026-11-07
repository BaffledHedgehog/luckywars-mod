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

/** Real native registered clock and original current Belt payload, inventory and healing. */
public final class RegenerationBeltChecks {
    private RegenerationBeltChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Regeneration Belt: "+why);}
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
    static final class ScheduledClient implements AutoCloseable {
        List<net.minecraft.network.protocol.Packet<?>> packets(){return connection.packets;}
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
        String diagnostics(){
            var state=new LinkedHashMap<String,Object>();state.put("player",player.getScoreboardName());state.put("level",player.level().dimension().identifier());state.put("position",player.position());state.put("loaded",listener.hasClientLoaded());state.put("changingDimension",player.isChangingDimension());state.put("samePlayerListener",listener==player.connection);state.put("currentPlayerListener",player.connection==null?"null":player.connection.getClass().getName()+"@"+Integer.toHexString(System.identityHashCode(player.connection)));state.put("scopedListener",listener.getClass().getName()+"@"+Integer.toHexString(System.identityHashCode(listener)));state.put("latestTeleport",String.valueOf(connection.latestPosition));
            for(var name:List.of("awaitingPositionFromClient","awaitingTeleport","awaitingTeleportTime","tickCount"))try{state.put(name,field(listener,name).get(listener));}catch(ReflectiveOperationException error){state.put(name,"unavailable:"+error.getClass().getSimpleName());}
            return state.toString();
        }
        void acknowledge(){listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());try{if(field(listener,"awaitingPositionFromClient").get(listener)!=null){var teleport=Objects.requireNonNull(connection.latestPosition,"actual emitted teleport packet survives measurement windows: "+diagnostics());listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleport.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));}}catch(ReflectiveOperationException error){throw new IllegalStateException("native teleport ACK fields: "+diagnostics(),error);}require(ready(),"native player loaded and latest actual teleport acknowledged: "+diagnostics());}
        boolean ready(){try{return listener.hasClientLoaded()&&!player.isChangingDimension()&&field(listener,"awaitingPositionFromClient").get(listener)==null;}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
        int updates(){try{return updates.getInt(listener);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
        @Override public void close(){try{game.server().getConnection().getConnections().remove(connection);}finally{player.connection=previous;}}
    }
    static final class Snapshot {
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
            var picked=Stats.ITEM_PICKED_UP.get(LbcAccessoryItems.Kind.REGENERATION_BELT.item);stats.put(picked,player.getStats().getValue(picked));for(var type:List.of(EntityTypes.COW,EntityTypes.SKELETON)){var killed=Stats.ENTITY_KILLED.get(type);stats.put(killed,player.getStats().getValue(killed));}
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
        final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
        final GameController game;ServerPlayer owner,other;Snapshot saved,otherSaved;ScheduledClient client,audience;
        net.minecraft.server.level.ServerLevel level;Vec3 at;LbcRegenerationBelt belt;
        net.minecraft.world.entity.Marker marker;int stage,started,when,phaseBefore,ownerPhaseUpdates,otherPhaseUpdates;java.lang.reflect.Field phase;
        float baseline,otherBaseline;boolean closed,regen,ruleCaptured,objectiveAdded,skills,levelups,createdStasis,hadStasisTag;
        net.minecraft.world.scores.Objective y;net.minecraft.advancements.AdvancementHolder task;ItemStack token;
        Fixture(GameController game){this.game=game;}
        ItemStack loot(int count){var result=game.lbc().rollTable(level,at,owner,"lbc:ported/regeneration_belt");require(result.size()==1,"exact narrow original Belt leaf");var stack=result.getFirst();stack.setCount(count);return stack;}
        void initialize(){
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));require(owner!=null&&other!=null&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two actual active participants");
            saved=new Snapshot(game,owner);otherSaved=new Snapshot(game,other);level=owner.level();at=new Vec3(owner.getX(),Math.min(240,level.getMaxY()-40),owner.getZ());belt=game.items().regenerationBelt();
            skills=game.config().lbcSkills;levelups=game.config().lbcLevelups;require(!game.items().legacyEffects().personalStasis().active(owner),"no pre-existing personal stasis in scoped Belt fixture");game.config().lbcSkills=false;game.config().lbcLevelups=true;
            regen=level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.NATURAL_HEALTH_REGENERATION);ruleCaptured=true;level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.NATURAL_HEALTH_REGENERATION,false,game.server());
            try{phase=field(belt,"phase");phaseBefore=phase.getInt(belt);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
            for(var p:List.of(owner,other)){
                p.stopUsingItem();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);p.removeAllEffects();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.getAbilities().flying=false;p.noPhysics=false;p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setKnownMovement(Vec3.ZERO);p.setOnGround(true);
                for(var tag:List.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"))p.entityTags().remove(tag);
                p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);p.setHealth(100);p.setAbsorptionAmount(0);p.getFoodData().setFoodLevel(8);p.getFoodData().setSaturation(0);
                var profile=game.players().get(p.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcSkill=0;profile.lbcChallenge=0;profile.lbcTrinket=0;Arrays.fill(profile.levelups,0);Arrays.fill(profile.trinkets,0);game.items().mana().baseRegen(p,0);game.items().magic().inventoryChanged(p);
            }
            owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);other.teleportTo(level,at.x+8,at.y,at.z,Set.of(),0,0,true);client=new ScheduledClient(game,owner);audience=new ScheduledClient(game,other);
            token=loot(1);require(token.is(LbcAccessoryItems.Kind.REGENERATION_BELT.item)&&Identifier.parse("lbc:regenerationbelt").equals(token.get(DataComponents.ITEM_MODEL))&&token.get(DataComponents.LORE).lines().size()==4,"exact vanilla source carrier/model/four lore rows");
            require(LbcFartus.data(token).getCompound("PublicBukkitValues").orElseThrow().getCompound("lws:regen").orElseThrow().getDoubleOr("lws:value",0)==.04,"original PDC .04HP/tick retained");
            scopes();reforge();task=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/accesories/regeneration_belt"));require(task!=null&&task.value().criteria().size()==1,"actual impossible source task");for(var key:task.value().criteria().keySet())owner.getAdvancements().revoke(task,key);
            var board=game.server().getScoreboard();y=board.getObjective("y");if(y==null){objectiveAdded=true;y=board.addObjective("y",net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,Component.literal("Belt source y"),net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER,false,null);}
            board.getOrCreatePlayerScore(owner,y,true).set(0);
            owner.getInventory().setItem(9,loot(1));other.getInventory().setItem(9,loot(1));begin(-1);
        }
        @SuppressWarnings("unchecked") <T>T weightedAccessory(java.util.function.Supplier<T> action){
            var key=net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:transformables_accessory"));var table=game.server().reloadableRegistries().getLootTable(key);
            try{var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)field(table,"pools").get(table);require(pools.size()==1&&pools.getFirst().entries.size()>1,"actual original sword pool");var sequence=(Optional<Identifier>)field(table,"randomSequence").get(table);require(sequence.isEmpty(),"original sword pool uses normal native level RNG");var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,at).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);var pool=pools.getFirst();require(context.getLuck()==0&&(pool.condition.isEmpty()||pool.condition.get().value().test(context)),"actual normal reforge context/pool admission");int total=0,offset=-1,selected=0;var weights=new ArrayList<Integer>();for(int i=0;i<pool.entries.size();i++){var expanded=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();pool.entries.get(i).expand(context,expanded::add);int sum=0;for(var entry:expanded){int weight=entry.getWeight(context.getLuck());if(weight<=0)continue;if(i==2){if(offset<0)offset=total;selected++;}total=Math.addExact(total,weight);sum=Math.addExact(sum,weight);}weights.add(sum);}require(selected==1&&offset>=0&&weights.get(2)==10,"original Belt entry2 native effective weight10");int bound=total,pick=offset;var random=field(level,"random");var original=random.get(level);int[] calls={0};random.set(level,java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(bound)){calls[0]++;return pick;}try{return method.invoke(original,args);}catch(java.lang.reflect.InvocationTargetException error){throw error.getCause();}}));try{T result=action.get();require(calls[0]==1,"one actual normal pool selection, no retries");SkyWars2.LOGGER.info("[check] Belt native original pool entry2 weights={} total={} selectedOffset={}",weights,bound,pick);return result;}finally{random.set(level,original);}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        void reforge(){
            require(owner.getLuck()==0,"normal reforge source luck0");var donorStack=new ItemStack(Items.STICK);var data=new net.minecraft.nbt.CompoundTag();data.putInt("transformable",1);data.putInt("accessory",1);donorStack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));var donor=new net.minecraft.world.entity.item.ItemEntity(level,at.x,at.y,at.z,donorStack);donor.setNoGravity(true);actors.add(donor);require(level.addFreshEntity(donor),"actual normal accessory reforge input");var outputs=new ArrayList<ItemStack>();weightedAccessory(()->{try{var factory=LbcBolts.class.getDeclaredMethod("reforgeInput",net.minecraft.world.entity.item.ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);factory.setAccessible(true);var work=(LbcStonekivi.Work)factory.invoke(game.items().bolts(),donor,LbcBolts.ReforgeFamily.NORMAL,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step(),"actual existing NORMAL reforge Work boundary");return true;}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}});require(donor.isRemoved()&&donorStack.isEmpty()&&outputs.size()==1&&ItemStack.matches(outputs.getFirst(),token),"existing normal reforge delivers original Belt identity/presentation and consumes1");
        }
        void scopes(){
            // Paper starts with the actual selected mainhand, not fixed hotbar slot0.
            // The prior native match fixture may leave any hotbar slot selected.
            owner.getInventory().setSelectedSlot(0);
            owner.getInventory().clearContent();owner.getInventory().setItem(0,loot(1));owner.getInventory().setItem(40,loot(2));owner.getInventory().setItem(39,loot(3));owner.getInventory().setItem(10,loot(64));near(belt.rate(owner),2.8,"each physical main/offhand/armor/inventory unit exactly once");
            owner.inventoryMenu.getCraftSlots().setItem(0,loot(7));owner.containerMenu.setCarried(loot(9));near(belt.rate(owner),2.8,"cursor/craft not Paper healing inventory");
            owner.getInventory().getItem(10).setCount(5);near(belt.rate(owner),.44,"programmatic physical count read live without cache events");
            owner.getInventory().clearContent();owner.getInventory().setItem(0,loot(2));var same=loot(3);var sameFlags=LbcFartus.data(same);sameFlags.getCompound("PublicBukkitValues").orElseThrow().getCompound("lws:regen").orElseThrow().putDouble("lws:value",999);same.set(DataComponents.CUSTOM_DATA,CustomData.of(sameFlags));owner.getInventory().setItem(40,same);
            double mainValue=LbcFartus.data(owner.getMainHandItem()).getCompound("PublicBukkitValues").orElseThrow().getCompound("lws:regen").orElseThrow().getDoubleOr("lws:value",0);
            require(owner.getInventory().getSelectedSlot()==0&&owner.getMainHandItem()==owner.getInventory().getItem(0)&&mainValue==.04,"independent source mainhand is actual selected slot0 with unchanged copied PDC .04");
            SkyWars2.LOGGER.info("[check] regeneration-belt source group control selected={} mainCount={} mainValue={} offCount={} offValue={}",owner.getInventory().getSelectedSlot(),owner.getMainHandItem().getCount(),mainValue,owner.getOffhandItem().getCount(),sameFlags.getCompound("PublicBukkitValues").orElseThrow().getCompound("lws:regen").orElseThrow().getDoubleOr("lws:value",0));
            near(belt.rate(owner),.2,"same-id first source group value wins, physical amounts aggregate once");
            var first=owner.getInventory().getItem(0);var firstFlags=LbcFartus.data(first);firstFlags.getCompound("PublicBukkitValues").orElseThrow().getCompound("lws:regen").orElseThrow().putBoolean("lws:stackable",false);first.set(DataComponents.CUSTOM_DATA,CustomData.of(firstFlags));near(belt.rate(owner),.04,"same-id first nonstackable group remains a single grant");
            var mismatch=loot(3);var flags=LbcFartus.data(mismatch);flags.getCompound("PublicBukkitValues").orElseThrow().getCompound("lws:regen").orElseThrow().putString("lws:slot","head");mismatch.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));owner.getInventory().clearContent();owner.getInventory().setItem(9,mismatch);near(belt.rate(owner),0,"PDC slot predicate retained");owner.getInventory().setItem(9,ItemStack.EMPTY);owner.getInventory().setItem(39,mismatch);near(belt.rate(owner),.12,"genuine head PDC slot activates in head once");
            owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);
        }
        void begin(int next){stage=next;when=game.server().getTickCount();baseline=owner.getHealth();otherBaseline=other.getHealth();ownerPhaseUpdates=client.updates();otherPhaseUpdates=audience.updates();if(started==0)started=when;}
        void expected(float actual,float base,double amount,int ticks,String why){float expected=base;for(int i=0;i<ticks;i++)expected+= (float)amount;require(Math.abs(actual-expected)<.002,why+" actual="+actual+" expected="+expected+" ticks="+ticks);}
        void removeMarker(){if(marker!=null){game.items().magic().unloaded(marker);marker.discard();marker=null;}}
        void clearCreatedStasis(){if(createdStasis){game.items().legacyEffects().personalStasis().clear(owner);createdStasis=false;if(hadStasisTag)owner.entityTags().add(LbcPersonalStasis.TAG);}}
        String taskDiagnostics(){
            var score=game.server().getScoreboard().getPlayerScoreInfo(owner,y);
            int count=owner.getInventory().clearOrCountMatchingItems(stack->!stack.isEmpty()&&LbcFartus.data(stack).getIntOr("heall",0)==1,true,0,owner.inventoryMenu.getCraftSlots());
            try{return "server="+game.server().getTickCount()+" phase="+phase.getInt(belt)+" y="+(score==null?"missing":score.value())+" ownerY="+owner.getY()+" nativeCount="+count+" physical="+owner.getInventory().getItem(9).getCount()+" craft="+owner.inventoryMenu.getCraftSlots().getItem(0).getCount()+" cursor="+owner.containerMenu.getCarried().getCount()+" stasis="+game.items().legacyEffects().personalStasis().active(owner)+" task="+owner.getAdvancements().getOrStartProgress(task).isDone();}
            catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        boolean step(){
            client.acknowledge();audience.acknowledge();int elapsed=game.server().getTickCount()-when;require(game.server().getTickCount()-started<330,"bounded native Belt scheduler");
            if(stage==-1){
                // Console setup can precede this tick's Belt pulse while the newly registered listener
                // has not ticked yet. Prime after BOTH actual listeners advance, then measure strictly.
                if(client.updates()<=ownerPhaseUpdates||audience.updates()<=otherPhaseUpdates)return false;
                SkyWars2.LOGGER.info("[check] regeneration-belt primed native clock server={} ownerUpdates={} otherUpdates={} ownerHP={} otherHP={}",game.server().getTickCount(),client.updates(),audience.updates(),owner.getHealth(),other.getHealth());
                begin(0);return false;
            }
            require(client.updates()-ownerPhaseUpdates==elapsed&&audience.updates()-otherPhaseUpdates==elapsed,"each measured source pulse follows exactly one real update per listener: elapsed="+elapsed+" owner="+(client.updates()-ownerPhaseUpdates)+" other="+(audience.updates()-otherPhaseUpdates));
            if(elapsed<5)return false;
            switch(stage){
                case 0->{expected(owner.getHealth(),baseline,.04,elapsed,"native per-tick flat healing");expected(other.getHealth(),otherBaseline,.04,elapsed,"second player per-tick healing");require(!owner.hasEffect(MobEffects.REGENERATION),"ordinary healing creates no Regen potion");owner.getInventory().getItem(9).setCount(64);begin(1);}
                case 1->{expected(owner.getHealth(),baseline,2.56,elapsed,"live stack mutation changes very next tick");owner.entityTags().add("nomagic_active");require(game.items().shooters().stun(other,owner,12),"real native input stun");begin(2);}
                case 2->{expected(owner.getHealth(),baseline,2.56,elapsed,"self passive continues under input stun/nomagic tag");game.items().shooters().clearStun(owner);owner.entityTags().remove("nomagic_active");marker=new net.minecraft.world.entity.Marker(EntityTypes.MARKER,level);marker.setPos(owner.position().add(19,19,19));marker.entityTags().add("stopper_magic");require(level.addFreshEntity(marker),"native diagonal AABB stopper marker");game.items().magic().loaded(marker);begin(3);}
                case 3->{near(owner.getHealth(),baseline,"Paper marker AABB20 suppresses outside sphere20");removeMarker();owner.entityTags().add("time_stopped");begin(4);}
                case 4->{near(owner.getHealth(),baseline,"per-player Time Stop pauses passive");expected(other.getHealth(),otherBaseline,.04,elapsed,"paused player does not return from whole task");owner.entityTags().remove("time_stopped");owner.setHealth(100);owner.entityTags().add("sin_greed_active");begin(5);}
                case 5->{expected(owner.getHealth(),baseline,2.56,elapsed,"Greed permits native self Belt healing");owner.entityTags().remove("sin_greed_active");owner.getInventory().getItem(9).setCount(1);owner.setHealth(1000);owner.addEffect(ItemEffectLevels.create(MobEffects.ABSORPTION,100,-2),owner);owner.setHealth(owner.getMaxHealth());var shadow=ShadowAbsorption.state(owner);shadow.stored=0;require(shadow.capacity>0,"real negative-absorption shadow ledger");begin(6);}
                case 6->{near(owner.getHealth(),baseline,"visible max remains clamped while native heal fills shadow HP");require(ShadowAbsorption.state(owner).stored>0,"Belt native healing reaches negative-absorption shadow health");owner.removeAllEffects();owner.setHealth(100);owner.getInventory().clearContent();owner.getInventory().setItem(9,loot(1));var imprinted=game.server().getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION,Identifier.parse("minecraft:imprinted")));require(imprinted!=null,"real native Imprinted level");owner.teleportTo(imprinted,at.x,at.y,at.z,Set.of(),0,0,true);client.acknowledge();begin(7);}
                case 7->{near(owner.getHealth(),baseline,"Imprinted excludes only the transferred player");expected(other.getHealth(),otherBaseline,.04,elapsed,"Imprinted player never aborts second player healing");owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);client.acknowledge();owner.inventoryMenu.getCraftSlots().setItem(0,loot(4));owner.containerMenu.setCarried(loot(5));
                    var stasis=game.items().legacyEffects().personalStasis();require(!stasis.active(owner),"no pre-existing personal stasis to replace");hadStasisTag=owner.entityTags().contains(LbcPersonalStasis.TAG);createdStasis=true;stasis.start(owner,120);require(stasis.active(owner)&&owner.entityTags().contains(LbcPersonalStasis.TAG),"native personal stasis holds canonical source movement ledger");
                    // Shish writes the shared y score from position every tick except under this source anchor.
                    game.server().getScoreboard().getOrCreatePlayerScore(owner,y,true).set(-100000);try{phase.setInt(belt,0);}catch(IllegalAccessException e){throw new IllegalStateException(e);}begin(8);}
                case 8->{var score=game.server().getScoreboard().getPlayerScoreInfo(owner,y);if(score==null||score.value()!=-100000||!game.items().legacyEffects().personalStasis().active(owner))require(false,"native source anchor preserves assigned y predicate: "+taskDiagnostics());if(elapsed<100){require(!owner.getAdvancements().getOrStartProgress(task).isDone(),"no early five-second task pulse");return false;}require(elapsed==100&&owner.getAdvancements().getOrStartProgress(task).isDone(),"actual100tick pulse counts10 full-clear units including craft/cursor with source y score: "+taskDiagnostics());require(owner.getInventory().getItem(9).getCount()==1&&owner.inventoryMenu.getCraftSlots().getItem(0).getCount()==4&&owner.containerMenu.getCarried().getCount()==5,"source clear0 count never consumes items");var profile=game.players().get(owner.getUUID());Arrays.fill(profile.levelups,0);profile.levelups[0]=3;game.lbc().apply(owner,false);near(owner.getAttributeValue(Attributes.MAX_HEALTH),1150,"existing unlocked levelup3 gives exact+.15 multiplied max health");return true;}
                default->throw new IllegalStateException("Belt stage");
            }return false;
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;for(Runnable action:List.<Runnable>of(this::clearCreatedStasis,this::removeMarker,()->actors.forEach(Entity::discard),()->{if(owner!=null&&level!=null&&owner.level()!=level){owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true);if(client!=null)client.acknowledge();}},()->{if(owner!=null){owner.entityTags().remove("time_stopped");owner.entityTags().remove("sin_greed_active");game.items().shooters().clearStun(owner);}},()->{if(phase!=null)try{phase.setInt(belt,phaseBefore);}catch(IllegalAccessException e){throw new IllegalStateException(e);}},()->{if(client!=null)client.close();if(audience!=null)audience.close();},()->{if(ruleCaptured)level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.NATURAL_HEALTH_REGENERATION,regen,game.server());},()->{game.config().lbcSkills=skills;game.config().lbcLevelups=levelups;if(saved!=null)saved.restore(game);if(otherSaved!=null)otherSaved.restore(game);},()->{if(saved!=null){saved.restoreAttributes();saved.restoreUse();}if(otherSaved!=null){otherSaved.restoreAttributes();otherSaved.restoreUse();}},()->{if(objectiveAdded)game.server().getScoreboard().removeObjective(y);}))try{action.run();}catch(Throwable e){if(failure==null)failure=e;else failure.addSuppressed(e);}if(failure!=null)throw new IllegalStateException("Belt fixture restoration",failure);}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive Belt fixture");running=new Fixture(game);try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(game==null||running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS regeneration-belt (exact source Paper PDC physical-unit scopes/live mutation; registered native flat-heal clock/stun/nomagic/AABB beacon/TimeStop/self-Greed/shadowHP; real100tick full-clear source task and existing+.15 levelup3 reward; exact scoped restore)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL regeneration-belt",error);}}
}
