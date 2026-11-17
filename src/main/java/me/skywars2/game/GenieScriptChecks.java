package me.skywars2.game;

import java.util.concurrent.CompletableFuture;
import me.skywars2.game.genie.*;
import me.skywars2.game.genie.script.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Real vanilla input, persisted per-item state, clone isolation, pause and reset cancellation. */
final class GenieScriptChecks implements AutoCloseable {
    private final GameController game;
    private final WoolBucketChecks.Fixture base;
    private final GenieScriptRuntime runtime;
    private final CompletableFuture<ScriptRepository.Loaded> prepared;
    private final CompletableFuture<ScriptRepository.Loaded> preparedFairness;
    private final GenieScriptRuntime fairness;
    private final BlockPos marker;
    private ItemStack original,clone;
    private int phase,when,sequence=600;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Genie item script: "+why);}
    GenieScriptChecks(GameController game,WoolBucketChecks.Fixture base){
        this.game=game;this.base=base;runtime=game.items().genie().runtime().scripts();
        marker=BlockPos.containing(base.resident(false)).offset(2,-2,2);base.set(marker,Blocks.AIR.defaultBlockState());
        String command="setblock "+marker.getX()+" "+marker.getY()+" "+marker.getZ()+" minecraft:gold_block";
        String source="""
            {"version":1,"state":{"counter":0},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[
            ["load","state"],["const","counter"],["load","state"],["const","counter"],["get"],["const",1],["add"],["put"],["drop"],
            ["const",2],["wait"],["const",%s],["call","world.command",1],["return"]]}}}
            """.formatted(new com.google.gson.JsonPrimitive(command));
        prepared=runtime.prepare(source);when=game.server().getTickCount();
        fairness=new GenieScriptRuntime(game,32,100_000_000L);
        preparedFairness=fairness.prepare("""
            {"version":1,"state":{"counter":0},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[
            ["label","loop"],["load","state"],["const","counter"],["load","state"],["const","counter"],["get"],["const",1],["add"],["put"],["drop"],["jump","loop"]]}}}
            """);
    }
    private int count(ItemStack item){return GenieScriptItem.state(item).get("counter").getAsInt();}
    private void click(){
        base.client.acknowledge();base.owner.connection.handleClientTickEnd(new ServerboundClientTickEndPacket());
        base.owner.connection.handleUseItem(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND,sequence++,base.owner.getYRot(),base.owner.getXRot()));
    }
    private void next(int value){phase=value;when=game.server().getTickCount();}
    boolean tick(){
        require(game.server().getTickCount()-when<100,"bounded real event/state scenario phase "+phase);
        if(phase==0){
            if(!prepared.isDone())return false;
            original=GenieScriptItem.construct(prepared.join(),"iron_sword","Native script test","A normal click runs item-local code.",1);clone=original.copy();
            require(GenieRuntime.matches(original)&&original.has(net.minecraft.core.component.DataComponents.TOOL),"script item retains real native sword tools and event capture");
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
            if(game.server().getTickCount()-when<6)return false;
            require(base.level.getBlockState(marker).isAir()&&count(original)==2,"reset cancels a waiting native command instead of replaying it after the delay");
            click();runtime.tick();require(count(original)==3,"a later accepted click reloads the persisted item state");
            base.pause();next(4);return false;
        }
        if(phase==4){
            if(game.server().getTickCount()-when<6)return false;
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
            runtime.forget(base.owner);return true;
        }
        throw new IllegalStateException("unknown script check phase");
    }
    @Override public void close(){fairness.close();}
}
