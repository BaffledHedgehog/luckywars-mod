package me.skywars2.game;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.*;
import net.minecraft.resources.Identifier;

/** Source12/15/17 reactions; native bodies/teleports/effects/attribution remain canonical. */
public final class LbcReactiveDefense {
    private static final String HORSE="skywars2.reactive.twilight",SPIKE="skywars2.reactive.ground_spike",OWNER="skywars2.reactive.owner:",TEAM="skywars2.reactive.team:";
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final CompoundTag SPIKE_PRESET=spikePreset();
    private static CompoundTag spikePreset(){try{return TagParser.parseCompoundFully("{block_state:{id:'minecraft:pointed_dripstone',properties:{vertical_direction:'up',thickness:'tip',waterlogged:'false'}},billboard:'fixed',width:0.2f,teleport_duration:30,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[0.6f,1.2f,0.6f],translation:[-0.3f,0f,-0.3f]}}");}catch(Exception e){throw new IllegalStateException("Source grounded spike preset",e);}}
    private enum Kind { BANANA,HORSE,SPIKE }
    private record Proc(Kind kind,UUID owner,String epoch,ServerLevel level,Vec3 at,UUID body,String team,LbcIndirectKills.Owner cause) { }
    private static final class Actor {
        final Entity body;final UUID owner;final String epoch,team;final Kind kind;final LbcIndirectKills.Owner cause;
        int remaining;long due;
        Actor(Entity body,UUID owner,String epoch,String team,Kind kind,LbcIndirectKills.Owner cause,int remaining,long due){this.body=body;this.owner=owner;this.epoch=epoch;this.team=team;this.kind=kind;this.cause=cause;this.remaining=remaining;this.due=due;}
    }
    public record BodyView(Entity body,UUID owner,boolean horse,int remaining,long due) { }
    private final GameController game;private final Predicate<ServerPlayer> admission;
    private final ArrayDeque<Proc> pending=new ArrayDeque<>();private final ArrayDeque<Actor> work=new ArrayDeque<>();
    private final IdentityHashMap<Entity,Actor> actors=new IdentityHashMap<>();
    private final Map<ServerLevel,Long> clocks=new IdentityHashMap<>();
    private final Set<Entity> spawning=Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean restoring,publishing,ticking,draining;private int lastTick=Integer.MIN_VALUE,outputTick=Integer.MIN_VALUE,outputSpent;
    public LbcReactiveDefense(GameController game,Predicate<ServerPlayer> admission){this.game=game;this.admission=Objects.requireNonNull(admission);}
    private boolean eligible(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&!p.isRemoved()&&p.isAlive()&&!game.isObserver(p)&&LbcDimensionItems.ordinary(p)&&admission.test(p);}
    private boolean paused(ServerLevel l){return game.items().wands().timeStopped()||game.items().timeStop().stopped(l);}
    private static boolean resident(ServerLevel l,Vec3 at){return l.hasChunkAt(BlockPos.containing(at));}
    private static String team(ServerPlayer p){return p.getTeam()==null?null:p.getTeam().getName();}
    private boolean roll(ServerPlayer p,int count,double first,double extra){return count>0&&p.getRandom().nextFloat()<first+extra*(count-1d);}
    /** Root passes the actual pre-hit native unit counts; this method never reads inventory. */
    public void received(ServerPlayer p,int bananaCount,int spikeCount){
        if(restoring||!eligible(p))return;Vec3 feet=p.position(),eyes=p.getEyePosition();
        if(!game.items().magic().suppressed(p)&&roll(p,bananaCount,.05,.025))accept(p,Kind.BANANA,eyes);
        if(roll(p,spikeCount,.10,.02))accept(p,Kind.SPIKE,feet);
        drain();
    }
    /** Once per existing source1second phase; deltaY is floor(PosY*100)-previous floor(PosY*100). */
    public void fall(ServerPlayer p,int copies,int deltaY){
        if(restoring||!eligible(p)||game.items().magic().suppressed(p)||p.onGround()||deltaY>-150)return;
        if(roll(p,copies,.05,.02))accept(p,Kind.HORSE,p.getEyePosition());drain();
    }
    private void accept(ServerPlayer p,Kind kind,Vec3 at){pending.addLast(new Proc(kind,p.getUUID(),game.items().epoch(),p.level(),at,UUID.randomUUID(),team(p),game.items().indirectKills().coldCause(p,kind==Kind.SPIKE)));}
    private void drain(){
        if(publishing||restoring||draining)return;
        int now=game.server().getTickCount();if(outputTick!=now){outputTick=now;outputSpent=0;}
        int visits=Math.min(64-outputSpent,pending.size());draining=true;
        try{while(visits-->0&&!pending.isEmpty()){outputSpent++;var proc=pending.removeFirst();
            if(!game.items().currentEpoch(proc.epoch))continue;
            var p=game.server().getPlayerList().getPlayer(proc.owner);
            if(p==null||p.isRemoved()||p.level()!=proc.level)continue;
            if(paused(proc.level)||game.items().timeStop().frozen(p)||game.items().wands().inputTimeStopped(p)){pending.addLast(proc);continue;}
            if(!eligible(p))continue;
            if(!resident(proc.level,proc.at)){pending.addLast(proc);continue;}
            if(proc.kind==Kind.BANANA){banana(proc,p);continue;}
            if(!publish(proc,p))pending.addLast(proc);
        }}finally{draining=false;}
    }
    private void banana(Proc proc,ServerPlayer p){
        var from=proc.at;LbcOverload.sound(p,from,"minecraft:entity.enderman.teleport",2,1,16);
        proc.level.sendParticles(ParticleTypes.PORTAL,true,false,from.x,from.y,from.z+1,100,.2,.5,.2,.5);
        var to=LbcSurfaceTeleport.destination(proc.level,Vec3.ZERO,6,proc.level.dimension().equals(Level.NETHER)?125:proc.level.getMaxY()+1,p.getRandom());
        // Source raises the actual player .5 even when spreadplayers failed to find a column.
        var actual=to==null?p.position():to;
        p.teleportTo(proc.level,actual.x,actual.y+.5,actual.z,Set.of(),p.getYRot(),p.getXRot(),true);
        var dest=p.position();proc.level.sendParticles(ParticleTypes.REVERSE_PORTAL,true,false,dest.x,dest.y,dest.z+1,100,.2,.5,.2,.5);
        LbcOverload.sound(p,from,"minecraft:entity.enderman.teleport",2,1,16);
        p.connection.send(new ClientboundSoundPacket(Holder.direct(SoundEvents.ENDERMAN_TELEPORT),SoundSource.MASTER,dest.x,dest.y,dest.z,1,1,p.getRandom().nextLong()));
    }
    private boolean publish(Proc proc,ServerPlayer p){
        Entity body;
        if(proc.kind==Kind.HORSE){
            var horse=new Horse(EntityTypes.HORSE,proc.level);horse.setAge(-10000);horse.setHealth(8);horse.setDeltaMovement(Vec3.ZERO);
            horse.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));horse.setItemSlot(EquipmentSlot.BODY,new ItemStack(Items.DIAMOND_HORSE_ARMOR));
            horse.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,-1,0,false,false,true),p);
            horse.setCustomName(Component.translatable("twilight_sparkle").withStyle(style->style.withColor(0xb83ac4).withItalic(false)));horse.setCustomNameVisible(true);
            horse.entityTags().addAll(List.of(HORSE,"twilight_sparkle","magic","summactivated"));body=horse;
        }else{
            var spike=new BlockDisplay(EntityTypes.BLOCK_DISPLAY,proc.level);spike.load(TagValueInput.create(ProblemReporter.DISCARDING,proc.level.registryAccess(),SPIKE_PRESET));spike.setBlockState(Blocks.POINTED_DRIPSTONE.defaultBlockState());
            spike.entityTags().addAll(List.of(SPIKE,"hedgehog_spike","slowraycast","player"));body=spike;
        }
        body.setUUID(proc.body);body.setPos(proc.at);body.entityTags().add(OWNER+proc.owner);if(proc.team!=null)body.entityTags().add(TEAM+proc.team);game.items().markCurrentItemEpoch(body);
        long now=clocks.computeIfAbsent(proc.level,ignored->0L);var state=new Actor(body,proc.owner,proc.epoch,proc.team,proc.kind,proc.cause,proc.kind==Kind.HORSE?100:0,now+(proc.kind==Kind.HORSE?2:1));
        actors.put(body,state);spawning.add(body);publishing=true;boolean added;
        try{game.items().timeStop().prepareFrozen(body);added=proc.level.addFreshEntity(body);}finally{publishing=false;spawning.remove(body);}
        if(!added){actors.remove(body);body.discard();return false;}
        // A shared native purge may have removed an admitted magical horse. That is a completed/cancelled output.
        if(body.isRemoved()){actors.remove(body);return true;}
        work.addLast(state);attachTeam(state);
        if(proc.kind==Kind.HORSE){mirror(state);p.startRiding(body,true,false);game.items().mana().score(p,"falltimer").set(0);LbcOverload.sound(proc.level,proc.at,"minecraft:entity.ender_dragon.flap",SoundSource.PLAYERS,3,1,0,0);}
        else{LbcOverload.sound(proc.level,proc.at,"minecraft:entity.silverfish.death",SoundSource.PLAYERS,1,1,0,0);proc.level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,Items.IRON_INGOT),proc.at.x,proc.at.y+.5,proc.at.z,10,0,.5,0,0);}
        return true;
    }
    private void attachTeam(Actor state){if(state.team!=null){var t=game.server().getScoreboard().getPlayerTeam(state.team);if(t!=null)game.server().getScoreboard().addPlayerToTeam(state.body.getScoreboardName(),t);}}
    private void mirror(Actor a){if(a.kind==Kind.HORSE)game.items().mana().score(a.body,"sprint_timer").set(a.remaining);}
    public void tick(){
        int now=game.server().getTickCount();if(restoring||ticking||lastTick==now||!game.server().tickRateManager().runsNormally()||game.server().tickRateManager().isFrozen())return;
        lastTick=now;ticking=true;
        try{
            for(var level:clocks.keySet())if(!paused(level))clocks.put(level,clocks.get(level)+1);
            drain();int visits=Math.min(128,work.size());
            while(visits-->0&&!work.isEmpty()){var a=work.removeFirst();if(actors.get(a.body)!=a)continue;
                if(a.body.isRemoved()||!game.items().currentEpoch(a.epoch)||!game.playing()||game.stage()==GameController.Stage.ENDING){retire(a);continue;}
                var level=(ServerLevel)a.body.level();long clock=clocks.computeIfAbsent(level,ignored->0L);
                if(paused(level)||game.items().timeStop().frozen(a.body)||!resident(level,a.body.position())){work.addLast(a);continue;}
                if(a.kind==Kind.HORSE&&game.items().magic().suppressedAt(level,a.body.position())){retire(a);continue;}
                if(clock>=a.due){
                    if(a.kind==Kind.HORSE){int elapsed=(int)Math.min(Integer.MAX_VALUE,1+(clock-a.due)/2);a.due+=2L*elapsed;horse(a,elapsed);}
                    else{a.due++;spike(a);}
                }
                if(actors.get(a.body)==a)work.addLast(a);
            }
        }finally{ticking=false;}
    }
    private void horse(Actor a,int elapsed){
        if(a.remaining<=0)return;var level=(ServerLevel)a.body.level();
        a.remaining=Math.max(0,a.remaining-elapsed);mirror(a);
        if(a.remaining==0)return;
        if(a.body.getFirstPassenger() instanceof ServerPlayer rider&&!game.isObserver(rider)&&rider.isAlive()&&game.canUseGameplayItems(rider)){
            var direction=rider.getLookAngle();a.body.setDeltaMovement(new Vec3(quantized(direction.x),quantized(direction.y),quantized(direction.z)));
            if(rider.getRandom().nextFloat()<.1f)LbcOverload.sound(level,a.body.position(),"minecraft:entity.ender_dragon.flap",SoundSource.NEUTRAL,1,1.5f,0,0);
        }
    }
    private static double quantized(double value){return Math.floor(value*10000)*.0001;}
    private boolean target(Actor a,LivingEntity e){
        if(e.getUUID().equals(a.owner)||!e.getType().builtInRegistryHolder().is(MOBS)||!game.items().wands().validRoleTarget(e,false))return false;
        var p=game.server().getPlayerList().getPlayer(a.owner);if(p!=null&&(p.isAlliedTo(e)||game.items().summons().friendly(p,e)))return false;
        return a.team==null||e.getTeam()==null||!a.team.equals(e.getTeam().getName());
    }
    private void spike(Actor a){
        var body=a.body;var at=body.position();var level=(ServerLevel)body.level();
        var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(.4),e->target(a,e)&&e.position().distanceToSqr(at)<=.16);
        var nearest=targets.stream().min(Comparator.comparingDouble(e->e.position().distanceToSqr(at))).orElse(null);
        if(nearest!=null){
            var p=game.server().getPlayerList().getPlayer(a.owner);var type=level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW);
            try{nearest.hurtServer(level,game.items().indirectKills().captureDamage(new DamageSource(type,body,p==null?body:p),a.cause),4);}
            finally{level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,Blocks.DRIPSTONE_BLOCK.defaultBlockState()),at.x,at.y+.5,at.z,10,.1,.3,.1,0);LbcOverload.sound(level,at,"minecraft:entity.zombie.break_wooden_door",SoundSource.BLOCKS,1,1,0,0);retire(a);}
            return;
        }
        double falling=(body.getDeltaMovement().y-.08)*.98;
        var hit=level.clip(new ClipContext(at.add(0,.001,0),at.add(0,falling,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,body));
        if(hit.getType()==HitResult.Type.BLOCK){body.setPos(hit.getLocation());body.setDeltaMovement(Vec3.ZERO);}
        else{body.setPos(at.add(0,falling,0));body.setDeltaMovement(0,falling,0);}
    }
    private void retire(Actor a){actors.remove(a.body);work.remove(a);if(!a.body.isRemoved())a.body.discard();}
    public ServerPlayer owner(Entity body){var a=actors.get(body);return a==null?null:game.server().getPlayerList().getPlayer(a.owner);}
    public static boolean managed(Entity body){return body!=null&&(body.entityTags().contains(HORSE)||body.entityTags().contains(SPIKE));}
    public List<BodyView> snapshot(){return work.stream().filter(a->actors.get(a.body)==a).map(a->new BodyView(a.body,a.owner,a.kind==Kind.HORSE,a.remaining,a.due)).toList();}
    public int pendingCount(){return pending.size();}
    public void loaded(Entity body){
        if(restoring||MatchActorCheckpoint.restoring(body)||!managed(body)||spawning.contains(body)||actors.containsKey(body))return;
        if(!game.playing()||!game.items().currentEpoch(body)){body.discard();return;}
        UUID owner=null;String team=null;for(var tag:body.entityTags())try{if(tag.startsWith(OWNER))owner=UUID.fromString(tag.substring(OWNER.length()));else if(tag.startsWith(TEAM))team=tag.substring(TEAM.length());}catch(IllegalArgumentException bad){body.discard();return;}
        Kind kind=body.entityTags().contains(HORSE)&&body instanceof Horse?Kind.HORSE:body.entityTags().contains(SPIKE)&&body instanceof BlockDisplay?Kind.SPIKE:null;
        if(owner==null||kind==null){body.discard();return;}
        var level=(ServerLevel)body.level();long clock=clocks.computeIfAbsent(level,ignored->0L);
        var a=new Actor(body,owner,game.items().epoch(),team,kind,new LbcIndirectKills.Owner(owner,0,false,kind==Kind.SPIKE),kind==Kind.HORSE?Math.max(0,game.items().mana().score(body,"sprint_timer").get()):0,clock+(kind==Kind.HORSE?2:1));actors.put(body,a);work.addLast(a);attachTeam(a);
    }
    public void unloaded(Entity body){if(restoring||MatchActorCheckpoint.restoring(body))return;var a=actors.remove(body);if(a!=null)work.remove(a);}
    public void forget(ServerPlayer player){if(restoring||MatchPlayerCheckpoint.restoring(player))return;pending.removeIf(p->p.owner.equals(player.getUUID()));for(var a:List.copyOf(actors.values()))if(a.owner.equals(player.getUUID()))retire(a);}
    public void stop(){if(restoring)return;pending.clear();for(var a:List.copyOf(actors.values()))retire(a);work.clear();clocks.clear();lastTick=Integer.MIN_VALUE;}
    public void prepareFrozen(){boundary();restoring=true;}
    public void publishFrozen(){restoring=false;lastTick=outputTick=game.server().getTickCount();outputSpent=64;}
    private void boundary(){if(publishing||ticking||draining||!spawning.isEmpty())throw new IllegalStateException("Finish reactive native callbacks before checkpoint");}
    Checkpoint checkpoint(MatchSpellCheckpoint.Links links){boundary();return new Checkpoint(links);}
    private record ActorImage(MatchDamageSources.Ref body,UUID owner,String epoch,String team,Kind kind,LbcIndirectKills.Owner cause,int remaining,long due) { }
    final class Checkpoint {
        final List<Proc> saved=List.copyOf(pending);final List<ActorImage> bodies;final Map<ServerLevel,Long> time=new IdentityHashMap<>(clocks);
        Checkpoint(MatchSpellCheckpoint.Links links){bodies=work.stream().filter(a->actors.get(a.body)==a).map(a->new ActorImage(links.key(a.body),a.owner,a.epoch,a.team,a.kind,a.cause,a.remaining,a.due)).toList();}
        Runnable prepare(MatchSpellCheckpoint.Links links){var next=new ArrayList<Actor>();for(var a:bodies)next.add(new Actor(links.get(a.body),a.owner,a.epoch,a.team,a.kind,a.cause,a.remaining,a.due));return ()->{pending.clear();pending.addAll(saved);actors.clear();work.clear();for(var a:next){actors.put(a.body,a);work.addLast(a);mirror(a);}clocks.clear();clocks.putAll(time);};}
    }
    public boolean cloneRecognizes(Entity body){return actors.containsKey(body);}
    public boolean cloneProtected(Entity body){return false;}
    public LbcCloneSkills.ActorImage cloneImage(Entity source){
        var a=actors.get(source);if(a==null)return null;var owner=a.owner;var epoch=a.epoch;var team=a.team;var kind=a.kind;var cause=a.cause;int remaining=a.remaining;long wait=Math.max(0,a.due-clocks.getOrDefault((ServerLevel)source.level(),0L));
        return new LbcCloneSkills.ActorImage(){
            public void install(Entity original,Entity clone,Map<Entity,Entity> family){var level=(ServerLevel)clone.level();long clock=clocks.computeIfAbsent(level,ignored->0L);var next=new Actor(clone,owner,epoch,team,kind,cause,remaining,clock+wait);actors.put(clone,next);work.addLast(next);mirror(next);attachTeam(next);}
            public void rollback(Entity clone){var next=actors.remove(clone);if(next!=null)work.remove(next);}
        };
    }
}
