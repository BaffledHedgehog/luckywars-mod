package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Packet-level placement/fluid inputs and the vanilla item's first placement state. */
public final class CubeInteractionChecks {
    private CubeInteractionChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("Cube interactions: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false);var stats=new NativeMeleeChecks.NativeStats(f.owner)){
            f.prepareParticipants();var at=f.owner.blockPosition().offset(0,-1,2);
            var states=new ArrayList<BlockState>();for(var kind:LbcLuckyBlocks.Kind.values())states.add(LbcLuckyBlocks.block(kind).defaultBlockState());states.add(LbcConsumedBlock.BLOCK.defaultBlockState());
            var items=new ArrayList<Item>();for(var state:states)items.add(state.getBlock().asItem());
            var test=new Input(f,at);
            for(var item:items){
                var server=item.getDefaultInstance();var saved=server.copy();
                var client=PolymerItemUtils.getPolymerItemStack(server,TooltipFlag.NORMAL,f.owner.connection.getPacketContext(),f.owner.registryAccess());
                require(client.is(Items.NOTE_BLOCK)&&Objects.equals(client.get(DataComponents.ITEM_MODEL),server.get(DataComponents.ITEM_MODEL)),"exact note carrier preserves authored item model "+item);
                var data=client.get(DataComponents.BLOCK_STATE);require(data!=null,"client receives placement properties "+item);
                require(!client.getOrDefault(DataComponents.TOOLTIP_DISPLAY,net.minecraft.world.item.component.TooltipDisplay.DEFAULT).shows(DataComponents.BLOCK_STATE),"transport properties stay out of tooltip");
                var block=((BlockItem)item).getBlock();var expected=((PolymerBlock)block).getPolymerBlockState(block.defaultBlockState(),f.owner.connection.getPacketContext());
                for(var support:List.of(Blocks.STONE,Blocks.GOLD_BLOCK,Blocks.OAK_PLANKS)){
                    f.set(at,support.defaultBlockState());f.set(at.above(),Blocks.AIR.defaultBlockState());f.owner.setItemInHand(InteractionHand.MAIN_HAND,client.copy());
                    var place=new BlockPlaceContext(f.owner,InteractionHand.MAIN_HAND,client,test.hit());
                    var nativePlacement=Blocks.NOTE_BLOCK.getStateForPlacement(place);
                    require(nativePlacement!=null&&data.apply(nativePlacement)==expected,"vanilla placement plus transmitted properties predicts exact palette on "+support);
                }
                require(ItemStack.matches(server,saved)&&ItemStack.matches(server,PolymerItemUtils.getRealItemStack(client,f.owner.connection.getPacketContext(),f.owner.registryAccess())),"client-only prediction preserves complete native stack and Polymer roundtrip");
            }
            var supports=new ArrayList<>(states);supports.add(Blocks.STONE.defaultBlockState());var held=new ArrayList<>(items);held.add(Items.STONE);
            for(var support:supports)for(var item:held)for(var hand:InteractionHand.values())for(boolean crouch:new boolean[]{false,true}){
                test.prepare(support,hand,new ItemStack(item,2),crouch);test.send(hand,false);
                require(f.level.getBlockState(at.above()).is(((BlockItem)item).getBlock())&&f.level.getBlockState(at.above(2)).isAir(),"one real placement on "+support+" "+item+" "+hand+" crouch="+crouch);
                require(f.owner.getItemInHand(hand).is(item)&&f.owner.getItemInHand(hand).getCount()==1,"used-hand block debit exactly once");test.other(hand);
                int self=support.getBlock() instanceof LbcBlockModels.Cube&&!crouch?1:0;
                require(test.sounds(f.client,"minecraft:block.wood.place")+test.sounds(f.client,"minecraft:block.stone.place")==self,"self audio agrees with client prediction "+support+" "+hand+" crouch="+crouch);
                require(test.sounds(f.audience,"minecraft:block.wood.place")+test.sounds(f.audience,"minecraft:block.stone.place")==1,"observer receives exactly one placement sound");
            }
            for(var support:states)for(var hand:InteractionHand.values())for(boolean crouch:new boolean[]{false,true})for(var bucket:List.of(Items.WATER_BUCKET,Items.LAVA_BUCKET)){
                test.prepare(support,hand,new ItemStack(bucket),crouch);test.send(hand,true);
                var fluid=bucket==Items.WATER_BUCKET?Blocks.WATER:Blocks.LAVA;
                require(f.level.getBlockState(at.above()).is(fluid)&&f.owner.getItemInHand(hand).is(Items.BUCKET),"real bucket packet places fluid and consumes once "+support+" "+hand+" crouch="+crouch);test.other(hand);
                String sound=bucket==Items.WATER_BUCKET?"minecraft:item.bucket.empty":"minecraft:item.bucket.empty_lava";
                require(test.sounds(f.client,sound)==(crouch?0:1)&&test.sounds(f.audience,sound)==1,"bucket sound reaches unpredicted owner once and observer once");
                f.owner.setItemInHand(hand,new ItemStack(Items.BUCKET));test.clear();test.send(hand,true);
                require(f.level.getBlockState(at.above()).isAir()&&f.owner.getItemInHand(hand).is(bucket),"native bucket pickup also works without duplicate use");test.other(hand);
            }
        }
        SkyWars2.LOGGER.info("[check] PASS cube-interactions (all4 cubes; vanilla first-state projection, hidden transport/full native roundtrip;100 actual placement inputs with both hands/crouch/ordinary surface and exact self/observer audio;32 fluid pour/pickup inputs with duplicate-use suppression)");
    }
    private static final class Input {
        final WoolBucketChecks.Fixture f;final BlockPos at;int sequence;
        Input(WoolBucketChecks.Fixture f,BlockPos at){this.f=f;this.at=at;}
        BlockHitResult hit(){return new BlockHitResult(Vec3.atCenterOf(at).add(0,.5,0),Direction.UP,at,false);}
        void clear(){f.client.packets().clear();f.audience.packets().clear();}
        void prepare(BlockState support,InteractionHand hand,ItemStack held,boolean crouch){
            f.owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
            f.owner.setShiftKeyDown(crouch);f.owner.stopUsingItem();
            f.set(at,support);f.set(at.above(),Blocks.AIR.defaultBlockState());f.set(at.above(2),Blocks.AIR.defaultBlockState());
            f.owner.setItemInHand(hand,held);f.owner.setItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,7));
            f.owner.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(at).add(0,.5,0));f.client.acknowledge();clear();
        }
        void send(InteractionHand hand,boolean air){
            f.owner.connection.handleUseItemOn(new ServerboundUseItemOnPacket(hand,hit(),++sequence));
            if(air)f.owner.connection.handleUseItem(new ServerboundUseItemPacket(hand,++sequence,f.owner.getYRot(),f.owner.getXRot()));
            f.owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
        }
        void other(InteractionHand used){var s=f.owner.getItemInHand(used==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);require(s.is(Items.STICK)&&s.getCount()==7,"other hand remains intact");}
        long sounds(RegenerationBeltChecks.ScheduledClient client,String id){return client.packets().stream().filter(ClientboundSoundPacket.class::isInstance).map(ClientboundSoundPacket.class::cast).filter(p->p.getSound().value().location().equals(Identifier.parse(id))).count();}
    }
}
