package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;
import java.util.*;

/** Native hard-acid emitters and the source delayed-death counter, without marker scans. */
final class LbcAcidCorruption {
    static final EntityType<Charge> TYPE=registerType();
    private static final TagKey<Block> AIRS=TagKey.create(Registries.BLOCK,Identifier.parse("minecraft:airs"));
    private static final DustColorTransitionOptions BALL=new DustColorTransitionOptions(0x030003,0x03660d,4);
    private static final DustColorTransitionOptions DROP=new DustColorTransitionOptions(0x030003,0x036603,4);
    private static final List<Holder<MobEffect>> DEBUFFS=List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.HUNGER,
            MobEffects.INSTANT_DAMAGE,MobEffects.MINING_FATIGUE,MobEffects.NAUSEA,MobEffects.POISON,MobEffects.SLOWNESS,
            MobEffects.UNLUCK,MobEffects.WEAKNESS,MobEffects.WITHER);
    private static final int[] DURATIONS={200,400,600,1,600,600,200,400,600,400,200},AMPLIFIERS={0,0,10,0,10,0,3,2,100,1,4};
    static void register(){}
    private static EntityType<Charge> registerType(){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:hard_acid"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                .sized(.1f,.1f).clientTrackingRange(4).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);return type;
    }
    static final class Charge extends Projectile implements PolymerEntity {
        boolean ball;int age;
        LbcIndirectKills.Owner terrainCause;
        Charge(EntityType<? extends Charge> type,Level level){super(type,level);}
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
        @Override public EntityType<?> getPolymerEntityType(PacketContext context){return EntityTypes.MARKER;}
        @Override public boolean shouldBeSaved(){return false;}
        @Override public boolean isPushedByFluid(){return false;}
        @Override public void tick(){
            var game=SkyWars2.game();if(game==null){discard();return;}
            if(game.items().timeStop().frozen(this))return;
            baseTick();game.items().acidCorruption().flight(this);
        }
    }
    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final Map<LivingEntity,LbcIndirectKills.Owner> deaths=new WeakHashMap<>();
    LbcAcidCorruption(GameController game){this.game=game;}
    List<Charge> snapshot(){return List.copyOf(charges);}
    boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy){
        if(!game.items().mana().visible(owner)||game.items().actionsBlocked(owner)||game.items().wands().inputTimeStopped(owner)
                ||game.items().magic().suppressed(owner)||game.items().magic().suppressedAt(owner.level(),owner.getEyePosition()))return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();int mode=Mth.clamp(data.getIntOr("mode",1),1,4);
        if(academy&&owner.isShiftKeyDown()){
            mode=mode%4+1;data.putInt("mode",mode);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));owner.getInventory().setChanged();
            owner.sendSystemMessage(Component.translatable(switch(mode){case 2->"mode_reverse";case 3->"mode_titul";case 4->"mode_chuma";default->"mode_default";}));return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly("acidcorrupt_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost))return false;
        if(!academy&&owner.isShiftKeyDown()&&LbcAcademy.trial(stack))meditate(owner,hand,stack);
        else if(academy&&mode==2)sacrifice(owner);
        else cast(owner,academy?mode:1);
        LbcOverload.sound(owner,owner.position(),"minecraft:entity.zombie_villager.cure",1,2,0);
        game.items().wands().wandCasted(owner);return true;
    }
    private void meditate(ServerPlayer owner,InteractionHand hand,ItemStack stack){
        owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,600,4,false,false),owner);
        for(var effect:List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.HUNGER,MobEffects.INSTANT_DAMAGE,
                MobEffects.NAUSEA,MobEffects.POISON,MobEffects.SLOWNESS,MobEffects.WEAKNESS,MobEffects.WITHER))
            owner.addEffect(ItemEffectLevels.create(effect,-1,effect==MobEffects.INSTANT_DAMAGE?99:0),owner);
        var at=owner.position();owner.level().sendParticles(new DustParticleOptions(0x0d332d,.365f),true,false,at.x,at.y+10,at.z,15,.3,10,.3,1000);
        game.items().academy().ready(owner,hand,stack);
    }
    /** One native first contact replaces independent entity/wall terminals in recursive source functions. */
    void cast(ServerPlayer owner,int mode){
        rayAt(owner,mode,owner.getEyePosition(),owner.getLookAngle());
    }
    /** Reverse intentionally resolves at the owner; other helper rays use the detached pose. */
    void emitAt(ServerPlayer owner,InteractionHand hand,ItemStack stack,int mode,Vec3 from,Vec3 direction){
        if(mode==2){sacrifice(owner);return;}
        rayAt(owner,mode,from,direction.normalize());
    }
    private void rayAt(ServerPlayer owner,int mode,Vec3 from,Vec3 direction){
        double range=mode==1?112.5:mode==3?200:150;var level=owner.level();
        var to=from.add(direction.scale(range));var wall=LbcTerra.wallHit(level,from,to);if(wall!=null)to=wall.getLocation();
        var entity=ProjectileUtil.getEntityHitResult(owner,from,to,new AABB(from,to).inflate(1),e->e instanceof LivingEntity
                &&(mode==1?game.items().wands().projectileTarget(owner,e):wild(e)),from.distanceToSqr(to));
        HitResult hit=entity!=null?entity:wall;Vec3 end=hit==null?to:hit.getLocation();
        double spacing=mode==1?.75:.5;int samples=(int)Math.floor(from.distanceTo(end)/spacing);
        for(int i=0;i<=samples;i++){
            var at=from.add(direction.scale(i*spacing));if(!level.hasChunkAt(BlockPos.containing(at))||mode!=1&&game.items().magic().suppressedAt(level,at))return;
            if(mode==1)level.sendParticles(ParticleTypes.SNEEZE,true,false,at.x,at.y,at.z,1,.1,.1,.1,0);
            else level.sendParticles(new DustParticleOptions(mode==3?0:0x1ce9f8,.8f),true,false,at.x,at.y,at.z,1,.1,.1,.1,0);
        }
        if(hit==null||!level.hasChunkAt(BlockPos.containing(end))||mode!=1&&game.items().magic().suppressedAt(level,end))return;
        if(mode==1)spawn(owner,end,true);
        else if(mode==3){if(entity!=null&&entity.getEntity() instanceof LivingEntity target)applyDeath(owner,target,120);}
        else for(var target:level.getEntitiesOfClass(ServerPlayer.class,new AABB(end,end).inflate(10),p->wild(p)&&p.position().distanceToSqr(end)<=100))
            game.items().mana().applyReverse(owner,target,30);
    }
    private boolean wild(Entity e){return game.items().wands().validTarget(e)&&!game.items().magic().suppressedAt((ServerLevel)e.level(),e.position());}
    Charge spawn(ServerPlayer owner,Vec3 at,boolean ball){
        var c=new Charge(TYPE,owner.level());c.setOwner(owner);c.ball=ball;c.setPos(at);c.setDeltaMovement(ball?Vec3.ZERO:new Vec3(0,-.5,0));
        c.entityTags().addAll(List.of(ball?"hard_acid_ball":"hard_acid","slowraycast",game.items().epoch()));
        game.items().timeStop().prepareFrozen(c);if(!owner.level().addFreshEntity(c))c.discard();return c;
    }
    void sacrifice(ServerPlayer owner){
        double health=0;var victims=new ArrayList<LivingEntity>();
        owner.level().getAllEntities().forEach(e->{if(e instanceof LivingEntity l&&!(e instanceof ServerPlayer)&&game.items().wands().validTarget(e)&&(owner.isAlliedTo(e)||LbcWtf.managed(e)&&game.items().summons().friendly(owner,e)))victims.add(l);});
        for(var victim:victims){
            if(victim instanceof LbcWtf.Chef chef){
                // (user) Sacrifice only knocks the Chef; count the remaining accepted damage to that knock once.
                if(game.items().wtf().stasis(chef)>0)continue;
                float remaining=Math.max(0,20-game.items().wtf().damageCount(chef));
                if(game.items().indirectKills().inheritedKill(chef,game.items().indirectKills().wildCause(owner))&&game.items().wtf().stasis(chef)>0)health+=remaining;
            }else{health+=Mth.floor(victim.getHealth());game.items().indirectKills().inheritedKill(victim,game.items().indirectKills().wildCause(owner));}
        }
        int duration=(int)Math.min(Integer.MAX_VALUE,Math.floor(health/10)*20);
        if(duration>0)owner.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,duration,4),owner);
    }
    void flight(Charge c){
        if(c.isRemoved())return;var level=(ServerLevel)c.level();
        if(!(c.getOwner() instanceof ServerPlayer owner)||!owner.isAlive()||owner.isRemoved()||!game.canUseGameplayItems(owner)
                ||!game.items().currentEpoch(c)||!level.hasChunkAt(c.blockPosition())){c.discard();return;}
        if(game.items().timeStop().stopped(level)||game.items().timeStop().frozen(c))return;
        if(c.age++>=(c.ball?400:300)){c.discard();return;}
        var at=c.position();level.sendParticles(c.ball?BALL:DROP,at.x,at.y+(c.ball?0:.3),at.z,c.ball?10:1,c.ball?3:0,c.ball?3:0,c.ball?3:0,0);
        if(c.ball){
            if(level.getRandom().nextFloat()<.2f)spawn(owner,new Vec3(Mth.floor(at.x)+level.getRandom().nextInt(9)-4,
                    Mth.floor(at.y)+level.getRandom().nextInt(9)-4,Mth.floor(at.z)+level.getRandom().nextInt(9)-4),false);
            for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(6),e->e.position().distanceToSqr(at)<=36&&target(owner,e)))
                if(level.getRandom().nextFloat()<.35f)debuff(owner,target,level.getRandom().nextInt(DEBUFFS.size()));
        }else{
            Vec3 end=at.add(c.getDeltaMovement());var wall=LbcTerra.wallHit(level,at,end);if(wall!=null)end=wall.getLocation();
            var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.CACTUS),c,owner);
            LbcDamage.hurtSegment(level,damage,at,end,Vec3.ZERO,.5,2,Integer.MAX_VALUE,e->e instanceof LivingEntity&&target(owner,e),false,e->{
                if(level.getRandom().nextFloat()<.2f)debuff(owner,(LivingEntity)e,level.getRandom().nextInt(DEBUFFS.size()));
            },null);
            c.setPos(end);
            if(wall!=null){
                if(level.hasChunkAt(BlockPos.containing(end))){
                    if(c.terrainCause==null||!c.terrainCause.player().equals(owner.getUUID()))c.terrainCause=game.items().indirectKills().coldCause(owner,true);
                    var point=end;game.items().indirectKills().trapChanges(level,c.terrainCause,()->{
                        for(var p:BlockPos.betweenClosed(BlockPos.containing(point.add(-.3,-.3,-.3)),BlockPos.containing(point.add(.3,.3,.3))))
                            if(level.hasChunkAt(p)&&!level.getBlockState(p).is(AIRS))erase(level,p);
                    });
                }c.discard();
            }
        }
        if(c.age>=(c.ball?400:300))c.discard();
    }
    private static void erase(ServerLevel level,BlockPos p){
        if(level.getBlockEntity(p) instanceof net.minecraft.world.Container container)container.clearContent();
        level.setBlock(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE
                |Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE);
    }
    private boolean target(ServerPlayer owner,Entity e){return game.items().wands().projectileTarget(owner,e);}
    void debuff(ServerPlayer owner,LivingEntity target,int index){target.addEffect(ItemEffectLevels.create(DEBUFFS.get(index),DURATIONS[index],AMPLIFIERS[index]),owner);}
    void adoptRawFoodStatuses(ServerPlayer player){
        if(deathRemaining(player)>0&&!deaths.containsKey(player)&&!game.items().magic().rejectCustomDebuff(player))
            deaths.put(player,game.items().indirectKills().wildCause(player));
    }
    int deathRemaining(LivingEntity target){return Math.max(0,game.items().mana().hudValue(target,"delayed_death"));}
    void applyDeath(ServerPlayer owner,LivingEntity target,int seconds){
        if(!recordDeath(target,seconds,game.items().indirectKills().wildCause(owner),false))return;
        target.addEffect(ItemEffectLevels.create(MobEffects.NAUSEA,20,0),owner);
        if(target instanceof ServerPlayer p)for(int i=0;i<4;i++)p.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
                net.minecraft.sounds.SoundEvents.AMBIENT_CAVE,net.minecraft.sounds.SoundSource.MASTER,
                p.getX(),p.getY(),p.getZ(),1,1,p.getRandom().nextLong()));
        var at=target.position();((ServerLevel)target.level()).sendParticles(new DustParticleOptions(0x2a4957,1),true,false,at.x,at.y+4,at.z,100,.3,4,.3,0);
    }
    /** Source poison arrow assigns its scaled marker duration to the existing seconds counter. */
    void arrowDeath(LivingEntity target,int seconds,LbcIndirectKills.Owner cause){recordDeath(target,seconds,cause,true);}
    private boolean recordDeath(LivingEntity target,int seconds,LbcIndirectKills.Owner cause,boolean replace){
        if(seconds<=0||!wild(target)||target.entityTags().contains("sin_greed_active")||target instanceof ServerPlayer p&&game.items().magic().rejectCustomDebuff(p))return false;
        game.items().mana().score(target,"delayed_death").set(replace?seconds:(int)Math.min(Integer.MAX_VALUE,(long)deathRemaining(target)+seconds));
        deaths.put(target,cause);return true;
    }
    void tick(){
        for(var entry:List.copyOf(deaths.entrySet())){
            var target=entry.getKey();if(target==null||game.items().polymorph().masked(target))continue;if(!target.isAlive()||target.isRemoved()||deathRemaining(target)==0){clear(target);continue;}
            if(target instanceof ServerPlayer p&&game.items().magic().rejectCustomDebuff(p)){clear(target);continue;}
            if(game.server().getTickCount()%20!=0||game.items().timeStop().stopped((ServerLevel)target.level())||target.entityTags().contains("sin_greed_active"))continue;
            int remaining=deathRemaining(target);if(remaining==1&&game.items().mana().hudValue(target,"void_resistance")<=0)game.items().indirectKills().inheritedExecution(target,entry.getValue());
            game.items().mana().score(target,"delayed_death").set(remaining-1);
            if(remaining==1)deaths.remove(target);
            else if(target instanceof ServerPlayer p&&(remaining-1)%10==0)p.sendSystemMessage(Component.translatable("delayed_death").append(Integer.toString(remaining-1)).append(Component.translatable("delayed_death_2")).withStyle(net.minecraft.ChatFormatting.DARK_RED));
        }
    }
    void clear(LivingEntity target){deaths.remove(target);game.items().mana().score(target,"delayed_death").set(0);}
    void loaded(Entity e){if(e instanceof Charge c){c.entityTags().remove("magic");c.entityTags().remove("mecha");if(game.playing()&&game.items().currentEpoch(c))charges.add(c);else c.discard();}}
    void unloaded(Entity e){if(e instanceof Charge c)charges.remove(c);}
    void forget(ServerPlayer owner){clear(owner);for(var c:List.copyOf(charges))if(c.getOwner()==owner)c.discard();}
    void stop(){for(var target:List.copyOf(deaths.keySet()))clear(target);for(var c:List.copyOf(charges))c.discard();charges.clear();}

    LbcCloneSkills.ActorImage cloneStatusImage(Entity actor) {
        if(!(actor instanceof LivingEntity living)||!deaths.containsKey(living))return null;
        var cause=deaths.get(living);int remaining=deathRemaining(living);
        return new LbcCloneSkills.ActorImage() {
            public void install(Entity original,Entity clone,Map<Entity,Entity> family) {
                var target=(LivingEntity)clone;deaths.put(target,cause);
                game.items().mana().score(target,"delayed_death").set(remaining);
            }
            public void rollback(Entity clone){deaths.remove(clone);}
        };
    }
}
