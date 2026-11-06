package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Real menu transactions, weighted native loot and mob allegiance/attribution checks. */
public final class OrdinaryToolChecks {
    private OrdinaryToolChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Ordinary tools: "+why);}
    private static ItemStack loot(AllGrimoireChecks.Fixture f,String name){var rows=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:ported/"+name);require(rows.size()==1,"one leaf "+name);return rows.getFirst();}
    private static int enchant(AllGrimoireChecks.Fixture f,ItemStack item,net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key){return EnchantmentHelper.getItemEnchantmentLevel(f.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key),item);}
    private static void presentation(AllGrimoireChecks.Fixture f){
        for(var row:List.of("random_enchanted_axe","tomahawk","poleaxe","hammer","op_hoe","sickle","random_enchanted_pickaxe","silk_pickaxe")){
            var s=loot(f,row);require(s.getCount()==1&&s.get(DataComponents.CUSTOM_NAME)!=null&&s.get(DataComponents.LORE)!=null&&s.get(DataComponents.TOOLTIP_STYLE)!=null,"authored tool presentation "+row);
            require(LbcFartus.data(s).getIntOr("transformable",0)==1,"Reforge flag "+row);
            require(s.get(DataComponents.TOOL)!=null&&s.getMaxDamage()>0,"native mining tool/durability "+row);
        }
        var silk=loot(f,"silk_pickaxe");require(silk.is(LbcNativeToolItems.Kind.SILK_PICKAXE.item)&&enchant(f,silk,Enchantments.SILK_TOUCH)==10,"source diamond SilkX");
        var op=loot(f,"op_hoe");require(op.is(LbcNativeToolItems.Kind.OP_HOE.item)&&enchant(f,op,Enchantments.EFFICIENCY)==255&&enchant(f,op,Enchantments.FORTUNE)==25,"source OP hoe enchantments unchanged");
        var random=loot(f,"random_enchanted_pickaxe");require(random.is(LbcNativeToolItems.Kind.RANDOM_ENCHANTED_PICKAXE.item)&&!random.getEnchantments().isEmpty(),"native enchant-with-levels still rolls actual enchants");
        for(String pool:List.of("axe","hoe","pickaxe"))for(int n=0;n<100;n++){
            var out=f.base.game.lbc().rollTable(f.level,f.at,f.base.owner,"lbc:transformables_"+pool);require(out.size()==1&&LbcFartus.data(out.getFirst()).getIntOr(pool,0)==1,"complete weighted "+pool);
        }
        var crafted=loot(f,"communism");var defaults=LbcCommunism.ITEM.getDefaultInstance();for(var key:List.of(DataComponents.CUSTOM_NAME,DataComponents.LORE,DataComponents.ITEM_MODEL,DataComponents.CUSTOM_DATA,DataComponents.TOOLTIP_STYLE))require(Objects.equals(crafted.get(key),defaults.get(key)),"recipe/command presentation equality "+key);
    }
    private static void recipes(AllGrimoireChecks.Fixture f){var p=f.base.owner;
        for(boolean bench:new boolean[]{false,true}){
            p.closeContainer();p.getInventory().clearContent();if(bench){var pos=BlockPos.containing(f.at).offset(-6,0,-6);f.level.setBlock(pos,Blocks.CRAFTING_TABLE.defaultBlockState(),2);p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,pl)->new net.minecraft.world.inventory.CraftingMenu(id,inv,net.minecraft.world.inventory.ContainerLevelAccess.create(f.level,pos)),net.minecraft.network.chat.Component.literal("Tools fixture")));}
            var menu=(AbstractCraftingMenu)p.containerMenu;var slots=menu.getInputGridSlots();slots.getFirst().setByPlayer(loot(f,"hammer").copyWithCount(2));slots.getLast().setByPlayer(loot(f,"sickle").copyWithCount(2));
            require(menu.getResultSlot().getItem().is(LbcCommunism.ITEM),"exact native shapeless preview "+bench);
            for(int n=0;n<20;n++)require(menu.getResultSlot().getItem().is(LbcCommunism.ITEM),"stable preview");
            require(slots.getFirst().getItem().getCount()==2,"preview retains ingredients");
            slots.get(1).setByPlayer(new ItemStack(Items.STICK));require(menu.getResultSlot().getItem().isEmpty(),"extra ingredient rejects");slots.get(1).setByPlayer(ItemStack.EMPTY);
            for(int n=0;n<36;n++)p.getInventory().setItem(n,new ItemStack(Items.COBBLESTONE,64));GameChecks.inventoryClick(p,0,0,ContainerInput.QUICK_MOVE);require(slots.getFirst().getItem().getCount()==2,"full inventory cannot consume inputs");
            p.getInventory().clearContent();GameChecks.inventoryClick(p,0,0,ContainerInput.PICKUP);require(menu.getCarried().is(LbcCommunism.ITEM)&&slots.getFirst().getItem().getCount()==1&&slots.getLast().getItem().getCount()==1,"take commits one of each donor "+bench);menu.setCarried(ItemStack.EMPTY);
            GameChecks.inventoryClick(p,0,0,ContainerInput.QUICK_MOVE);int total=0;for(int n=0;n<36;n++)if(p.getInventory().getItem(n).is(LbcCommunism.ITEM))total+=p.getInventory().getItem(n).getCount();require(total==1&&slots.getFirst().getItem().isEmpty()&&slots.getLast().getItem().isEmpty(),"shift take only remaining recipe");p.closeContainer();p.getInventory().clearContent();
        }
    }
    private static Skeleton mob(AllGrimoireChecks.Fixture f,double z){var m=new Skeleton(EntityTypes.SKELETON,f.level);m.setNoAi(true);m.setNoGravity(true);m.setPos(f.at.add(.5,1,z));require(f.level.addFreshEntity(m),"spawn native target");return m;}
    private static void cast(AllGrimoireChecks.Fixture f,InteractionHand hand){f.base.owner.setItemInHand(hand,loot(f,"communism"));require(f.base.input(hand),"actual native use "+hand);}
    private static void allegiance(AllGrimoireChecks.Fixture f){var g=f.base.game;var p=f.base.owner;var board=g.server().getScoreboard();var oldA=p.getTeam();var oldB=f.beta.getTeam();
        var a=board.addPlayerTeam("codex_tools_a");var b=board.addPlayerTeam("codex_tools_b");a.setAllowFriendlyFire(false);b.setAllowFriendlyFire(false);
        var affected=new ArrayList<Entity>();
        try{board.addPlayerToTeam(p.getScoreboardName(),a);board.addPlayerToTeam(f.beta.getScoreboardName(),b);
            f.beta.setPos(f.at.add(12,4,0));p.setYRot(0);p.setXRot(0);p.getInventory().clearContent();f.base.mana.set(p,-100);
            // Keep the whole tested lane resident/air, with a source-style terminal at eight blocks.
            for(int z=0;z<=10;z++)f.level.setBlock(BlockPos.containing(f.at.add(0,p.getEyeHeight(),z)),Blocks.AIR.defaultBlockState(),2|16|32);
            f.level.setBlock(BlockPos.containing(f.at.add(0,p.getEyeHeight(),8)),Blocks.STONE.defaultBlockState(),2|16|32);
            f.level.setBlock(BlockPos.containing(f.at.add(0,p.getEyeHeight(),4)),Blocks.DANDELION.defaultBlockState(),2|16|32);
            var first=mob(f,2);var second=mob(f,5);var behind=mob(f,10);affected.addAll(List.of(first,second,behind));board.addPlayerToTeam(first.getScoreboardName(),b);first.setTarget(f.beta);
            cast(f,InteractionHand.MAIN_HAND);require(first.getTeam()==a&&second.getTeam()==a&&behind.getTeam()==null,"all intersecting mobs until first solid block");require(first.getTarget()==null&&first.getHealth()==20&&LbcIndirectKills.playerSource(first)==p&&g.items().summons().friendly(p,first),"allegiance/AI reset/new damage attribution, no fake hit");require(f.base.mana.get(p)==-100,"no cost or mana admission");
            require(f.beta.getTeam()==b&&!f.beta.entityTags().contains("communist"),"players excluded");
            first.discard();second.discard();behind.discard();p.getInventory().clearContent();
            var guard=mob(f,2);affected.add(guard);guard.entityTags().add("strict_map_object");cast(f,InteractionHand.OFF_HAND);require(g.items().communism().owner(guard)==null,"service actor excluded");guard.discard();
            var stopped=mob(f,2);affected.add(stopped);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,loot(f,"communism"));p.entityTags().add("time_stopped");try{require(!f.base.input(InteractionHand.MAIN_HAND)&&g.items().communism().owner(stopped)==null,"time-stop admission");}finally{p.entityTags().remove("time_stopped");}
            p.entityTags().add("nomagic_active");try{require(!f.base.input(InteractionHand.MAIN_HAND),"antimagic admission");}finally{p.entityTags().remove("nomagic_active");}stopped.discard();
            // Native cultist maps must transfer, not keep targeting on behalf of the former owner.
            var summons=g.items().summons();var owned=summons.spawnPreset(f.beta,"luck",f.at.add(.5,1,3));require(owned!=null,"source cultist fixture");owned.setNoAi(true);owned.setNoGravity(true);owned.setPos(f.at.add(.5,1,3));affected.add(owned);
            cast(f,InteractionHand.MAIN_HAND);require(summons.owner(owned)==p&&LbcIndirectKills.playerSource(owned)==p&&summons.friendly(p,owned)&&!summons.friendly(f.beta,owned),"cultist state and damage owner transfer together");require(!summons.canMelee(owned,p)&&summons.canMelee(owned,f.beta),"new owner and allies protected by real melee guard");
            var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(EntityTypes.ARROW,f.level);arrow.setOwner(owned);arrow.setPos(owned.position());affected.add(arrow);require(f.level.addFreshEntity(arrow),"native mob arrow");require(arrow.getOwner()==p,"future projectiles inherit new owner");summons.stop();
            var converted=new Skeleton(EntityTypes.SKELETON,f.level);summons.ownerChanged(converted,p); // unknown actor must not become a managed summon
            require(!summons.snapshot().contains(converted),"owner change never invents summon state");
        }finally{g.items().summons().stop();g.items().communism().stop();for(var e:affected){board.removePlayerFromTeam(e.getScoreboardName());e.discard();}board.removePlayerFromTeam(p.getScoreboardName());board.removePlayerFromTeam(f.beta.getScoreboardName());board.removePlayerTeam(a);board.removePlayerTeam(b);if(oldA!=null)board.addPlayerToTeam(p.getScoreboardName(),oldA);if(oldB!=null)board.addPlayerToTeam(f.beta.getScoreboardName(),oldB);}
    }
    public static void run(GameController g){try(var nativeStats=new NativeMeleeChecks.NativeStats(g.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")),LbcNativeToolItems.items());var f=new AllGrimoireChecks.Fixture(g)){try{presentation(f);recipes(f);f.reset();allegiance(f);}finally{g.items().communism().stop();}}SkyWars2.LOGGER.info("[check] PASS ordinary-tools");}
}
