package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.mixin.EnchantmentRulesMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** Actual Survival anvil pickup, native storage codecs, table generation and Polymer projection. */
public final class AdvancedAnvilChecks {
    private AdvancedAnvilChecks() { }
    private static void require(boolean ok,String text){if(!ok)throw new IllegalStateException("Advanced anvil: "+text);}
    private static Holder<Enchantment> holder(ServerPlayer player,ResourceKey<Enchantment> key){return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);}
    private static ItemEnchantments enchantments(ServerPlayer player,ResourceKey<Enchantment> key,int value){var mutable=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);mutable.set(holder(player,key),value);var immutable=mutable.toImmutable();require(immutable.getLevel(holder(player,key))==value,"authoritative positive storage "+value);return immutable;}
    private static ItemStack book(ServerPlayer player,ResourceKey<Enchantment> key,int value){var book=new ItemStack(Items.ENCHANTED_BOOK);book.set(DataComponents.STORED_ENCHANTMENTS,enchantments(player,key,value));return book;}
    private static int value(ServerPlayer player,ItemStack stack,ResourceKey<Enchantment> key){return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(holder(player,key));}
    private record Taken(ItemStack output,int cost) { }
    private record Stored(ItemStack restored,net.minecraft.nbt.Tag encoded) { }
    private static Taken take(ServerPlayer player,BlockPos pos,ItemStack first,ItemStack second,int remaining){
        player.level().setBlock(pos,Blocks.ANVIL.defaultBlockState(),818);
        var menu=new AnvilMenu(62,player.getInventory(),ContainerLevelAccess.create(player.level(),pos));menu.getSlot(0).set(first.copy());menu.getSlot(1).set(second.copy());menu.createResult();
        int cost=menu.getCost();var result=menu.getSlot(2).getItem().copy();require(!result.isEmpty() && cost>=1 && cost<=10 && menu.getSlot(2).mayPickup(player),"Survival native result has no Too Expensive gate; cost="+cost+" first="+first+" second="+second+" result="+result);
        int before=player.experienceLevel;menu.getSlot(2).onTake(player,result);require(player.experienceLevel==before-cost && menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().getCount()==remaining,"native exact level debit and ordinary input/material consumption");return new Taken(result,cost);
    }
    private static void mergeCases(ServerPlayer player,BlockPos pos){
        int[][] cases={{1,1,2},{1,2,3},{2,1,3},{3,1,3},{99,99,123},{100,1,100},{255,255,318},{256,256,320},{Integer.MAX_VALUE,1,Integer.MAX_VALUE},{Integer.MAX_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE},{Integer.MAX_VALUE-1,Integer.MAX_VALUE,Integer.MAX_VALUE}};
        for(var pair:cases){var taken=take(player,pos,book(player,Enchantments.SHARPNESS,pair[0]),book(player,Enchantments.SHARPNESS,pair[1]),0);require(value(player,taken.output(),Enchantments.SHARPNESS)==pair[2],"native merge "+pair[0]+"+"+pair[1]+"="+pair[2]);}
        var curse=take(player,pos,book(player,Enchantments.BINDING_CURSE,1),book(player,Enchantments.BINDING_CURSE,1),0);require(value(player,curse.output(),Enchantments.BINDING_CURSE)==2,"native curse I+I becomes II despite registry max I");
        var untouched=take(player,pos,book(player,Enchantments.SHARPNESS,7),book(player,Enchantments.UNBREAKING,1001),0);require(value(player,untouched.output(),Enchantments.SHARPNESS)==7 && value(player,untouched.output(),Enchantments.UNBREAKING)==1001,"absent enchantment transfers unchanged and unrelated enchantment survives");
        var taxed=book(player,Enchantments.SHARPNESS,99);taxed.set(DataComponents.REPAIR_COST,Integer.MAX_VALUE);var addition=book(player,Enchantments.SHARPNESS,99);addition.set(DataComponents.REPAIR_COST,Integer.MAX_VALUE);
        player.setExperienceLevels(10);var capped=take(player,pos,taxed,addition,0);require(capped.cost()==10 && player.experienceLevel==0 && value(player,capped.output(),Enchantments.SHARPNESS)==123,"maximum prior work taxes still cost exactly10 and Survival can take at ten levels");player.setExperienceLevels(1000);
        var arbitrary=take(player,pos,new ItemStack(Items.DIRT),book(player,Enchantments.SHARPNESS,300),0);require(arbitrary.output().is(Items.DIRT) && value(player,arbitrary.output(),Enchantments.SHARPNESS)==300,"actual book on arbitrary item preserves high enchantment");
        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.set(DataComponents.ENCHANTMENTS,enchantments(player,Enchantments.SHARPNESS,99));sword.setDamageValue(500);var other=sword.copy();var merged=take(player,pos,sword,other,0);require(value(player,merged.output(),Enchantments.SHARPNESS)==123 && merged.output().getDamageValue()<500,"ordinary item/item merge retains native repair and consumption");
        var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);armor.setDamageValue(100);var repaired=take(player,pos,armor,new ItemStack(Items.DIAMOND,5),4);require(repaired.output().getDamageValue()==0,"ordinary material repair consumes exactly one diamond");
        // Empty/no-op operations remain empty and free.
        var invalid=new AnvilMenu(63,player.getInventory(),ContainerLevelAccess.create(player.level(),pos));invalid.getSlot(0).set(new ItemStack(Items.DIRT));invalid.createResult();require(invalid.getSlot(2).getItem().isEmpty() && invalid.getCost()==0,"empty addition remains no-op");
    }
    private static void clientComponents(ServerPlayer player,ItemStack projected){
        for(var key:List.of(DataComponents.ENCHANTMENTS,DataComponents.STORED_ENCHANTMENTS)){
            var component=projected.getOrDefault(key,ItemEnchantments.EMPTY);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),player.registryAccess());
            try{ItemEnchantments.STREAM_CODEC.encode(buffer,component);var decoded=ItemEnchantments.STREAM_CODEC.decode(buffer);require(decoded.entrySet().size()==component.entrySet().size(),"native wire component count");for(var entry:decoded.entrySet())require(entry.getIntValue()>0 && entry.getIntValue()<=255 && entry.getIntValue()==component.getLevel(entry.getKey()),"actual native wire enchantment component within vanilla range");}finally{buffer.release();}
        }
    }
    private static void storageAndProjection(GameController game,ServerPlayer player){
        var savedProfile=game.players().get(player.getUUID());int savedChallenge=savedProfile.lbcChallenge;
        try{savedProfile.lbcChallenge=0;game.lbc().refreshKnowledgeView(player);
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND){@Override public net.minecraft.network.PacketListener getPacketListener(){return player.connection;}};
        var provider=(net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider)connection;
        net.fabricmc.fabric.api.networking.v1.context.PacketContext.runWithContext(provider,()->{
            for(int amount:new int[]{300,1001,Integer.MAX_VALUE})for(int type=0;type<3;type++){
                var real=new ItemStack(type==0?Items.DIAMOND_SWORD:type==1?Items.ENCHANTED_BOOK:LbcSlabDropItem.Kind.NORMAL.item());
                var component=type==1?DataComponents.STORED_ENCHANTMENTS:DataComponents.ENCHANTMENTS;real.set(component,enchantments(player,Enchantments.SHARPNESS,amount));
                if(type==2)real.set(DataComponents.STORED_ENCHANTMENTS,enchantments(player,Enchantments.UNBREAKING,amount));
                // Polymer's ItemStack codec deliberately projects under an active networking
                // context. Disk persistence must run without that context, like native saves.
                var stored=net.fabricmc.fabric.api.networking.v1.context.PacketContext.supplyWithoutContext(()->{var ops=player.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);var encoded=ItemStack.CODEC.encodeStart(ops,real).getOrThrow();return new Stored(ItemStack.CODEC.parse(ops,encoded).getOrThrow(),encoded);});
                var restored=stored.restored();int loaded=restored.getOrDefault(component,ItemEnchantments.EMPTY).getLevel(holder(player,Enchantments.SHARPNESS));
                require(ItemStack.matches(real,restored) && loaded==amount,"native save/load retains authoritative >255/intmax component; type="+type+" expectedLevel="+amount+" loadedLevel="+loaded+" same="+ItemStack.matches(real,restored)+" original="+real.getComponents()+" restored="+restored.getComponents()+" encoded="+stored.encoded());
                var projected=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(real,net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),player.registryAccess());
                require(projected!=real,"high vanilla and Polymer stacks enter projection");for(var key:List.of(DataComponents.ENCHANTMENTS,DataComponents.STORED_ENCHANTMENTS))for(var entry:projected.getOrDefault(key,ItemEnchantments.EMPTY).entrySet())require(entry.getIntValue()>0 && entry.getIntValue()<=255,"client native enchantment components stay within vanilla range");
                clientComponents(player,projected);
                require(real.get(component).getLevel(holder(player,Enchantments.SHARPNESS))==amount,"projection never mutates authoritative component");
                var authoritative=eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(projected,provider.getPacketContext(),player.registryAccess());require(ItemStack.matches(real,authoritative),"Polymer metadata authoritative high-level roundtrip");
                var lore=projected.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines();require(lore.stream().anyMatch(row->row.getString().contains(Integer.toString(amount))) && lore.stream().noneMatch(row->row.getString().contains("enchantment.level."+amount)),"client lore displays truthful decimal level "+amount);
            }
            var profile=game.players().get(player.getUUID());int challenge=profile.lbcChallenge;
            try{profile.lbcChallenge=1;game.lbc().refreshKnowledgeView(player);require(game.lbc().hidesKnowledge(player),"actual hidden knowledge context");for(var item:List.of(Items.DIAMOND_SWORD,Items.ENCHANTED_BOOK,LbcSlabDropItem.Kind.NORMAL.item())){var real=new ItemStack(item);real.set(DataComponents.ENCHANTMENTS,enchantments(player,Enchantments.SHARPNESS,1001));real.set(DataComponents.STORED_ENCHANTMENTS,enchantments(player,Enchantments.UNBREAKING,1001));var projected=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(real,net.minecraft.world.item.TooltipFlag.NORMAL,provider.getPacketContext(),player.registryAccess());clientComponents(player,projected);var lore=projected.getOrDefault(DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines();require(lore.size()==1 && lore.getFirst().getString().equals("????????????????????????????") && projected.getHoverName().getString().equals("????????????????????????????"),"knowledge-hidden projection keeps source-exact question marks without enchantment disclosure");require(ItemStack.matches(real,eu.pb4.polymer.core.api.item.PolymerItemUtils.getRealItemStack(projected,provider.getPacketContext(),player.registryAccess())),"hidden viewer authoritative high-level roundtrip");}}
            finally{profile.lbcChallenge=challenge;game.lbc().refreshKnowledgeView(player);}
        });
        var mutable=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);var sharp=holder(player,Enchantments.SHARPNESS);mutable.upgrade(sharp,1001);require(mutable.toImmutable().getLevel(sharp)==1001,"native upgrade retains high value");mutable.upgrade(sharp,300);require(mutable.toImmutable().getLevel(sharp)==1001,"native upgrade keeps stronger value");mutable.set(sharp,0);require(mutable.toImmutable().getLevel(sharp)==0,"zero still removes enchantment");
        }finally{savedProfile.lbcChallenge=savedChallenge;game.lbc().refreshKnowledgeView(player);}
    }
    private static void unchangedGeneration(GameController game,ServerPlayer player,BlockPos pos,Set<BlockPos> region){
        require(holder(player,Enchantments.SHARPNESS).value().getMaxLevel()==5 && holder(player,Enchantments.UNBREAKING).value().getMaxLevel()==3 && holder(player,Enchantments.BINDING_CURSE).value().getMaxLevel()==1,"registry max levels remain vanilla; no global max-level override");
        var op=game.lbc().rollTable(player.level(),player.position(),player,"lbc:opbook_old");require(op.size()==1 && op.getFirst().is(Items.ENCHANTED_BOOK),"native old OP book remains one vanilla book");var old=EnchantmentHelper.getEnchantmentsForCrafting(op.getFirst());require(old.entrySet().size()==28 && value(player,op.getFirst(),Enchantments.SHARPNESS)==5 && value(player,op.getFirst(),Enchantments.PIERCING)==5 && value(player,op.getFirst(),Enchantments.PROTECTION)==4 && value(player,op.getFirst(),Enchantments.MENDING)==1,"old OP book retains original28-entry payload, including source Piercing5");
        for(var p:region)player.level().setBlock(p,Blocks.AIR.defaultBlockState(),818);player.level().setBlock(pos,Blocks.ENCHANTING_TABLE.defaultBlockState(),818);for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Math.abs(x)==2 || Math.abs(z)==2)for(int y=0;y<=1;y++)player.level().setBlock(pos.offset(x,y,z),Blocks.BOOKSHELF.defaultBlockState(),818);
        var menu=new EnchantmentMenu(64,player.getInventory(),ContainerLevelAccess.create(player.level(),pos));for(int seed=1;seed<=16;seed++){menu.setData(3,seed*0x9E3779B9);menu.getSlot(0).set(new ItemStack(Items.DIAMOND_CHESTPLATE));menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,3));int before=player.experienceLevel;require(menu.costs[2]==30 && menu.clickMenuButton(player,2),"actual native table offers thirty-level selection");var enchantments=EnchantmentHelper.getEnchantmentsForCrafting(menu.getSlot(0).getItem());require(!enchantments.isEmpty() && player.experienceLevel==before-3 && menu.getSlot(1).getItem().isEmpty(),"native table consumes three levels/lapis");for(var entry:enchantments.entrySet())require(entry.getIntValue()>=1 && entry.getIntValue()<=entry.getKey().value().getMaxLevel(),"native table generated level remains bounded by native definition");}
    }
    public static void run(GameController game){
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));require(game.playing() && player!=null && game.allowDamage(player),"actual match participant");var state=new GameChecks.WandThirdPlayerState(player,game.items().mana());var level=player.level();int seed=player.getEnchantmentSeed();var stat=net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.ENCHANT_ITEM);int priorStat=player.getStats().getValue(stat);var pos=new BlockPos(player.blockPosition().getX(),290,player.blockPosition().getZ());var blocks=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
        try{for(var p:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,2,2))){var at=p.immutable();level.getChunkAt(at);blocks.put(at,game.maps().snapshot(level,at));if(level.getBlockEntity(at) instanceof net.minecraft.world.Container container)container.clearContent();level.setBlock(at,Blocks.AIR.defaultBlockState(),818);}player.closeContainer();player.getInventory().clearContent();player.removeAllEffects();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()+1.5,Set.of(),0,0,true);player.setExperienceLevels(1000);
            mergeCases(player,pos);storageAndProjection(game,player);unchangedGeneration(game,player,pos,blocks.keySet());
        }finally{for(var entry:blocks.entrySet()){if(level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container container)container.clearContent();game.maps().restoreSnapshot(level,entry.getKey(),entry.getValue());}state.restore(game);((EnchantmentRulesMixin.PlayerSeedAccess)player).skywars2$setEnchantmentSeed(seed);player.getStats().setValue(player,stat,priorStat);game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();}
        SkyWars2.LOGGER.info("[check] PASS advanced-anvil");
    }
}
