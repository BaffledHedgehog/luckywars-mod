package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/** Synchronous native item/service callbacks and captured packets, not an audio playback test. */
public final class ItemSoundAuditChecks {
    private ItemSoundAuditChecks() { }
    private static void require(boolean value,String why) {
        if(!value) throw new IllegalStateException("Item sounds: "+why);
    }
    private static final class Recording extends Connection {
        final ServerPlayer player;final List<Packet<?>> packets=new ArrayList<>();
        Recording(ServerPlayer player){super(PacketFlow.SERVERBOUND);this.player=player;}
        @Override public boolean isMemoryConnection(){return true;}
        @Override public boolean isConnected(){return true;}
        @Override public PacketListener getPacketListener(){return player.connection;}
        @Override public void send(Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){packets.add(packet);}
        @Override public void flushChannel(){ }
        List<ClientboundSoundPacket> sounds(String id){
            return packets.stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast)
                    .filter(p->p.getSound().value().location().toString().equals(id)).toList();
        }
    }
    private static final class Swap implements AutoCloseable {
        final ServerPlayer player;final ServerGamePacketListenerImpl previous;final Recording recording;
        Swap(GameController game,ServerPlayer player){
            this.player=player;previous=player.connection;recording=new Recording(player);
            player.connection=new ServerGamePacketListenerImpl(game.server(),recording,player,
                    new CommonListenerCookie(player.getGameProfile(),0,player.clientInformation(),false));
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        public void close(){player.connection=previous;previous.resetPosition();}
    }
    private static void sound(ClientboundSoundPacket p,float volume,float pitch,Vec3 at){
        float cap=SoundNormalization.volumeCap(p.getSound().value().location());
        if(cap<1)volume=Math.min(volume,cap);
        require(p.getSource()==SoundSource.MASTER&&p.getVolume()==volume&&p.getPitch()==pitch,
                "MASTER volume"+volume+" pitch"+pitch+" for "+p.getSound().value().location());
        require(Math.abs(p.getX()-at.x)<=.125&&Math.abs(p.getY()-at.y)<=.125&&Math.abs(p.getZ()-at.z)<=.125,
                "source position / minimum relocation retains native eighth-block quantization");
    }
    private static void move(ServerPlayer p,Vec3 at){
        p.teleportTo(p.level(),at.x,at.y,at.z,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);
    }
    private static boolean use(ServerPlayer p){return p.getMainHandItem().getItem().use(p.level(),p,InteractionHand.MAIN_HAND).consumesAction();}
    private static void clear(Recording a,Recording b){a.packets.clear();b.packets.clear();}
    private static Field field(Class<?> type,String name){
        try{var f=type.getDeclaredField(name);f.setAccessible(true);return f;}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static Object read(Object target,String name){
        try{return field(target.getClass(),name).get(target);}catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    private static void write(Object target,String name,Object value){
        try{field(target.getClass(),name).set(target,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    private static void dispatch(LbcHelicopterEncounter service,Object pair,long parity){
        try{Method m=LbcHelicopterEncounter.class.getDeclaredMethod("dispatch",pair.getClass(),long.class);m.setAccessible(true);m.invoke(service,pair,parity);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("native encounter phase dispatch",e);}
    }
    private static Object copyMaps(Object value){
        if(value instanceof Map<?,?> map){var copy=new LinkedHashMap<Object,Object>();map.forEach((k,v)->copy.put(k,copyMaps(v)));return copy;}
        return value;
    }
    private static Map<Field,Object> maps(Object target){
        var saved=new LinkedHashMap<Field,Object>();
        try{for(var f:target.getClass().getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())&&Map.class.isAssignableFrom(f.getType())){
            f.setAccessible(true);saved.put(f,copyMaps(f.get(target)));
        }}catch(IllegalAccessException e){throw new IllegalStateException(e);}return saved;
    }
    private static Object copyProfile(Object value){
        if(value instanceof int[] a)return a.clone();if(value instanceof Set<?> s)return new HashSet<>(s);return value;
    }
    private static Map<Field,Object> profile(Object target){
        var saved=new LinkedHashMap<Field,Object>();
        try{for(var f:target.getClass().getFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers()))saved.put(f,copyProfile(f.get(target)));}
        catch(IllegalAccessException e){throw new IllegalStateException(e);}return saved;
    }
    private static void restoreProfile(Object target,Map<Field,Object> saved){
        try{for(var e:saved.entrySet())e.getKey().set(target,copyProfile(e.getValue()));}catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    @SuppressWarnings("unchecked")
    private static void restoreMaps(Object target,Map<Field,Object> saved){
        try{for(var entry:saved.entrySet()){
            var current=(Map<Object,Object>)entry.getKey().get(target);current.clear();current.putAll((Map<Object,Object>)copyMaps(entry.getValue()));
        }}catch(IllegalAccessException e){throw new IllegalStateException(e);}
    }
    private static void ender(GameController game,ServerPlayer owner,ServerPlayer viewer,Vec3 origin,Recording a,Recording b){
        String id="minecraft:entity.enderman.teleport";
        for(int distance:List.of(15,17,30)){
            move(owner,origin);move(viewer,origin.add(distance,0,0));
            var wand=game.lbc().rollTable(owner.level(),origin,owner,"lbc:ported/ender_wand").getFirst();
            owner.setItemInHand(InteractionHand.MAIN_HAND,wand);game.items().mana().set(owner,100000);
            clear(a,b);require(use(owner),"actual Ender native item input accepts cast");
            var heard=b.sounds(id);
            require(heard.size()==(distance==15?2:0),"Ender selector16 remains independent of volume2 at distance"+distance);
            for(var p:heard)sound(p,2,1,origin);
            require(a.sounds(id).stream().anyMatch(p->p.getVolume()==200),"Ender private caster teleport cue remains");
        }
        owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);move(owner,origin);
    }
    private static void exahoming(GameController game,ServerPlayer owner,ServerPlayer viewer,Vec3 origin,Recording a,Recording b){
        try{
            for(int distance:List.of(5,20)){
                move(owner,origin);move(viewer,origin.add(distance,0,0));owner.setXRot(-90);
                owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcWands.EXAHOMING));game.items().mana().set(owner,100000);
                clear(a,b);require(use(owner),"native ordinary Exahoming paid input");
                var own=a.sounds("minecraft:entity.firework_rocket.shoot");var heard=b.sounds("minecraft:entity.firework_rocket.shoot");
                require(own.size()==1&&heard.size()==(distance==5?1:0),"Exahoming source shoot event and native range16");
                sound(own.getFirst(),1,1,origin);if(!heard.isEmpty())sound(heard.getFirst(),1,1,origin);
                game.items().homing().stop();
            }
        }finally{game.items().homing().stop();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setXRot(0);}
    }
    private static void acid(GameController game,ServerPlayer owner,ServerPlayer target,Vec3 origin,Recording a,Recording b){
        move(owner,origin);move(target,origin.add(0,0,4));
        var stack=new ItemStack(LbcWands.ACADEMY_ACID_CORRUPTION);
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putInt("mode",3);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
        game.items().mana().set(owner,100000);clear(a,b);
        require(use(owner)&&game.items().acidCorruption().deathRemaining(target)==120,"actual Academy AcidCorrupt mode3 ray afflicts native target");
        var packets=b.sounds("minecraft:ambient.cave");
        require(packets.size()==4&&a.sounds("minecraft:ambient.cave").isEmpty(),"four cave cues are private to afflicted target");
        for(var packet:packets)sound(packet,1,1,target.position());
        game.items().acidCorruption().clear(target);target.removeAllEffects();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    }
    private static void barrier(GameController game,ServerPlayer owner,ServerPlayer viewer,Vec3 origin,Recording a,Recording b){
        var service=game.items().defensiveArcanums();var mana=game.items().mana();String id="minecraft:block.beacon.activate";
        move(owner,origin);move(viewer,origin.add(4,0,0));mana.score(owner,"manaregen_visual").set(1);mana.set(owner,10000);
        owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcDefensiveArcanums.BARRIER));
        clear(a,b);service.tickPlayer(owner);
        require(service.activeHands(owner)==1&&a.sounds(id).size()==1&&b.sounds(id).size()==1,"first paid hand activates one beacon cue");
        sound(a.sounds(id).getFirst(),1,1,origin);
        clear(a,b);service.tickPlayer(owner);service.tickPlayer(owner);
        require(a.sounds(id).isEmpty()&&b.sounds(id).isEmpty(),"paid barrier upkeep never repeats activation cue");
        owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(LbcDefensiveArcanums.BARRIER));
        clear(a,b);service.tickPlayer(owner);
        require(service.activeHands(owner)==2&&a.sounds(id).size()==1,"new second hand activates exactly once");
        owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);service.tickPlayer(owner);
        owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcDefensiveArcanums.BARRIER));clear(a,b);service.tickPlayer(owner);
        require(a.sounds(id).size()==1,"unequip then re-equip resets only that hand activation");
        mana.set(owner,0);clear(a,b);service.tickPlayer(owner);
        require(service.activeHands(owner)==0&&a.sounds(id).isEmpty(),"failed payment clears latches silently");
        mana.set(owner,10000);clear(a,b);service.tickPlayer(owner);
        require(service.activeHands(owner)==2&&a.sounds(id).size()==2,"both reset hands reactivate with two cues after payment resumes");
        owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);service.forget(owner);
    }
    private static void tanner(GameController game,ServerPlayer owner,ServerPlayer viewer,Vec3 origin,Recording a,Recording b,
            Map<BlockPos,MapManager.BlockSnapshot> blocks){
        var level=owner.level();var service=game.items().tanner();move(owner,origin);move(viewer,origin.add(4,0,0));
        var at=BlockPos.containing(origin.add(2,1,1));blocks.putIfAbsent(at,game.maps().snapshot(at));
        require(level.getBlockState(at).isAir(),"owned Tanner conversion cell initially air");
        var arrow=new Arrow(level,owner,new ItemStack(Items.ARROW),null);arrow.setPos(Vec3.atCenterOf(at).add(.21,-.34,.12));arrow.setNoGravity(true);
        try{
            require(level.addFreshEntity(arrow),"real native Arrow admitted for Tanner conversion");
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcTanner.ITEM));game.items().mana().set(owner,500);
            clear(a,b);service.tick();
            require(arrow.isRemoved()&&level.getBlockState(at).is(Blocks.DIRT),"actual Tanner service pulse converts projectile into dirt");
            var heard=a.sounds("minecraft:block.rooted_dirt.break");
            require(heard.size()==1&&b.sounds("minecraft:block.rooted_dirt.break").size()==1,"native conversion emits one cue to each nearby listener");
            sound(heard.getFirst(),1,1,Vec3.atCenterOf(at).add(.21,-.34,.12));
        }finally{
            arrow.discard();owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            for(int i=0;i<40&&(service.count()>0||service.pendingCount()>0);i++)service.tick();
            require(service.count()==0&&service.pendingCount()==0,"owned Tanner work retires before fixture cleanup");
        }
    }
    private static void helicopter(GameController game,ServerPlayer owner,ServerPlayer viewer,Vec3 origin,Recording a,Recording b){
        var level=owner.level();var service=game.items().helicopterEncounter();
        var token=new ItemEntity(level,origin.x,origin.y-20,origin.z-50,new ItemStack(LbcHelicopterTriggerItem.TOKEN));
        token.setNoGravity(true);token.setYRot(0);token.setXRot(0);token.setPickUpDelay(1000);
        try{
            require(level.hasChunkAt(BlockPos.containing(token.position()))&&level.addFreshEntity(token),"resident native encounter token");
            require(service.spawn(token)&&service.activeCount()==1,"actual encounter spawn creates owned test pair");
            Object pair=((Collection<?>)read(service,"pairs")).iterator().next();
            var view=service.snapshot().getFirst();var display=view.display();var hitbox=view.hitbox();
            for(int distance:List.of(40,149,151)){
                move(owner,origin.add(0,0,12));move(viewer,origin.add(distance,0,0));
                boolean bomb=false;
                for(int attempt=0;attempt<200&&!bomb;attempt++){
                    display.setPos(origin);write(pair,"phase",3);hitbox.hurtTime=10;clear(a,b);dispatch(service,pair,0);
                    var hurt=b.sounds("minecraft:entity.blaze.hurt");
                    require(hurt.size()==(distance<=150?1:0),"encounter hurt selector150 at"+distance);
                    if(!hurt.isEmpty())sound(hurt.getFirst(),1,1.5f,viewer.position().add(-2,0,0));
                    var own=a.sounds("minecraft:lbcsounds.drop_bomb");bomb=!own.isEmpty();
                    if(bomb){
                        sound(own.getFirst(),.5f,1,origin);
                        var heard=b.sounds("minecraft:lbcsounds.drop_bomb");
                        require(heard.size()==(distance<=150?1:0),"encounter phase3 selector150 at"+distance);
                        if(!heard.isEmpty())sound(heard.getFirst(),.5f,1,viewer.position().add(-2,0,0));
                    }
                    for(var attack:service.attackSnapshot()){attack.discard();service.entityUnloaded(attack);}
                }
                require(bomb,"real phase3 probability produced cue within bounded200 dispatches");
            }
            for(long parity:List.of(0L,1L))for(double distance:List.of(79.5,80.5)){
                display.setPos(origin);write(pair,"phase",1);hitbox.hurtTime=0;
                // Caster eyes exactly face along +Z; beta remains farther than the intended victim.
                move(owner,origin.add(0,-owner.getEyeHeight(),20));
                var context=origin.add(0,0,8);move(viewer,context.add(-distance,0,0));
                clear(a,b);dispatch(service,pair,parity);
                Vec3 at=context.add(parity==0?2:-2,0,0);
                var local=a.sounds("minecraft:entity.firework_rocket.blast");var heard=b.sounds("minecraft:entity.firework_rocket.blast");
                require(local.size()==1&&heard.size()==(distance<80?1:0),"shot context selector80 and deterministic parity"+parity+" at"+distance);
                sound(local.getFirst(),1,parity==0?2:1.8f,at);
                if(!heard.isEmpty())sound(heard.getFirst(),.5f,parity==0?2:1.8f,
                        viewer.position().add(at.subtract(viewer.position()).normalize().scale(2)));
                for(var attack:service.attackSnapshot()){attack.discard();service.entityUnloaded(attack);}
            }
            clear(a,b);service.stop();
            require(service.activeCount()==0&&service.attackSnapshot().isEmpty(),"actual encounter stop removes test actors");
            for(var recording:List.of(a,b))for(String id:List.of("minecraft:lbcsounds.helicopter","minecraft:lbcsounds.kazahstan"))
                require(recording.packets.stream().anyMatch(p->p instanceof ClientboundStopSoundPacket stop&&stop.getSource()==null
                        &&stop.getName()!=null&&stop.getName().toString().equals(id)),"encounter stop sends all-category stop for"+id);
        }finally{
            token.discard();service.entityUnloaded(token);service.stop();
        }
    }
    private static void clocks(GameController game,ServerPlayer owner,ServerPlayer viewer,Vec3 origin,Recording a,Recording b){
        var service=game.items().timeStop();move(owner,origin);move(viewer,origin.add(60,0,0));
        owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcStand.CLOCKS));
        clear(a,b);
        try{
            require(use(owner)&&service.stopped(owner.level()),"actual Stand clock item input begins native Time Stop");
            var own=a.sounds("minecraft:lbcsounds.zawardo");var heard=b.sounds("minecraft:lbcsounds.zawardo");
            require(own.size()==1&&heard.size()==1,"clock cue reaches caster and listener60 blocks away");
            sound(own.getFirst(),1,1,origin);sound(heard.getFirst(),.5f,1,viewer.position().add(-2,0,0));
            clear(a,b);require(!use(owner),"clock cooldown rejects immediate second activation");
            require(a.sounds("minecraft:lbcsounds.zawardo").isEmpty()&&b.sounds("minecraft:lbcsounds.zawardo").isEmpty(),"rejected clock input emits no cue");
            service.tick();move(owner,origin.add(.5,0,0));service.tick();move(owner,origin.add(2,0,0));service.tick();
            clear(a,b);service.end(owner.level());
            var impacts=a.sounds("minecraft:entity.lightning_bolt.impact");
            require(impacts.size()==2&&b.sounds("minecraft:entity.lightning_bolt.impact").isEmpty(),"sparse Time Stop trail restores two cues, distant frozen listener hears none");
            sound(impacts.get(0),.1f,2,origin);sound(impacts.get(1),.1f,2,origin.add(2,0,0));
        }finally{service.end(owner.level());service.forget(owner);owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
        for(int distance:List.of(15,17)){
            move(owner,origin);move(viewer,origin.add(0,0,distance));owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(LbcStand.CLOCKS));
            try{
                require(use(owner),"fresh actual clock cast for restore audience"+distance);
                service.tick();move(owner,origin.add(.5,0,0));service.tick();move(owner,origin.add(2,0,0));service.tick();
                // Native extension keeps the already sampled positions and does not duplicate them.
                service.begin(owner.level(),30);clear(a,b);service.end(owner.level());
                var impacts=b.sounds("minecraft:entity.lightning_bolt.impact");
                require(impacts.size()==(distance==15?2:0),"restore selector16 / no minimum volume at"+distance);
                if(distance==15){sound(impacts.get(0),.1f,2,origin);sound(impacts.get(1),.1f,2,origin.add(2,0,0));}
            }finally{service.end(owner.level());service.forget(owner);owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
        }
    }
    public static void run(GameController game){
        var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        var viewer=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
        require(game.playing()&&owner!=null&&viewer!=null&&game.allowDamage(owner)&&game.allowDamage(viewer),"two real match participants");
        var level=game.maps().level();var mana=game.items().mana();
        require(owner.level()==level&&viewer.level()==level&&!game.items().actionsBlocked(owner)&&!game.items().magic().suppressed(owner),"unblocked participants in match world");
        require(owner.containerMenu==owner.inventoryMenu&&viewer.containerMenu==viewer.inventoryMenu,"initial inventory menus");
        require(game.items().tanner().count()==0&&game.items().tanner().pendingCount()==0&&game.items().helicopterEncounter().activeCount()==0
                &&game.items().helicopterEncounter().attackSnapshot().isEmpty(),"exclusive Tanner / encounter fixture");
        require(!game.items().timeStop().stopped(level)&&game.items().timeStop().cooldownTicks(owner)==0
                &&game.items().timeStop().pendingDamageCount()==0,"fresh clocks without pending stopped damage");
        require(((Map<?,?>)read(game.items().timeStop(),"stops")).isEmpty()&&((Map<?,?>)read(game.items().timeStop(),"cooldowns")).isEmpty(),
                "exclusive global clock service before explicit callbacks");
        for(var p:List.of(owner,viewer))require(game.items().defensiveArcanums().activeHands(p)==0&&game.items().acidCorruption().deathRemaining(p)==0
                &&!game.items().frost().isFrozen(p),"no pre-existing barrier, delayed death or frost state");
        var origin=Vec3.atBottomCenterOf(new BlockPos(owner.blockPosition().getX(),280,owner.blockPosition().getZ()));
        for(var p:BlockPos.betweenClosed(BlockPos.containing(origin).offset(-6,-6,-6),BlockPos.containing(origin).offset(6,8,6)))
            require(level.hasChunkAt(p)&&level.getBlockState(p).isAir(),"resident empty high-altitude fixture volume");
        require(level.getEntities((Entity)null,new AABB(origin,origin).inflate(8),e->e!=owner&&e!=viewer).isEmpty(),"no foreign actors in native ray/conversion fixture");
        var saved=new GameChecks.WandThirdPlayerState(owner,mana);var otherSaved=new GameChecks.WandThirdPlayerState(viewer,mana);
        var indirectMaps=maps(game.items().indirectKills());
        var ownerProfile=game.players().get(owner.getUUID());var savedProfile=profile(ownerProfile);
        var customStats=new LinkedHashMap<net.minecraft.stats.Stat<net.minecraft.resources.Identifier>,Integer>();
        for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){
            var stat=net.minecraft.stats.Stats.CUSTOM.get(id);customStats.put(stat,owner.getStats().getValue(stat));
        }
        int regen=mana.regeneration(owner),fall=mana.score(owner,"falltimer").get(),death=mana.score(viewer,"delayed_death").get();
        boolean offhand=game.config().offhandWands,shift=owner.isShiftKeyDown(),otherShift=viewer.isShiftKeyDown();
        var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());level.getAllEntities().forEach(before::add);
        var advancements=new LinkedHashMap<net.minecraft.advancements.AdvancementHolder,Set<String>>();
        for(var adv:game.server().getAdvancements().getAllAdvancements()){
            var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(adv).getCompletedCriteria().forEach(done::add);advancements.put(adv,done);
        }
        try(var ownSwap=new Swap(game,owner);var otherSwap=new Swap(game,viewer)){
            game.config().offhandWands=true;
            for(var p:List.of(owner,viewer)){
                p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.containerMenu.setCarried(ItemStack.EMPTY);
                p.getInventory().setSelectedSlot(0);p.setNoGravity(true);p.setShiftKeyDown(false);p.removeAllEffects();mana.noMagic(p,0);
                game.items().magic().inventoryChanged(p);
            }
            ender(game,owner,viewer,origin,ownSwap.recording,otherSwap.recording);
            exahoming(game,owner,viewer,origin,ownSwap.recording,otherSwap.recording);
            acid(game,owner,viewer,origin,ownSwap.recording,otherSwap.recording);
            barrier(game,owner,viewer,origin,ownSwap.recording,otherSwap.recording);
            tanner(game,owner,viewer,origin,ownSwap.recording,otherSwap.recording,blocks);
            helicopter(game,owner,viewer,origin,ownSwap.recording,otherSwap.recording);
            clocks(game,owner,viewer,origin,ownSwap.recording,otherSwap.recording);
            SkyWars2.LOGGER.info("[check] PASS item-sounds (native Ender audience16; ordinary Exahoming shoot1/1; AcidCorrupt four private caves; paid-hand barrier activation lifecycle; native Tanner conversion MASTER; encounter phase/hurt/shot selector and minimum geometry, stop packets; native clock cue1/1 minimum.5 and sparse restore impact audience16)");
        }finally{
            game.items().defensiveArcanums().forget(owner);game.items().acidCorruption().clear(viewer);
            game.items().timeStop().end(level);game.items().timeStop().forget(owner);
            var after=new ArrayList<Entity>();level.getAllEntities().forEach(after::add);
            for(var entity:after)if(!before.contains(entity)&&entity!=owner&&entity!=viewer)entity.discard();
            blocks.forEach((pos,snapshot)->game.maps().restoreSnapshot(pos,snapshot));
            saved.restore(game);otherSaved.restore(game);owner.setShiftKeyDown(shift);viewer.setShiftKeyDown(otherShift);game.config().offhandWands=offhand;
            mana.score(owner,"manaregen_visual").set(regen);mana.score(owner,"falltimer").set(fall);mana.score(viewer,"delayed_death").set(death);
            advancements.forEach((adv,done)->{for(String key:adv.value().criteria().keySet())if(done.contains(key))owner.getAdvancements().award(adv,key);else owner.getAdvancements().revoke(adv,key);});
            restoreProfile(ownerProfile,savedProfile);customStats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));
            game.items().magic().inventoryChanged(owner);game.items().magic().inventoryChanged(viewer);
            restoreMaps(game.items().indirectKills(),indirectMaps);
        }
    }
}
