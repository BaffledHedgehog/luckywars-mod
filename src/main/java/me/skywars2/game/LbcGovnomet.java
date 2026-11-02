package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Cannon only: source marker and brown dust, not the distinct bouncing-kit poop model. */
public final class LbcGovnomet {
    public static final EntityType<Charge> TYPE=registerType();
    public static final Item ITEM=registerItem();
    private static final DustParticleOptions DUST=new DustParticleOptions(0x624022,1);
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final Set<FallingBlockEntity> falling=new LinkedHashSet<>();
    private final ArrayList<Charge> batch=new ArrayList<>();

    private static EntityType<Charge> registerType() {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:govnomet_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type);
        return type;
    }
    private static Item registerItem() {
        var key=ResourceKey.create(Registries.ITEM,Identifier.parse("skywars2:govnomet"));
        return Registry.register(BuiltInRegistries.ITEM,key,new SimplePolymerItem(new Item.Properties() {
            @Override public Identifier effectiveModel() { return Identifier.parse("lbc:govnomet"); }
        }.setId(key).stacksTo(1).overrideDescription("shit_cannon"),Items.CARROT_ON_A_STICK,true) {
            @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
                var game=me.skywars2.SkyWars2.game();
                return game!=null && player instanceof ServerPlayer owner && game.items().wands().use(owner,hand)
                        ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
        });
    }
    public static void register() { }
    public static boolean isGovnomet(ItemStack stack) {
        var data=stack.get(DataComponents.CUSTOM_DATA);
        return stack.is(ITEM) || !stack.isEmpty() && data!=null && data.copyTag().getIntOr("govnomet",0)==1;
    }
    public static final class Charge extends Projectile implements PolymerEntity {
        int age;
        boolean mega,retired;
        Charge(EntityType<? extends Charge> type,Level level) { super(type,level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }
    LbcGovnomet(GameController game) { this.game=game; }
    public int count() { return charges.size(); }
    List<Charge> snapshot() { return List.copyOf(charges); }
    int fallingCount() { return falling.size(); }

    /** Exactly one used hand; the common wand dispatcher handles the other hand. No ammo/mana/cooldown. */
    public boolean use(ServerPlayer owner,InteractionHand hand) {
        if(!isGovnomet(owner.getItemInHand(hand)) || hand==InteractionHand.OFF_HAND && !game.config().offhandWands
                || !validOwner(owner) || game.items().actionsBlocked(owner) || game.items().wands().inputTimeStopped(owner)
                || game.items().magic().suppressed(owner)
                || game.items().magic().mechanismSuppressedAt(owner.level(),owner.position())) return false;
        var charge=new Charge(TYPE,owner.level());
        charge.mega=game.config().lbcSkills && game.players().get(owner.getUUID()).lbcSkill==9;
        float yaw=owner.getYRot(),pitch=owner.getXRot();
        if(!charge.mega) { yaw+=owner.level().getRandom().nextInt(41)-20; pitch+=owner.level().getRandom().nextInt(21)-10; }
        charge.setOwner(owner);
        charge.setYRot(yaw); charge.setXRot(pitch);
        charge.setPos(owner.getEyePosition().add(owner.getLookAngle().normalize().scale(.5)));
        charge.setDeltaMovement(Vec3.directionFromRotation(pitch,yaw).scale(2));
        charge.setNoGravity(true);
        // Source carries neither magic nor mecha: suppression gates cast, not flight.
        game.items().markCurrentItemEpoch(charge);
        if(!owner.level().hasChunkAt(charge.blockPosition()) || !owner.level().addFreshEntity(charge)) { charge.discard(); return false; }
        charges.add(charge);
        LbcOverload.sound(owner,charge.position(),"minecraft:block.gravel.place",1,2,0);
        // The source muzzle offset must not tunnel through a wall immediately in front of the eyes.
        if(!move(charge,owner,owner.getEyePosition(),charge.position(),false)) remove(charge);
        return true;
    }
    void loaded(Entity entity) {
        if(entity instanceof Charge charge) {
            if(charge.isRemoved()) return;
            if(charge.retired || charge.level()!=game.maps().level() || !game.items().currentEpoch(charge)
                    || !(charge.getOwner() instanceof ServerPlayer owner) || !validOwner(owner)) charge.discard();
            else charges.add(charge);
        }
    }
    void unloaded(Entity entity) {
        // Removal may already be in progress; discard only on a live chunk-tracking exit.
        if(entity instanceof Charge charge) {
            charge.retired=true; charges.remove(charge);
            if(!charge.isRemoved()) charge.discard();
        }
        if(entity instanceof FallingBlockEntity block) falling.remove(block);
    }
    public void tick() {
        for(var block:List.copyOf(falling)) {
            if(!falling.contains(block)) continue;
            if(block.isRemoved()) { falling.remove(block); continue; }
            if(block.entityTags().contains("skywars2.bouncing_poop.dirt")) {
                if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(block)) {
                    falling.remove(block);block.discard();
                }
                continue;
            }
            var owner=game.items().electro().reflectedOwner(block);
            if(owner==null || !validOwner(owner) || owner.level()!=block.level() || !game.items().currentEpoch(block)) {
                falling.remove(block); block.discard();
            }
        }
        batch.addAll(charges);
        try {
            for(int index=0;index<batch.size();index++) {
                var charge=batch.get(index);
                if(!charges.contains(charge)) continue;
                if(charge.isRemoved() || !(charge.getOwner() instanceof ServerPlayer owner) || !validOwner(owner)
                        || owner.level()!=charge.level() || !game.items().currentEpoch(charge)) { remove(charge); continue; }
                if(game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")) continue;
                if(!step(charge,owner) || ++charge.age>=50) remove(charge);
            }
        } finally { batch.clear(); }
    }
    private boolean step(Charge charge,ServerPlayer owner) {
        return move(charge,owner,charge.position(),charge.position().add(charge.getDeltaMovement()),true);
    }
    private boolean move(Charge charge,ServerPlayer owner,Vec3 from,Vec3 to,boolean trail) {
        Vec3 motion=charge.getDeltaMovement();
        if(!owner.level().hasChunkAt(BlockPos.containing(from)) || motion.lengthSqr()<1e-12) return false;
        var hit=game.items().wands().firstHit(owner,from,to,entity->entity.getType().builtInRegistryHolder().is(MOBS),true);
        Vec3 end=hit==null ? to : hit.getLocation();
        var shield=game.items().wands().shieldHit(owner,from,end);
        if(shield!=null) end=shield.getLocation();
        double distance=from.distanceTo(end);
        for(double d=0;trail && d<=distance+1e-7;d+=.5) {
            Vec3 point=from.add(motion.normalize().scale(Math.min(d,distance)));
            owner.level().sendParticles(DUST,true,false,point.x,point.y,point.z,charge.mega?4:1,0,0,0,0);
        }
        charge.setPos(end);
        if(shield!=null) {
            LbcOverload.sound(owner,shield.getEntity().position(),"minecraft:item.shield.block",1,1,0);
            return false;
        }
        if(hit==null) return true;
        if(hit instanceof BlockHitResult wall && !owner.level().hasChunkAt(wall.getBlockPos())) return false;
        // A single earliest native collision is terminal. Never run independent wall/entity impacts.
        impact(charge,owner,end,hit instanceof EntityHitResult entity ? entity.getEntity() : null,motion.normalize());
        return false;
    }
    private void impact(Charge charge,ServerPlayer owner,Vec3 at,Entity direct,Vec3 direction) {
        if(!active(charge,owner)) return;
        var level=owner.level();
        if(direct!=null && level.getEntities((Entity)null,new AABB(at,at).inflate(.0001),
                entity->entity.entityTags().contains("govno_creeper") && entity.position().distanceToSqr(at)<=16).size()>0)
            game.items().mana().award(owner,"lbc:true_advancements/shooter/shit_cannon");
        if(charge.mega) {
            for(int i=0;i<10;i++) {
                if(!active(charge,owner)) return;
                spawnDirt(owner,at);
            }
            if(!active(charge,owner)) return;
            // Radius-1 native creeper semantics, with explicit player attribution and no spawned carrier.
            var damage=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.PLAYER_EXPLOSION),charge,owner);
            game.items().indirectKills().worldChanges(level,charge,()->
                    level.explode(charge,damage,blastCalculator(charge),at,1,false,Level.ExplosionInteraction.MOB));
        } else {
            // Source replaces the preceding sample, outside the wall, without block loot.
            BlockPos pos=BlockPos.containing(at.subtract(direction.scale(1e-5)));
            if(level.hasChunkAt(pos)) game.items().indirectKills().trapChanges(owner,()->level.setBlock(pos,Blocks.DIRT.defaultBlockState(),
                    Block.UPDATE_CLIENTS|Block.UPDATE_SUPPRESS_DROPS|Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS));
        }
        // Explosion callbacks can synchronously end the match and clear this service.
        if(!active(charge,owner)) return;
        LbcOverload.sound(owner,at,"minecraft:block.gravel.place",1,0,0);
        // Native hitbox contact, never the legacy distance<=15 selector interpreted as splash R15.
        var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(.0001),
                target->target.getType().builtInRegistryHolder().is(MOBS) && game.items().wands().projectileTarget(owner,target));
        if(direct instanceof LivingEntity living && !targets.contains(living) && game.items().wands().projectileTarget(owner,living)) targets.add(living);
        for(var target:targets) {
            if(!active(charge,owner)) break;
            if(target instanceof ServerPlayer && !(target instanceof ServerPlayer player && game.items().magic().rejectEffect(player,MobEffects.NAUSEA)))
                target.addEffect(ItemEffectLevels.create(MobEffects.NAUSEA,300,0),charge);
            applyWither(target,charge);
        }
    }
    void spawnDirt(ServerPlayer owner,Vec3 at) {
        spawnDirt(owner,at,false);
    }
    void spawnDirt(ServerPlayer owner,Vec3 at,boolean retained) {
        spawnDirt(owner.level(),at,owner,retained,null);
    }
    void spawnDirt(ServerLevel level,Vec3 at,LbcSpellSource.Source source) {
        try(var ignored=game.items().spellSources().scope(source,level)) {
            spawnDirt(level,at,game.server().getPlayerList().getPlayer(source.author()),true,source);
        }
    }
    private void spawnDirt(ServerLevel level,Vec3 at,ServerPlayer owner,boolean retained,LbcSpellSource.Source source) {
        var block=new OwnedDirt(level);
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
        output.store("BlockState",net.minecraft.world.level.block.state.BlockState.CODEC,Blocks.DIRT.defaultBlockState());
        output.putInt("Time",1); output.putBoolean("DropItem",true); output.putBoolean("HurtEntities",true);
        output.putFloat("FallHurtAmount",2); output.putInt("FallHurtMax",1000);
        block.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));
        var random=level.getRandom();
        block.setPos(at.add((random.nextInt(101)-50)*.01,(random.nextInt(101)-50)*.01,(random.nextInt(101)-50)*.01));
        block.setDeltaMovement(Vec3.ZERO);
        if(owner!=null)game.items().electro().assignOwner(block,owner);
        if(source!=null){game.items().spellSources().bind(block,source);game.items().markCurrentItemEpoch(block);}
        block.entityTags().add("skywars2.govnomet.dirt");
        if(retained)block.entityTags().add("skywars2.bouncing_poop.dirt");
        if(level.hasChunkAt(block.blockPosition()) && level.addFreshEntity(block)) falling.add(block);
        else block.discard();
    }

    /** Source rubble has native gravity/landing/drops, but must never survive a server restart. */
    private final class OwnedDirt extends FallingBlockEntity {
        OwnedDirt(ServerLevel level) { super(EntityTypes.FALLING_BLOCK,level); }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public void tick() {
            if(entityTags().contains("skywars2.bouncing_poop.dirt")){
                if(!game.playing()||game.stage()==GameController.Stage.ENDING||!game.items().currentEpoch(this)
                        ||!LbcDimensionItems.ordinary(level())){discard();return;}
                var source=game.items().spellSources().source(this);
                if(source!=null)game.items().indirectKills().capturedWorldChanges((ServerLevel)level(),source.cause(),()->super.tick());
                else game.items().indirectKills().worldChanges((ServerLevel)level(),this,()->super.tick());return;
            }
            var owner=game.items().electro().reflectedOwner(this);
            if(owner==null || !validOwner(owner) || owner.level()!=level() || !game.items().currentEpoch(this)) {
                discard(); return;
            }
            // Native landing placement and resulting suffocation keep the current (possibly reflected) author.
            game.items().indirectKills().trapChanges(owner,()->super.tick());
        }
    }

    /** (user) Only explosion HP damage is reduced; terrain and native knockback retain their source behavior. */
    static net.minecraft.world.level.ExplosionDamageCalculator blastCalculator(Entity source) {
        return new net.minecraft.world.level.EntityBasedExplosionDamageCalculator(source) {
            @Override public float getEntityDamageAmount(net.minecraft.world.level.Explosion explosion,Entity entity,float exposure) {
                return super.getEntityDamageAmount(explosion,entity,exposure)*.2f;
            }
        };
    }
    private void applyWither(LivingEntity target,Charge charge) {
        target.addEffect(ItemEffectLevels.create(MobEffects.WITHER,charge.mega?60:20,10),charge);
    }

    private boolean validOwner(ServerPlayer owner) {
        return owner!=null && !owner.isRemoved() && owner.isAlive() && owner.level()==game.maps().level()
                && game.canUseGameplayItems(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean active(Charge charge,ServerPlayer owner) {
        return charges.contains(charge) && !charge.isRemoved() && validOwner(owner) && game.items().currentEpoch(charge);
    }
    private void remove(Charge charge) { charges.remove(charge); charge.discard(); }
    public void forget(ServerPlayer owner) {
        for(var charge:List.copyOf(charges)) if(charge.getOwner()==owner) remove(charge);
        for(var block:List.copyOf(falling)) if(!block.entityTags().contains("skywars2.bouncing_poop.dirt")&&game.items().electro().reflectedOwner(block)==owner) { falling.remove(block); block.discard(); }
    }
    public void stop() {
        for(var charge:List.copyOf(charges)) remove(charge);
        for(var block:List.copyOf(falling)) block.discard();
        falling.clear(); batch.clear();
    }

    static boolean cloneRecognizes(Entity e){return e instanceof Charge||e instanceof LbcGovnomet.OwnedDirt;}
    LbcCloneSkills.ActorImage cloneImage(Entity source) {
        if(source instanceof OwnedDirt)return CloneComplexProjectileStates.image(OwnedDirt::new,
                e->falling.add((FallingBlockEntity)e),e->falling.remove(e));
        if(!(source instanceof Charge c)||c.retired)return null;
        int age=c.age;boolean mega=c.mega;
        return CloneComplexProjectileStates.image(e->{var n=(Charge)e;n.age=age;n.mega=mega;n.retired=false;
            charges.add(n);},e->charges.remove(e));
    }

}
