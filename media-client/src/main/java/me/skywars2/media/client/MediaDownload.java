package me.skywars2.media.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.resolver.NoopAddressResolverGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import javax.net.ssl.SNIHostName;
import me.skywars2.media.MediaClip;
import me.skywars2.media.MediaRelayEndpoint;

/** Downloads one selected clip without using the system proxy or storing credentials. */
final class MediaDownload {
    static final long MAX_BYTES = 64L * 1024 * 1024;
    private static final int PORT = 443;
    private static final int MAX_REDIRECTS = 3;
    private static final int CONNECT_MILLIS = 8_000;
    private static final int TOTAL_SECONDS = 60;
    private static final String RELAY_PREFIX = "/luckywars-media/";
    private static final Set<String> CDN_ROOTS = Set.of("tiktokcdn.com", "tiktokcdn-us.com",
            "tiktokcdn-eu.com", "tiktokcdn-in.com", "tiktokv.com", "muscdn.com");
    private static final Pattern DNS_HOST = Pattern.compile(
            "(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?");
    private static final Pattern IPV4 = Pattern.compile("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}");
    private static final Pattern MEDIA_PATH = Pattern.compile("/[A-Za-z0-9._~/-]+");
    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes 0-([0-9]{1,9})/([0-9]{1,9})");
    private static final System.Logger LOGGER = System.getLogger(MediaDownload.class.getName());

    private final Path temporaryDirectory;
    private final URI relayOrigin;

    /** The relay origin must come from trusted client configuration, never from a clip packet. */
    MediaDownload(Path temporaryDirectory, URI trustedVelocityOrigin) {
        this.temporaryDirectory = temporaryDirectory;
        this.relayOrigin = trustedVelocityOrigin;
        if (relayOrigin != null) validateRelayOrigin(relayOrigin);
    }

    Transfer direct(URI uri) {
        return start(uri, true, MAX_BYTES);
    }

    /** Call after a CDN failure or after releasing an unsupported CDN file. */
    Transfer fallback(URI uri) {
        return start(uri, false, MAX_BYTES);
    }

    Transfer segment(URI uri) {
        return start(uri, false, MediaClip.MAX_SEGMENT_BYTES);
    }

    private Transfer start(URI uri, boolean direct, long maxBytes) {
        validate(uri, direct, relayOrigin);
        return new Transfer(temporaryDirectory, uri, direct, relayOrigin, maxBytes);
    }

    static String validateRelayOrigin(URI origin) {
        String host = httpsHost(origin);
        if (!origin.getRawPath().isEmpty() && !origin.getRawPath().equals("/") || origin.getRawQuery() != null)
            throw new IllegalArgumentException("Media relay must be a HTTPS origin");
        return host;
    }

    static void validate(URI uri, boolean direct, URI relayOrigin) {
        String host = httpsHost(uri);
        String path = uri.getRawPath();
        if (!MEDIA_PATH.matcher(path).matches() || !uri.normalize().getRawPath().equals(path))
            throw new IllegalArgumentException("Invalid media path");
        if (direct) {
            if (uri.getPort() != -1 || !path.startsWith("/video/tos/") || path.length() == "/video/tos/".length()
                    || CDN_ROOTS.stream().noneMatch(root -> host.equals(root) || host.endsWith("." + root)))
                throw new IllegalArgumentException("Media URL is outside the TikTok CDN");
        } else if (relayOrigin == null || !host.equals(validateRelayOrigin(relayOrigin)) || port(uri) != port(relayOrigin)
                || !path.startsWith(RELAY_PREFIX) || path.length() == RELAY_PREFIX.length()) {
            throw new IllegalArgumentException("Media URL is outside the configured relay");
        }
    }

