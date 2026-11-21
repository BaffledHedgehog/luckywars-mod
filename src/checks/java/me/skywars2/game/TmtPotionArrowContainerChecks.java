package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.*;

/** Actual native potion-arrow collisions and old recursive container placement/open/break. */
public final class TmtPotionArrowContainerChecks {
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("TMT potion arrows/container: "+why);}
    private static LbcTmtAbilities.Ability dose(GameController game,String name){
        return game.items().tmt().catalog().families().stream().filter(f->f.id().equals("native/ammo/potion/minecraft:"+name)).findFirst().orElseThrow().variants().getFirst();
    }
    private static ItemStack combined(int carrier,int count,List<LbcTmtAbilities.Ability> abilities){
        var item=LbcTmtItems.prototype(carrier).copyWithCount(count);var data=item.get(DataComponents.CUSTOM_DATA).copyTag();LbcTmtAbilities.write(data,abilities);item.set(DataComponents.CUSTOM_DATA,CustomData.of(data));return item;
    }
    private static void collide(AbstractArrow arrow,Cow victim){arrow.setPos(victim.position().add(0,.5,-2));arrow.setDeltaMovement(new Vec3(0,0,4));arrow.tick();}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false);
                var ownerStats=new NativeMeleeChecks.NativeStats(f.owner,List.of(LbcTmtItems.prototype(3).getItem(),Items.TIPPED_ARROW));
                var otherStats=new NativeMeleeChecks.NativeStats(f.other,List.of(LbcTmtItems.prototype(0).getItem(),Items.DYED_SHULKER_BOX.pick(DyeColor.BLACK)))){
            f.prepareParticipants();var original=new HashSet<UUID>();f.level.getAllEntities().forEach(e->original.add(e.getUUID()));
            try{arrows(game,f);container(game,f);}
            finally{for(var e:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!original.contains(e.getUUID()))f.own(e);}
        }
    }
    private static void arrows(GameController game,WoolBucketChecks.Fixture f){
        var poison=dose(game,"poison");var weakness=dose(game,"weakness");
        var victim=new Cow(EntityTypes.COW,f.level);victim.setNoAi(true);victim.setNoGravity(true);victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);victim.setHealth(1000);victim.setPos(f.home.add(0,500,6));require(f.level.addFreshEntity(victim),"real native target");f.own(victim);
        var expected=new HashMap<net.minecraft.core.Holder<MobEffect>,MobEffectInstance>();
        for(var property:List.of(poison,weakness)){
            var source=LbcTmtComponents.source(game,property);require(source.is(Items.TIPPED_ARROW)&&source.get(DataComponents.POTION_DURATION_SCALE)==.125f,"native arrow duration scale remains in actual source");
            var control=((ArrowItem)source.getItem()).createArrow(f.level,source,f.owner,new ItemStack(Items.BOW));f.own(control);require(f.level.addFreshEntity(control),"native control arrow publication");collide(control,victim);
            for(var effect:victim.getActiveEffects())expected.put(effect.getEffect(),new MobEffectInstance(effect));victim.removeAllEffects();victim.setInvulnerableTime(0);victim.damageCooldownTime=0;victim.setHealth(1000);f.discard(control);
        }
        require(expected.containsKey(MobEffects.POISON)&&expected.containsKey(MobEffects.WEAKNESS),"real control hits apply both ordinary effects");
        var weapon=combined(3,1,List.of(poison,weakness));weapon.set(DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.of(12345),List.of(new MobEffectInstance(MobEffects.REGENERATION,900,2)),Optional.empty()));
        var ammo=new ItemStack(Items.ARROW,5);f.owner.setItemInHand(InteractionHand.MAIN_HAND,weapon);f.owner.setItemInHand(InteractionHand.OFF_HAND,ammo);weapon.set(DataComponents.CHARGED_PROJECTILES,ChargedProjectiles.ofNonEmpty(List.of(ammo.copyWithCount(1))));
        var prior=new HashSet<UUID>();f.level.getAllEntities().forEach(e->prior.add(e.getUUID()));
        require(f.owner.gameMode.useItem(f.owner,f.level,weapon,InteractionHand.MAIN_HAND).consumesAction(),"real native TMT charged shot");
        var bodies=new ArrayList<AbstractArrow>();for(var e:f.level.getAllEntities())if(!prior.contains(e.getUUID())&&e instanceof AbstractArrow arrow){bodies.add(arrow);f.own(e);}
        require(bodies.size()==1&&ammo.getCount()==5&&weapon.get(DataComponents.CHARGED_PROJECTILES).isEmpty(),"one accepted native chamber, no extra ammo debit or free reload");var arrow=bodies.getFirst();
        require(ItemStack.isSameItemSameComponents(arrow.getPickupItemStackOrigin(),ammo)&&LbcTmtAmmoPayloads.state(arrow).entries.size()==2,"physical ammo/pickup unchanged and two private payloads retained");
        var image=LbcTmtRanged.image(game,arrow);var restored=LbcTmtRanged.donors(f.owner,List.of(image)).getFirst();require(LbcTmtAmmoPayloads.state(restored).entries.size()==2,"native entity codec keeps both doses");f.discard(restored);
        collide(arrow,victim);require(victim.getHealth()<1000&&!victim.hasEffect(MobEffects.REGENERATION),"accepted physical hit; unrelated outer potion cannot leak");
        for(var entry:expected.entrySet()){var actual=victim.getEffect(entry.getKey());var control=entry.getValue();require(actual!=null&&actual.getAmplifier()==control.getAmplifier()&&actual.getDuration()==control.getDuration(),"composed hit matches actual native potion-arrow level/duration: "+entry.getKey());}
        require(!LbcTmtAmmoPayloads.supports(new ItemStack(Items.TIPPED_ARROW))&&!LbcTmtAmmoPayloads.supports(LbcImbaNativeItems.INFINITE_ARROW.getDefaultInstance()),"empty arrows and free-ammo markers are not extra status payloads");
        f.discard(arrow);f.discard(victim);
    }
    private static void container(GameController game,WoolBucketChecks.Fixture f){
        LbcTmtAbilities.Ability old=null;
        for(var family:game.items().tmt().catalog().families())for(var property:family.variants()){
            if(LbcTmtComponents.nativeItem(property.data())!=Items.DYED_SHULKER_BOX.pick(DyeColor.BLACK))continue;
            var source=LbcTmtComponents.source(game,property);var table=source.get(DataComponents.CONTAINER_LOOT);if(table!=null&&table.lootTable().identifier().toString().equals("lbc:infinity_old")){old=property;break;}
        }
        require(old!=null,"real old recursive shulker source admitted");require(!LbcTmtBlockUses.authoredShulker(new ItemStack(Items.DYED_SHULKER_BOX.pick(DyeColor.BLACK))),"bare vanilla box remains outside ability domain");
        var item=combined(0,2,List.of(old));var outerLoot=new SeededContainerLoot(ResourceKey.create(Registries.LOOT_TABLE,Identifier.parse("minecraft:chests/simple_dungeon")),87L);item.set(DataComponents.CONTAINER_LOOT,outerLoot);var before=item.copyWithCount(1);f.other.setItemInHand(InteractionHand.MAIN_HAND,item);
        var floor=f.other.blockPosition().offset(0,-1,3);var at=floor.above();f.set(floor,Blocks.STONE.defaultBlockState());f.set(at,Blocks.AIR.defaultBlockState());f.set(at.above(),Blocks.AIR.defaultBlockState());
        var hit=new BlockHitResult(Vec3.atBottomCenterOf(at),Direction.UP,floor,false);require(f.other.gameMode.useItemOn(f.other,f.level,item,InteractionHand.MAIN_HAND,hit).consumesAction()&&ItemStack.matches(item,before),"native placement debits one actual carrier and restores outer components");
        require(f.level.getBlockEntity(at) instanceof ShulkerBoxBlockEntity,"real native placed container");var box=(ShulkerBoxBlockEntity)f.level.getBlockEntity(at);
        require(box.getLootTable()!=null&&box.getLootTable().identifier().toString().equals("lbc:infinity_old"),"private old loot table wins over conflicting outer dungeon table");box.unpackLootTable(f.other);
        boolean recursive=false;for(int i=0;i<box.getContainerSize();i++){var nested=box.getItem(i).get(DataComponents.CONTAINER_LOOT);if(nested!=null&&nested.lootTable().identifier().toString().equals("lbc:infinity_old"))recursive=true;}
        require(recursive&&box.getLootTable()==null,"actual old loot opens once and returns its own recursive container");
        var prior=new HashSet<UUID>();f.level.getAllEntities().forEach(e->prior.add(e.getUUID()));require(f.level.destroyBlock(at,true,f.other),"actual placed shulker break");
        var drops=new ArrayList<ItemEntity>();for(var e:f.level.getAllEntities())if(!prior.contains(e.getUUID())&&e instanceof ItemEntity drop){drops.add(drop);f.own(drop);}
        require(drops.size()==1&&LbcTmtItems.matches(drops.getFirst().getItem()),"one restored physical TMT drop, not a loose container/contents fanout");var restored=drops.getFirst().getItem();
        require(outerLoot.equals(restored.get(DataComponents.CONTAINER_LOOT))&&LbcTmtAbilities.read(restored).size()==1,"outer source and its single private ability restored");
        var privateBox=LbcTmtComponents.source(game,LbcTmtAbilities.read(restored).getFirst());require(!privateBox.has(DataComponents.CONTAINER_LOOT)&&privateBox.has(DataComponents.CONTAINER),"opened private container contents survive break without regenerating loot");
    }
}
