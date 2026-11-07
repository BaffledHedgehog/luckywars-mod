package me.skywars2.game;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Fireworks;

/** Four current source ranged properties, at the accepted native shot boundary. */
public final class LbcLegacyRanged {
    private LbcLegacyRanged(){}
    private static final net.fabricmc.fabric.api.attachment.v1.AttachmentType<Ammo> AMMO=
            net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry.create(net.minecraft.resources.Identifier.parse("skywars2:legacy_ranged_ammo"),
                    b->b.persistent(net.minecraft.nbt.CompoundTag.CODEC.xmap(Ammo::new,Ammo::save)));
    public static void register(){}
    static net.fabricmc.fabric.api.attachment.v1.AttachmentType<Ammo> ammoType(){return AMMO;}
    /** Persistent actor-owned accepted ammo; mutable contexts are private to this native body. */
    static final class Ammo {
        private net.minecraft.nbt.CompoundTag encoded;
        private LbcBeamAmmo payload;
        private LbcIndirectKills.Owner cause;
        Ammo(net.minecraft.nbt.CompoundTag encoded){this.encoded=encoded.copy();}
        Ammo(LbcBeamAmmo payload,LbcIndirectKills.Owner cause){this.payload=payload;this.cause=cause;}
        private boolean resolve(GameController game,Entity actor){
            if(payload!=null)return true;
            payload=LbcBeamAmmo.load(game,(net.minecraft.server.level.ServerLevel)actor.level(),encoded.getCompoundOrEmpty("payload"));
            if(payload==null)return false;
            cause=new LbcIndirectKills.Owner(java.util.UUID.fromString(encoded.getStringOr("author","")),encoded.getLongOr("order",0),encoded.getBooleanOr("friendly",false),true);
            encoded=null;return true;
        }
        net.minecraft.nbt.CompoundTag save(){
            if(payload==null)return encoded.copy();var tag=new net.minecraft.nbt.CompoundTag();tag.put("payload",payload.save());
            tag.putString("author",cause.player().toString());tag.putLong("order",cause.order());tag.putBoolean("friendly",cause.friendlyFire());return tag;
        }
        Ammo copy(){return payload==null?new Ammo(encoded):new Ammo(payload.copy(),cause);}
    }
    static void captureAmmo(GameController game,ServerPlayer owner,ItemStack weapon,Projectile donor,Entity output){
        var payload=(LbcBeamAmmo)LbcBeamAmmo.capture(game,owner,weapon,List.of(donor));
        if(payload!=null)output.setAttached(AMMO,new Ammo(payload,game.items().indirectKills().coldCause(owner,true)));
    }
    static void copyAmmo(Entity original,Entity clone){var ammo=original.getAttached(AMMO);if(ammo!=null)clone.setAttached(AMMO,ammo.copy());}
    static void flight(GameController game,Entity actor){var ammo=actor.getAttached(AMMO);if(ammo!=null&&ammo.resolve(game,actor))ammo.payload.nativeFlight(actor,ammo.cause);}
    static void contact(GameController game,Entity actor,net.minecraft.world.entity.LivingEntity target,net.minecraft.world.phys.Vec3 point){var ammo=actor.getAttached(AMMO);if(ammo!=null&&ammo.resolve(game,actor))ammo.payload.nativeContact(actor,target,point,ammo.cause);}
    static void ground(GameController game,Entity actor,net.minecraft.world.phys.Vec3 point){var ammo=actor.getAttached(AMMO);if(ammo!=null&&ammo.resolve(game,actor))ammo.payload.nativeGround(actor,point,ammo.cause);}
    static Boolean canAffect(GameController game,Entity actor,Entity target){var ammo=actor.getAttached(AMMO);return ammo==null?null:ammo.resolve(game,actor)&&ammo.payload.nativeTarget(target);}
    public static boolean hasAmmo(Entity actor){return actor.getAttached(AMMO)!=null;}
    static void markTraits(GameController game,Entity actor,int mask){var ammo=actor.getAttached(AMMO);if(ammo!=null&&ammo.resolve(game,actor))ammo.payload.markTraits(mask);}
    /** Selected source ammo belongs to each actual next-chamber rocket, independent of its fixed visuals. */
    public static void installRocketAmmo(GameController game,ServerPlayer owner,ItemStack fired,List<? extends Projectile> family){
        boolean ghast=ItemData.intValue(fired,"fastexplode",0)==1||LbcTmtAbilities.read(fired).stream().anyMatch(a->((ItemData.IntReader)(Object)a.data()).skywars2$readInt("fastexplode",0)==1);
        for(var shot:family)if(shot instanceof net.minecraft.world.entity.projectile.FireworkRocketEntity rocket&&!shot.isRemoved()&&shot.getOwner()==owner&&!hasAmmo(shot)){
            var stored=rocket.getItem().getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            if(!ghast&&!stored.contains("skywars2_ghast_ammo"))continue;
            var source=stored.contains("skywars2_ghast_ammo")?ItemStack.CODEC.parse(owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),stored.getCompoundOrEmpty("skywars2_ghast_ammo")).getOrThrow():new ItemStack(Items.ARROW);
            var donor=source.getItem() instanceof net.minecraft.world.item.ArrowItem arrow?arrow.createArrow(owner.level(),source,owner,fired):new Arrow(owner.level(),owner,new ItemStack(Items.ARROW),fired);
            donor.setOwner(owner);donor.setPos(shot.position());donor.setDeltaMovement(game.items().timeStop().persistedVelocity(shot,shot.getDeltaMovement()));donor.setYRot(shot.getYRot());donor.setXRot(shot.getXRot());
            LbcTmtAmmoPayloads.install(game,owner,fired,List.of(donor));captureAmmo(game,owner,fired,donor,shot);LbcTmtAmmoPayloads.copyPayloads(donor,shot);donor.discard();
        }
    }
    public static void rocketFlight(GameController game,Entity rocket){flight(game,rocket);}
    public static void rocketContact(GameController game,Entity rocket,net.minecraft.world.entity.LivingEntity target){contact(game,rocket,target,rocket.position());}
    public static void rocketGround(GameController game,Entity rocket,net.minecraft.world.phys.Vec3 point){ground(game,rocket,point);}
    public static boolean beforeArrow(GameController game,net.minecraft.world.entity.projectile.arrow.AbstractArrow arrow){
        if(arrow.level() instanceof net.minecraft.server.level.ServerLevel level&&arrow.entityTags().contains("skywars2.legacy_double")&&!game.items().timeStop().frozen(arrow)&&!game.items().timeStop().stopped(level)
                &&((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)arrow).skywars2$isInGround()){arrow.discard();return false;}return true;
    }
    private static final Fireworks GHAST_FIREWORKS=Fireworks.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,
            com.google.gson.JsonParser.parseString("{\"flight_duration\":3,\"explosions\":["
                    +"{\"shape\":\"star\",\"colors\":[16772608],\"fade_colors\":[16711680],\"has_trail\":true,\"has_twinkle\":true},"
                    +"{\"shape\":\"small_ball\",\"colors\":[16712965],\"fade_colors\":[10249480],\"has_trail\":true,\"has_twinkle\":true},"
                    +"{\"shape\":\"large_ball\",\"colors\":[16757504],\"fade_colors\":[9539985],\"has_trail\":true,\"has_twinkle\":true}]}"))
            .getOrThrow();
    public static boolean converts(ItemStack weapon){return ItemData.intValue(weapon,"fastfired",0)==1||ItemData.intValue(weapon,"penisgun",0)==1;}
    /** Current double shares the old native arrow-copy owner; no extra ammunition draw. */
    public static List<Projectile> augment(GameController game,ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        return ItemData.intValue(weapon,"double",0)==1?game.items().balance().augment(owner,weapon,family):family;
    }
    /** After the one shared trait roll. Only the accepted ordinary arrow is converted. */
    public static boolean convert(GameController game,ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        boolean fire=ItemData.intValue(weapon,"fastfired",0)==1&&!game.items().magic().mechanismSuppressed(owner);
        boolean bomb=ItemData.intValue(weapon,"penisgun",0)==1;
        if(!fire&&!bomb)return false;
        // The source nearest/limit1 selector means one conversion per accepted volley.
        for(var shot:family){
            if(!(shot instanceof Arrow)||shot.isRemoved()||shot.getOwner()!=owner||shot.level()!=owner.level())continue;
            var motion=game.items().timeStop().persistedVelocity(shot,shot.getDeltaMovement());
            Entity output;
            if(fire)output=game.items().specialFireballs().spawnRangedFire(owner,owner.getEyePosition().add(owner.getLookAngle().scale(1.2)),motion,shot);
            else output=game.items().kfcRewards().launchRangedBomb(owner,shot.position(),motion,2.5f);
            if(output==null)return false;
            captureAmmo(game,owner,weapon,shot,output);
            LbcTmtAmmoPayloads.copyPayloads(shot,output);
            if(bomb){
                output.entityTags().addAll(shot.entityTags());
            }
            shot.discard();return true;
        }
        return false;
    }
    static ItemStack ghastRocket(){var rocket=new ItemStack(Items.FIREWORK_ROCKET);rocket.set(DataComponents.FIREWORKS,GHAST_FIREWORKS);return rocket;}
    /** Modifies only the next chamber of the same live weapon in the hand that fired. */
    public static void afterShot(GameController game,ServerPlayer owner,InteractionHand hand,ItemStack weapon,List<Projectile> family,ItemStack fired){
        if(family.isEmpty())return;
        if(ItemData.intValue(fired,"penisgun",0)==1)
            owner.getInventory().placeItemBackInInventory(new ItemStack(Items.ARROW),net.minecraft.util.Prediction.SERVER_ONLY);
        if(game.items().magic().mechanismSuppressed(owner))return;
        boolean fire=ItemData.intValue(fired,"fastfired",0)==1,ghast=ItemData.intValue(fired,"fastexplode",0)==1;
        if(!fire&&!ghast)return;
        if(weapon.isEmpty()||!(weapon.getItem() instanceof CrossbowItem crossbow)||owner.getItemInHand(hand)!=weapon||CrossbowItem.isCharged(weapon))return;
        if(fire){weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(new ItemStack(Items.ARROW))));return;}
        var selected=LbcMinigunCrossbow.selectedAmmo(owner,crossbow);
        if(selected.isEmpty())return;
        var drawn=LbcInfiniteAmmo.drawReload(owner,weapon,selected);if(drawn.isEmpty())return;
        var rockets=new java.util.ArrayList<ItemStack>();for(var ammo:drawn){var rocket=ghastRocket();var stored=new net.minecraft.nbt.CompoundTag();
            stored.put("skywars2_ghast_ammo",ItemStack.CODEC.encodeStart(owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),ammo).getOrThrow());rocket.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(stored));rockets.add(rocket);}
        weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(rockets));
    }
}
