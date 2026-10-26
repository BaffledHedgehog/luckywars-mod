package me.skywars2.game;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreHolder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Vampire wand casts and its finite native charge share mana, targeting and lifecycle services. */
public final class LbcLifestealer {
    public static final EntityType<Charge> TYPE=registerType();
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private final Set<Charge> charges=new LinkedHashSet<>();
    private final ArrayList<Charge> batch=new ArrayList<>();

    public static final class Charge extends Projectile implements PolymerEntity {
        int age,lifetime=40;
        Charge(EntityType<? extends Charge> type,Level level) { super(type,level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
        @Override public EntityType<?> getPolymerEntityType(PacketContext context) { return EntityTypes.MARKER; }
        @Override public boolean shouldBeSaved() { return false; }
        @Override public boolean isPushedByFluid() { return false; }
    }
    private static EntityType<Charge> registerType() {
        var key=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("skywars2:lifestealer_charge"));
        var type=Registry.register(BuiltInRegistries.ENTITY_TYPE,key,
                EntityType.Builder.<Charge>of(Charge::new,MobCategory.MISC)
                        .sized(.1f,.1f).clientTrackingRange(8).updateInterval(1).build(key));
        PolymerEntityUtils.registerType(type); return type;
    }
    public static void register() { }
    LbcLifestealer(GameController game) { this.game=game; }
    public int count() { return charges.size(); }
    List<Charge> snapshot() { return List.copyOf(charges); }

    public boolean use(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy) {
        if(!valid(owner) || paused(owner) || game.items().magic().suppressed(owner)
                || game.items().magic().suppressedAt(owner.level(),owner.getEyePosition())) return false;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(academy && owner.isShiftKeyDown()) {
            int mode=data.getIntOr("mode",1)==2?1:2;
            data.putInt("mode",mode); stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
            owner.getInventory().setChanged();
            owner.sendSystemMessage(Component.translatable(mode==1?"mode_caster":"mode_charge"));
            return true;
        }
        int cost=game.items().mana().score(ScoreHolder.forNameOnly("vampire_wand_mana_use"),"lbc.math").get();
        if(!game.items().mana().trySpendWand(owner,cost)) return false;
        if(academy && data.getIntOr("mode",1)==2) spawn(owner);
        else cast(owner,hand,stack,academy);
        game.items().wands().wandCasted(owner);
        return true;
    }

    void spawn(ServerPlayer owner) {
        Vec3 origin=owner.getEyePosition().add(owner.getLookAngle());
        LbcOverload.sound(owner,origin,"minecraft:entity.cat.hiss",1,2,0);
        if(!owner.level().hasChunkAt(BlockPos.containing(origin))) return;
        var charge=new Charge(TYPE,owner.level());
        charge.setOwner(owner); charge.setPos(origin); charge.setNoGravity(true);
        charge.setYRot(owner.getYRot()); charge.setXRot(owner.getXRot());
        charge.setDeltaMovement(owner.getLookAngle().normalize().scale(4.5));
        charge.entityTags().add("magic"); charge.entityTags().add("life_steal");
        game.items().markCurrentItemEpoch(charge);
        if(owner.level().addFreshEntity(charge)) {
            charges.add(charge);
            // Исходная точка в одном блоке от глаз не позволяет выстрелить сквозь близкую стену.
            move(charge,owner,owner.getEyePosition(),origin,false);
        }
    }

