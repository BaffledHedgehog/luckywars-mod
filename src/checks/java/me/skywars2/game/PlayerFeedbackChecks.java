package me.skywars2.game;

import java.util.*;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.skywars2.game.items.tmt.LbcTmtItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Exercises real observer/use/impact packets instead of only checking stored preferences. */
final class PlayerFeedbackChecks {
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Player feedback: "+why);}
    static void run(GameController game,WoolBucketChecks.Fixture f){
        var current=SguiUtils.getCurrentGui(f.owner);if(current!=null)current.close();
        checkSprint(game,f);checkTmt(game,f);checkProjectile(f);checkObserver(game,f);
        me.skywars2.SkyWars2.LOGGER.info("[check] Player feedback: live 0/5/100 sprint packets, non-consuming TMT client carrier, native arrow impacts/Punch and silent read-only observer containers/TAB passed");
    }
    private static void checkSprint(GameController game,WoolBucketChecks.Fixture f){
        var profile=game.players().get(f.owner.getUUID());int saved=profile.fovEffects;boolean sprint=f.owner.isSprinting();
        try{
            for(int percent:new int[]{0,5,100})for(boolean running:new boolean[]{false,true}){
                profile.fovEffects=percent;game.items().rifleZoom().personalSettingsChanged(f.owner);f.owner.setSprinting(running);
                int from=f.client.packets().size();
                f.owner.connection.send(new ClientboundUpdateAttributesPacket(f.owner.getId(),List.of(f.owner.getAttribute(Attributes.MOVEMENT_SPEED))));
                var packets=SmokeVolumeChecks.flatten(f.client.packets().subList(from,f.client.packets().size()));
                var abilities=packets.stream().filter(ClientboundPlayerAbilitiesPacket.class::isInstance).map(ClientboundPlayerAbilitiesPacket.class::cast).reduce((a,b)->b).orElseThrow();
                double speed=f.owner.getAttributeValue(Attributes.MOVEMENT_SPEED),nativeWalk=f.owner.getAbilities().getWalkingSpeed();
                double actual=(speed/abilities.getWalkingSpeed()+1)/2;
                double expected=1+((speed/nativeWalk+1)/2-1)*percent/100.;
                require(Math.abs(actual-expected)<1e-5,"actual sprint packet follows "+percent+"%: "+actual+" vs "+expected);
            }
        }finally{f.owner.setSprinting(sprint);profile.fovEffects=saved;game.items().rifleZoom().personalSettingsChanged(f.owner);}
    }
    private static void checkTmt(GameController game,WoolBucketChecks.Fixture f){
        var item=LbcTmtItems.prototype(2).copyWithCount(4);
        item.set(DataComponents.ITEM_MODEL,net.minecraft.resources.Identifier.parse("lbc:tmt/procedural"));
        require(((eu.pb4.polymer.core.api.item.PolymerItem)item.getItem()).getPolymerItem(item,net.fabricmc.fabric.api.networking.v1.context.PacketContext.get())==Items.STICK,
                "client cannot predict snowball consumption for a property activation");
        require(LbcTmtItems.vanillaCarrier(item)==Items.SNOWBALL,"real throw-family classification stays native");
        f.owner.setItemInHand(InteractionHand.MAIN_HAND,item);
        var before=new HashSet<UUID>();f.level.getAllEntities().forEach(e->before.add(e.getUUID()));
        require(f.owner.gameMode.useItem(f.owner,f.level,item,InteractionHand.MAIN_HAND).consumesAction()&&item.getCount()==3,
                "server-native snowball use still spends exactly one and succeeds");
        var shots=new ArrayList<Entity>();f.level.getAllEntities().forEach(e->{if(e.getType()==EntityTypes.SNOWBALL&&!before.contains(e.getUUID()))shots.add(e);});
        require(shots.size()==1,"exactly one actual native thrown body");shots.forEach(f::discard);
    }
    private static void checkProjectile(WoolBucketChecks.Fixture f){
        var shooter=EntityTypes.SKELETON.create(f.level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);f.own(shooter);
        shooter.setNoAi(true);shooter.setPos(f.owner.position().add(18,4,0));shooter.setYRot(90);
        require(f.level.addFreshEntity(shooter),"native shooter publication");
        for(int punch:new int[]{0,2}){
            var victim=EntityTypes.COW.create(f.level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);f.own(victim);
            victim.setNoAi(true);victim.setNoGravity(true);victim.setPos(f.owner.position().add(8,4,0));
            var direction=new Vec3(1,.35,.15).normalize();var start=victim.getBoundingBox().getCenter().subtract(direction.scale(2));
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(start).offset(-1,-1,-1),victim.blockPosition().offset(2,3,2)))f.set(pos.immutable(),Blocks.AIR.defaultBlockState());
            require(f.level.addFreshEntity(victim),"native impact victim publication");
            var bow=new ItemStack(Items.BOW);
            if(punch>0)bow.enchant(f.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PUNCH),punch);
            var arrow=new net.minecraft.world.entity.projectile.arrow.Arrow(f.level,shooter,new ItemStack(Items.ARROW),bow);f.own(arrow);
            arrow.setPos(start);arrow.setDeltaMovement(direction.scale(3));require(f.level.addFreshEntity(arrow),"native arrow publication");
            arrow.tick();
            require(victim.getHealth()<victim.getMaxHealth(),"actual swept arrow impact applied damage");
            var impulse=victim.getDeltaMovement();
            require(impulse.lengthSqr()>1e-8&&impulse.normalize().dot(direction)>.999,
                    "native impact and Punch follow full velocity despite shooter facing the opposite way; punch="+punch+" impulse="+impulse);
            f.discard(arrow);f.discard(victim);
        }
        f.discard(shooter);
    }
    private static void checkObserver(GameController game,WoolBucketChecks.Fixture f){
        var board=game.server().getScoreboard();var original=f.owner.getTeam();
        var team=board.addPlayerTeam("check_observer_feedback");team.setPlayerPrefix(net.minecraft.network.chat.Component.literal("PREFIX ").withStyle(net.minecraft.ChatFormatting.RED));
        board.addPlayerToTeam(f.owner.getScoreboardName(),team);
        try{
            game.spectators().enter(f.owner);
            var tab=f.owner.getTabListDisplayName();
            require(tab!=null&&tab.getString().equals(f.owner.getScoreboardName())&&tab.getStyle().isItalic()
                    &&tab.getStyle().getColor().equals(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GRAY)),
                    "observer TAB is gray italic nickname without its real team prefix/color");
            var pos=f.owner.blockPosition().offset(2,0,0);int sequence=23000;
            for(var block:List.of(Blocks.CHEST,Blocks.BARREL,Blocks.SHULKER_BOX,Blocks.FURNACE,Blocks.HOPPER)){
                f.set(pos,block.defaultBlockState());var contents=(net.minecraft.world.Container)f.level.getBlockEntity(pos);
                contents.setItem(0,new ItemStack(Items.DIAMOND,7));
                int from=f.audience.packets().size();f.client.acknowledge();
                f.owner.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false),sequence++));
                var gui=(SimpleGui)SguiUtils.getCurrentGui(f.owner);require(gui!=null&&gui.getGuiElement(0).getItemStack().getCount()==7,"packet opens read-only "+block);
                f.owner.containerMenu.clicked(0,0,ContainerInput.PICKUP,f.owner);f.owner.containerMenu.clicked(0,0,ContainerInput.QUICK_MOVE,f.owner);
                require(contents.getItem(0).getCount()==7&&f.owner.containerMenu.getCarried().isEmpty(),"preview cannot take container contents");
                contents.setItem(0,new ItemStack(Items.DIAMOND,3));game.spectators().tick();
                require(gui.getGuiElement(0).getItemStack().getCount()==3,"observer preview follows actual content changes");gui.close();
                var traffic=SmokeVolumeChecks.flatten(f.audience.packets().subList(from,f.audience.packets().size()));
                require(traffic.stream().noneMatch(p->p instanceof ClientboundBlockEventPacket||p instanceof ClientboundSoundPacket),"observer open/close emits no world animation or sound");
            }
            var left=Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT);
            var otherPos=pos.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(left));
            f.set(pos,left);f.set(otherPos,left.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT));
            ((net.minecraft.world.Container)f.level.getBlockEntity(pos)).setItem(0,new ItemStack(Items.DIAMOND,7));
            ((net.minecraft.world.Container)f.level.getBlockEntity(otherPos)).setItem(0,new ItemStack(Items.EMERALD,5));
            require(game.spectators().openContainer(f.owner,pos),"double chest inspection opens");
            var doubleGui=(SimpleGui)SguiUtils.getCurrentGui(f.owner);
            require(doubleGui.getSize()==54&&java.util.Set.of(doubleGui.getGuiElement(0).getItemStack().getItem(),doubleGui.getGuiElement(27).getItemStack().getItem()).equals(java.util.Set.of(Items.DIAMOND,Items.EMERALD)),"both native halves of double chest are visible");
            doubleGui.close();f.set(otherPos,Blocks.AIR.defaultBlockState());
            f.set(pos,Blocks.ENDER_CHEST.defaultBlockState());
            var ender=f.owner.getEnderChestInventory();var savedEnder=ender.getItem(0).copy();
            try{
                ender.setItem(0,new ItemStack(Items.GOLD_INGOT,9));
                require(game.spectators().openContainer(f.owner,pos),"ender chest inspection opens");
                var enderGui=(SimpleGui)SguiUtils.getCurrentGui(f.owner);
                require(enderGui.getGuiElement(0).getItemStack().getCount()==9,"viewer sees own ender contents");enderGui.close();
            }finally{ender.setItem(0,savedEnder);}
            for(var type:java.util.List.of(EntityTypes.CHEST_MINECART,EntityTypes.HOPPER_MINECART)){
                var cart=type.create(f.level,net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);f.own(cart);
                cart.setPos(f.owner.position().add(1,0,0));require(f.level.addFreshEntity(cart),"native container vehicle publication");
                var contents=(net.minecraft.world.Container)cart;contents.setItem(0,new ItemStack(Items.IRON_INGOT,6));
                f.owner.connection.handleInteract(new ServerboundInteractPacket(cart.getId(),InteractionHand.MAIN_HAND,Vec3.ZERO,false));
                var cartGui=(SimpleGui)SguiUtils.getCurrentGui(f.owner);
                require(cartGui!=null&&cartGui.getGuiElement(0).getItemStack().getCount()==6&&!game.spectators().attached(f.owner),"interact inspects vehicle inventory rather than attaching its camera");
                f.owner.containerMenu.clicked(0,0,ContainerInput.PICKUP,f.owner);
                require(contents.getItem(0).getCount()==6&&f.owner.containerMenu.getCarried().isEmpty(),"vehicle inspection stays read-only");
                cartGui.close();
                var random=(net.minecraft.world.entity.vehicle.ContainerEntity)cart;
                var pending=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("lbc:ported/lucky_block"));
                random.setContainerLootTable(pending);require(game.spectators().openContainer(f.owner,cart),"unopened vehicle preview opens");
                game.spectators().tick();require(random.getContainerLootTable().equals(pending),"vehicle preview never initializes random loot");
                f.discard(cart);game.spectators().tick();require(SguiUtils.getCurrentGui(f.owner)==null,"removed vehicle closes its preview");
            }
            f.set(pos,Blocks.CHEST.defaultBlockState());
            var unopened=(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity)f.level.getBlockEntity(pos);
            var table=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.Identifier.parse("lbc:ported/lucky_block"));
            unopened.setLootTable(table);
            require(game.spectators().openContainer(f.owner,pos)&&unopened.getLootTable().equals(table),"observer never rolls unopened random loot");
            game.spectators().tick();require(unopened.getLootTable().equals(table),"live preview also preserves unopened loot");
        }finally{
            game.spectators().remove(f.owner);board.removePlayerFromTeam(f.owner.getScoreboardName(),team);board.removePlayerTeam(team);
            if(original!=null)board.addPlayerToTeam(f.owner.getScoreboardName(),original);
        }
    }
}
