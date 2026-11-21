package me.skywars2.game.genie;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import me.skywars2.game.genie.script.ScriptJson;

/** A real loopback HTTPS peer: pinning, bearer auth, polling, cancellation, and error mapping. */
public final class GenieRemoteChatChecks {
    public static void main(String[] args) throws Exception {
        var store = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(Path.of(args[0]))) { store.load(input, "fixture-only".toCharArray()); }
        var keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, "fixture-only".toCharArray());
        var tls = SSLContext.getInstance("TLS");
        tls.init(keys.getKeyManagers(), null, null);
        String pin = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(store.getCertificate("fixture").getEncoded()));
        String token = "s".repeat(43);
        var polls = new AtomicInteger();
        var deleted = new AtomicInteger();
        var mode = new AtomicInteger();
        var waiting = new CountDownLatch(1);
        var server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(tls));
        server.createContext("/v1/", exchange -> {
            require(exchange.getRequestHeaders().getFirst("Authorization").equals("Bearer " + token), "bearer auth");
            String body;
            if (exchange.getRequestMethod().equals("DELETE")) {
                deleted.incrementAndGet(); body = "{}";
            } else if (exchange.getRequestURI().getPath().endsWith("probe")) {
                body = "{\"ready\":true}";
            } else if (exchange.getRequestMethod().equals("PUT")) {
                var request = ScriptJson.parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                require(request.get("knowledge").getAsString().equals("{}"), "knowledge survives encoding");
                require(request.get("verifier").getAsString().equals("eA=="), "original verifier bytes");
                polls.set(0); body = "{\"state\":\"queued\"}";
            } else if (mode.get() == 1) {
                waiting.countDown(); body = "{\"state\":\"running\"}";
            } else if (mode.get() == 2) {
                body = "{\"state\":\"failed\",\"issue\":\"AUTH_REQUIRED\"}";
            } else if (polls.incrementAndGet() == 1) {
                body = "{\"state\":\"running\"}";
            } else {
                body = "{\"state\":\"complete\",\"result\":{\"reply\":\"{\\\"decision\\\":\\\"reject\\\"}\",\"executions\":[]}}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            String url = "https://127.0.0.1:" + server.getAddress().getPort() + "/v1/";
            var remote = new GenieRemoteChatTransport(url, token, pin);
            remote.connect();
            var observation = remote.run("wish", "{}", "{}", new byte[]{'x'});
            require(observation.reply().equals("{\"decision\":\"reject\"}"), "exact model reply");
            require(deleted.get() == 1 && polls.get() == 2, "poll and result cleanup");
            try {
                new GenieRemoteChatTransport(url, token, "0".repeat(64)).connect();
                throw new AssertionError("wrong certificate pin accepted");
            } catch (javax.net.ssl.SSLHandshakeException expected) { }
            mode.set(1);
            var failure = new AtomicReference<Throwable>();
            var task = new Thread(() -> {
                try { remote.run("cancel", "{}", "{}", new byte[]{'x'}); }
                catch (Throwable thrown) { failure.set(thrown); }
            });
            task.start();
            require(waiting.await(5, TimeUnit.SECONDS), "running job reached remote");
            task.interrupt(); task.join(Duration.ofSeconds(5));
            require(!task.isAlive() && failure.get() instanceof InterruptedException && deleted.get() == 2,
                    "interruption cancels remote job without losing cleanup");
            mode.set(2);
            try {
                remote.run("auth", "{}", "{}", new byte[]{'x'});
                throw new AssertionError("logged-out account accepted");
            } catch (GenieHostedChatTransport.Unavailable expected) {
                require(expected.issue == GenieHostedChatTransport.Issue.AUTH_REQUIRED, "auth issue retained");
            }
            require(deleted.get() == 3, "failed job erased");
            System.out.println("PASS: real pinned HTTPS, bearer auth, payload, result polling, cancel cleanup and authentication failures");
        } finally { server.stop(0); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
