package me.skywars2.media.client;

import java.util.*;

/** Bounded jitter buffer shared by the sound thread and render thread. No world or disk access. */
public final class MediaTimeline {
    public static final long STEP=50_000_000L;
    private final NavigableMap<Long,byte[]> audio=new TreeMap<>();
    private long highest=-1,next=-1,first=-1,anchor,pausedAt;
    private int bytes;
    private boolean started,closed,paused;
    public synchronized boolean offer(long serial,byte[] pcm){
        if(closed||serial<=highest)return false;
        // A long network interruption is a seek, not a reason to replay old sound.
        if(highest>=0&&serial-highest>8){audio.clear();started=false;next=first=-1;}
        highest=serial;bytes=pcm.length;audio.put(serial,pcm.clone());
        while(audio.size()>8)audio.pollFirstEntry();
        return true;
    }
    public synchronized boolean primed(){return !closed&&audio.size()>=4;}
    public synchronized void start(long now){
        if(!started&&!audio.isEmpty()){first=next=audio.firstKey();anchor=now;started=true;}
    }
    synchronized void setPaused(boolean value, long now) {
        if (paused == value) return;
        paused = value;
        if (paused) pausedAt = now;
        else if (started) anchor += Math.max(0, now - Math.max(pausedAt, anchor));
    }
    public synchronized long presented(long now){
        return started ? first + Math.max(0, (paused ? pausedAt : now) - anchor) / STEP : -1;
    }
    public synchronized byte[] read(){
        if(!started||closed)return new byte[Math.max(0,bytes)];
        byte[] result=audio.remove(next++);audio.headMap(next,false).clear();
        return result==null?new byte[bytes]:result;
    }
    public synchronized boolean running(){return started&&!closed;}
    synchronized void discardPlayed(long now) {
        if (!started || closed) return;
        long played = presented(now);
        audio.headMap(played, true).clear();
        next = Math.max(next, played + 1);
    }
    public synchronized int queued(){return audio.size();}
    public synchronized void close(){closed=true;audio.clear();}
}
