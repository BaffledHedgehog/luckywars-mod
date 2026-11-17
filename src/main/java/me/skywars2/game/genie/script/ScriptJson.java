package me.skywars2.game.genie.script;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Strict JSON and a shared, Unicode-preserving digest format for local/cloud verification. */
public final class ScriptJson {
    private ScriptJson(){}
    public static JsonObject parse(String source){
        if(source.length()>4194304)throw new IllegalArgumentException("JSON input exceeds storage bounds");
        try(var reader=new JsonReader(new StringReader(source))){
            reader.setStrictness(Strictness.STRICT);
            var root=read(reader,0,new int[]{0,0});
            if(reader.peek()!=JsonToken.END_DOCUMENT||!root.isJsonObject())throw new IllegalArgumentException("one JSON object required");
            return root.getAsJsonObject();
        }catch(IOException|NumberFormatException error){throw new IllegalArgumentException("invalid script JSON",error);}
    }
    private static JsonElement read(JsonReader reader,int depth,int[] used)throws IOException{
        if(depth>128||++used[0]>262144)throw new IllegalArgumentException("JSON data exceeds storage bounds");
        return switch(reader.peek()){
            case BEGIN_OBJECT->{
                var object=new JsonObject();reader.beginObject();
                while(reader.hasNext()){
                    String key=reader.nextName();text(key,used);
                    if(object.has(key))throw new IllegalArgumentException("duplicate JSON key: "+key);
                    object.add(key,read(reader,depth+1,used));
                }
                reader.endObject();yield object;
            }
            case BEGIN_ARRAY->{
                var array=new JsonArray();reader.beginArray();while(reader.hasNext())array.add(read(reader,depth+1,used));reader.endArray();yield array;
            }
            case STRING->{String value=reader.nextString();text(value,used);yield new JsonPrimitive(value);}
            case NUMBER->{var value=new JsonPrimitive(new BigDecimal(reader.nextString()));ScriptValues.number(value);yield value;}
            case BOOLEAN->new JsonPrimitive(reader.nextBoolean());
            case NULL->{reader.nextNull();yield JsonNull.INSTANCE;}
            default->throw new IllegalArgumentException("unexpected JSON token");
        };
    }
    private static void text(String value,int[] used){
        checkText(value);used[1]+=value.length();
        if(used[1]>1048576)throw new IllegalArgumentException("JSON text exceeds storage bounds");
    }
    static void checkText(String value){
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(Character.isHighSurrogate(c)){
                if(++i>=value.length()||!Character.isLowSurrogate(value.charAt(i)))throw new IllegalArgumentException("unpaired Unicode surrogate");
            }else if(Character.isLowSurrogate(c))throw new IllegalArgumentException("unpaired Unicode surrogate");
        }
    }
    public static String canonical(JsonElement value){
        ScriptValues.check(value);var out=new StringBuilder();write(value,out);return out.toString();
    }
    public static String digest(JsonElement value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(value).getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    private static void write(JsonElement value,StringBuilder out){
        if(value.isJsonNull()){out.append("null");return;}
        if(value.isJsonObject()){
            out.append('{');boolean first=true;
            for(var entry:new TreeMap<>(value.getAsJsonObject().asMap()).entrySet()){
                if(!first)out.append(',');first=false;quote(entry.getKey(),out);out.append(':');write(entry.getValue(),out);
            }
            out.append('}');return;
        }
        if(value.isJsonArray()){
            out.append('[');boolean first=true;for(var child:value.getAsJsonArray()){if(!first)out.append(',');first=false;write(child,out);}out.append(']');return;
        }
        var primitive=value.getAsJsonPrimitive();
        if(primitive.isString())quote(primitive.getAsString(),out);
        else if(primitive.isBoolean())out.append(primitive.getAsBoolean());
        else{
            // Scientific notation keeps compact input such as 1e4096 compact in a digest.
            var number=ScriptValues.number(value);out.append(number.signum()==0?"0":number.stripTrailingZeros().toString());
        }
    }
    private static void quote(String value,StringBuilder out){
        checkText(value);out.append('"');
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            switch(c){
                case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '\n'->out.append("\\n");
                case '\r'->out.append("\\r");case '\t'->out.append("\\t");case '\b'->out.append("\\b");case '\f'->out.append("\\f");
                default->{if(c<32)out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}
            }
        }
        out.append('"');
    }
}
