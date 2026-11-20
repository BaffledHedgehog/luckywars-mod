package me.skywars2.media.client;

import java.util.*;

/** Bounded jitter buffer shared by the sound thread and render thread. No world or disk access. */
public final class MediaTimeline {
    public static final long STEP=50_000_000L;
    private record Silence(long starts, long ends) { }
    private final NavigableMap<Long,byte[]> audio=new TreeMap<>();
    private final Deque<Silence> silence = new ArrayDeque<>();
    private final boolean contiguous;
    private long queuedSilence, playedSilence;
    private long highest=-1,next=-1,first=-1,anchor,pausedAt;
    private int bytes;
    private boolean started,closed,paused;
    public MediaTimeline() { this(false); }
    MediaTimeline(boolean contiguous) { this.contiguous = contiguous; }
    public synchronized boolean offer(long serial,byte[] pcm){
        if(closed||serial<=highest)return false;
        // A long network interruption is a seek, not a reason to replay old sound.
        if (contiguous && (serial != highest + 1 || audio.size() >= 8)) return false;
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
        if (!started) return -1;
        long elapsed = Math.max(0, (paused ? pausedAt : now) - anchor);
        while (!silence.isEmpty() && silence.peekFirst().ends <= elapsed) {
            var played = silence.removeFirst();
            playedSilence += played.ends - played.starts;
        }
        var gap = silence.peekFirst();
        if (gap != null && gap.starts <= elapsed) return first + (gap.starts - playedSilence) / STEP - 1;
        long serial = first + (elapsed - playedSilence) / STEP;
        return contiguous ? Math.min(serial, highest) : serial;
    }
    public synchronized byte[] read(){
        if(!started||closed)return new byte[Math.max(0,bytes)];
        byte[] result = audio.remove(next);
        if (result == null && contiguous) {
            // OpenAL prequeues samples. Apply an inserted silence slice at its actual playback
            // position and leave the pending source serial available when the next page arrives.
            long start = (next - first) * STEP + queuedSilence;
            var last = silence.peekLast();
            if (last != null && last.ends == start) {
                silence.removeLast();
                silence.addLast(new Silence(last.starts, start + STEP));
            } else silence.addLast(new Silence(start, start + STEP));
            queuedSilence += STEP;
        } else {
            next++;
            audio.headMap(next, false).clear();
        }
        return result == null ? new byte[bytes] : result;
    }
    public synchronized boolean running(){return started&&!closed;}
    synchronized void discardPlayed(long now) {
        if (!started || closed) return;
        long played = presented(now);
        audio.headMap(played, true).clear();
        next = Math.max(next, played + 1);
        if (contiguous && played == highest && silence.isEmpty()) {
            // With no audio channel there is no sampler to insert silence. Hold the visual clock
            // at the next source-frame boundary so a late page is still played in order.
            long end = anchor + (played - first + 1) * STEP + playedSilence;
            if (now > end) anchor += now - end;
        }
    }
    public synchronized int queued(){return audio.size();}
    public synchronized void close(){closed=true;audio.clear();silence.clear();}
}
