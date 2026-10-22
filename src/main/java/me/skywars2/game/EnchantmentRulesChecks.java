package me.skywars2.game;

import me.skywars2.SkyWars2;
import me.skywars2.mixin.EnchantmentRulesMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
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

/** Survival native menus retain their ordinary XP, input consumption and level limits. */
public final class EnchantmentRulesChecks {
    private EnchantmentRulesChecks() { }
    private static void require(boolean condition,String message) {
        if(!condition)throw new IllegalStateException("Enchantment rules: "+message);
    }
    private static Holder<Enchantment> holder(ServerPlayer player,ResourceKey<Enchantment> key) {
        return player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }
    private static ItemStack book(ServerPlayer player,List<ResourceKey<Enchantment>> keys,int level) {
        var result=new ItemStack(Items.ENCHANTED_BOOK);
        var enchantments=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for(var key:keys)enchantments.set(holder(player,key),level);
        result.set(DataComponents.STORED_ENCHANTMENTS,enchantments.toImmutable());return result;
    }
    private static int level(ServerPlayer player,ItemStack item,ResourceKey<Enchantment> key) {
        return EnchantmentHelper.getEnchantmentsForCrafting(item).getLevel(holder(player,key));
    }
    private static ItemStack combine(ServerPlayer player,BlockPos pos,ItemStack input,ItemStack addition) {
        var menu=new AnvilMenu(42,player.getInventory(),ContainerLevelAccess.create(player.level(),pos));
        menu.getSlot(0).set(input.copy());menu.getSlot(1).set(addition.copy());menu.createResult();
        var output=menu.getSlot(2).getItem().copy();int cost=menu.getCost(),before=player.experienceLevel;
        require(!output.isEmpty() && cost>0 && cost<=10 && menu.getSlot(2).mayPickup(player),
                "Survival native anvil result/pickup; input="+input+" addition="+addition+" output="+output+" cost="+cost);
        menu.getSlot(2).onTake(player,output);
        require(player.experienceLevel==before-cost && menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(),
                "native take spends exact XP cost and consumes both inputs");return output;
    }
    public static void run(GameController game) {
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(game.playing() && player!=null && game.allowDamage(player),"actual live match participant");
        var state=new GameChecks.WandThirdPlayerState(player,game.items().mana());var level=player.level();
        int seed=player.getEnchantmentSeed();var enchantStat=net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.ENCHANT_ITEM);int originalEnchantStat=player.getStats().getValue(enchantStat);var center=new BlockPos(player.blockPosition().getX(),270,player.blockPosition().getZ());
        var saved=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();Cow cow=null;
        try {
            for(var p:BlockPos.betweenClosed(center.offset(-2,-1,-2),center.offset(2,2,2))) {
                var pos=p.immutable();level.getChunkAt(pos);saved.put(pos,game.maps().snapshot(pos));
                if(level.getBlockEntity(pos) instanceof net.minecraft.world.Container container)container.clearContent();
                level.setBlock(pos,Blocks.AIR.defaultBlockState(),818);
            }
            player.closeContainer();player.getInventory().clearContent();player.setGameMode(GameType.SURVIVAL);
            player.setNoGravity(true);player.teleportTo(level,center.getX()+.5,270,center.getZ()+1.5,Set.of(),0,0,true);
            player.setExperienceLevels(1000);level.setBlock(center,Blocks.ANVIL.defaultBlockState(),818);
            var sharp=holder(player,Enchantments.SHARPNESS);var smite=holder(player,Enchantments.SMITE);
            require(Enchantment.areCompatible(sharp,smite) && Enchantment.areCompatible(smite,sharp)
                    && !Enchantment.areCompatible(sharp,sharp),"global distinct compatibility and same-identity exclusion");
            var dirt=combine(player,center,new ItemStack(Items.DIRT),book(player,List.of(Enchantments.SHARPNESS),5));
            require(dirt.is(Items.DIRT) && level(player,dirt,Enchantments.SHARPNESS)==5,"Survival Sharpness5 book applies to dirt");
            cow=new Cow(EntityTypes.COW,level);cow.setPos(center.getX()+.5,271,center.getZ()+.5);cow.setNoAi(true);cow.setNoGravity(true);
            require(level.addFreshEntity(cow),"native damage target");
            float ordinary=EnchantmentHelper.modifyDamage(level,new ItemStack(Items.DIRT),cow,player.damageSources().playerAttack(player),1);
            float enchanted=EnchantmentHelper.modifyDamage(level,dirt,cow,player.damageSources().playerAttack(player),1);
            require(ordinary==1 && enchanted==4,"native Sharpness damage effect executes on arbitrary enchanted item");
            var paper=new ItemStack(Items.PAPER);paper.remove(DataComponents.ENCHANTMENTS);
            var enchantedPaper=combine(player,center,paper,book(player,List.of(Enchantments.UNBREAKING),1));
            require(!paper.has(DataComponents.ENCHANTMENTS) && level(player,enchantedPaper,Enchantments.UNBREAKING)==1,
                    "anvil book supports a custom input with explicitly removed enchantment component");
            var protections=List.of(Enchantments.PROTECTION,Enchantments.FIRE_PROTECTION,Enchantments.BLAST_PROTECTION,Enchantments.PROJECTILE_PROTECTION);
            var armor=combine(player,center,new ItemStack(Items.DIAMOND_CHESTPLATE),book(player,protections,4));
            require(protections.stream().allMatch(key->level(player,armor,key)==4),"all four previously conflicting protection types coexist on armor");
            var merged=combine(player,center,book(player,List.of(Enchantments.SHARPNESS),1),book(player,List.of(Enchantments.SMITE),1));
            require(level(player,merged,Enchantments.SHARPNESS)==1 && level(player,merged,Enchantments.SMITE)==1,
                    "native stored-book merge keeps formerly incompatible enchantments");
            var upgraded=combine(player,center,book(player,List.of(Enchantments.SHARPNESS),4),book(player,List.of(Enchantments.SHARPNESS),4));
            require(level(player,upgraded,Enchantments.SHARPNESS)==5,"same enchantment still uses native level upgrade");
            var capped=combine(player,center,book(player,List.of(Enchantments.SHARPNESS),5),book(player,List.of(Enchantments.SHARPNESS),5));
            require(level(player,capped,Enchantments.SHARPNESS)==6,"anvil quarter-weaker merge exceeds native maximum");
            var ordinaryAddition=new ItemStack(Items.DIAMOND_AXE);ordinaryAddition.enchant(sharp,1);
            var denied=new AnvilMenu(43,player.getInventory(),ContainerLevelAccess.create(level,center));
            denied.getSlot(0).set(new ItemStack(Items.DIRT));denied.getSlot(1).set(ordinaryAddition);denied.createResult();
            require(denied.getSlot(2).getItem().isEmpty(),"ordinary non-book addition preserves native material restrictions");
            var costly=new ItemStack(Items.DIRT);costly.set(DataComponents.REPAIR_COST,40);
            denied.getSlot(0).set(costly);denied.getSlot(1).set(book(player,List.of(Enchantments.SHARPNESS),1));denied.createResult();
            require(denied.getCost()==10 && !denied.getSlot(2).getItem().isEmpty() && denied.getSlot(2).mayPickup(player),"Survival prior work taxes cap at ten without too-expensive rejection");

            // This is the actual table menu path, including bookshelf calculation, selection, lapis and XP debit.
            for(var p:saved.keySet())level.setBlock(p,Blocks.AIR.defaultBlockState(),818);
            level.setBlock(center,Blocks.ENCHANTING_TABLE.defaultBlockState(),818);
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Math.abs(x)==2 || Math.abs(z)==2)
                for(int y=0;y<=1;y++)level.setBlock(center.offset(x,y,z),Blocks.BOOKSHELF.defaultBlockState(),818);
            var table=new EnchantmentMenu(44,player.getInventory(),ContainerLevelAccess.create(level,center));
            boolean mixed=false;
            for(int trial=1;trial<=128;trial++) {
                table.setData(3,trial*0x9E3779B9);table.getSlot(0).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
                table.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI,3));int before=player.experienceLevel;
                require(table.costs[2]==30 && table.clickMenuButton(player,2),"real30-level native table option with resident bookshelves");
                var result=table.getSlot(0).getItem();var enchants=EnchantmentHelper.getEnchantmentsForCrafting(result);
                require(!enchants.isEmpty() && player.experienceLevel==before-3 && table.getSlot(1).getItem().isEmpty(),
                        "native table result spends three levels and lapis");
                long count=protections.stream().filter(key->level(player,result,key)>0).count();mixed|=count>=2;
                var identities=new HashSet<Holder<Enchantment>>();for(var entry:enchants.entrySet())
                    require(identities.add(entry.getKey()),"native table never repeats same enchantment identity");
            }
            require(mixed,"actual native table selection chains retain previously incompatible protection combinations");
        } finally {
            if(cow!=null)cow.discard();
            for(var entry:saved.entrySet()) {
                if(level.getBlockEntity(entry.getKey()) instanceof net.minecraft.world.Container container)container.clearContent();
                game.maps().restoreSnapshot(entry.getKey(),entry.getValue());
            }
            state.restore(game);((EnchantmentRulesMixin.PlayerSeedAccess)player).skywars2$setEnchantmentSeed(seed);player.getStats().setValue(player,enchantStat,originalEnchantStat);
            game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();
        }
        SkyWars2.LOGGER.info("[check] PASS enchantment-rules");
    }
}
