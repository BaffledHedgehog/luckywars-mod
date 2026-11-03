package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.skywars2.mixin.ShulkerBowMixin;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Immutable accepted ammo plus a detached native trait context. The context is never spawned. */
public final class LbcBeamAmmo implements LbcChargedBows.BeamPayload {
    private static final TagKey<EntityType<?>> MOBS=TagKey.create(Registries.ENTITY_TYPE,Identifier.parse("minecraft:mobs"));
    private final GameController game;
    private ServerPlayer owner;
    private final ItemStack weapon;
    private final String epoch;
    private final List<Carrier> carriers;
    private final int variant;
    private final boolean shulker,madoka;
    private Vec3 previous;
    private boolean worldVariants=true;

    private static final class Carrier {
        final ItemStack ammo,weapon;
        final ServerLevel level;
        final int spectral,fire,variant;
        Arrow context;
        final Set<UUID> contacts=new LinkedHashSet<>();
        boolean traitsActive=true;
        Carrier(ItemStack ammo,ItemStack weapon,int spectral,int fire,int variant,Arrow context){
            this.ammo=ammo.copy();this.weapon=weapon.copy();this.spectral=spectral;this.fire=fire;this.variant=variant;this.context=context;this.level=(ServerLevel)context.level();
        }
    }
    /** Only arrows admitted by the exact successful native family are copied. No foreign entities. */
    public static LbcChargedBows.BeamPayload capture(GameController game,ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        var carriers=new ArrayList<Carrier>();
        for(var projectile:family){
            if(!(projectile instanceof AbstractArrow arrow)||arrow.isRemoved()||arrow.getOwner()!=owner||arrow.level()!=owner.level())continue;
            var ammo=arrow.getPickupItemStackOrigin().copy();var nativeWeapon=arrow.getWeaponItem();
            var accepted=nativeWeapon==null?weapon:nativeWeapon;
            var context=new Arrow(owner.level(),owner,ammo.copy(),accepted.copy());
            context.setUUID(arrow.getUUID());context.setPos(arrow.position());context.setDeltaMovement(arrow.getDeltaMovement());
            context.setYRot(arrow.getYRot());context.setXRot(arrow.getXRot());context.setNoGravity(true);
            context.entityTags().addAll(arrow.entityTags());game.items().markCurrentItemEpoch(context);
            context.setRemainingFireTicks(arrow.getRemainingFireTicks());
            context.setBaseDamage(((ShulkerBowMixin.ArrowAccess)arrow).skywars2$baseDamage());
            context.setCritArrow(arrow.isCritArrow());((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)context).skywars2$setPierceLevel(arrow.getPierceLevel());
            int spectral=arrow instanceof SpectralArrow s?((ShulkerBowMixin.SpectralAccess)s).skywars2$duration():0;
            carriers.add(new Carrier(ammo,accepted,spectral,arrow.getRemainingFireTicks(),variant(ammo),context));
        }
        return carriers.isEmpty()?null:new LbcBeamAmmo(game,owner,weapon,carriers);
    }
    public static LbcChargedBows.BeamPayload captureImpact(GameController game,ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        var payload=(LbcBeamAmmo)capture(game,owner,weapon,family);if(payload!=null)payload.worldVariants=false;return payload;
    }
    private LbcBeamAmmo(GameController game,ServerPlayer owner,ItemStack weapon,List<Carrier> carriers){
        this.game=game;this.owner=owner;this.weapon=weapon.copy();this.carriers=List.copyOf(carriers);epoch=game.items().epoch();
        variant=carriers.getFirst().variant;
        boolean magical=!game.items().magic().suppressed(owner);
        shulker=magical&&ItemData.intValue(weapon,"shulker_new",0)==1;
        madoka=magical&&LbcMadokaBow.matches(weapon);
    }
    private static int variant(ItemStack ammo){
        var contents=ammo.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        int color=contents.customColor().orElse(-1);
        if(color==6710877)return 300;if(color==6710881)return 301;if(color==6710882)return 302;
        for(var effect:contents.getAllEffects())if(effect.is(MobEffects.RAID_OMEN))return effect.getAmplifier();
        return 0;
    }
    @Override public int variant(){return variant;}
    @Override public void sample(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause,boolean railgun){
        if(owner==null||worldVariants&&owner.level()!=level||!game.items().currentEpoch(epoch))return;
        var direction=previous==null?carriers.getFirst().context.getDeltaMovement().normalize():point.subtract(previous).normalize();previous=point;
        // One spatial query per sample is shared by every ammo member of this accepted family.
        var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(3),target->
                target!=owner&&target.isAlive()&&!target.isSpectator()&&!(target instanceof Creeper)
                        &&target.position().distanceToSqr(point)<=9&&target.getType().builtInRegistryHolder().is(MOBS)
                        &&game.items().wands().validRoleTarget(target,false)&&game.items().wands().validTarget(target));
        for(var carrier:carriers){
            var context=carrier.context;context.setDeltaMovement(direction);
            context.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));
            context.setXRot((float)-Math.toDegrees(Math.asin(Math.clamp(direction.y,-1,1))));
            // Source custom children share the accepted cause and existing native services.
            if(worldVariants)game.items().quiver().beamSample(owner,level,point,cause,carrier.variant);
            if(railgun&&carrier.variant==111)for(var target:targets)game.items().indirectKills().withCause(target,cause,
                    ()->target.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,200,111),owner));
            if(shulker)game.items().shulkerBow().beamSample(owner,weapon,context,point,cause);
            if(carrier.traitsActive){game.items().trinketArrows().beamSample(context,point);carrier.traitsActive=!context.isRemoved();}
            else context.setPos(point);
            for(var target:targets)if(carrier.contacts.add(target.getUUID()))contact(level,carrier,target,cause);
        }
    }
    private void contact(ServerLevel level,Carrier carrier,LivingEntity target,LbcIndirectKills.Owner cause){
        game.items().indirectKills().withCause(target,cause,()->{
            if(carrier.fire>0){target.igniteForSeconds(5);game.items().indirectKills().projectileIgnited(target,owner,true);}
            if(carrier.spectral>0)target.addEffect(new MobEffectInstance(MobEffects.GLOWING,carrier.spectral),owner);
            if(shulker)target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,200),owner);
            carrier.ammo.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).forEachEffect(effect->{
                if(game.items().quiver().acceptCarrier(target,effect,owner,cause))return;
                if(effect.getEffect().value().isInstantaneous())effect.getEffect().value().applyInstantaneousEffect(level,carrier.context,owner,target,effect.getAmplifier(),1);
                else target.addEffect(effect,owner);
            },carrier.ammo.getOrDefault(DataComponents.POTION_DURATION_SCALE,1f));
            if(madoka)game.items().madokaBow().beamContact(owner,weapon,carrier.context,target);
        });
    }
    /** No Arrow entity is part of the saved graph; only typed native values and owner links are retained. */
    private record Image(Carrier carrier,Set<String> tags,Set<UUID> contacts,boolean traitsActive,
                         Vec3 point,Vec3 velocity,float yaw,float pitch,boolean critical,byte pierce,
                         double damage,int fire,UUID id) { }
    @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        var author=links.key(owner);var savedPrevious=previous;
        var images=carriers.stream().map(c->new Image(c,Set.copyOf(c.context.entityTags()),Set.copyOf(c.contacts),c.traitsActive,
                c.context.position(),c.context.getDeltaMovement(),c.context.getYRot(),c.context.getXRot(),c.context.isCritArrow(),
                c.context.getPierceLevel(),((ShulkerBowMixin.ArrowAccess)c.context).skywars2$baseDamage(),c.context.getRemainingFireTicks(),c.context.getUUID())).toList();
        return rebound->{
            var restored=(ServerPlayer)rebound.get(author);var contexts=new ArrayList<Arrow>(images.size());
            for(var image:images){
                var c=image.carrier;var context=new Arrow(c.level,restored,c.ammo.copy(),c.weapon.copy());
                context.setUUID(image.id);context.entityTags().addAll(image.tags);context.setNoGravity(true);
                context.setPos(image.point);context.setDeltaMovement(image.velocity);context.setYRot(image.yaw);context.setXRot(image.pitch);
                context.setCritArrow(image.critical);((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)context).skywars2$setPierceLevel(image.pierce);context.setBaseDamage(image.damage);context.setRemainingFireTicks(image.fire);
                contexts.add(context);
            }
            return ()->{owner=restored;previous=savedPrevious;for(int i=0;i<images.size();i++){
                var image=images.get(i);var c=image.carrier;c.context=contexts.get(i);c.contacts.clear();c.contacts.addAll(image.contacts);c.traitsActive=image.traitsActive;
            }};
        };
    }
}
