package me.skywars2.game.genie.script;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

/** VM contracts need neither a Minecraft world nor credentials or a model request. */
public final class GenieScriptContracts {
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void rejected(Runnable run,String why){
        try{run.run();throw new AssertionError(why);}catch(IllegalArgumentException expected){}
    }
    private static ScriptProgram compile(String code){
        return ScriptProgram.compile(JsonParser.parseString(code).getAsJsonObject(),Map.of("world.command",new ScriptProgram.Signature(1,1)),Set.of("USE"));
    }
    private static void memoryContracts(){
        var spin=compile("""
            {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[["label","loop"],["jump","loop"]]}}}
            """);
        ScriptMachine.Host unused=(name,values)->{throw new AssertionError("unexpected native call");};
        var shared=new ScriptMemory(1800);var live=new ArrayList<ScriptMachine>();boolean exhausted=false;
        for(int i=0;i<20;i++){
            var state=new JsonObject();state.addProperty("text","x".repeat(3000));
            try{live.add(new ScriptMachine(spin,"main",List.of(new JsonObject()),state,unused,shared));}
            catch(IllegalArgumentException expected){exhausted=true;break;}
        }
        require(exhausted&&live.size()>1,"individually valid fibers must share one aggregate live-data bound");
        live.removeFirst().cancel();var replacement=new JsonObject();replacement.addProperty("text","y".repeat(3000));
        live.add(new ScriptMachine(spin,"main",List.of(new JsonObject()),replacement,unused,shared));
        for(var fiber:live)fiber.cancel();require(shared.census()==0,"cancelled fibers release their state, frames and shared program");
        var transientSource=ScriptJson.parse("""
            {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[["label","loop"],["const",[]],["drop"],["jump","loop"]]}}}
            """);
        var data=new JsonArray();for(int i=0;i<200;i++)data.add(i);
        transientSource.getAsJsonObject("functions").getAsJsonObject("main").getAsJsonArray("code").get(1).getAsJsonArray().set(1,data);
        var churn=new ScriptMachine(ScriptProgram.compile(transientSource,Map.of(),Set.of("USE")),"main",List.of(new JsonObject()),new JsonObject(),unused,new ScriptMemory(3000));
        require(churn.advance(0,9000)==ScriptMachine.Status.RUNNING,"discarded temporary values must be reclaimed instead of accumulating an allocation quota");
        churn.cancel();
        var pendingCode=compile("""
            {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[["const","x"],["call","world.command",1],["return"]]}}}
            """);
        var future=new CompletableFuture<JsonElement>();var memory=new ScriptMemory(512);
        var pending=new ScriptMachine(pendingCode,"main",List.of(new JsonObject()),new JsonObject(),
            (name,values)->new ScriptMachine.Pending(future,()->future.cancel(false)),memory);
        pending.advance(0,20);var oversized=new JsonObject();oversized.addProperty("text","x".repeat(8000));future.complete(oversized);
        rejected(()->pending.advance(1,20),"a native continuation bypassed the aggregate memory bound");
        require(pending.status()==ScriptMachine.Status.CANCELLED&&memory.census()==0,"oversized native output cancels only its fiber and releases it");
        var nativePool=new ScriptMemory(10000);long[] during={0};
        var argumentSource=transientSource.deepCopy();var code=argumentSource.getAsJsonObject("functions").getAsJsonObject("main").getAsJsonArray("code");
        code.remove(0);code.remove(1);code.remove(1);code.add(JsonParser.parseString("[\"call\",\"world.command\",1]"));code.add(JsonParser.parseString("[\"return\"]"));
        var argumentMachine=new ScriptMachine(ScriptProgram.compile(argumentSource,Map.of("world.command",new ScriptProgram.Signature(1,1)),Set.of("USE")),"main",List.of(new JsonObject()),new JsonObject(),
            (name,values)->{during[0]=nativePool.census();return new ScriptMachine.Value(JsonNull.INSTANCE);},nativePool);
        long before=nativePool.census();argumentMachine.advance(0,20);
        require(during[0]>before+150,"native argument copies must remain rooted while a host call takes its own census");
        require(nativePool.census()==0,"completed fibers release shared code and continuation roots");
    }
    private static void checkpointContracts(){
        var program = compile("""
            {"version":1,"state":{"nested":{"counter":0}},"handlers":{"USE":"main"},"functions":{
            "waiter":{"parameters":["alias"],"code":[["const",5],["wait"],["load","alias"],["const","counter"],["const",42],["put"],["return"]]},
            "main":{"parameters":["event"],"code":[["load","state"],["const","nested"],["get"],["dup"],["call","waiter",1],["drop"],["const","counter"],["get"],["return"]]}}}
            """);
        ScriptMachine.Host host = (name,args) -> { throw new AssertionError("unexpected native call"); };
        var state = program.initialState();
        var machine = new ScriptMachine(program,"main",List.of(new JsonObject()),state,host);
        require(machine.advance(10,100)==ScriptMachine.Status.WAITING,"checkpoint captures wait inside a nested call");
        var graph = new ScriptGraphCopy();
        var savedState = graph.copy(state);
        var image = machine.checkpoint(graph,pending -> { throw new AssertionError("unexpected deferred host call"); });
        require(machine.advance(15,100)==ScriptMachine.Status.DONE&&state.getAsJsonObject("nested").get("counter").getAsInt()==42,"future mutates live state only");
        for (int repeat=0; repeat<2; repeat++) {
            var copies = new ScriptGraphCopy();
            var restoredState = copies.copy(savedState).getAsJsonObject();
            var restored = image.restore(restoredState,copies,host,new ScriptMemory());
            require(restored.advance(14,100)==ScriptMachine.Status.WAITING&&restoredState.getAsJsonObject("nested").get("counter").getAsInt()==0,
                "reusable image retains the saved clock and independent mutable state");
            require(restored.advance(15,100)==ScriptMachine.Status.DONE&&restored.result().getAsInt()==42
                &&restoredState.getAsJsonObject("nested").get("counter").getAsInt()==42,
                "operand, nested frame local and item state all keep the same restored alias");
        }
    }

