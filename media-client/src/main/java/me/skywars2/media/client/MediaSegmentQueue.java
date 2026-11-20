package me.skywars2.media.client;

import java.nio.file.Path;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import me.skywars2.media.MediaPackets;

/** One not-yet-decoded segment. The worker owns the elected current part after taking it. */
final class MediaSegmentQueue implements AutoCloseable {
    interface Download extends AutoCloseable {
        CompletionStage<Path> file();
        @Override void close();
    }

    record Part(MediaPackets.Segment description, Download download) implements AutoCloseable {
        @Override public void close() { download.close(); }
    }

    private final MediaPackets.Segment initial;
    private long expected;
    private boolean waiting = true, closed;
    private Part pending;

    MediaSegmentQueue(MediaPackets.Segment initial) { this.initial = initial; }

    synchronized boolean expect(long sequence) {
        if (closed || waiting || pending != null || sequence != expected + 1) return false;
        expected = sequence;
        waiting = true;
        return true;
    }

    synchronized boolean offer(MediaPackets.Segment segment, Function<String, Download> fetch) {
        if (closed || !waiting || segment.sequence() != expected || !initial.session().equals(segment.session())
                || segment.totalFrames() != initial.totalFrames() || segment.width() != initial.width()
                || segment.height() != initial.height()) return false;
        pending = new Part(segment, fetch.apply(segment.uri()));
        waiting = false;
        notifyAll();
        return true;
    }

    synchronized Part take() throws InterruptedException {
        while (!closed && pending == null) wait();
        Part part = pending;
        pending = null;
        return part;
    }

    @Override public void close() {
        Part part;
        synchronized (this) {
            closed = true;
            part = pending;
            pending = null;
            notifyAll();
        }
        if (part != null) part.close();
    }
}
