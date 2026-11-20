package me.skywars2.media;

import java.io.InputStream;
import java.net.URI;

public final class MediaRelayEndpoint {
    public static final URI VELOCITY = URI.create("https://185.222.161.49:8447");

    private MediaRelayEndpoint() {}

    /** The bundled CA applies only to this origin, never to direct CDN requests. */
    public static InputStream certificate(URI origin) {
        if (!VELOCITY.resolve("/").equals(origin.resolve("/"))) return null;
        return MediaRelayEndpoint.class.getResourceAsStream("/skywars2/media/velocity-ca.pem");
    }
}
