package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native collision and item input; the real item scheduler advances psychic-disorder ticks. */
public final class WeaponSuppressionChecks {
    private WeaponSuppressionChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Weapon suppression: "+why);}
    private static java.lang.reflect.Field field(Object target,String name){for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException ignored){}throw new IllegalStateException("Missing field "+name);}
    @SuppressWarnings("unchecked") private static Map<Object,Object> map(Object target,String name){try{return (Map<Object,Object>)field(target,name).get(target);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static Object read(Object target,String name){try{return field(target,name).get(target);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Object target,String name,Object value){try{field(target,name).set(target,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static final class Wire extends net.minecraft.network.Connection {
        net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket latest;
        Wire(){super(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);}
        @Override public boolean isConnecting(){return false;}@Override public boolean isConnected(){return true;}@Override public boolean isMemoryConnection(){return true;}
        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener,boolean flush){if(packet instanceof net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket pos)latest=pos;}
        @Override public void flushChannel(){}
    }
    private static final class Client implements AutoCloseable {
        final GameController game;final ServerPlayer owner;final net.minecraft.server.network.ServerGamePacketListenerImpl previous,listener;final Wire wire=new Wire();
        Client(GameController game,ServerPlayer owner){this.game=game;this.owner=owner;previous=owner.connection;listener=new net.minecraft.server.network.ServerGamePacketListenerImpl(game.server(),wire,owner,new net.minecraft.server.network.CommonListenerCookie(owner.getGameProfile(),0,owner.clientInformation(),false));set(wire,"packetListener",listener);owner.connection=listener;listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());listener.teleport(owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot());ack();game.server().getConnection().getConnections().add(wire);}
        void ack(){listener.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());if(read(listener,"awaitingPositionFromClient")!=null){var packet=Objects.requireNonNull(wire.latest);listener.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(packet.id(),owner.getX(),owner.getY(),owner.getZ(),owner.getYRot(),owner.getXRot()));}require(listener.hasClientLoaded()&&!owner.isChangingDimension(),"real native load and teleport acknowledgement");}
        int updates(){return (Integer)read(listener,"tickCount");}
        @Override public void close(){game.server().getConnection().getConnections().remove(wire);owner.connection=previous;}
    }
    private static final class Fixture implements AutoCloseable {
        final GameController game;final ElementalGrimoireChecks.Fixture base;final ServerPlayer owner;final Vec3 at;final String epoch;
        final SauvojenMeditationChecks.Attribution attribution;
        final Map<BlockPos,MapManager.BlockSnapshot> cells=new LinkedHashMap<>();final Set<Long> forced=new LinkedHashSet<>(),wanted=new LinkedHashSet<>();
        final Map<net.minecraft.world.scores.Objective,Integer> scores=new LinkedHashMap<>();final Map<net.minecraft.stats.Stat<?>,Integer> stats=new LinkedHashMap<>();
        final Map<net.minecraft.advancements.AdvancementHolder,Set<String>> advancements=new LinkedHashMap<>();final Set<String> unlocks;
        final Map<Object,Object> curses,rotations;final Object oldCurse,oldRotation;final boolean hadCurse,hadRotation;
        final int oldCooldown;final boolean skills;final Vec3 knownMovement;final Set<Entity> created=Collections.newSetFromMap(new IdentityHashMap<>());Client client;Marker marker;BlockPos beacon;Cow witness;boolean ready,closed;int warm,stage=-1,expected;float beforeHealth;
        Fixture(GameController game){
            this.game=game;base=new ElementalGrimoireChecks.Fixture(game);owner=base.owner;epoch=game.items().epoch();knownMovement=owner.getKnownMovement();
            attribution=new SauvojenMeditationChecks.Attribution(game,List.of(owner));skills=game.config().lbcSkills;
            unlocks=new HashSet<>(game.players().get(owner.getUUID()).lbcUnlocks);oldCooldown=game.items().shooters().cooldownRemaining(owner);
            for(var objective:game.server().getScoreboard().getObjectives()){var value=game.server().getScoreboard().getPlayerScoreInfo(owner,objective);if(value!=null)scores.put(objective,value.value());}
            for(var id:net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,owner.getStats().getValue(stat));}
            for(var item:List.of(LbcAk47.ITEM,LbcShooters.SLR)){var stat=Stats.ITEM_USED.get(item);stats.put(stat,owner.getStats().getValue(stat));}
            for(var advancement:game.server().getAdvancements().getAllAdvancements()){var done=new HashSet<String>();owner.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(done::add);advancements.put(advancement,done);}
            curses=map(game.items(),"turnCurses");rotations=map(game.items(),"rotations");hadCurse=curses.containsKey(owner.getUUID());hadRotation=rotations.containsKey(owner.getUUID());oldCurse=curses.remove(owner.getUUID());oldRotation=rotations.remove(owner.getUUID());
            base.initialize();game.config().lbcSkills=false;owner.removeAllEffects();owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);owner.setHealth(1000);owner.setAbsorptionAmount(0);owner.getFoodData().setFoodLevel(10);owner.getFoodData().setSaturation(0);
            at=new Vec3(Math.floor(owner.getX())+.5,owner.getY(),Math.floor(owner.getZ())+.5);home();client=new Client(game,owner);
            for(int x=(BlockPos.containing(at).getX()-4)>>4;x<=(BlockPos.containing(at).getX()+4)>>4;x++)for(int z=(BlockPos.containing(at).getZ()-2)>>4;z<=(BlockPos.containing(at).getZ()+66)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);wanted.add(key);if(!owner.level().getForceLoadedChunks().contains(key)){forced.add(key);owner.level().setChunkForced(x,z,true);}owner.level().getChunk(x,z);
            }
        }
        void home(){owner.teleportTo(owner.level(),at.x,at.y,at.z,Set.of(),0,0,true);owner.setDeltaMovement(Vec3.ZERO);owner.setShiftKeyDown(false);if(client!=null)client.ack();}
        void cell(BlockPos pos){cells.computeIfAbsent(pos.immutable(),p->game.maps().snapshot(p));}
        void clearField(){if(marker!=null){game.items().magic().unloaded(marker);marker.discard();marker=null;}if(beacon!=null){owner.level().setBlock(beacon,Blocks.AIR.defaultBlockState(),3);game.items().magic().blockChanged(beacon,owner.level().getBlockState(beacon));beacon=null;}}
        void zone(boolean mechanism,boolean nativeBlock,Vec3 center){
            clearField();beacon=BlockPos.containing(center);cell(beacon);
            if(nativeBlock){var block=mechanism?LbcMagic.MECHANISM_BEACON_BLOCK:LbcMagic.MAGIC_BEACON_BLOCK;owner.level().setBlock(beacon,block.defaultBlockState(),3);block.setPlacedBy(owner.level(),beacon,owner.level().getBlockState(beacon),owner,new ItemStack(mechanism?LbcMagic.MECHANISM_BEACON:LbcMagic.MAGIC_BEACON));game.items().magic().blockChanged(beacon,owner.level().getBlockState(beacon));}
            else{marker=new Marker(EntityTypes.MARKER,owner.level());marker.setPos(center);marker.entityTags().add(mechanism?"stopper":"stopper_magic");require(owner.level().addFreshEntity(marker),"actual legacy field publication");game.items().magic().loaded(marker);}
            require(mechanism?game.items().magic().mechanismSuppressedAt(owner.level(),center):game.items().magic().beaconSuppressed(center),"actual field indexed");
        }
        boolean prepare(){require(++warm<200,"bounded resident entity-chunk readiness");if(wanted.stream().anyMatch(k->!owner.level().areEntitiesLoaded(k))||client.updates()==0)return false;
            require(owner.getAttributeValue(Attributes.ARMOR)==0&&owner.getAttributeValue(Attributes.ARMOR_TOUGHNESS)==0,"actual native player update removed former armor modifiers before positive controls");
            if(witness==null){for(var pos:BlockPos.betweenClosed(BlockPos.containing(at.add(-4,-1,-2)),BlockPos.containing(at.add(4,4,66)))){cell(pos);owner.level().setBlock(pos,Blocks.AIR.defaultBlockState(),2);}witness=new Cow(EntityTypes.COW,owner.level());witness.setNoAi(true);witness.setNoGravity(true);witness.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);witness.setHealth(1000);witness.setPos(at.add(0,1.1,58));require(owner.level().addFreshEntity(witness),"native collision witness publication");warm=0;return false;}
            if(warm<3)return false;return true;
        }
        void resetCow(double distance){witness.setPos(at.add(0,1.1,distance));witness.setHealth(1000);witness.setInvulnerableTime(0);witness.damageCooldownTime=0;witness.hurtTime=0;witness.removeAllEffects();}
        void ray(){game.items().ak47().nativeRay(owner,new Vec3(0,0,1),null,64);}
        void guns(){
            owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            resetCow(58);ray();require(witness.getHealth()==991,"positive control: shared native AK collision hits outside target for9");
            for(boolean nativeBlock:List.of(false,true)){
                zone(true,nativeBlock,at.add(3,1,30));require(!game.items().magic().mechanismSuppressed(owner),"AK shooter starts outside TECH sphere");
                resetCow(58);ray();require(witness.getHealth()==1000,"AK cannot cross TECH field and hit target beyond far boundary native="+nativeBlock);
                resetCow(16);ray();require(witness.getHealth()==1000,"AK cannot hit within TECH field native="+nativeBlock);
                resetCow(4);ray();require(witness.getHealth()==991,"AK keeps collision before first TECH entry native="+nativeBlock);
                var wall=BlockPos.containing(at.add(0,1,16));owner.level().setBlock(wall,Blocks.DIRT.defaultBlockState(),3);resetCow(58);ray();require(owner.level().getBlockState(wall).is(Blocks.DIRT),"TECH clips before native terrain destruction");clearField();ray();require(owner.level().getBlockState(wall).isAir(),"positive control: identical AK ray destroys unjammed terrain");for(var drop:owner.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(wall).inflate(2)))if(!base.before.contains(drop))created.add(drop);
                zone(false,nativeBlock,at.add(3,1,30));resetCow(58);ray();require(witness.getHealth()==991,"MAG leaves shared AK ray active native="+nativeBlock);clearField();
            }
            var source=game.lbc().rollTable(owner.level(),at,owner,"lbc:ported/slr");require(source.size()==1,"actual original SLR source");
            for(boolean nativeBlock:List.of(false,true)){
                home();owner.setItemInHand(InteractionHand.MAIN_HAND,source.getFirst().copy());owner.getInventory().setItem(9,new ItemStack(Items.ARROW,3));game.items().shooters().resetCooldown(owner);zone(false,nativeBlock,at.add(3,0,0));
                require(base.input(InteractionHand.MAIN_HAND)&&owner.getInventory().getItem(9).getCount()==2&&game.items().shooters().cooldownRemaining(owner)==15,"native SLR item fires under MAG with normal ammo/cooldown native="+nativeBlock);
                home();game.items().shooters().resetCooldown(owner);zone(true,nativeBlock,at.add(3,0,0));int arrows=owner.getInventory().getItem(9).getCount();
                require(!base.input(InteractionHand.MAIN_HAND)&&owner.getInventory().getItem(9).getCount()==arrows&&game.items().shooters().cooldownRemaining(owner)==0&&owner.getXRot()==0,"TECH rejects native SLR before ammo/cooldown/recoil native="+nativeBlock);clearField();
            }
            owner.getInventory().clearContent();home();witness.discard();
        }
        void armCurse(int variant){clearField();home();owner.removeAllEffects();owner.setHealth(1000);owner.setAbsorptionAmount(0);owner.setInvulnerableTime(0);owner.damageCooldownTime=0;curses.remove(owner.getUUID());rotations.remove(owner.getUUID());
            if(variant<4)zone((variant&1)!=0,variant>=2,at.add(3,0,0));
            var broken=new ItemStack(Items.TOTEM_OF_UNDYING);var flags=new net.minecraft.nbt.CompoundTag();flags.putInt("broken_totem",1);broken.set(DataComponents.CUSTOM_DATA,CustomData.of(flags));game.items().usedTotem(owner,broken);
            require(game.items().curseTicks(owner)==100,"actual broken-totem reward arms psychic disorder variant="+variant);owner.removeEffect(MobEffects.BLINDNESS);expected=100;stage=variant*2;beforeHealth=owner.getHealth();
        }
        boolean step(){require(game.playing()&&game.items().currentEpoch(epoch),"exclusive live match fixture");client.ack();
            if(!ready){if(!prepare())return false;guns();ready=true;armCurse(0);return false;}
            require(game.items().curseTicks(owner)==--expected,"actual scheduler advances disorder through jammer variant="+(stage/2));
            if(stage%2==0){owner.setInvulnerableTime(0);owner.damageCooldownTime=0;owner.setYRot(25);beforeHealth=owner.getHealth();stage++;return false;}
            require(owner.getHealth()==beforeHealth-1&&owner.hasEffect(MobEffects.BLINDNESS)&&owner.getEffect(MobEffects.BLINDNESS).getAmplifier()==0,"actual native rotation damage and Blindness under jammer variant="+(stage/2));
            int next=stage/2+1;if(next>=5)return true;armCurse(next);return false;
        }
        @Override public void close(){if(closed)return;closed=true;
            try{clearField();if(witness!=null){game.items().indirectKills().clear(witness);witness.discard();}for(var entity:created)entity.discard();curses.remove(owner.getUUID());rotations.remove(owner.getUUID());
                for(var entry:cells.entrySet())game.maps().restoreSnapshot(entry.getKey(),entry.getValue());for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);
                if(client!=null)client.close();base.retain=true;base.close();owner.setKnownMovement(knownMovement);attribution.close();game.config().lbcSkills=skills;game.items().shooters().setCooldown(owner,oldCooldown);
                if(hadCurse)curses.put(owner.getUUID(),oldCurse);if(hadRotation)rotations.put(owner.getUUID(),oldRotation);
                for(var objective:game.server().getScoreboard().getObjectives())if(scores.containsKey(objective))game.server().getScoreboard().getOrCreatePlayerScore(owner,objective).set(scores.get(objective));else game.server().getScoreboard().resetSinglePlayerScore(owner,objective);
                stats.forEach((stat,value)->owner.getStats().setValue(owner,stat,value));advancements.forEach((a,done)->{for(var key:a.value().criteria().keySet())if(done.contains(key))owner.getAdvancements().award(a,key);else owner.getAdvancements().revoke(a,key);});var profile=game.players().get(owner.getUUID());profile.lbcUnlocks.clear();profile.lbcUnlocks.addAll(unlocks);
            }finally{owner.connection.resetPosition();}
        }
    }
    private static Fixture running;
    public static void run(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);}
    public static void tick(GameController game){if(running==null)return;try{if(running.step()){running.close();running=null;SkyWars2.LOGGER.info("[check] PASS weapon-suppression (AK first TECH boundary/entity/terrain/MAG controls; native SLR MAG-vs-TECH input; actual psychic-disorder timer/damage under both jammer providers)");}}catch(Throwable failure){try{running.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL weapon-suppression",failure);}}
    public static void stop(GameController game){if(running!=null){try{running.close();}finally{running=null;}}}
}
