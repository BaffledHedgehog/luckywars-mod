package me.skywars2.game.items.phone;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Account recommendations as expiring URLs, independent of the vanilla transcode queue. */
final class TikTokLinkFeed implements AutoCloseable {
    @FunctionalInterface interface Fetcher { List<TikTokMedia.CdnSource> fetch(List<String> excluded)throws Exception; }
    private final Path root,cookies,incoming;
    private final Fetcher fetcher;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"skywars-tiktok-links");t.setDaemon(true);t.setPriority(Thread.MIN_PRIORITY);return t;});
    private final ArrayDeque<TikTokMedia.CdnSource> ready=new ArrayDeque<>();
    private final LinkedHashSet<String> seen=new LinkedHashSet<>();
    private Future<List<TikTokMedia.CdnSource>> pending;
    private volatile Process process;
    private boolean closed;
    private boolean initialized;
    private long retry;
    TikTokLinkFeed(TikTokCache cache){this(cache,null);}
    TikTokLinkFeed(TikTokCache cache,Fetcher fetcher){root=cache.root();cookies=cache.cookies();incoming=root.resolve("link-incoming");this.fetcher=fetcher==null?this::fetch:fetcher;}
    int size(){return ready.size();}
    TikTokMedia.CdnSource take(){prune();return ready.pollFirst();}
    private void prune(){long now=Instant.now().getEpochSecond();ready.removeIf(source->source.expiresAt()<now+30);}
    void tick(boolean demand){
        if(closed)return;prune();
        if(pending!=null&&pending.isDone()){
            try{for(var source:pending.get())if(ready.size()<20&&seen.add(source.clipId()))ready.addLast(source);retry=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);}
            catch(InterruptedException canceled){Thread.currentThread().interrupt();}
            catch(ExecutionException|CancellationException failure){retry=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);}
            pending=null;while(seen.size()>200)seen.remove(seen.iterator().next());
        }
        if(demand&&pending==null&&ready.size()<20&&System.nanoTime()>=retry&&Files.isRegularFile(cookies)){
            var excluded=List.copyOf(seen);pending=worker.submit(()->fetcher.fetch(excluded));
        }
    }
    private List<TikTokMedia.CdnSource> fetch(List<String> excluded)throws IOException,InterruptedException{
        Files.createDirectories(incoming);
        if(!initialized){
            try(var stale=Files.list(incoming)){for(var path:stale.toList())if(path.getFileName().toString().matches("[0-9a-f-]{36}"))remove(path);}
            initialized=true;
        }
        var stage=incoming.resolve(UUID.randomUUID().toString());
        var exclude=incoming.resolve("exclude.json");Files.writeString(exclude,new Gson().toJson(excluded),StandardCharsets.UTF_8);
        var command=List.of(System.getenv().getOrDefault("SKYWARS_TIKTOK_PYTHON","python"),"-u",root.resolve("worker.py").toString(),
                "--links-only","--stage",stage.toString(),"--cookies",cookies.toString(),"--exclude",exclude.toString());
        var builder=new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD).redirectOutput(ProcessBuilder.Redirect.DISCARD);
        var dependencies=System.getenv("SKYWARS_TIKTOK_PYTHONPATH");if(dependencies!=null&&!dependencies.isBlank())builder.environment().put("PYTHONPATH",dependencies);
        Process owned=null;
        try{
            owned=builder.start();process=owned;
            if(!owned.waitFor(90,TimeUnit.SECONDS)||owned.exitValue()!=0)throw new IOException("feed_unavailable");
            var file=stage.resolve("links.json");if(Files.size(file)>128*1024)throw new IOException("feed_invalid");
            var rows=JsonParser.parseString(Files.readString(file)).getAsJsonArray();if(rows.size()>20)throw new IOException("feed_invalid");
            var result=new ArrayList<TikTokMedia.CdnSource>();long now=Instant.now().getEpochSecond();
            for(var row:rows){var value=row.getAsJsonObject();var source=TikTokMedia.sourceDescriptor(value.get("clipId").getAsString(),value.get("url").getAsString(),value.get("expiresAt").getAsLong(),now);if(source!=null)result.add(source);}
            if(result.isEmpty())throw new IOException("feed_unavailable");
            return List.copyOf(result);
        }catch(RuntimeException invalid){throw new IOException("feed_invalid");}
        finally{if(owned!=null)destroy(owned);if(process==owned)process=null;remove(stage);}
    }
    private void remove(Path target){
        if(!target.toAbsolutePath().normalize().startsWith(incoming.toAbsolutePath().normalize())||target.equals(incoming))throw new IllegalArgumentException("link_stage");
        if(Files.exists(target))try(var entries=Files.walk(target)){for(var path:entries.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}catch(IOException ignored){}
    }
    private static void destroy(Process owned){owned.descendants().forEach(p->{try{p.destroyForcibly();}catch(RuntimeException ignored){}});if(owned.isAlive())owned.destroyForcibly();}
    @Override public void close(){closed=true;var owned=process;if(owned!=null)destroy(owned);if(pending!=null)pending.cancel(true);worker.shutdownNow();ready.clear();seen.clear();}
}
