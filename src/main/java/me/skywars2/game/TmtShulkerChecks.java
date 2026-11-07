package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Genuine game-mode placement/open/destroy/pickup with private loot keys and a durable physical carrier. */
public final class TmtShulkerChecks {
    private TmtShulkerChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("TMT shulker: "+why);}
    private static LbcTmtAbilities.Ability property(GameController game,ItemStack source){
        var data=source.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        data.putString(LbcTmtAbilities.NATIVE,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(source.getItem()).toString());
        LbcTmtComponents.capture(game,source,data);return new LbcTmtAbilities.Ability("native/shulker/"+source.get(DataComponents.CONTAINER_LOOT).lootTable().identifier().getPath(),CustomData.of(data));
    }
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var initial=new HashSet<UUID>();for(var entity:f.level.getAllEntities())initial.add(entity.getUUID());
            var stats=new LinkedHashMap<net.minecraft.stats.Stat<?>,Integer>();
            try{
                var sources=new ArrayList<ItemStack>();
                sources.add(game.lbc().rollTable(f.level,f.home,f.owner,"lbc:infinity").stream().filter(LbcInfiniteShulker::isBase).findFirst().orElseThrow());
                sources.add(f.loot("lbc:infinity_upgraded_only_shulker"));sources.add(f.loot("lbc:infinity_upgraded_only_shulker_luck"));
                for(int index=0;index<sources.size();index++){
                    var source=sources.get(index).copy();var loot=source.get(DataComponents.CONTAINER_LOOT);
                    source.set(DataComponents.CONTAINER_LOOT,new SeededContainerLoot(loot.lootTable(),9157L+index));
                    require(LbcTmtBlockUses.canonical(source)==source.getItem(),"exact authored native flag/table admission");
                    require(LbcTmtBlockUses.canonical(source.getItem().getDefaultInstance())==null,"ordinary vanilla shulker is not an extra ability");
                    var wrong=source.copy();wrong.set(DataComponents.CONTAINER_LOOT,new SeededContainerLoot(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("minecraft:chests/simple_dungeon")),1));
                    require(LbcTmtBlockUses.canonical(wrong)==null,"authored flag with wrong table is rejected");
                    var ability=property(game,source);var other=new LbcTmtAbilities.Ability("test/independent",CustomData.of(new net.minecraft.nbt.CompoundTag()));
                    var stack=LbcTmtItems.prototype(2);stack.setCount(2);var root=stack.get(DataComponents.CUSTOM_DATA).copyTag();
                    root.putInt("tmt_texture_seed",0x314159+index);LbcTmtAbilities.write(root,List.of(ability,other));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
                    stack.set(DataComponents.CUSTOM_NAME,Component.literal("durable full TMT shulker "+index));
                    var outerPotion=new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.of(157319),List.of(),Optional.empty());stack.set(DataComponents.POTION_CONTENTS,outerPotion);
                    var expected=stack.copyWithCount(1);f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);var off=new ItemStack(Items.STICK,7);f.owner.setItemInHand(InteractionHand.OFF_HAND,off);
                    var used=net.minecraft.stats.Stats.ITEM_USED.get(stack.getItem());stats.putIfAbsent(used,f.owner.getStats().getValue(used));
                    var floor=BlockPos.containing(f.home.add(index-1,-1,3));var pos=floor.above();
                    f.set(floor,Blocks.STONE.defaultBlockState());f.set(pos,Blocks.AIR.defaultBlockState());f.set(pos.above(),Blocks.AIR.defaultBlockState());
                    var hit=new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,floor,false);game.items().tmt().forget(f.owner);
                    boolean mayBuild=f.owner.getAbilities().mayBuild;
                    try{f.owner.getAbilities().mayBuild=false;
                        require(!f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction()&&stack.getCount()==2&&f.level.getBlockState(pos).isAir(),"native build permission denies placement and debit");
                    }finally{f.owner.getAbilities().mayBuild=mayBuild;}
                    // Infinite shulkers retain their native container contract under both fields.
                    if(index!=1){f.field(false,f.owner.position());require(game.items().magic().suppressed(f.owner),"real positive MAG field");}
                    if(index!=0){f.field(true,f.owner.position());require(game.items().magic().mechanismSuppressedAt(f.level,f.owner.position(),20),"real positive TECH field");}
                    require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"real registered TMT shulker input remains native under MAG/TECH");
                    require(stack.getCount()==1&&ItemStack.matches(stack,expected)&&f.owner.getOffhandItem()==off&&off.getCount()==7,"one actual hand debit; full outer seed/properties/components restored");
                    require(f.level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity,"native placed shulker entity");var block=(ShulkerBoxBlockEntity)f.level.getBlockEntity(pos);
                    require(loot.lootTable().equals(block.getLootTable())&&block.getLootTableSeed()==9157L+index,"private variant table and seed reach native container");
                    var blockData=block.components().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
                    require(!blockData.contains(LbcTmtAbilities.ACTIVE)&&!blockData.contains("tmt_context"),"no active/private-view marker persisted on block");
                    f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
                    var open=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
                    require(f.owner.gameMode.useItemOn(f.owner,f.level,ItemStack.EMPTY,InteractionHand.MAIN_HAND,open).consumesAction()&&f.owner.containerMenu!=f.owner.inventoryMenu,"actual native shulker opening/menu");
                    require(block.getLootTable()==null,"native opening fills exactly the chosen table");f.owner.closeContainer();
                    var filled=block.collectComponents().get(DataComponents.CONTAINER);
                    require(filled!=null,"native generated contents retained");
                    require(f.owner.gameMode.useItemOn(f.owner,f.level,ItemStack.EMPTY,InteractionHand.MAIN_HAND,open).consumesAction(),"native repeat opening");f.owner.closeContainer();
                    require(Objects.equals(filled,block.collectComponents().get(DataComponents.CONTAINER)),"native repeat opening never rerolls filled contents");
                    try{
                        f.owner.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                        f.owner.connection.handlePickItemFromBlock(new net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket(pos,true));
                        var clone=f.owner.getMainHandItem();require(LbcTmtItems.matches(clone)&&clone.getItem()==expected.getItem()&&!clone.get(DataComponents.CUSTOM_DATA).copyTag().contains("tmt_placed_source"),"native data-inclusive creative pick restores clean full carrier after vanilla clone component copy");
                    }finally{f.owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);f.owner.getInventory().clearContent();}
                    var mined=net.minecraft.stats.Stats.BLOCK_MINED.get(f.level.getBlockState(pos).getBlock());stats.putIfAbsent(mined,f.owner.getStats().getValue(mined));
                    var before=new HashSet<UUID>();for(var entity:f.level.getAllEntities())before.add(entity.getUUID());
                    require(f.owner.gameMode.destroyBlock(pos),"actual survival native destruction");
                    var drops=new ArrayList<ItemEntity>();for(var entity:f.level.getAllEntities())if(entity instanceof ItemEntity item&&!before.contains(item.getUUID())){f.own(item);drops.add(item);}
                    var drop=drops.stream().filter(item->LbcTmtItems.matches(item.getItem())).findFirst().orElseThrow();
                    var physical=drop.getItem();require(physical.getItem()==expected.getItem()&&physical.getCount()==1&&Objects.equals(physical.get(DataComponents.POTION_CONTENTS),outerPotion)&&Objects.equals(physical.get(DataComponents.CUSTOM_NAME),expected.get(DataComponents.CUSTOM_NAME)),"physical drop restores carrier/name/outer components");
                    require(ItemData.intValue(physical,"tmt_texture_seed",-1)==0x314159+index,"physical drop retains explicitly authored procedural seed");
                    var actualSlots=LbcTmtAbilities.read(physical);
                    require(actualSlots.stream().map(LbcTmtAbilities.Ability::id).toList().equals(List.of(ability.id(),other.id()))&&actualSlots.get(1).equals(other),"physical drop retains exact ordered property identities and untouched sibling payload");
                    var selected=actualSlots.getFirst();var selectedFlags=selected.data().copyTag();selectedFlags.remove(LbcTmtComponents.KEY);
                    var originalFlags=ability.data().copyTag();originalFlags.remove(LbcTmtComponents.KEY);
                    require(selectedFlags.equals(originalFlags),"selected property changes only its native container component patch");
                    var expectedPhysical=expected.copy();var expectedRoot=expected.get(DataComponents.CUSTOM_DATA).copyTag();
                    expectedRoot.put(LbcTmtAbilities.SLOTS,physical.get(DataComponents.CUSTOM_DATA).copyTag().get(LbcTmtAbilities.SLOTS).copy());
                    expectedPhysical.set(DataComponents.CUSTOM_DATA,CustomData.of(expectedRoot));
                    require(ItemStack.matches(physical,expectedPhysical),"physical drop retains every outer component and custom datum outside its intentionally updated private property");
                    var nativeAgain=LbcTmtComponents.source(game,selected);
                    require(!nativeAgain.has(DataComponents.CONTAINER_LOOT)&&Objects.equals(nativeAgain.get(DataComponents.CONTAINER),filled),"drop private ability contains native filled contents instead of a fresh loot roll");
                    require(ItemData.stringValue(physical,LbcTmtAbilities.ACTIVE,"").isEmpty()&&!physical.get(DataComponents.CUSTOM_DATA).copyTag().contains("tmt_placed_source"),"physical drop is clean of placement/context markers");
                    var encoded=ItemStack.CODEC.encodeStart(f.owner.registryAccess().createSerializationContext(NbtOps.INSTANCE),physical).getOrThrow();require(ItemStack.matches(physical,ItemStack.CODEC.parse(f.owner.registryAccess().createSerializationContext(NbtOps.INSTANCE),encoded).getOrThrow()),"durable carrier native item codec");
                    var picked=net.minecraft.stats.Stats.ITEM_PICKED_UP.get(physical.getItem());stats.putIfAbsent(picked,f.owner.getStats().getValue(picked));
                    var pickup=physical.copy();drop.setNoPickUpDelay();drop.playerTouch(f.owner);
                    boolean found=false;for(int slot=0;slot<f.owner.getInventory().getContainerSize();slot++)if(ItemStack.matches(f.owner.getInventory().getItem(slot),pickup))found=true;
                    require(drop.isRemoved()&&found,"actual physical native item pickup restores complete carrier");
                    f.owner.getInventory().clearContent();f.owner.setItemInHand(InteractionHand.MAIN_HAND,pickup.copy());game.items().tmt().forget(f.owner);
                    require(f.owner.gameMode.useItemOn(f.owner,f.level,f.owner.getMainHandItem(),InteractionHand.MAIN_HAND,hit).consumesAction(),"picked-up filled carrier re-places through actual native BlockItem");
                    var placedAgain=(ShulkerBoxBlockEntity)f.level.getBlockEntity(pos);
                    require(placedAgain!=null&&placedAgain.getLootTable()==null&&Objects.equals(placedAgain.collectComponents().get(DataComponents.CONTAINER),filled),"native filled container survives physical pickup and re-placement without loot regeneration");
                    f.set(pos,Blocks.AIR.defaultBlockState());f.owner.getInventory().clearContent();f.removeFields();
                }
            }finally{
                game.items().tmt().forget(f.owner);f.owner.closeContainer();stats.forEach((stat,value)->f.owner.getStats().setValue(f.owner,stat,value));
                for(var entity:com.google.common.collect.Lists.newArrayList(f.level.getAllEntities()))if(!initial.contains(entity.getUUID())){f.own(entity);f.discard(entity);}
            }
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-shulker (three exact authored variants, MAG/TECH-independent native permission/placement/debit/menu/fill/destruction/pickup; full carrier seed/slots/components and selected native contents/codec retained; ordinary shulker exclusion and clean durable metadata)");
    }
}