    private static void repositoryContracts()throws Exception{
        Files.createDirectories(Path.of("build/verification"));var root=Files.createTempDirectory(Path.of("build/verification"),"genie-code-contracts-").toAbsolutePath();
        String source="""
            {"version":1,"state":{"counter":7},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[["load","state"],["const","counter"],["get"],["return"]]}}}
            """;
        String id;
        try{
            try(var repository=new ScriptRepository(root,Map.of(),Set.of("USE"),1024)){
                var ready=repository.prepare(source).get(10,TimeUnit.SECONDS);id=ready.id();
                require(Files.isRegularFile(root.resolve(id+".json.gz")),"granted code must already exist independently of the process cache");
                require(id.equals(ScriptJson.digest(ScriptJson.parse(source))),"code identity comes from canonical program contents");
                var first=ready.program().initialState();first.addProperty("counter",99);
                require(ready.program().initialState().get("counter").getAsInt()==7,"item copies start with independent mutable state");
            }
            try(var reopened=new ScriptRepository(root,Map.of(),Set.of("USE"),1024)){
                var restored=reopened.load(id).get(10,TimeUnit.SECONDS);
                var fiber=new ScriptMachine(restored.program(),"main",List.of(new JsonObject()),restored.program().initialState(),(name,values)->{throw new AssertionError();});
                require(fiber.advance(0,100)==ScriptMachine.Status.DONE&&fiber.result().getAsInt()==7,"compressed code survives cache eviction and reopening");
                require(reopened.load("../outside").handle((value,error)->error!=null).get(10,TimeUnit.SECONDS),"item hashes cannot escape the code directory");
            }
            byte[] valid=Files.readAllBytes(root.resolve(id+".json.gz"));
            var other=ScriptJson.parse(source);other.getAsJsonObject("state").addProperty("counter",8);
            try(var gzip=new java.util.zip.GZIPOutputStream(Files.newOutputStream(root.resolve(id+".json.gz")))){gzip.write(ScriptJson.canonical(other).getBytes(StandardCharsets.UTF_8));}
            try(var reopened=new ScriptRepository(root,Map.of(),Set.of("USE"),1024)){
                require(reopened.load(id).handle((value,error)->error!=null).get(10,TimeUnit.SECONDS),"another technically valid program cannot silently replace a referenced hash");
                reopened.prepare(source).get(10,TimeUnit.SECONDS);
                require(Arrays.equals(valid,Files.readAllBytes(root.resolve(id+".json.gz"))),"preparing identical contents repairs only their own code file");
            }
            var closed=new ScriptRepository(root,Map.of(),Set.of("USE"));closed.close();
            require(closed.prepare(source).handle((value,error)->error!=null).get(10,TimeUnit.SECONDS),"a stopped repository must not accept another item generation");
        }finally{
            try(var paths=Files.walk(root)){for(var path:paths.sorted(Comparator.reverseOrder()).toList()){
                require(path.toAbsolutePath().normalize().startsWith(root),"fixture cleanup escaped its own directory");Files.deleteIfExists(path);
            }}
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length==1&&args[0].equals("--contract")){
            System.out.println(ScriptJson.canonical(ScriptProgram.contract(Map.of("world.command",new ScriptProgram.Signature(1,1)),Set.of("USE"))));return;
        }
        if(args.length==1&&args[0].equals("--verify-lines")){
            try(var reader=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))){
                for(String line;(line=reader.readLine())!=null;){
                    var report=new JsonObject();
                    try{var source=ScriptJson.parse(line);ScriptProgram.compile(source,Map.of("world.command",new ScriptProgram.Signature(1,1)),Set.of("USE"));
                        report.addProperty("ok",true);report.addProperty("program_sha256",ScriptJson.digest(source));
                    }catch(IllegalArgumentException invalid){report.addProperty("ok",false);report.addProperty("error",invalid.getMessage());}
                    System.out.println(report);
                }
            }
            return;
        }
        String program="""
            {"version":1,"state":{"counter":0},"handlers":{"USE":"main"},"functions":{
              "double":{"parameters":["n"],"code":[["load","n"],["const",2],["mul"],["return"]]},
              "main":{"parameters":["event"],"code":[
                ["const",0],["store","i"],
                ["label","again"],["load","i"],["const",10],["lt"],["jump_if_false","finished"],
                ["load","state"],["const","counter"],["load","i"],["call","double",1],["put"],["drop"],
                ["load","i"],["const",1],["add"],["store","i"],["jump","again"],
                ["label","finished"],["const",3],["wait"],
                ["const","particle minecraft:end_rod ~ ~ ~ 0 0 0 0 1"],["call","world.command",1],["return"]
              ]}}}""";
        var script=compile(program);var commands=new ArrayList<String>();var state=script.initialState();
        var machine=new ScriptMachine(script,script.handler("USE"),List.of(new JsonObject()),state,(name,values)->{
            commands.add(values.getFirst().getAsString());return new ScriptMachine.Value(new JsonPrimitive(1));
        });
        int slices=0;while(machine.advance(20,7)==ScriptMachine.Status.RUNNING)require(++slices<1000,"loop terminates");
        require(slices>1&&machine.status()==ScriptMachine.Status.WAITING,"fuel yields retain loop and function continuations");
        require(state.get("counter").getAsInt()==18&&commands.isEmpty(),"functions, loop and mutable state execute before wait");
        require(machine.advance(22,100)==ScriptMachine.Status.WAITING&&commands.isEmpty(),"wait uses caller's clock without blocking");
        require(machine.advance(23,100)==ScriptMachine.Status.DONE&&commands.size()==1&&machine.result().getAsInt()==1,"resumed native call and return");
        var endless=compile("""
            {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[["label","again"],["jump","again"]]}}}
            """);
        var loop=new ScriptMachine(endless,"main",List.of(new JsonObject()),new JsonObject(),(name,values)->{throw new AssertionError();});
        for(int i=0;i<100;i++)require(loop.advance(i,100)==ScriptMachine.Status.RUNNING,"valid infinite code cooperatively yields");
        loop.cancel();require(loop.advance(1000,100)==ScriptMachine.Status.CANCELLED,"cancelled code cannot restart");
        rejected(()->compile(program.replace("[\"load\",\"i\"],[\"const\",10]","[\"load\",\"missing\"],[\"const\",10]")),"unknown variable accepted");
        rejected(()->compile(program.replace("\"double\",1","\"undefined_function\",1")),"unknown function accepted");
        rejected(()->compile(program.replace("[\"call\",\"double\",1]","[\"call\",\"double\",2]")),"wrong function arity accepted");
        rejected(()->compile(program.replace("\"jump\",\"again\"","\"jump\",\"unknown\"")),"unknown label accepted");
        rejected(()->compile(program.replace("[\"const\",0],[\"store\",\"i\"]","[\"store\",\"i\"]")),"stack underflow accepted");
        String pendingProgram="""
            {"version":1,"state":{},"handlers":{"USE":"main"},"functions":{"main":{"parameters":["event"],"code":[["const","say test"],["call","world.command",1],["return"]]}}}
            """;
        var completion=new CompletableFuture<JsonElement>();int[] cancelled={0};
        var pending=new ScriptMachine(compile(pendingProgram),"main",List.of(new JsonObject()),new JsonObject(),(name,values)->new ScriptMachine.Pending(completion,()->cancelled[0]++));
        require(pending.advance(0,100)==ScriptMachine.Status.WAITING,"asynchronous host work never blocks the VM caller");
        completion.complete(new JsonPrimitive(77));require(pending.advance(1,100)==ScriptMachine.Status.DONE&&pending.result().getAsInt()==77,"asynchronous host result resumes exactly once");
        var abandoned=new ScriptMachine(compile(pendingProgram),"main",List.of(new JsonObject()),new JsonObject(),(name,values)->new ScriptMachine.Pending(new CompletableFuture<>(),()->cancelled[0]++));
        abandoned.advance(0,100);abandoned.cancel();require(cancelled[0]==1,"owned host work cancelled once");
        var failedFuture=new CompletableFuture<JsonElement>();
        var failed=new ScriptMachine(compile(pendingProgram),"main",List.of(new JsonObject()),new JsonObject(),
            (name,values)->new ScriptMachine.Pending(failedFuture,()->{throw new AssertionError("completed host work must not be cancelled");}));
        failed.advance(0,100);failedFuture.completeExceptionally(new IllegalStateException("host failed"));
        rejected(()->failed.advance(1,100),"failed asynchronous host accepted");
        require(failed.status()==ScriptMachine.Status.CANCELLED&&failed.advance(2,100)==ScriptMachine.Status.CANCELLED,"failed continuation cannot resume past the failure");
        int[] attempts={0};
        var broken=new ScriptMachine(compile(pendingProgram),"main",List.of(new JsonObject()),new JsonObject(),(name,values)->{
            attempts[0]++;throw new IllegalStateException("native call failed");
        });
        require(broken.advance(0,100,System.nanoTime()-1)==ScriptMachine.Status.RUNNING&&attempts[0]==0,"expired shared deadline yields before native side effects");
        rejected(()->broken.advance(1,100),"failed native call accepted");
        require(attempts[0]==1&&broken.advance(2,100)==ScriptMachine.Status.CANCELLED,"native call failure cancels the fiber and is not replayed");
        var cyclic=new JsonObject();cyclic.add("self",cyclic);rejected(()->ScriptValues.copy(cyclic),"cyclic data accepted");
        var shared=new JsonArray();JsonElement value=new JsonObject();
        for(int i=0;i<20;i++){var pair=new JsonArray();pair.add(value);pair.add(value);value=pair;}
        shared.add(value);rejected(()->ScriptValues.copy(shared),"exponentially expanding aliases accepted");
        // Technical validation must not impose the previous gameplay damage16 or budget48.
        var high=compile(pendingProgram.replace("say test","damage @e[limit=1] 1000000"));
        require(high.handler("USE").equals("main"),"valid high-power command was judged by the syntax handler");
        rejected(()->ScriptJson.parse("{\"version\":1,\"version\":1}"),"duplicate JSON keys accepted");
        rejected(()->ScriptJson.parse("{'value':1}"),"non-JSON syntax accepted");
        rejected(()->ScriptJson.parse("{\"value\":NaN}"),"non-finite number accepted");
        rejected(()->ScriptJson.parse("{\"value\":\"\\ud800\"}"),"unpaired Unicode surrogate accepted");
        require(ScriptJson.canonical(ScriptJson.parse("{\"z\":-0.00,\"a\":\"ё\\n😀\",\"n\":1.2300e2}")).equals("{\"a\":\"ё\\n😀\",\"n\":123,\"z\":0}"),
            "canonical JSON preserves Unicode and numeric values");
        memoryContracts();
        checkpointContracts();
        repositoryContracts();
        System.out.println("GENIE_SCRIPT_CONTRACTS_PASS functions, loops, mutable state, cooperative fuel, delays, host calls, cancellation, static verifier and aggregate live memory; no gameplay-power policy");
    }
}
