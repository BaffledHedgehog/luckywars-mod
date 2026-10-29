package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;

/** Real two-hand item input, common queued rays, native inventories and a single lightning terminal. */
public final class RewardToolChecks {
    private RewardToolChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Reward tools: "+why);}
    private static ItemStack tool(String flag){var s=new ItemStack(Items.CARROT_ON_A_STICK);var d=new CompoundTag();d.putInt("raycastspell",1);d.putInt(flag,1);s.set(DataComponents.CUSTOM_DATA,CustomData.of(d));return s;}
    private static final class Fixture implements AutoCloseable {
        final GameController game;final AllGrimoireChecks.Fixture base;final List<Entity> owned=new ArrayList<>();
        final net.minecraft.server.level.ServerPlayer p;final BlockPos block;final MapManager.BlockSnapshot original;
        Cow cow;int stage,at;ItemStack glove;long bolts;boolean closed;
        Fixture(GameController game){this.game=game;base=new AllGrimoireChecks.Fixture(game);p=base.base.owner;
            base.beta.setPos(p.position().add(10,0,0));base.beta.setNoGravity(true);p.getInventory().clearContent();p.setYRot(0);p.setXRot(0);
            block=BlockPos.containing(p.getEyePosition().add(0,0,6));original=game.maps().snapshot(p.level(),block);at=game.server().getTickCount();
        }
        void ack(){
            ClientboundPlayerPositionPacket last=null;for(var packet:base.base.recording.packets)if(packet instanceof ClientboundPlayerPositionPacket pos)last=pos;
            if(last!=null)p.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(last.id(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));
            p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        }
        Cow cow(){var result=new Cow(EntityTypes.COW,p.level());result.setPos(p.getEyePosition().add(0,-.5,3));result.setNoAi(true);result.setNoGravity(true);require(p.level().addFreshEntity(result),"native hit target");owned.add(result);return result;}
        void give(String flag,InteractionHand hand){p.stopUsingItem();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);glove=tool(flag);p.setItemInHand(hand,glove);ack();require(p.gameMode.useItem(p,p.level(),glove,hand).consumesAction(),"native raw input "+flag+" "+hand);at=game.server().getTickCount();}
        boolean result(ItemStack got,ItemStack expected){return ItemStack.isSameItemSameComponents(got,expected)&&got.getCount()==expected.getCount()||IdentifierModel(got,"lbc:trusi");}
        boolean IdentifierModel(ItemStack s,String id){return net.minecraft.resources.Identifier.parse(id).equals(s.get(DataComponents.ITEM_MODEL));}
        void transferCases(){
            var donor=base.beta;donor.getInventory().clearContent();donor.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.GOLD_INGOT,11));
            var sameTeam=p.getTeam();var oldTeam=donor.getTeam();
            if(oldTeam!=null)game.server().getScoreboard().removePlayerFromTeam(donor.getScoreboardName());
            if(sameTeam!=null)game.server().getScoreboard().addPlayerToTeam(donor.getScoreboardName(),sameTeam);
            try{p.setItemInHand(InteractionHand.MAIN_HAND,tool("krazha"));require(LbcRewardTools.steal(game,p,InteractionHand.MAIN_HAND,donor)&&donor.getOffhandItem().isEmpty()&&result(p.getMainHandItem(),new ItemStack(Items.GOLD_INGOT,11)),"allied player offhand fallback");
                p.setItemInHand(InteractionHand.MAIN_HAND,tool("krazha"));require(LbcRewardTools.steal(game,p,InteractionHand.MAIN_HAND,donor)&&IdentifierModel(p.getMainHandItem(),"lbc:trusi"),"empty player yields authored substitute");
            }finally{if(donor.getTeam()!=null)game.server().getScoreboard().removePlayerFromTeam(donor.getScoreboardName());if(oldTeam!=null)game.server().getScoreboard().addPlayerToTeam(donor.getScoreboardName(),oldTeam);}
            var villager=new Villager(EntityTypes.VILLAGER,p.level());villager.setPos(p.position().add(4,0,0));villager.setNoAi(true);villager.setNoGravity(true);require(p.level().addFreshEntity(villager),"native inventory mob");owned.add(villager);
            var bread=new ItemStack(Items.BREAD,9);bread.set(DataComponents.CUSTOM_NAME,Component.literal("retained components"));villager.getInventory().setItem(0,bread);
            p.setItemInHand(InteractionHand.MAIN_HAND,tool("krazha"));require(LbcRewardTools.steal(game,p,InteractionHand.MAIN_HAND,villager)&&villager.getInventory().isEmpty()&&result(p.getMainHandItem(),bread),"native mob inventory full-stack fallback");
            var failed=tool("krazha");p.setItemInHand(InteractionHand.MAIN_HAND,failed);require(!LbcRewardTools.steal(game,p,InteractionHand.MAIN_HAND,villager)&&p.getMainHandItem()==failed&&failed.getCount()==1,"nothing to steal preserves exact glove");
        }
        long bolts(){long n=0;for(var e:p.level().getAllEntities())if(e instanceof LightningBolt&&!e.isRemoved())n++;return n;}
        boolean tick(){ack();int now=game.server().getTickCount();require(now-at<20,"bounded native queue stage="+stage);
            if(stage==0){transferCases();cow=cow();cow.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND,7));cow.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.EMERALD,3));give("krazha",InteractionHand.OFF_HAND);stage++;return false;}
            if(now==at)return false;
            if(stage==1){if(p.getOffhandItem()==glove)return false;require(cow.getMainHandItem().isEmpty()&&cow.getOffhandItem().getCount()==3&&result(p.getOffhandItem(),new ItemStack(Items.DIAMOND,7)),"queued exact mob hit uses mainhand before offhand");cow.discard();
                p.level().setBlock(block,Blocks.BEDROCK.defaultBlockState(),3);give("shetka",InteractionHand.MAIN_HAND);stage++;return false;}
            if(stage==2){if(!p.level().getBlockState(block).isAir())return false;require(p.getMainHandItem()==glove,"brush retains item after native erase");p.level().setBlock(block,Blocks.STONE.defaultBlockState(),3);bolts=bolts();give("lightning_bolt",InteractionHand.MAIN_HAND);stage++;return false;}
            if(stage==3){if(bolts()==bolts)return false;require(bolts()==bolts+1,"one wall terminal produces exactly one native bolt");return true;}
            throw new IllegalStateException("unknown stage");
        }
        public void close(){if(closed)return;closed=true;try{for(var e:owned){game.items().entityUnloaded(e);e.discard();}game.maps().restoreSnapshot(p.level(),block,original);}finally{base.close();}}
    }
    private static Fixture running;
    public static void start(GameController game){require(running==null,"exclusive fixture");running=new Fixture(game);SkyWars2.LOGGER.info("[check] START reward-tools");}
    public static void tick(GameController game){if(running==null)return;try{if(!running.tick())return;running.close();running=null;SkyWars2.LOGGER.info("[check] PASS reward-tools (native both-hand input/common queued exact hits, mob mainhand/offhand/inventory priorities, allied/empty players, failure retention, bedrock brush, singular native lightning)");}
        catch(Throwable error){try{running.close();}catch(Throwable cleanup){error.addSuppressed(cleanup);}finally{running=null;}SkyWars2.LOGGER.error("[check] FAIL reward-tools",error);}}
}
