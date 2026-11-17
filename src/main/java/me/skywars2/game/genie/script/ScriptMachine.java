package me.skywars2.game.genie.script;

import com.google.gson.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Cooperative stack machine: loops, recursion, data, host calls and waits require no server reload. */
public final class ScriptMachine {
    public enum Status { RUNNING,WAITING,DONE,CANCELLED }
    public sealed interface Result permits Value,Pending {}
    public record Value(JsonElement value) implements Result{
        public Value{value=value==null?JsonNull.INSTANCE:value;}
    }
    public record Pending(CompletableFuture<JsonElement> future,Runnable cancel) implements Result{
        public Pending{Objects.requireNonNull(future);Objects.requireNonNull(cancel);}
    }
    @FunctionalInterface public interface Host { Result call(String name,List<JsonElement> arguments); }
    private static final MathContext MATH=MathContext.DECIMAL128;
    private static final class Frame {
        final String name;final ScriptProgram.Function function;final Map<String,JsonElement> locals=new HashMap<>();final int base;int pc;
        Frame(String name,ScriptProgram.Function function,List<JsonElement> arguments,int base){
            this.name=name;this.function=function;this.base=base;
            for(int i=0;i<arguments.size();i++)locals.put(function.parameters().get(i),arguments.get(i));
        }
    }
    private final ScriptProgram program;
    private final Host host;
    private final JsonObject state;
    private final ScriptMemory memory;
    private final ArrayDeque<Frame> frames=new ArrayDeque<>();
    private final ArrayList<JsonElement> operands=new ArrayList<>();
    private long wakeTick=Long.MIN_VALUE;
    private Pending pending;
    private JsonElement result=JsonNull.INSTANCE;
    private Status status=Status.RUNNING;
    private String preparedText;
    private List<JsonElement> nativeArguments=List.of();
    private long stateRevision;
    private int lastAdvanceInstructions;
    public ScriptMachine(ScriptProgram program,String entry,List<JsonElement> arguments,JsonObject state,Host host){
        this(program,entry,arguments,state,host,new ScriptMemory());
    }
    public ScriptMachine(ScriptProgram program,String entry,List<JsonElement> arguments,JsonObject state,Host host,ScriptMemory memory){
        this.program=Objects.requireNonNull(program);this.host=Objects.requireNonNull(host);this.state=Objects.requireNonNull(state);
        this.memory=Objects.requireNonNull(memory);
        for(var argument:arguments)ScriptValues.check(argument);
        ScriptValues.check(state);enter(entry,List.copyOf(arguments));
        memory.track(this,this::memoryRoots,this::memoryOverhead,program,program.memoryOverhead());
    }
    public JsonElement result(){return result;}
    public JsonObject state(){return state;}
    public Status status(){return status;}
    public long stateRevision(){return stateRevision;}
    public int lastAdvanceInstructions(){return lastAdvanceInstructions;}
    public void cancel(){
        if(status==Status.DONE||status==Status.CANCELLED)return;
        status=Status.CANCELLED;var task=pending;pending=null;frames.clear();operands.clear();preparedText=null;nativeArguments=List.of();memory.forget(this);
        if(task!=null)task.cancel.run();
    }
    private void enter(String name,List<JsonElement> arguments){
        var function=program.function(name);
        if(function==null||function.parameters().size()!=arguments.size())throw new IllegalArgumentException("invalid script function call: "+name);
        if(frames.size()>=256)throw new IllegalArgumentException("script call stack exceeds memory bounds");
        frames.addLast(new Frame(name,function,arguments,operands.size()));
    }
    /** A caller supplies its shared tick budget. Exhausting it preserves the continuation. */
    public Status advance(long tick,int fuel){
        return advance(tick,fuel,Long.MAX_VALUE);
    }
    /** The game host can share a wall-clock deadline across every active item fiber. */
    public Status advance(long tick,int fuel,long deadlineNanos){
        lastAdvanceInstructions=0;
        if(fuel<0)throw new IllegalArgumentException("negative instruction budget");
        if(status==Status.DONE||status==Status.CANCELLED)return status;
        if(tick<wakeTick){status=Status.WAITING;return status;}
        if(pending!=null){
            if(!pending.future.isDone()){status=Status.WAITING;return status;}
            var task=pending;pending=null;
            try{var value=task.future.join();value=value==null?JsonNull.INSTANCE:value;nativeArguments=List.of(value);memory.reserveValue(value);push(ScriptValues.copy(value));}
            catch(RuntimeException error){cancel();throw new IllegalArgumentException("script host continuation failed",error);}
            finally{nativeArguments=List.of();}
        }
        status=Status.RUNNING;
        while(fuel-->0&&!frames.isEmpty()){
            if(deadlineNanos!=Long.MAX_VALUE&&System.nanoTime()>=deadlineNanos)return status;
            var frame=frames.getLast();var instruction=frame.function.code().get(frame.pc++);
            lastAdvanceInstructions++;
            try{
                reserve(frame,instruction);
                switch(instruction.op()){
                    case CONST->push(ScriptValues.copy(instruction.value()));
                    case LOAD->push(instruction.text().equals("state")?state:frame.locals.getOrDefault(instruction.text(),JsonNull.INSTANCE));
                    case STORE->frame.locals.put(instruction.text(),pop());
                    case DROP->pop();
                    case DUP->push(operands.getLast());
                    case GET->{var key=pop();var target=pop();push(get(target,key));}
                    case PUT->{var value=ScriptValues.copy(pop());var key=pop();var target=pop();put(target,key,value);ScriptValues.check(state);stateRevision++;push(value);}
                    case ARRAY->{
                        var values=arguments(instruction.number());var array=new JsonArray();values.forEach(array::add);ScriptValues.check(array);push(array);
                    }
                    case OBJECT->{
                        var values=arguments(instruction.number()*2);var object=new JsonObject();
                        for(int i=0;i<values.size();i+=2)object.add(text(values.get(i)),values.get(i+1));ScriptValues.check(object);push(object);
                    }
                    case ADD,SUB,MUL,DIV,MOD,EQ,NE,LT,LE,GT,GE->binary(instruction.op());
                    case NOT->push(new JsonPrimitive(!ScriptValues.truth(pop())));
                    case NEG->push(ScriptValues.number(ScriptValues.number(pop()).negate(MATH)));
                    case JUMP->frame.pc=instruction.number();
                    case JUMP_IF_FALSE->{if(!ScriptValues.truth(pop()))frame.pc=instruction.number();}
                    case CALL->{
                        var arguments=arguments(instruction.number());
                        if(program.function(instruction.text())!=null)enter(instruction.text(),arguments);
                        else{
                            nativeArguments=arguments;
                            var call=Objects.requireNonNull(host.call(instruction.text(),List.copyOf(arguments)));
                            if(call instanceof Value immediate){memory.reserveValue(immediate.value);push(ScriptValues.copy(immediate.value));}
                            else{pending=(Pending)call;status=Status.WAITING;return status;}
                        }
                    }
                    case RETURN->{
                        var value=pop();frames.removeLast();while(operands.size()>frame.base)pop();
                        if(frames.isEmpty()){result=value;status=Status.DONE;memory.forget(this);return status;}push(value);
                    }
                    case WAIT->{
                        long ticks=ScriptValues.number(pop()).longValueExact();
                        if(ticks<0)throw new IllegalArgumentException("negative wait");
                        wakeTick=Math.addExact(tick,Math.max(1,ticks));status=Status.WAITING;return status;
                    }
                    case HALT->{frames.clear();operands.clear();status=Status.DONE;memory.forget(this);return status;}
                }
            }catch(RuntimeException error){
                try{cancel();}catch(RuntimeException cleanup){error.addSuppressed(cleanup);}
                throw new IllegalArgumentException("script "+frame.name+" at instruction "+(frame.pc-1)+": "+error.getMessage(),error);
            }finally{preparedText=null;nativeArguments=List.of();}
        }
        return status;
    }
    private List<JsonElement> memoryRoots(){
        var roots=new ArrayList<JsonElement>();roots.add(state);roots.add(result);roots.addAll(program.memoryRoots());roots.addAll(operands);roots.addAll(nativeArguments);
        for(var frame:frames)roots.addAll(frame.locals.values());
        if(pending!=null&&pending.future.isDone()&&!pending.future.isCompletedExceptionally()&&!pending.future.isCancelled()){
            var value=pending.future.getNow(JsonNull.INSTANCE);if(value!=null)roots.add(value);
        }
        return roots;
    }
    private long memoryOverhead(){
        long units=16+operands.size();
        for(var frame:frames)units+=16+4L*frame.locals.size();
        return units;
    }
    private JsonElement peek(int reverse){return operands.get(operands.size()-1-reverse);}
    /** Reserve once before consuming roots; temporary stack removals cannot hide live data. */
    private void reserve(Frame frame,ScriptProgram.Instruction instruction){
        long units=switch(instruction.op()){
            case CONST->ScriptValues.units(instruction.value())+1;
            case LOAD,DUP,GET,RETURN->1;
            case STORE->frame.locals.containsKey(instruction.text())?0:4+(instruction.text().length()+7L)/8;
            case PUT->ScriptValues.units(peek(0))+12+(peek(1).isJsonPrimitive()&&peek(1).getAsJsonPrimitive().isString()?(peek(1).getAsString().length()+7L)/8:0);
            case ARRAY->5L+instruction.number();
            case OBJECT->{
                long total=5+8L*instruction.number();
                for(int i=0;i<instruction.number();i++)total+=(text(peek(i*2+1)).length()+7L)/8;
                yield total;
            }
            case ADD->{
                if(isText(peek(0))||isText(peek(1))){
                    preparedText=string(peek(1))+string(peek(0));
                    if(preparedText.length()>1048576)throw new IllegalArgumentException("text exceeds memory bounds");
                    yield 5+(preparedText.length()+7L)/8;
                }
                yield 16;
            }
            case SUB,MUL,DIV,MOD,EQ,NE,LT,LE,GT,GE,NEG,NOT->16;
            case CALL->16+4L*instruction.number();
            default->0;
        };
        memory.reserve(units);
    }
    private JsonElement pop(){
        var frame=frames.getLast();if(operands.size()<=frame.base)throw new IllegalArgumentException("operand stack underflow");
        return operands.removeLast();
    }
    private void push(JsonElement value){
        if(operands.size()>=65536)throw new IllegalArgumentException("operand stack exceeds memory bounds");
        operands.add(value==null?JsonNull.INSTANCE:value);
    }
    private List<JsonElement> arguments(int count){
        var values=new ArrayList<JsonElement>(count);for(int i=0;i<count;i++)values.add(pop());Collections.reverse(values);return values;
    }
    private static String text(JsonElement value){
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString())throw new IllegalArgumentException("text expected");
        return value.getAsString();
    }
    private static JsonElement get(JsonElement target,JsonElement key){
        if(target.isJsonObject()){var value=target.getAsJsonObject().get(text(key));return value==null?JsonNull.INSTANCE:value;}
        if(target.isJsonArray()){
            int index=ScriptValues.number(key).intValueExact();var array=target.getAsJsonArray();
            return index>=0&&index<array.size()?array.get(index):JsonNull.INSTANCE;
        }
        if(target.isJsonNull())return JsonNull.INSTANCE;
        throw new IllegalArgumentException("collection expected");
    }
    private static void put(JsonElement target,JsonElement key,JsonElement value){
        if(target.isJsonObject())target.getAsJsonObject().add(text(key),value);
        else if(target.isJsonArray()){
            int index=ScriptValues.number(key).intValueExact();var array=target.getAsJsonArray();
            if(index<0||index>array.size())throw new IllegalArgumentException("array index outside value");
            if(index==array.size())array.add(value);else array.set(index,value);
        }else throw new IllegalArgumentException("mutable collection expected");
        ScriptValues.check(target);
    }
    private void binary(ScriptProgram.Op op){
        var right=pop();var left=pop();
        if(op==ScriptProgram.Op.EQ||op==ScriptProgram.Op.NE){
            boolean equal=left.equals(right);push(new JsonPrimitive(op==ScriptProgram.Op.EQ?equal:!equal));return;
        }
        if(op==ScriptProgram.Op.ADD&&(isText(left)||isText(right))){
            push(new JsonPrimitive(preparedText));return;
        }
        var a=ScriptValues.number(left);var b=ScriptValues.number(right);
        push(switch(op){
            case ADD->ScriptValues.number(a.add(b,MATH));case SUB->ScriptValues.number(a.subtract(b,MATH));
            case MUL->ScriptValues.number(a.multiply(b,MATH));case DIV->ScriptValues.number(a.divide(b,MATH));
            case MOD->ScriptValues.number(a.remainder(b,MATH));
            case LT->new JsonPrimitive(a.compareTo(b)<0);case LE->new JsonPrimitive(a.compareTo(b)<=0);
            case GT->new JsonPrimitive(a.compareTo(b)>0);case GE->new JsonPrimitive(a.compareTo(b)>=0);
            default->throw new IllegalArgumentException("invalid binary operator");
        });
    }
    private static boolean isText(JsonElement value){return value.isJsonPrimitive()&&value.getAsJsonPrimitive().isString();}
    private static String string(JsonElement value){return isText(value)?value.getAsString():value.toString();}
}
