package me.skywars2.game.genie;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.skywars2.game.*;
import me.skywars2.game.genie.script.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/** Cooperative item-local programs, with one shared game-thread budget and no blocking disk reads. */
public final class GenieScriptRuntime implements AutoCloseable {
    public static final Set<String> EVENTS=Set.of("USE","HIT","HURT","JUMP","SHOT","EAT","SPRINT","TICK","MINING","LEFT_CLICK","SNEAK","THROW","SNEAK_PRESS","BLOCK_USE");
    private record Event(String name,JsonObject data,ItemStack captured){}
    private static final class Fiber {
        final String event;final ScriptMachine machine;final GenieWorldHost host;long revision;
        Fiber(String event,ScriptMachine machine,GenieWorldHost host){this.event=event;this.machine=machine;this.host=host;}
        void close(){try{machine.cancel();}finally{host.close();}}
    }
    private static final class Context {
        final UUID owner;final String epoch,code,serial;ItemStack item;String encoded;
        final CompletableFuture<ScriptRepository.Loaded> program;
        final ArrayDeque<Event> events=new ArrayDeque<>();final ArrayDeque<Fiber> fibers=new ArrayDeque<>();
        JsonObject state;long clock;boolean failed,retired,detached;Fiber running;
        Context(ServerPlayer owner,String epoch,ItemStack item,CompletableFuture<ScriptRepository.Loaded> program,boolean detached){
            this.owner=owner.getUUID();this.epoch=epoch;this.item=item;this.program=program;
            this.detached=detached;
            code=GenieScriptItem.code(item);serial=GenieScriptItem.serial(item);encoded=GenieScriptItem.encodedState(item);state=GenieScriptItem.state(item);
        }
    }
    private final GameController game;
    private final int instructionBudget;
    private final long sliceNanos;
    private final ScriptMemory memory=new ScriptMemory();
    private final ArrayDeque<Context> contexts=new ArrayDeque<>();
    private final Map<ItemStack,Context> bindings=new IdentityHashMap<>();
    private ScriptRepository repository;
    private boolean dispatching;
    public GenieScriptRuntime(GameController game){this(game,4096,1_500_000L);}
    public GenieScriptRuntime(GameController game,int instructionBudget,long sliceNanos){
        this.game=Objects.requireNonNull(game);
        if(instructionBudget<1||sliceNanos<1||sliceNanos>1_000_000_000L)throw new IllegalArgumentException("invalid item scheduler budget");
        this.instructionBudget=instructionBudget;this.sliceNanos=sliceNanos;
    }
    private void mainThread(){if(!game.server().isSameThread())throw new IllegalStateException("item runtime requires the game thread");}
    private ScriptRepository repository(){
        if(repository==null)repository=new ScriptRepository(game.server().getWorldPath(LevelResource.ROOT).resolve("skywars2/genie/programs"),GenieWorldHost.api(),EVENTS);
        return repository;
    }
    public CompletableFuture<ScriptRepository.Loaded> prepare(String source){mainThread();return repository().prepare(source);}
    private boolean admitted(ServerPlayer owner){
        return owner.isAlive()&&!owner.isRemoved()&&!game.balanceLegacy()&&!game.isObserver(owner)
            &&!game.items().actionsBlocked(owner)&&!GenieNative.stopped(game,owner)
            &&(game.canUseGameplayItems(owner)||game.stage().name().equals("ENDING"));
    }
    private List<ItemStack> inventory(ServerPlayer owner){
        var result=new ArrayList<ItemStack>();var inventory=owner.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++)result.add(inventory.getItem(i));
        for(var slot:EquipmentSlot.values())result.add(owner.getItemBySlot(slot));
        result.add(owner.containerMenu.getCarried());return result;
    }
    private boolean owned(ServerPlayer owner,ItemStack item){for(var candidate:inventory(owner))if(candidate==item&&!item.isEmpty())return true;return false;}
    private static boolean same(Context context,ItemStack stack){
        return !stack.isEmpty()&&context.code.equals(GenieScriptItem.code(stack))&&context.serial.equals(GenieScriptItem.serial(stack))&&context.encoded.equals(GenieScriptItem.encodedState(stack));
    }
    private Context context(ServerPlayer owner,ItemStack item,boolean detached){
        var known=bindings.get(item);if(known!=null&&known.owner.equals(owner.getUUID()))return known;
        // A native slot move may replace the ItemStack object. Never steal an extant clone's context.
        Context moved=null;
        for(var candidate:contexts)if(!candidate.detached&&candidate.owner.equals(owner.getUUID())&&!owned(owner,candidate.item)&&same(candidate,item)){
            if(moved!=null){moved=null;break;}moved=candidate;
        }
        if(moved!=null){bindings.remove(moved.item);moved.item=item;moved.detached=detached;bindings.put(item,moved);return moved;}
        var created=new Context(owner,game.items().epoch(),item,repository().load(GenieScriptItem.code(item)),detached);
        memory.track(created,()->{
            var roots=new ArrayList<JsonElement>();roots.add(created.state);for(var event:created.events)roots.add(event.data);return roots;
        },()->24L+16L*created.events.size());
        contexts.addLast(created);bindings.put(item,created);return created;
    }
    public void input(ServerPlayer owner,LbcTmtTriggers.Input input,Entity target){
        // The native consumption, mining and volley continuations provide their exact used source.
        if(input==LbcTmtTriggers.Input.SHOT||input==LbcTmtTriggers.Input.EAT||input==LbcTmtTriggers.Input.MINING)return;
        event(owner,input==LbcTmtTriggers.Input.RIGHT_CLICK?"USE":input.name(),target,null,null);
    }
    public void event(ServerPlayer owner,String name,Entity target,ItemStack captured,InteractionHand hand){
        mainThread();if(dispatching||!EVENTS.contains(name)||!admitted(owner))return;dispatching=true;
        try{
            var selected=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());
            if(captured!=null)selected.add(captured);else{
                selected.add(owner.getMainHandItem());selected.add(owner.getOffhandItem());
                if(Set.of("TICK","HURT","JUMP","SPRINT").contains(name))
                    for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))selected.add(owner.getItemBySlot(slot));
            }
            for(var source:selected){
                if(!GenieScriptItem.matches(source))continue;
                ItemStack physical=source;
                if(captured!=null){
                    physical=hand==null?ItemStack.EMPTY:owner.getItemInHand(hand);
                    if(!GenieScriptItem.code(source).equals(GenieScriptItem.code(physical))||!GenieScriptItem.serial(source).equals(GenieScriptItem.serial(physical)))physical=source;
                }
                Context context=null;
                try{
                    context=context(owner,physical,!owned(owner,physical));if(context.failed)continue;
                    if(context.program.isDone()&&!context.program.isCompletedExceptionally()&&!context.program.isCancelled()){
                        var program=context.program.join().program();
                        if(program.handler(name)==null&&(!name.equals("BLOCK_USE")||program.handler("USE")==null))continue;
                    }
                    if(name.equals("TICK")&&(context.events.stream().anyMatch(event->event.name.equals("TICK"))||context.fibers.stream().anyMatch(fiber->fiber.event.equals("TICK"))))continue;
                    if(context.events.size()>=64)throw new IllegalArgumentException("item event backlog exceeds storage bounds");
                    var data=new JsonObject();data.addProperty("type",name);data.addProperty("owner",owner.getUUID().toString());data.addProperty("tick",context.clock);
                    data.addProperty("hand",hand==null?source==owner.getOffhandItem()?"offhand":"mainhand":hand==InteractionHand.MAIN_HAND?"mainhand":"offhand");
                    data.add("target",target==null?JsonNull.INSTANCE:new JsonPrimitive(target.getUUID().toString()));
                    memory.reserveValue(data);context.events.addLast(new Event(name,data,source.copy()));
                }catch(RuntimeException error){if(context!=null)fail(context,error);else report(owner,error);}
            }
        }finally{dispatching=false;}
    }
    /** Includes loading contexts: accepted clicks wait for disk instead of disappearing on cache misses. */
    public void tick(){
        mainThread();if(contexts.isEmpty())return;
        // Logical clocks advance even for items which do not receive a CPU slice this tick.
        for(var context:List.copyOf(contexts)){
            var owner=game.server().getPlayerList().getPlayer(context.owner);
            if(owner!=null&&!context.failed&&admitted(owner))context.clock++;
        }
        int fuel=instructionBudget,nativeJobs=8;
        long deadline=System.nanoTime()+sliceNanos;int visits=contexts.size();
        while(visits-->0&&!contexts.isEmpty()){
            // Stop rotating here. Visiting the skipped tail would return the same hog to the front.
            if(fuel<=0||System.nanoTime()>=deadline)break;
            var context=contexts.removeFirst();var owner=game.server().getPlayerList().getPlayer(context.owner);
            if(owner==null||!owner.isAlive()||owner.isRemoved()||!context.epoch.equals(game.items().epoch())){release(context);continue;}
            if(context.failed&&!owned(owner,context.item)){release(context);continue;}
            contexts.addLast(context);if(context.failed||!admitted(owner))continue;
            if(!context.program.isDone())continue;
            try{
                var program=context.program.join().program();
                if(!context.events.isEmpty()){
                    var event=context.events.removeFirst();String handler=program.handler(event.name);
                    if(handler==null&&event.name.equals("BLOCK_USE"))handler=program.handler("USE");
                    if(handler!=null){
                        var host=new GenieWorldHost(game,owner,event.captured,()->admitted(owner)&&context.epoch.equals(game.items().epoch()),()->context.clock,memory);
                        try{context.fibers.addLast(new Fiber(event.name,new ScriptMachine(program,handler,List.of(event.data),context.state,host,memory),host));}
                        catch(RuntimeException error){host.close();throw error;}
                    }
                }
                int count=context.fibers.size();
                while(count-->0&&fuel>0&&System.nanoTime()<deadline){
                    var fiber=context.fibers.removeFirst();context.running=fiber;
                    try{
                        fiber.machine.advance(context.clock,Math.min(fuel,64),deadline);fuel-=fiber.machine.lastAdvanceInstructions();
                        // Persist before native commands can move or copy the physical source stack.
                        if(fiber.machine.stateRevision()!=fiber.revision){fiber.revision=fiber.machine.stateRevision();flush(context,owner);}
                        if(nativeJobs>0)nativeJobs-=fiber.host.drain(nativeJobs,deadline);
                        if(context.retired||fiber.machine.status()==ScriptMachine.Status.DONE||fiber.machine.status()==ScriptMachine.Status.CANCELLED)fiber.close();
                        else context.fibers.addLast(fiber);
                    }catch(RuntimeException error){fiber.close();throw error;}
                    finally{context.running=null;}
                    if(context.retired)break;
                }
                if(context.events.isEmpty()&&context.fibers.isEmpty()&&!owned(owner,context.item)){contexts.remove(context);release(context);}
            }catch(RuntimeException error){if(!context.retired)fail(context,error);}
        }
    }
    private void flush(Context context,ServerPlayer owner){
        String encoded=ScriptJson.canonical(context.state);if(encoded.equals(context.encoded))return;
        if(!context.detached&&!owned(owner,context.item)){
            ItemStack replacement=null;for(var item:inventory(owner))if(same(context,item)){
                if(replacement!=null){replacement=null;break;}replacement=item;
            }
            if(replacement!=null){bindings.remove(context.item);context.item=replacement;bindings.put(replacement,context);}
        }
        context.encoded=encoded;GenieScriptItem.state(context.item,encoded);
        owner.getInventory().setChanged();owner.containerMenu.broadcastChanges();
    }
    private static void report(ServerPlayer owner,RuntimeException error){System.getLogger("skywars2.genie").log(System.Logger.Level.WARNING,"Item program stopped for "+owner.getUUID()+": "+error.getMessage());}
    private void fail(Context context,RuntimeException error){
        context.failed=true;context.events.clear();while(!context.fibers.isEmpty())context.fibers.removeFirst().close();context.state=new JsonObject();
        var owner=game.server().getPlayerList().getPlayer(context.owner);if(owner!=null)report(owner,error);
    }
    private void release(Context context){
        context.retired=true;context.events.clear();if(context.running!=null)context.running.close();
        while(!context.fibers.isEmpty())context.fibers.removeFirst().close();memory.forget(context);bindings.remove(context.item,context);
    }
    public void forget(ServerPlayer owner){mainThread();for(var context:List.copyOf(contexts))if(context.owner.equals(owner.getUUID())){contexts.remove(context);release(context);}}
    public void clear(){mainThread();while(!contexts.isEmpty())release(contexts.removeFirst());bindings.clear();}
    @Override public void close(){clear();if(repository!=null){repository.close();repository=null;}}
}
