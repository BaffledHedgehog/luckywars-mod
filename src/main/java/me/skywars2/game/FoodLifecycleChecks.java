package me.skywars2.game;
import me.skywars2.SkyWars2;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
/** Bounded real native consume callbacks and actual server ticks; no production clock is advanced by the fixture. */
public final class FoodLifecycleChecks {
    private FoodLifecycleChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Food lifecycle: "+why);}
    private static java.lang.reflect.Field field(Object value,String name){for(Class<?> t=value.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException(name);}
    private static Object read(Object v,String n){try{return field(v,n).get(v);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object v,String n,Object x){try{field(v,n).set(v,x);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static final class Wire extends net.minecraft.network.Connection {
        final List<net.minecraft.network.protocol.Packet<?>> packets=new ArrayList<>();ClientboundPlayerPositionPacket position;
        Wire(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public boolean isConnecting(){return false;}
        @Override public boolean isConnected(){return true;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);if(packet instanceof ClientboundPlayerPositionPacket p)position=p;}
        @Override public void flushChannel(){}
    }
    private static final class Client implements AutoCloseable {
        final GameController game;final ServerPlayer player;final net.minecraft.server.network.ServerGamePacketListenerImpl previous,listener;final Wire wire=new Wire();
        Client(GameController game,ServerPlayer player){this.game=game;this.player=player;previous=player.connection;listener=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),wire,player,new net.minecraft.server.network.CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));set(wire,"packetListener",listener);listener.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());listener.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());ack();game.server().getConnection().getConnections().add(wire);}
        void ack(){listener.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());if(read(listener,"awaitingPositionFromClient")!=null){require(wire.position!=null,"actual teleport packet");listener.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(wire.position.id(),player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot()));}require(listener.hasClientLoaded()&&!player.isChangingDimension()&&read(listener,"awaitingPositionFromClient")==null,"loaded native listener");}
        int updates(){return (Integer)read(listener,"tickCount");}
        public void close(){game.server().getConnection().getConnections().remove(wire);player.connection=previous;previous.resetPosition();}
    }
    /** Capture mutable author state touched by real damage, jumping, equipment and scheduled player ticks. */
    private static final class State {
        final ServerPlayer player;final GameChecks.WandThirdPlayerState state;final boolean ground,sprint,shift;final Vec3 known;final net.minecraft.world.entity.Pose pose;final Entity camera;
        final net.minecraft.world.entity.ai.attributes.AttributeMap attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(EntityTypes.PLAYER));
        final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();
        final Map<java.lang.reflect.Field,Object> values=new LinkedHashMap<>(),combat=new LinkedHashMap<>(),food=new LinkedHashMap<>(),profile=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();final Map<EquipmentSlot,ItemStack> equipment=new EnumMap<>(EquipmentSlot.class);
        State(GameController game,ServerPlayer p){player=p;require(!p.isUsingItem(),"fixture starts with idle hands");state=new GameChecks.WandThirdPlayerState(p,game.items().mana());ground=p.onGround();sprint=p.isSprinting();shift=p.isShiftKeyDown();known=p.getKnownMovement();pose=p.getPose();camera=p.getCamera();
            for(var h:net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements().toList()){var a=p.getAttribute(h);if(a!=null)attributes.getInstance(h).replaceFrom(a);}
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var s=Stats.CUSTOM.get(id);stats.put(s,p.getStats().getValue(s));}for(var type:List.of(Stats.ITEM_USED,Stats.ITEM_BROKEN))for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){var s=type.get(item);stats.put(s,p.getStats().getValue(s));}
            for(var objective:game.server().getScoreboard().getObjectives()){var score=game.server().getScoreboard().getPlayerScoreInfo(p,objective);if(score!=null)scores.put(objective,score.value());}
            for(var a:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();p.getAdvancements().getOrStartProgress(a).getCompletedCriteria().forEach(done::add);advancements.put(a,done);}
            for(var name:List.of("attackStrengthTicker","itemSwapTicker","xxa","yya","zza","jumping","yHeadRot","yHeadRotO","hurtTime","hurtDuration","lastHurt","lastDamageStamp","lastDamageSource","lastRecordedFoodLevel","lastSentFood","lastFoodSaturationZero","lastSentHealth","lastRecordedHealthAndAbsorption"))values.put(field(p,name),read(p,name));
            try{for(var f:p.getCombatTracker().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);var v=f.get(p.getCombatTracker());if(v instanceof List<?> list)combat.put(f,new ArrayList<>(list));else if(!java.lang.reflect.Modifier.isFinal(f.getModifiers()))combat.put(f,v);}for(var f:p.getFoodData().getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);food.put(f,f.get(p.getFoodData()));}for(var f:game.players().get(p.getUUID()).getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){var v=f.get(game.players().get(p.getUUID()));profile.put(f,v instanceof int[] a?a.clone():v instanceof Set<?> s?new HashSet<>(s):v);}for(var entry:((Map<?,?>)read(p,"lastEquipmentItems")).entrySet())equipment.put((EquipmentSlot)entry.getKey(),((ItemStack)entry.getValue()).copy());}catch(IllegalAccessException e){throw new IllegalStateException(e);}
        }
        @SuppressWarnings({"rawtypes","unchecked"}) void restore(GameController game){state.restore(game);player.getAttributes().assignAllValues(attributes);player.setHealth(state.health);player.setAbsorptionAmount(state.absorption);player.setOnGround(ground);player.setSprinting(sprint);player.setShiftKeyDown(shift);player.setKnownMovement(known);player.setPose(pose);stats.forEach((s,v)->player.getStats().setValue(player,s,v));for(var o:game.server().getScoreboard().getObjectives())if(scores.containsKey(o))game.server().getScoreboard().getOrCreatePlayerScore(player,o,true).set(scores.get(o));else game.server().getScoreboard().resetSinglePlayerScore(player,o);advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))player.getAdvancements().award(a,key);else player.getAdvancements().revoke(a,key);});
            try{for(var e:values.entrySet())e.getKey().set(player,e.getValue());for(var e:combat.entrySet())if(e.getValue() instanceof List old){var current=(List)e.getKey().get(player.getCombatTracker());current.clear();current.addAll(old);}else e.getKey().set(player.getCombatTracker(),e.getValue());for(var e:food.entrySet())e.getKey().set(player.getFoodData(),e.getValue());for(var e:profile.entrySet())e.getKey().set(game.players().get(player.getUUID()),e.getValue());var equipmentMap=(Map<EquipmentSlot,ItemStack>)read(player,"lastEquipmentItems");equipmentMap.clear();equipment.forEach((s,i)->equipmentMap.put(s,i.copy()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}player.setCamera(camera);
        }
    }

    private static void restored(State saved){var p=saved.player;var old=saved.state;var a=p.getAbilities();require(a.invulnerable==old.abilitiesInvulnerable&&a.mayfly==old.mayFly&&a.flying==old.flying&&a.instabuild==old.instabuild&&a.mayBuild==old.mayBuild,"teardown restores exact borrowed native abilities including invulnerable="+old.abilitiesInvulnerable);require(p.gameMode()==old.mode&&p.noPhysics==old.noPhysics&&p.isNoGravity()==old.noGravity,"teardown restores native mode/physics");require(p.getInvulnerableTime()==old.invulnerable&&p.damageCooldownTime==old.damageCooldown,"teardown restores original damage immunity timers");require(p.entityTags().equals(old.tags),"teardown restores exact borrowed role/protection tags");}
    private static void equipmentUpdate(LivingEntity entity){try{var method=LivingEntity.class.getDeclaredMethod("detectEquipmentUpdates");method.setAccessible(true);method.invoke(entity);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static final List<Item> SOURCE=List.of(Items.APPLE,Items.GOLDEN_APPLE,Items.ENCHANTED_GOLDEN_APPLE,Items.MELON_SLICE,Items.SWEET_BERRIES,Items.GLOW_BERRIES,Items.CHORUS_FRUIT,Items.CARROT,Items.GOLDEN_CARROT,Items.POTATO,Items.BAKED_POTATO,Items.POISONOUS_POTATO,Items.BEETROOT,Items.DRIED_KELP,Items.BEEF,Items.COOKED_BEEF,Items.PORKCHOP,Items.COOKED_PORKCHOP,Items.MUTTON,Items.COOKED_MUTTON,Items.CHICKEN,Items.COOKED_CHICKEN,Items.RABBIT,Items.COOKED_RABBIT,Items.COD,Items.COOKED_COD,Items.SALMON,Items.COOKED_SALMON,Items.TROPICAL_FISH,Items.PUFFERFISH,Items.BREAD,Items.COOKIE,Items.PUMPKIN_PIE,Items.ROTTEN_FLESH,Items.SPIDER_EYE,Items.MUSHROOM_STEW,Items.BEETROOT_SOUP,Items.RABBIT_STEW,Items.SUSPICIOUS_STEW,Items.MILK_BUCKET,Items.HONEY_BOTTLE);
    private static final Identifier HALLOWEEN=Identifier.parse("lbc:true_advancements/event/eat_halloween_food");
    private static final class Fixture implements AutoCloseable {
        final GameController game;ServerPlayer owner;State saved;Client client;SauvojenMeditationChecks.Attribution attribution;
        net.minecraft.server.level.ServerLevel level;Vec3 origin;boolean closed,prepared,stopOwned;int phase,start,stage;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final Set<Long> forced=new LinkedHashSet<>();
        final Map<String,ItemStack> foods=new LinkedHashMap<>();final Set<Entity> owned=Collections.newSetFromMap(new IdentityHashMap<>());
        net.minecraft.world.entity.item.FallingBlockEntity pumpkin;Vec3 frozenPosition;LbcIndirectKills.Owner captured;
        Fixture(GameController game){this.game=game;}
        void initialize(){
            owner=Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
            require(game.playing()&&game.canUseGameplayItems(owner)&&owner.containerMenu==owner.inventoryMenu&&!owner.isUsingItem(),"active idle native participant");
            require(game.items().foodLifecycle().pendingCount()==0&&game.items().foodLifecycle().actorCount()==0&&!game.items().timeStop().stopped(owner.level()),"exclusive idle lifecycle service/no borrowed TimeStop");
            saved=new State(game,owner);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner));level=owner.level();
            origin=new Vec3(Math.floor(owner.getX())+128.5,Math.min(224,level.getMaxY()-32),Math.floor(owner.getZ())+.5);
            for(var p:game.server().getPlayerList().getPlayers())if(p!=owner&&p.level()==level)require(p.position().distanceToSqr(origin)>48*48,"isolated native fixture");
            var center=BlockPos.containing(origin);
            for(int x=(center.getX()-16)>>4;x<=(center.getX()+16)>>4;x++)for(int z=(center.getZ()-16)>>4;z<=(center.getZ()+16)>>4;z++){if(level.setChunkForced(x,z,true))forced.add(net.minecraft.world.level.ChunkPos.pack(x,z));level.getChunk(x,z);}
            for(var pos:BlockPos.betweenClosed(center.offset(-15,-1,-15),center.offset(15,8,15))){require(level.getBlockState(pos).isAir(),"empty bounded native volume "+pos);terrain.put(pos.immutable(),game.maps().snapshot(level,pos));}
            for(int x=-15;x<=15;x++)for(int z=-15;z<=15;z++)level.setBlock(center.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
            client=new Client(game,owner);prepared=true;reset();
            for(var id:List.of("inf_steak","legacy_luckyblock","halloween/lucky_items","badapple","random_eatable_slab_random_count"))nativeFoods(id);
            for(var flag:List.of("inf","inf_old","halloween_food","halloween_chorus","bad_apple","nonmember"))require(foods.containsKey(flag),"actual packaged native source leaf "+flag);
            synchronous();start=game.server().getTickCount();phase=start;
        }
        @SuppressWarnings("unchecked") void nativeFoods(String id){
            var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:"+id)));
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,origin).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).withOptionalRandomSeed(17).create(Optional.empty());
            var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)read(table,"pools");require(!pools.isEmpty(),"native source table "+id);
            for(var pool:pools)for(var source:pool.entries){var entries=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();source.expand(context,entries::add);for(var entry:entries){var out=new ArrayList<ItemStack>();entry.createItemStack(out::add,context);for(var item:out){
                var data=LbcFartus.data(item);for(var flag:List.of("inf","inf_old","halloween_chorus","bad_apple"))if(data.getIntOr(flag,0)==1)foods.putIfAbsent(flag,item.copy());
                if(data.getIntOr("halloween_food",0)==1&&data.getIntOr("halloween_chorus",0)==0)foods.putIfAbsent("halloween_food",item.copy());
                if(id.equals("random_eatable_slab_random_count")&&item.has(DataComponents.CONSUMABLE)&&!LbcFoodLifecycle.countsAsFood(item))foods.putIfAbsent("nonmember",item.copy());
            }}}
        }
        ItemStack food(String flag){var src=foods.get(flag);require(src!=null&&src.has(DataComponents.CONSUMABLE),"native consumable payload "+flag);var out=src.copy();out.setCount(2);require(ItemStack.isSameItemSameComponents(src,out),"all source components preserved "+flag);return out;}
        int score(String key){return game.items().mana().score(owner,key).get();}
        void reset(){owner.stopUsingItem();owner.removeAllEffects();owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))owner.setItemSlot(slot,ItemStack.EMPTY);
            owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);owner.getAbilities().invulnerable=false;owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);
            for(var tag:List.of("time_stopped","have_stand","spectator","strict_map_object","rcdenyalways","magic_academy","sin_greed_active","nomagic_active","infinite_resistance","infinite_heal","infinite_saturation"))owner.entityTags().remove(tag);
            var profile=game.players().get(owner.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);equipmentUpdate(owner);owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(500);owner.setAbsorptionAmount(0);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.clearFire();owner.getFoodData().setFoodLevel(1);
            owner.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);client.ack();
        }
        ItemStack consume(String key,InteractionHand hand){var item=food(key);owner.setItemInHand(hand,item);owner.startUsingItem(hand);var out=item.finishUsingItem(level,owner);owner.stopUsingItem();require(item.getCount()==1,"exact sole native debit "+key+"/"+hand);return out;}
        void aliases(){
            var random=read(owner,"random");int[] draws={0};
            try{set(owner,"random",java.lang.reflect.Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{
                if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(20)&&StackWalker.getInstance().walk(frames->frames.anyMatch(frame->frame.getClassName().equals(LbcKfcFood.class.getName())))){draws[0]++;return 19;}
                try{return method.invoke(random,args);}catch(java.lang.reflect.InvocationTargetException error){throw error.getCause();}
            }));var item=new ItemStack(LbcKfcFood.KFC_CHICK,2);owner.setItemInHand(InteractionHand.MAIN_HAND,item);item.finishUsingItem(level,owner);require(item.getCount()==1&&draws[0]==1,"native KFC alias one debit and existing single dispatch");}finally{set(owner,"random",random);}
            var before=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getAllEntities().forEach(before::add);
            try{var item=new ItemStack(LbcKfcFood.GOVNO_EATEN,2);owner.setItemInHand(InteractionHand.MAIN_HAND,item);item.finishUsingItem(level,owner);require(item.getCount()==1,"native cookie alias sole debit");}finally{level.getAllEntities().forEach(entity->{if(!before.contains(entity))owned.add(entity);});}
            for(var entity:List.copyOf(owned)){game.items().entityUnloaded(entity);entity.discard();owned.remove(entity);}
        }
        void synchronous(){
            require(SOURCE.size()==41&&new HashSet<>(SOURCE).size()==41&&LbcFoodLifecycle.sourceFoodCount()==41,"exact original41 membership");
            for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM)require(LbcFoodLifecycle.countsAsFood(new ItemStack(item))==(SOURCE.contains(item)||item==LbcKfcFood.KFC_CHICK||item==LbcKfcFood.GOVNO_EATEN),"no inferred generic FOOD membership "+item);
            int count=score("food_count");for(var item:SOURCE){reset();var held=new ItemStack(item,2);owner.setItemInHand(InteractionHand.MAIN_HAND,held);require(held.has(DataComponents.CONSUMABLE),"native source food consumable "+item);held.finishUsingItem(level,owner);require(held.getCount()==1,"one native debit for source food "+item);require(score("food_count")==++count,"single callback food_count "+item);}
            reset();aliases();count+=2;require(score("food_count")==count,"native KFC/cookie aliases count exactly once without duplicate wrapper debit");
            reset();consume("nonmember",InteractionHand.MAIN_HAND);require(score("food_count")==count,"actual source edible slab excluded from original tag");
            reset();owner.getInventory().setItem(9,food("inf"));var ordinary=new ItemStack(Items.APPLE,2);owner.setItemInHand(InteractionHand.MAIN_HAND,ordinary);ordinary.finishUsingItem(level,owner);require(owner.getMainHandItem()==ordinary&&ordinary.getCount()==1&&owner.getInventory().getItem(9).getCount()==2,"inventory-only infinite payload does not activate or debit");
            reset();consume("inf",InteractionHand.MAIN_HAND);require(ItemStack.isSameItemSameComponents(owner.getMainHandItem(),LbcFoodLifecycle.infiniteSteak())&&owner.getMainHandItem().getCount()==1,"immediate exact mainhand infinite replacement");
            reset();owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,7));consume("inf",InteractionHand.OFF_HAND);require(owner.getMainHandItem().is(Items.STICK)&&owner.getMainHandItem().getCount()==7&&ItemStack.isSameItemSameComponents(owner.getOffhandItem(),LbcFoodLifecycle.infiniteSteak())&&owner.getOffhandItem().getCount()==1,"offhand infinite food preserves mainhand");
            reset();consume("bad_apple",InteractionHand.MAIN_HAND);for(var effect:List.of(MobEffects.HASTE,MobEffects.CONDUIT_POWER,MobEffects.WATER_BREATHING)){var active=owner.getEffect(effect);require(active!=null&&active.getAmplifier()==0&&active.getDuration()==6000&&active.isVisible(),"actual bad apple ordinary layer "+effect);}
            reset();var advancement=game.server().getAdvancements().get(HALLOWEEN);require(advancement!=null&&advancement.value().criteria().size()==100,"actual100-criterion task is packaged");for(var key:advancement.value().criteria().keySet())owner.getAdvancements().revoke(advancement,key);game.items().mana().score(owner,"pumpkin_counter").set(0);
            consume("halloween_food",InteractionHand.MAIN_HAND);consume("halloween_food",InteractionHand.MAIN_HAND);require(score("pumpkin_counter")==1,"multiple actual consume callbacks same native tick count once");require(owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().iterator().next().equals("1")&&!owner.getAdvancements().getOrStartProgress(advancement).isDone(),"only real individual criterion1, no invented completion reward");
            reset();consume("inf_old",InteractionHand.MAIN_HAND);require(game.items().foodLifecycle().pendingCount()==1&&owner.entityTags().contains("scheduled_eating"),"real old-stew queue before debit");owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,7));
        }
        void pumpkins(){
            reset();consume("halloween_chorus",InteractionHand.MAIN_HAND);owned.addAll(game.items().foodLifecycle().snapshot());require(owned.size()==20,"exact20 native falling pumpkins");
            for(var entity:owned){require(entity instanceof net.minecraft.world.entity.item.FallingBlockEntity,"native falling entity, no creeper carrier");var actor=(net.minecraft.world.entity.item.FallingBlockEntity)entity;require(actor.getBlockState().is(Blocks.PUMPKIN)&&actor.entityTags().contains("pumpkinfall"),"literal source pumpkin payload");var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());actor.saveWithoutId(out);var nbt=out.buildResult();require(nbt.getBooleanOr("HurtEntities",false)&&nbt.getFloatOr("FallHurtAmount",0)==5&&nbt.getIntOr("FallHurtMax",0)==10,"native capped falling damage5/max10");
                var motion=actor.getDeltaMovement();require(motion.x>=-.5&&motion.x<=.5&&motion.z>=-.5&&motion.z<=.5&&motion.y>=.5&&motion.y<=1.5,"source randomized motion bounds");for(double value:new double[]{motion.x,motion.y,motion.z})require(Math.abs(value*100-Math.rint(value*100))<1e-8,"source hundredth lattice");
                var cause=game.items().foodLifecycle().cause(actor);require(cause!=null&&cause.player().equals(owner.getUUID())&&cause.friendlyFire()&&game.items().foodLifecycle().owner(actor)==owner&&game.items().currentEpoch(actor)&&LbcFoodLifecycle.wild(actor),"captured native WILD owner/current epoch");
                if(pumpkin==null){pumpkin=actor;captured=cause;}else{game.items().entityUnloaded(actor);actor.discard();}
            }
            game.items().foodLifecycle().forget(owner);require(!pumpkin.isRemoved()&&game.items().foodLifecycle().cause(pumpkin).equals(captured),"accepted native consequence survives owner forget");
            owner.teleportTo(level,origin.x,origin.y,origin.z,Set.of(),0,0,true);client.ack();game.items().timeStop().begin(level,100);stopOwned=true;frozenPosition=pumpkin.position();require(game.items().timeStop().frozen(pumpkin),"real TimeStop captures falling pumpkin");phase=game.server().getTickCount();
        }
        boolean step(){client.ack();require(game.server().getTickCount()-start<100,"bounded actual native tick timeout");int elapsed=game.server().getTickCount()-phase;
            if(stage==0){if(elapsed<1)return false;require(game.items().foodLifecycle().pendingCount()==0&&!owner.entityTags().contains("scheduled_eating")&&ItemStack.isSameItemSameComponents(owner.getMainHandItem(),LbcFoodLifecycle.oldInfiniteStew())&&owner.getMainHandItem().getCount()==1,"actual next native tick replaces current mainhand once");
                consume("halloween_food",InteractionHand.MAIN_HAND);consume("halloween_food",InteractionHand.MAIN_HAND);require(score("pumpkin_counter")==2,"next actual tick permits exactly one Halloween progression");
                reset();consume("inf_old",InteractionHand.OFF_HAND);owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,9));phase=game.server().getTickCount();stage++;return false;}
            if(stage==1){if(elapsed<1)return false;require(owner.getMainHandItem().is(Items.STICK)&&owner.getMainHandItem().getCount()==9&&ItemStack.isSameItemSameComponents(owner.getOffhandItem(),LbcFoodLifecycle.oldInfiniteStew())&&owner.getOffhandItem().getCount()==1,"delayed offhand stew preserves changed mainhand");pumpkins();stage++;return false;}
            if(stage==2){if(elapsed<5)return false;require(!pumpkin.isRemoved()&&pumpkin.position().equals(frozenPosition),"native falling pumpkin stays still across five real TimeStop ticks");game.items().timeStop().end(level);stopOwned=false;phase=game.server().getTickCount();stage++;return false;}
            if(stage==3){if(elapsed<3)return false;require(!pumpkin.isRemoved()&&!pumpkin.position().equals(frozenPosition),"real native falling physics resumes");game.items().entityUnloaded(pumpkin);pumpkin.discard();require(game.items().foodLifecycle().cause(pumpkin).equals(captured),"immutable removed direct-source cause remains resolvable");return true;}
            throw new IllegalStateException("invalid lifecycle fixture stage");
        }
        public void close(){if(closed)return;closed=true;try{if(stopOwned){game.items().timeStop().end(level);stopOwned=false;}if(prepared){owner.stopUsingItem();owned.addAll(game.items().foodLifecycle().snapshot());game.items().foodLifecycle().forget(owner);for(var entity:owned){game.items().entityUnloaded(entity);entity.discard();}}
            if(level!=null){for(var entry:terrain.entrySet())game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());for(var entry:terrain.entrySet())require(game.maps().snapshot(level,entry.getKey()).equals(entry.getValue()),"exact terrain/provenance restore "+entry.getKey());}
        }finally{try{if(saved!=null)saved.restore(game);if(client!=null)client.ack();if(attribution!=null)attribution.close();if(saved!=null)restored(saved);}finally{if(client!=null)client.close();if(level!=null)for(long key:forced)level.setChunkForced((int)key,(int)(key>>32),false);}}}
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive lifecycle fixture");running=new Fixture(game);try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}}
    public static void start(GameController game){run(game);}
    public static boolean tick(GameController game){if(running==null)return true;try{if(!running.step())return false;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS food-lifecycle (actual41 native source-food consumes/single debit, native nonmember leaf, current/old main/offhand infinite helpers across actual ticks, two real Halloween criteria/tick guard, bad apple layers, one20 pumpkin batch/captured WILD cause/native TimeStop pause+resume/exact teardown; no100-completion reward or landing damage/load claim)");return true;}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL food-lifecycle",error);return true;}}
    public static void cancel(GameController game){if(running!=null)try{running.close();}finally{running=null;}}
}
