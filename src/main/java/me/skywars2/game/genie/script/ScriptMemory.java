package me.skywars2.game.genie.script;

import com.google.gson.*;
import java.util.*;
import java.util.function.*;

/** Shared live-data budget. Conservative allocation accounting is reclaimed by a graph census. */
public final class ScriptMemory {
    public static final long DEFAULT_UNITS=1048576; // Units conservatively approximate 16 bytes.
    private record Roots(Supplier<? extends Iterable<JsonElement>> values,LongSupplier overhead,Object shared,long sharedUnits){}
    private final long limit;
    private final Map<Object,Roots> sources=new IdentityHashMap<>();
    private long estimated;
    public ScriptMemory(){this(DEFAULT_UNITS);}
    public ScriptMemory(long limit){if(limit<64)throw new IllegalArgumentException("invalid script memory budget");this.limit=limit;}
    public void track(Object owner,Supplier<? extends Iterable<JsonElement>> roots,LongSupplier overhead){
        track(owner,roots,overhead,null,0);
    }
    void track(Object owner,Supplier<? extends Iterable<JsonElement>> roots,LongSupplier overhead,Object shared,long sharedUnits){
        if(sources.containsKey(owner))throw new IllegalArgumentException("duplicate script memory owner");
        sources.put(owner,new Roots(roots,overhead,shared,sharedUnits));
        try{census();}catch(RuntimeException error){sources.remove(owner);throw error;}
    }
    public void forget(Object owner){sources.remove(owner);}
    public long limit(){return limit;}
    public void reserve(long units){
        if(units<0||units>limit)throw exhausted();
        if(estimated>limit-units)census();
        if(estimated>limit-units)throw exhausted();
        estimated+=units;
    }
    public void reserveValue(JsonElement value){reserve(ScriptValues.units(value)+1);}
    /** One reservation while every source is still rooted, before copying a whole argument list. */
    public void reserveValues(Iterable<JsonElement> values){
        long units=16;for(var value:values)units=Math.addExact(units,ScriptValues.units(value)+1);reserve(units);
    }
    public long census(){
        var seen=Collections.newSetFromMap(new IdentityHashMap<JsonElement,Boolean>());
        var shared=Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
        var values=new ArrayDeque<JsonElement>();long total=0;
        for(var source:sources.values()){
            total=add(total,source.overhead.getAsLong());
            if(source.shared!=null&&shared.add(source.shared))total=add(total,source.sharedUnits);
            for(var value:source.values.get())if(value!=null&&seen.add(value))values.addLast(value);
        }
        while(!values.isEmpty()){
            var value=values.removeLast();
            if(value.isJsonObject()){
                var object=value.getAsJsonObject();total=add(total,4+6L*object.size());
                for(var entry:object.entrySet()){
                    total=add(total,(entry.getKey().length()+7L)/8);
                    if(seen.add(entry.getValue()))values.addLast(entry.getValue());
                }
            }else if(value.isJsonArray()){
                var array=value.getAsJsonArray();total=add(total,4L+array.size());
                for(var child:array)if(seen.add(child))values.addLast(child);
            }else total=add(total,ScriptValues.scalarUnits(value));
        }
        estimated=total;return total;
    }
    private long add(long current,long units){if(units<0||current>limit-units)throw exhausted();return current+units;}
    private static IllegalArgumentException exhausted(){return new IllegalArgumentException("item script live memory budget exceeded");}
}
