package me.skywars2.game;

import io.netty.buffer.Unpooled;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AcademyInteractionChecks {
    private AcademyInteractionChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Academy interaction: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var p=f.owner;var at=f.home.add(0,8,0);f.move(p,at);f.move(f.other,at.add(12,0,0));p.setYRot(0);p.setXRot(0);
            var teacher=EntityTypes.INTERACTION.create(f.level,EntitySpawnReason.TRIGGERED);
            teacher.load(TagValueInput.create(ProblemReporter.DISCARDING,p.registryAccess(),TagParser.parseCompoundFully("{width:1.4f,height:2.2f,response:1b,Tags:['magic_academy']}")));
            teacher.setPos(at.add(0,0,1.5));require(f.level.addFreshEntity(teacher),"native teacher interaction");f.own(teacher);
            var raw=new ClientboundSetEntityDataPacket(teacher.getId(),teacher.getEntityData().getNonDefaultValues());
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),p.registryAccess());
            try{
                ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,raw);
                var wire=ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                int width=me.skywars2.mixin.AcademyInteractionMixin.Data.skywars2$width().id(),height=me.skywars2.mixin.AcademyInteractionMixin.Data.skywars2$height().id();
                require(wire.packedItems().stream().anyMatch(v->v.id()==width&&v.value().equals(0f))
                        &&wire.packedItems().stream().anyMatch(v->v.id()==height&&v.value().equals(0f)),"native encoded client metadata has no pickable area");
                require(teacher.getBoundingBox().getXsize()>1&&teacher.getBoundingBox().getYsize()>2,"authoritative teacher bounds stay intact for upgrading");
            }finally{buffer.release();}
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE));p.getFoodData().setFoodLevel(0);
            require(!game.items().academy().interact(p,InteractionHand.MAIN_HAND,teacher),"non-wand direct entity input is not consumed");
            require(p.gameMode.useItem(p,f.level,p.getMainHandItem(),InteractionHand.MAIN_HAND).consumesAction()&&p.isUsingItem(),"food starts its real native use behind transparent teacher");p.stopUsingItem();
            var wall=BlockPos.containing(at.add(0,1,3));f.set(wall,Blocks.STONE.defaultBlockState());f.set(wall.north(),Blocks.AIR.defaultBlockState());
            var stack=new ItemStack(Items.COBBLESTONE,3);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            require(p.gameMode.useItemOn(p,f.level,stack,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(wall).add(0,0,-.5),Direction.NORTH,wall,false)).consumesAction()
                    &&f.level.getBlockState(wall.north()).is(Blocks.COBBLESTONE)&&stack.getCount()==2,"native block placement/debit passes teacher");
            var wand=new ItemStack(LbcWands.FIRE_WAND);p.setItemInHand(InteractionHand.MAIN_HAND,wand);game.items().mana().set(p,5000);int before=game.items().mana().get(p);
            require(p.gameMode.useItem(p,f.level,wand,InteractionHand.MAIN_HAND).consumesAction()
                    &&LbcFartus.data(p.getMainHandItem()).getIntOr("upgradable",0)==1&&game.items().mana().get(p)==before,"looking at teacher with an upgradeable wand still assigns the real trial without casting");
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
        SkyWars2.LOGGER.info("[check] PASS academy-interaction (actual encoded transparent client bounds, intact native teacher bounds, food/block native use and real wand trial routing)");
    }
}
