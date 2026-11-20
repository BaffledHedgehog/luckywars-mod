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
        System.out.println("PASS MediaTimeline: priming,20FPS,stereo,underrun,duplicate,queue bound,pause,resume,stop");
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
