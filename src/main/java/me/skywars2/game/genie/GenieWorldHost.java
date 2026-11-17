package me.skywars2.game.genie;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import me.skywars2.game.*;
import me.skywars2.game.genie.script.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;

/** Real world API for item code. Call/drain on the game thread; no network worker touches a level. */
public final class GenieWorldHost implements ScriptMachine.Host,AutoCloseable {
    private record Work(Supplier<JsonElement> action,CompletableFuture<JsonElement> future){}
    private static final Map<String,ScriptProgram.Signature> API=Map.of(
        "world.command",new ScriptProgram.Signature(1,1),
        "world.select",new ScriptProgram.Signature(1,1),
        "world.raycast",new ScriptProgram.Signature(4,4),
        "entity.info",new ScriptProgram.Signature(1,1),
        "math.random",new ScriptProgram.Signature(0,2),
        "time.tick",new ScriptProgram.Signature(0,0));
    private final GameController game;
    private final ServerPlayer owner;
    private final ItemStack captured;
    private final BooleanSupplier admitted;
    private final LongSupplier clock;
    private final ArrayDeque<Work> queue=new ArrayDeque<>();
    private boolean closed,draining;

    public GenieWorldHost(GameController game,ServerPlayer owner,ItemStack captured,BooleanSupplier admitted,LongSupplier clock){
        this.game=Objects.requireNonNull(game);this.owner=Objects.requireNonNull(owner);
        this.captured=Objects.requireNonNull(captured);this.admitted=Objects.requireNonNull(admitted);this.clock=Objects.requireNonNull(clock);
    }
    public static Map<String,ScriptProgram.Signature> api(){return API;}
    private void mainThread(){if(!game.server().isSameThread())throw new IllegalStateException("item world API requires the game thread");}
    @Override public ScriptMachine.Result call(String name,List<JsonElement> values){
        mainThread();var signature=API.get(name);
        if(signature==null||!signature.accepts(values.size()))throw new IllegalArgumentException("unknown world API or argument count");
        if(closed)throw new IllegalStateException("item world host is closed");
        if(name.equals("time.tick"))return new ScriptMachine.Value(new JsonPrimitive(clock.getAsLong()));
        if(name.equals("math.random")){
            double low=values.size()==2?number(values.get(0)):0,high=values.isEmpty()?1:number(values.getLast());
            if(high<low||!Double.isFinite(high-low))throw new IllegalArgumentException("invalid random interval");
            return new ScriptMachine.Value(new JsonPrimitive(low+(high-low)*owner.getRandom().nextDouble()));
        }
        // Pending work retains immutable arguments, never references the VM's mutable state.
        if(queue.size()>=4096)throw new IllegalStateException("item native-work backlog exceeds storage bounds");
        var arguments=new ArrayList<JsonElement>();for(var value:values)arguments.add(value.deepCopy());
        var future=new CompletableFuture<JsonElement>();
        var work=new Work(()->execute(name,arguments),future);queue.addLast(work);
        return new ScriptMachine.Pending(future,()->{queue.remove(work);future.cancel(false);});
    }
    /** Share maximum/deadline across hosts in the scheduler; this method does not own another clock. */
    public int drain(int maximum,long deadlineNanos){
        mainThread();if(maximum<0)throw new IllegalArgumentException("negative native job budget");
        if(draining||closed)return 0;int completed=0;draining=true;
        try{
            while(completed<maximum&&!queue.isEmpty()&&System.nanoTime()<deadlineNanos){
                if(!owner.isAlive()||owner.isRemoved()){close();break;}
                if(!admitted.getAsBoolean())break;
                var work=queue.removeFirst();if(work.future.isDone())continue;completed++;
                try(var scope=WeaponEnchantments.using(owner,captured)){
                    work.future.complete(executeChecked(work.action));
                }catch(RuntimeException error){work.future.completeExceptionally(error);}
            }
        }finally{draining=false;}
        return completed;
    }
    private static JsonElement executeChecked(Supplier<JsonElement> action){var result=action.get();return result==null?JsonNull.INSTANCE:result;}
    public int pending(){return queue.size();}
    @Override public void close(){
        mainThread();closed=true;while(!queue.isEmpty())queue.removeFirst().future.cancel(false);
    }
    private JsonElement execute(String name,List<JsonElement> values){
        return switch(name){
            case "world.command"->command(text(values.getFirst()));
            case "world.select"->select(text(values.getFirst()));
            case "world.raycast"->ray(vector(values.get(0)),vector(values.get(1)),number(values.get(2)),bool(values.get(3)));
            case "entity.info"->info(find(text(values.getFirst())));
            default->throw new IllegalArgumentException("unknown queued world API");
        };
    }
    private CommandSourceStack source(CommandSource output){
        // Gameplay authority does not inherit the account's operator/owner authority.
        return owner.createCommandSourceStack().withSource(output).withPermission(LevelBasedPermissionSet.GAMEMASTER);
    }
    /** Technical preflight is also available before accepting a generated literal command. */
    public void validateCommand(String line){mainThread();parseCommand(line,source(CommandSource.NULL));}
    private ParseResults<CommandSourceStack> parseCommand(String line,CommandSourceStack source){
        if(line.isBlank()||line.length()>1048576||line.indexOf('\n')>=0||line.indexOf('\r')>=0)
            throw new IllegalArgumentException("one bounded native command line required");
        var dispatcher=game.server().getCommands().getDispatcher();
        var parsed=dispatcher.parse(Commands.trimOptionalPrefix(line),source);
        try{Commands.validateParseResults(parsed);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException invalid){
            throw new IllegalArgumentException(invalid.getRawMessage().getString());
        }
        // Vanilla permits /reload at GAMEMASTER too. Item code has no reload/restart lifecycle.
        // Inspect the parsed redirect chain, not text prefixes: execute/return wrappers are included.
        var forbidden=Collections.newSetFromMap(new IdentityHashMap<CommandNode<CommandSourceStack>,Boolean>());
        var root=dispatcher.getRoot();
        for(String name:List.of("reload","stop")){var node=root.getChild(name);if(node!=null)forbidden.add(node);}
        var packs=root.getChild("datapack");
        if(packs!=null)for(String name:List.of("enable","disable")){var node=packs.getChild(name);if(node!=null)forbidden.add(node);}
        for(var context=parsed.getContext();context!=null;context=context.getChild())for(var part:context.getNodes()){
            var node=part.getNode();
            for(var excluded:forbidden)if(node==excluded||node.getRedirect()==excluded
                    ||node.getCommand()!=null&&node.getCommand()==excluded.getCommand())
                throw new IllegalArgumentException("item commands cannot reload packs or control the server lifecycle");
        }
        return parsed;
    }
    private JsonObject command(String line){
        var result=new JsonObject();var output=new JsonArray();int[] totals={0,0};
        var sink=new CommandSource(){
            @Override public void sendSystemMessage(Component message){if(output.size()<64)output.add(message.getString());}
            @Override public boolean acceptsSuccess(){return true;}
            @Override public boolean acceptsFailure(){return true;}
            @Override public boolean shouldInformAdmins(){return false;}
        };
        var commands=game.server().getCommands();
        var context=source(sink).withCallback((success,value)->{if(success)totals[0]++;totals[1]+=value;});
        var parsed=parseCommand(line,context);
        commands.performCommand(parsed,line);
        result.addProperty("successes",totals[0]);result.addProperty("result",totals[1]);result.add("output",output);return result;
    }
    private JsonArray select(String selector){
        try{
            var reader=new StringReader(selector);var parsed=EntityArgument.entities().parse(reader);
            if(reader.canRead())throw new IllegalArgumentException("unexpected selector suffix");
            var result=new JsonArray();
            for(var entity:parsed.findEntities(source(CommandSource.NULL))){
                if(result.size()>=65536)throw new IllegalArgumentException("selector result exceeds item data storage");
                result.add(entity.getUUID().toString());
            }
            return result;
        }catch(com.mojang.brigadier.exceptions.CommandSyntaxException invalid){throw new IllegalArgumentException(invalid.getRawMessage().getString());}
    }
    private Entity find(String id){
        var uuid=UUID.fromString(id);
        for(var level:game.server().getAllLevels()){var found=level.getEntity(uuid);if(found!=null)return found;}
        return null;
    }
    private JsonElement info(Entity entity){
        if(entity==null)return JsonNull.INSTANCE;
        var result=new JsonObject();result.addProperty("id",entity.getUUID().toString());
        result.addProperty("type",BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        result.addProperty("dimension",entity.level().dimension().identifier().toString());
        result.addProperty("alive",entity.isAlive());result.addProperty("on_ground",entity.onGround());
        result.add("position",vector(entity.position()));result.add("velocity",vector(entity.getDeltaMovement()));
        result.add("eye_position",vector(entity.getEyePosition()));
        result.add("look",vector(entity.getLookAngle()));result.addProperty("name",entity.getName().getString());
        if(entity instanceof LivingEntity living){
            result.addProperty("health",living.getHealth());result.addProperty("max_health",living.getMaxHealth());
        }
        return result;
    }
    private JsonObject ray(Vec3 from,Vec3 to,double margin,boolean entities){
        if(margin<0||margin>64||from.distanceToSqr(to)>1024*1024)
            throw new IllegalArgumentException("one spatial query supports a 1024-block segment and 64-block margin; split longer queries across ticks");
        var level=owner.level();var wall=GenieNative.wall(level,from,to);var end=wall==null?to:wall.getLocation();
        var result=new JsonObject();result.add("end",vector(end));result.add("block",JsonNull.INSTANCE);
        if(wall!=null){
            var block=new JsonObject();block.add("position",vector(Vec3.atLowerCornerOf(wall.getBlockPos())));
            boolean loaded=level.hasChunkAt(wall.getBlockPos());block.addProperty("loaded",loaded);
            if(loaded)block.addProperty("id",BuiltInRegistries.BLOCK.getKey(level.getBlockState(wall.getBlockPos()).getBlock()).toString());
            else block.add("id",JsonNull.INSTANCE);
            block.addProperty("face",wall.getDirection().getSerializedName());result.add("block",block);
        }
        var hits=new ArrayList<JsonObject>();
        // Broad-phase AABB intersection is strict at the edge, while native clip accepts it.
        // Vanilla ProjectileUtil also pads its search independently of the actual hitbox.
        if(entities)for(var entity:level.getEntities(owner,new AABB(from,end).inflate(margin+1),e->!e.isRemoved())){
            var box=entity.getBoundingBox().inflate(margin);
            var point=box.contains(from)?from:box.clip(from,end).orElse(null);if(point==null)continue;
            var hit=new JsonObject();hit.addProperty("entity",entity.getUUID().toString());hit.add("position",vector(point));
            hit.addProperty("distance",from.distanceTo(point));hits.add(hit);
            if(hits.size()>65536)throw new IllegalArgumentException("ray result exceeds item data storage");
        }
        hits.sort(Comparator.comparingDouble(e->e.get("distance").getAsDouble()));
        var array=new JsonArray();hits.forEach(array::add);result.add("entities",array);return result;
    }
    private static double number(JsonElement value){
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("numeric API argument required");
        double result=value.getAsDouble();if(!Double.isFinite(result))throw new IllegalArgumentException("finite API argument required");return result;
    }
    private static String text(JsonElement value){
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException("text API argument required");return value.getAsString();
    }
    private static boolean bool(JsonElement value){
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException("boolean API argument required");return value.getAsBoolean();
    }
    private static Vec3 vector(JsonElement value){
        if(!value.isJsonArray()||value.getAsJsonArray().size()!=3)throw new IllegalArgumentException("three-coordinate vector required");
        var a=value.getAsJsonArray();return new Vec3(number(a.get(0)),number(a.get(1)),number(a.get(2)));
    }
    private static JsonArray vector(Vec3 value){var result=new JsonArray();result.add(value.x);result.add(value.y);result.add(value.z);return result;}
}
