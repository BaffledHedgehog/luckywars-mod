package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.lang.reflect.Field;
import java.util.*;
import me.skywars2.game.LbcHedgehogItems.SpikeKind;

/** Real native arrow contacts and consumed items; dimension transport is observed on the cloned actor. */
public final class HedgehogSpikeChecks {
    private HedgehogSpikeChecks(){}
    private static Fixture running;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Hedgehog spikes: "+why);}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final LbcHedgehogSpikes service;final ServerPlayer owner,other;final HedgehogChecks.Saved saved,otherSaved;final Map<Field,Object> config;
        final Set<Entity> before=Collections.newSetFromMap(new IdentityHashMap<>());final Set<UUID> owned=new HashSet<>();final Vec3 at;final String epoch;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();final Set<Long> forced=new HashSet<>(),previousForced;
        final net.minecraft.world.scores.PlayerTeam oldOwnerTeam,oldOtherTeam,team;long last=Long.MIN_VALUE;int phase;boolean closed;
        Fixture(GameController game){this.game=game;service=game.items().hedgehogSpikes();epoch=game.items().epoch();owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));other=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestBeta"));
            require(game.playing()&&owner!=null&&other!=null&&owner.level()==other.level()&&game.canUseGameplayItems(owner)&&game.canUseGameplayItems(other),"two native active participants");
            require(service.trackedCount()==0&&service.pendingCount()==0&&game.items().hedgehog().trackedCount()==0&&!game.items().godHedgehog().blocksSilverfish(owner.level()),"exclusive clean services");
            saved=new HedgehogChecks.Saved(game,owner);otherSaved=new HedgehogChecks.Saved(game,other);config=HedgehogChecks.fields(game.config());owner.level().getAllEntities().forEach(before::add);
            at=new Vec3((owner.blockPosition().getX()>>4)*16+8.37,270,(owner.blockPosition().getZ()>>4)*16+8.37);previousForced=new HashSet<>(owner.level().getForceLoadedChunks());
            oldOwnerTeam=owner.getTeam();oldOtherTeam=other.getTeam();var board=game.server().getScoreboard();require(board.getPlayerTeam("hedgehog_spikes")==null,"unique spike fixture team");team=board.addPlayerTeam("hedgehog_spikes");team.setAllowFriendlyFire(false);
        }
        void initialize(){
            require(before.stream().noneMatch(e->!e.isRemoved()&&!(e instanceof ServerPlayer)&&e.position().distanceToSqr(at)<40*40),"isolated clear spike region: "+before.stream().filter(e->!e.isRemoved()&&!(e instanceof ServerPlayer)&&e.position().distanceToSqr(at)<40*40).limit(6).map(e->e.getClass().getSimpleName()+"/"+e.getUUID()+"@"+e.position()).toList());var base=BlockPos.containing(at);
            for(int x=(base.getX()-24)>>4;x<=(base.getX()+24)>>4;x++)for(int z=(base.getZ()-24)>>4;z<=(base.getZ()+24)>>4;z++){
                long key=net.minecraft.world.level.ChunkPos.pack(x,z);if(!previousForced.contains(key)){owner.level().setChunkForced(x,z,true);forced.add(key);}owner.level().getChunk(x,z);}
            for(var p:BlockPos.betweenClosed(base.offset(-4,-1,-4),base.offset(4,15,14)))require(owner.level().getBlockState(p).isAir(),"empty source spike volume");
            game.config().ranked=false;game.config().lbcSkills=false;game.config().offhandWands=true;
            for(var p:List.of(owner,other)){p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.setNoGravity(true);p.setShiftKeyDown(false);p.setSprinting(false);p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);
                p.entityTags().removeAll(Set.of("time_stopped","sin_greed_active"));game.items().mana().noMagic(p,1000);game.items().mana().set(p,100000);
                var profile=game.players().get(p.getUUID());profile.lbcSkill=0;profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);Arrays.fill(profile.levelups,0);game.items().magic().inventoryChanged(p);}
            var board=game.server().getScoreboard();board.addPlayerToTeam(owner.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);move(owner,at);move(other,at.add(10,0,-10));
        }
        void move(ServerPlayer p,Vec3 point){p.teleportTo(p.level(),point.x,point.y,point.z,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);}
        Cow cow(Vec3 point){var c=new Cow(EntityTypes.COW,owner.level());c.setPos(point);c.setNoAi(true);c.setNoGravity(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);c.setHealth(200);require(owner.level().addFreshEntity(c),"native spike cow");owned.add(c.getUUID());return c;}
        Entity find(UUID id){for(var level:game.server().getAllLevels()){var e=level.getEntity(id);if(e!=null)return e;}return null;}
        void clear(){service.stop();game.items().hedgehog().retireOwned(owner.getUUID(),true);game.items().hedgehog().retireOwned(other.getUUID(),true);
            for(var id:owned){var e=find(id);if(e instanceof Cow&&!e.isAlive()&&e.level() instanceof ServerLevel level)for(var drop:level.getEntities((Entity)null,e.getBoundingBox().inflate(3),d->!before.contains(d)&&(d instanceof net.minecraft.world.entity.item.ItemEntity||d instanceof net.minecraft.world.entity.ExperienceOrb)))drop.discard();
                if(e instanceof LivingEntity l)game.items().indirectKills().clear(l);if(e!=null)e.discard();}owned.clear();}
        Arrow hit(SpikeKind kind,Cow target){var arrow=service.emit(owner,kind,new Vec3(target.getX(),target.getY()+.5,target.getZ()-1),new Vec3(0,0,2));require(arrow!=null,"native arrow launch "+kind);arrow.tick();return arrow;}
        void consumption(){
            for(var hand:InteractionHand.values())for(var kind:SpikeKind.values()){
                var stack=new ItemStack(kind.item(),2);owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);owner.setItemInHand(hand,stack);
                int used=owner.getStats().getValue(Stats.ITEM_USED.get(kind.item()));int mana=game.items().mana().get(owner);
                require(owner.gameMode.useItem(owner,owner.level(),stack,hand).consumesAction()&&stack.getCount()==1&&owner.getStats().getValue(Stats.ITEM_USED.get(kind.item()))==used+1&&game.items().mana().get(owner)==mana,"actual consumed native item/use statistic/no mana "+hand+"/"+kind);
                require(service.trackedCount()==(kind==SpikeKind.IMPULSE?0:1),"six physical transports and impulse hitscan");clear();
            }
            var stack=new ItemStack(SpikeKind.STANDARD.item(),2);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);game.items().timeStop().begin(owner.level(),40);
            try{require(!owner.gameMode.useItem(owner,owner.level(),stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==2,"Time Stop prevents launch/debit");}finally{game.items().timeStop().end(owner.level());}
        }
        void payloads(){
            var target=cow(at.add(0,0,4));hit(SpikeKind.STANDARD,target);require(target.getHealth()==180&&target.getLastHurtByMob()==owner,"STANDARD physical base10 atspeed2 deals20 with caster credit");clear();
            target=cow(at.add(0,0,4));hit(SpikeKind.BLIND,target);require(target.getHealth()==197&&game.items().psycho().hudValue(target,"piss")==6000&&!target.hasEffect(net.minecraft.world.effect.MobEffects.RAID_OMEN),"BLIND base1.5 and exact cumulative6000 source counter without Raid");
            target.setInvulnerableTime(0);target.damageCooldownTime=0;hit(SpikeKind.BLIND,target);require(game.items().psycho().hudValue(target,"piss")==12000,"second physical BLIND adds counter instead of replacing");clear();
            target=cow(at.add(0,0,4));for(var key:List.of("dmg_dealt","food_count","lolidefecation","sleep_time","greed_prestige"))game.items().mana().score(target,key).set(47);target.entityTags().add("greed_cursed");
            hit(SpikeKind.POISON,target);require(service.pendingCount()==1&&target.getHealth()==197,"POISON native accepted hit queues exact reset");service.tick();
            var resetTarget=target;require(game.items().mana().score(target,"dmg_dealt").get()==1&&List.of("food_count","lolidefecation","sleep_time","greed_prestige").stream().allMatch(k->game.items().mana().score(resetTarget,k).get()==0)&&target.entityTags().contains("have_mom")&&!target.entityTags().contains("greed_cursed"),"POISON exact witch-gens reset scores/tags");clear();
            target=cow(at.add(0,0,4));hit(SpikeKind.WITHER,target);var water=target.getEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING);
            require(target.getHealth()==198&&water!=null&&water.getAmplifier()==1&&water.getDuration()==600,"WITHER full600tick enhancedwater1 native payload");game.items().magic().colors().tickWater();service.tick();
            require(target.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)&&target.getEffect(net.minecraft.world.effect.MobEffects.WITHER).getAmplifier()==3,"enhancedwater creates source Wither3 while in AIR");
            target.setHealth(1);target.setInvulnerableTime(0);target.damageCooldownTime=0;target.hurtServer(owner.level(),target.damageSources().wither(),100);require(target.isDeadOrDying()&&target.getKillCredit()==owner,"actual derived Wither lethal damage inherits spike thrower");clear();
        }
        void protectAndReflect(){
            var target=cow(at.add(0,0,4));target.entityTags().add("strict_map_object");var shot=hit(SpikeKind.POISON,target);require(target.getHealth()==200&&service.pendingCount()==0,"technical role blocks physical hurt and payload");clear();
            move(other,at.add(0,0,4));other.setHealth(20);var ally=service.emit(owner,SpikeKind.STANDARD,other.position().add(0,.5,-1),new Vec3(0,0,2));require(ally!=null,"ally shot launch");ally.tick();require(other.getHealth()==20,"physical spike protects allied player");clear();
            other.entityTags().add("sin_greed_active");try{
                var vanilla=new Arrow(owner.level(),owner,new ItemStack(Items.ARROW),null);vanilla.setPos(other.position().add(0,.5,-1));vanilla.setDeltaMovement(0,0,2);owned.add(vanilla.getUUID());require(owner.level().addFreshEntity(vanilla),"real vanilla arrow admitted against allied Greed");
                try{vanilla.tick();require(vanilla.getOwner()==other&&vanilla.getDeltaMovement().z<0&&other.getHealth()==20&&vanilla.entityTags().contains(LbcGreed.REFLECTED),"shared native AbstractArrow gate allows allied Greed reflection: owner="+(vanilla.getOwner()==null?null:vanilla.getOwner().getScoreboardName())+", motion="+vanilla.getDeltaMovement()+", hp="+other.getHealth()+", removed="+vanilla.isRemoved());
                    vanilla.tick();require(vanilla.getOwner()==other&&vanilla.getDeltaMovement().z<0&&other.getHealth()==20&&service.pendingCount()==0,"next native vanilla-arrow tick escapes its new Greed owner without self-reflection: owner="+(vanilla.getOwner()==null?null:vanilla.getOwner().getScoreboardName())+", motion="+vanilla.getDeltaMovement()+", hp="+other.getHealth()+", point="+vanilla.position());
                }finally{vanilla.discard();}
                var reflect=service.emit(owner,SpikeKind.POISON,other.position().add(0,.5,-1),new Vec3(0,0,2));require(reflect!=null,"Greed shot");reflect.tick();
                require(reflect.getOwner()==other&&reflect.getDeltaMovement().z<0&&other.getHealth()==20&&service.pendingCount()==0,"actual Greed preimpact reverses speed/currentowner without hurt/payload: owner="+(reflect.getOwner()==null?null:reflect.getOwner().getScoreboardName())+", motion="+reflect.getDeltaMovement()+", hp="+other.getHealth()+", pending="+service.pendingCount()+", removed="+reflect.isRemoved()+", point="+reflect.position()+", reflected="+reflect.entityTags().contains(LbcGreed.REFLECTED));
                reflect.tick();require(reflect.getOwner()==other&&reflect.getDeltaMovement().z<0&&other.getHealth()==20&&service.pendingCount()==0,"next native spike tick escapes its new Greed owner without self-reflection or payload: owner="+(reflect.getOwner()==null?null:reflect.getOwner().getScoreboardName())+", motion="+reflect.getDeltaMovement()+", hp="+other.getHealth()+", pending="+service.pendingCount()+", point="+reflect.position());
                other.entityTags().remove("sin_greed_active");target=cow(at.add(0,0,2));reflect.setPos(target.position().add(0,.5,1));reflect.setDeltaMovement(0,0,-2);reflect.tick();service.tick();
                require(target.entityTags().contains("have_mom")&&target.getLastHurtByMob()==other,"reflected real impact uses current owner for reset and direct damage");
            }finally{other.entityTags().remove("sin_greed_active");}clear();move(other,at.add(10,0,-10));
        }
        void boomAndImpulse(){
            var target=cow(at.add(0,0,4));hit(SpikeKind.BOOM,target);require(service.pendingCount()==1&&game.items().hedgehog().wallCount(owner.level())==0,"BOOM queues after accepted native hurt");service.tick();
            require(game.items().hedgehog().wallCount(owner.level())==65,"BOOM living hit creates real65wall native tower family");clear();
            var block=BlockPos.containing(at.add(0,1,4));blocks.put(block,game.maps().snapshot(owner.level(),block));owner.level().setBlock(block,Blocks.STONE.defaultBlockState(),2);
            var shot=service.emit(owner,SpikeKind.BOOM,new Vec3(block.getX()+.5,block.getY()+.5,block.getZ()-1),new Vec3(0,0,2));require(shot!=null,"BOOM block launch");shot.tick();
            require(shot.isRemoved()&&game.items().hedgehog().wallCount(owner.level())==65,"actual native block impact triggers65walls once");clear();owner.level().setBlock(block,Blocks.AIR.defaultBlockState(),2);
            var first=cow(at.add(0,1,3));var second=cow(at.add(0,1,6));service.impulse(owner);
            require(first.getHealth()<193&&second.getHealth()<193&&first.getLastHurtByMob()==owner&&second.getLastHurtByMob()==owner,"source impulse repeated7/cell pierces both creatures with owner credit");clear();
        }
        void dimensions(){
            var originalLevel=other.level();var point=other.position();var board=game.server().getScoreboard();
            board.removePlayerFromTeam(other.getScoreboardName(),team);
            try{
                move(other,at.add(0,0,4));other.setHealth(20);other.setInvulnerableTime(0);other.damageCooldownTime=0;
                var arrow=service.emit(owner,SpikeKind.CONFUSE,other.position().add(0,.5,-1),new Vec3(0,0,2));require(arrow!=null,"native player-target CONFUSE launch");arrow.tick();
                require(service.pendingCount()==1&&other.level()==originalLevel,"accepted physical CONFUSE queues the actual player");service.tick();
                require(other.level()!=originalLevel&&game.canUseGameplayItems(other),"actual accepted-arrow CONFUSE transfers the same player to a source dimension");other.hasChangedDimension();
                var stack=new ItemStack(SpikeKind.STANDARD.item(),2);other.setItemInHand(InteractionHand.MAIN_HAND,stack);
                require(other.gameMode.useItem(other,other.level(),stack,InteractionHand.MAIN_HAND).consumesAction()&&stack.getCount()==1,"actual spike item remains usable after real crossworld transfer");
                if(!other.level().dimension().equals(LbcHedgehogDimensions.NEXUS)){
                    int roll=other.level().dimension().equals(LbcHedgehogDimensions.IMPRINTED)?4:1;
                    require(game.items().hedgehogDimensions().teleport(other,owner,roll)&&other.level()==originalLevel,"supported source-world branch returns actual player to arena overworld alias");other.hasChangedDimension();
                }
            }finally{
                service.stop();other.teleportTo(originalLevel,point.x,point.y,point.z,Set.of(),0,0,true);other.hasChangedDimension();other.setHealth(20);
                board.addPlayerToTeam(other.getScoreboardName(),team);game.items().indirectKills().clear(other);
            }
            clear();
        }
        boolean step(){if(owner.level().getGameTime()==last)return false;last=owner.level().getGameTime();require(game.playing()&&game.items().currentEpoch(epoch),"active source epoch");
            switch(phase++){case 0->consumption();case 1->payloads();case 2->protectAndReflect();case 3->boomAndImpulse();case 4->{dimensions();return true;}default->throw new IllegalStateException();}return false;}
        @Override public void close(){if(closed)return;closed=true;try{game.items().timeStop().end(owner.level());clear();for(var p:List.of(owner,other))game.items().indirectKills().clear(p);
            for(var entry:blocks.entrySet())game.maps().restoreSnapshot(owner.level(),entry.getKey(),entry.getValue());HedgehogChecks.restore(game.config(),config);
            var board=game.server().getScoreboard();for(var p:List.of(owner,other))if(p.getTeam()==team)board.removePlayerFromTeam(p.getScoreboardName(),team);if(oldOwnerTeam!=null)board.addPlayerToTeam(owner.getScoreboardName(),oldOwnerTeam);if(oldOtherTeam!=null)board.addPlayerToTeam(other.getScoreboardName(),oldOtherTeam);board.removePlayerTeam(team);
            saved.restore(game);otherSaved.restore(game);game.players().save();game.items().magic().flushCleansing();
            }finally{for(long key:forced)owner.level().setChunkForced(net.minecraft.world.level.ChunkPos.getX(key),net.minecraft.world.level.ChunkPos.getZ(key),false);}}
    }
    public static void run(GameController game){require(running==null,"exclusive spike fixture");var f=new Fixture(game);running=f;try{f.initialize();}catch(RuntimeException|Error e){try{f.close();}finally{running=null;}throw e;}}
    public static void tick(GameController game){if(running==null)return;var f=running;try{if(f.step()){f.close();running=null;SkyWars2.LOGGER.info("[check] PASS hedgehog-spikes");}}catch(Throwable error){try{f.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL hedgehog-spikes",error);}}
    private static List<Arrow> retained;private static String stopEpoch;
    public static void stopProbe(GameController game){require(retained==null&&running==null&&game.items().hedgehogSpikes().trackedCount()==0,"exclusive spike stop");var owner=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(owner!=null&&game.canUseGameplayItems(owner),"actual retained thrower");
        var shot=game.items().hedgehogSpikes().emit(owner,SpikeKind.BLIND,owner.getEyePosition().add(0,8,0),Vec3.ZERO);require(shot!=null,"retained actual physical spike");shot.entityTags().add("time_stopped");retained=List.of(shot);stopEpoch=game.items().epoch();SkyWars2.LOGGER.info("[check] PASS hedgehog-spikes-stop-probe");}
    public static void cleanup(GameController game){require(retained!=null,"spike stop probe exists");try{require(!game.items().currentEpoch(stopEpoch)&&game.items().hedgehogSpikes().trackedCount()==0&&game.items().hedgehogSpikes().pendingCount()==0&&retained.stream().allMatch(Entity::isRemoved),"real stop removes physical spikes and invalidates epoch");}finally{retained.forEach(Entity::discard);retained=null;stopEpoch=null;}SkyWars2.LOGGER.info("[check] PASS hedgehog-spikes-cleanup");}
}
