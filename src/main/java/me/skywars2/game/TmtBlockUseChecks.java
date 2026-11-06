package me.skywars2.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Native game-mode use-on, exact private entity payload and real source factory/debit composition. */
public final class TmtBlockUseChecks {
    private TmtBlockUseChecks(){}
    private static void require(boolean yes,String why){if(!yes)throw new IllegalStateException("TMT block use: "+why);}
    private static LbcTmtAbilities.Ability property(GameController game,ItemStack source){
        var id=BuiltInRegistries.ITEM.getKey(source.getItem());var data=source.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        data.putString(LbcTmtAbilities.NATIVE,id.toString());LbcTmtComponents.capture(game,source,data);
        return new LbcTmtAbilities.Ability("native/block/"+id.getPath(),CustomData.of(data));
    }
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var initial=new HashSet<java.util.UUID>();for(var e:f.level.getAllEntities())initial.add(e.getUUID());
            var used=net.minecraft.stats.Stats.ITEM_USED.get(LbcTmtItems.prototype(2).getItem());int count=f.owner.getStats().getValue(used);
            int otherUsed=f.other.getStats().getValue(used);
            require(game.items().imbaTerrainUtilities().ready()&&game.items().imbaTerrainUtilities().state().jobs()==0,"exclusive prepared native terrain work");
            try{
                var crystal=property(game,LbcImbaPlaceableItems.END_CRYSTAL.getDefaultInstance());
                var authored=LbcSpecialEggs.LOW_VEX.getDefaultInstance().copy();var tag=authored.get(DataComponents.ENTITY_DATA).copyTagWithoutId();
                tag.putBoolean("NoAI",true);tag.putString("CustomName","TMT authored native egg");
                authored.set(DataComponents.ENTITY_DATA,TypedEntityData.of(EntityTypes.VEX,tag));var vex=property(game,authored);
                var stack=LbcTmtItems.prototype(2);stack.setCount(3);var root=stack.get(DataComponents.CUSTOM_DATA).copyTag();
                LbcTmtAbilities.write(root,List.of(crystal,vex));stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));
                var originalPotion=new PotionContents(java.util.Optional.empty(),java.util.Optional.of(123456),List.of(),java.util.Optional.empty());
                stack.set(DataComponents.POTION_CONTENTS,originalPotion);stack.set(DataComponents.CUSTOM_NAME,Component.literal("same outer procedural item"));
                var before=stack.copyWithCount(1);f.owner.setItemInHand(InteractionHand.MAIN_HAND,stack);var off=new ItemStack(Items.STICK,7);f.owner.setItemInHand(InteractionHand.OFF_HAND,off);
                var pad=BlockPos.containing(f.home.add(0,1,3));f.set(pad,Blocks.STONE.defaultBlockState());f.set(pad.above(),Blocks.AIR.defaultBlockState());f.set(pad.above(2),Blocks.AIR.defaultBlockState());
                var hit=new BlockHitResult(Vec3.atBottomCenterOf(pad.above()),Direction.UP,pad,false);
                require(f.owner.gameMode.useItemOn(f.owner,f.level,stack,InteractionHand.MAIN_HAND,hit).consumesAction(),"actual registered TMT use-on reaches native factories");
                require(stack.getCount()==1&&f.owner.getOffhandItem()==off&&off.getCount()==7,"each accepted native factory owns one used-hand debit");
                require(ItemStack.matches(stack,before)&&!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"same outer item/slots/potion/name restored after both independent native payloads");
                int crystals=0,vexes=0;Vex actor=null;
                for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())){if(e instanceof EndCrystal)crystals++;if(e instanceof Vex found){vexes++;actor=found;}f.own(e);}
                require(crystals==1&&vexes==1&&actor!=null&&actor.isNoAi()&&actor.getHealth()==1&&actor.getCustomName()!=null&&actor.getCustomName().getString().equals("TMT authored native egg"),"two genuine native outputs preserve source Vex HP and exact authored entity payload");
                var encoded=ItemStack.CODEC.encodeStart(f.owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),stack).getOrThrow();
                var loaded=ItemStack.CODEC.parse(f.owner.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),encoded).getOrThrow();require(ItemStack.matches(stack,loaded),"native item codec preserves private component template");
                try{LbcTmtAbilities.invoke(stack,vex.id(),()->{
                    require(stack.get(DataComponents.ENTITY_DATA).type()==EntityTypes.VEX&&!stack.has(DataComponents.POTION_CONTENTS),"selected callback sees only its native payload");
                    require(ItemStack.matches(LbcTmtAbilities.outerCopy(stack),before),"whole-item refund sees original components rather than the selected native payload");
                    throw new IllegalArgumentException("expected native scope failure");
                });throw new IllegalStateException("missing deliberate failure");}catch(IllegalArgumentException expected){require(expected.getMessage().equals("expected native scope failure"),"correct scope failure");}
                require(ItemStack.matches(stack,before)&&!LbcTmtAbilities.active(stack)&&!LbcTmtAbilities.nativePredicatesActive(),"exception restores all native components and classifier scopes");
                var block=LbcTmtItems.prototype(2);block.setCount(2);var blockRoot=block.get(DataComponents.CUSTOM_DATA).copyTag();
                LbcTmtAbilities.write(blockRoot,List.of(property(game,LbcImbaFollowupItems.DEBRIS.getDefaultInstance())));block.set(DataComponents.CUSTOM_DATA,CustomData.of(blockRoot));
                f.other.setItemInHand(InteractionHand.MAIN_HAND,block);var floor=f.other.blockPosition().offset(0,-1,2);
                f.set(floor,Blocks.STONE.defaultBlockState());f.set(floor.above(),Blocks.AIR.defaultBlockState());
                var blockHit=new BlockHitResult(Vec3.atBottomCenterOf(floor.above()),Direction.UP,floor,false);
                f.field(false,f.other.position());require(game.items().magic().suppressed(f.other),"actual positive MAG field");
                require(!f.other.gameMode.useItemOn(f.other,f.level,block,InteractionHand.MAIN_HAND,blockHit).consumesAction()
                        &&block.getCount()==2&&f.level.getBlockState(floor.above()).isAir()&&game.items().imbaTerrainUtilities().state().jobs()==0,"native MAG denial preserves item, world and terrain queue");
                f.removeFields();
                require(f.other.gameMode.useItemOn(f.other,f.level,block,InteractionHand.MAIN_HAND,blockHit).consumesAction()&&block.getCount()==1
                        &&f.level.getBlockState(floor.above()).is(Blocks.ANCIENT_DEBRIS)&&game.items().imbaTerrainUtilities().state().jobs()==1,"real native BlockItem placement and one existing terrain job; failed attempt does not lock out the accepted input");
            }finally{
                f.owner.getStats().setValue(f.owner,used,count);
                f.other.getStats().setValue(f.other,used,otherUsed);f.removeFields();game.items().imbaTerrainUtilities().stop();
                for(var e:f.level.getAllEntities())if(!initial.contains(e.getUUID())){f.own(e);game.items().entityUnloaded(e);e.discard();}
            }
        }
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-block-use (real used-hand context, two native egg outputs/debits, independent authored entity payload, original components/codec/refund/exception restoration; native BlockItem placement/shared terrain job and actual MAG rejection)");
    }
}
