package me.skywars2.game;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.*;
import me.skywars2.game.genie.GenieWorldHost;
import me.skywars2.game.genie.script.*;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Actual native commands, selectors and collision shapes, using the existing owned Genie fixture. */
final class GenieWorldChecks {
    private GenieWorldChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Genie world API: "+why);}
    private static void rejected(Runnable action,String why){
        try{action.run();throw new IllegalStateException("Genie world API: "+why);}catch(IllegalArgumentException expected){}
    }
    private static JsonArray vector(Vec3 v){var a=new JsonArray();a.add(v.x);a.add(v.y);a.add(v.z);return a;}
    private static ScriptMachine.Pending queued(GenieWorldHost host,String method,JsonElement...args){
        var result=host.call(method,List.of(args));require(result instanceof ScriptMachine.Pending,"world action is queued");
        return (ScriptMachine.Pending)result;
    }
    private static JsonElement complete(GenieWorldHost host,ScriptMachine.Pending work){
        require(!work.future().isDone(),"no world action before scheduler admission");
        require(host.drain(1,Long.MAX_VALUE)==1&&work.future().isDone(),"one native job completes in its owned budget");
        return work.future().join();
    }
    static void run(GameController game,WoolBucketChecks.Fixture base){
        var center=BlockPos.containing(base.resident(false));var start=center.offset(-6,0,0);
        for(var pos:BlockPos.betweenClosed(start.offset(-1,-1,-1),start.offset(12,2,1)))base.set(pos.immutable(),Blocks.AIR.defaultBlockState());
        var first=start.offset(1,-1,0);var second=start.offset(2,-1,0);
        boolean[] admitted={true};
        try(var host=new GenieWorldHost(game,base.owner,new ItemStack(Items.STICK),()->admitted[0],()->123L)){
            var command=queued(host,"world.command",new JsonPrimitive("setblock "+first.getX()+" "+first.getY()+" "+first.getZ()+" minecraft:stone"));
            require(base.level.getBlockState(first).isAir(),"native setblock has not run during enqueue");
            admitted[0]=false;require(host.drain(1,Long.MAX_VALUE)==0&&!command.future().isDone(),"paused item does not run queued native work");
            admitted[0]=true;
            var result=complete(host,command).getAsJsonObject();
            require(base.level.getBlockState(first).is(Blocks.STONE)&&result.get("successes").getAsInt()==1,"real native command and success callback");
            var cancelled=queued(host,"world.command",new JsonPrimitive("setblock "+second.getX()+" "+second.getY()+" "+second.getZ()+" minecraft:stone"));
            cancelled.cancel().run();require(host.pending()==0&&cancelled.future().isCancelled()&&base.level.getBlockState(second).isAir(),"cancelled work never reaches the native world");
            var self=complete(host,queued(host,"world.select",new JsonPrimitive("@s"))).getAsJsonArray();
            require(self.size()==1&&self.get(0).getAsString().equals(base.owner.getUUID().toString()),"native selector retains actual player source");
            var time=(ScriptMachine.Value)host.call("time.tick",List.of());require(time.value().getAsLong()==123,"host uses caller's logical clock");

            // Verify gameplay authority through the native dispatcher without executing any admin command.
            try{
                var method=GenieWorldHost.class.getDeclaredMethod("source",CommandSource.class);method.setAccessible(true);
                var source=(CommandSourceStack)method.invoke(host,CommandSource.NULL);
                require(Commands.getParseException(game.server().getCommands().getDispatcher().parse("stop",source))!=null,
                    "the account's operator status cannot grant native administrator authority to item code");
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            rejected(()->host.validateCommand("reload"),"reload accepted");
            rejected(()->host.validateCommand("execute run reload"),"execute wrapper bypassed reload guard");
            rejected(()->host.validateCommand("return run reload"),"return wrapper bypassed reload guard");
            rejected(()->host.validateCommand("datapack disable vanilla"),"pack reload accepted");
            host.validateCommand("datapack list");

            var grass=start.offset(2,0,0);var slab=start.offset(4,0,0);var stone=start.offset(10,0,0);
            base.set(grass,Blocks.SHORT_GRASS.defaultBlockState());base.set(slab,Blocks.STONE_SLAB.defaultBlockState());base.set(stone,Blocks.STONE.defaultBlockState());
            Vec3 from=Vec3.atLowerCornerOf(start).add(.25,.75,.5),to=from.add(11,0,0);
            var cow=Objects.requireNonNull(EntityTypes.COW.create(base.level,EntitySpawnReason.TRIGGERED));
            cow.setNoAi(true);cow.setPos(Vec3.atLowerCornerOf(start).add(7,.0,.5));
            require(base.level.addFreshEntity(cow),"native cow witness published");base.own(cow);
            var snow=new Snowball(base.level,from.x+6,from.y,from.z,new ItemStack(Items.SNOWBALL));
            snow.setNoGravity(true);require(base.level.addFreshEntity(snow),"native projectile witness published");base.own(snow);
            try{
                var ray=complete(host,queued(host,"world.raycast",vector(from),vector(to),new JsonPrimitive(0),new JsonPrimitive(true))).getAsJsonObject();
                var wall=ray.getAsJsonObject("block");
                require(wall.get("loaded").getAsBoolean()&&wall.get("id").getAsString().equals("minecraft:stone")
                    &&wall.getAsJsonArray("position").get(0).getAsInt()==stone.getX(),"collision ray skips grass and empty upper half of slab");
                var hits=new HashSet<String>();for(var value:ray.getAsJsonArray("entities"))hits.add(value.getAsJsonObject().get("entity").getAsString());
                require(hits.contains(cow.getUUID().toString())&&hits.contains(snow.getUUID().toString()),
                    "one ray traverses multiple bodies and includes native projectiles: result="+ray+" cow="+cow.getBoundingBox()+" snow="+snow.getBoundingBox());
                var info=complete(host,queued(host,"entity.info",new JsonPrimitive(cow.getUUID().toString()))).getAsJsonObject();
                require(info.get("type").getAsString().equals("minecraft:cow")&&info.has("eye_position")&&info.get("health").getAsFloat()==cow.getHealth(),"native entity information is actual world state");
            }finally{base.discard(cow);base.discard(snow);}
            Vec3 absent=new Vec3(1048576.25,from.y,1048576.25);var absentPos=BlockPos.containing(absent);
            require(!base.level.hasChunkAt(absentPos),"remote witness starts unresident");
            var remote=complete(host,queued(host,"world.raycast",vector(absent),vector(absent.add(32,0,0)),new JsonPrimitive(0),new JsonPrimitive(true))).getAsJsonObject();
            require(!remote.getAsJsonObject("block").get("loaded").getAsBoolean()
                &&remote.getAsJsonObject("block").get("id").isJsonNull()&&!base.level.hasChunkAt(absentPos)
                &&!base.level.hasChunkAt(absentPos.offset(32,0,0)),"ray and hit metadata do not load missing chunks");

            // The real VM resolves world calls through this host; a completed command resumes once.
            var code=ScriptJson.parse("""
                {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[
                ["const","execute if entity @s run data get entity @s Pos"],
                ["call","world.command",1],["return"]]}}}
                """);
            var program=ScriptProgram.compile(code,GenieWorldHost.api(),Set.of("USE"));
            var fiber=new ScriptMachine(program,"main",List.of(new JsonObject()),program.initialState(),host);
            require(fiber.advance(0,100)==ScriptMachine.Status.WAITING,"real VM yields for queued native command");
            require(host.drain(1,Long.MAX_VALUE)==1&&fiber.advance(1,100)==ScriptMachine.Status.DONE,"VM resumes after real command");
            require(fiber.result().getAsJsonObject().get("successes").getAsInt()>0
                &&!fiber.result().getAsJsonObject().getAsJsonArray("output").isEmpty(),"native data-get result and output are available to code");
            var abandoned=queued(host,"world.select",new JsonPrimitive("@s"));
            host.close();require(abandoned.future().isCancelled()&&host.pending()==0,"item context close cancels every owned pending job");
        }
    }
}
