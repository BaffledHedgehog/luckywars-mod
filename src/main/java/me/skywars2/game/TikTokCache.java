package me.skywars2.game;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.world.level.material.MapColor;

/** Five originals + prepared clips, one owned background preparation process, no tick-thread transcode. */
final class TikTokCache implements AutoCloseable {
    static final int CAPACITY=5;
    private static final long MAX_DISK=768L*1024*1024;
    private final Path root, videos, incoming, cookies;
    private final boolean fillQueue;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"skywars-tiktok-prepare");t.setDaemon(true);return t;});
    private final Deque<TikTokMedia> clips=new ArrayDeque<>();
    private Future<Path> pending;
    private volatile Process process;
    private volatile boolean closed;
    private long retryAfter;
    private String error="";

    TikTokCache(Path root, Path cookies) throws IOException {
        this(root,cookies,true);
    }
    TikTokCache(Path root, Path cookies,boolean fillQueue) throws IOException {
        this.fillQueue=fillQueue;
        this.root=root.toAbsolutePath().normalize(); this.cookies=cookies.toAbsolutePath().normalize();
        videos=this.root.resolve("videos"); incoming=this.root.resolve("incoming");
        Files.createDirectories(videos); Files.createDirectories(incoming);
        try(var entries=Files.list(incoming)){for(var path:entries.toList())remove(path);}
        try(var resource=TikTokCache.class.getResourceAsStream("/skywars2/tiktok/worker.py")){
            if(resource==null)throw new IOException("worker_missing");
            Files.copy(resource,this.root.resolve("worker.py"),StandardCopyOption.REPLACE_EXISTING);
        }
        var palette=new JsonArray();
        for(int i=0;i<256;i++){
            int color=MapColor.getColorFromPackedId(i);var rgb=new JsonArray();
            rgb.add((color>>16)&255);rgb.add((color>>8)&255);rgb.add(color&255);palette.add(rgb);
        }
        Files.writeString(this.root.resolve("palette.json"),palette.toString(),StandardCharsets.UTF_8);
        try(var entries=Files.list(videos)){
            for(var path:entries.sorted().toList()){
                try {
                    if(clips.size()>=CAPACITY || !path.getFileName().toString().matches("[0-9]{8,24}")) {remove(path);continue;}
                    clips.addLast(new TikTokMedia(path.getFileName().toString(),path));
                } catch(IOException invalid){remove(path);}
            }
        }
        while(size()>MAX_DISK&&!clips.isEmpty())retire(clips.getFirst().id);
    }

    boolean configured(){return Files.isRegularFile(cookies);}
    int sizeReady(){return clips.size();}
    String error(){return error;}
    String stateKey(){
        if(!configured())return "tiktok.waiting_for_import";
        return switch(error){
            case "browser_missing"->"tiktok.browser_missing";
            case "feed_unavailable"->"tiktok.feed_unavailable";
            case "cache_full"->"tiktok.cache_full";
            case ""->clips.size()==CAPACITY?"tiktok.ready":"tiktok.preparing";
            default->"tiktok.prepare_failed";
        };
    }
    List<String> ids(){return clips.stream().map(c->c.id).toList();}
    TikTokMedia next(String previous){
        if(clips.isEmpty())return null;
        var list=List.copyOf(clips);
        for(int i=0;i<list.size();i++)if(list.get(i).id.equals(previous))return list.get((i+1)%list.size());
        return list.getFirst();
    }

    void importCookies(String json) throws IOException {
        String normalized=TikTokCookies.normalize(json);
        applyImportedCookies(normalized);
    }
    void applyImportedCookies(String normalized) throws IOException {
        cancel(); TikTokCookies.save(cookies,normalized);
        while(!clips.isEmpty())retire(clips.getFirst().id);
        retryAfter=0; error="";
    }

    void tick(long now, Set<String> inUse) {
        if(closed)return;
        if(pending!=null&&pending.isDone()){
            try {
                Path prepared=pending.get();
                var manifest=JsonParser.parseString(Files.readString(prepared.resolve("manifest.json"))).getAsJsonObject();
                String id=manifest.get("id").getAsString();
                if(!id.matches("[0-9]{8,24}")||ids().contains(id))throw new IOException("clip_invalid");
                if(size()+directorySize(prepared)>MAX_DISK)throw new IOException("cache_full");
                Path target=videos.resolve(id);
                Files.move(prepared,target,StandardCopyOption.ATOMIC_MOVE);
                clips.addLast(new TikTokMedia(id,target));error="";
            } catch(Exception failure){
                Throwable cause=failure instanceof ExecutionException&&failure.getCause()!=null?failure.getCause():failure;
                error=safeCode(cause.getMessage());retryAfter=now+1200;
            }
            pending=null;
            // A worker never publishes partial output; every failed/obsolete staging directory is disposable.
            try(var entries=Files.list(incoming)){for(var path:entries.toList())remove(path);}catch(IOException ignored){}
        }
        if(fillQueue&&pending==null&&clips.size()<CAPACITY&&configured()&&now>=retryAfter){
            var excluded=ids();String token=UUID.randomUUID().toString();Path stage=incoming.resolve(token);
            pending=worker.submit(()->prepare(stage,excluded));
        }
    }

    void watched(String id, Set<String> inUse) {
        if(!id.isEmpty()&&!inUse.contains(id)&&clips.size()>1)retire(id);
    }

    private Path prepare(Path stage,List<String> excluded) throws IOException,InterruptedException {
        Path exclude=root.resolve("exclude.json");
        Files.writeString(exclude,new Gson().toJson(excluded),StandardCharsets.UTF_8);
        String python=System.getenv().getOrDefault("SKYWARS_TIKTOK_PYTHON","python");
        var builder=new ProcessBuilder(python,"-u",root.resolve("worker.py").toString(),"--stage",stage.toString(),
                "--palette",root.resolve("palette.json").toString(),"--cookies",cookies.toString(),"--exclude",exclude.toString());
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        // Only fixed status codes are read; subprocess/library diagnostics never reach server logs.
        Process owned=builder.start();process=owned;
        try {
            if(!owned.waitFor(8,TimeUnit.MINUTES)){throw new IOException("worker_timeout");}
            byte[] status=owned.getInputStream().readNBytes(2048);
            if(owned.exitValue()!=0){
                String code="prepare_failed";
                try{code=safeCode(JsonParser.parseString(new String(status,StandardCharsets.UTF_8)).getAsJsonObject().get("error").getAsString());}
                catch(RuntimeException ignored){}
                throw new IOException(code);
            }
            if(!Files.isRegularFile(stage.resolve("manifest.json")))throw new IOException("prepare_failed");
            return stage;
        } finally {destroy(owned);if(process==owned)process=null;}
    }

    private void retire(String id){
        var iterator=clips.iterator();
        while(iterator.hasNext()){
            var clip=iterator.next();if(!clip.id.equals(id))continue;
            iterator.remove();try{clip.close();remove(clip.directory);}catch(IOException ignored){}return;
        }
    }
    private long size() throws IOException {return directorySize(videos);}
    private static long directorySize(Path directory) throws IOException {
        final long[] bytes={0};
        Files.walkFileTree(directory,new SimpleFileVisitor<>(){
            @Override public FileVisitResult visitFile(Path p,BasicFileAttributes a){bytes[0]+=a.size();return FileVisitResult.CONTINUE;}
        });return bytes[0];
    }
    private void remove(Path path) throws IOException {
        Path target=path.toAbsolutePath().normalize();
        if(!target.startsWith(root)||target.equals(root)||target.equals(cookies))throw new IOException("cache_path_invalid");
        Files.walkFileTree(target,new SimpleFileVisitor<>(){
            @Override public FileVisitResult visitFile(Path p,BasicFileAttributes a)throws IOException{Files.delete(p);return FileVisitResult.CONTINUE;}
            @Override public FileVisitResult postVisitDirectory(Path p,IOException error)throws IOException{if(error!=null)throw error;Files.delete(p);return FileVisitResult.CONTINUE;}
        });
    }
    private static void destroy(Process owned){
        owned.descendants().forEach(child->{try{child.destroyForcibly();}catch(RuntimeException ignored){}});
        if(owned.isAlive())owned.destroyForcibly();
    }
    private static String safeCode(String code){
        return code!=null&&Set.of("browser_missing","feed_unavailable","cache_full").contains(code)?code:"prepare_failed";
    }
    private void cancel(){
        var owned=process;if(owned!=null)destroy(owned);
        if(pending!=null){pending.cancel(true);pending=null;}
    }
    @Override public void close(){
        closed=true;cancel();worker.shutdownNow();
        for(var clip:clips)try{clip.close();}catch(IOException ignored){}
        clips.clear();
    }
}
