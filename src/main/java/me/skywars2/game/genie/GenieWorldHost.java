package me.skywars2.game.genie;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;
import me.skywars2.game.*;
import me.skywars2.game.combat.WeaponEnchantments;
import me.skywars2.game.commands.SkyWarsFeedbackCommands;
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
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/** Real world API for item code. Call/drain on the game thread; no network worker touches a level. */
public final class GenieWorldHost implements ScriptMachine.Host,AutoCloseable {
    private record Work(String name,List<JsonElement> arguments,CompletableFuture<JsonElement> future){}
    private static final Map<String,ScriptProgram.Signature> API=Map.ofEntries(
        Map.entry("world.command",new ScriptProgram.Signature(1,1)),
        Map.entry("world.select",new ScriptProgram.Signature(1,1)),
        Map.entry("world.raycast",new ScriptProgram.Signature(4,4)),
        Map.entry("combat.hitscan",new ScriptProgram.Signature(4,4)),
        Map.entry("structure.place",new ScriptProgram.Signature(2,2)),
        Map.entry("entity.info",new ScriptProgram.Signature(1,1)),
        Map.entry("entity.attribute",new ScriptProgram.Signature(2,2)),
        Map.entry("entity.change_attribute",new ScriptProgram.Signature(4,4)),
        Map.entry("entity.launch",new ScriptProgram.Signature(1,4)),
        Map.entry("lbc.mana",new ScriptProgram.Signature(1,1)),
        Map.entry("lbc.counter",new ScriptProgram.Signature(2,2)),
        Map.entry("lbc.change_counter",new ScriptProgram.Signature(4,4)),
        Map.entry("lbc.apply_status",new ScriptProgram.Signature(3,3)),
        Map.entry("lbc.grimoire",new ScriptProgram.Signature(1,2)),
        Map.entry("lbc.cast",new ScriptProgram.Signature(1,4)),
        Map.entry("lbc.cast_at",new ScriptProgram.Signature(2,3)),
        Map.entry("lbc.summon",new ScriptProgram.Signature(2,2)),
        Map.entry("item.use_charge",new ScriptProgram.Signature(1,1)),
        Map.entry("math.random",new ScriptProgram.Signature(0,2)),
        Map.entry("time.tick",new ScriptProgram.Signature(0,0)));
    private final GameController game;
    private final ServerPlayer owner;
    private final ItemStack captured;
    private final BooleanSupplier admitted;
    private final LongSupplier clock;
    private final IntPredicate charge;
    private final ScriptMemory memory;
    private final ArrayDeque<Work> queue=new ArrayDeque<>();
    private Work active;
    private boolean closed,draining;

    public GenieWorldHost(GameController game,ServerPlayer owner,ItemStack captured,BooleanSupplier admitted,LongSupplier clock){
        this(game,owner,captured,admitted,clock,new ScriptMemory());
    }
    public GenieWorldHost(GameController game,ServerPlayer owner,ItemStack captured,BooleanSupplier admitted,LongSupplier clock,ScriptMemory memory){
        this(game,owner,captured,admitted,clock,memory,limit->false);
    }
    GenieWorldHost(GameController game,ServerPlayer owner,ItemStack captured,BooleanSupplier admitted,LongSupplier clock,
                   ScriptMemory memory,IntPredicate charge){
        this.game=Objects.requireNonNull(game);this.owner=Objects.requireNonNull(owner);
        this.captured=Objects.requireNonNull(captured);this.admitted=Objects.requireNonNull(admitted);this.clock=Objects.requireNonNull(clock);
        this.charge=Objects.requireNonNull(charge);
        this.memory=Objects.requireNonNull(memory);memory.track(this,this::memoryRoots,()->16+8L*queue.size());
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
        memory.reserveValues(values);
        var arguments=new ArrayList<JsonElement>();for(var value:values)arguments.add(value.deepCopy());
        return enqueue(name, List.copyOf(arguments));
    }

    private ScriptMachine.Pending enqueue(String name, List<JsonElement> arguments) {
        var future=new CompletableFuture<JsonElement>();
        var work=new Work(name,arguments,future);queue.addLast(work);
        return new ScriptMachine.Pending(future,()->{queue.remove(work);future.cancel(false);});
    }

    ItemStack capturedItem() { return captured.copy(); }

