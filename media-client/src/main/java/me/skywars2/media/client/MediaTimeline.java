package me.skywars2.media.client;

import java.util.*;

/** Bounded jitter buffer shared by the sound thread and render thread. No world or disk access. */
public final class MediaTimeline {
    public static final long STEP=50_000_000L;
    private record Silence(long starts, long ends) { }
    private final NavigableMap<Long,byte[]> audio=new TreeMap<>();
    private final Deque<Silence> silence = new ArrayDeque<>();
    private final boolean contiguous;
    private MediaRate rate=MediaRate.DEFAULT;
    private long queuedSilence, playedSilence;
    private long highest=-1,next=-1,first=-1,anchor,pausedAt;
    private int bytes;
    private boolean started,closed,paused;
    public MediaTimeline() { this(false); }
    MediaTimeline(boolean contiguous) { this.contiguous = contiguous; }
    public synchronized boolean offer(long serial,byte[] pcm){
        if(closed||serial<=highest)return false;
        // A long network interruption is a seek, not a reason to replay old sound.
        if (contiguous && (serial != highest + 1 || audio.size() >= capacity())) return false;
        if(highest>=0&&serial-highest>8){audio.clear();started=false;next=first=-1;}
        highest=serial;bytes=pcm.length;audio.put(serial,pcm.clone());
        while(audio.size()>capacity())audio.pollFirstEntry();
        return true;
    }
    public synchronized boolean primed(){return !closed&&audio.size()>=Math.max(4,rate.framesFor(.2));}
    synchronized void rate(MediaRate value){if(highest>=0)throw new IllegalStateException("Media clock already populated");rate=value;}
    synchronized int audioSlices(){return rate.framesFor(.05);}
    synchronized int capacity(){return Math.max(8,rate.framesFor(.4));}
    synchronized boolean producerBlocked(int videoFrames){int lead=Math.max(6,rate.framesFor(.3));return audio.size()>=lead||videoFrames>=lead;}
    synchronized long retainedFrom(){return audio.isEmpty()?highest+1:audio.firstKey();}
    /** A released OpenAL channel has lost its prequeued buffers; restart from retained source PCM. */
    public synchronized void reprime(){
        started=false;first=next=-1;silence.clear();queuedSilence=playedSilence=0;
    }
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
        if (gap != null && gap.starts <= elapsed) return first + rate.frameAt(gap.starts - playedSilence) - 1;
        long serial = first + rate.frameAt(elapsed - playedSilence);
        return contiguous ? Math.min(serial, highest) : serial;
    }
    public synchronized byte[] read(){
        if(!started||closed)return new byte[Math.max(0,bytes)];
        byte[] result = audio.remove(next);
        if (result == null && contiguous) {
            // OpenAL prequeues samples. Apply an inserted silence slice at its actual playback
            // position and leave the pending source serial available when the next page arrives.
            long start = rate.nanos(next-first) + queuedSilence;
            long duration=rate.nanos(next-first+1)-rate.nanos(next-first);
            var last = silence.peekLast();
            if (last != null && last.ends == start) {
                silence.removeLast();
                silence.addLast(new Silence(last.starts, start + duration));
            } else silence.addLast(new Silence(start, start + duration));
            queuedSilence += duration;
        } else {
            next++;
            audio.headMap(next, false).clear();
        }
        return result == null ? new byte[contiguous?rate.pcmBytes(Math.max(0,next-first)):bytes] : result;
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
            long end = anchor + rate.nanos(played-first+1) + playedSilence;
            if (now > end) anchor += now - end;
        }
    }
    public synchronized int queued(){return audio.size();}
    public synchronized void close(){closed=true;audio.clear();silence.clear();}
}
