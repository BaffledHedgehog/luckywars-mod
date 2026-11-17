package me.skywars2.game.genie.script;

import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;

/** Data and allocation limits protect the tick and heap; they do not rate an item's power. */
final class ScriptValues {
    private ScriptValues(){}
    private record Visit(JsonElement value,int depth,boolean exit){}
    static void check(JsonElement value){
        var queue=new ArrayDeque<Visit>();queue.add(new Visit(value,0,false));int nodes=0,characters=0;
        var active=Collections.newSetFromMap(new IdentityHashMap<JsonElement,Boolean>());
        while(!queue.isEmpty()){
            var entry=queue.removeLast();var current=entry.value;int depth=entry.depth;
            if(entry.exit){active.remove(current);continue;}
            if(++nodes>262144||depth>128)throw new IllegalArgumentException("script data exceeds memory bounds");
            if(current.isJsonObject()||current.isJsonArray()){
                if(!active.add(current))throw new IllegalArgumentException("cyclic script value");
                queue.add(new Visit(current,depth,true));
                if(current.isJsonObject())for(var child:current.getAsJsonObject().entrySet()){
                    ScriptJson.checkText(child.getKey());characters+=child.getKey().length();queue.add(new Visit(child.getValue(),depth+1,false));
                }else for(var child:current.getAsJsonArray())queue.add(new Visit(child,depth+1,false));
            }else if(current.isJsonPrimitive()){
                var primitive=current.getAsJsonPrimitive();
                if(primitive.isString()){ScriptJson.checkText(primitive.getAsString());characters+=primitive.getAsString().length();}
                if(primitive.isNumber())number(current);
            }
            if(characters>1048576)throw new IllegalArgumentException("script text exceeds memory bounds");
        }
    }
    static JsonElement copy(JsonElement value){check(value);return value.deepCopy();}
    static BigDecimal number(JsonElement value){
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("numeric operand expected");
        var number=value.getAsBigDecimal();
        if(number.precision()>4096||Math.abs((long)number.scale())>4096)throw new IllegalArgumentException("numeric storage exceeds memory bounds");
        return number;
    }
    static JsonElement number(BigDecimal value){var result=new JsonPrimitive(value);number(result);return result;}
    static boolean truth(JsonElement value){
        if(value==null||value.isJsonNull())return false;
        if(value.isJsonPrimitive()){
            var p=value.getAsJsonPrimitive();
            if(p.isBoolean())return p.getAsBoolean();
            if(p.isNumber())return number(value).signum()!=0;
            return !p.getAsString().isEmpty();
        }
        return true;
    }
}