    public ScriptMachine.PendingImage checkpointPending(ScriptMachine.Pending task, ScriptGraphCopy graph) {
        mainThread();
        if (draining) throw new IllegalStateException("Cannot checkpoint a native item action during execution");
        for (var work : queue) {
            if (work.future != task.future()) continue;
            String name = work.name;
            var arguments = work.arguments.stream().map(graph::copy).toList();
            return (host, restoreGraph) -> {
                var restored = (GenieWorldHost) host;
                restored.memory.reserveValues(arguments);
                return restored.enqueue(name, arguments.stream().map(restoreGraph::copy).toList());
            };
        }
        throw new IllegalStateException("Item continuation has no queued native action");
    }
    /** Share maximum/deadline across hosts in the scheduler; this method does not own another clock. */
    public int drain(int maximum,long deadlineNanos){
        mainThread();if(maximum<0)throw new IllegalArgumentException("negative native job budget");
        if(draining||closed)return 0;int completed=0;draining=true;
        try{
            while(completed<maximum&&!queue.isEmpty()&&System.nanoTime()<deadlineNanos){
                if(!owner.isAlive()||owner.isRemoved()){close();break;}
                if(!admitted.getAsBoolean())break;
                var work=queue.removeFirst();if(work.future.isDone())continue;completed++;active=work;
                try(var scope=WeaponEnchantments.using(owner,captured)){
                    var result=execute(work.name,work.arguments);result=result==null?JsonNull.INSTANCE:result;
                    memory.reserveValue(result);work.future.complete(result);
                }catch(RuntimeException error){work.future.completeExceptionally(error);}
                finally{active=null;}
            }
        }finally{draining=false;}
        return completed;
    }
    private List<JsonElement> memoryRoots(){
        var roots=new ArrayList<JsonElement>();for(var work:queue)roots.addAll(work.arguments);
        if(active!=null)roots.addAll(active.arguments);return roots;
    }
    public int pending(){return queue.size();}
    @Override public void close(){
        mainThread();closed=true;while(!queue.isEmpty())queue.removeFirst().future.cancel(false);memory.forget(this);
    }
    private JsonElement execute(String name,List<JsonElement> values){
        return switch(name){
            case "world.command"->command(text(values.getFirst()));
            case "world.select"->select(text(values.getFirst()));
            case "world.raycast"->ray(vector(values.get(0)),vector(values.get(1)),number(values.get(2)),bool(values.get(3)));
            case "combat.hitscan"->new JsonPrimitive(GenieNative.hitscan(game,owner,
                vector(values.get(0)),vector(values.get(1)),number(values.get(2)),number(values.get(3))));
            case "structure.place"->GenieStructures.place(owner,text(values.get(0)),values.get(1));
            case "entity.info"->info(find(text(values.getFirst())));
            case "entity.attribute","entity.change_attribute"->attribute(values);
            case "entity.launch"->launch(values);
            case "lbc.mana"->find(text(values.getFirst())) instanceof ServerPlayer player
                    ? GeniePlayerParameters.mana(game, player) : JsonNull.INSTANCE;
            case "lbc.counter","lbc.change_counter"->counter(values);
            case "lbc.apply_status"->applyStatus(values);
            case "lbc.grimoire"->grimoire(values);
            case "lbc.summon"->summon(values);
            case "lbc.cast"->cast(values);
            case "lbc.cast_at"->castAt(values);
            case "item.use_charge"->{
                double requested=number(values.getFirst());
                int limit=(int)requested;
                if(limit!=requested||limit<1||limit>4096)
                    throw new IllegalArgumentException("item charge limit must be an integer between 1 and 4096");
                yield new JsonPrimitive(charge.test(limit));
            }
            default->throw new IllegalArgumentException("unknown queued world API");
        };
    }
    private CommandSourceStack source(CommandSource output){
        // Gameplay authority does not inherit the account's operator/owner authority.
        return owner.createCommandSourceStack().withSource(GenieCommandAccess.scopedOutput(owner.level(),output)).withPermission(LevelBasedPermissionSet.GAMEMASTER);
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
        GenieCommandAccess.check(parsed,dispatcher.getRoot());
        return parsed;
    }

    private JsonElement launch(List<JsonElement> values) {
        var type=BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(text(values.getFirst())))
            .orElseThrow(()->new IllegalArgumentException("unknown entity type")).value();
        Vec3 position=values.size()>=2&&!values.get(1).isJsonNull()?vector(values.get(1)):null;
        Vec3 velocity=values.size()>=3&&!values.get(2).isJsonNull()?vector(values.get(2)):owner.getLookAngle().scale(1.6);
        int life=200;
        if(values.size()==4){
            if(!values.get(3).isJsonPrimitive()||!values.get(3).getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("integer lifetime expected");
            life=values.get(3).getAsBigDecimal().intValueExact();
        }
        var entity=game.items().genie().runtime().launchNative(owner,captured,type,position,velocity,life,0);
        return entity==null?JsonNull.INSTANCE:new JsonPrimitive(entity.getUUID().toString());
    }

