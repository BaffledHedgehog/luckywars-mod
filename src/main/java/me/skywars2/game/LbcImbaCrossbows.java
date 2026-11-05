package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItem;
import java.util.*;
import me.skywars2.SkyWars2;
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
    enum Kind {
        QUICK("imba_quick_crossbow","d33ed6f281fa",null),
        FAST("imba_fast_crossbow","9edfe7cdf94b","imba_fast_crossbow"),
        SHOTGUN("imba_shotgun","5a9a22e48f9c","imba_shotgun"),
        SNIPER("imba_sniper_crossbow","1e5550f8a752","imba_sniper_crossbow"),
        SUPER("imba_super_shotgun","9ba94abb5b06","imba_super_shotgun"),
        GATLING("imba_gatlingator","e3ca7ba5ed19","imba_gatlingator");
        final String id,definition,flag;Item item;
        Kind(String id,String suffix,String flag){this.id=id;definition="/data/lbc/loot_table/item_defs/minecraft/crossbow__"+suffix+".json";this.flag=flag;}
        boolean continuous(){return this==FAST||this==GATLING;}
        boolean beam(){return this==SNIPER||this==SUPER||this==GATLING;}
    }
    static {for(var kind:Kind.values())kind.item=register(kind);}
    private LbcImbaCrossbows(){}
    public static void register(){}
    static Kind kind(ItemStack stack){
        if(stack.isEmpty())return null;
        for(var kind:Kind.values())if(stack.is(kind.item)||kind.flag!=null&&ItemData.intValue(stack,kind.flag,0)==1)return kind;
        return null;
    }
    static Item legacyTarget(ItemStack stack){var kind=kind(stack);return kind==null?null:kind.item;}
    public static boolean isBeam(ItemStack stack){var kind=kind(stack);return kind!=null&&kind.beam();}
    static boolean allowed(GameController game,ServerPlayer player){return game.playing()&&game.canUseGameplayItems(player)
            &&LbcDimensionItems.ordinary(player.level())&&!game.items().actionsBlocked(player)
            &&!game.items().wands().inputTimeStopped(player)&&!game.items().magic().mechanismSuppressed(player)
            &&!game.items().magic().artifactSuppressedAt(player.level(),player.position());}
    private static Item register(Kind kind){
        var p=LbcUtilityItemPrototypes.properties("skywars2:"+kind.id,kind.definition,Items.CROSSBOW,1,Set.of("minecraft:enchantments"))
                .durability(465).enchantable(1);
        try(var stream=LbcImbaCrossbows.class.getResourceAsStream(kind.definition)){
            var entry=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("pools").get(0).getAsJsonObject().getAsJsonArray("entries").get(0).getAsJsonObject();
            for(var row:entry.getAsJsonArray("modifier")){
                var object=row.getAsJsonObject();if(!object.has("components"))continue;
                var components=object.getAsJsonObject("components");if(!components.has("minecraft:enchantments"))continue;
                var enchants=components.get("minecraft:enchantments").deepCopy();
                p.delayedComponent(DataComponents.ENCHANTMENTS,lookup->ItemEnchantments.CODEC.parse(lookup.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),enchants).getOrThrow());
            }
        }catch(java.io.IOException e){throw new IllegalStateException(kind.id,e);}
        return Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:"+kind.id),new NativeCrossbow(p,kind));
    }
    private static final class NativeCrossbow extends CrossbowItem implements PolymerItem {
        final Kind kind;
        NativeCrossbow(Properties properties,Kind kind){super(properties);this.kind=kind;}
        public Item getPolymerItem(ItemStack stack,PacketContext context){return Items.CROSSBOW;}
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
                    shot.setPos(arrow.position());shot.entityTags().addAll(arrow.entityTags());shot.setBaseDamage(((me.skywars2.mixin.ShulkerBowMixin.ArrowAccess)arrow).skywars2$baseDamage());
                    shot.setCritArrow(arrow.isCritArrow());shot.setRemainingFireTicks(arrow.getRemainingFireTicks());shot.setNoGravity(arrow.isNoGravity());
                    ((me.skywars2.mixin.WandProjectileMixin.ArrowAccess)shot).skywars2$setPierceLevel(arrow.getPierceLevel());
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
