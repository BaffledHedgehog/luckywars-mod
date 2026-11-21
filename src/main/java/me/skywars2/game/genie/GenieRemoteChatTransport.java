package me.skywars2.game.genie;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import me.skywars2.game.genie.script.ScriptJson;

/** A pinned, authenticated queue to the dedicated browser on a separate hosting machine. */
final class GenieRemoteChatTransport implements GenieScriptModel.Transport {
    private static final int MAX_REPLY = 4_194_304;
    private final HttpClient client;
    private final URI base;
    private final String token;

    GenieRemoteChatTransport(String address, String token, String certificateSha256) throws IOException {
        this.base = URI.create(address);
        if (!"https".equals(base.getScheme()) || base.getHost() == null || base.getUserInfo() != null
                || base.getQuery() != null || base.getFragment() != null || !"/v1/".equals(base.getPath())
                || !token.matches("[A-Za-z0-9_-]{43,128}") || !certificateSha256.matches("[a-f0-9]{64}")) {
            throw new IOException("Invalid hosted ChatGPT gateway configuration");
        }
        this.token = token;
        try {
            byte[] pin = HexFormat.of().parseHex(certificateSha256);
            var tls = SSLContext.getInstance("TLS");
            tls.init(null, new TrustManager[] { new X509TrustManager() {
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                @Override public void checkClientTrusted(X509Certificate[] chain, String auth) throws CertificateException {
                    throw new CertificateException("Client authentication is unsupported");
                }
                @Override public void checkServerTrusted(X509Certificate[] chain, String auth) throws CertificateException {
                    if (chain == null || chain.length == 0) throw new CertificateException("Missing gateway certificate");
                    chain[0].checkValidity();
                    try {
                        if (!MessageDigest.isEqual(pin, MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded()))) {
                            throw new CertificateException("Gateway certificate does not match configured pin");
                        }
                    } catch (GeneralSecurityException failure) { throw new CertificateException(failure); }
                }
            } }, null);
            // Standard HTTPS hostname verification is retained in addition to the exact certificate pin.
            client = HttpClient.newBuilder().sslContext(tls).connectTimeout(Duration.ofSeconds(10)).build();
        } catch (GeneralSecurityException failure) { throw new IOException("Cannot initialize gateway TLS", failure); }
    }

    void connect() throws IOException, InterruptedException {
        var result = send("probe", "GET", null);
        if (!result.has("ready") || !result.get("ready").getAsBoolean()) throw unavailable(result);
    }

    @Override public GenieScriptModel.Observation run(String prompt, String knowledge, String contract, byte[] verifier)
            throws IOException, InterruptedException {
        String path = "jobs/" + UUID.randomUUID();
        var input = new JsonObject();
        input.addProperty("prompt", prompt);
        input.addProperty("knowledge", knowledge);
        input.addProperty("contract", contract);
        input.addProperty("verifier", Base64.getEncoder().encodeToString(verifier));
        try {
            send(path, "PUT", ScriptJson.canonical(input));
            for (;;) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                var status = send(path, "GET", null);
                String state = status.has("state") ? status.get("state").getAsString() : "";
                if (state.equals("failed")) throw unavailable(status);
                if (state.equals("complete")) {
                    var result = status.getAsJsonObject("result");
                    var receipts = new ArrayList<JsonObject>();
                    for (var receipt : result.getAsJsonArray("executions")) receipts.add(receipt.getAsJsonObject());
                    return new GenieScriptModel.Observation(result.get("reply").getAsString(), List.copyOf(receipts));
                }
                if (!state.equals("queued") && !state.equals("running")) throw unavailable(status);
                // Polls renew the remote lease. There is no deadline while the wish remains active.
                Thread.sleep(1000);
            }
        } finally {
            boolean interrupted = Thread.interrupted();
            try { send(path, "DELETE", null); }
            catch (IOException ignored) { /* A lost client lease also cancels and erases the request. */ }
            catch (InterruptedException canceled) { interrupted = true; }
            finally { if (interrupted) Thread.currentThread().interrupt(); }
        }
    }

    private JsonObject send(String path, String method, String body) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(path.equals("probe") ? 75 : 20))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        try (var stream = response.body()) {
            byte[] bytes = stream.readNBytes(MAX_REPLY + 1);
            if (bytes.length > MAX_REPLY) throw new IOException("Hosted ChatGPT response exceeds limit");
            var result = ScriptJson.parse(new String(bytes, StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw unavailable(result);
            return result;
        }
    }

    private static GenieHostedChatTransport.Unavailable unavailable(JsonObject response) {
        if (response.has("issue")) {
            for (var issue : GenieHostedChatTransport.Issue.values()) {
                if (issue.name().equals(response.get("issue").getAsString())) return new GenieHostedChatTransport.Unavailable(issue);
            }
        }
        return new GenieHostedChatTransport.Unavailable(GenieHostedChatTransport.Issue.UNAVAILABLE);
    }
}