    private JsonElement summon(List<JsonElement> values) {
        var ids = GenieNative.summon(game, owner, captured, Identifier.parse(text(values.get(0))), vector(values.get(1)));
        var bodies = new JsonArray();
        for (var id : ids) bodies.add(id.toString());
        return bodies;
    }

    private JsonElement cast(List<JsonElement> values) {
        var id = Identifier.parse(text(values.getFirst()));
        Vec3 origin = values.size() >= 2 && !values.get(1).isJsonNull() ? vector(values.get(1)) : owner.getEyePosition();
        Vec3 direction = values.size() >= 3 && !values.get(2).isJsonNull() ? vector(values.get(2)) : owner.getLookAngle();
        InteractionHand hand = values.size() == 4 && !values.get(3).isJsonNull()
                ? castHand(values.get(3)) : InteractionHand.MAIN_HAND;
        return new JsonPrimitive(GenieNative.cast(game, owner, hand, captured, id, origin, direction));
    }

    private JsonElement castAt(List<JsonElement> values) {
        Integer lifetime = null;
        if (values.size() == 3) {
            var value = values.get(2);
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
                throw new IllegalArgumentException("integer lifetime expected");
            try {
                lifetime = value.getAsBigDecimal().intValueExact();
            } catch (ArithmeticException invalid) {
                throw new IllegalArgumentException("integer lifetime expected", invalid);
            }
        }
        return new JsonPrimitive(GenieNative.castAt(game, owner, captured,
                Identifier.parse(text(values.getFirst())), vector(values.get(1)), lifetime));
    }

    private static InteractionHand castHand(JsonElement value) {
        return switch (text(value)) {
            case "mainhand" -> InteractionHand.MAIN_HAND;
            case "offhand" -> InteractionHand.OFF_HAND;
            default -> throw new IllegalArgumentException("unknown spell hand");
        };
    }

    private JsonElement grimoire(List<JsonElement> values) {
        InteractionHand hand = values.size() == 1 ? InteractionHand.MAIN_HAND : castHand(values.get(1));
        return new JsonPrimitive(GenieNative.grimoire(game,owner,Identifier.parse(text(values.getFirst())),hand));
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
        return owner.level().getEntity(UUID.fromString(id));
    }
    private JsonElement applyStatus(List<JsonElement> values) {
        String target = text(values.get(0));
        String status = text(values.get(1));
        double ticks = number(values.get(2));
        if (ticks < 1 || ticks > Integer.MAX_VALUE || ticks != Math.rint(ticks))
            throw new IllegalArgumentException("status duration requires a positive integer in ticks");
        return new JsonPrimitive(find(target) instanceof ServerPlayer player
                && SkyWarsFeedbackCommands.applyFromItem(game, owner, player, status, (int) ticks));
    }
    private JsonElement counter(List<JsonElement> values) {
        if (!(find(text(values.getFirst())) instanceof ServerPlayer player)) return JsonNull.INSTANCE;
        String key = text(values.get(1));
        Integer result;
        if (values.size() == 2) result = GeniePlayerParameters.read(game, player, key);
        else {
            double amount = number(values.get(3));
            if (amount != Math.rint(amount) || amount < Integer.MIN_VALUE || amount > Integer.MAX_VALUE)
                throw new IllegalArgumentException("counter change requires a signed 32-bit integer");
            result = GeniePlayerParameters.change(game, owner, player, key, additive(values.get(2)), (int) amount);
        }
        return result == null ? JsonNull.INSTANCE : new JsonPrimitive(result);
    }

    private JsonElement attribute(List<JsonElement> values) {
        if (!(find(text(values.getFirst())) instanceof LivingEntity target)) return JsonNull.INSTANCE;
        var type = BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(text(values.get(1)))).orElse(null);
        if (type == null) return JsonNull.INSTANCE;
        // Read through the attribute map without creating an instance just for a query.
        var attributes = target.getAttributes();
        if (!attributes.hasAttribute(type)) return JsonNull.INSTANCE;
        if (values.size() == 4) {
            double amount = number(values.get(3));
            double requested = additive(values.get(2)) ? attributes.getBaseValue(type) + amount : amount;
            if (!Double.isFinite(requested)) throw new IllegalArgumentException("attribute result must be finite");
            attributes.getInstance(type).setBaseValue(type.value().sanitizeValue(requested));
        }
        var result = new JsonObject();
        result.addProperty("base", attributes.getBaseValue(type));
        result.addProperty("value", attributes.getValue(type));
        return result;
    }

    private static boolean additive(JsonElement operation) {
        return switch (text(operation)) {
            case "add" -> true;
            case "set" -> false;
            default -> throw new IllegalArgumentException("expected set or add");
        };
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
        result.add("entity",hits.isEmpty()?JsonNull.INSTANCE:hits.getFirst().get("entity"));
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
