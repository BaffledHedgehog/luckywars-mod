package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;
import static me.skywars2.game.LbcHedgehogItems.SpikeKind;

/** Seven real wall drops: six native arrow transports and the authored piercing impulse ray. */
public final class LbcHedgehogSpikes {
    private static final String TAG="skywars2.hedgehog_spike";
    private final GameController game;
    private final List<SpikeArrow> arrows=new ArrayList<>();
    private final List<Pending> pending=new ArrayList<>();
    private final Map<LivingEntity,Water> water=new IdentityHashMap<>();
    private static final String[] THROW_FLAGS={"confuse","blind","boom","standart","wither","poison","smoke","smoke_old","smoke_bomb"};
    private record Pending(LivingEntity target,SpikeKind kind,ServerPlayer author,LbcIndirectKills.Owner cause,String epoch){}
    private record Water(MobEffectInstance effect,ServerPlayer author,LbcIndirectKills.Owner cause,String epoch){}
    public LbcHedgehogSpikes(GameController game){this.game=game;}
    /** Ordinary darts use the same native arrow transport as the wall's six spikes. */
    public static int legacyThrow(ItemStack stack){
        if(!stack.is(Items.SNOWBALL)&&!LbcClownKit.smoke(stack))return 0;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(data.getIntOr("special",0)!=1)return 0;
        for(int i=0;i<THROW_FLAGS.length;i++)if(data.getIntOr(THROW_FLAGS[i],0)==1)return i+1;
        return 0;
    }
    public boolean useLegacy(ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);int variant=legacyThrow(stack);
        if(variant==0||!game.playing()||game.stage()==GameController.Stage.ENDING||!p.isAlive()||game.isObserver(p)
                ||!game.canUseGameplayItems(p)||!LbcDimensionItems.ordinary(p)||frozen(p)||game.items().actionsBlocked(p))return false;
        if(variant==9&&(game.items().magic().mechanismSuppressed(p)||!game.items().clownKit().smokeAllowed(p.level(),p.getEyePosition())))return false;
        var transport=new Snowball(p.level(),p,stack);transport.shootFromRotation(p,p.getXRot(),p.getYRot(),0,1.5f,1);
        if(emitLegacy(p,variant,transport.position(),transport.getDeltaMovement())==null)return false;
        LbcOverload.sound(p,p.position(),"minecraft:entity.snowball.throw",.5f,.4f/(p.getRandom().nextFloat()*.4f+.8f),0);
        p.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,p);return true;
    }
    public static boolean managed(Entity e){return e!=null&&e.entityTags().contains(TAG);}
    private boolean resident(ServerLevel level,Vec3 at){var b=BlockPos.containing(at);return level.getChunkSource().getChunkNow(b.getX()>>4,b.getZ()>>4)!=null;}
    private boolean frozen(Entity e){return !game.server().tickRateManager().runsNormally()||game.items().timeStop().frozen(e)||e.entityTags().contains("time_stopped")
            ||game.items().wands().timeStopped()||e instanceof LivingEntity l&&game.items().frost().isFrozen(l);}
    private boolean allowed(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING&&p.isAlive()&&!p.isRemoved()
            &&LbcDimensionItems.ordinary(p)&&game.canUseGameplayItems(p)&&!game.isObserver(p)&&!frozen(p)&&!game.items().actionsBlocked(p)
            &&!game.items().magic().suppressed(p)&&!game.items().magic().suppressedAt(p.level(),p.getEyePosition());}
    private boolean valid(SpikeArrow a){return !a.isRemoved()&&game.playing()&&game.items().currentEpoch(a)
            &&a.getOwner() instanceof ServerPlayer p&&p.level()==a.level()&&resident((ServerLevel)a.level(),a.position());}
    private boolean effectTarget(LivingEntity target){return game.items().wands().validTarget(target)
            &&!frozen(target)&&!game.items().magic().suppressedAt((ServerLevel)target.level(),target.position())
            &&(!(target instanceof ServerPlayer p)||!game.items().magic().rejectCustomEffect(p));}
    private boolean immune(LivingEntity target){return game.items().mana().score(ScoreHolder.forNameOnly("#lbcskill"),"swrg.math").get()==1&&game.items().mana().score(target,"lbc.skill").get()==19;}
    public boolean use(ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);var kind=LbcHedgehogItems.spike(stack);if(kind==null||!allowed(p))return false;
        var transport=new Snowball(p.level(),p,stack);transport.shootFromRotation(p,p.getXRot(),p.getYRot(),0,1.5f,1);
        if(kind==SpikeKind.IMPULSE)impulse(p);else if(emit(p,kind,transport.position(),transport.getDeltaMovement())==null)return false;
        LbcOverload.sound(p,p.position(),"minecraft:entity.snowball.throw",.5f,.4f/(p.getRandom().nextFloat()*.4f+.8f),0);
        p.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,p);return true;
    }
    private static double baseDamage(SpikeKind kind){return switch(kind){case STANDARD->10;case BLIND,POISON->1.5;case BOOM->.01;default->1;};}
    private static int color(SpikeKind kind){return switch(kind){case CONFUSE->16746227;case BLIND->16771895;case POISON->1119083;case WITHER->0;case BOOM->9079434;default->-1;};}
    private static ItemStack pickup(SpikeKind kind){return pickup(kind,0);}
    private static ItemStack pickup(SpikeKind kind,int legacy){
        var result=new ItemStack(kind==SpikeKind.STANDARD?Items.ARROW:Items.TIPPED_ARROW);
        int tint=legacy==0?color(kind):switch(legacy){case 1->11733922;case 2->0;case 3->16756516;case 5->7368816;case 6->392982;default->16777215;};
        if(kind!=SpikeKind.STANDARD){result.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.of(tint),List.of(),Optional.empty()));result.set(DataComponents.POTION_DURATION_SCALE,1f);}
        return result;
    }
    private final class SpikeArrow extends Arrow {
        final SpikeKind kind;final UUID originalAuthor;final int legacy;boolean retired,blockDone;
        SpikeArrow(ServerPlayer p,SpikeKind kind){this(p,kind,0);}
        SpikeArrow(ServerPlayer p,SpikeKind kind,int legacy){super(p.level(),p,pickup(kind,legacy),null);this.kind=kind;this.legacy=legacy;originalAuthor=p.getUUID();
            setBaseDamage(legacy==0?baseDamage(kind):switch(legacy){case 1->3;case 2,6->2.5;case 3->2;case 4->4;case 5->1.8;default->.01;});
            if(kind!=SpikeKind.BOOM||legacy>0)pickup=legacy==3||legacy>=7?AbstractArrow.Pickup.DISALLOWED:AbstractArrow.Pickup.CREATIVE_ONLY;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override protected boolean canHitEntity(Entity target){
            if(!(getOwner() instanceof ServerPlayer p))return false;
            // Greed must remain a physical contact so the native guard can reflect before any payload.
            return super.canHitEntity(target)&&(LbcGreed.active(target)||game.items().wands().physicalProjectileTarget(this,target));
        }
        @Override public void tick(){if(!valid(this)){retire(this);return;}if(frozen(this))return;
            if(legacy==0&&game.items().magic().suppressedAt((ServerLevel)level(),position())){retire(this);return;}
            if(legacy==9&&(!game.items().clownKit().smokeAllowed((ServerLevel)level(),position())
                    ||Double.isFinite(game.items().magic().mechanismSuppressionFraction((ServerLevel)level(),position(),position().add(getDeltaMovement()))))){retire(this);return;}
            super.tick();}
        @Override protected void doPostHurtEffects(LivingEntity target){
            // Native AbstractArrow invokes this only after an accepted hurt, including shield admission.
            if(getOwner() instanceof ServerPlayer p){if(legacy>0)legacyImpact(this,target,p,target.position());else accepted(this,target,p);}
        }
        @Override protected void onHitBlock(BlockHitResult result){
            super.onHitBlock(result);if(blockDone||retired)return;blockDone=true;
            if(legacy>0&&valid(this)&&getOwner() instanceof ServerPlayer p)legacyImpact(this,null,p,result.getLocation());
            else if(kind==SpikeKind.BOOM&&valid(this)&&getOwner() instanceof ServerPlayer p&&!game.items().magic().suppressedAt((ServerLevel)level(),position()))
                game.items().hedgehog().spawnWall((ServerLevel)level(),position(),p,Vec3.directionFromRotation(getXRot(),getYRot()));
            retire(this);
        }
    }
    /** Native launch boundary also permits deterministic real-impact checks without inventing flight physics. */
    public Arrow emit(ServerPlayer p,SpikeKind kind,Vec3 at,Vec3 velocity){
        if(kind==SpikeKind.IMPULSE||!allowed(p)||!resident(p.level(),at))return null;
        var a=new SpikeArrow(p,kind);a.setPos(at);a.setDeltaMovement(velocity);a.entityTags().add(TAG);game.items().markCurrentItemEpoch(a);arrows.add(a);
        LbcShooters.orient(a);game.items().timeStop().prepareFrozen(a);if(!p.level().addFreshEntity(a)||a.isRemoved()){retire(a);return null;}return a;
    }
    Arrow emitLegacy(ServerPlayer p,int variant,Vec3 at,Vec3 velocity){
        if(variant<1||variant>9||!resident(p.level(),at)||variant==9&&!game.items().clownKit().smokeAllowed(p.level(),at))return null;
        var kind=switch(variant){case 1->SpikeKind.CONFUSE;case 2->SpikeKind.BLIND;case 3->SpikeKind.BOOM;case 4->SpikeKind.STANDARD;case 5->SpikeKind.WITHER;default->SpikeKind.POISON;};
        var arrow=new SpikeArrow(p,kind,variant);arrow.setPos(at);arrow.setDeltaMovement(velocity);arrow.entityTags().add(TAG);
        game.items().markCurrentItemEpoch(arrow);arrows.add(arrow);LbcShooters.orient(arrow);game.items().timeStop().prepareFrozen(arrow);
        if(!p.level().addFreshEntity(arrow)||arrow.isRemoved()){retire(arrow);return null;}return arrow;
    }
    private void legacyImpact(SpikeArrow arrow,LivingEntity target,ServerPlayer owner,Vec3 at){
        if(!valid(arrow)||target!=null&&!game.items().wands().validTarget(target))return;
        var cause=game.items().indirectKills().wildCause(owner);
        switch(arrow.legacy){
            case 1->{if(target!=null&&!immune(target)&&(!(target instanceof ServerPlayer player)||!game.items().magic().rejectCustomEffect(player))){
                boolean borders=game.config().noBorders;var center=borders?target.position():Vec3.ZERO;
                int ceiling=target.level().dimension().equals(net.minecraft.world.level.Level.NETHER)?120:target.level().getMaxY()-1;
                var destination=LbcSurfaceTeleport.destination((ServerLevel)target.level(),center,borders?140:190,ceiling,owner.getRandom());
                if(destination!=null&&target.teleportTo((ServerLevel)target.level(),destination.x,destination.y,destination.z,Set.of(),target.getYRot(),target.getXRot(),true))
                    game.items().indirectKills().teleported(target,owner,true);
            }}
            case 2,5,6->{if(target!=null){var effect=ItemEffectLevels.create(arrow.legacy==2?MobEffects.BLINDNESS:arrow.legacy==5?MobEffects.WITHER:MobEffects.POISON,
                    arrow.legacy==2?120:100,arrow.legacy==2?0:2);game.items().indirectKills().wildEffect(target,effect,cause);}}
            case 3->{if((target==null||!immune(target))&&!game.items().magic().mechanismSuppressedAt(owner.level(),at)&&!game.items().magic().luckyEffectWarded(owner.level(),at))
                    game.items().babylonEffects().primeExplosionAt(owner.level(),at,cause);}
            case 7,8->game.items().reactiveArtifacts().grenadeSmoke(owner,at,arrow.legacy==8);
            case 9->{game.items().clownKit().gas(owner,at);retire(arrow);}
            default->{}
        }
    }
    private void accepted(SpikeArrow arrow,LivingEntity target,ServerPlayer author){
        if(!valid(arrow)||!effectTarget(target)||!game.items().wands().physicalProjectileTarget(arrow,target)||immune(target))return;
        var cause=game.items().indirectKills().coldCause(author,true);
        switch(arrow.kind){
            case BLIND->game.items().psycho().afflict(target,author,0,0,0,6000);
            case WITHER->{var effect=ItemEffectLevels.create(MobEffects.WATER_BREATHING,600,1);if(target.addEffect(effect,author)){
                var current=target.getEffect(MobEffects.WATER_BREATHING);if(current!=null&&current.getAmplifier()==1)water.put(target,new Water(current,author,cause,game.items().epoch()));
                game.items().magic().colors().effectsChanged(target);}}
            case CONFUSE,POISON,BOOM->pending.add(new Pending(target,arrow.kind,author,cause,game.items().epoch()));
            default->{}
        }
    }
    void impulse(ServerPlayer p){
        var origin=p.getEyePosition();var direction=p.getLookAngle().normalize();var level=p.level();var cause=game.items().indirectKills().coldCause(p,true);
        var source=game.items().indirectKills().captureDamage(new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.ARROW),p),cause);
        for(int i=0;i<=1000;i++){
            var at=origin.add(direction.scale(i*.25));if(!resident(level,at)||game.items().magic().suppressedAt(level,at))return;
            level.sendParticles(new DustParticleOptions(0xd2e3e4,1),at.x,at.y,at.z,1,0,0,0,0);
            var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(at.x,at.y,at.z,at.x+1,at.y+1,at.z+1),e->game.items().wands().projectileTarget(p,e));
            if(!targets.isEmpty())targets.getFirst().hurtServer(level,source,7);
            // Source checks the next sample's air before recursing; entities never terminate this ray.
            var next=at.add(direction.scale(.25));if(!resident(level,next)||!level.getBlockState(BlockPos.containing(next)).isAir())return;
        }
    }
    public void tick(){
        if(!game.server().tickRateManager().runsNormally())return;
        arrows.removeIf(Entity::isRemoved);
        for(var entry:List.copyOf(pending)){
            var target=entry.target;
            if(!game.items().currentEpoch(entry.epoch)||!target.isAlive()||target.isRemoved()){pending.remove(entry);continue;}
            if(frozen(target)||LbcGreed.active(target))continue;
            pending.remove(entry);if(!effectTarget(target)||immune(target))continue;
            switch(entry.kind){
                case CONFUSE->game.items().hedgehogDimensions().teleport(target,entry.author);
                case POISON->resetWitch(target);
                case BOOM->game.items().hedgehog().spawnWall((ServerLevel)target.level(),target.position(),entry.author,target.getLookAngle());
                default->{}
            }
        }
        for(var item:List.copyOf(water.entrySet())){
            var target=item.getKey();var state=item.getValue();var current=target.getEffect(MobEffects.WATER_BREATHING);
            if(!game.items().currentEpoch(state.epoch)||target.isRemoved()||!target.isAlive()||current!=state.effect){water.remove(target);continue;}
            if(frozen(target)||!effectTarget(target))continue;
            // Existing native enhanced-water service owns gameplay. Restore its original spike author
            // on the derived Wither instance, which is refreshed by that service without a source.
            var wither=target.getEffect(MobEffects.WITHER);
            if(wither!=null&&wither.getAmplifier()==3&&target.level().getBlockState(target.blockPosition()).is(Blocks.AIR))
                game.items().indirectKills().effectChanged(target,wither,state.author);
        }
    }
    private void resetWitch(LivingEntity target){
        game.items().legacyEffects().resetWitch(target);
    }
    private void retire(SpikeArrow a){a.retired=true;arrows.remove(a);a.discard();}
    public ServerPlayer owner(Entity e){return e instanceof LbcHedgehogSpikes.SpikeArrow a&&a.getOwner() instanceof ServerPlayer p?p:null;}
    public void loaded(Entity e){if(managed(e)&&!arrows.contains(e))e.discard();}
    public void unloaded(Entity e){if(e instanceof LbcHedgehogSpikes.SpikeArrow a)retire(a);if(e instanceof LivingEntity l){pending.removeIf(p->p.target==l);water.remove(l);}}
    public void forget(ServerPlayer p){for(var a:List.copyOf(arrows))if(a.getOwner()==p)retire(a);pending.removeIf(e->e.target==p);water.remove(p);}
    public void stop(){for(var a:List.copyOf(arrows))retire(a);pending.clear();water.clear();}
    public int trackedCount(){return arrows.size();}
    public int pendingCount(){return pending.size();}
    public List<Arrow> projectiles(){return List.copyOf(arrows);}
}
