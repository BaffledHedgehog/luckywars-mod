package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Actual ordinary-dimension player ticks, native fire immunity, End escalation and emitted HUD packets. */
public final class DimensionTickChecks {
    private DimensionTickChecks(){}
    private static Fixture running;
    private enum ArcPhase { LOCAL_STOP, FOREIGN_STOP, LIFETIME }
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Dimension ticks: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final WoolBucketChecks.Fixture f;final int oldEnd,oldVoid,oldTotems,oldEye;
        int phase,when,began,endCount;float hp;boolean closed;
        final List<Cow> eyeWitnesses=new ArrayList<>();
        Cow visible,observer,foreign;
        ServerLevel eyePause;
        int eyePhase,eyeWhen,eyeOwnedAt,eyeOtherAt,eyeMark,pausedEyeTime;
        ServerLevel arcPause;
        ArcPhase arcPhase;
        int arcWhen;
        List<LbcWands.ElectroTrace> arcs=List.of();
        List<Vec3> arcOrigins=List.of();
        Cow arcVictim;
        ServerLevel sniperLevel;
        BlockPos sniperSlab;
        MapManager.BlockSnapshot sniperSlabBefore;
        LbcWands.ElectroTrace sniperTrace;
        Cow sniperVictim;
        List<Vec3> sniperCenters=List.of();
        int sniperStarted,sniperPacketStart;
        boolean sniperStepped,sniperStoppedArena,sniperGriefing,sniperRulesChanged;
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
        void beginElectro(){
            var owner=f.owner;
            var level=owner.level();
            require(level!=f.level&&(level.dimension().equals(Level.NETHER)||level.dimension().equals(Level.END)),
                    "regular Electro is tested in a real ordinary offworld dimension");
            require(!game.items().timeStop().stopped(level)&&!game.items().timeStop().stopped(f.level),
                    "exclusive native world clocks for the electric arc scene");
            var origin=owner.getEyePosition();
            for(int offsets=-1;offsets<=3;offsets++){
                var pos=BlockPos.containing(origin.add(0,0,offsets));
                level.getChunkAt(pos);
                require(level.getBlockState(pos).isAir(),"resident unobstructed Electro scene at "+pos);
            }
            arcVictim=eyeCow(level,origin.add(0,-.7,1.2),false);
            arcVictim.setHealth(arcVictim.getMaxHealth());
            owner.stopUsingItem();owner.setShiftKeyDown(false);
            owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var loot=game.lbc().rollTable(level,owner.position(),owner,"lbc:ported/electro_wand");
            require(loot.size()==1&&loot.getFirst().is(LbcWands.ELECTRO),"real regular Electro loot leaf");
            var wand=loot.getFirst();
            owner.setItemInHand(InteractionHand.MAIN_HAND,wand);
            game.items().mana().set(owner,5000);
            var before=game.items().wands().electroSnapshot();
            require(owner.gameMode.useItem(owner,level,wand,InteractionHand.MAIN_HAND).consumesAction(),
                    "real native held Electro use in "+level.dimension());
            arcs=game.items().wands().electroSnapshot().stream().filter(t->!before.contains(t)&&t.owner==owner).toList();
            require(arcs.stream().map(t->t.lifetime).toList().equals(List.of(40,45,50))
                            &&arcs.stream().allMatch(t->t.level==level&&t.age==0),
                    "three accepted offworld native trace bodies with original active lifetimes");
            arcOrigins=arcs.stream().map(t->t.position).toList();
            arcPause=level;
            game.items().timeStop().begin(arcPause,20);
            arcPhase=ArcPhase.LOCAL_STOP;
            arcWhen=game.server().getTickCount();
        }
        void parkArcs(){
            // Keep the lifetime witness in the same resident air lane after the real first-segment hit.
            for(int i=0;i<arcs.size();i++){
                var arc=arcs.get(i);
                arc.position=arcOrigins.get(i);
                arc.yaw=0;
                arc.pitch=0;
            }
        }
        boolean stepElectro(){
            int now=game.server().getTickCount();
            if(now==arcWhen)return false;
            var live=game.items().wands().electroSnapshot();
            if(arcPhase==ArcPhase.LOCAL_STOP){
                require(live.containsAll(arcs)&&arcs.stream().allMatch(t->t.age==0)
                                &&arcs.stream().map(t->t.position).toList().equals(arcOrigins)
                                &&arcVictim.getHealth()==arcVictim.getMaxHealth(),
                        "local TimeStop preserves all accepted arc clocks, positions and target HP in "+f.owner.level().dimension());
                if(now-arcWhen<2)return false;
                game.items().timeStop().end(arcPause);
                arcPause=f.level;
                game.items().timeStop().begin(arcPause,20);
                arcPhase=ArcPhase.FOREIGN_STOP;
                arcWhen=now;
                return false;
            }
            if(arcPhase==ArcPhase.FOREIGN_STOP){
                var damage=arcVictim.getLastDamageSource();
                require(game.items().timeStop().stopped(arcPause)&&live.containsAll(arcs)
                                &&arcs.stream().allMatch(t->t.age==1)
                                &&!arcs.stream().map(t->t.position).toList().equals(arcOrigins),
                        "foreign arena TimeStop does not pause native offworld progress in "+f.owner.level().dimension());
                require(arcVictim.getHealth()<arcVictim.getMaxHealth()&&damage!=null
                                &&damage.is(LbcDamage.ELECTRO_BOLT)&&damage.getEntity()==f.owner,
                        "first resumed segment hits the real offworld target with native electrical damage and shooter credit");
                game.items().timeStop().end(arcPause);arcPause=null;
                arcVictim.discard();arcVictim=null;
                parkArcs();
                arcPhase=ArcPhase.LIFETIME;
                arcWhen=now;
                return false;
            }
            int activeTicks=1+now-arcWhen;
            for(var arc:arcs){
                require(live.contains(arc)==(activeTicks<arc.lifetime)&&arc.age==Math.min(activeTicks,arc.lifetime),
                        "each accepted trace expires after its exact active age in "+f.owner.level().dimension()
                                +" (age "+arc.age+", elapsed "+activeTicks+", lifetime "+arc.lifetime+")");
            }
            if(activeTicks<50){parkArcs();return false;}
            return true;
        }
        void beginSniper(){
            var owner=f.owner;
            sniperLevel=owner.level();
            require(sniperLevel.dimension().equals(Level.END)&&!game.items().timeStop().stopped(sniperLevel)
                            &&!game.items().timeStop().stopped(f.level),
                    "real native End shooter and independent clocks");
            require(owner.teleportTo(sniperLevel,owner.getX(),Math.floor(owner.getY())+.75,owner.getZ(),Set.of(),0,0,true),
                    "native volley passes above the single slab's collision box");
            f.client.acknowledge();
            owner.setDeltaMovement(Vec3.ZERO);
            owner.getAbilities().invulnerable=true;
            var loot=game.lbc().rollTable(sniperLevel,owner.position(),owner,"lbc:ported/imba_sniper_crossbow");
            require(loot.size()==1&&loot.getFirst().is(LbcImbaCrossbows.Kind.SNIPER.item),"actual native sniper loot leaf");
            var weapon=loot.getFirst();
            weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));
            owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);
            sniperSlab=BlockPos.containing(owner.position().add(0,0,2));
            for(int offset=0;offset<=18;offset++){
                var cell=BlockPos.containing(owner.position().add(0,0,offset));
                sniperLevel.getChunkAt(cell);
                require(sniperLevel.getBlockState(cell).isAir(),"single-slab End ray has an empty resident setup");
            }
            sniperSlabBefore=game.maps().snapshot(sniperLevel,sniperSlab);
            sniperLevel.setBlock(sniperSlab,Blocks.SMOOTH_STONE_SLAB.defaultBlockState(),
                    Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
            sniperGriefing=sniperLevel.getGameRules().get(GameRules.MOB_GRIEFING);
            sniperRulesChanged=true;
            sniperLevel.getGameRules().set(GameRules.MOB_GRIEFING,false,game.server());
            sniperVictim=eyeCow(sniperLevel,Vec3.atCenterOf(sniperSlab).add(4,.25,0),false);
            sniperVictim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
            sniperVictim.setHealth(1000);
            game.items().timeStop().begin(f.level,300);
            sniperStoppedArena=true;
            var before=game.items().wands().electroSnapshot();
            sniperPacketStart=f.client.packets().size();
            require(owner.gameMode.useItem(owner,sniperLevel,weapon,InteractionHand.MAIN_HAND).consumesAction(),
                    "charged native sniper fires in End during foreign arena TimeStop");
            var traces=game.items().wands().electroSnapshot().stream().filter(t->t.owner==owner&&!before.contains(t)).toList();
            require(traces.size()==1&&traces.getFirst().erecto!=null&&traces.getFirst().erecto.kind==LbcErecto.CROSSBOW,
                    "real regular sniper produces one offworld beam");
            sniperTrace=traces.getFirst();
            var aim=Vec3.directionFromRotation(sniperTrace.pitch,sniperTrace.yaw);
            var centers=new ArrayList<Vec3>();
            for(int sample=0;sample<=60;sample++){
                var point=sniperTrace.position.add(aim.scale(sample*.3));
                if(BlockPos.containing(point).equals(sniperSlab))centers.add(point);
            }
            sniperCenters=List.copyOf(centers);
            require(sniperCenters.size()>=3&&sniperCenters.size()<=4&&sniperTrace.level==sniperLevel&&sniperTrace.age==0,
                    "one slab contributes only three or four expected native blast centers");
            sniperStarted=game.server().getTickCount();
        }
        boolean stepSniper(){
            int elapsed=game.server().getTickCount()-sniperStarted;
            if(elapsed==0)return false;
            require(elapsed<240&&game.items().timeStop().stopped(f.level)&&!game.items().timeStop().stopped(sniperLevel),
                    "End queue is observed while the foreign arena clock remains stopped");
            if(!sniperStepped){
                require(sniperTrace.age==1,"native crossbow ray actually advances in End while its arena is stopped");
                removeTraces(List.of(sniperTrace));
                sniperStepped=true;
            }
            var packets=tail(f.client,sniperPacketStart).stream().filter(ClientboundExplodePacket.class::isInstance)
                    .map(ClientboundExplodePacket.class::cast).toList();
            require(packets.size()<=sniperCenters.size(),"only the initial ray segment can publish explosions");
            for(int index=0;index<packets.size();index++)
                require(packets.get(index).radius()==20&&packets.get(index).center().equals(sniperCenters.get(index)),
                        "native End blast packet keeps exact accepted center and radius");
            if(packets.size()!=sniperCenters.size())return false;
            var damage=sniperVictim.getLastDamageSource();
            require(sniperVictim.getHealth()<1000&&damage!=null&&damage.getDirectEntity()==null&&damage.getEntity()==f.owner
                            &&damage.typeHolder().equals(sniperLevel.damageSources().explosion(null,f.owner).typeHolder()),
                    "offworld native source-null MOB blast hits the End target and keeps shooter attribution");
            require(sniperLevel.getBlockState(sniperSlab).is(Blocks.SMOOTH_STONE_SLAB)&&sniperTrace.erecto.pendingBlasts==null
                            &&game.items().stonekivi().queuedSnapshot().stream().noneMatch(work->
                                work instanceof LbcErecto.CrossbowBlasts blast&&blast.path==sniperTrace.erecto),
                    "offworld job retires after the single slab while native mob griefing remains disabled");
            closeSniper();
            return true;
        }
        void removeTraces(List<LbcWands.ElectroTrace> owned){
            ((Set<?>)WoolBucketChecks.read(game.items().wands(),"electroTraces")).removeAll(owned);
        }
        void closeSniper(){
            if(sniperStoppedArena){game.items().timeStop().end(f.level);sniperStoppedArena=false;}
            if(sniperTrace!=null){
                removeTraces(List.of(sniperTrace));
                if(sniperTrace.erecto.pendingBlasts!=null)sniperTrace.erecto.pendingBlasts.cancel();
                sniperTrace=null;
            }
            if(sniperVictim!=null){sniperVictim.discard();sniperVictim=null;}
            if(sniperSlabBefore!=null){
                game.maps().restoreSnapshot(sniperLevel,sniperSlab,sniperSlabBefore,Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS);
                sniperSlabBefore=null;
            }
            if(sniperRulesChanged){
                sniperLevel.getGameRules().set(GameRules.MOB_GRIEFING,sniperGriefing,game.server());
                sniperRulesChanged=false;
            }
        }
        boolean step(){
            f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount();
            require(now-began<900,"bounded actual fire/End tick, radar and offworld projectile scenes");
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
            if(phase==5){
                if(!stepEye())return false;
                beginElectro();phase=6;return false;
            }
            if(phase==6){
                if(!stepElectro())return false;
                move(game.server().getLevel(Level.END));
                beginElectro();phase=7;return false;
            }
            if(phase==7){
                if(!stepElectro())return false;
                beginSniper();phase=8;return false;
            }
            if(!stepSniper())return false;
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
            try{if(eyePause!=null)game.items().timeStop().end(eyePause);if(arcPause!=null)game.items().timeStop().end(arcPause);
                closeSniper();removeTraces(arcs);
                for(var cow:eyeWitnesses)if(!cow.isRemoved())cow.discard();
                game.items().radar().forget(f.owner);
                if(f.owner.level()!=f.level){f.owner.teleportTo(f.level,f.home.x,f.home.y,f.home.z,Set.of(),0,0,true);f.client.acknowledge();}}
            finally{try{f.close();}finally{game.items().mana().score(f.owner,"end_damage").set(oldEnd);game.items().mana().score(f.owner,"void_resistance").set(oldVoid);eyeTime(oldEye);f.owner.getStats().setValue(f.owner,net.minecraft.stats.Stats.ITEM_USED.get(net.minecraft.world.item.Items.TOTEM_OF_UNDYING),oldTotems);}}
        }
    }
    public static void run(GameController game){require(running==null,"exclusive dimension fixture");running=new Fixture(game);
        try{running.setup();}catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;throw error;}}
    public static void tick(GameController game){if(running==null)return;try{if(!running.step())return;running.close();running=null;
        SkyWars2.LOGGER.info("[check] PASS dimension-ticks (Nether fire and End escalation; native HUD and Eye; regular Electro hit/exact active lifetime in both dimensions; one native End sniper impact under foreign arena TimeStop)");
    }catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}running=null;SkyWars2.LOGGER.error("[check] FAIL dimension-ticks",error);}}
}
