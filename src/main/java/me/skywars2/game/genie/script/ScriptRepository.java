package me.skywars2.game.genie.script;

import com.google.gson.JsonObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

/** Item NBT carries a content hash, not a copy of the entire program on every inventory update. */
public final class ScriptRepository implements AutoCloseable {
    public record Loaded(String id,ScriptProgram program){}
    private record Cached(Loaded value,long weight){}
    private static final int MAX_BYTES=16*1024*1024;
    private final Path root;
    private final Map<String,ScriptProgram.Signature> api;
    private final Set<String> events;
    private final long cacheLimit;
    private final ExecutorService worker;
    private final Map<String,Cached> cache=new LinkedHashMap<>(16,.75f,true);
    private final Map<String,CompletableFuture<Loaded>> loading=new HashMap<>();
    private final Set<CompletableFuture<Loaded>> requests=new HashSet<>();
    private long cacheWeight;
    private boolean closed;
    public ScriptRepository(Path root,Map<String,ScriptProgram.Signature> api,Set<String> events){this(root,api,events,16L*1024*1024);}
    public ScriptRepository(Path root,Map<String,ScriptProgram.Signature> api,Set<String> events,long cacheLimit){
        this.root=root.toAbsolutePath().normalize();this.api=Map.copyOf(api);this.events=Set.copyOf(events);
        if(cacheLimit<1024)throw new IllegalArgumentException("invalid code cache size");this.cacheLimit=cacheLimit;
        worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(16),task->{var thread=new Thread(task,"skywars2-genie-code");thread.setDaemon(true);thread.setPriority(Thread.MIN_PRIORITY);return thread;});
    }
    /** Successful completion means code is technically compiled AND durably stored before granting it. */
    public CompletableFuture<Loaded> prepare(String source){
        if(source==null||source.length()>4194304)return CompletableFuture.failedFuture(new IllegalArgumentException("item code exceeds input bounds"));
        return submit(()->{
            var json=ScriptJson.parse(source);var program=ScriptProgram.compile(json,api,events);
            String canonical=ScriptJson.canonical(json),id=ScriptJson.digest(json);Path target=path(id),temporary=null;
            try{
                Files.createDirectories(root);temporary=Files.createTempFile(root,id+"-",".tmp");
                try(var output=new GZIPOutputStream(Files.newOutputStream(temporary))){output.write(canonical.getBytes(StandardCharsets.UTF_8));}
                try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException unsupported){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
                var loaded=new Loaded(id,program);remember(loaded,canonical.length());return loaded;
            }catch(IOException error){throw new CompletionException("cannot persist item code",error);}
            finally{if(temporary!=null)try{Files.deleteIfExists(temporary);}catch(IOException ignored){}}
        });
    }
    /** No file operation on the game thread; callers may retain/replay an event until this completes. */
    public synchronized CompletableFuture<Loaded> load(String id){
        if(closed)return CompletableFuture.failedFuture(new IllegalStateException("code repository is closed"));
        try{path(id);}catch(IllegalArgumentException invalid){return CompletableFuture.failedFuture(invalid);}
        var known=cache.get(id);if(known!=null)return CompletableFuture.completedFuture(known.value);
        var pending=loading.get(id);if(pending!=null)return pending;
        var result=submit(()->{
                byte[] bytes;
                try(var input=new GZIPInputStream(Files.newInputStream(path(id)))){
                    bytes=input.readNBytes(MAX_BYTES+1);
                }
                if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("stored item code exceeds input bounds");
                var json=ScriptJson.parse(new String(bytes,StandardCharsets.UTF_8));
                if(!ScriptJson.digest(json).equals(id))throw new IllegalArgumentException("stored item code hash mismatch");
                var compiled=new Loaded(id,ScriptProgram.compile(json,api,events));remember(compiled,bytes.length);return compiled;
        });
        loading.put(id,result);result.whenComplete((value,error)->{synchronized(this){loading.remove(id,result);}});
        return result;
    }
    private synchronized CompletableFuture<Loaded> submit(Callable<Loaded> operation){
        if(closed)return CompletableFuture.failedFuture(new IllegalStateException("code repository is closed"));
        var result=new CompletableFuture<Loaded>();requests.add(result);
        try{worker.execute(()->{
            try{if(!result.isCancelled())result.complete(operation.call());}catch(Exception error){result.completeExceptionally(error);}
            finally{synchronized(this){requests.remove(result);}}
        });}catch(RejectedExecutionException busy){requests.remove(result);result.completeExceptionally(busy);}
        return result;
    }
    private Path path(String id){
        if(id==null||!id.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("invalid item code hash");
        return root.resolve(id+".json.gz");
    }
    private synchronized void remember(Loaded value,long characters){
        if(closed)return;var previous=cache.remove(value.id);if(previous!=null)cacheWeight-=previous.weight;
        long weight=characters*4+512;cache.put(value.id,new Cached(value,weight));cacheWeight+=weight;
        while(cacheWeight>cacheLimit||cache.size()>1024){var iterator=cache.entrySet().iterator();var oldest=iterator.next();cacheWeight-=oldest.getValue().weight;iterator.remove();}
    }
    @Override public synchronized void close(){
        if(closed)return;closed=true;for(var future:List.copyOf(requests))future.cancel(false);requests.clear();loading.clear();cache.clear();cacheWeight=0;worker.shutdownNow();
    }
}
