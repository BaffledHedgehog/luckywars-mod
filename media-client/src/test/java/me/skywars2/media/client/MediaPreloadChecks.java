package me.skywars2.media.client;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import me.skywars2.media.MediaPackets;

final class MediaPreloadChecks {
    static void run()throws Exception{
        var files=new java.util.concurrent.ConcurrentHashMap<String,Path>();
        var fetched=new java.util.concurrent.ConcurrentHashMap<String,Integer>();
        try(var preloads=new MediaPreloads((uri,direct)->{
            fetched.merge(uri,1,Integer::sum);
            var ready=new CompletableFuture<Path>();
            if(direct)ready.completeExceptionally(new IllegalArgumentException("regional CDN restriction"));
            else try{var file=Files.createTempFile("phone-preload-check-",".mp4");Files.write(file,new byte[]{1,2,3});files.put(uri,file);ready.complete(file);}catch(Exception failure){throw new IllegalStateException(failure);}
            return new MediaSegmentQueue.Download(){
                public CompletionStage<Path> file(){return ready;}
                public void close(){var file=files.remove(uri);if(file!=null)try{Files.delete(file);}catch(Exception failure){throw new IllegalStateException(failure);}}
            };
        })){
            var links=new ArrayList<MediaPackets.Link>();for(int i=0;i<3;i++)links.add(new MediaPackets.Link(UUID.randomUUID(),"https://cdn/"+i,"https://relay/"+i,true));
            preloads.update(links);
            var selected=preloads.take(links.getFirst().cdn());if(selected==null)throw new AssertionError("prepared download is claimed by either source alias");
            var path=selected.file().toCompletableFuture().get(2,TimeUnit.SECONDS);
            if(!Files.exists(path)||!fetched.containsKey(links.getFirst().relay()))throw new AssertionError("CDN failure warms original through relay");
            long deadline=System.nanoTime()+2_000_000_000L;
            while(files.size()<3&&System.nanoTime()<deadline)Thread.sleep(5);
            preloads.close();
            deadline=System.nanoTime()+2_000_000_000L;
            while(files.size()>1&&System.nanoTime()<deadline)Thread.sleep(5);
            if(!Files.exists(path))throw new AssertionError("clearing irrelevant cache cannot delete claimed active video");
            selected.close();
            if(Files.exists(path))throw new AssertionError("finished playback deletes its original");
            if(preloads.take(links.getFirst().cdn())!=null)throw new AssertionError("one-owner claim, no stale duplicate playback");
        }
        if(!files.isEmpty())throw new AssertionError("cancelled preloads leave no temporary originals");
        System.out.println("PASS three-video preload: regional CDN fallback, alias reuse, ownership transfer, irrelevant-file cleanup");
    }
}
