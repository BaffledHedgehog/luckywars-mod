package me.skywars2.game.items.weapons.ranged;

import eu.pb4.polymer.core.api.item.PolymerItem;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.GameController;
import me.skywars2.game.entities.LbcDimensionItems;
import me.skywars2.game.items.ItemData;
import me.skywars2.game.items.LbcUtilityItemPrototypes;
import me.skywars2.game.items.tmt.LbcTmtAmmoPayloads;
import me.skywars2.game.items.tmt.LbcTmtItems;
import me.skywars2.game.items.tmt.LbcTmtRanged;
import me.skywars2.game.items.tmt.LbcTmtTriggers;
import me.skywars2.game.items.weapons.firearms.LbcMinigunCrossbow;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/** Source crossbows share native draw, successful-volley hooks and the existing Erecto traces. */
public final class LbcImbaCrossbows {
    public enum Kind {
        QUICK("imba_quick_crossbow","d33ed6f281fa",null),
        FAST("imba_fast_crossbow","9edfe7cdf94b","imba_fast_crossbow"),
        SHOTGUN("imba_shotgun","5a9a22e48f9c","imba_shotgun"),
        SNIPER("imba_sniper_crossbow","1e5550f8a752","imba_sniper_crossbow"),
        SUPER("imba_super_shotgun","9ba94abb5b06","imba_super_shotgun"),
        GATLING("imba_gatlingator","e3ca7ba5ed19","imba_gatlingator");
        public final String id,definition,flag;public Item item;
        Kind(String id,String suffix,String flag){this.id=id;definition="/data/lbc/loot_table/item_defs/minecraft/crossbow__"+suffix+".json";this.flag=flag;}
        boolean continuous(){return this==FAST||this==GATLING;}
        boolean beam(){return beamPaths()>0;}
        public int beamPaths(){return switch(this){case SNIPER->1;case SUPER->32;case GATLING->4;default->0;};}
    }
    static {for(var kind:Kind.values())kind.item=register(kind);}
    private LbcImbaCrossbows(){}
    public static void register(){}
    static Kind kind(ItemStack stack){
        if(stack.isEmpty())return null;
        for(var kind:Kind.values())if(stack.is(kind.item)||kind.flag!=null&&ItemData.intValue(stack,kind.flag,0)==1)return kind;
        return null;
    }
    public static Item legacyTarget(ItemStack stack){var kind=kind(stack);return kind==null?null:kind.item;}
    public static boolean isBeam(ItemStack stack){var kind=kind(stack);return kind!=null&&kind.beam();}
    static boolean allowed(GameController game,ServerPlayer player){return game.playing()&&game.canUseGameplayItems(player)
            &&LbcDimensionItems.ordinary(player.level())&&!game.items().actionsBlocked(player)
            &&!game.items().wands().inputTimeStopped(player)&&!game.items().magic().mechanismSuppressed(player)
            &&!game.items().magic().artifactSuppressedAt(player.level(),player.position());}
    public static boolean canShoot(LivingEntity shooter,ItemStack weapon){return shotAdmission(shooter,weapon,false)>=0;}
    /** Reserves the whole family before native payment. A negative result rejects the shot. */
    public static int reserveShot(LivingEntity shooter,ItemStack weapon){return shotAdmission(shooter,weapon,true);}
    public static void releaseUnusedShot(LivingEntity shooter,int paths){
        if(paths>0&&shooter instanceof ServerPlayer owner){
            var game=SkyWars2.game();
            if(game!=null)game.items().erecto().releaseShotPaths(owner,paths);
        }
    }
    private static int shotAdmission(LivingEntity shooter,ItemStack weapon,boolean reserve){
        if(!(shooter instanceof ServerPlayer owner)
                ||!weapon.has(DataComponents.CUSTOM_DATA)&&!(weapon.getItem() instanceof NativeCrossbow))return 0;
        int count;
        if(LbcTmtItems.matches(weapon)){
            count=LbcTmtTriggers.nativeShotAbilities(weapon).stream().mapToInt(LbcTmtRanged::beamPaths).sum();
        }else{
            var kind=kind(weapon);
            if(kind==null||!kind.beam()||LbcChargedBows.matches(weapon))return 0;
            count=kind.beamPaths();
        }
        if(count==0)return 0;
        var game=SkyWars2.game();
        if(game==null||!allowed(game,owner)||LbcRangedShots.replacesNative(game,owner,weapon))return 0;
        return game.items().erecto().admitCrossbow(owner,count,reserve)?count:-1;
    }
    private static Item register(Kind kind){
        var properties=LbcUtilityItemPrototypes.deferredProperties("skywars2:"+kind.id,kind.definition,Items.CROSSBOW,1);
        return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+kind.id),new NativeCrossbow(properties,kind));
    }
    private static final class NativeCrossbow extends LbcProjectileWeaponItems.NativeCrossbow {
        final Kind kind;
        NativeCrossbow(Properties properties,Kind kind){super(properties);this.kind=kind;}

        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
            if(kind!=Kind.QUICK&&player instanceof ServerPlayer owner){var game=SkyWars2.game();if(game==null||!allowed(game,owner))return InteractionResult.FAIL;}
            if(!kind.continuous())return super.use(level,player,hand);
            player.startUsingItem(hand);return InteractionResult.CONSUME;
        }
        @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return kind.continuous()?72000:super.getUseDuration(stack,entity);}
        @Override public boolean releaseUsing(ItemStack stack,Level level,LivingEntity entity,int remaining){return !kind.continuous()&&super.releaseUsing(stack,level,entity,remaining);}
        @Override public void onUseTick(Level level,LivingEntity entity,ItemStack stack,int remaining){
            if(!kind.continuous()){super.onUseTick(level,entity,stack,remaining);return;}
            if(!(entity instanceof ServerPlayer owner)||owner.getUseItem()!=stack)return;
            var game=SkyWars2.game();if(game==null||!allowed(game,owner))return;
            if(kind==Kind.GATLING&&!canShoot(owner,stack))return;
            if(!isCharged(stack)){
                var selected=LbcMinigunCrossbow.selectedAmmo(owner,this);if(selected.isEmpty())return;
                var drawn=LbcInfiniteAmmo.drawReload(owner,stack,selected);if(drawn.isEmpty())return;
                stack.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(drawn));
                // The source Gatlingator spends three arrows per active tick, including its last partial stack.
                // Selected infinite ammo alone is free; unrelated inventory flags never conserve ammo.
                if(kind==Kind.GATLING&&!owner.hasInfiniteMaterials()&&!LbcInfiniteAmmo.conserves(selected)){
                    for(int i=0;i<2;i++){var extra=LbcMinigunCrossbow.selectedAmmo(owner,this);if(extra.isEmpty()||LbcInfiniteAmmo.conserves(extra))break;extra.shrink(1);}
                }
            }
            performShooting(owner.level(),owner,owner.getUsedItemHand(),stack,3.15f,1,null);
        }
    }
    /** Expand only this accepted physical family, before its single shared trinket roll. */
    public static List<Projectile> augment(GameController game,ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        var kind=kind(weapon);if(kind==null||kind==Kind.QUICK)return family;
        if(!allowed(game,owner)){family.forEach(Projectile::discard);return List.of();}
        if(kind.beam())return family;
        var result=new ArrayList<Projectile>();
        for(var original:family){
            boolean synthetic=!(original instanceof AbstractArrow);
            if(synthetic){result.add(original);if(kind!=Kind.SHOTGUN)continue;}
            var arrow=original instanceof AbstractArrow a?a:new net.minecraft.world.entity.projectile.arrow.Arrow(owner.level(),owner,new ItemStack(Items.ARROW),weapon);
            if(synthetic)arrow.setPos(owner.getEyePosition());
            int count=kind==Kind.SHOTGUN?256:1;
            for(int i=0;i<count;i++){
                AbstractArrow shot=arrow;
                if(i>0){
                    var ammo=arrow.getPickupItemStackOrigin();var item=ammo.getItem() instanceof ArrowItem a?a:(ArrowItem)Items.ARROW;
                    shot=item.createArrow(owner.level(),ammo,owner,weapon);
                    shot.setPos(arrow.position());shot.entityTags().addAll(arrow.entityTags());shot.setBaseDamage(((me.skywars2.mixin.projectiles.ShulkerBowMixin.ArrowAccess)arrow).skywars2$baseDamage());
                    LbcTmtAmmoPayloads.copyPayloads(arrow,shot);
                    shot.setCritArrow(arrow.isCritArrow());shot.setRemainingFireTicks(arrow.getRemainingFireTicks());shot.setNoGravity(arrow.isNoGravity());
                    ((me.skywars2.mixin.projectiles.WandProjectileMixin.ArrowAccess)shot).skywars2$setPierceLevel(arrow.getPierceLevel());
                    shot.pickup=arrow.pickup;
                }
                var aim=owner.getLookAngle();var random=owner.getRandom();
                if(kind==Kind.SHOTGUN){shot.setDeltaMovement(aim.scale(5).add((random.nextInt(2001)-1000)*.0005,(random.nextInt(2001)-1000)*.0005,(random.nextInt(2001)-1000)*.0005));shot.pickup=AbstractArrow.Pickup.DISALLOWED;}
                else shot.setDeltaMovement(aim.scale(7).add((random.nextInt(9)-4)*.1,(random.nextInt(9)-4)*.1,(random.nextInt(9)-4)*.1));
                if(i>0||synthetic){game.items().markCurrentItemEpoch(shot);game.items().timeStop().prepareFrozen(shot);if(!owner.level().addFreshEntity(shot))continue;}
                result.add(shot);
            }
        }
        return result;
    }
    /** A beam replaces exactly its own accepted family, with its already rolled ammo/traits. */
    public static boolean release(GameController game,ServerPlayer owner,ItemStack weapon,List<Projectile> family){
        var kind=kind(weapon);if(kind==null||!kind.beam())return false;
        if(!family.isEmpty()&&allowed(game,owner))game.items().erecto().crossbow(owner,weapon,family,kind);
        for(var projectile:family)if(projectile instanceof AbstractArrow)projectile.discard();return true;
    }
}
