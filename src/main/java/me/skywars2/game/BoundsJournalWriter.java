package me.skywars2.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Serial, coalescing disk writes. Only immutable coordinates cross the server-thread boundary. */
public final class BoundsJournalWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger("skywars2/map-journal");
    public record Region(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) { }
    public record Snapshot(String dimension, List<Region> bounds) {
        public Snapshot {
            if (!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                throw new IllegalArgumentException("Invalid journal dimension");
            bounds = List.copyOf(bounds);
        }
    }
    @FunctionalInterface public interface Sink { void write(Path journal, String json) throws IOException; }
    private static final class Pending {
        Snapshot snapshot;
        final CompletableFuture<Boolean> result = new CompletableFuture<>();
        Pending(Snapshot snapshot) { this.snapshot = snapshot; }
    }
    private final Set<Path> paths;
    private final LinkedHashMap<Path, Pending> pending = new LinkedHashMap<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "skywars2-map-journal");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    // Injectable file operation; neither the sink nor its worker may access live world state.
    private volatile Sink sink;
    private boolean draining;
    private boolean closed;

    public BoundsJournalWriter(Set<Path> paths) { this(paths, BoundsJournalWriter::writeAtomic); }
    BoundsJournalWriter(Set<Path> paths, Sink sink) {
        this.paths = paths.stream().map(path -> path.toAbsolutePath().normalize()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (this.paths.isEmpty() || this.paths.size() > 2) throw new IllegalArgumentException("Only the two arena journals are supported");
        this.sink = Objects.requireNonNull(sink);
    }

    /** Pending revisions share one completion: it acknowledges the latest replacement snapshot. */
    public synchronized CompletableFuture<Boolean> submit(Path path, Snapshot snapshot) {
        path = path.toAbsolutePath().normalize();
        if (!paths.contains(path)) throw new IllegalArgumentException("Unowned arena journal: " + path);
        if (closed) return CompletableFuture.completedFuture(false);
        Pending request = pending.get(path);
        if (request == null) {
            request = new Pending(Objects.requireNonNull(snapshot));
            pending.put(path, request);
        } else request.snapshot = Objects.requireNonNull(snapshot);
        if (!draining) {
            draining = true;
            worker.execute(this::drain);
        }
        return request.result;
    }

    private void drain() {
        while (true) {
            Path path;
            Pending request;
            synchronized (this) {
                var entry = pending.pollFirstEntry();
                if (entry == null) { draining = false; return; }
                path = entry.getKey();
                request = entry.getValue();
            }
            boolean success = false;
            try {
                sink.write(path, encode(request.snapshot));
                success = true;
            } catch (IOException | RuntimeException error) {
                LOGGER.error("Could not write arena bounds journal {}: {}", path, error.getMessage());
            } finally {
                request.result.complete(success);
            }
        }
    }

    private static String encode(Snapshot snapshot) {
        StringBuilder json = new StringBuilder(128 + snapshot.bounds.size() * 48);
        json.append("{\"version\":2,\"dimension\":\"").append(snapshot.dimension).append("\",\"bounds\":[");
        boolean first = true;
        for (Region box : snapshot.bounds) {
            if (!first) json.append(',');
            first = false;
            json.append('[').append(box.minX).append(',').append(box.minY).append(',').append(box.minZ)
                    .append(',').append(box.maxX).append(',').append(box.maxY).append(',').append(box.maxZ).append(']');
        }
        return json.append("]}\n").toString();
    }

    static void writeAtomic(Path journal, String json) throws IOException {
        Path temporary = journal.resolveSibling(journal.getFileName() + ".tmp");
        try {
            Files.createDirectories(journal.getParent());
            Files.writeString(temporary, json, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, journal, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, journal, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            throw error;
        }
    }

    /** Called only during server shutdown, never while processing a game tick. */
    public boolean shutdown() {
        synchronized (this) { closed = true; worker.shutdown(); }
        try { return worker.awaitTermination(30, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return false; }
    }
}
