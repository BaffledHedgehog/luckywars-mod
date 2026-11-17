package me.skywars2.game.genie.script;

import com.google.gson.*;
import java.util.*;

/**
 * Item-local code. Validation concerns syntax, references and stack shape, never wish balance.
 * Symbolic labels and named functions are compiled before the program can perform any action.
 */
public final class ScriptProgram {
    public static final int VERSION=1;
    public record Signature(int minimum,int maximum){
        public Signature{if(minimum<0||maximum<minimum)throw new IllegalArgumentException("invalid API arity");}
        public boolean accepts(int count){return count>=minimum&&count<=maximum;}
    }
    public enum Op {
        CONST,LOAD,STORE,DROP,DUP,GET,PUT,ARRAY,OBJECT,
        ADD,SUB,MUL,DIV,MOD,EQ,NE,LT,LE,GT,GE,NOT,NEG,
        JUMP,JUMP_IF_FALSE,CALL,RETURN,WAIT,HALT;
        public int width(){return switch(this){
            case CONST,LOAD,STORE,ARRAY,OBJECT,JUMP,JUMP_IF_FALSE->2;case CALL->3;default->1;
        };}
        public int needed(int count){return switch(this){
            case STORE,DROP,DUP,NOT,NEG,JUMP_IF_FALSE,RETURN,WAIT->1;
            case GET,ADD,SUB,MUL,DIV,MOD,EQ,NE,LT,LE,GT,GE->2;
            case PUT->3;case CALL,ARRAY->count;case OBJECT->count*2;default->0;
        };}
        public int added(){return switch(this){
            case CONST,LOAD,GET,PUT,ARRAY,OBJECT,ADD,SUB,MUL,DIV,MOD,EQ,NE,LT,LE,GT,GE,NOT,NEG,CALL->1;
            case DUP->2;default->0;
        };}
    }
    public record Instruction(Op op,String text,JsonElement value,int number){}
    public record Function(List<String> parameters,List<Instruction> code){
        public Function{parameters=List.copyOf(parameters);code=List.copyOf(code);}
    }
    private final Map<String,Function> functions;
    private final Map<String,String> handlers;
    private final Map<String,Signature> api;
    private final JsonObject initialState;
    private final List<JsonElement> memoryRoots;
    private final long memoryOverhead;
    private ScriptProgram(Map<String,Function> functions,Map<String,String> handlers,Map<String,Signature> api,JsonObject initialState){
        this.functions=Map.copyOf(functions);this.handlers=Map.copyOf(handlers);this.api=Map.copyOf(api);this.initialState=initialState.deepCopy();
        var roots=new ArrayList<JsonElement>();roots.add(this.initialState);long overhead=16;
        for(var entry:this.functions.entrySet()){
            overhead+=8+(entry.getKey().length()+7L)/8;
            for(var parameter:entry.getValue().parameters())overhead+=4+(parameter.length()+7L)/8;
            for(var instruction:entry.getValue().code()){
                overhead+=8;if(instruction.text()!=null)overhead+=(instruction.text().length()+7L)/8;
                if(instruction.value()!=null)roots.add(instruction.value());
            }
        }
        memoryRoots=List.copyOf(roots);memoryOverhead=overhead;
    }
    public Function function(String name){return functions.get(name);}
    public Signature signature(String name){return api.get(name);}
    public String handler(String event){return handlers.get(event);}
    public JsonObject initialState(){return initialState.deepCopy();}
    List<JsonElement> memoryRoots(){return memoryRoots;}
    long memoryOverhead(){return memoryOverhead;}
    /** Export the actual compiler contract for the cloud verifier, never a second handwritten schema. */
    public static JsonObject contract(Map<String,Signature> api,Set<String> events){
        var root=new JsonObject();root.addProperty("language","luckywars-item-script");root.addProperty("version",VERSION);
        root.addProperty("canonical","unicode-utf16-bigdecimal-v2");
        var limits=new JsonObject();
        limits.addProperty("functions",256);limits.addProperty("parameters",64);limits.addProperty("instructions",65536);
        limits.addProperty("stack",65536);limits.addProperty("collection",65536);
        limits.addProperty("nodes",262144);limits.addProperty("depth",128);limits.addProperty("characters",1048576);
        limits.addProperty("number_precision",4096);limits.addProperty("number_scale",4096);
        root.add("limits",limits);var operations=new JsonObject();
        for(var op:Op.values()){
            var definition=new JsonObject();definition.addProperty("width",op.width());definition.addProperty("pushes",op.added());
            if(op==Op.CALL||op==Op.ARRAY)definition.addProperty("pops","n");
            else if(op==Op.OBJECT)definition.addProperty("pops","2n");
            else definition.addProperty("pops",op.needed(0));
            operations.add(op.name(),definition);
        }
        root.add("opcodes",operations);var calls=new JsonObject();
        new TreeMap<>(api).forEach((id,signature)->{
            var definition=new JsonObject();definition.addProperty("minimum",signature.minimum());definition.addProperty("maximum",signature.maximum());calls.add(id,definition);
        });
        root.add("api",calls);var inputs=new JsonArray();new TreeSet<>(events).forEach(inputs::add);root.add("events",inputs);
        return root;
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
    private static String name(JsonElement value){
        require(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString(),"identifier must be text");
        String name=value.getAsString();require(name.matches("[A-Za-z_][A-Za-z0-9_.:]*")&&name.length()<=128,"invalid identifier");return name;
    }
    private static int integer(JsonElement value){
        require(value!=null&&value.isJsonPrimitive()&&value.getAsJsonPrimitive().isNumber(),"integer expected");
        try{return value.getAsBigDecimal().intValueExact();}catch(ArithmeticException error){throw new IllegalArgumentException("integer outside native range");}
    }
    public static ScriptProgram compile(JsonObject source,Map<String,Signature> api,Set<String> events){
        require(source.keySet().equals(Set.of("version","state","handlers","functions")),"unknown or missing program fields");
        require(integer(source.get("version"))==VERSION,"unsupported program version");
        require(source.get("state").isJsonObject()&&source.get("handlers").isJsonObject()&&source.get("functions").isJsonObject(),"program sections must be objects");
        ScriptValues.check(source);
        var definitions=source.getAsJsonObject("functions");require(!definitions.isEmpty()&&definitions.size()<=256,"function table exceeds memory bounds");
        var arities=new HashMap<String,Signature>();var parameters=new HashMap<String,List<String>>();
        for(var definition:definitions.entrySet()){
            String id=name(new JsonPrimitive(definition.getKey()));require(!api.containsKey(id),"function shadows native API");
            require(definition.getValue().isJsonObject(),"function must be an object");var fn=definition.getValue().getAsJsonObject();
            require(fn.keySet().equals(Set.of("parameters","code"))&&fn.get("parameters").isJsonArray()&&fn.get("code").isJsonArray(),"invalid function fields");
            var params=new ArrayList<String>();for(var parameter:fn.getAsJsonArray("parameters"))params.add(name(parameter));
            require(new HashSet<>(params).size()==params.size()&&!params.contains("state"),"duplicate or reserved parameter");
            require(params.size()<=64,"function argument table exceeds memory bounds");
            parameters.put(id,List.copyOf(params));arities.put(id,new Signature(params.size(),params.size()));
        }
        var all=new HashMap<>(api);all.putAll(arities);var compiled=new LinkedHashMap<String,Function>();int total=0;
        for(var definition:definitions.entrySet()){
            String id=definition.getKey();var instructions=definition.getValue().getAsJsonObject().getAsJsonArray("code");
            total+=instructions.size();require(total<=65536,"program instruction table exceeds memory bounds");
            compiled.put(id,compileFunction(parameters.get(id),instructions,all));
        }
        var handlers=new LinkedHashMap<String,String>();
        for(var binding:source.getAsJsonObject("handlers").entrySet()){
            require(events.contains(binding.getKey()),"unknown item event: "+binding.getKey());
            String fn=name(binding.getValue());require(arities.containsKey(fn)&&arities.get(fn).accepts(1),"event handler must accept one event argument");
            handlers.put(binding.getKey(),fn);
        }
        require(!handlers.isEmpty(),"program has no event entry point");
        return new ScriptProgram(compiled,handlers,api,source.getAsJsonObject("state"));
    }
    private static Function compileFunction(List<String> parameters,JsonArray source,Map<String,Signature> calls){
        var labels=new HashMap<String,Integer>();var code=new ArrayList<Instruction>();var locals=new HashSet<>(parameters);locals.add("state");
        for(var value:source){
            require(value.isJsonArray()&&!value.getAsJsonArray().isEmpty(),"instruction must be a nonempty array");var row=value.getAsJsonArray();
            String operation=name(row.get(0)).toUpperCase(Locale.ROOT);
            if(operation.equals("LABEL")){
                require(row.size()==2,"LABEL requires one identifier");String label=name(row.get(1));
                require(labels.putIfAbsent(label,code.size())==null,"duplicate label: "+label);continue;
            }
            Op op;try{op=Op.valueOf(operation);}catch(IllegalArgumentException unknown){throw new IllegalArgumentException("unknown instruction: "+operation);}
            int width=op.width();
            require(row.size()==width,"wrong instruction arity: "+op);String text=null;JsonElement constant=null;int number=0;
            switch(op){
                case CONST->constant=row.get(1).deepCopy();
                case LOAD,STORE,JUMP,JUMP_IF_FALSE,CALL->text=name(row.get(1));
                case ARRAY,OBJECT->{number=integer(row.get(1));require(number>=0&&number<=65536,"collection exceeds memory bounds");}
                default->{}
            }
            if(op==Op.STORE){require(!text.equals("state"),"cannot replace state root");locals.add(text);}
            if(op==Op.CALL){
                number=integer(row.get(2));var signature=calls.get(text);
                require(signature!=null&&signature.accepts(number),"unknown call or invalid argument count: "+text);
            }
            code.add(new Instruction(op,text,constant,number));
        }
        code.add(new Instruction(Op.CONST,null,JsonNull.INSTANCE,0));code.add(new Instruction(Op.RETURN,null,null,0));
        for(int i=0;i<code.size();i++){
            var instruction=code.get(i);
            if(instruction.op==Op.LOAD)require(locals.contains(instruction.text),"undeclared variable: "+instruction.text);
            if(instruction.op==Op.JUMP||instruction.op==Op.JUMP_IF_FALSE){
                var target=labels.get(instruction.text);require(target!=null,"unknown label: "+instruction.text);
                code.set(i,new Instruction(instruction.op,instruction.text,null,target));
            }
        }
        checkStack(code);
        return new Function(parameters,code);
    }
    private static void checkStack(List<Instruction> code){
        var heights=new HashMap<Integer,Integer>();var pending=new ArrayDeque<int[]>();pending.add(new int[]{0,0});
        while(!pending.isEmpty()){
            var position=pending.removeFirst();int pc=position[0],height=position[1];require(pc>=0&&pc<code.size(),"jump outside function");
            var old=heights.putIfAbsent(pc,height);if(old!=null){require(old==height,"inconsistent operand stack at control-flow merge");continue;}
            var instruction=code.get(pc);int needed=instruction.op.needed(instruction.number);
            require(height>=needed,"operand stack underflow at "+pc);
            int added=instruction.op.added();
            int next=height-needed+added;require(next<=65536,"operand stack exceeds memory bounds");
            if(instruction.op==Op.RETURN||instruction.op==Op.HALT)continue;
            if(instruction.op==Op.JUMP||instruction.op==Op.JUMP_IF_FALSE)pending.add(new int[]{instruction.number,next});
            if(instruction.op!=Op.JUMP)pending.add(new int[]{pc+1,next});
        }
    }
}
