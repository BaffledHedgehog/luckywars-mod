package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;

/** Scheduled native sprint observations, native fill/reforge controls and an actual scoped player kill. */
public final class MoiseiBootsChecks {
    private MoiseiBootsChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Moisei: "+why);}
    private static Field field(Object owner,String name){for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("missing native fixture field "+name);}
    private static Object member(Object owner,String name){try{return field(owner,name).get(owner);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static Object invoke(Object owner,String name,Class<?>[] parameters,Object... arguments){try{var method=owner.getClass().getDeclaredMethod(name,parameters);method.setAccessible(true);return method.invoke(owner,arguments);}catch(ReflectiveOperationException error){if(error instanceof InvocationTargetException wrapped&&wrapped.getCause() instanceof RuntimeException cause)throw cause;throw new IllegalStateException(error);}}
    /** Reuse the qualified native snapshot/connection fixtures, including writable FOOD restore and retained ACK packet. */
    private static Object fixture(String nested,GameController game,ServerPlayer player){try{var type=Class.forName("me.skywars2.game.PassiveConsumableChecks$"+nested);var constructor=type.getDeclaredConstructor(GameController.class,ServerPlayer.class);constructor.setAccessible(true);return constructor.newInstance(game,player);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private static Object call(Object owner,String name){return invoke(owner,name,new Class<?>[0]);}
    private static Trial running;
    private static final int[] TIMES={1,40,41,80,81,120,121,160,161,200,201,240,241,280,281,320,321,360,361,400,401,450,451,500,501,550,551,600,601,650};
    private static final int[] AMPS={0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,20,20,30,30,40,40,50,50};
    private static final class Trial implements AutoCloseable {
        final GameController game;final ServerPlayer owner,other;final Object saved,otherSaved,client,audience;
        final ElementalGrimoireChecks.Fixture base;final SauvojenMeditationChecks.Attribution attribution;final LbcMoiseiBoots service;
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();final Map<BlockPos,net.minecraft.world.level.block.state.BlockState> expected=new LinkedHashMap<>();
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final Set<String> tags,otherTags;
        final Vec3 at;ItemStack boots;final String epoch;final int killedPlayers;int updates,mode,index,elapsed,clock;long emitted;boolean closed;
        Trial(GameController game){this.game=game;owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.playing()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two actual native participants");
            saved=fixture("Snapshot",game,owner);otherSaved=fixture("Snapshot",game,other);tags=new HashSet<>(owner.entityTags());otherTags=new HashSet<>(other.entityTags());
            killedPlayers=owner.getStats().getValue(net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER));base=new ElementalGrimoireChecks.Fixture(game);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));
            service=game.items().moiseiBoots();require(service.snapshot().isEmpty(),"exclusive empty Moisei service");base.initialize();at=owner.position();epoch=game.items().epoch();
            client=fixture("ScheduledClient",game,owner);audience=fixture("ScheduledClient",game,other);
            owner.level().getAllEntities().forEach(before::add);owner.getInventory().clearContent();owner.removeAllEffects();owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);owner.setHealth(10000);
            for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))owner.setItemSlot(slot,ItemStack.EMPTY);
            var p=game.players().get(owner.getUUID());p.lbcSkill=0;p.lbcTrinket=0;Arrays.fill(p.trinkets,0);Arrays.fill(p.levelups,0);p.lbcChallenge=0;
            for(var tag:List.of("sin_greed_active","nomagic_active","time_stopped","strict_map_object","magic_academy","spectator","rcdenyalways"))owner.entityTags().remove(tag);
        }
        void initialize(){
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-13,-1,-13)),BlockPos.containing(at.add(13,-1,13)))){
                require(owner.level().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null&&owner.level().getBlockState(pos).isAir(),"resident clear path comparison plane");
                terrain.put(pos.immutable(),game.maps().snapshot(owner.level(),pos));
            }
            boots=loot("lbc:ported/moisei_boots");presentation();acquisition();killTask();
            owner.setItemSlot(EquipmentSlot.FEET,boots.copy());owner.setXRot(37);owner.setYRot(45);owner.setSprinting(false);updates=updates();mode=-1;
        }
        int updates(){return (Integer)call(client,"updates");}
        void ack(){call(client,"acknowledge");call(audience,"acknowledge");}
        void hold(){owner.setNoGravity(true);owner.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);owner.setOnGround(true);owner.getFoodData().setFoodLevel(20);owner.getFoodData().setSaturation(5);owner.setSprinting(true);}
        ItemStack loot(String table){var result=game.lbc().rollTable(owner.level(),at,owner,table);require(result.size()==1,"native singleton leaf "+table);return result.getFirst();}
        void presentation(){
            require(boots.is(Items.LEATHER_BOOTS)&&boots.getMaxDamage()==600&&LbcFartus.data(boots).getIntOr("moisei",0)==1&&LbcFartus.data(boots).size()==3,"source vanilla boots durability/flags");
            require(Identifier.parse("lbc:armors/moisei_boots").equals(boots.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_ss").equals(boots.get(DataComponents.TOOLTIP_STYLE)),"source exact item model/tier");
            var equipment=boots.get(DataComponents.EQUIPPABLE);require(equipment!=null&&equipment.slot()==EquipmentSlot.FEET&&equipment.assetId().orElseThrow().identifier().equals(Identifier.parse("lbc:moisei")),"source equipment asset");
            var attrs=boots.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers();require(attrs.size()==1&&attrs.getFirst().attribute().equals(Attributes.ARMOR)&&attrs.getFirst().modifier().amount()==2&&attrs.getFirst().modifier().id().equals(Identifier.parse("minecraft:gehrththc55yyyyyharmor")),"source only armor2 native component");
            require(boots.get(DataComponents.LORE).lines().size()==2&&boots.get(DataComponents.CUSTOM_NAME).getStyle().getColor().getValue()==0x632f19,"source name color/two lore rows");
        }
        @SuppressWarnings("unchecked") <T>T weighted(Supplier<T> action){
            var table=game.server().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("lbc:transformables_boots_all")));
            var pools=(List<net.minecraft.world.level.storage.loot.LootPool>)member(table,"pools");require(pools.size()==1&&pools.getFirst().entries.size()==5,"original five-entry boots-all pool");
            var sequence=(Optional<Identifier>)member(table,"randomSequence");require(sequence.isEmpty(),"normal level RNG sequence");
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(owner.level()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,at).withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,owner).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.COMMAND);
            var context=new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(sequence);int total=0,selected=-1;
            for(int i=0;i<5;i++){var entries=new ArrayList<net.minecraft.world.level.storage.loot.entries.LootPoolEntry>();pools.getFirst().entries.get(i).expand(context,entries::add);
                for(var entry:entries){int weight=entry.getWeight(context.getLuck());if(weight<=0)continue;if(i==4)selected=total;total+=weight;}}
            require(selected>=0,"native original Moisei weighted entry available");var random=field(owner.level(),"random");Object original;
            try{original=random.get(owner.level());final int bound=total,pick=selected;int[] calls={0};
                random.set(owner.level(),Proxy.newProxyInstance(net.minecraft.util.RandomSource.class.getClassLoader(),new Class<?>[]{net.minecraft.util.RandomSource.class},(proxy,method,args)->{
                    if(method.getName().equals("nextInt")&&args!=null&&args.length==1&&args[0].equals(bound)){calls[0]++;return pick;}
                    try{return method.invoke(original,args);}catch(InvocationTargetException error){throw error.getCause();}}));
                try{T value=action.get();require(calls[0]==1,"one native boots-all weighted selection without retries");return value;}finally{random.set(owner.level(),original);}
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        }
        void acquisition(){
            var rolled=weighted(()->loot("lbc:transformables_boots_all"));require(ItemStack.matches(rolled,boots),"actual original boots-all source table produces exact native item");
            var donor=new ItemEntity(owner.level(),at.x,at.y,at.z,boots.copy());donor.setNoGravity(true);require(owner.level().addFreshEntity(donor),"actual ULTRA donor resident");
            var outputs=new ArrayList<ItemStack>();try{weighted(()->{try{var method=LbcBolts.class.getDeclaredMethod("reforgeInput",ItemEntity.class,LbcBolts.ReforgeFamily.class,int.class,ServerPlayer.class,java.util.function.Consumer.class);method.setAccessible(true);
                var work=(LbcStonekivi.Work)method.invoke(game.items().bolts(),donor,LbcBolts.ReforgeFamily.ULTRA,1,owner,(java.util.function.Consumer<ItemStack>)outputs::add);require(work!=null&&work.step(),"actual existing ULTRA work completes");return true;}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}});
                require(donor.isRemoved()&&outputs.size()==1&&ItemStack.matches(outputs.getFirst(),boots),"ordinary existing ULTRA source route consumes donor and supplies original boots");
            }finally{donor.discard();}
        }
        boolean selectKit(){boolean ranked=game.config().ranked;game.config().ranked=false;game.spectators().enter(owner);
            try{return game.lbc().select(owner,LbcLoadouts.Kind.KITS,9000,6);}finally{game.config().ranked=ranked;game.spectators().remove(owner);owner.setGameMode(GameType.SURVIVAL);owner.setNoGravity(true);}}
        @SuppressWarnings("unchecked") void killTask(){
            var task=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/armor/moisei_boots"));require(task!=null,"original native player-kill task imported");
            for(var criterion:task.value().criteria().keySet())owner.getAdvancements().revoke(task,criterion);game.players().get(owner.getUUID()).lbcUnlocks.remove("lbc:true_advancements/armor/moisei_boots");
            require(!selectKit(),"actual existing kit7 selection locked before task");
            require(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestMoiseiVictim"))==null,"exclusive disposable native kill witness");
            var victim=GameChecks.createBot(game,"TestMoiseiVictim");require(victim!=null&&victim.getClass()==ServerPlayer.class,"real vanilla connected player victim");
            var participants=(Map<UUID,Object>)member(game,"participants");Object enrolled=null;
            try{
                var template=participants.get(owner.getUUID());var type=template.getClass();var constructor=type.getDeclaredConstructor(MapManager.Spawn.class,int.class);constructor.setAccessible(true);
                enrolled=constructor.newInstance(member(template,"spawn"),1);participants.put(victim.getUUID(),enrolled);game.spectators().remove(victim);
                victim.setGameMode(GameType.SURVIVAL);victim.getInventory().clearContent();victim.removeAllEffects();victim.getAbilities().invulnerable=false;victim.setHealth(1);victim.setAbsorptionAmount(0);victim.setInvulnerableTime(0);victim.damageCooldownTime=0;
                victim.teleportTo(owner.level(),at.x,at.y,at.z+3,Set.of(),180,0,true);owner.setItemSlot(EquipmentSlot.FEET,boots.copy());owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                require(victim.hurtServer(owner.level(),victim.damageSources().playerAttack(owner),1000)&&victim.isDeadOrDying()&&game.awaitingRespawn(victim.getUUID()),"actual accepted native player death, not a manual criterion award");
                require(owner.getAdvancements().getOrStartProgress(task).isDone()&&selectKit(),"real equipped kill awards source task and opens existing kit7");
                var dirt=game.lbc().rollTable(owner.level(),at,owner,"lbc:kit/moisei");require(dirt.stream().allMatch(stack->stack.is(Items.DIRT))&&dirt.stream().mapToInt(ItemStack::getCount).sum()==1024,"existing kit7 grants source1024 dirt and no invented boots");
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            finally{participants.remove(victim.getUUID());game.spectators().remove(victim);game.items().indirectKills().clear(victim);victim.connection.onDisconnect(new net.minecraft.network.DisconnectionDetails(net.minecraft.network.chat.Component.literal("Moisei native kill fixture complete")));}
        }
        void restorePlane(){for(var entry:terrain.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());}
        void nativePath(int timer){
            var source=game.server().createCommandSourceStack().withLevel(owner.level()).withEntity(owner).withPosition(owner.position().add(0,-1,0)).withRotation(new Vec2(0,owner.getYRot())).withSuppressedOutput();
            var commands=new ArrayList<String>();for(var sample:List.of("0",".25",".5",".75","1","1.5","2","3"))commands.add("fill ^ ^ ^"+sample+" ^ ^ ^"+sample+" minecraft:dirt replace #minecraft:airs strict");
            if(timer>=100)commands.add("fill ^ ^ ^3.5 ^ ^ ^4 minecraft:dirt replace #minecraft:airs strict");if(timer>=200)commands.add("fill ^ ^ ^4 ^ ^ ^5 minecraft:dirt replace #minecraft:airs strict");
            if(timer>=300){commands.add("fill ^ ^ ^5 ^ ^ ^6 minecraft:dirt replace #minecraft:airs strict");commands.add("fill ^ ^ ^6 ^ ^ ^7 minecraft:dirt replace #minecraft:airs strict");}
            if(timer>=400)commands.add("fill ^ ^ ^7 ^ ^ ^8 minecraft:dirt replace #minecraft:airs strict");if(timer>=500){commands.add("fill ^ ^ ^8 ^ ^ ^9 minecraft:dirt replace #minecraft:airs strict");commands.add("fill ^ ^ ^9 ^ ^ ^10 minecraft:dirt replace #minecraft:airs strict");}
            if(timer>=600)commands.add("fill ^ ^ ^10 ^ ^ ^11 minecraft:dirt replace #minecraft:airs strict");
            for(var command:commands)game.server().getCommands().performPrefixedCommand(source,command);
        }
        void prepareTier(){hold();owner.removeEffect(MobEffects.SPEED);restorePlane();game.items().mana().score(owner,"sprint_timer").set(TIMES[index]-1);
            nativePath(TIMES[index]);expected.clear();for(var pos:terrain.keySet())expected.put(pos,owner.level().getBlockState(pos));restorePlane();updates=updates();}
        void speed(int amplifier){var effect=owner.getEffect(MobEffects.SPEED);require(effect!=null&&effect.getAmplifier()==amplifier&&effect.getDuration()==20,"source native20 tick Speed"+(amplifier+1)+" actual="+effect);}
        boolean step(){ack();if(updates()==updates)return false;require(updates()-updates==1,"one real scheduled native player update per observation");updates=updates();require(game.items().currentEpoch(epoch)&&game.playing(),"same active fixture epoch");
            switch(mode){
                case -1->{require((Boolean)call(client,"ready")&&(Boolean)call(audience,"ready"),"actual native connection clocks primed and teleports acknowledged");mode=0;prepareTier();}
                case 0->{require(game.items().mana().score(owner,"sprint_timer").get()==TIMES[index],"source increment-before-grant timer expected="+TIMES[index]+" actual="+game.items().mana().score(owner,"sprint_timer").get()+" sprint="+owner.isSprinting()+" updates="+updates+" usable="+game.canUseGameplayItems(owner)+" observer="+game.isObserver(owner)+" speed="+owner.getEffect(MobEffects.SPEED));speed(AMPS[index]);
                    for(var entry:expected.entrySet())require(owner.level().getBlockState(entry.getKey())==entry.getValue(),"actual STRICT flat source geometry equals independent native fill at timer"+TIMES[index]);
                    if(++index<TIMES.length){prepareTier();return false;}
                    owner.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);owner.removeEffect(MobEffects.SPEED);game.items().mana().score(owner,"sprint_timer").set(99);hold();mode=1;
                }
                case 1->{require(game.items().mana().score(owner,"sprint_timer").get()==100&&!owner.hasEffect(MobEffects.SPEED),"SECRET shared clock increments even unequipped");owner.setItemSlot(EquipmentSlot.FEET,boots.copy());hold();mode=2;}
                case 2->{require(game.items().mana().score(owner,"sprint_timer").get()==101,"mid-sprint equip inherits accumulated shared clock");speed(2);
                    require(game.items().shooters().stun(other,owner,20)&&game.items().actionsBlocked(owner),"actual input stun admitted");game.items().mana().score(owner,"sprint_timer").set(40);owner.removeEffect(MobEffects.SPEED);hold();mode=3;}
                case 3->{require(game.items().shooters().stunned(owner)&&!owner.isSprinting()&&game.items().mana().score(owner,"sprint_timer").get()==0&&!owner.hasEffect(MobEffects.SPEED),"native input stun genuinely stops sprint, resetting its canonical continuous-sprint clock without a fake passive pause");game.items().shooters().clearStun(owner);
                    for(int i=0;i<10&&!game.items().frost().isFrozen(owner);i++)game.items().frost().add(owner,other);require(game.items().frost().isFrozen(owner),"native ice freeze admitted");game.items().mana().score(owner,"sprint_timer").set(80);owner.removeEffect(MobEffects.SPEED);hold();mode=4;}
                case 4->{require(game.items().mana().score(owner,"sprint_timer").get()==81,"ice leaves sprint passive active");speed(2);game.items().frost().clear(owner);game.items().mana().score(owner,"sprint_timer").set(123);game.items().timeStop().begin(owner.level(),10);mode=5;}
                case 5->{require(game.items().mana().score(owner,"sprint_timer").get()==123,"actual Time Stop pauses canonical clock");game.items().timeStop().end(owner.level());
                    hold();owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,200,4,false,false,false),owner);owner.setXRot(-35);game.items().mana().score(owner,"sprint_timer").set(650);emitted=service.spawned();elapsed=0;mode=6;}
                case 6->{speed(50);require(game.items().mana().score(owner,"sprint_timer").get()==651+elapsed&&service.spawned()-emitted==10L*(elapsed+1),"ten real native TNT every651+ sprint tick without slowing");
                    var point=owner.position().subtract(owner.getLookAngle().scale(5));var tnts=service.snapshot();require(tnts.size()==10&&tnts.stream().allMatch(entity->entity instanceof PrimedTnt tnt&&tnt.getClass()==PrimedTnt.class&&tnt.getFuse()==0&&tnt.getDeltaMovement().equals(Vec3.ZERO)&&tnt.position().distanceToSqr(point)<1e-8&&service.owner(tnt)==owner),"source full-pitch feet-back5 native fuse0 TNT formation");
                    require(service.canAffect(tnts.getFirst(),owner),"self-source admitted for ordinary native explosion");other.entityTags().add("sin_greed_active");try{require(!service.canAffect(tnts.getFirst(),other),"shared Greed target excluded");}finally{other.entityTags().remove("sin_greed_active");}
                    if(++elapsed<3){hold();return false;}owner.setSprinting(false);clock=updates;mode=7;
                }
                case 7->{require(game.items().mana().score(owner,"sprint_timer").get()==0,"actual stop-sprint resets shared timer");if(updates-clock<23)return false;require(!owner.hasEffect(MobEffects.SPEED),"native Speed expires after sprint refresh ends");service.forget(owner);require(service.snapshot().isEmpty(),"owned actor lifecycle cleanup");return true;}
                default->throw new IllegalStateException("fixture stage");
            }
            return false;
        }
        @Override public void close(){if(closed)return;closed=true;Throwable failure=null;
            for(Runnable action:List.<Runnable>of(()->{game.items().timeStop().end(owner.level());service.forget(owner);game.items().shooters().clearStun(owner);game.items().frost().clear(owner);},this::restorePlane,
                ()->{for(var entity:owner.level().getAllEntities())if(!before.contains(entity)&&entity instanceof ItemEntity&&entity.position().distanceToSqr(at)<900)entity.discard();},()->{call(client,"close");call(audience,"close");},base::close,
                ()->invoke(saved,"restore",new Class<?>[]{GameController.class},game),()->invoke(otherSaved,"restore",new Class<?>[]{GameController.class},game),()->{call(saved,"restoreAttributes");call(otherSaved,"restoreAttributes");call(saved,"restoreUse");call(otherSaved,"restoreUse");},
                ()->{owner.entityTags().clear();owner.entityTags().addAll(tags);other.entityTags().clear();other.entityTags().addAll(otherTags);owner.getStats().setValue(owner,net.minecraft.stats.Stats.ENTITY_KILLED.get(EntityTypes.PLAYER),killedPlayers);},attribution::close,()->game.players().save()))try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}
            if(failure!=null)throw new IllegalStateException("Moisei scoped restoration",failure);
        }
    }
    public static void run(GameController game){require(running==null,"exclusive scheduled trial");var trial=new Trial(game);try{trial.initialize();running=trial;SkyWars2.LOGGER.info("[check] moisei-boots running (source tier/geometry/TNT/native expiry observations)");}catch(Throwable error){try{trial.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS moisei-boots (native source leaf/ULTRA route, real player-kill task/kit7 dirt gate,30 source tier boundaries/native STRICT geometry, unequipped clock, input stun/ice/TimeStop, ten native TNT every651+ tick/full pitch, native expiry/scoped restore)");}}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL moisei-boots",error);}}
}
