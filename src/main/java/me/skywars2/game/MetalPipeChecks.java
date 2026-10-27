package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.*;
import java.util.*;

/** Explicit operator qualification; natural physics and the real item/input scheduler own the effects. */
public final class MetalPipeChecks {
    private MetalPipeChecks(){}
    private static Fixture running;
    private static ItemEntity stopWitness;
    private static String stopEpoch;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Metal Pipe: "+why);}
    private static Field field(Object target,String name){for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing field "+name);}
    private static Object read(Object target,String name){try{return field(target,name).get(target);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static void write(Object target,String name,Object value){try{field(target,name).set(target,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static Object call(Object target,String name,Object... args){try{for(var method:target.getClass().getDeclaredMethods())if(method.getName().equals(name)&&method.getParameterCount()==args.length){method.setAccessible(true);return method.invoke(target,args);}throw new NoSuchMethodException(name);}catch(InvocationTargetException error){throw new IllegalStateException(name,error.getCause());}catch(ReflectiveOperationException error){throw new IllegalStateException(name,error);}}
    private static Object shared(String name,GameController game,ServerPlayer player){try{var type=Class.forName(PassiveConsumableChecks.class.getName()+"$"+name);var constructor=type.getDeclaredConstructor(GameController.class,ServerPlayer.class);constructor.setAccessible(true);return constructor.newInstance(game,player);}catch(ReflectiveOperationException error){throw new IllegalStateException("Shared fixture "+name,error);}}
    private static ItemStack raw(String key){var stack=new ItemStack(Items.STICK);var data=new CompoundTag();data.putInt(key,1);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return stack;}
    private static ItemStack pdc(String namespace){var stack=new ItemStack(Items.STONE);var root=new CompoundTag();var pdc=new CompoundTag();var nested=new CompoundTag();nested.putString(namespace+":type","metal_pipe");pdc.put(namespace+":item",nested);root.put("PublicBukkitValues",pdc);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return stack;}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final boolean probe;
        ServerPlayer owner,other;ServerLevel level;LbcMetalPipe service;
        Object saved,otherSaved,client,audience;SauvojenMeditationChecks.Attribution attribution;
        ItemStack pipe,divider;ItemEntity falling,source,far,exempt,standing;Cow center,inside,outside,greedy,protectedRole;Marker beacon;
        Vec3 origin,otherOrigin,landing;int floor,start,boundary,stage=-1,landingSounds,firstLandingTick,mana,oldPhase;
        Integer savedTimer,otherSavedTimer;int craftedBefore;boolean clockCaptured;
        boolean closed,stopped,oldSkills,oldOffhand,configured,otherObserver,oldCrux,oldOtherCrux;
        net.minecraft.world.scores.PlayerTeam team,ownerTeam,otherTeam;
        final Set<Entity> actors=Collections.newSetFromMap(new IdentityHashMap<>());
        final Set<Entity> baseline=Collections.newSetFromMap(new IdentityHashMap<>());
        final Map<BlockPos,MapManager.BlockSnapshot> terrain=new LinkedHashMap<>();
        Fixture(GameController game,boolean probe){this.game=game;this.probe=probe;}
        void initialize(){
            owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(owner!=null&&other!=null&&game.allowDamage(owner)&&game.allowDamage(other),"two active match participants");
            require(owner.containerMenu==owner.inventoryMenu&&other.containerMenu==other.inventoryMenu,"closed personal menus");
            require(!game.items().timeStop().stopped(owner.level())&&!game.items().shooters().stunned(owner)&&!game.items().frost().isFrozen(owner),"ambient status preserved");
            saved=shared("Snapshot",game,owner);otherSaved=shared("Snapshot",game,other);attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner,other));
            level=owner.level();origin=owner.position();otherOrigin=other.position();service=game.items().metalPipe();require(service.trackedCount()==0,"exclusive physical pipe fixture count="+service.trackedCount());oldPhase=(Integer)read(service,"phase");
            var clocks=clocks();savedTimer=clocks.get(owner);otherSavedTimer=clocks.get(other);clockCaptured=true;clocks.remove(owner);game.items().mana().score(owner,"metal_divider_cd").set(0);craftedBefore=owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_CRAFTED.get(Items.CARROT_ON_A_STICK));
            level.getAllEntities().forEach(baseline::add);oldSkills=game.config().lbcSkills;oldOffhand=game.config().offhandWands;configured=true;game.config().offhandWands=true;
            oldCrux=owner.entityTags().contains("obtained_crux");oldOtherCrux=other.entityTags().contains("obtained_crux");
            client=shared("ScheduledClient",game,owner);audience=shared("ScheduledClient",game,other);
            for(var player:List.of(owner,other)){player.stopUsingItem();player.removeAllEffects();player.getInventory().clearContent();player.inventoryMenu.getCraftSlots().clearContent();player.containerMenu.setCarried(ItemStack.EMPTY);player.entityTags().add("obtained_crux");player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.setKnownMovement(Vec3.ZERO);}
            game.items().mana().inventoryChanged(owner);game.items().mana().baseRegen(owner,0);game.items().mana().set(owner,0);mana=0;
            pipe=loot("lbc:metal_pipe");presentation(pipe);divider=loot("lbc:ported/metal_pipe_divider");require(LbcMetalPipe.divider(divider),"original full Divider staging leaf");
            for(var namespace:List.of("luckywars","lws","lucky_wars"))require(LbcMetalPipe.matches(pdc(namespace)),"exact nested PDC "+namespace);
            require(!LbcMetalPipe.matches(raw("metal_pipe"))&&!LbcMetalPipe.matches(ItemStack.EMPTY)&&LbcMetalPipe.divider(raw("divider_by_metal_pipe")),"raw melee flag distinct from ground PDC and exact divider raw flag");
            floor=net.minecraft.util.Mth.floor(origin.y)+8;landing=new Vec3(Math.floor(origin.x)+.5,floor+1,Math.floor(origin.z)+.5);
            require(floor+6<level.getMaxY(),"bounded resident test platform");
            if(!probe){platform();craft();}
            start=game.server().getTickCount();
        }
        ItemStack loot(String id){var values=game.lbc().rollTable(level,origin,owner,id);require(values.size()==1,"one native loot result "+id);return values.getFirst();}
        void presentation(ItemStack stack){require(LbcMetalPipe.matches(stack)&&stack.is(Items.SPYGLASS)&&stack.getCount()==1,"source spyglass/count/PDC");require(Identifier.parse("lbc:metal_pipe").equals(stack.get(DataComponents.ITEM_MODEL))&&Identifier.parse("lbc:tier_d").equals(stack.get(DataComponents.TOOLTIP_STYLE))&&stack.has(DataComponents.CUSTOM_NAME)&&stack.has(DataComponents.LORE),"source model/tier/name/lore");var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();for(var key:List.of("metal_pipe","craftusable","transformable","sword"))require(data.getIntOr(key,0)==1,"source raw flag "+key);var attributes=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);require(attributes!=null&&attributes.modifiers().size()==2,"source two melee modifiers");require(attributes.modifiers().stream().anyMatch(entry->entry.modifier().amount()==10)&&attributes.modifiers().stream().anyMatch(entry->Math.abs(entry.modifier().amount()+3.1)<1e-8),"source attack damage10/speed-3.1");}
        void put(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){require(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null,"fixture never loads absent chunk");terrain.computeIfAbsent(pos.immutable(),game.maps()::snapshot);game.maps().restoreSnapshot(pos,new MapManager.BlockSnapshot(state,null));}
        void platform(){var base=BlockPos.containing(landing).below();for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++){put(base.offset(x,0,z),Blocks.STONE.defaultBlockState());for(int y=1;y<=6;y++)put(base.offset(x,y,z),Blocks.AIR.defaultBlockState());}}
        void craft(){
            var ingredient=raw("greed_greed");var input=CraftingInput.of(2,1,List.of(pipe.copy(),ingredient.copy()));require(LbcCraftingRecipe.craftingKind(input)==LbcCraftingRecipe.Kind.METAL_DIVIDER,"native shared recipe recognizes pipe+Divider");
            require(LbcCraftingRecipe.craftingKind(CraftingInput.of(2,1,List.of(pipe.copy(),pipe.copy())))!=LbcCraftingRecipe.Kind.METAL_DIVIDER,"duplicate pipe rejected");
            var menu=owner.inventoryMenu;var slots=menu.getInputGridSlots();slots.getFirst().setByPlayer(pipe.copyWithCount(2));slots.getLast().setByPlayer(ingredient.copyWithCount(2));
            require(LbcMetalPipe.divider(menu.getResultSlot().getItem())&&menu.getCarried().isEmpty(),"real inventory grid previews full Divider");
            GameChecks.inventoryClick(owner,0,0,ContainerInput.PICKUP);require(LbcMetalPipe.divider(menu.getCarried())&&menu.getCarried().getCount()==1&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"native result take consumes one of each stacked donor");
            menu.setCarried(ItemStack.EMPTY);slots.forEach(slot->slot.setByPlayer(ItemStack.EMPTY));owner.getInventory().clearContent();
        }
        Cow cow(Vec3 at){var actor=new Cow(EntityTypes.COW,level);actor.setPos(at);actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;actor.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);actor.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);actor.setHealth(100);actors.add(actor);require(level.addFreshEntity(actor),"real native living target");return actor;}
        ItemEntity item(ItemStack stack,Vec3 at,boolean gravity){var actor=new ItemEntity(level,at.x,at.y,at.z,stack.copy());actor.setThrower(owner);actor.setDeltaMovement(Vec3.ZERO);actor.setNoGravity(!gravity);actor.setPickUpDelay(32767);actors.add(actor);require(level.addFreshEntity(actor),"native source item inserted");return actor;}
        @SuppressWarnings("unchecked") List<Object> packets(Object listener){return (List<Object>)read(read(listener,"connection"),"packets");}
        List<ClientboundSoundPacket> sounds(float volume,float pitch){return packets(client).stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).filter(sound->sound.getSound().value().location().equals(Identifier.parse("minecraft:lbcsounds.metal_pipe"))&&sound.getSource()==SoundSource.MASTER&&sound.getVolume()==volume&&sound.getPitch()==pitch).toList();}
        void acknowledge(){call(client,"acknowledge");call(audience,"acknowledge");}
        void next(){boundary=game.server().getTickCount();stage++;}
        void clearActors(){for(var actor:actors)if(actor!=stopWitness)actor.discard();actors.clear();}
        boolean input(InteractionHand hand){owner.stopUsingItem();return owner.gameMode.useItem(owner,level,owner.getItemInHand(hand),hand).consumesAction();}
        void noForeignItems(){for(var actor:level.getAllEntities())if(actor instanceof ItemEntity&&!actor.isRemoved())require(actors.contains(actor),"global Divider test requires exclusively scoped loaded item entities");}
        void team(){var board=game.server().getScoreboard();ownerTeam=owner.getTeam();otherTeam=other.getTeam();team=board.addPlayerTeam("pipe_fixture");board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);}
        void unteam(){if(team==null)return;var board=game.server().getScoreboard();if(ownerTeam==null)board.removePlayerFromTeam(owner.getScoreboardName());else board.addPlayerToTeam(owner.getScoreboardName(),ownerTeam);if(otherTeam==null)board.removePlayerFromTeam(other.getScoreboardName());else board.addPlayerToTeam(other.getScoreboardName(),otherTeam);board.removePlayerTeam(team);team=null;}
        void gatherNewItems(){if(level==null||owner==null)return;for(var actor:level.getAllEntities())if(actor instanceof ItemEntity item&&!baseline.contains(actor)&&!actors.contains(actor)&&(LbcMetalPipe.matches(item.getItem())||LbcMetalPipe.divider(item.getItem()))&&item.getOwner()==owner)actors.add(actor);}
        boolean step(){
            int now=game.server().getTickCount();require(now-start<440,"bounded actual native fixture stage"+stage);
            if(stage==-1){start=now;if(probe){falling=item(pipe,landing.add(0,4,0),false);require(service.trackedCount()==1,"real lifecycle creates stop witness");stopWitness=falling;stopEpoch=game.items().epoch();stage=90;boundary=now;return false;}
                center=cow(landing.add(0,0,1));inside=cow(landing.add(3,0,0));outside=cow(landing.add(3.2,0,0));greedy=cow(landing.add(-1,0,0));greedy.entityTags().add("sin_greed_active");protectedRole=cow(landing.add(0,0,-1.8));protectedRole.entityTags().add("strict_map_object");
                team();other.setPos(landing.add(0,0,-1));other.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);other.setHealth(100);other.getInventory().clearContent();acknowledge();
                owner.entityTags().add("nomagic_active");game.config().lbcSkills=false;beacon=new Marker(EntityTypes.MARKER,level);beacon.setPos(landing);beacon.entityTags().add("stopper_magic");actors.add(beacon);require(level.addFreshEntity(beacon),"real physical-suppression negative control marker");
                falling=item(pipe,landing.add(0,4,0),true);require(service.trackedCount()==1&&(Integer)read(falling,"pickupDelay")==32767,"physical spawn tracked with native infinite pickup sentinel");landingSounds=sounds(4,1).size();boundary=now;stage=0;return false;}
            if(stage==90){require(now==boundary+1&&!stopWitness.isRemoved()&&service.trackedCount()==1,"actual scheduler retains live physical stop witness");return true;}
            if(stage==0){
                if(!falling.onGround()){require(center.getHealth()==100&&inside.getHealth()==100&&sounds(4,1).size()==landingSounds,"airborne pipe has no early impact");require(now-boundary<35,"natural native landing bounded");return false;}
                require(center.getHealth()==75&&inside.getHealth()==75&&outside.getHealth()==100,"raw25 native damage and actual bbox2.5 inside/outside boundary");require(greedy.getHealth()==100&&other.getHealth()==100&&protectedRole.getHealth()==100,"absolute Greed/default allied player/protected creature role");
                require(center.getLastDamageSource()!=null&&center.getLastDamageSource().getEntity()==owner&&center.getLastDamageSource().getDirectEntity()==falling&&!LbcDamage.isMeleeAttack(center.getLastDamageSource(),owner),"native pipe damage retains creator and physical direct source without held-melee triggers");require(sounds(4,1).size()==landingSounds+1,"one real landing sound MASTER4/pitch1");require((Integer)read(falling,"pickupDelay")==20,"landing opens native20tick pickup window");require(game.items().mana().get(owner)==mana,"physical noMagic/beacon/config disabled landing spends no mana");
                firstLandingTick=now;next();return false;
            }
            if(stage==1){require(now==boundary+1&&center.getHealth()==75&&sounds(4,1).size()==landingSounds+1,"grounded pipe does not repeat impact");center.setInvulnerableTime(0);inside.setInvulnerableTime(0);falling.setPos(landing.add(0,3,0));falling.setDeltaMovement(0,.05,0);falling.setOnGround(false);next();return false;}
            if(stage==2){require((Integer)read(falling,"pickupDelay")==32767&&center.getHealth()==75,"ground-to-air rearms infinite pickup sentinel");next();return false;}
            if(stage==3){if(!falling.onGround()){require(now-boundary<35,"second natural landing bounded");return false;}require(center.getHealth()==50&&inside.getHealth()==50&&sounds(4,1).size()==landingSounds+2,"real second landing rearms one impact");clearActors();unteam();other.setPos(otherOrigin);acknowledge();owner.entityTags().remove("nomagic_active");game.config().lbcSkills=true;next();return false;}
            if(stage==4){
                var target=cow(landing.add(0,0,1));owner.setItemInHand(InteractionHand.MAIN_HAND,raw("metal_pipe"));int before=sounds(2,2).size();require(target.hurtServer(level,level.damageSources().playerAttack(owner),1),"accepted attributed native melee damage");require(sounds(2,2).size()==before+1,"raw carried melee flag emits source forward MASTER2/pitch2 sound");
                var sound=sounds(2,2).getLast();var forward=owner.position().add(owner.getLookAngle());require(new Vec3(sound.getX(),sound.getY(),sound.getZ()).distanceToSqr(forward)<.05,"native packet sound position is source forward one block");
                var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(level,owner,new ItemStack(Items.ARROW),null);arrow.setPos(landing.add(0,4,0));arrow.setNoGravity(true);arrow.setDeltaMovement(Vec3.ZERO);actors.add(arrow);require(level.addFreshEntity(arrow),"native attributed arrow control inserted");target.setInvulnerableTime(0);require(target.hurtServer(level,level.damageSources().arrow(arrow,owner),1)&&sounds(2,2).size()==before+1,"accepted arrow damage excludes carried pipe sound");
                var fatal=cow(landing.add(0,0,2));fatal.setHealth(1);var deathBefore=Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());level.getEntitiesOfClass(Entity.class,fatal.getBoundingBox().inflate(3)).forEach(deathBefore::add);require(fatal.hurtServer(level,level.damageSources().playerAttack(owner),1)&&!fatal.isAlive()&&sounds(2,2).size()==before+2,"fatal native damage retains the melee sound");for(var drop:level.getEntitiesOfClass(Entity.class,fatal.getBoundingBox().inflate(3),actor->!deathBefore.contains(actor)&&(actor instanceof ItemEntity||actor instanceof net.minecraft.world.entity.ExperienceOrb)))actors.add(drop);var ordinary=cow(landing.add(0,0,2));owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);require(ordinary.hurtServer(level,level.damageSources().playerAttack(owner),1)&&sounds(2,2).size()==before+2,"independent ordinary accepted hit emits no pipe sound");clearActors();owner.getInventory().clearContent();source=item(pipe.copyWithCount(2),landing.add(0,3,0),false);far=item(raw("greed_greed").copyWithCount(3),landing.add(.5,3,0),false);write(service,"phase",0);boundary=now;stage=40;return false;
            }
            if(stage==40){int elapsed=now-boundary;if(elapsed<5){require(!source.isRemoved()&&!far.isRemoved(),"ground craft waits for real five-tick pulse");return false;}require(elapsed==5&&source.isRemoved()&&far.isRemoved(),"source ground craft consumes both entire stacked entities");gatherNewItems();var outputs=actors.stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast).filter(item->!item.isRemoved()).toList();require(outputs.size()==1&&LbcMetalPipe.divider(outputs.getFirst().getItem())&&outputs.getFirst().getItem().getCount()==1,"ground recipe emits one actual native Metal Divider");clearActors();stage=5;boundary=now;return false;}
            if(stage==5){
                falling=item(pipe,landing.add(0,4,0),true);game.items().timeStop().begin(level,20);stopped=true;boundary=now;landingSounds=sounds(4,1).size();next();return false;
            }
            if(stage==6){require(!falling.onGround()&&sounds(4,1).size()==landingSounds,"actual Time Stop pauses physical landing");if(now-boundary<3)return false;game.items().timeStop().end(level);stopped=false;falling.entityTags().remove(game.items().epoch());falling.entityTags().add("skywars2.items.fixture_stale_epoch");next();return false;}
            if(stage==7){require(falling.isRemoved()&&service.trackedCount()==0,"stale epoch is quietly discarded by real scheduler");clearActors();noForeignItems();source=item(new ItemStack(Items.DIAMOND,64),landing.add(0,3,0),false);far=item(new ItemStack(Items.EMERALD,32),landing.add(18,3,0),false);exempt=item(new ItemStack(Items.IRON_INGOT,7),landing.add(-2,3,0),false);exempt.entityTags().add("metal_pipe");standing=item(pipe,landing,true);owner.setItemInHand(InteractionHand.OFF_HAND,divider.copy());next();return false;}
            if(stage==8){
                noForeignItems();require(standing.onGround(),"existing PDC pipe lies on real native floor before Divider use");require(input(InteractionHand.OFF_HAND),"real native offhand Divider use accepted");gatherNewItems();require(source.isRemoved()&&far.isRemoved()&&standing.isRemoved()&&!exempt.isRemoved()&&exempt.getItem().getCount()==7,"global full-stack conversion and source entity-tag exclusion");
                var fresh=actors.stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast).filter(item->item!=exempt&&!item.isRemoved()).toList();require(fresh.size()==3,"one new Pipe per whole stack including already lying PDC pipe, no range filter");for(var item:fresh){presentation(item.getItem());require(item.getOwner()==owner,"converted pipe source-owned");item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);}require(fresh.stream().anyMatch(item->item.position().distanceToSqr(source.position())<1e-8)&&fresh.stream().anyMatch(item->item.position().distanceToSqr(far.position())<1e-8),"converted loot uses each original position");for(var item:fresh){item.setPos(item.position().add(0,1,0));item.setOnGround(false);}require(owner.getOffhandItem().getCount()==1&&game.items().mana().get(owner)==mana,"Divider retained and zero mana spent");require(service.cooldownTicks(owner)==300,"native15second cooldown begins at300 active ticks");
                source=item(new ItemStack(Items.DIAMOND,64),landing.add(1,3,0),false);game.items().timeStop().begin(level,20);stopped=true;next();return false;
            }
            if(stage==9){require(service.cooldownTicks(owner)==300&&!source.isRemoved(),"Time Stop pauses Divider active cooldown");if(now-boundary<3)return false;game.items().timeStop().end(level);stopped=false;next();return false;}
            if(stage==10){int elapsed=now-boundary;if(elapsed<300){require(service.cooldownTicks(owner)==300-elapsed,"actual active cooldown tick "+elapsed);if(elapsed==299){input(InteractionHand.OFF_HAND);require(!source.isRemoved(),"cannot convert at active tick299");}return false;}require(elapsed==300&&service.cooldownTicks(owner)==0,"exact300 active-tick expiry");require(input(InteractionHand.OFF_HAND),"real native recast after expiry");gatherNewItems();require(source.isRemoved(),"expiry permits conversion");service.forget(owner);require(service.cooldownTicks(owner)==0,"forget clears only own cooldown");clearActors();game.items().mana().score(owner,"metal_divider_cd").set(2);service.loaded(owner);require(service.cooldownTicks(owner)==40,"native player-load callback rehydrates persisted cooldown");boundary=now;stage=110;return false;}
            if(stage==110){int elapsed=now-boundary;require(service.cooldownTicks(owner)==40-elapsed,"rehydrated clock advances on actual native tick");if(elapsed<2)return false;service.forget(owner);stage=11;return false;}
            if(stage==11){
                // The observer transition is synchronous so no gameplay stage is held across a native tick.
                game.spectators().enter(owner);otherObserver=true;owner.getInventory().clearContent();owner.setItemInHand(InteractionHand.MAIN_HAND,divider.copy());source=item(new ItemStack(Items.DIAMOND,8),landing.add(0,3,0),false);input(InteractionHand.MAIN_HAND);require(!source.isRemoved(),"observer role cannot use global Divider");game.spectators().remove(owner);otherObserver=false;call(saved,"restore",game);call(saved,"restoreAttributes");owner.entityTags().add("obtained_crux");owner.getInventory().clearContent();clearActors();require(service.trackedCount()==0,"no live fixture pipes remain");return true;
            }
            throw new IllegalStateException("Unknown stage "+stage);
        }
        @Override public void close(){if(closed)return;closed=true;var actions=new ArrayList<Runnable>();actions.add(this::gatherNewItems);actions.add(this::clearActors);actions.add(()->{if(stopped)game.items().timeStop().end(level);});actions.add(()->{if(otherObserver)game.spectators().remove(owner);});actions.add(this::unteam);actions.add(()->{if(service!=null){service.forget(owner);write(service,"phase",oldPhase);}});actions.add(()->{for(var entry:terrain.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());terrain.clear();});actions.add(()->{if(configured){game.config().lbcSkills=oldSkills;game.config().offhandWands=oldOffhand;}});actions.add(()->{if(client!=null)call(client,"close");if(audience!=null)call(audience,"close");});actions.add(()->restore(saved));actions.add(()->restore(otherSaved));actions.add(this::restoreClocks);actions.add(()->{if(owner!=null&&!oldCrux)owner.entityTags().remove("obtained_crux");if(other!=null&&!oldOtherCrux)other.entityTags().remove("obtained_crux");});actions.add(()->{if(attribution!=null)attribution.close();});Throwable failure=null;for(var action:actions)try{action.run();}catch(Throwable error){if(failure==null)failure=error;else failure.addSuppressed(error);}if(failure!=null)throw new IllegalStateException("Metal Pipe scoped restoration",failure);}
        @SuppressWarnings("unchecked") Map<ServerPlayer,Integer> clocks(){return (Map<ServerPlayer,Integer>)read(service,"timers");}
        void restoreClocks(){if(!clockCaptured)return;var clocks=clocks();clocks.remove(owner);clocks.remove(other);if(savedTimer!=null)clocks.put(owner,savedTimer);if(otherSavedTimer!=null)clocks.put(other,otherSavedTimer);owner.getStats().setValue(owner,net.minecraft.stats.Stats.ITEM_CRAFTED.get(Items.CARROT_ON_A_STICK),craftedBefore);}
        void restore(Object snapshot){if(snapshot!=null){call(snapshot,"restore",game);call(snapshot,"restoreAttributes");call(snapshot,"restoreUse");call(snapshot,"restoreProfile",game);}}
    }
    private static void begin(GameController game,boolean probe){require(running==null,"exclusive asynchronous fixture");running=new Fixture(game,probe);try{running.initialize();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}throw error;}}
    public static void run(GameController game){begin(game,false);}
    public static void stopProbe(GameController game){require(stopWitness==null,"exclusive actual-stop witness");begin(game,true);}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){boolean probe=running.probe;running.close();running=null;SkyWars2.LOGGER.info(probe?"[check] PASS metal-pipe-stop-probe":"[check] PASS metal-pipe (exact native loot/PDC; natural landing/raw25/AABB/causal origin; native pickup20/rearm; landing+melee sound packets; passive physical gates/allies/Greed; native craft and offhand Divider whole-stack/global conversion; exact300 active ticks/Time Stop/forget/role/epoch; scoped restoration)");}}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL metal-pipe",error);}}
    public static void cleanup(GameController game){if(running!=null){var old=running;running=null;old.close();}try{if(stopWitness!=null)require(!game.playing()&&!game.items().currentEpoch(stopEpoch)&&stopWitness.isRemoved()&&game.items().metalPipe().trackedCount()==0,"actual normal stop discards source-owned physical pipe and invalidates epoch");}finally{if(stopWitness!=null)stopWitness.discard();stopWitness=null;stopEpoch=null;}SkyWars2.LOGGER.info("[check] PASS metal-pipe-cleanup");}
}