    private int strength(ServerPlayer owner) {
        return owner.entityTags().contains("have_stand") && LbcStand.hasClocks(owner)
                ? game.items().stand().overheaven(owner)>0?2:1 : 0;
    }
    private void heal(ServerPlayer owner,int count,int strength) {
        if(count<=0) return;
        int tier=Math.min(3,(count-1)/3);
        if(strength==0 && tier<3) {
            owner.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,20,tier+2),owner);
            return;
        }
        int amplifier=strength==0?4:strength==1?switch(tier) { case 0->0; case 1->1; case 2->5; default->20; }
                :switch(tier) { case 0->1; case 1->2; case 2->10; default->40; };
        // /effect give ... instant_health 1 задаёт один тик для мгновенного эффекта.
        owner.addEffect(ItemEffectLevels.create(MobEffects.INSTANT_HEALTH,1,amplifier),owner);
    }
    private boolean target(ServerPlayer owner,Entity candidate) {
        return game.items().lifetimes().target(owner,candidate)
                && !candidate.entityTags().contains("nomagic_active")
                && !game.items().magic().suppressedAt(owner.level(),candidate.position());
    }

    void cast(ServerPlayer owner,InteractionHand hand,ItemStack stack,boolean academy) {
        Vec3 center=owner.position();
        int strength=strength(owner),accepted=0;
        var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(academy?DamageTypes.GENERIC:DamageTypes.CACTUS),owner);
        float damage=strength==2?10:strength==1?5:1;
        boolean friendlyMob=false;
        var team=owner.getTeam();
        boolean advancementTeam=team!=null && validAdvancementTeam(team.getName());
        for(var entity:owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(20),
                candidate->candidate.getType().builtInRegistryHolder().is(MOBS)
                        && candidate.position().distanceToSqr(center)<=400)) {
            double distance=entity.position().distanceToSqr(center);
            if(!academy && advancementTeam && !(entity instanceof ServerPlayer) && entity.getTeam()==team
                    && distance>=.01 && game.items().wands().target(owner,entity,true)) friendlyMob=true;
            if(distance<.0001 || !target(owner,entity)) continue;
            if(entity instanceof ServerPlayer player) player.sendSystemMessage(Component.translatable("player_vampirism"));
            // Лечение и задание считают только принятый сервером удар, включая общую очередь Time Stop.
            if(entity.hurtServer(owner.level(),source,damage)) accepted++;
        }
        heal(owner,accepted,strength);
        if(!academy) {
            if(friendlyMob) game.items().mana().award(owner,"lbc:true_advancements/staffs/vampire");
            game.items().academy().lifestealerCast(owner,hand,stack,accepted);
        }
        LbcOverload.sound(owner,"minecraft:entity.cat.hiss",1.5f,2,0);
        owner.level().sendParticles(ParticleTypes.DAMAGE_INDICATOR,true,false,center.x,center.y+1,center.z,200,12,12,12,1);
    }
    private void impact(Charge charge,ServerPlayer owner,Vec3 point) {
        // Первое столкновение завершает заряд даже рядом с владельцем; общие guards защищают его и союзников.
        remove(charge);
        if(!valid(owner) || !game.items().currentEpoch(charge)) return;
        int strength=strength(owner),accepted=0;
        float damage=strength==2?10:strength==1?5:1;
        var source=new DamageSource(owner.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.GENERIC),charge,owner);
        for(var entity:owner.level().getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(7),
                candidate->candidate.position().distanceToSqr(point)<=49 && target(owner,candidate))) {
            if(entity.hurtServer(owner.level(),source,damage)) accepted++;
        }
        heal(owner,accepted,strength);
    }
    private static boolean validAdvancementTeam(String name) {
        try { int team=Integer.parseInt(name); return team>=1 && team<=32; }
        catch(NumberFormatException ignored) { return false; }
    }

    public void tick() {
        batch.addAll(charges);
        try {
            for(var charge:batch) {
                if(!charges.contains(charge)) continue;
                // Read the native owner every tick: Electro reflection deliberately replaces it.
                if(charge.isRemoved() || !(charge.getOwner() instanceof ServerPlayer owner) || !valid(owner)
                        || owner.level()!=charge.level() || charge.level()!=game.maps().level()
                        || !game.items().currentEpoch(charge)) { remove(charge); continue; }
                if(paused(owner)) continue;
                Vec3 from=charge.position(),to=from.add(charge.getDeltaMovement());
                move(charge,owner,from,to,true);
            }
        } finally { batch.clear(); }
    }
    private void move(Charge charge,ServerPlayer owner,Vec3 from,Vec3 to,boolean activeTick) {
        if(!owner.level().hasChunkAt(BlockPos.containing(from))) { remove(charge); return; }
        HitResult hit=game.items().wands().firstHit(owner,from,to);
        Vec3 end=hit==null?to:hit.getLocation();
        var shield=game.items().wands().shieldHit(owner,from,end);
        boolean shieldBlocked=shield!=null && (hit==null
                || from.distanceToSqr(shield.getLocation())<from.distanceToSqr(end));
        if(shieldBlocked) end=shield.getLocation();
        double suppression=game.items().magic().suppressionFraction(owner.level(),from,end);
        boolean suppressed=Double.isFinite(suppression);
        if(suppressed) end=from.lerp(end,suppression);
        charge.setPos(end);
        if(activeTick) trail(owner,from,end);
        if(suppressed || shieldBlocked) { remove(charge); return; }
        if(hit instanceof BlockHitResult wall && !owner.level().hasChunkAt(wall.getBlockPos())) { remove(charge); return; }
        if(hit!=null) { impact(charge,owner,end); return; }
        if(activeTick && ++charge.age>=charge.lifetime) remove(charge);
    }
    private void trail(ServerPlayer owner,Vec3 from,Vec3 end) {
        double distance=from.distanceTo(end);
        // Particle sampling is independent of the single swept collision query.
        for(int i=0;i<15 && i*.3<=distance+1e-7;i++) {
            Vec3 point=distance<1e-9?from:from.lerp(end,i*.3/distance);
            owner.level().sendParticles(ParticleTypes.DAMAGE_INDICATOR,true,false,point.x,point.y,point.z,1,.1,.1,.1,0);
        }
    }
    private boolean valid(ServerPlayer owner) {
        return owner!=null && !owner.isRemoved() && owner.isAlive() && owner.level()==game.maps().level()
                && game.items().mana().visible(owner) && !game.isObserver(owner) && game.stage()!=GameController.Stage.ENDING;
    }
    private boolean paused(ServerPlayer owner) {
        return game.items().wands().timeStopped() || owner.entityTags().contains("time_stopped")
                || game.items().frost().isFrozen(owner);
    }
    void loaded(Entity entity) {
        if(entity instanceof Charge charge) {
            if(!game.items().currentEpoch(charge) || !(charge.getOwner() instanceof ServerPlayer owner)
                    || !valid(owner) || owner.level()!=charge.level()) { remove(charge); return; }
            charges.add(charge);
        }
    }
    void unloaded(Entity entity) { if(entity instanceof Charge charge) remove(charge); }
    void forget(ServerPlayer owner) { for(var charge:List.copyOf(charges)) if(charge.getOwner()==owner) remove(charge); }
    private void remove(Charge charge) { charges.remove(charge); if(!charge.isRemoved()) charge.discard(); }
    public void stop() { for(var charge:List.copyOf(charges)) remove(charge); charges.clear(); batch.clear(); }
}
