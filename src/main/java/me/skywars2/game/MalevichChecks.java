package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.*;
import java.util.*;

/** Operator fixture. All admission, commits and ground pulses run through the real controller. */
public final class MalevichChecks {
    private MalevichChecks() { }
    private static Fixture running;
    private static List<PrimedTnt> stopWitness=List.of();
    private static String stopEpoch;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Malevich: "+why);}
    private static Field field(Object target,String name){
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{
            var result=type.getDeclaredField(name);result.setAccessible(true);return result;
        }catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing field "+name);
    }
    private static Object read(Object target,String name){try{return field(target,name).get(target);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static void write(Object target,String name,Object value){try{field(target,name).set(target,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static Object call(Object target,String name,Object... args){
        try{for(var method:target.getClass().getDeclaredMethods())if(method.getName().equals(name)&&method.getParameterCount()==args.length){method.setAccessible(true);return method.invoke(target,args);}throw new NoSuchMethodException(name);}
        catch(InvocationTargetException error){throw new IllegalStateException(name,error.getCause());}
        catch(ReflectiveOperationException error){throw new IllegalStateException(name,error);}
    }
    private static Object shared(String name,GameController game,ServerPlayer player){
        try{var type=Class.forName(PassiveConsumableChecks.class.getName()+"$"+name);var constructor=type.getDeclaredConstructor(GameController.class,ServerPlayer.class);constructor.setAccessible(true);return constructor.newInstance(game,player);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Shared fixture "+name,error);}
    }
    private static ItemStack pdc(String namespace){
        var stack=new ItemStack(Items.STONE);var root=new CompoundTag();var values=new CompoundTag();var nested=new CompoundTag();
        nested.putString(namespace+":type","malevich");values.put(namespace+":item",nested);root.put("PublicBukkitValues",values);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return stack;
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean probe;
        ServerPlayer owner,other;ServerLevel level;LbcMalevich service;
        Object snapshot,client,oldStage;SauvojenMeditationChecks.Attribution attribution;
        Vec3 origin,ground;ItemStack square,cursor,occupied;Marker marker;
        int oldPhase,start,boundary,stage=-1,mana;boolean configured,oldSkills,closed,stopped,observer,stunned;
        final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Entity> existing=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<PrimedTnt> baseline=Collections.newSetFromMap(new IdentityHashMap<>());
        List<ItemEntity> males=List.of();ItemEntity victim,inside,outside;
        Fixture(GameController game,boolean probe){this.game=game;this.probe=probe;}
        void initialize(){
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.allowDamage(owner)&&game.allowDamage(other),"two active participants");
            require(owner.containerMenu==owner.inventoryMenu,"personal inventory closed");
            require(!game.items().timeStop().stopped(owner.level())&&!game.items().shooters().stunned(owner)&&!game.items().frost().isFrozen(owner),"ambient statuses preserved");
            snapshot=shared("Snapshot",game,owner);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));
            level=owner.level();origin=owner.position();ground=origin.add(0,12,0);service=game.items().malevich();oldPhase=(Integer)read(service,"phase");oldStage=read(game,"stage");
            require(service.pendingCount()==0&&service.groundCount()==0,"exclusive pending inventory fills and ground squares");baseline.addAll(game.items().tntDrops().snapshot());
            oldSkills=game.config().lbcSkills;configured=true;client=shared("ScheduledClient",game,owner);
            level.getAllEntities().forEach(existing::add);
            var loot=game.lbc().rollTable(level,origin,owner,"lbc:ported/malevich");require(loot.size()==1,"one exact staging leaf");square=loot.getFirst();presentation(square);
            LbcInventoryHazards.recordProducer(square,other.getUUID(),game.items().epoch());
            for(var namespace:List.of("luckywars","lws","lucky_wars"))require(LbcMalevich.matches(pdc(namespace)),"nested PDC pair "+namespace);
            var raw=new ItemStack(Items.STONE);var tag=new CompoundTag();tag.putInt("malevich",1);raw.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            require(!LbcMalevich.matches(raw)&&!LbcMalevich.matches(ItemStack.EMPTY),"raw flag and empty stack excluded");
            reset();start=game.server().getTickCount();
        }
        void presentation(ItemStack stack){
            require(LbcMalevich.matches(stack)&&stack.is(Items.REPEATING_COMMAND_BLOCK)&&stack.getCount()==1,"original carrier/count/PDC");
            require(Identifier.parse("lbc:malevich").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_ff").equals(stack.get(DataComponents.TOOLTIP_STYLE))&&stack.has(DataComponents.CUSTOM_NAME)&&stack.has(DataComponents.LORE),"original model/tier/name/lore");
            var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();require(data.getIntOr("instantitem",0)==1&&data.getIntOr("malevich",0)==1,"original raw presentation flags retained");
            var attributes=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);require(attributes!=null&&attributes.modifiers().size()==1,"one original random attribute");
            var entry=attributes.modifiers().getFirst();require(entry.modifier().id().equals(Identifier.parse("minecraft:dv89seuf8jeswfdiskhgs"))&&entry.modifier().amount()>=1&&entry.modifier().amount()<=100,"native random reinforcement amount1..100 and source id");
        }
        void clearActors(){for(var actor:actors)if(!stopWitness.contains(actor))actor.discard();actors.clear();}
        void reset(){
            clearActors();if(marker!=null){marker.discard();marker=null;}
            if(stopped){game.items().timeStop().end(level);stopped=false;}if(stunned){game.items().shooters().clearStun(owner);stunned=false;}
            if(observer){game.spectators().remove(owner);observer=false;call(snapshot,"restore",game);call(snapshot,"restoreAttributes");}
            owner.stopUsingItem();owner.removeAllEffects();owner.getInventory().clearContent();owner.inventoryMenu.getCraftSlots().clearContent();owner.containerMenu.setCarried(ItemStack.EMPTY);
            owner.entityTags().remove("nomagic_active");owner.entityTags().add("obtained_crux");owner.setNoGravity(true);owner.setPos(origin);owner.setDeltaMovement(Vec3.ZERO);owner.setKnownMovement(Vec3.ZERO);call(client,"acknowledge");
            game.players().get(owner.getUUID()).lbcSkill=0;game.items().mana().inventoryChanged(owner);game.items().mana().baseRegen(owner,0);game.items().mana().set(owner,0);mana=0;
        }
        void arm(int phase){net.minecraft.advancements.triggers.CriteriaTriggers.INVENTORY_CHANGED.trigger(owner,owner.getInventory(),square);write(service,"phase",phase);boundary=game.server().getTickCount();}
        int count(){int result=0;for(int i=0;i<36;i++)if(!owner.getInventory().getItem(i).isEmpty())result++;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND))if(!owner.getItemBySlot(slot).isEmpty())result++;return result;}
        void carry(){owner.getInventory().setItem(9,square.copy());}
        void beacon(Vec3 at){marker=new Marker(EntityTypes.MARKER,level);marker.setPos(at);marker.entityTags().add("stopper_magic");require(level.addFreshEntity(marker),"real suppression marker");require(game.items().magic().beaconSuppressedAabb(level,at,20),"native marker positively suppresses its area");}
        ItemEntity drop(ItemStack stack,Vec3 at){
            var actor=new ItemEntity(level,at.x,at.y,at.z,stack.copy());actor.setDeltaMovement(Vec3.ZERO);actor.setNoGravity(true);actor.setPickUpDelay(32767);actor.setThrower(owner);actors.add(actor);require(level.addFreshEntity(actor),"native item insertion");return actor;
        }
        List<PrimedTnt> freshTnt(){return game.items().tntDrops().snapshot().stream().filter(t->!baseline.contains(t)&&!t.isRemoved()).toList();}
        void tnt(boolean keep){
            var list=freshTnt();require(list.size()>=101&&list.size()<=202,"one or two native TNT per101 entities");
            for(var actor:list){actors.add(actor);var v=actor.getDeltaMovement();require(v.x>=-.1&&v.x<.1&&v.z>=-.1&&v.z<.1&&v.y>=0&&v.y<.2&&actor.getFuse()>=0&&actor.getFuse()<=10,"source native fuse/velocity bounds before entity tick");require(LbcTntDrops.managed(actor)&&game.items().currentEpoch(actor)&&actor.getOwner()==other,"shared native TNT lifecycle/original producer/epoch");actor.setFuse(32767);actor.setNoGravity(true);actor.setDeltaMovement(Vec3.ZERO);}
            if(keep){stopWitness=List.copyOf(list);stopEpoch=game.items().epoch();}
        }
        void threshold(int n){var result=new ArrayList<ItemEntity>();for(int i=0;i<n;i++){var stack=square.copy();var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt("malevich_fixture_identity",i);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));result.add(drop(stack,ground));}males=result;arm(19);}
        @SuppressWarnings("unchecked") void stalePending(){
            // Change only this fixture's captured epoch, without changing the running match epoch.
            var jobs=(Map<Object,Object>)read(service,"pending");var job=jobs.get(owner);require(job!=null,"actual queued epoch witness");
            try{var parts=job.getClass().getRecordComponents();var values=new Object[parts.length];for(int i=0;i<parts.length;i++){var accessor=parts[i].getAccessor();accessor.setAccessible(true);values[i]=parts[i].getName().equals("epoch")?"skywars2.items.fixture_old_epoch":accessor.invoke(job);}var constructor=job.getClass().getDeclaredConstructors()[0];constructor.setAccessible(true);jobs.put(owner,constructor.newInstance(values));}
            catch(ReflectiveOperationException error){throw new IllegalStateException("scoped queued epoch witness",error);}
        }
        boolean step(){
            int now=game.server().getTickCount();require(now-start<160,"bounded real scheduler stage"+stage);
            if(stage==-1){start=now;if(probe){threshold(101);stage=90;}else{carry();cursor=new ItemStack(Items.DIAMOND,7);occupied=new ItemStack(Items.EMERALD,13);owner.containerMenu.setCarried(cursor.copy());owner.getInventory().setItem(4,occupied.copy());arm(0);stage=0;}return false;}
            if(stage==90){require(now==boundary+1,"actual ground stop-probe pulse");require(males.stream().allMatch(Entity::isRemoved),"source Malevich entities consumed");tnt(true);return true;}
            if(stage==0){require(now==boundary+1&&count()==2&&service.pendingCount()==1&&(Integer)read(service,"phase")==1,"phase1 admission schedules without immediate fill");stage=1;return false;}
            if(stage==1){require(now==boundary+2&&count()==41&&service.pendingCount()==0,"phase2 next native tick fills all41 slots atomically");require(ItemStack.matches(owner.getInventory().getItem(4),occupied)&&ItemStack.matches(owner.containerMenu.getCarried(),cursor),"occupied storage/cursor retained");var amounts=new HashSet<Double>();for(int i=0;i<36;i++)if(i!=4){var stack=owner.getInventory().getItem(i);presentation(stack);amounts.add(stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers().getFirst().modifier().amount());}for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.OFFHAND))presentation(owner.getItemBySlot(slot));require(amounts.size()>1&&game.items().mana().get(owner)==mana,"independent native random draws and no mana debit");reset();owner.containerMenu.setCarried(square.copy());owner.inventoryMenu.getCraftSlots().setItem(0,square.copy());arm(0);stage++;return false;}
            if(stage==2){require(service.pendingCount()==0&&count()==0,"cursor/crafting alone do not admit");reset();owner.setItemSlot(EquipmentSlot.HEAD,square.copy());owner.entityTags().add("nomagic_active");game.config().lbcSkills=false;require(game.items().shooters().stun(other,owner,20),"real stun installed");stunned=true;arm(0);stage++;return false;}
            if(stage==3){require(service.pendingCount()==1&&count()==1,"armor carries passive under noMagic/stun/config disabled");stage++;return false;}
            if(stage==4){require(count()==41,"armor passive commits through active-input gates");reset();owner.setItemSlot(EquipmentSlot.OFFHAND,square.copy());game.players().get(owner.getUUID()).lbcSkill=2;game.config().lbcSkills=false;arm(0);stage++;return false;}
            if(stage==5){require(service.pendingCount()==0&&count()==1,"native skill2 exemption independent of config");reset();carry();arm(0);stage++;return false;}
            if(stage==6){require(service.pendingCount()==1,"removal case actually admitted");owner.getInventory().clearContent();stage++;return false;}
            if(stage==7){require(count()==0&&service.pendingCount()==0,"source possession rechecked at commit");carry();arm(0);stage++;return false;}
            if(stage==8){require(count()==1&&service.pendingCount()==1,"suppression case admitted with only source square");beacon(origin);stage++;return false;}
            if(stage==9){require(count()==1&&service.pendingCount()==0,"beacon rechecked at commit count="+count()+" pending="+service.pendingCount()+" removed="+marker.isRemoved()+" at="+marker.position()+" bbox="+marker.getBoundingBox()+" owner="+owner.position()+" origin="+origin+" suppressed="+game.items().magic().beaconSuppressedAabb(level,owner.position(),20));reset();carry();arm(0);stage++;return false;}
            if(stage==10){require(service.pendingCount()==1,"skill change admitted");game.players().get(owner.getUUID()).lbcSkill=2;stage++;return false;}
            if(stage==11){require(count()==1&&service.pendingCount()==0,"skill2 rechecked at commit");reset();carry();game.items().timeStop().begin(level,20);stopped=true;arm(0);stage++;return false;}
            if(stage==12){require((Integer)read(service,"phase")==0&&service.pendingCount()==0&&count()==1,"Time Stop pauses phase and admission");reset();carry();arm(0);stage++;return false;}
            if(stage==13){require(service.pendingCount()==1,"stop cancellation admitted");service.stop();require(service.pendingCount()==0&&(Integer)read(service,"phase")==0,"stop clears pending/cadence");owner.getInventory().clearContent();stage++;return false;}
            if(stage==14){require(count()==0,"cancelled fill never commits");write(game,"stage",GameController.Stage.ENDING);try{require(!(Boolean)call(service,"admitted",owner),"ENDING admission rejected synchronously");}finally{write(game,"stage",oldStage);}reset();game.spectators().enter(owner);observer=true;owner.getInventory().clearContent();carry();arm(0);stage++;return false;}
            if(stage==15){require(service.pendingCount()==0&&count()==1,"observer role excluded");reset();var male=drop(square,ground);males=List.of(male);victim=drop(new ItemStack(Items.DIAMOND,64),ground.add(1,0,0));inside=drop(new ItemStack(Items.GOLD_INGOT,64),ground.add(3.20,0,0));outside=drop(new ItemStack(Items.IRON_INGOT,64),ground.add(3.30,0,0));arm(0);stage++;return false;}
            if(stage==16){int elapsed=now-boundary;if(elapsed<20){require(!victim.isRemoved()&&!inside.isRemoved(),"no early ground pulse "+elapsed);return false;}require(elapsed==20,"exact ground20 cadence");require(victim.isRemoved()&&inside.isRemoved()&&!outside.isRemoved()&&!males.getFirst().isRemoved(),"whole-stack conversion and expanded native AABB boundary");var drops=level.getEntitiesOfClass(ItemEntity.class,males.getFirst().getBoundingBox().inflate(4),item->!actors.contains(item)&&!existing.contains(item));actors.addAll(drops);require(drops.size()==2,"one native loot per removed64 stack");for(var item:drops){presentation(item.getItem());require(item.getOwner()==other&&game.items().inventoryHazards().producerOf(item.getItem())==other,"converted loot retains original producer before current thrower");var v=item.getDeltaMovement();require(v.x>=-.1&&v.x<.1&&v.z>=-.1&&v.z<.1&&Math.abs(v.y-.2)<1e-8,"Paper dropItem native constructor velocity");}reset();var stack=square.copy();stack.setCount(101);males=List.of(drop(stack,ground));arm(19);stage++;return false;}
            if(stage==17){require(!males.getFirst().isRemoved()&&freshTnt().isEmpty(),"stack count101 is one entity, never threshold");reset();threshold(100);stage++;return false;}
            if(stage==18){require(males.stream().noneMatch(Entity::isRemoved)&&freshTnt().isEmpty(),"exact100 distinct entities never explode");reset();threshold(101);beacon(ground);stage=180;return false;}
            if(stage==180){require(males.stream().noneMatch(Entity::isRemoved)&&freshTnt().isEmpty(),"beacon protects all101 ground squares");reset();threshold(101);stage=19;return false;}
            if(stage==19){require(males.stream().allMatch(Entity::isRemoved),">100 distinct entities explode");tnt(false);reset();carry();arm(0);stage++;return false;}
            if(stage==20){require(service.pendingCount()==1,"role-change case admitted");game.spectators().enter(owner);observer=true;owner.getInventory().clearContent();carry();stage++;return false;}
            if(stage==21){require(count()==1&&service.pendingCount()==0,"observer role rechecked at deferred commit");reset();carry();arm(0);stage++;return false;}
            if(stage==22){require(service.pendingCount()==1,"forget case actually admitted");service.forget(owner);require(service.pendingCount()==0,"forget cancels pending");owner.getInventory().clearContent();stage++;return false;}
            if(stage==23){require(count()==0,"forgotten deferred fill never commits");reset();carry();arm(0);stage++;return false;}
            if(stage==24){require(service.pendingCount()==1,"pending Time Stop case admitted");game.items().timeStop().begin(level,20);stopped=true;stage++;return false;}
            if(stage==25){require(service.pendingCount()==1&&count()==1&&(Integer)read(service,"phase")==1,"Time Stop retains pending commit and cadence");game.items().timeStop().end(level);stopped=false;stage++;return false;}
            if(stage==26){require(count()==41&&service.pendingCount()==0,"pending resumes on next active tick");reset();carry();arm(1);stage++;return false;}
            if(stage==27){int elapsed=now-boundary;if(elapsed<5){require(service.pendingCount()==0&&count()==1,"no admission between five-tick phases "+elapsed);return false;}require(elapsed==5&&service.pendingCount()==1&&(Integer)read(service,"phase")==6,"next admission exactly phase6");stage++;return false;}
            if(stage==28){require(count()==41&&service.pendingCount()==0,"phase7 commits next five-tick admission");reset();carry();arm(0);stage++;return false;}
            if(stage==29){require(service.pendingCount()==1,"stale epoch case admitted");stalePending();stage++;return false;}
            if(stage==30){require(count()==1&&service.pendingCount()==0,"old captured map epoch cancels pending commit");reset();var offline=java.util.UUID.fromString("aa110000-0000-0000-0000-000000000011");var copy=square.copy();LbcInventoryHazards.recordProducer(copy,offline,game.items().epoch());owner.getInventory().setItem(9,copy);arm(0);stage++;return false;}
            if(stage==31){require(service.pendingCount()==1,"offline-source fill actually queued");stage++;return false;}
            if(stage==32){require(count()==41,"offline-source native fill completes");var offline=java.util.UUID.fromString("aa110000-0000-0000-0000-000000000011");for(int i=0;i<=40;i++)require(offline.equals(game.items().inventoryHazards().producerIdOf(owner.getInventory().getItem(i))),"offline creator UUID retained in every native copy");var actor=game.items().tntDrops().spawnRecordedActor(level,ground,offline,Vec3.ZERO,300);require(actor!=null,"real native TNT with offline creator");actors.add(actor);var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());actor.saveWithoutId(output);var stored=output.buildResult();require(net.minecraft.core.UUIDUtil.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,stored.get("owner")).getOrThrow().equals(offline),"native TNT serializes unresolved original owner UUID");actor.discard();arm(2);stage++;return false;}
            if(stage==33){require(service.pendingCount()==0&&count()==41,"full inventory schedules no repeated fills");if(now-boundary<10)return false;reset();require(service.pendingCount()==0,"no fixture work remains");return true;}
            throw new IllegalStateException("Unknown stage "+stage);
        }
        @Override public void close(){
            if(closed)return;closed=true;var actions=new ArrayList<Runnable>();
            actions.add(this::clearActors);actions.add(()->{if(marker!=null)marker.discard();});actions.add(()->{if(stopped)game.items().timeStop().end(level);});actions.add(()->{if(stunned)game.items().shooters().clearStun(owner);});actions.add(()->{if(observer)game.spectators().remove(owner);});
            actions.add(()->{if(service!=null){service.forget(owner);write(service,"phase",oldPhase);}});actions.add(()->{if(oldStage!=null)write(game,"stage",oldStage);});actions.add(()->{if(configured)game.config().lbcSkills=oldSkills;});
            actions.add(()->{if(client!=null)call(client,"close");});actions.add(()->{if(snapshot!=null){call(snapshot,"restore",game);call(snapshot,"restoreAttributes");call(snapshot,"restoreUse");call(snapshot,"restoreProfile",game);}});actions.add(()->{if(attribution!=null)attribution.close();});
            Throwable failure=null;for(var action:actions)try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}if(failure!=null)throw new IllegalStateException("Malevich scoped restoration",failure);
        }
    }
    private static void begin(GameController game,boolean probe){require(running==null,"exclusive asynchronous fixture");running=new Fixture(game,probe);try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}}
    public static void run(GameController game){begin(game,false);}
    public static void stopProbe(GameController game){require(stopWitness.isEmpty(),"exclusive stop witness");begin(game,true);}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){boolean probe=running.probe;running.close();running=null;SkyWars2.LOGGER.info(probe?"[check] PASS malevich-stop-probe":"[check] PASS malevich (native controller phase1 admission/phase2 commit; original loot/PDC/presentation; carried slots and passive gates; commit guards; ground20/AABB/whole-stack conversion; distinct-entity threshold and shared owned TNT; scoped restoration)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL malevich",error);}}
    public static void cleanup(GameController game){if(running!=null){var old=running;running=null;old.close();}try{if(!stopWitness.isEmpty())require(!game.playing()&&!game.items().currentEpoch(stopEpoch)&&stopWitness.stream().allMatch(Entity::isRemoved)&&stopWitness.stream().noneMatch(game.items().tntDrops().snapshot()::contains),"actual match stop removes owned native TNT and invalidates epoch");}finally{for(var actor:stopWitness)actor.discard();stopWitness=List.of();stopEpoch=null;}SkyWars2.LOGGER.info("[check] PASS malevich-cleanup");}
}

