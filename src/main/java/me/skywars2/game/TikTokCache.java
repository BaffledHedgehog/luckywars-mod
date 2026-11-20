package me.skywars2.game;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.world.level.material.MapColor;

/** Bounded originals + prepared clips, one owned background preparation process, no tick-thread transcode. */
final class TikTokCache implements AutoCloseable {
    static final int CAPACITY=20;
    private static final long MAX_DISK=3L*1024*1024*1024;
    @FunctionalInterface interface Preparation { Path run(Path stage,List<String> excluded,Path audioSource) throws Exception; }
    private final Preparation preparation;
    private final Path root, videos, incoming, cookies;
    private final boolean fillQueue;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"skywars-tiktok-prepare");t.setDaemon(true);return t;});
    private final Deque<TikTokMedia> clips=new ArrayDeque<>();
    private final LinkedHashSet<String> recent=new LinkedHashSet<>();
    private Future<Path> pending;
    private String pendingAudioId="";
    private final Set<String> attemptedAudio=new HashSet<>();
    private volatile Process process;
    private volatile boolean closed;
    private long retryAfter;
    private String error="";

    TikTokCache(Path root, Path cookies) throws IOException {
        this(root,cookies,true);
    }
    TikTokCache(Path root, Path cookies,boolean fillQueue) throws IOException {
        this(root,cookies,fillQueue,null);
    }
    TikTokCache(Path root, Path cookies,boolean fillQueue,Preparation preparation) throws IOException {
        this.fillQueue=fillQueue;
        this.preparation=preparation==null?this::prepare:preparation;
        this.root=root.toAbsolutePath().normalize(); this.cookies=cookies.toAbsolutePath().normalize();
        videos=this.root.resolve("videos"); incoming=this.root.resolve("incoming");
        Files.createDirectories(videos); Files.createDirectories(incoming);
        Path history=this.root.resolve("recent.json");
        if(Files.isRegularFile(history))try{
            for(var id:JsonParser.parseString(Files.readString(history)).getAsJsonArray())
                if(id.isJsonPrimitive()&&id.getAsString().matches("[0-9]{8,24}"))remember(id.getAsString());
        }catch(RuntimeException invalid){recent.clear();}
        try(var entries=Files.list(incoming)){for(var path:entries.toList())remove(path);}
        for(String name:List.of("worker.py","spectral.py"))
            try(var resource=TikTokCache.class.getResourceAsStream("/skywars2/tiktok/"+name)){
                if(resource==null)throw new IOException("worker_missing");
                Files.copy(resource,this.root.resolve(name),StandardCopyOption.REPLACE_EXISTING);
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
        recent.clear();saveRecent();
        retryAfter=0; error="";
    }

    void tick(long now, Set<String> inUse) {
        if(closed)return;
        long wallNow=System.nanoTime();
        if(pending!=null&&pending.isDone()){
            try {
                Path prepared=pending.get();
                if(!pendingAudioId.isEmpty()){
                    for(var clip:clips)if(clip.id.equals(pendingAudioId)){
                        long bytes=Files.size(prepared.resolve("clip.tka"));
                        if(size()+bytes>MAX_DISK)throw new IOException("cache_full");
                        Files.move(prepared.resolve("clip.tka"),clip.directory.resolve("clip.tka"),
                                StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                        if(!clip.openSpectral())throw new IOException("spectral_invalid");
                        break;
                    }
                    error="";
                }else{
                    var manifest=JsonParser.parseString(Files.readString(prepared.resolve("manifest.json"))).getAsJsonObject();
                    String id=manifest.get("id").getAsString();
                    if(!id.matches("[0-9]{8,24}")||ids().contains(id))throw new IOException("clip_invalid");
                    if(size()+directorySize(prepared)>MAX_DISK)throw new IOException("cache_full");
                    Path target=videos.resolve(id);
                    Files.move(prepared,target,StandardCopyOption.ATOMIC_MOVE);
                    clips.addLast(new TikTokMedia(id,target));error="";
                }
            } catch(Exception failure){
                Throwable cause=failure instanceof ExecutionException&&failure.getCause()!=null?failure.getCause():failure;
                String nextError=safeCode(cause.getMessage());
                if(!nextError.equals(error))me.skywars2.SkyWars2.LOGGER.warn("TikTok media preparation failed ({})",nextError);
                error=nextError;retryAfter=wallNow+TimeUnit.SECONDS.toNanos(60);
            }
            pending=null;pendingAudioId="";
            // A worker never publishes partial output; every failed/obsolete staging directory is disposable.
            try(var entries=Files.list(incoming)){for(var path:entries.toList())remove(path);}catch(IOException ignored){}
        }
        if(fillQueue&&pending==null&&wallNow>=retryAfter){
            for(var clip:clips)if(!clip.hasSpectral()&&attemptedAudio.add(clip.id)){
                pendingAudioId=clip.id;
                var source=clip.directory;var stage=incoming.resolve(UUID.randomUUID().toString());
                pending=worker.submit(()->preparation.run(stage,List.of(),source));break;
            }
        }
        if(fillQueue&&pending==null&&clips.size()<CAPACITY&&configured()&&wallNow>=retryAfter){
            var excluded=new LinkedHashSet<>(recent);excluded.addAll(ids());
            String token=UUID.randomUUID().toString();Path stage=incoming.resolve(token);
            var snapshot=List.copyOf(excluded);
            pending=worker.submit(()->preparation.run(stage,snapshot,null));
        }
    }

    void watched(String id, Set<String> inUse) {
        if(!id.isEmpty()&&!id.equals(pendingAudioId)&&!inUse.contains(id)&&clips.size()>1)retire(id);
    }

    private Path prepare(Path stage,List<String> excluded,Path audioSource) throws IOException,InterruptedException {
        String python=System.getenv().getOrDefault("SKYWARS_TIKTOK_PYTHON","python");
        var command=new ArrayList<>(List.of(python,"-u",root.resolve("worker.py").toString(),"--stage",stage.toString()));
        if(audioSource!=null)command.addAll(List.of("--spectral-source",audioSource.toString()));
        else{
            Path exclude=root.resolve("exclude.json");
            Files.writeString(exclude,new Gson().toJson(excluded),StandardCharsets.UTF_8);
            command.addAll(List.of("--palette",root.resolve("palette.json").toString(),"--cookies",cookies.toString(),"--exclude",exclude.toString()));
        }
        var builder=new ProcessBuilder(command);
        String dependencies=System.getenv("SKYWARS_TIKTOK_PYTHONPATH");
        if(dependencies!=null&&!dependencies.isBlank())builder.environment().put("PYTHONPATH",dependencies);
        builder.redirectError(ProcessBuilder.Redirect.DISCARD);
        // Only fixed status codes are read; subprocess/library diagnostics never reach server logs.
        Process owned;
        try{owned=builder.start();}
        catch(IOException failure){throw new IOException("python_unavailable",failure);}
        process=owned;
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
            iterator.remove();remember(id);saveRecent();
            try{clip.close();remove(clip.directory);}catch(IOException ignored){}return;
        }
    }
    private void remember(String id){
        recent.remove(id);recent.add(id);
        while(recent.size()>CAPACITY*5)recent.remove(recent.iterator().next());
    }
    private void saveRecent(){
        try{Files.writeString(root.resolve("recent.json"),new Gson().toJson(recent));}catch(IOException ignored){}
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
        return code!=null&&Set.of("python_unavailable","dependencies_missing","browser_missing","feed_unavailable",
                "download_failed","download_size","transcode_failed","cookies_invalid","cache_full","worker_timeout").contains(code)
                ?code:"prepare_failed";
    }
    private void cancel(){
        var owned=process;if(owned!=null)destroy(owned);
        if(pending!=null){pending.cancel(true);pending=null;}pendingAudioId="";
    }
    @Override public void close(){
        closed=true;cancel();worker.shutdownNow();
        for(var clip:clips)try{clip.close();}catch(IOException ignored){}
        clips.clear();
    }
}
