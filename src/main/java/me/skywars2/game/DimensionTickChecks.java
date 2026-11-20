package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.skywars2.SkyWars2;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Actual ordinary-dimension player ticks, native fire immunity, End escalation and emitted HUD packets. */
public final class DimensionTickChecks {
    private DimensionTickChecks(){}
    private static Fixture running;
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Dimension ticks: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final int oldEnd,oldVoid,oldTotems,oldEye;
        int phase,when,began,endCount;float hp;boolean closed;
        final List<Cow> eyeWitnesses=new ArrayList<>();
        Cow visible,observer,foreign;
        ServerLevel eyePause;
        int eyePhase,eyeWhen,eyeOwnedAt,eyeOtherAt,eyeMark,pausedEyeTime;
        Fixture(GameController game){this.game=game;f=new WoolBucketChecks.Fixture(game,false);
            oldTotems=f.owner.getStats().getValue(net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
            oldEnd=game.items().mana().score(f.owner,"end_damage").get();oldVoid=game.items().mana().score(f.owner,"void_resistance").get();
            oldEye=eyeTime();}
        void move(ServerLevel level){
            // Explicit fixture bootstrap, not a gameplay synchronous chunk-loading path.
            var packets=(java.util.List<?>)WoolBucketChecks.read(f.client.connection,"packets");int before=packets.size();
            var at=new Vec3(f.home.x,200,f.home.z);level.getChunkAt(net.minecraft.core.BlockPos.containing(at));
            require(f.owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"actual native dimension transfer");
            f.owner.setNoGravity(true);f.owner.setDeltaMovement(Vec3.ZERO);f.client.acknowledge();
            game.items().invalidateHud(f.owner);game.items().refreshHud(f.owner);
            require(game.items().mana().visible(f.owner)&&packets.subList(before,packets.size()).stream().anyMatch(ClientboundSetActionBarTextPacket.class::isInstance),"native actionbar remains admitted and emits a packet in "+level.dimension());
        }
        void setup(){
            f.prepareParticipants();f.owner.removeAllEffects();f.owner.getInventory().clearContent();
            game.items().mana().score(f.owner,"void_resistance").set(0);game.items().mana().score(f.owner,"end_damage").set(3);
            f.owner.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);f.owner.setHealth(f.owner.getMaxHealth());
            f.owner.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
            require(f.owner.isAlive()&&f.owner.getHealth()==f.owner.getMaxHealth()&&f.owner.getHealth()>10,"native totem-protected full-health fire witness: health="+f.owner.getHealth()+", maximum="+f.owner.getMaxHealth());
            f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
            f.owner.getFoodData().setFoodLevel(10);began=when=game.server().getTickCount();hp=f.owner.getHealth();
            move(game.server().getLevel(Level.NETHER));
        }
        int eyeTime(){return game.items().mana().score(f.owner,"all_seeing_eye").get();}
        void eyeTime(int ticks){game.items().mana().score(f.owner,"all_seeing_eye").set(ticks);}
        Cow eyeCow(ServerLevel world,Vec3 position,boolean spectator){
            world.getChunkAt(net.minecraft.core.BlockPos.containing(position));
            var cow=new Cow(EntityTypes.COW,world);eyeWitnesses.add(cow);
            cow.setPos(position);cow.setNoAi(true);cow.setNoGravity(true);
            if(spectator)cow.entityTags().add("spectator");
            require(world.addFreshEntity(cow),"real native All-Seeing Eye target is loaded in "+world.dimension());
            return cow;
        }
        void beginEye(){
            var at=f.owner.position();
            visible=eyeCow(f.owner.level(),at.add(0,0,2),false);
            observer=eyeCow(f.owner.level(),at.add(0,0,-2),true);
            foreign=eyeCow(f.level,at.add(0,0,-3),false);
            f.owner.addEffect(ItemEffectLevels.create(MobEffects.NAUSEA,40,0));eyeTime(30);eyeFrame(0);
        }
        void eyeFrame(int next){
            eyePhase=next;eyeWhen=game.server().getTickCount();
            eyeOwnedAt=f.client.packets().size();eyeOtherAt=f.audience.packets().size();
        }
        boolean stepEye(){
            if(game.server().getTickCount()==eyeWhen)return false;
            var packets=tail(f.client,eyeOwnedAt);var other=tail(f.audience,eyeOtherAt);
            if(eyePhase==0){
                eyeMark=eyeMarkAt(packets,visible);
                require(eyeTime()==29&&!f.owner.hasEffect(MobEffects.NAUSEA)&&eyeMark>=0,
                        "ordinary-dimension native eye pulse consumes exactly one charge and privately marks the real target in "+f.owner.level().dimension());
                require(eyeMarkAt(packets,observer)<0&&eyeMarkAt(packets,foreign)<0&&eyeMarkAt(other,visible)<0,
                        "observer and other-world mobs are excluded and the bystander never receives the mark");
                visible.setPos(visible.position().add(.25,.5,.5));eyeFrame(1);return false;
            }
            if(eyePhase==1){
                var expected=markPosition(visible);
                var positions=packets.stream().filter(p->p instanceof ClientboundEntityPositionSyncPacket sync&&sync.id()==eyeMark)
                        .map(ClientboundEntityPositionSyncPacket.class::cast).map(sync->sync.position().endPosition()).toList();
                require(positions.stream().anyMatch(pos->pos.distanceToSqr(expected)<.000001),
                        "same private display follows the mob's new exact position in "+f.owner.level().dimension()
                                +" (id="+eyeMark+", expected="+expected+", received="+positions+", active ticks="+eyeTime()+")");
                require(eyeMarkAt(packets,visible)<0&&other.stream().noneMatch(p->p instanceof ClientboundEntityPositionSyncPacket sync&&sync.id()==eyeMark),
                        "movement neither recreates the marker nor leaks its position to the other world");
                pausedEyeTime=eyeTime();eyePause=f.owner.level();eyeFrame(2);game.items().timeStop().begin(eyePause,20);return false;
            }
            if(eyePhase==2){
                require(eyeTime()==pausedEyeTime&&eyeMarkAt(packets,visible)<0&&removed(packets,eyeMark),
                        "local native Time Stop freezes the Eye clock and retires inactive radar");
                game.items().timeStop().end(eyePause);eyePause=null;eyeFrame(3);return false;
            }
            if(eyePhase==3){
                eyeMark=eyeMarkAt(packets,visible);
                require(eyeTime()==pausedEyeTime-1&&eyeMark>=0,"local Time Stop thaw resumes the same native Eye target");
                eyeFrame(4);visible.discard();return false;
            }
            require(eyeMarkAt(packets,visible)<0&&removed(packets,eyeMark),"native target removal retires its private mark");
            eyeTime(0);observer.discard();foreign.discard();return true;
        }
        private static List<Packet<?>> tail(RegenerationBeltChecks.ScheduledClient client,int from){
            var wire=client.packets();var packets=new ArrayList<Packet<?>>();
            for(int index=from;index<wire.size();index++)flatten(wire.get(index),packets);
            return packets;
        }
        private static void flatten(Packet<?> packet,List<Packet<?>> result){
            if(packet instanceof ClientboundBundlePacket bundle)bundle.subPackets().forEach(child->flatten(child,result));
            else result.add(packet);
        }
        private static Vec3 markPosition(Cow target){return new Vec3(target.getX(),target.getBoundingBox().maxY+.3,target.getZ());}
        private static int eyeMarkAt(List<Packet<?>> packets,Cow target){
            var pos=markPosition(target);
            for(var packet:packets)if(packet instanceof ClientboundAddEntityPacket add&&add.getType()==EntityTypes.ITEM_DISPLAY
                    &&new Vec3(add.getX(),add.getY(),add.getZ()).distanceToSqr(pos)<.000001)return add.getId();
            return -1;
        }
        private static boolean removed(List<Packet<?>> packets,int id){
            return packets.stream().anyMatch(p->p instanceof ClientboundRemoveEntitiesPacket remove&&remove.entityIds().contains(id));
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount();
            require(now-began<480,"bounded actual fire/End tick and radar scene");
            if(phase==0){
                if(now==when||now%10!=0)return false;
                require(f.owner.isAlive()&&f.owner.getOffhandItem().isEmpty()
                        &&((Number)WoolBucketChecks.read(f.owner,"lastHurt")).floatValue()==1_000_000
                        &&f.owner.getLastDamageSource()!=null&&f.owner.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE),"actual Nether ten-tick native million in_fire consumes exactly the real totem");
                f.owner.removeAllEffects();f.owner.setAbsorptionAmount(0);f.owner.setHealth(f.owner.getMaxHealth());f.owner.setInvulnerableTime(0);f.owner.damageCooldownTime=0;
                f.owner.addEffect(ItemEffectLevels.create(MobEffects.FIRE_RESISTANCE,100,0));hp=f.owner.getHealth();when=now;phase=1;return false;
            }
            if(phase==1){
                if(now==when||now%10!=0)return false;
                require(f.owner.getHealth()==hp,"native Fire Resistance protects from the same million fire damage");
                move(game.server().getLevel(Level.END));when=now;phase=2;return false;
            }
            if(phase==2){
                if(now==when||now%200!=0)return false;
                endCount=game.items().mana().score(f.owner,"end_damage").get();
                require(endCount==4&&f.owner.getHealth()==hp-4,"actual End ten-second native outside_border and source (3+1)*6/5 escalation");
                game.items().mana().score(f.owner,"void_resistance").set(30);hp=f.owner.getHealth();when=now;phase=3;return false;
            }
            var panel=game.attributeHud();
            if(phase==3){
                if(now==when||now%200!=0)return false;
                require(game.items().mana().score(f.owner,"end_damage").get()==6&&f.owner.getHealth()==hp,"End counter continues growing while positive void resistance blocks its native damage");
                panel.clear(f.owner);for(int i=0;i<25;i++)panel.tick();
                require(panel.shown(f.owner)&&panel.displayedLines(f.owner).stream().anyMatch(c->c.getString().contains("\uE171")),"ordinary End dimension retains sidebar and actual life glyph");
                beginEye();phase=4;return false;
            }
            if(phase==4){
                if(!stepEye())return false;
                move(game.server().getLevel(Level.NETHER));f.owner.addEffect(ItemEffectLevels.create(MobEffects.FIRE_RESISTANCE,120,0));
                beginEye();phase=5;return false;
            }
            if(!stepEye())return false;
            for(var key:java.util.List.of(LbcHedgehogDimensions.NEXUS,LbcHedgehogDimensions.IMPRINTED)){
                var level=game.server().getLevel(key);var at=new Vec3(f.home.x,200,f.home.z);level.getChunkAt(net.minecraft.core.BlockPos.containing(at));
                var packets=(java.util.List<?>)WoolBucketChecks.read(f.client.connection,"packets");int before=packets.size();
                require(f.owner.teleportTo(level,at.x,at.y,at.z,Set.of(),0,0,true),"actual special-dimension transfer");f.client.acknowledge();
                game.items().refreshHud(f.owner);panel.tick();
                require(!panel.shown(f.owner)&&!game.items().mana().visible(f.owner)
                        &&packets.subList(before,packets.size()).stream().filter(ClientboundSetActionBarTextPacket.class::isInstance)
                            .map(ClientboundSetActionBarTextPacket.class::cast).allMatch(p->p.text().getString().isEmpty()),"both GUI owners stay hidden in "+key);
            }
            return true;
        }
        public void close(){if(closed)return;closed=true;
            try{if(eyePause!=null)game.items().timeStop().end(eyePause);for(var cow:eyeWitnesses)if(!cow.isRemoved())cow.discard();
                game.items().radar().forget(f.owner);
                if(f.owner.level()!=f.level){f.owner.teleportTo(f.level,f.home.x,f.home.y,f.home.z,Set.of(),0,0,true);f.client.acknowledge();}}
            finally{try{f.close();}finally{game.items().mana().score(f.owner,"end_damage").set(oldEnd);game.items().mana().score(f.owner,"void_resistance").set(oldVoid);eyeTime(oldEye);f.owner.getStats().setValue(f.owner,net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.TOTEM_OF_UNDYING),oldTotems);}}
        }
    }
    public static void run(GameController game){require(running==null,"exclusive dimension fixture");running=new Fixture(game);
        try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS dimension-ticks (actual Nether ten-tick million in_fire; native Fire Resistance; actual End200tick escalation/outside_border/void-resistance; emitted actionbar and private moving Eye radar in both dimensions, with local Time Stop and world/observer exclusions)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL dimension-ticks",error);}}
}
