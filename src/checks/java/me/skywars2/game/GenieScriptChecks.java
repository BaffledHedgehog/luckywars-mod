package me.skywars2.game;

import java.util.concurrent.CompletableFuture;
import me.skywars2.game.genie.*;
import me.skywars2.game.genie.script.*;
import me.skywars2.game.items.tmt.LbcTmtTriggers;
import me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Real vanilla input, persisted per-item state, clone isolation, pause and reset cancellation. */
final class GenieScriptChecks implements AutoCloseable {
    private final GameController game;
    private final WoolBucketChecks.Fixture base;
    private final GenieScriptRuntime runtime;
    private final CompletableFuture<ScriptRepository.Loaded> prepared;
    private final CompletableFuture<ScriptRepository.Loaded> preparedFairness;
    private final CompletableFuture<ScriptRepository.Loaded> preparedPassive;
    private final CompletableFuture<ScriptRepository.Loaded> preparedCharged;
    private final GenieScriptRuntime fairness;
    private final BlockPos marker;
    private ItemStack original,clone;
    private GenieRuntime.Snapshot rewindScripts;
    private MatchPlayerCheckpoint rewindPlayer;
    private int phase,when,sequence=600;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Genie item script: "+why);}
    GenieScriptChecks(GameController game,WoolBucketChecks.Fixture base){
        this.game=game;this.base=base;runtime=game.items().genie().runtime().scripts();
        marker=BlockPos.containing(base.resident(false)).offset(2,-2,2);base.set(marker,Blocks.AIR.defaultBlockState());
        String command="setblock "+marker.getX()+" "+marker.getY()+" "+marker.getZ()+" minecraft:gold_block";
        String source="""
            {"version":1,"state":{"counter":0},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[
            ["load","state"],["const","counter"],["load","state"],["const","counter"],["get"],["const",1],["add"],["put"],["drop"],
            ["const",8],["wait"],["const",%s],["call","world.command",1],["return"]]}}}
            """.formatted(new com.google.gson.JsonPrimitive(command));
        prepared=runtime.prepare(source);when=game.server().getTickCount();
        preparedCharged=runtime.prepare("""
            {"version":1,"state":{"activated":0,"block_presses":0},"handlers":{"USE":"main","BLOCK_USE":"block"},"functions":{"main":{"parameters":["event"],"code":[
            ["const",2],["call","item.use_charge",1],["jump_if_false","done"],
            ["load","state"],["const","activated"],["load","state"],["const","activated"],["get"],["const",1],["add"],["put"],["drop"],
            ["const",%s],["call","world.command",1],["return"],["label","done"],["const",false],["return"]]},
            "block":{"parameters":["event"],"code":[["load","state"],["const","block_presses"],
            ["load","state"],["const","block_presses"],["get"],["const",1],["add"],["put"],["return"]]}}}
            """.formatted(new com.google.gson.JsonPrimitive(command)));
        preparedPassive = runtime.prepare("""
            {"version":1,"state":{"counter":0},"handlers":{"TICK":"main"},"functions":{"main":{"parameters":["event"],"code":[
            ["load","state"],["const","counter"],["load","state"],["const","counter"],["get"],["const",1],["add"],["put"],["return"]]}}}
            """);
        fairness=new GenieScriptRuntime(game,32,100_000_000L);
        preparedFairness=fairness.prepare("""
            {"version":1,"state":{"counter":0},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[
            ["label","loop"],["load","state"],["const","counter"],["load","state"],["const","counter"],["get"],["const",1],["add"],["put"],["drop"],["jump","loop"]]}}}
            """);
    }
    private int count(ItemStack item){return GenieScriptItem.state(item).get("counter").getAsInt();}
    private void click(){
        base.client.acknowledge();base.owner.connection.handleClientTickEnd(new ServerboundClientTickEndPacket());
        sendAirUse(InteractionHand.MAIN_HAND);
    }
    private void sendAirUse(InteractionHand hand){
        base.owner.connection.handleUseItem(new ServerboundUseItemPacket(hand,sequence++,base.owner.getYRot(),base.owner.getXRot()));
    }
    private void sendEntityUse(EntityHitResult hit, InteractionHand hand){
        var target=hit.getEntity();
        base.owner.connection.handleInteract(new ServerboundInteractPacket(target.getId(),hand,
                hit.getLocation().subtract(target.position()),false));
    }
    private void next(int value){phase=value;when=game.server().getTickCount();}
    boolean tick(){
        require(game.server().getTickCount()-when<100,"bounded real event/state scenario phase "+phase);
        if(phase==0){
            if(!prepared.isDone())return false;
            original=GenieChecks.restoreSavedGift(
                    GenieScriptItem.construct(prepared.join(),"iron_sword","Native script test","A normal click runs item-local code.",1),base.owner);
            clone=original.copy();
            require(GenieRuntime.matches(original)&&original.has(net.minecraft.core.component.DataComponents.TOOL),"script item retains real native sword tools and event capture");
            GenieChecks.requireGiftPresentation(original);
            var data=original.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
            require(!data.contains("functions")&&!data.contains("genie_program")&&GenieScriptItem.encodedState(original).length()<64,"wire NBT contains code identity and small state, not the static program");
            base.owner.setItemInHand(InteractionHand.MAIN_HAND,original);base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);base.owner.getInventory().setItem(10,clone);
            click();next(1);return false;
        }
        if(phase==1){
            if(!base.level.getBlockState(marker).is(Blocks.GOLD_BLOCK))return false;
            require(count(original)==1&&count(clone)==0,"one real right-click triggers once and does not share state with an identical full-NBT copy");
            base.set(marker,Blocks.AIR.defaultBlockState());
            base.owner.getInventory().setItem(10,original);base.owner.setItemInHand(InteractionHand.MAIN_HAND,clone);
            click();next(2);return false;
        }
        if(phase==2){
            if(!base.level.getBlockState(marker).is(Blocks.GOLD_BLOCK))return false;
            require(count(original)==1&&count(clone)==1,"the copied item has its own independent code state");
            base.set(marker,Blocks.AIR.defaultBlockState());
            base.owner.getInventory().setItem(10,clone);base.owner.setItemInHand(InteractionHand.MAIN_HAND,original);
            click();runtime.tick();require(count(original)==2,"moving the original to another slot preserves its state");
            game.items().genie().cancel(base.owner);next(3);return false;
        }
        if(phase==3){
            if(game.server().getTickCount()-when<12)return false;
            require(base.level.getBlockState(marker).isAir()&&count(original)==2,"reset cancels a waiting native command instead of replaying it after the delay");
            click();runtime.tick();require(count(original)==3,"a later accepted click reloads the persisted item state");
            base.pause();next(4);return false;
        }
        if(phase==4){
            if(game.server().getTickCount()-when<12)return false;
            require(base.level.getBlockState(marker).isAir()&&count(original)==3,"TimeStop pauses item continuations and their logical waits");
            base.thaw();next(5);return false;
        }
        if(phase==5){
            if(!base.level.getBlockState(marker).is(Blocks.GOLD_BLOCK))return false;
            require(count(original)==3&&count(clone)==1,"thaw resumes accepted code once without retriggering the click");
            if(!preparedFairness.isDone())return false;
            var first=GenieScriptItem.construct(preparedFairness.join(),"stick","First","Fair scheduling.",1);
            var second=first.copy();base.owner.setItemInHand(InteractionHand.MAIN_HAND,first);base.owner.setItemInHand(InteractionHand.OFF_HAND,second);
            fairness.event(base.owner,"USE",null,null,null);fairness.tick();fairness.tick();
            require(count(first)>0&&count(second)>0,"a balanced infinite first handler cannot starve another item when the shared instruction budget expires");
            close();base.owner.setItemInHand(InteractionHand.MAIN_HAND,original);base.owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            base.set(marker,Blocks.AIR.defaultBlockState());
            click();runtime.tick();require(count(original)==4,"physical item begins the pre-rewind action");
            // Save with identical NBT in a second slot. The running action belongs to the selected stack.
            base.owner.getInventory().setItem(10,original.copy());
            click(); // This accepted event is still queued at the save point.
            rewindScripts=game.items().genie().runtime().snapshot();
            rewindPlayer=new MatchPlayerCheckpoint(game,base.owner);
            next(6);return false;
        }
        if(phase==6){
            if(!base.level.getBlockState(marker).is(Blocks.GOLD_BLOCK)||game.server().getTickCount()-when<16)return false;
            require(count(original)==5,"future processes the second accepted click before rewind");
            var manager=game.server().tickRateManager();boolean frozen=manager.isFrozen();manager.setFrozen(true);
            try {
                game.items().genie().stop();
                rewindPlayer.restoreFrozen();
                rewindScripts.restore(game.items().genie().runtime());
                base.set(marker,Blocks.AIR.defaultBlockState());
            } finally { manager.setFrozen(frozen); }
            base.client.acknowledge();
            original=base.owner.getMainHandItem();clone=base.owner.getInventory().getItem(10);
            require(count(original)==4&&count(clone)==4,"native player rewind re-creates both stacks with their saved data");
            next(7);return false;
        }
        if(phase==7){
            if(!base.level.getBlockState(marker).is(Blocks.GOLD_BLOCK))return false;
            require(count(original)==5&&count(clone)==4,"saved queue and wait resume beside an identical copy without rewriting its slot");
            var below = base.owner.blockPosition().below();
            var hit = new BlockHitResult(Vec3.atCenterOf(below).add(0,.5,0),Direction.UP,below,false);
            base.owner.gameMode.useItemOn(base.owner,base.level,original,InteractionHand.MAIN_HAND,hit);
            next(8);return false;
        }
        if(phase==8){
            if (game.server().getTickCount() - when < 5 || !preparedPassive.isDone() || !preparedCharged.isDone()) return false;
            require(count(original)==6&&count(clone)==4,"native use-on-block invokes the generic USE handler once even though the block event is also emitted");
            checkPassiveInput();
            var support=base.owner.blockPosition().below();
            base.set(support,Blocks.STONE.defaultBlockState());
            checkCharges(new BlockHitResult(Vec3.atCenterOf(support).add(0,.5,0),Direction.UP,support,false));
            checkCharges(null);
            var target=new Cow(EntityTypes.COW,base.level);
            target.setNoAi(true);target.setNoGravity(true);target.setPos(base.owner.position().add(0,0,2));
            require(base.level.addFreshEntity(target),"real neutral entity target for the packed input sequence");
            base.own(target);
            try { checkCharges(new EntityHitResult(target,target.position().add(0,1,0))); }
            finally { base.discard(target); }
            runtime.forget(base.owner);return true;
        }
        throw new IllegalStateException("unknown script check phase");
    }
    private void checkCharges(HitResult hit){
        var owner=base.owner;
        var limited=GenieScriptItem.construct(preparedCharged.join(),"stick","Limited script","Two actual uses.",1);
        owner.setItemInHand(InteractionHand.MAIN_HAND,limited);
        owner.setItemInHand(InteractionHand.OFF_HAND,hit instanceof BlockHitResult?ItemStack.EMPTY:new ItemStack(Items.SNOWBALL,3));
        for(int use=1;use<=2;use++){
            var copy=limited.copy();
            var copyState=copy.copy();
            owner.getInventory().setItem(10,copy);
            base.set(marker,Blocks.AIR.defaultBlockState());
            if(use==1){
                base.client.acknowledge();owner.connection.handleClientTickEnd(ServerboundClientTickEndPacket.INSTANCE);
                if(hit instanceof BlockHitResult block){
                    owner.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND,block,sequence++));
                    sendAirUse(InteractionHand.MAIN_HAND);
                    owner.connection.handleUseItemOn(new ServerboundUseItemOnPacket(InteractionHand.OFF_HAND,block,sequence++));
                }else{
                    if(hit instanceof EntityHitResult entity){
                        owner.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,entity.getLocation());
                        sendEntityUse(entity,InteractionHand.MAIN_HAND);
                    }else owner.setXRot(-90);
                    var nearby=owner.getBoundingBox().inflate(3);
                    var prior=base.level.getEntitiesOfClass(Snowball.class,nearby,shot->shot.getOwner()==owner);
                    sendAirUse(InteractionHand.MAIN_HAND);
                    if(hit instanceof EntityHitResult entity)sendEntityUse(entity,InteractionHand.OFF_HAND);
                    sendAirUse(InteractionHand.OFF_HAND);
                    var thrown=base.level.getEntitiesOfClass(Snowball.class,nearby,shot->shot.getOwner()==owner&&!prior.contains(shot));
                    thrown.forEach(base::discard);
                    require(thrown.size()==1 && owner.getOffhandItem().getCount()==2,
                            "an air fallback still throws the actual offhand snowball exactly once");
                }
            }else{
                owner.setXRot(-90);
                sendAirUse(InteractionHand.MAIN_HAND); // An independent click in the same client frame still spends a use.
            }
            for(int step=0;step<8;step++)runtime.tick();
            require(base.level.getBlockState(marker).is(Blocks.GOLD_BLOCK),"each paid use runs the effect, including the final use");
            require(limited.isEmpty()==(use==2)&&ItemStack.matches(copy,copyState),
                    "only the exact gift spends one charge per "+(hit==null?"air":hit.getType())+" click, including the last charge");
            if(use==1)require(GenieScriptItem.state(limited).get("activated").getAsInt()==1
                            && GenieScriptItem.state(limited).get("block_presses").getAsInt()==(hit instanceof BlockHitResult?1:0),
                    "the vanilla hand sequence queues one USE charge and only block clicks queue one BLOCK_USE");
        }
        owner.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        base.set(marker,Blocks.AIR.defaultBlockState());
        click();
        runtime.tick();
        require(base.level.getBlockState(marker).isAir(),"empty hand cannot use the expired script again");
    }
    private void checkPassiveInput() {
        var owner = base.owner;
        var main = owner.getMainHandItem();
        var off = owner.getOffhandItem();
        var head = owner.getItemBySlot(EquipmentSlot.HEAD);
        var stored = owner.getInventory().getItem(10);
        var passive = GenieScriptItem.construct(preparedPassive.join(), "iron_helmet", "Passive script", "Native tick input.", 1);
        var backpackCopy = passive.copy();
        try {
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            owner.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            owner.setItemSlot(EquipmentSlot.HEAD, passive);
            owner.getInventory().setItem(10, backpackCopy);
            LbcTmtTriggers.input(owner, LbcTmtTriggers.Input.TICK);
            runtime.tick();
            require(count(passive) == 1 && count(backpackCopy) == 0, "the equipped-only Genie receives TICK; the inventory copy stays idle");

            owner.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            owner.setItemInHand(InteractionHand.OFF_HAND, passive);
            LbcTmtTriggers.input(owner, LbcTmtTriggers.Input.TICK);
            runtime.tick();
            require(count(passive) == 2 && count(backpackCopy) == 0, "an offhand-only Genie receives TICK after moving off the armor slot");
        } finally {
            owner.setItemSlot(EquipmentSlot.HEAD, head);
            owner.setItemInHand(InteractionHand.MAIN_HAND, main);
            owner.setItemInHand(InteractionHand.OFF_HAND, off);
            owner.getInventory().setItem(10, stored);
        }
    }

    @Override public void close(){fairness.close();}
}
