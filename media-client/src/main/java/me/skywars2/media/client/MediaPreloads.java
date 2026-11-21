package me.skywars2.media.client;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import me.skywars2.media.MediaPackets;

/** Three owned temporary originals; claiming transfers file ownership to the decoder. */
final class MediaPreloads implements AutoCloseable {
    @FunctionalInterface interface Fetch { MediaSegmentQueue.Download open(String uri,boolean direct); }
    private final Fetch fetch;
    private final Map<String,Entry> entries=new LinkedHashMap<>();
    MediaPreloads(MediaDownload downloads){this((uri,direct)->direct?downloads.direct(URI.create(uri)):downloads.fallback(URI.create(uri)));}
    MediaPreloads(Fetch fetch){this.fetch=fetch;}
    private static String key(MediaPackets.Link link){return link.cdn().isEmpty()?link.relay():link.cdn();}
    synchronized void update(List<MediaPackets.Link> clips){
        var wanted=new HashSet<String>();for(var clip:clips)wanted.add(key(clip));
        entries.entrySet().removeIf(e->{if(wanted.contains(e.getKey()))return false;e.getValue().close();return true;});
        for(var clip:clips){
            String key=key(clip);var old=entries.get(key);
            if(old!=null&&!old.ready.isCompletedExceptionally())continue;
            if(old!=null)old.close();entries.put(key,new Entry(clip));
        }
    }
    synchronized MediaSegmentQueue.Download take(String uri){
        var iterator=entries.entrySet().iterator();
        while(iterator.hasNext()){
            var entry=iterator.next().getValue();
            if((entry.link.cdn().equals(uri)||entry.link.relay().equals(uri))&&!entry.ready.isCompletedExceptionally()){
                iterator.remove();return entry;
            }
        }
        return null;
    }
    @Override public synchronized void close(){for(var entry:entries.values())entry.close();entries.clear();}
    private final class Entry implements MediaSegmentQueue.Download {
        final MediaPackets.Link link;
        final CompletableFuture<Path> ready=new CompletableFuture<>();
        final Thread worker;
        MediaSegmentQueue.Download transfer;boolean closed;
        Entry(MediaPackets.Link link){this.link=link;worker=Thread.ofVirtual().name("skywars-media-preload").start(this::prepare);}
        private void prepare(){
            try{
                if(!link.cdn().isEmpty())try{
                    var direct=fetch.open(link.cdn(),true);install(direct);
                    ready.complete(direct.file().toCompletableFuture().get(8,TimeUnit.SECONDS));return;
                }catch(ExecutionException|TimeoutException|IllegalArgumentException failed){release();}
                if(link.relay().isEmpty()||!link.relayMp4())throw new IllegalArgumentException("media_preload_original_unavailable");
                var relay=fetch.open(link.relay(),false);install(relay);
                ready.complete(relay.file().toCompletableFuture().get());
            }catch(InterruptedException cancelled){Thread.currentThread().interrupt();ready.cancel(false);release();}
            catch(ExecutionException|RuntimeException failed){ready.completeExceptionally(failed);release();}
        }
        private synchronized void install(MediaSegmentQueue.Download value)throws InterruptedException{
            if(closed){value.close();throw new InterruptedException();}transfer=value;
        }
        private synchronized void release(){if(transfer!=null){transfer.close();transfer=null;}}
        @Override public CompletionStage<Path> file(){return ready.minimalCompletionStage();}
        @Override public synchronized void close(){if(closed)return;closed=true;worker.interrupt();ready.cancel(false);release();}
    }
}
