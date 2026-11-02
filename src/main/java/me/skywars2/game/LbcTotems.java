package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** The real vanilla totem selects/consumes the hand and restores health; these are its source rewards. */
public final class LbcTotems {
    private static final String MOB="skywars2.devil_totem_skeleton",OWNER=MOB+".owner.";
    private static final int[][] OFFSETS={{1,0},{0,1},{-1,0},{0,-1}};
    private final GameController game;
    private final Set<WitherSkeleton> skeletons=new LinkedHashSet<>();
    private final Set<Lava> lava=new LinkedHashSet<>();
    private final ArrayDeque<Refund> refunds=new ArrayDeque<>();
    private IdentityHashMap<ServerPlayer,Debt> deaths=new IdentityHashMap<>();
    private final Map<ServerPlayer,Deque<DamageSource>> contexts=new IdentityHashMap<>();
    private int clock;private int callbackDepth;private boolean rewinding;
    private record Debt(LbcIndirectKills.Owner cause,int phase){}
    LbcTotems(GameController game){this.game=game;}
    public void begin(ServerPlayer p,DamageSource source){
        callbackDepth++;try{contexts.computeIfAbsent(p,ignored->new ArrayDeque<>()).addLast(source);
        }finally{callbackDepth--;}
    }
    public void end(ServerPlayer p){
        callbackDepth++;try{var stack=contexts.get(p);if(stack!=null){stack.removeLast();if(stack.isEmpty())contexts.remove(p);}
        }finally{callbackDepth--;}
    }
    static boolean devil(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("deviltotem",0)==1;}
    private boolean participant(ServerPlayer p){
        var id=p.level().dimension().identifier().toString();
        return game.canUseGameplayItems(p)&&!game.awaitingRespawn(p.getUUID())&&!id.equals("minecraft:nexus")&&!id.equals("minecraft:imprinted");
    }
    private boolean running(ServerPlayer p){return participant(p)&&p.isAlive()&&!game.items().timeStop().stopped(p.level())&&!p.entityTags().contains("time_stopped");}
    private static boolean entityResident(ServerLevel level,BlockPos pos){return level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null;}
    private static boolean resident(ServerLevel level,BlockPos pos){return !level.isOutsideBuildHeight(pos)&&entityResident(level,pos);}
    int infinity(ItemStack stack,ServerLevel level){return stack.getEnchantments().getLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY));}
    boolean refundable(ItemStack stack,ServerLevel level){var data=LbcFartus.data(stack);return data.getIntOr("transformable",0)==1&&data.getIntOr("totem",0)==1&&infinity(stack,level)>0;}
    /** Runs from the actual used_totem criterion, before native health/effect restoration, exactly as the source reward. */
    public void used(ServerPlayer p,ItemStack consumed){
        if(rewinding)return;
        callbackDepth++;try{
        if(!participant(p))return;
        if(refundable(consumed,p.level())){
            refunds.addLast(new Refund(p.level(),p.position(),consumed.copyWithCount(1),infinity(consumed,p.level()),game.items().epoch()));
            refundsTick();
        }
        if(devil(consumed))burst(p);

        }finally{callbackDepth--;}
    }
    private static final class Refund {
        final ServerLevel level;final Vec3 at;ItemStack copy;final String epoch;int remaining;
        Refund(ServerLevel level,Vec3 at,ItemStack copy,int count,String epoch){this.level=level;this.at=at;this.copy=copy;remaining=count;this.epoch=epoch;}
    }
    /** A high level retains its full debt without materializing billions of entities inside one damage callback. */
    void refundsTick(){
        if(rewinding)return;
        callbackDepth++;try{
        long deadline=System.nanoTime()+2_000_000;int spawned=0,idle=0;
        while(!refunds.isEmpty()&&spawned<64&&(spawned==0||System.nanoTime()<deadline)){
            var next=refunds.removeFirst();
            if(!game.playing()||!game.items().currentEpoch(next.epoch))continue;
            if(game.items().timeStop().stopped(next.level)||!entityResident(next.level,BlockPos.containing(next.at))){refunds.addLast(next);if(++idle>=refunds.size())break;continue;}
            var entity=new ItemEntity(next.level,next.at.x,next.at.y,next.at.z,next.copy.copy());entity.setDefaultPickUpDelay();
            if(next.level.addFreshEntity(entity)){next.remaining--;spawned++;idle=0;}else idle++;
            if(next.remaining>0)refunds.addLast(next);
            if(idle>refunds.size()+1)break;
        }

        }finally{callbackDepth--;}
    }
    long pendingRefunds(){long total=0;for(var r:refunds)total+=r.remaining;return total;}
    private boolean wild(LivingEntity target){return game.items().wands().validRoleTarget(target,true)&&!LbcGreed.active(target);}
    private void harm(ServerPlayer p,LivingEntity target){
        if(!target.isAlive()||!wild(target))return;
        // Instant DamageII's native formula, with a captured WILD cause instead of a transient effect.
        if(target.isInvertedHealAndHarm())target.heal(8);
        else target.hurtServer((ServerLevel)target.level(),game.items().indirectKills().captureDamage(
                new DamageSource(target.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.MAGIC),p),
                game.items().indirectKills().wildCause(p)),12);
    }
    /** Public to native fixtures; production calls this only on the shared five-second pulse. */
    void heldPulse(ServerPlayer p){
        if(rewinding)return;
        callbackDepth++;try{
        if(!running(p))return;
        for(var hand:InteractionHand.values()){
            if(!running(p)||!devil(p.getItemInHand(hand)))continue;
            var targets=p.level().players().stream().filter(t->t!=p&&t.position().distanceToSqr(p.position())>=.01
                    &&t.position().distanceToSqr(p.position())<=400&&!p.isAlliedTo(t)&&game.items().wands().validTarget(t)).toList();
            for(var target:targets)for(var offset:OFFSETS)spawn(p,target.position().add(offset[0],0,offset[1]));
            if(!targets.isEmpty())harm(p,p);
        }

        }finally{callbackDepth--;}
    }
    private void spawn(ServerPlayer p,Vec3 at){
        var level=p.level();if(!entityResident(level,BlockPos.containing(at)))return;
        var mob=new WitherSkeleton(EntityTypes.WITHER_SKELETON,level);mob.setPos(at);
        mob.finalizeSpawn(level,level.getCurrentDifficultyAt(mob.blockPosition()),EntitySpawnReason.COMMAND,null);
        mob.entityTags().add(MOB);mob.entityTags().add(OWNER+p.getUUID());game.items().markCurrentItemEpoch(mob);
        skeletons.add(mob);game.items().timeStop().prepareFrozen(mob);if(!level.addFreshEntity(mob))skeletons.remove(mob);
    }
    public static boolean managed(Entity e){return e!=null&&e.entityTags().contains(MOB);}
    public ServerPlayer owner(Entity e){
        if(!managed(e))return null;
        for(var tag:e.entityTags())if(tag.startsWith(OWNER))try{return game.server().getPlayerList().getPlayer(UUID.fromString(tag.substring(OWNER.length())));}catch(IllegalArgumentException ignored){}
        return null;
    }
    public boolean canAffect(Entity actor,Entity target){return !managed(actor)||target==null||target instanceof LivingEntity living&&wild(living);}
    void loaded(Entity e){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(e))return;if(!managed(e))return;if(!game.playing()||!game.items().currentEpoch(e)){e.discard();return;}if(e instanceof WitherSkeleton s){skeletons.add(s);game.items().timeStop().prepareFrozen(s);}
        }finally{callbackDepth--;}
    }
    void unloaded(Entity e){
        callbackDepth++;try{if(rewinding||MatchActorCheckpoint.restoring(e))return;if(e instanceof WitherSkeleton s)skeletons.remove(s);
        }finally{callbackDepth--;}
    }
    private void burst(ServerPlayer p){
        var level=p.level();var at=p.position();var cause=game.items().indirectKills().wildCause(p);
        var stack=contexts.get(p);var original=stack==null?p.getLastDamageSource():stack.peekLast();
        var killer=original==null?null:LbcIndirectKills.playerSource(original.getEntity());
        if(killer==null)killer=LbcIndirectKills.playerSource(p.getKillCredit());
        var deathCause=killer==null||killer==p?null:game.items().indirectKills().wildCause(killer);
        game.items().mana().award(p,"lbc:true_advancements/totems/devil");
        // Native vanilla protection immediately clears the user's transient source potion effects.
        // No one-tick Resistance workaround is needed; only other entities retain the source WitherII.
        for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(10),t->wild(t)&&t.position().distanceToSqr(at)<=100)){
            if(target!=p)game.items().indirectKills().wildEffect(target,ItemEffectLevels.create(MobEffects.WITHER,1_200_000,1,true,false),cause);
            harm(p,target);
        }
        game.items().indirectKills().trapChanges(level,cause,()->{
            var center=BlockPos.containing(at);
            for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))if(resident(level,pos))level.setBlock(pos,Blocks.LAVA.defaultBlockState(),Block.UPDATE_ALL);
        });
        var selected=new ArrayList<BlockPos>();var center=BlockPos.containing(at);var origin=center.offset(-15,-15,-15);
        for(var pos:BlockPos.betweenClosed(origin,origin.offset(29,29,29)))
            if(level.getRandom().nextFloat()<.15f&&resident(level,pos)
                    &&(Math.abs(pos.getX()-center.getX())>1||Math.abs(pos.getY()-center.getY())>1||Math.abs(pos.getZ()-center.getZ())>1))selected.add(pos.immutable());
        var task=new Lava(level,selected,cause,game.items().epoch());lava.add(task);if(!task.step())game.items().stonekivi().queuedWork(task);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y,at.z,3000,1,1,1,2);
        level.sendParticles(ParticleTypes.SOUL,at.x,at.y,at.z+1,500,5,5,5,0);
        LbcOverload.sound(p,at,"minecraft:entity.wither.death",1,2,0);
        if(!LbcParasite.active(p)){game.items().mana().score(p,"death_timer").set(5);deaths.put(p,new Debt(deathCause,0));}
    }
    private final class Lava implements LbcStonekivi.Work {
        final ServerLevel level;final List<BlockPos> cells;final LbcIndirectKills.Owner cause;final String epoch;int cursor;boolean cancelled;
        Lava(ServerLevel level,Collection<BlockPos> cells,LbcIndirectKills.Owner cause,String epoch){this.level=level;this.cells=cells.stream().map(BlockPos::immutable).toList();this.cause=cause;this.epoch=epoch;}
        @Override public boolean step(){
        if(rewinding)return false;
        callbackDepth++;try{
            if(cancelled||!game.items().currentEpoch(epoch)||!game.playing()){cancel();return true;}
            if(game.items().timeStop().stopped(level))return false;
            long deadline=System.nanoTime()+1_000_000;
            game.items().indirectKills().trapChanges(level,cause,()->{
                for(int n=0;n<256&&cursor<cells.size()&&(n==0||System.nanoTime()<deadline);n++){
                    var pos=cells.get(cursor++);if(resident(level,pos))level.setBlock(pos,Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL,8),Block.UPDATE_ALL);
                }
            });
            if(cursor>=cells.size()){lava.remove(this);return true;}return false;

        }finally{callbackDepth--;}
    }
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            checkpointBoundary();int next=cursor;boolean stopped=cancelled;
            return links->{checkpointBoundary();MatchAuthorityCheckpoint.world(game,level);return ()->{cursor=next;cancelled=stopped;};};
        }
        @Override public void cancel(){
        callbackDepth++;try{cancelled=true;lava.remove(this);
        }finally{callbackDepth--;}
    }
    }
    /** Source death_timer is five two-tick units; it bypasses another totem, with source void/Greed exemptions. */
    void debtsTick(){
        if(rewinding)return;
        callbackDepth++;try{for(var entry:List.copyOf(deaths.entrySet())){
        var p=entry.getKey();var debt=entry.getValue();
        if(LbcParasite.active(p)){clear(p);continue;}
        if(!p.isAlive()||p.isRemoved()||!participant(p)){deaths.remove(p);continue;}
        if(!running(p))continue;
        if(debt.phase==0){deaths.put(p,new Debt(debt.cause,1));continue;}
        int remaining=game.items().mana().hudValue(p,"death_timer");
        if(remaining>0)game.items().mana().score(p,"death_timer").set(--remaining);
        if(remaining<=0){deaths.remove(p);if(game.items().mana().hudValue(p,"void_resistance")<=0&&!LbcGreed.active(p)){
            game.items().indirectKills().inheritedKill(p,debt.cause);
        }}else deaths.put(p,new Debt(debt.cause,0));
    }
        }finally{callbackDepth--;}
    }
    void tick(){
        if(rewinding)return;
        callbackDepth++;try{
        if(!game.server().tickRateManager().runsNormally())return;
        skeletons.removeIf(Entity::isRemoved);debtsTick();refundsTick();
        if(++clock>=100){clock=0;for(var p:List.copyOf(game.server().getPlayerList().getPlayers()))heldPulse(p);}

        }finally{callbackDepth--;}
    }
    int actors(){return skeletons.size();}int work(){return lava.size();}int pendingDeaths(){return deaths.size();}
    List<WitherSkeleton> snapshot(){return List.copyOf(skeletons);}
    void clear(ServerPlayer p){
        callbackDepth++;try{deaths.remove(p);game.items().mana().score(p,"death_timer").set(0);
        }finally{callbackDepth--;}
    }
    void stop(){
        callbackDepth++;try{for(var task:List.copyOf(lava))task.cancel();for(var actor:List.copyOf(skeletons))actor.discard();skeletons.clear();deaths.clear();contexts.clear();refunds.clear();clock=0;for(var p:game.server().getPlayerList().getPlayers())clear(p);
        }finally{callbackDepth--;}
    }

    /** The native damage context is a live stack frame, never a resumable debt or saved callback. */
    void checkpointBoundary(){if(callbackDepth!=0||!contexts.isEmpty())throw new IllegalStateException("Finish native totem damage/reward callbacks before checkpointing");}
    void prepareFrozen(){checkpointBoundary();rewinding=true;}
    void publishFrozen(){checkpointBoundary();rewinding=false;}
    void cancelFrozen(){
        checkpointBoundary();if(rewinding)for(var task:List.copyOf(lava))task.cancel();rewinding=false;
    }
    Checkpoint checkpoint(MatchQueuedWorkCheckpoint.Graph graph){checkpointBoundary();return new Checkpoint(graph);}
    final class Checkpoint {
        private record RefundImage(Refund original,ItemStack payload,int remaining){}
        private final List<MatchDamageSources.Ref> actors;
        private final List<Lava> work=List.copyOf(lava);
        private final List<RefundImage> debts;
        private final MatchSpellCheckpoint.IdentityImage<ServerPlayer,Debt> deathImage;
        private final int savedClock=clock;
        Checkpoint(MatchQueuedWorkCheckpoint.Graph graph){
            if(rewinding)throw new IllegalStateException("Publish native totem state before checkpointing");
            actors=skeletons.stream().map(body->{var ref=graph.links.key(body);if(ref.historical()!=null)throw new IllegalStateException("Totem summon requires its canonical native body owner");return ref;}).toList();
            deathImage=new MatchSpellCheckpoint.IdentityImage<>(deaths,graph.links);
            debts=refunds.stream().map(r->new RefundImage(r,r.copy.copy(),r.remaining)).toList();
            work.forEach(graph::capture);
        }
        Runnable prepare(MatchSpellCheckpoint.Links links){
            checkpointBoundary();var bodies=actors.stream().map(ref->(WitherSkeleton)links.get(ref)).toList();
            var restoredDeaths=deathImage.restore(links);var payloads=debts.stream().map(r->r.payload.copy()).toList();
            for(var debt:debts)MatchAuthorityCheckpoint.world(game,debt.original.level);
            return ()->{skeletons.clear();skeletons.addAll(bodies);lava.clear();lava.addAll(work);deaths=restoredDeaths;
                refunds.clear();for(int i=0;i<debts.size();i++){var image=debts.get(i);image.original.copy=payloads.get(i);image.original.remaining=image.remaining;refunds.addLast(image.original);}
                clock=savedClock;};
        }
    }
}
