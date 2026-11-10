package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real accepted packets, native crafting clicks and public combat/mining entry points. */
final class FeedbackInputChecks {
    private FeedbackInputChecks(){}
    private static void require(boolean condition,String why){if(!condition)throw new IllegalStateException("Feedback input: "+why);}
    static void run(GameController game){
        require(game.server().getPort()==25577,"isolated fixture only");
        commands(game);
        try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeMeleeChecks.NativeStats(f.owner)){
            f.prepareParticipants();
            try{
                piston(f);mining(f);craft(f);lingering(f);rockets(f);shield(f);freeze(f);slabs(f);clock(f);statuses(f);
            }finally{
                game.items().legacyEffects().personalStasis().clear(f.owner);
                game.items().frost().clear(f.owner);
                game.items().resetRocket(f.owner);
                game.items().radar().stop();
                game.items().soulless().clear(f.owner);game.items().kiuas().clearCurse(f.owner);
                f.owner.inventoryMenu.getCraftSlots().clearContent();f.owner.inventoryMenu.setCarried(ItemStack.EMPTY);
            }
        }
        SkyWars2.LOGGER.info("[check] PASS feedback-input (single command root; native tool tags, exact-stack crafting, jump edges/held key, offhand shield, composed freezing, shared slab attributes and projected clock model)");
    }
    private static void commands(GameController game){
        var root=game.server().getCommands().getDispatcher().getRoot();var sw=root.getChild("skywars");require(sw!=null,"public skywars root");
        for(String name:List.of("gallery","developer","mapsave","mapdelete","settings","tps")){
            require(root.getChild(name)==null,"no unprefixed mod alias "+name);
            if(!name.equals("mapdelete"))require(sw.getChild(name)!=null,"namespaced leaf "+name);
        }
        require(root.getChild("sw")==null,"old sw alias removed");
        require(root.getChild("gamemode")!=null&&root.getChild("enchant")!=null,"vanilla roots retained");
        require(SkyWarsFeedbackCommands.effectIds().contains(LbcStaticCharge.COUNTER),"tranquillizer is testable");
        for(String id:List.of("lbc:ice_wand","lbc:ported/ice_wand","lbc:shock_potion","lbc:unstable_reactive","lbc:ported/shock_potion","lbc:ported/unstable_reactive","lbc:ported/electrostatic_potion"))
            require(LootTestCatalog.isSuggested(Identifier.parse(id)),"ready loot autocomplete "+id);
        for(var row:Map.of("lbc:ported/unstable_reactive",104,"lbc:ported/electrostatic_potion",126).entrySet()){
            var loot=game.lbc().rollTable(game.maps().level(),game.maps().lobbyPosition(),null,row.getKey());
            require(!loot.isEmpty()&&loot.stream().anyMatch(stack->{var contents=stack.get(DataComponents.POTION_CONTENTS);if(contents==null)return false;for(var e:contents.getAllEffects())if(e.getAmplifier()==row.getValue())return true;return false;}),"public potion alias actually resolves its complete effect "+row.getKey());
        }
    }
    private static void piston(WoolBucketChecks.Fixture f){
        var p=f.owner;f.move(p,f.home.add(0,20,0));p.setYRot(0);p.setXRot(0);
        var block=BlockPos.containing(p.getEyePosition().add(0,0,2));var field=p.blockPosition();f.remember(field);
        for(var tag:List.of("stopper_magic","stopper")){
            f.set(block,Blocks.STONE.defaultBlockState());f.set(block.south(),Blocks.AIR.defaultBlockState());
            p.setItemInHand(InteractionHand.MAIN_HAND,f.leaf("piston_sword"));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            var marker=new Marker(EntityTypes.MARKER,f.level);marker.setPos(p.position());marker.entityTags().add(tag);
            require(f.level.addFreshEntity(marker),"native Piston field");f.game.items().magic().loaded(marker);
            try{
                long before=f.game.items().pendingCombat().pistonWrites();
                p.gameMode.useItem(p,f.level,p.getMainHandItem(),InteractionHand.MAIN_HAND);
                require((f.game.items().pendingCombat().pistonWrites()>before)==tag.equals("stopper_magic"),"native Piston input ignores MAG and obeys TECH: "+tag);
            }finally{f.game.items().magic().unloaded(marker);marker.discard();f.set(field,Blocks.AIR.defaultBlockState());f.game.items().magic().blockChanged(field,f.level.getBlockState(field));}
        }
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    }
    private static void mining(WoolBucketChecks.Fixture f){
        var at=f.owner.blockPosition().above(5);var nativeState=Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState();
        for(var item:List.of(Items.AIR,Items.WOODEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.IRON_AXE)){
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,item==Items.AIR?ItemStack.EMPTY:new ItemStack(item));
            f.set(at,nativeState);float rate=nativeState.getDestroyProgress(f.owner,f.level,at);
            for(var block:List.of(LbcMagic.MAGIC_BEACON_BLOCK,LbcMagic.MECHANISM_BEACON_BLOCK)){
                f.set(at,block.defaultBlockState());float actual=f.level.getBlockState(at).getDestroyProgress(f.owner,f.level,at);
                require(Float.compare(rate,actual)==0,"jammer mining equals lightning rod for "+item+": "+actual+" / "+rate);
            }
        }
        f.set(at,Blocks.AIR.defaultBlockState());
        var source=f.owner.damageSources().playerAttack(f.other);
        for(float armor:new float[]{-20,-19.5f,-1,0})for(float toughness:new float[]{-20,-1,0}){
            float actual=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(f.owner,10,source,armor,toughness);
            float expected=net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(f.owner,10,source,0,0);
            require(Float.compare(actual,expected)==0,"negative defense buffer "+armor+" / "+toughness);
        }
    }
    private static void craft(WoolBucketChecks.Fixture f){
        var player=f.owner;var menu=player.inventoryMenu;require(player.containerMenu==menu,"native inventory menu");
        var grid=menu.getCraftSlots();grid.clearContent();menu.setCarried(ItemStack.EMPTY);
        var donor=f.nested(new ItemStack(Items.DIAMOND,13),91);var original=donor.copy();
        grid.setItem(0,donor);grid.setItem(3,f.loot("lbc:item_defs/lbc/imitator__440c8c4567cc").copyWithCount(2));
        require(ItemStack.matches(menu.getResultSlot().getItem(),original),"preview is a full exact component copy");
        GameChecks.inventoryClick(player,0,0,ContainerInput.PICKUP);
        require(ItemStack.matches(menu.getCarried(),original)&&ItemStack.matches(grid.getItem(0),original),"pickup preserves donor and duplicates its whole stack");
        require(grid.getItem(3).getCount()==1,"one imitator consumed");menu.setCarried(ItemStack.EMPTY);
        GameChecks.inventoryClick(player,0,0,ContainerInput.QUICK_MOVE);
        require(ItemStack.matches(grid.getItem(0),original)&&grid.getItem(3).isEmpty(),"shift craft preserves donor and spends final imitator");
        int output=0;for(int slot=0;slot<36;slot++){var item=player.getInventory().getItem(slot);if(ItemStack.isSameItemSameComponents(item,original))output+=item.getCount();}
        require(output==13,"one complete stack delivered by shift craft");grid.clearContent();player.getInventory().clearContent();
        var table=player.blockPosition().offset(1,0,0);f.set(table,Blocks.CRAFTING_TABLE.defaultBlockState());
        player.openMenu(Blocks.CRAFTING_TABLE.defaultBlockState().getMenuProvider(f.level,table));
        require(player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu,"actual workbench menu opened");
        var work=(net.minecraft.world.inventory.CraftingMenu)player.containerMenu;var large=work.getInputGridSlots();
        try{
            large.get(0).set(original.copy());large.get(8).set(f.loot("lbc:item_defs/lbc/imitator__440c8c4567cc").copyWithCount(3));
            for(int slot=0;slot<36;slot++)player.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));
            GameChecks.inventoryClick(player,0,0,ContainerInput.QUICK_MOVE);
            require(ItemStack.matches(large.get(0).getItem(),original)&&large.get(8).getItem().getCount()==3,"full inventory shift craft consumes neither donor nor imitator");
            player.getInventory().clearContent();GameChecks.inventoryClick(player,0,0,ContainerInput.QUICK_MOVE);
            require(ItemStack.matches(large.get(0).getItem(),original)&&large.get(8).getItem().isEmpty(),"workbench shift crafts exactly the available imitators");
            int copies=0;for(int slot=0;slot<36;slot++){var item=player.getInventory().getItem(slot);if(ItemStack.isSameItemSameComponents(item,original))copies+=item.getCount();}
            require(copies==39,"three complete exact stacks delivered without consuming donor");
        }finally{large.forEach(slot->slot.set(ItemStack.EMPTY));work.setCarried(ItemStack.EMPTY);player.getInventory().clearContent();player.closeContainer();}
    }
    private static void input(ServerPlayer player,boolean jump){player.connection.handlePlayerInput(new ServerboundPlayerInputPacket(new Input(false,false,false,false,jump,false,false)));}
    private static void rockets(WoolBucketChecks.Fixture f){
        var p=f.owner;var boots=new ItemStack(LbcItems.ROCKET_BOOTS);p.setOnGround(false);p.setXRot(-45);p.setItemSlot(EquipmentSlot.FEET,boots);
        require(!boots.has(DataComponents.GLIDER)&&f.game.items().rocketJumps(p)==3,"three charges without glider");
        input(p,false);input(p,true);require(f.game.items().rocketJumps(p)==2&&p.getDeltaMovement().lengthSqr()>0,"accepted airborne jump press boosts");
        for(int i=0;i<8;i++)input(p,true);require(f.game.items().rocketJumps(p)==2,"held jump never spends additional charges");
        input(p,false);input(p,true);require(f.game.items().rocketJumps(p)==1,"second rising edge boosts");
        p.setOnGround(true);input(p,false);input(p,true);require(f.game.items().rocketJumps(p)==1,"ground jump does not consume an air boost");
        p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);p.setDeltaMovement(Vec3.ZERO);input(p,false);
    }
    private static void shield(WoolBucketChecks.Fixture f){
        var p=f.owner;var wand=f.loot("lbc:big_bob");p.setItemInHand(InteractionHand.MAIN_HAND,wand);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
        p.stopUsingItem();var use=p.gameMode.useItem(p,f.level,wand,InteractionHand.MAIN_HAND);
        require(!(use instanceof net.minecraft.world.InteractionResult.Success success)||!LbcUseInteractions.quietUse(p,wand)||!success.shouldSwing(),"passive carrot carrier does not request a use swing");
        require(p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.OFF_HAND,"one-shot main item permits offhand shield");p.stopUsingItem();
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));p.getFoodData().setFoodLevel(0);
        p.gameMode.useItem(p,f.level,p.getMainHandItem(),InteractionHand.MAIN_HAND);
        require(p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.MAIN_HAND,"continuous native main use retains priority");p.stopUsingItem();p.getInventory().clearContent();
    }
    private static void lingering(WoolBucketChecks.Fixture f){
        var p=f.owner;var table=p.blockPosition().below();f.set(table,Blocks.CRAFTING_TABLE.defaultBlockState());
        p.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inv,pl)->new net.minecraft.world.inventory.CraftingMenu(id,inv,
                net.minecraft.world.inventory.ContainerLevelAccess.create(f.level,table)),Component.literal("Lingering arrow fixture")));
        var menu=(net.minecraft.world.inventory.CraftingMenu)p.containerMenu;var grid=menu.getInputGridSlots();int cases=0;
        try{
            for(var item:net.minecraft.core.registries.BuiltInRegistries.ITEM){
                if(!(item instanceof LingeringPotionItem))continue;cases++;
                grid.forEach(slot->slot.setByPlayer(ItemStack.EMPTY));menu.setCarried(ItemStack.EMPTY);
                var potion=new ItemStack(item,2);
                var effects=new net.minecraft.world.item.alchemy.PotionContents(Optional.empty(),Optional.of(0xabc123),
                        List.of(ItemEffectLevels.create(net.minecraft.world.effect.MobEffects.RAID_OMEN,80,104)),Optional.empty());
                potion.set(DataComponents.POTION_CONTENTS,effects);
                for(int slot=0;slot<9;slot++)grid.get(slot).setByPlayer(slot==4?potion:new ItemStack(Items.ARROW,2));
                var result=menu.getResultSlot().getItem();
                require(result.is(Items.TIPPED_ARROW)&&result.getCount()==8&&effects.equals(result.get(DataComponents.POTION_CONTENTS)),
                        "native recipe preview retains every potion effect/color for "+item);
                GameChecks.inventoryClick(p,0,0,ContainerInput.PICKUP);
                require(menu.getCarried().is(Items.TIPPED_ARROW)&&menu.getCarried().getCount()==8
                        &&effects.equals(menu.getCarried().get(DataComponents.POTION_CONTENTS)),"native result pickup retains payload");
                for(int slot=0;slot<9;slot++)require(grid.get(slot).getItem().getCount()==1,"one potion and one arrow from each of eight slots consumed");
                menu.setCarried(ItemStack.EMPTY);grid.get(0).setByPlayer(ItemStack.EMPTY);
                require(menu.getResultSlot().getItem().isEmpty(),"incomplete arrow ring is rejected");
            }
            require(cases>=6,"all registered native lingering potion families covered");
        }finally{grid.forEach(slot->slot.setByPlayer(ItemStack.EMPTY));menu.setCarried(ItemStack.EMPTY);p.closeContainer();}
    }
    private static void freeze(WoolBucketChecks.Fixture f){
        var p=f.owner;var gravity=p.getAttribute(Attributes.GRAVITY);gravity.setBaseValue(.043);p.setNoGravity(false);p.setDeltaMovement(1,-3,2);
        var stasis=f.game.items().legacyEffects().personalStasis();stasis.start(p,-1);
        require(gravity.getValue()==0&&p.getDeltaMovement().equals(Vec3.ZERO),"stasis immediately sets zero gravity and motion");
        p.knockback(5,1,1,p.damageSources().playerAttack(f.other),1);p.setDeltaMovement(new Vec3(2,-100,4));
        require(p.getDeltaMovement().equals(Vec3.ZERO),"frozen knockback/acceleration cannot accumulate");
        f.pause();f.game.items().timeStop().prepareFrozen(p);stasis.clear(p);
        require(gravity.getValue()==0,"world stop retains its gravity lease after stasis clears");f.thaw();
        require(Math.abs(gravity.getValue()-.043)<1e-12&&!p.isNoGravity(),"last lease restores the original gravity and no-gravity flag");
        p.setDeltaMovement(0,-.2,0);for(int i=0;i<10;i++)f.game.items().frost().add(p,f.other);
        require(f.game.items().frost().isFrozen(p)&&gravity.getValue()==0&&p.getDeltaMovement().equals(Vec3.ZERO),"real cold producer freezes motion");
        f.game.items().frost().clear(p);require(Math.abs(gravity.getValue()-.043)<1e-12,"thaw restores gravity");p.removeAllEffects();
    }
    private static void slabs(WoolBucketChecks.Fixture f){
        var normal=f.loot("lbc:item_defs/minecraft/cherry_slab__2878c1e9434d");var imba=f.loot("lbc:item_defs/minecraft/cherry_slab__5e3ad2108725");
        var a=normal.get(DataComponents.ATTRIBUTE_MODIFIERS);var b=imba.get(DataComponents.ATTRIBUTE_MODIFIERS);
        require(a!=null&&b!=null&&a.modifiers().size()==1&&b.modifiers().size()==1,"both native slabs carry the source attribute");
        double first=a.modifiers().getFirst().modifier().amount(),second=b.modifiers().getFirst().modifier().amount();
        require(Math.abs(second-first*100)<=Math.max(1e-4,Math.abs(second)*1e-6),"source1s shared value, exact normal/IMBA scaling");
    }
    private static void clock(WoolBucketChecks.Fixture f){
        var stack=f.loot("lbc:clocks");var context=f.owner.connection.getPacketContext();
        var wire=eu.pb4.polymer.core.api.item.PolymerItemUtils.getPolymerItemStack(stack,TooltipFlag.NORMAL,context,f.owner.registryAccess());
        require(Identifier.parse("lbc:time_clock").equals(stack.get(DataComponents.ITEM_MODEL))
                &&Identifier.parse("lbc:time_clock").equals(wire.get(DataComponents.ITEM_MODEL)),"actual clock wire model equals the legacy source model");
    }
    private static void statuses(WoolBucketChecks.Fixture f){
        for(var id:List.of("effect_dimshifting","effect_rtp_4","effect_ghosting","effect_kiuaskivi","cold"))require(SkyWarsFeedbackCommands.effectIds().contains(id),"testable native consumer "+id);
        SkyWarsFeedbackCommands.apply(f.game,f.owner,"cold",40);
        require(f.game.items().frost().cold(f.owner)==2,"cold command enters native seconds-based decay state");
        SkyWarsFeedbackCommands.apply(f.game,f.owner,"cold",20);
        require(f.game.items().frost().cold(f.owner)==1,"reapplying cold changes actual existing state, not only compatibility score");
        f.game.items().frost().second();
        require(f.game.items().frost().cold(f.owner)==0,"native cold expiry follows command grant");
        f.game.items().frost().clear(f.owner);
        SkyWarsFeedbackCommands.apply(f.game,f.owner,"effect_kiuaskivi",40);
        require(f.game.items().mana().hudValue(f.owner,"effect_kiuaskivi")==40,"Kiuaskivi command uses source ticks, not seconds");f.game.items().kiuas().clearCurse(f.owner);
        SkyWarsFeedbackCommands.apply(f.game,f.owner,"effect_ghosting",45);
        require(f.game.items().soulless().ghostCount()==1&&f.game.items().mana().hudValue(f.owner,"effect_ghosting")==3,"ghost command enters actual consumer and rounds remaining seconds up");
        // Explicit engine frames test its existing20-frame debit, not elapsed wall time.
        for(int frame=0;frame<20;frame++)f.game.items().soulless().tick();
        require(f.game.items().mana().hudValue(f.owner,"effect_ghosting")==2,"native ghost consumer advances after command grant");
        f.game.items().soulless().clear(f.owner);
    }
}
