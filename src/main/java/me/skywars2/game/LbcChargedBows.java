package me.skywars2.game;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Accepted bow families become discrete rays on the existing fair queue, never extra arrows. */
public final class LbcChargedBows {
    /** The shared final-family transaction supplies all ammo effects and already rolled traits. */
    public interface BeamPayload {
        int variant();
        default void blockContact(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){}
        void sample(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause,boolean railgun);
        MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links);
    }
    @FunctionalInterface public interface Payloads {
        BeamPayload capture(ServerPlayer owner,ItemStack weapon,List<Projectile> finalizedFamily);
    }
    private static final TagKey<Block> TANNERKIVI=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:tannerkivi"));
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private static final int[][] LEAF_VOLUMES={{-2,-5,-2,2,7,2},{-5,-2,-2,5,4,2},{-2,-2,-5,2,4,5},{-3,-3,-3,3,6,3}};
    private static final int[][] FIRE_VOLUME={{-2,-2,-2,2,2,2}};
    private static final TicketType TICKET=new TicketType(0,TicketType.FLAG_LOADING|TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    private static final DustParticleOptions CYAN=new DustParticleOptions(0x40bfff,.8f);
    private static final DustParticleOptions GREEN=new DustParticleOptions(0x22eb9e,.5f);
    private static final DustParticleOptions GREEN_HIT=new DustParticleOptions(0x02ff02,1);
    private static final BlockParticleOption LEAVES=new BlockParticleOption(ParticleTypes.BLOCK,Blocks.SPRUCE_LEAVES.defaultBlockState());
    private final GameController game;
    private final Payloads payloads;
    private final LbcChargedBowCrafting crafting;
    private final Set<BeamWork> jobs=new LinkedHashSet<>();
    private final Set<Beam> beams=new LinkedHashSet<>();
    private record Chunk(ServerLevel level,ChunkPos pos) { }
    private static final class Loading {
        java.util.concurrent.CompletableFuture<?> future;int users;
        Loading(java.util.concurrent.CompletableFuture<?> future){this.future=future;}
    }
    private final Map<Chunk,Loading> loading=new HashMap<>();
    static void register(){Registry.register(BuiltInRegistries.TICKET_TYPE,Identifier.parse("skywars2:charged_bow"),TICKET);}
    public LbcChargedBows(GameController game,Payloads payloads){this.game=game;this.payloads=payloads;this.crafting=new LbcChargedBowCrafting(game);}
    LbcChargedBowCrafting crafting(){return crafting;}
    public void loaded(Entity body){crafting.loaded(body);}
    public void unloaded(Entity body){crafting.unloaded(body);}
    public void itemChanged(net.minecraft.world.entity.item.ItemEntity item){crafting.itemChanged(item);}
    public static boolean matches(ItemStack stack){return LbcChargedBowItems.matches(stack);}
    public static int range(boolean shish,int charge){return shish?Math.min(700,Math.max(0,charge)/4)
            :charge>=75?300:charge>=20?charge*4:Math.max(0,charge)/2;}
    private boolean admitted(ServerPlayer player){return game.playing()&&player.isAlive()&&!player.isRemoved()
            &&game.canUseGameplayItems(player)&&LbcDimensionItems.ordinary(player.level());}
    private boolean jammed(ServerLevel level,Vec3 at,boolean shish){return game.items().magic().mechanismSuppressedAt(level,at)
            ||game.items().magic().artifactSuppressedAt(level,at)||shish&&game.items().magic().suppressedAt(level,at);}
    /** Call after successful native debit/trait roll, before any ballistic family is allowed to tick. */
    public boolean release(ServerPlayer owner,ItemStack weapon,int activeCharge,List<Projectile> family){
        if(!matches(weapon))return false;
        boolean shish=LbcChargedBowItems.shishgun(weapon);
        // Capture the actual native family before retirement; no nearest-entity search or second debit.
        var accepted=List.copyOf(family);BeamPayload payload=null;
        if(admitted(owner)&&!game.items().timeStop().stopped(owner.level())&&!jammed(owner.level(),owner.position(),shish))
            payload=payloads.capture(owner,weapon.copy(),accepted);
        for(var projectile:accepted)projectile.discard();
        if(payload==null)return true;
        owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,20,4,false,shish,shish),owner);
        var eye=owner.getEyePosition();var direction=owner.getLookAngle().normalize();
        LbcOverload.sound(owner,owner.position(),"minecraft:item.trident.return",10,2,shish?.7f:.2f);
        LbcOverload.sound(owner,owner.position(),shish?"minecraft:block.azalea_leaves.break":"minecraft:entity.generic.explode",.3f,.8f,.3f);
        int distance=range(shish,activeCharge);
        if(shish&&distance<6)return true;
        int count=shish?distance:Math.max(1,distance);
        var cause=game.items().indirectKills().wildCause(owner);
        game.items().exasperato().replayableSamples(owner,eye.add(direction.scale(shish?3:2)),direction,count-1,
                new Beam(owner,payload,cause,!shish,false));
        return true;
    }
    /** Caller supplies its existing active-time bow charge; this method never adds another counter. */
    public void charging(ServerPlayer owner,ItemStack weapon,int activeCharge){
        if(!LbcChargedBowItems.shishgun(weapon)||!admitted(owner)
                ||game.items().timeStop().stopped(owner.level()))return;
        var eye=owner.getEyePosition();var direction=owner.getLookAngle().normalize();
        particles(owner.level(),LEAVES,eye.add(direction.scale(2)),1,0);
        if(activeCharge<5||activeCharge%8!=0)return;
        particles(owner.level(),LEAVES,eye.add(direction.scale(2)),1,0);
        LbcOverload.sound(owner,eye,"minecraft:entity.firework_rocket.blast",10,1,0);
        owner.sendSystemMessage(Component.literal("<===> : ").withStyle(net.minecraft.ChatFormatting.DARK_GREEN)
                .append(Component.literal(Integer.toString(activeCharge/4)).withStyle(net.minecraft.ChatFormatting.GREEN)));
        int count=Math.max(1,range(true,activeCharge));
        game.items().exasperato().replayableSamples(owner,eye,direction,count-1,new Beam(owner,null,null,false,true));
    }
    private final class Beam implements LbcExasperato.ReplayableRay {
        private final UUID author;private final ServerLevel level;private final String epoch;
        private final BeamPayload payload;private final LbcIndirectKills.Owner cause;
        private final boolean railgun,preview;private Vec3 direction;private boolean firstSample=true,homingLost;
        private LbcExasperato.RayTicket ticket;
        Beam(ServerPlayer owner,BeamPayload payload,LbcIndirectKills.Owner cause,boolean railgun,boolean preview){
            author=owner.getUUID();level=owner.level();epoch=game.items().epoch();this.payload=payload;this.cause=cause;
            this.railgun=railgun;this.preview=preview;direction=owner.getLookAngle().normalize();
        }
        public boolean block(BlockPos pos,BlockState state){return false;}
        public boolean target(Entity entity){return false;}
        public void trail(Vec3 from,Vec3 to){}
        public void submitted(LbcExasperato.RayTicket ticket){this.ticket=ticket;beams.add(this);}
        public void complete(HitResult hit){beams.remove(this);}
        public double sampleStep(){return 1;}
        public HitResult sampleAt(Vec3 point){
            if(!game.items().currentEpoch(epoch))return null;
            if(preview){
                level.sendParticles(GREEN,point.x,point.y,point.z,1,0,0,0,4);
                var back=point.subtract(direction.scale(.5));
                if(!level.getBlockState(BlockPos.containing(back)).is(AIRS))level.sendParticles(GREEN_HIT,back.x,back.y,back.z,40,.1,.1,.1,4);
                return null;
            }
            // Capture terrain contacts before the native blast changes its block. All traits
            // retain the same accepted family and cannot become extra ordinary-arrow attacks.
            if(!level.getBlockState(BlockPos.containing(point)).is(AIRS))payload.blockContact(level,point,cause);
            payload.sample(level,point,cause,railgun);
            if(railgun){
                if(payload.variant()==101&&level.getRandom().nextFloat()<.35f)enqueue(new BeamWork(level,point,cause,1,2));
                if(payload.variant()==111)enqueue(new BeamWork(level,point,cause,0,1));
                if(payload.variant()!=302)blast(level,point,cause,1,false);
                if(payload.variant()==301)blast(level,point,cause,1,false);
                particles(level,ParticleTypes.EXPLOSION,point,1,1.5);particles(level,CYAN,point,5,.1);
            }else{
                blast(level,point,cause,4,true);enqueue(new BeamWork(level,point,cause,3,0));
                for(float pitch:new float[]{2,1,1.5f})sound(level,point,"minecraft:block.azalea_leaves.break",.2f,pitch);
                particles(level,LEAVES,point,3,.2);
            }
            return null;
        }
        public boolean sampleContinues(Vec3 point){
            if(!railgun)return true;
            var next=point.add(direction);
            return !homingLost&&!jammed(level,next,false)&&(payload.variant()!=300||nearest(next)!=null);
        }
        private net.minecraft.world.entity.LivingEntity nearest(Vec3 point){
            return game.items().lifetimes().nearestBeamTarget(level,point,e->!e.getUUID().equals(author)
                    &&!e.isSpectator()&&!e.entityTags().contains("spectator")&&!e.entityTags().contains("strict_map_object")
                    &&!(e instanceof net.minecraft.world.entity.monster.Creeper)&&e.getType().builtInRegistryHolder().is(MOBS));
        }
        public Vec3 directionAt(Vec3 point,Vec3 previous){
            if(!firstSample&&!preview&&railgun&&payload.variant()==300){
                var target=nearest(point);homingLost=target==null;
                if(target!=null){var delta=target.position().subtract(point);if(delta.lengthSqr()>1e-12)direction=delta.normalize();}
            }
            firstSample=false;
            return direction;
        }
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
            var savedDirection=direction;var state=payload==null?null:payload.checkpoint(links);
            boolean savedFirst=firstSample,savedLost=homingLost,savedActive=beams.contains(this);
            return rebound->{var plan=state==null?null:state.prepare(rebound);return ()->{direction=savedDirection;firstSample=savedFirst;homingLost=savedLost;
                if(savedActive)beams.add(this);else beams.remove(this);if(plan!=null)plan.run();};};
        }
    }
    private void blast(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause,float radius,boolean tnt){
        if(!level.hasChunkAt(BlockPos.containing(point)))return;
        var damage=game.items().indirectKills().captureDamage(level.damageSources().explosion(null,null),cause);
        game.items().indirectKills().trapChanges(level,cause,()->level.explode(null,damage,null,point,radius,false,
                tnt?Level.ExplosionInteraction.TNT:Level.ExplosionInteraction.MOB));
    }
    private static void particles(ServerLevel level,net.minecraft.core.particles.ParticleOptions particle,Vec3 at,int count,double spread){level.sendParticles(particle,at.x,at.y,at.z,count,spread,spread,spread,0);}
    private void sound(ServerLevel level,Vec3 at,String sound,float volume,float pitch){
        level.playSound(null,at.x,at.y,at.z,net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse(sound)),net.minecraft.sounds.SoundSource.PLAYERS,volume,pitch);
    }
    private void enqueue(BeamWork job){jobs.add(job);game.items().stonekivi().queuedWork(job);}
    /** Delay and terrain cursor are typed shared-queue work, with no helper entities or tick NBT. */
    private final class BeamWork implements LbcStonekivi.Work {
        final ServerLevel level;final Vec3 point;final BlockPos center;final LbcIndirectKills.Owner cause;
        final String epoch;final int kind;int delay,volume,cursor;boolean cancelled;
        Chunk waiting;
        int createdTick;
        BeamWork(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause,int delay,int kind){
            this.level=level;this.point=point;center=BlockPos.containing(point);this.cause=cause;this.delay=delay;this.kind=kind;
            epoch=game.items().epoch();createdTick=game.server().getTickCount();
        }
        public boolean ready(){return cancelled||!game.items().currentEpoch(epoch)||delay<=0&&!game.items().timeStop().stopped(level)
                &&(waiting==null||level.getChunkSource().getChunkNow(waiting.pos.x(),waiting.pos.z())!=null||loading.get(waiting).future.isCompletedExceptionally());}
        public boolean step(){
            if(cancelled||!game.items().currentEpoch(epoch)||!game.playing()){cancel();return true;}
            if(kind==2){if(!resident(this,center))return false;blast(level,point,cause,4,true);cancel();return true;}
            // Source instant magic/mecha actor remains subject to both source jammer categories.
            if(kind==0&&jammed(level,point,true)){cancel();return true;}
            int[][] volumes=kind==0?LEAF_VOLUMES:FIRE_VOLUME;
            for(int n=0;n<128&&volume<volumes.length;n++){
                int[] b=volumes[volume];int sx=b[3]-b[0]+1,sy=b[4]-b[1]+1,sz=b[5]-b[2]+1;
                if(cursor>=sx*sy*sz){volume++;cursor=0;continue;}
                var pos=center.offset(b[0]+cursor%sx,b[1]+cursor/sx%sy,b[2]+cursor/(sx*sy));
                if(level.isOutsideBuildHeight(pos)){cursor++;continue;}
                if(!resident(this,pos))return false;
                cursor++;
                var state=level.getBlockState(pos);if(!state.is(kind==0?TANNERKIVI:AIRS))continue;
                var result=kind==0?Blocks.SPRUCE_LEAVES.defaultBlockState():Blocks.FIRE.defaultBlockState();
                game.items().indirectKills().capturedWorldChanges(level,cause,()->LbcDropBlocks.setBlock(level,pos,result,kind==0));
            }
            if(volume>=volumes.length){cancel();return true;}return false;
        }
        public void cancel(){cancelled=true;jobs.remove(this);release(this);}
        public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            int savedDelay=delay,savedVolume=volume,savedCursor=cursor;boolean savedCancelled=cancelled;
            return links->{MatchAuthorityCheckpoint.world(game,level);return ()->{release(this);delay=savedDelay;volume=savedVolume;cursor=savedCursor;
                cancelled=savedCancelled;createdTick=game.server().getTickCount();if(!cancelled)jobs.add(this);};};
        }
    }
    private void release(BeamWork job){
        if(job.waiting==null)return;
        var key=job.waiting;job.waiting=null;var request=loading.get(key);
        if(request!=null&&--request.users==0){key.level.getChunkSource().removeTicketWithRadius(TICKET,key.pos,0);loading.remove(key);}
    }
    private boolean resident(BeamWork job,BlockPos pos){
        var key=new Chunk(job.level,new ChunkPos(pos.getX()>>4,pos.getZ()>>4));
        if(job.waiting!=null&&!job.waiting.equals(key))release(job);
        if(job.waiting==null){var request=loading.computeIfAbsent(key,k->new Loading(k.level.getChunkSource().addTicketAndLoadWithRadius(TICKET,k.pos,0)));request.users++;job.waiting=key;}
        if(job.level.getChunkSource().getChunkNow(key.pos.x(),key.pos.z())!=null)return true;
        var request=loading.get(key);
        if(request.future.isCompletedExceptionally()||request.future.isCancelled()){
            key.level.getChunkSource().removeTicketWithRadius(TICKET,key.pos,0);
            request.future=key.level.getChunkSource().addTicketAndLoadWithRadius(TICKET,key.pos,0);
        }
        return false;
    }
    public void tick(){
        crafting.tick();int now=game.server().getTickCount();
        for(var job:List.copyOf(jobs))if(job.cancelled||!game.items().currentEpoch(job.epoch))job.cancel();
        else if(now!=job.createdTick&&!game.items().timeStop().stopped(job.level)){
            if(job.kind==0&&jammed(job.level,job.point,true)){job.cancel();continue;}
            if(job.delay>0)job.delay--;job.createdTick=now;
        }
    }
    public int pending(){return jobs.size();}
    public void stop(){crafting.stop();for(var beam:List.copyOf(beams))if(beam.ticket!=null)beam.ticket.cancel();beams.clear();for(var job:List.copyOf(jobs))job.cancel();jobs.clear();}
    void prepareFrozen(){beams.clear();for(var job:List.copyOf(jobs))release(job);jobs.clear();}
}
