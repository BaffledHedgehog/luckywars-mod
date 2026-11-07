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
    private final Set<Integer> variants;
    private final boolean shulker,madoka;
    private Vec3 previous;
    private boolean worldVariants=true;
    boolean precise;
    private int lastNativeTick=Integer.MIN_VALUE;

    private static final class Carrier {
        final ItemStack ammo,weapon;
        final ServerLevel level;
        final int spectral,fire,variant;
        Arrow context;
        final Set<UUID> contacts=new LinkedHashSet<>();
        boolean traitsActive=true,terminal;
        int lastReactiveTick=Integer.MIN_VALUE;
        LbcTmtAmmoPayloads.State payloads;
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
            context.setUUID(arrow.getUUID());context.setPos(arrow.position());context.setDeltaMovement(game.items().timeStop().persistedVelocity(arrow,arrow.getDeltaMovement()));
            context.setYRot(arrow.getYRot());context.setXRot(arrow.getXRot());context.setNoGravity(true);
            context.entityTags().addAll(arrow.entityTags());game.items().markCurrentItemEpoch(context);
            context.setRemainingFireTicks(arrow.getRemainingFireTicks());
            context.setBaseDamage(((ShulkerBowMixin.ArrowAccess)arrow).skywars2$baseDamage());
            context.setCritArrow(arrow.isCritArrow());((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)context).skywars2$setPierceLevel(arrow.getPierceLevel());
            int spectral=arrow instanceof SpectralArrow s?((ShulkerBowMixin.SpectralAccess)s).skywars2$duration():0;
            var carrier=new Carrier(ammo,accepted,spectral,arrow.getRemainingFireTicks(),variant(ammo),context);
            var extra=LbcTmtAmmoPayloads.state(arrow);carrier.payloads=extra==null?null:extra.copy();
            carriers.add(carrier);
        }
        return carriers.isEmpty()?null:new LbcBeamAmmo(game,owner,weapon,carriers);
    }
    public static LbcChargedBows.BeamPayload captureImpact(GameController game,ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        var payload=(LbcBeamAmmo)capture(game,owner,weapon,family);if(payload!=null)payload.worldVariants=false;return payload;
    }
    private LbcBeamAmmo(GameController game,ServerPlayer owner,ItemStack weapon,List<Carrier> carriers){
        this.game=game;this.owner=owner;this.weapon=weapon.copy();this.carriers=List.copyOf(carriers);epoch=game.items().epoch();
        variant=carriers.getFirst().variant;
        var values=new LinkedHashSet<Integer>();values.add(variant);
        for(var carrier:carriers)if(carrier.payloads!=null)for(var entry:carrier.payloads.entries)values.add(entry.payload.variant);
        variants=Set.copyOf(values);
        boolean magical=!game.items().magic().suppressed(owner);
        shulker=magical&&ItemData.intValue(weapon,"shulker_new",0)==1;
        madoka=magical&&LbcMadokaBow.matches(weapon);
    }
    LbcBeamAmmo copy(){
        var copied=new ArrayList<Carrier>();
        for(var carrier:carriers){
            var old=carrier.context;var context=new Arrow(carrier.level,owner,carrier.ammo.copy(),carrier.weapon.copy());
            context.setUUID(old.getUUID());context.entityTags().addAll(old.entityTags());context.setPos(old.position());
            context.setDeltaMovement(old.getDeltaMovement());context.setYRot(old.getYRot());context.setXRot(old.getXRot());
            context.setBaseDamage(((ShulkerBowMixin.ArrowAccess)old).skywars2$baseDamage());context.setCritArrow(old.isCritArrow());
            ((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)context).skywars2$setPierceLevel(old.getPierceLevel());
            context.setRemainingFireTicks(old.getRemainingFireTicks());context.setNoGravity(true);
            var c=new Carrier(carrier.ammo,carrier.weapon,carrier.spectral,carrier.fire,carrier.variant,context);
            c.payloads=carrier.payloads==null?null:carrier.payloads.copy();
            c.contacts.addAll(carrier.contacts);c.traitsActive=carrier.traitsActive;c.terminal=carrier.terminal;c.lastReactiveTick=carrier.lastReactiveTick;copied.add(c);
        }
        var result=new LbcBeamAmmo(game,owner,weapon,copied);result.previous=previous;result.worldVariants=worldVariants;result.precise=precise;result.lastNativeTick=lastNativeTick;return result;
    }
    /** One real actor tick; native contact alone selects the damage recipient. */
    boolean nativeTarget(Entity target){return game.items().wands().projectileTarget(owner,target);}
    void markTraits(int mask){for(var c:carriers)game.items().trinketArrows().mark(c.context,mask);}
    void nativeFlight(Entity actor,LbcIndirectKills.Owner cause){
        if(lastNativeTick==game.server().getTickCount()||actor.level()!=owner.level()||!game.items().currentEpoch(epoch))return;
        lastNativeTick=game.server().getTickCount();sampleTargets((ServerLevel)actor.level(),actor.position(),cause,false,List.of());
    }
    void nativeContact(Entity actor,LivingEntity target,Vec3 point,LbcIndirectKills.Owner cause){
        if(!game.items().currentEpoch(epoch)||!game.items().wands().projectileTarget(owner,target))return;
        nativeFlight(actor,cause);
        for(var carrier:carriers)if(carrier.contacts.add(target.getUUID())){carrier.context.setPos(point);contact((ServerLevel)actor.level(),carrier,target,cause);}
    }
    void nativeGround(Entity actor,Vec3 point,LbcIndirectKills.Owner cause){
        nativeFlight(actor,cause);blockContact((ServerLevel)actor.level(),point,cause);
        var hit=new net.minecraft.world.phys.BlockHitResult(point,net.minecraft.core.Direction.getApproximateNearest(actor.getDeltaMovement().reverse()),net.minecraft.core.BlockPos.containing(point),false);
        for(var carrier:carriers)if(carrier.traitsActive){game.items().trinketArrows().hit(carrier.context,hit);carrier.traitsActive=!carrier.context.isRemoved();}
    }
    /** Native attachment codec image; detached contexts are never published or globally indexed. */
    net.minecraft.nbt.CompoundTag save(){
        var tag=new net.minecraft.nbt.CompoundTag();var ops=owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        tag.putString("owner",owner.getUUID().toString());tag.putString("epoch",epoch);tag.put("weapon",ItemStack.CODEC.encodeStart(ops,weapon).getOrThrow());
        tag.putBoolean("world",worldVariants);tag.putBoolean("precise",precise);tag.putInt("frame",game.server().getTickCount());tag.putBoolean("native_this_tick",lastNativeTick==game.server().getTickCount());
        if(previous!=null)tag.put("previous",Vec3.CODEC.encodeStart(ops,previous).getOrThrow());
        var list=new net.minecraft.nbt.ListTag();for(var c:carriers){var row=new net.minecraft.nbt.CompoundTag();
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,owner.registryAccess());c.context.saveWithoutId(output);row.put("context",output.buildResult());
            row.put("ammo",ItemStack.CODEC.encodeStart(ops,c.ammo).getOrThrow());row.put("weapon",ItemStack.CODEC.encodeStart(ops,c.weapon).getOrThrow());
            row.putInt("spectral",c.spectral);row.putInt("fire",c.fire);row.putInt("variant",c.variant);row.putBoolean("traits",c.traitsActive);row.putBoolean("terminal",c.terminal);row.putBoolean("reactive_this_tick",c.lastReactiveTick==game.server().getTickCount());
            var contacts=new net.minecraft.nbt.ListTag();for(var id:c.contacts)contacts.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));row.put("contacts",contacts);
            if(c.payloads!=null)row.put("payloads",LbcTmtAmmoPayloads.CODEC.encodeStart(ops,c.payloads).getOrThrow());list.add(row);
        }tag.put("carriers",list);return tag;
    }
    static LbcBeamAmmo load(GameController game,ServerLevel level,net.minecraft.nbt.CompoundTag tag){
        var ownerId=UUID.fromString(tag.getStringOr("owner",""));var owner=game.server().getPlayerList().getPlayer(ownerId);
        if(owner==null)owner=MatchOfflinePlayers.find(ownerId);
        if(owner==null||!game.items().currentEpoch(tag.getStringOr("epoch","")))return null;
        var ops=level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);var carriers=new ArrayList<Carrier>();
        for(var value:tag.getListOrEmpty("carriers")){if(!(value instanceof net.minecraft.nbt.CompoundTag row))continue;
            var ammo=ItemStack.CODEC.parse(ops,row.getCompoundOrEmpty("ammo")).getOrThrow();var weapon=ItemStack.CODEC.parse(ops,row.getCompoundOrEmpty("weapon")).getOrThrow();
            var context=new Arrow(level,owner,ammo.copy(),weapon.copy());context.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),row.getCompoundOrEmpty("context")));
            var c=new Carrier(ammo,weapon,row.getIntOr("spectral",0),row.getIntOr("fire",0),row.getIntOr("variant",0),context);
            c.traitsActive=row.getBooleanOr("traits",true);c.terminal=row.getBooleanOr("terminal",false);c.lastReactiveTick=row.getBooleanOr("reactive_this_tick",false)?game.server().getTickCount():Integer.MIN_VALUE;
            for(var contact:row.getListOrEmpty("contacts"))contact.asString().ifPresent(id->c.contacts.add(UUID.fromString(id)));
            if(row.contains("payloads")){c.payloads=LbcTmtAmmoPayloads.CODEC.parse(ops,row.getCompoundOrEmpty("payloads")).getOrThrow();c.payloads.rebase(tag.getIntOr("frame",Integer.MIN_VALUE),game.server().getTickCount());}
            carriers.add(c);
        }
        if(carriers.isEmpty())return null;
        var result=new LbcBeamAmmo(game,owner,ItemStack.CODEC.parse(ops,tag.getCompoundOrEmpty("weapon")).getOrThrow(),carriers);
        result.worldVariants=tag.getBooleanOr("world",true);result.precise=tag.getBooleanOr("precise",false);result.previous=tag.contains("previous")?Vec3.CODEC.parse(ops,tag.get("previous")).getOrThrow():null;
        result.lastNativeTick=tag.getBooleanOr("native_this_tick",false)?game.server().getTickCount():Integer.MIN_VALUE;return result;
    }
    private static int variant(ItemStack ammo){
        var contents=ammo.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY);
        int color=contents.customColor().orElse(-1);
        if(color==6710877)return 300;if(color==6710881)return 301;if(color==6710882)return 302;
        for(var effect:contents.getAllEffects())if(effect.is(MobEffects.RAID_OMEN))return effect.getAmplifier();
        return 0;
    }
    @Override public int variant(){return variant;}
    /** Intrinsic ammunition and all extra properties participate without replacing their source snapshots. */
    public boolean hasVariant(int value){return variants.contains(value);}
    public Set<Integer> variants(){return variants;}
    @Override public void blockContact(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause){
        if(!worldVariants||cause==null||!game.items().currentEpoch(epoch))return;
        for(var c:carriers)if(!c.terminal){c.terminal=true;game.items().quiver().groundCarrier(c.context,owner,level,point,c.context.getYRot(),c.context.getXRot(),c.ammo,cause);
            game.items().quiver().payloadGround(c.context,c.payloads,owner,level,point,c.context.getYRot(),c.context.getXRot(),cause);}
    }

    @Override public void sample(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause,boolean railgun){
        if(owner==null||worldVariants&&owner.level()!=level||!game.items().currentEpoch(epoch))return;
        // One spatial query per sample is shared by every ammo member of this accepted family.
        var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(point,point).inflate(3),target->
                target!=owner&&target.isAlive()&&!target.isSpectator()&&!(target instanceof Creeper)
                        &&target.position().distanceToSqr(point)<=9&&target.getType().builtInRegistryHolder().is(MOBS)
                        &&game.items().wands().validRoleTarget(target,false)&&game.items().wands().validTarget(target));
        sampleTargets(level,point,cause,railgun,targets);
    }
    /** Mechanical sniper: one exact segment query per tick, shared by every source sample/carrier. */
    void lineSegment(ServerLevel level,Vec3 from,Vec3 to,LbcIndirectKills.Owner cause){
        if(owner==null||!game.items().currentEpoch(epoch))return;
        var targets=level.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(1),target->
                target!=owner&&target.isAlive()&&!target.isSpectator()&&!(target instanceof Creeper)
                &&target.getType().builtInRegistryHolder().is(MOBS)&&game.items().wands().validRoleTarget(target,false)
                &&game.items().wands().validTarget(target)&&(target.getBoundingBox().contains(from)||target.getBoundingBox().clip(from,to).isPresent()));
        previous=from;
        int samples=Math.max(1,(int)Math.round(from.distanceTo(to)/.3));
        for(int i=0;i<=samples;i++)sampleTargets(level,from.lerp(to,(double)i/samples),cause,false,targets);
    }
    private void sampleTargets(ServerLevel level,Vec3 point,LbcIndirectKills.Owner cause,boolean railgun,List<LivingEntity> targets){
        var start=previous==null?carriers.getFirst().context.position():previous;
        var direction=previous==null?carriers.getFirst().context.getDeltaMovement().normalize():point.subtract(previous).normalize();previous=point;
        for(var carrier:carriers){
            var context=carrier.context;context.setDeltaMovement(direction);
            context.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));
            context.setXRot((float)-Math.toDegrees(Math.asin(Math.clamp(direction.y,-1,1))));
            int tick=game.server().getTickCount();
            if(carrier.payloads!=null)carrier.payloads.rebase(tick);
            if(worldVariants&&carrier.lastReactiveTick!=tick&&game.items().quiver().reactive(owner,level,point,carrier.ammo,cause))carrier.lastReactiveTick=tick;
            // Source custom children share the accepted cause and existing native services.
            if(worldVariants)game.items().quiver().beamSample(owner,level,point,cause,carrier.variant);
            if(worldVariants&&carrier.payloads!=null)for(var entry:carrier.payloads.entries){entry.advance(tick);if(entry.expired())continue;
                game.items().quiver().payloadReactive(entry,owner,level,point,cause);
                game.items().quiver().beamSample(owner,level,point,cause,entry.payload.variant);
                if(railgun&&entry.payload.variant==111)for(var target:targets)game.items().indirectKills().withCause(target,cause,
                        ()->target.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,200,111),owner));
            }
            if(railgun&&carrier.variant==111)for(var target:targets)game.items().indirectKills().withCause(target,cause,
                    ()->target.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN,200,111),owner));
            if(shulker)game.items().shulkerBow().beamSample(owner,weapon,context,point,cause);
            if(shulker&&carrier.variant!=113&&carrier.payloads!=null&&carrier.payloads.entries.stream().anyMatch(e->!e.expired()&&e.payload.variant==113))
                game.items().shulkerBow().beamSample(owner,weapon,context,point,cause,true);
            if(carrier.traitsActive){game.items().trinketArrows().beamSample(context,point);carrier.traitsActive=!context.isRemoved();}
            else context.setPos(point);
            for(var target:targets)if((!precise||target.getBoundingBox().contains(point)||target.getBoundingBox().clip(start,point).isPresent())&&carrier.contacts.add(target.getUUID()))contact(level,carrier,target,cause);
        }
    }
    private void contact(ServerLevel level,Carrier carrier,LivingEntity target,LbcIndirectKills.Owner cause){
        game.items().indirectKills().withCause(target,cause,()->{
            if(LbcQuiver.carrierColor(carrier.ammo)==6710884||carrier.payloads!=null&&carrier.payloads.entries.stream().anyMatch(e->e.payload.color==6710884)){
                var hit=new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.ARROW),carrier.context,owner);
                target.hurtServer(level,game.items().indirectKills().captureDamage(hit,cause),1e20f);
            }

            if(carrier.fire>0){target.igniteForSeconds(5);game.items().indirectKills().projectileIgnited(target,owner,true);}
            if(carrier.spectral>0)target.addEffect(new MobEffectInstance(MobEffects.GLOWING,carrier.spectral),owner);
            if(shulker)target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,200),owner);
            carrier.ammo.getOrDefault(DataComponents.POTION_CONTENTS,PotionContents.EMPTY).forEachEffect(effect->{
                if(game.items().quiver().acceptCarrier(target,effect,owner,cause))return;
                if(effect.getEffect().value().isInstantaneous())effect.getEffect().value().applyInstantaneousEffect(level,carrier.context,owner,target,effect.getAmplifier(),1);
                else target.addEffect(effect,owner);
            },carrier.ammo.getOrDefault(DataComponents.POTION_DURATION_SCALE,1f));
            game.items().quiver().payloadContact(carrier.context,carrier.payloads,target,owner,cause);
            if(madoka)game.items().madokaBow().beamContact(owner,weapon,carrier.context,target);
        });
    }
    /** No Arrow entity is part of the saved graph; only typed native values and owner links are retained. */
    private record Image(Carrier carrier,Set<String> tags,Set<UUID> contacts,boolean traitsActive,boolean terminal,boolean reactiveThisTick,
                         Vec3 point,Vec3 velocity,float yaw,float pitch,boolean critical,byte pierce,
                         double damage,int fire,UUID id,LbcTmtAmmoPayloads.State payloads) { }
    @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchSpellCheckpoint.Links links){
        var author=links.key(owner);var savedPrevious=previous;int payloadFrame=game.server().getTickCount();boolean nativeThisTick=lastNativeTick==payloadFrame;
        var images=carriers.stream().map(c->new Image(c,Set.copyOf(c.context.entityTags()),Set.copyOf(c.contacts),c.traitsActive,c.terminal,c.lastReactiveTick==game.server().getTickCount(),
                c.context.position(),c.context.getDeltaMovement(),c.context.getYRot(),c.context.getXRot(),c.context.isCritArrow(),
                c.context.getPierceLevel(),((ShulkerBowMixin.ArrowAccess)c.context).skywars2$baseDamage(),c.context.getRemainingFireTicks(),c.context.getUUID(),c.payloads==null?null:c.payloads.copy())).toList();
        return rebound->{
            var restored=(ServerPlayer)rebound.get(author);var contexts=new ArrayList<Arrow>(images.size());
            for(var image:images){
                var c=image.carrier;var context=new Arrow(c.level,restored,c.ammo.copy(),c.weapon.copy());
                context.setUUID(image.id);context.entityTags().addAll(image.tags);context.setNoGravity(true);
                context.setPos(image.point);context.setDeltaMovement(image.velocity);context.setYRot(image.yaw);context.setXRot(image.pitch);
                context.setCritArrow(image.critical);((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)context).skywars2$setPierceLevel(image.pierce);context.setBaseDamage(image.damage);context.setRemainingFireTicks(image.fire);
                contexts.add(context);
            }
            return ()->{owner=restored;previous=savedPrevious;lastNativeTick=nativeThisTick?game.server().getTickCount():Integer.MIN_VALUE;for(int i=0;i<images.size();i++){
                var image=images.get(i);var c=image.carrier;c.context=contexts.get(i);c.contacts.clear();c.contacts.addAll(image.contacts);c.traitsActive=image.traitsActive;c.terminal=image.terminal;c.lastReactiveTick=image.reactiveThisTick?game.server().getTickCount():Integer.MIN_VALUE;c.payloads=image.payloads==null?null:image.payloads.copy();if(c.payloads!=null)c.payloads.rebase(payloadFrame,game.server().getTickCount());
            }};
        };
    }
}
