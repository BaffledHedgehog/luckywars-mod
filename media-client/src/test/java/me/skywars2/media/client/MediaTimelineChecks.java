package me.skywars2.media.client;

import java.util.Arrays;

public final class MediaTimelineChecks {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        var t=new MediaTimeline();
        byte[] pcm=new byte[9600];Arrays.fill(pcm,(byte)91);
        for(int i=10;i<14;i++)check(t.offer(i,pcm),"admit distinct frames");
        pcm[0]=0;check(t.primed(),"four 50ms slices prime the actual channel");
        t.start(1_000_000_000L);
        check(t.presented(1_149_999_999L)==12&&t.presented(1_150_000_000L)==13,"20FPS presentation clock");
        check(!t.offer(13,pcm)&&!t.offer(12,pcm),"duplicate/stale do not replay audio");
        for(int i=0;i<4;i++){var read=t.read();check(read.length==9600&&read[0]==91,"independent stereo PCM bytes");}
        check(Arrays.equals(t.read(),new byte[9600]),"underrun inserts one bounded silence frame");
        for(int i=14;i<100;i++)t.offer(i,pcm);
        check(t.queued()==8,"queue memory bounded under stalled render/audio");
        t.close();check(t.queued()==0&&!t.offer(101,pcm)&&!t.running(),"stop clears data and rejects late work");
        checkPause(pcm);
        checkDelayedSegment(pcm);
        checkChannelRecovery(pcm);
        for(String rate:new String[]{"20/1","30/1","60/1","30000/1001"})checkContinuousVideo(MediaRate.parse(rate));
        System.out.println("PASS MediaTimeline: priming,20FPS,stereo,underrun,duplicate,queue bound,pause,resume,stop");
    }

    private static void checkContinuousVideo(MediaRate rate){
        var timeline=new MediaTimeline(true);timeline.rate(rate);
        var video=new java.util.ArrayDeque<Long>();long produced=0,shown=-1,audioRead=0;
        long origin=1_000_000_000L;
        // The native source primes FOUR audio buffers ahead of presentation. A producer
        // bounded only by PCM used to evict every due video frame from the eight-frame queue.
        while(!timeline.producerBlocked(video.size())){
            timeline.offer(produced,new byte[rate.pcmBytes(produced)]);video.add(produced++);
        }
        timeline.start(origin);
        int prefetch=4*rate.framesFor(.05);
        for(int n=0;n<prefetch;n++){timeline.read();audioRead++;}
        for(int frame=0;frame<rate.framesFor(15);frame++){
            while(!timeline.producerBlocked(video.size())){
                check(timeline.offer(produced,new byte[rate.pcmBytes(produced)]),"ordered PCM at "+rate);video.add(produced++);
            }
            long target=timeline.presented(origin+rate.nanos(frame));
            while(!video.isEmpty()&&video.peekFirst()<=target)shown=video.removeFirst();
            check(shown==frame,"every source frame reaches presentation, no future-frame eviction at "+rate+" frame="+frame+" shown="+shown);
            timeline.read();audioRead++;
        }
        check(rate.samples(rate.framesFor(10))>470000,"source PCM remains continuous");
        timeline.close();
    }

    private static void checkChannelRecovery(byte[] pcm) {
        var t=new MediaTimeline(true);
        for(int n=0;n<8;n++)check(t.offer(n,pcm),"source PCM before device loss");
        t.start(1_000_000_000L);
        for(int n=0;n<4;n++)t.read(); // Four buffers belonged to the released channel.
        t.reprime();
        check(t.primed()&&!t.running(),"device loss retains unconsumed PCM and resets the clock");
        t.start(9_000_000_000L);
        check(t.presented(9_000_000_000L)==4&&t.read()[1]==91,"new channel resumes retained source without permanent silence");
        t.close();
    }

    private static void checkDelayedSegment(byte[] pcm) {
        var timeline = new MediaTimeline(true);
        long start = 1_000_000_000L, step = MediaTimeline.STEP;
        for (int serial = 0; serial < 4; serial++) check(timeline.offer(serial, pcm), "page's initial samples");
        timeline.start(start);
        for (int serial = 0; serial < 4; serial++) check(timeline.read()[1] == 91, "page's scheduled PCM in order");
        check(timeline.read()[1] == 0 && timeline.read()[1] == 0, "late HTTPS page buffers native silence");
        long pause = start + 5 * step;
        timeline.setPaused(true, pause);
        check(timeline.offer(4, pcm) && timeline.offer(5, pcm) && !timeline.offer(7, pcm), "received page retains exact source order");
        long thaw = pause + 20_000_000_000L;
        check(timeline.presented(thaw) == 3 && timeline.queued() == 2, "late page survives Time Stop");
        timeline.setPaused(false, thaw);
        check(timeline.read()[1] == 91 && timeline.read()[1] == 91, "late PCM plays without skipping source slices");
        check(timeline.presented(thaw + step - 1) == 3 && timeline.presented(thaw + step) == 4
                        && timeline.presented(thaw + 2 * step) == 5,
                "video resumes in exact phase with the delayed native audio");
        timeline.close();
    }

    private static void checkPause(byte[] pcm) {
        var timeline = new MediaTimeline();
        long start = 1_000_000_000L;
        long step = MediaTimeline.STEP;
        long frozen = start + 2 * step + step / 2;
        long resumed = frozen + 20_000_000_000L;
        for (int i = 40; i < 48; i++) timeline.offer(i, pcm);
        timeline.start(start);
        check(timeline.read()[1] == 91, "consume first sound slice before Time Stop");
        timeline.setPaused(true, frozen);
        timeline.setPaused(true, frozen + 100 * step);
        check(timeline.presented(resumed) == 42 && timeline.queued() == 7,
                "Time Stop holds presentation and keeps queued sound");
        timeline.setPaused(false, resumed);
        check(timeline.presented(resumed + step / 2 - 1) == 42
                && timeline.presented(resumed + step / 2) == 43,
                "resume from same frame phase after a long pause");
        check(timeline.read()[1] == 91 && timeline.queued() == 6, "resume from next retained sound slice");
        timeline.setPaused(true, resumed + step);
        timeline.close();
        check(timeline.queued() == 0 && !timeline.offer(48, pcm) && !timeline.running(),
                "stop during Time Stop clears buffered audio");

        var lateSound = new MediaTimeline();
        lateSound.offer(80, pcm);
        lateSound.setPaused(true, frozen);
        lateSound.start(frozen + 100 * step);
        lateSound.setPaused(false, resumed);
        check(lateSound.presented(resumed) == 80 && lateSound.presented(resumed + step) == 81,
                "audio primed during Time Stop starts on thaw without an artificial delay");
    }
}
