package me.skywars2.game.genie.script;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** VM contracts need neither a Minecraft world nor credentials or a model request. */
public final class GenieScriptContracts {
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void rejected(Runnable run,String why){
        try{run.run();throw new AssertionError(why);}catch(IllegalArgumentException expected){}
    }
    private static ScriptProgram compile(String code){
        return ScriptProgram.compile(JsonParser.parseString(code).getAsJsonObject(),Map.of("world.command",new ScriptProgram.Signature(1,1)),Set.of("USE"));
    }
    public static void main(String[] args)throws IOException{
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
        System.out.println("GENIE_SCRIPT_CONTRACTS_PASS functions, loops, mutable state, cooperative fuel, delays, host calls, cancellation, static verifier and data bounds; no gameplay-power policy");
    }
}