    private static String httpsHost(URI uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                || uri.getPort() == 0 || uri.getPort() > 65535
                || uri.toString().length() > 4096 || uri.toString().chars().anyMatch(c -> c < 33 || c > 126))
            throw new IllegalArgumentException("Invalid HTTPS media URL");
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String expectedAuthority = host + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
        boolean ip = IPV4.matcher(host).matches();
        if (!uri.getRawAuthority().equalsIgnoreCase(expectedAuthority)
                || !(ip ? publicAddress(InetAddress.ofLiteral(host)) : DNS_HOST.matcher(host).matches())
                || host.endsWith(".local") || host.endsWith(".localhost"))
            throw new IllegalArgumentException("Invalid media host");
        return host;
    }

    private static int port(URI uri) { return uri.getPort() < 0 ? PORT : uri.getPort(); }

    static boolean publicAddress(InetAddress address) {
        byte[] ip = address.getAddress();
        int a = ip[0] & 0xff, b = ip[1] & 0xff, c = ip[2] & 0xff;
        if (ip.length == 4) {
            return a != 0 && a != 10 && a != 127 && a < 224 && !(a == 100 && b >= 64 && b <= 127)
                    && !(a == 169 && b == 254) && !(a == 172 && b >= 16 && b <= 31)
                    && !(a == 192 && (b == 168 || b == 0 && (c == 0 || c == 2) || b == 88 && c == 99))
                    && !(a == 198 && (b == 18 || b == 19 || b == 51 && c == 100))
                    && !(a == 203 && b == 0 && c == 113);
        }
        // Require current global unicast and exclude transition and documentation ranges.
        return (a & 0xe0) == 0x20 && !(a == 0x20 && (b == 1 && (c < 2 || c == 0x0d && (ip[3] & 0xff) == 0xb8) || b == 2))
                && !(a == 0x3f && b == 0xff && (c & 0xf0) == 0);
    }

    /** Owns the partial and completed file. Close the decoder before closing this handle. */
    static final class Transfer implements MediaSegmentQueue.Download {
        private final CompletableFuture<Path> result = new CompletableFuture<>();
        private final Thread worker;
        private final long maxBytes;
        private boolean closed;
        private Channel channel;
        private Path retained;

        private Transfer(Path directory, URI uri, boolean direct, URI relayOrigin, long maxBytes) {
            this.maxBytes = maxBytes;
            worker = Thread.ofVirtual().name("skywars-media-download").unstarted(() -> download(directory, uri, direct, relayOrigin));
            result.orTimeout(TOTAL_SECONDS, TimeUnit.SECONDS).whenComplete((path, failure) -> {
                if (failure instanceof TimeoutException) cancel();
            });
            worker.start();
        }

        @Override public CompletionStage<Path> file() {
            return result.minimalCompletionStage();
        }

        @Override
        public void close() {
            result.cancel(false);
            cancel();
        }

        private void cancel() {
            Channel pending;
            Path complete;
            synchronized (this) {
                if (closed) return;
                closed = true;
                pending = channel;
                complete = retained;
                retained = null;
            }
            if (pending != null) pending.close();
            worker.interrupt();
            // An already downloaded file is closed before publication and has one owner.
            delete(complete);
        }

        private void download(Path directory, URI uri, boolean direct, URI relayOrigin) {
            Path temporary = null;
            try {
                Files.createDirectories(directory);
                temporary = Files.createTempFile(directory, "tiktok-", ".mp4");
                try (var file = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                    fetch(uri, direct, relayOrigin, file);
                }
                Path completed = temporary;
                synchronized (this) {
                    if (closed) return;
                    retained = completed;
                    temporary = null;
                }
                result.complete(completed);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                result.cancel(false);
            } catch (IOException | RuntimeException failure) {
                result.completeExceptionally(failure);
            } finally {
                delete(temporary);
            }
        }

        private void fetch(URI uri, boolean direct, URI relayOrigin, FileChannel file) throws IOException, InterruptedException {
            var tlsBuilder = SslContextBuilder.forClient().sslProvider(SslProvider.JDK);
            SslContext tls;
            try (var relayCertificate = direct ? null : MediaRelayEndpoint.certificate(relayOrigin)) {
                if (relayCertificate != null) tlsBuilder.trustManager(relayCertificate);
                tls = tlsBuilder.build();
            }
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                validate(uri, direct, relayOrigin);
                String next = fetchOne(uri, file, tls, direct);
                if (next == null) return;
                try {
                    uri = uri.resolve(URI.create(next));
                } catch (IllegalArgumentException invalid) {
                    // A CDN redirect can contain a signed query. Keep it out of exception logs.
                    throw new IOException("Invalid media redirect");
                }
            }
            throw new IOException("Too many media redirects");
        }

        private String fetchOne(URI uri, FileChannel file, SslContext tls, boolean direct) throws IOException, InterruptedException {
            String host = httpsHost(uri);
            int port = port(uri);
            InetAddress[] addresses = InetAddress.getAllByName(host);
            IOException unreachable = null;
            for (InetAddress address : addresses) {
                if (!publicAddress(address)) throw new IOException("Media host resolved to a non-public address");
            }
            for (InetAddress address : addresses) {
                if (Thread.interrupted()) throw new InterruptedException();
                var response = new CompletableFuture<String>();
                var request = new Bootstrap().group(Network.LOOP).channel(NioSocketChannel.class)
                        .resolver(NoopAddressResolverGroup.INSTANCE)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_MILLIS)
                        .handler(new ChannelInitializer<Channel>() {
                            @Override protected void initChannel(Channel transport) {
                                var ssl = tls.newHandler(transport.alloc(), host, port);
                                var parameters = ssl.engine().getSSLParameters();
                                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                                if (!IPV4.matcher(host).matches()) parameters.setServerNames(List.of(new SNIHostName(host)));
                                ssl.engine().setSSLParameters(parameters);
                                ssl.setHandshakeTimeoutMillis(CONNECT_MILLIS);
                                transport.pipeline().addLast(ssl, new HttpClientCodec(), new Response(response, file, direct, maxBytes));
                            }
                        }).connect(new InetSocketAddress(address, port));
                Channel transport = request.channel();
                try {
                    synchronized (this) {
                        if (closed) throw new InterruptedException();
                        channel = transport;
                    }
                    if (!request.await().isSuccess()) {
                        unreachable = new IOException("Media connection failed", request.cause());
                        continue;
                    }
                    String target = uri.getRawPath();
                    if (uri.getRawQuery() != null) target += "?" + uri.getRawQuery();
                    var message = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, target);
                    message.headers().set(HttpHeaderNames.HOST, uri.getRawAuthority()).set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                            .set(HttpHeaderNames.ACCEPT, "video/*, application/octet-stream")
                            .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.IDENTITY);
                    if (direct) {
                        message.headers().set(HttpHeaderNames.REFERER, "https://www.tiktok.com/")
                                .set(HttpHeaderNames.RANGE, "bytes=0-")
                                .set(HttpHeaderNames.USER_AGENT, "Mozilla/5.0");
                    }
                    transport.writeAndFlush(message).addListener(written -> {
                        if (!written.isSuccess()) response.completeExceptionally(written.cause());
                    });
                    return response.get();
                } catch (ExecutionException error) {
                    throw new IOException("Media response failed", error.getCause());
                } finally {
                    transport.close();
                    synchronized (this) {
                        if (channel == transport) channel = null;
                    }
                }
            }
            throw unreachable == null ? new IOException("No media address") : unreachable;
        }
    }

    private static final class Network {
        static final MultiThreadIoEventLoopGroup LOOP = new MultiThreadIoEventLoopGroup(1,
                new DefaultThreadFactory("skywars-media-https", true, Thread.MIN_PRIORITY), NioIoHandler.newFactory());
    }

    /** Completes with a redirect or with null after successfully writing the entire response. */
    private static final class Response extends SimpleChannelInboundHandler<HttpObject> {
        private final CompletableFuture<String> result;
        private final FileChannel file;
        private final boolean direct;
        private final long maxBytes;
        private boolean headers;
        private long length = -1, read;

        Response(CompletableFuture<String> result, FileChannel file, boolean direct, long maxBytes) {
            this.result = result;
            this.file = file;
            this.direct = direct;
            this.maxBytes = maxBytes;
        }

        @Override protected void channelRead0(ChannelHandlerContext context, HttpObject object) throws IOException {
            if (result.isDone()) return;
            if (!object.decoderResult().isSuccess()) throw new IOException("Invalid media HTTP response");
            if (object instanceof HttpResponse response) {
                headers = true;
                int status = response.status().code();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    var locations = response.headers().getAll(HttpHeaderNames.LOCATION);
                    if (locations.size() != 1 || locations.getFirst().length() > 4096)
                        throw new IOException("Invalid media redirect");
                    result.complete(locations.getFirst());
                    return;
                }
                if (status != 200 && !(direct && status == 206)) throw new IOException("Media HTTP status " + status);
                String encoding = response.headers().get(HttpHeaderNames.CONTENT_ENCODING);
                if (encoding != null && !encoding.equalsIgnoreCase("identity")) throw new IOException("Encoded media HTTP body");
                String type = response.headers().get(HttpHeaderNames.CONTENT_TYPE);
                if (type == null || !mediaType(type)) throw new IOException("Unsupported media content type");
                length = HttpUtil.getContentLength(response, -1);
                if (status == 206) {
                    String range = response.headers().get(HttpHeaderNames.CONTENT_RANGE, "");
                    var matches = CONTENT_RANGE.matcher(range);
                    if (!matches.matches()) throw new IOException("Invalid media content range");
                    long entire = Long.parseLong(matches.group(2));
                    if (Long.parseLong(matches.group(1)) + 1 != entire || length >= 0 && length != entire)
                        throw new IOException("Partial media file");
                    length = entire;
                }
                if (length == 0 || length > maxBytes) throw new IOException("Media response size exceeds its limit");
            }
            if (object instanceof HttpContent content) {
                if (!headers) throw new IOException("Missing media response headers");
                var data = content.content();
                read += data.readableBytes();
                if (read > maxBytes || length >= 0 && read > length) throw new IOException("Media response size exceeds its limit");
                while (data.isReadable()) {
                    if (data.readBytes(file, data.readableBytes()) == 0) throw new IOException("Could not write media file");
                }
                if (content instanceof LastHttpContent) {
                    if (read == 0 || length >= 0 && read != length) throw new IOException("Incomplete media file");
                    result.complete(null);
                }
            }
        }

        private static boolean mediaType(String value) {
            String mime = value.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            return mime.startsWith("video/") || mime.equals("application/mp4")
                    || mime.equals("application/octet-stream") || mime.equals("binary/octet-stream");
        }

        @Override public void channelInactive(ChannelHandlerContext context) {
            result.completeExceptionally(new IOException("Incomplete media response"));
            context.fireChannelInactive();
        }

        @Override public void exceptionCaught(ChannelHandlerContext context, Throwable failure) {
            result.completeExceptionally(failure);
            context.close();
        }
    }

    private static void delete(Path owned) {
        if (owned == null) return;
        try {
            Files.deleteIfExists(owned);
        } catch (IOException failure) {
            LOGGER.log(System.Logger.Level.WARNING, "Could not remove a temporary TikTok clip");
        }
    }
}
