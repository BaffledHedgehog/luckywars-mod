package me.skywars2.media.client;

import java.net.InetAddress;
import java.net.URI;

/** Local transport policy checks. No DNS or HTTPS requests. */
public final class MediaDownloadChecks {
    private static final URI RELAY = URI.create("https://media.example.com");
    private MediaDownloadChecks() { }

    public static void main(String[] args) throws Exception {
        MediaDownload.validate(URI.create("https://v16-webapp-prime.us.tiktokcdn.com/video/tos/abc/?expire=4000000000"), true, null);
        MediaDownload.validate(URI.create("https://v19.tiktokcdn-eu.com/video/tos/clip"), true, null);
        MediaDownload.validate(URI.create("https://v16.tiktokv.com/video/tos/clip"), true, null);
        MediaDownload.validate(URI.create("https://media.example.com:443/luckywars-media/clip_42.mp4?t=signed%3Dvalue"), false, RELAY);
        MediaDownload.validate(URI.create("https://1.1.1.1:8447/luckywars-media/clip_42"), false,
                URI.create("https://1.1.1.1:8447"));
        deny("http://v16.tiktokcdn.com/video/tos/clip", true);
        deny("https://tiktokcdn.com.example.com/video/tos/clip", true);
        deny("https://media.example.com/video/tos/clip", true);
        deny("https://tiktokcdn.com:443/video/tos/clip", true);
        deny("https://tiktokcdn.com:/video/tos/clip", true);
        deny("https://user@tiktokcdn.com/video/tos/clip", true);
        deny("https://tiktokcdn.com/video/tos/", true);
        deny("https://tiktokcdn.com/video/tos/../secret", true);
        deny("https://tiktokcdn.com/video/tos/clip#part", true);
        deny("https://tiktokcdn.com/luckywars-media/clip.mp4", false);
        deny("https://media.example.com:8443/luckywars-media/clip.mp4", false);
        deny("https://media.example.com/luckywars-media/%2e%2e/secret", false);
        deny("https://media.example.com/luckywars-media/../secret", false);
        deny("https://media.example.com/luckywars-media/", false);
        for (String host : new String[]{"0.0.0.0", "10.1.2.3", "100.64.1.2", "127.0.0.1",
                "169.254.169.254", "172.31.1.2", "192.0.2.1", "192.168.1.2", "198.18.0.1",
                "224.0.0.1", "::1", "::ffff:127.0.0.1", "fd00::1", "fe80::1", "2001:db8::1", "2002::1"}) {
            require(!MediaDownload.publicAddress(InetAddress.ofLiteral(host)), "public address filter denied " + host);
        }
        require(MediaDownload.publicAddress(InetAddress.ofLiteral("1.1.1.1")), "global IPv4");
        require(MediaDownload.publicAddress(InetAddress.ofLiteral("2606:4700:4700::1111")), "global IPv6");
        MediaPreloadChecks.run();
        System.out.println("MediaDownloadChecks passed");
    }

    private static void deny(String uri, boolean direct) {
        try {
            MediaDownload.validate(URI.create(uri), direct, RELAY);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Media route unexpectedly accepted");
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
