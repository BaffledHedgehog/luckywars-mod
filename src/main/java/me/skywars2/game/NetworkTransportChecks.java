package me.skywars2.game;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.io.ByteArrayOutputStream;
import java.util.*;
import me.skywars2.SkyWars2;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.fabricmc.fabric.api.networking.v1.context.PacketContextProvider;
import net.minecraft.network.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;

/** Native framing/compression and actual scheduled traffic; no physical WAN latency claim. */
public final class NetworkTransportChecks {
    private NetworkTransportChecks() { }
    private static void require(boolean value, String why) {
        if (!value) throw new IllegalStateException("Network transport: " + why);
    }

    record Wire(byte[] bytes, int flushes, int frames, Map<String, Integer> packetsByType,
                Map<String, Long> bytesByType) { }

    private static final class Counter extends ChannelOutboundHandlerAdapter {
        int flushes, frames;
        String type = "configuration";
        final Map<String, Integer> packets = new TreeMap<>();
        final Map<String, Long> bytes = new TreeMap<>();
        @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof ByteBuf buffer) bytes.merge(type, (long) buffer.readableBytes(), Long::sum);
            super.write(ctx, msg, promise);
        }
        @Override public void flush(ChannelHandlerContext ctx) throws Exception { flushes++; super.flush(ctx); }
    }

    static Wire encode(ServerPlayer player, List<Packet<?>> packets, boolean optimized, int compression, boolean encrypted) {
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel();
        try {
            Connection.configureSerialization(channel.pipeline(), PacketFlow.SERVERBOUND, false, null);
            require(channel.pipeline().get(ServerNetworkTransport.HANDLER)==null,"initial negotiation must flush immediately");
            connection.configurePacketHandler(channel.pipeline());
            // EmbeddedChannel is already active before these native handlers are attached.
            connection.channelActive(channel.pipeline().context(connection));
            require(connection.isConnected() && channel.isActive(), "active native test connection");
            connection.setupOutboundProtocol(GameProtocols.CLIENTBOUND_TEMPLATE.bind(RegistryFriendlyByteBuf.decorator(player.registryAccess())));
            var installed = channel.pipeline().get(ServerNetworkTransport.HANDLER);
            require(installed != null && channel.pipeline().names().indexOf(ServerNetworkTransport.HANDLER)
                < channel.pipeline().names().indexOf("prepender"), "PLAY hook precedes native frame encoding: " + channel.pipeline().names());
            require(ServerNetworkTransport.mediaClientTicks(channel)==-1,"media pacing waits for actual client tick evidence");
            var pacing=channel.pipeline().context("skywars2_media_ticks");
            require(pacing!=null,"PLAY installs the decoded inbound media tick observer");
            channel.pipeline().addAfter("skywars2_media_ticks","media_tick_probe",new ChannelInboundHandlerAdapter(){
                @Override public void channelRead(ChannelHandlerContext ctx,Object message){ }
            });
            ((ChannelInboundHandlerAdapter)pacing.handler()).channelRead(pacing,new ServerboundClientTickEndPacket());
            require(ServerNetworkTransport.mediaClientTicks(channel)==0,"real native client tick supplies one pacing credit");
            channel.pipeline().remove("media_tick_probe");
            ServerNetworkTransport.configure(channel.pipeline(), PacketFlow.SERVERBOUND, false,ConnectionProtocol.PLAY);
            require(channel.pipeline().get(ServerNetworkTransport.HANDLER) == installed, "idempotent, per-channel handler");
            if (!optimized) channel.pipeline().remove(ServerNetworkTransport.HANDLER);
            connection.setupCompression(compression, true);
            if (encrypted) connection.setEncryptionKey(
                net.minecraft.util.Crypt.getCipher(javax.crypto.Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(new byte[16], "AES")),
                net.minecraft.util.Crypt.getCipher(javax.crypto.Cipher.ENCRYPT_MODE, new javax.crypto.spec.SecretKeySpec(new byte[16], "AES")));
            channel.runPendingTasks();
            var counter = new Counter();
            channel.pipeline().addFirst("wire_counter", counter);
            channel.pipeline().addAfter("encoder", "packet_counter", new ChannelOutboundHandlerAdapter() {
                @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if (msg instanceof Packet<?> packet) {
                        counter.type = packet.getClass().getSimpleName(); counter.frames++;
                        counter.packets.merge(counter.type, 1, Integer::sum);
                    }
                    super.write(ctx, msg, promise);
                }
            });
            burst(channel, () -> PacketContext.runWithContext((PacketContextProvider) player.connection, () -> {
                for (var packet : packets) connection.send(packet);
            }));
            channel.checkException();
            var bytes = new ByteArrayOutputStream();
            Object message;
            while ((message = channel.readOutbound()) != null) {
                try {
                    require(message instanceof ByteBuf, "native output fully encoded");
                    var buffer = (ByteBuf) message;
                    byte[] part = new byte[buffer.readableBytes()]; buffer.readBytes(part); bytes.writeBytes(part);
                } finally { ReferenceCountUtil.release(message); }
            }
            return new Wire(bytes.toByteArray(), counter.flushes, counter.frames, Map.copyOf(counter.packets), Map.copyOf(counter.bytes));
        } catch (Exception error) { throw new IllegalStateException("native wire encoder", error); }
        finally { channel.finishAndReleaseAll(); }
    }

    static Wire compare(ServerPlayer player, List<Packet<?>> packets, int compression, boolean encrypted) {
        var before = encode(player, packets, false, compression, encrypted);
        var after = encode(player, packets, true, compression, encrypted);
        require(Arrays.equals(before.bytes, after.bytes), "exact framed" + (encrypted ? "/encrypted" : "") + " bytes/order, threshold=" + compression);
        require(before.frames == after.frames && before.packetsByType.equals(after.packetsByType)
            && before.bytesByType.equals(after.bytesByType), "same native packets and bytes per type");
        require(packets.isEmpty() ? after.flushes == 0 && before.flushes == 0 : after.flushes > 0 && after.flushes < before.flushes, "fewer flushes without packet loss: " + before.flushes + " -> " + after.flushes);
        SkyWars2.LOGGER.info("[check] native-wire threshold={} encrypted={} packets={} bytes={} flushes={}->{} types={} typeBytes={}",
            compression, encrypted, after.frames, after.bytes.length, before.flushes, after.flushes, after.packetsByType, after.bytesByType);
        return after;
    }

    /** Keep EmbeddedChannel's auto-drain outside the whole simulated event-loop turn. */
    private static void burst(EmbeddedChannel channel, Runnable body) {
        channel.pipeline().addLast("burst", new ChannelOutboundHandlerAdapter() {
            @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof Runnable task) {
                    try { task.run(); promise.setSuccess(); } catch (Throwable failure) { promise.setFailure(failure); }
                } else super.write(ctx, msg, promise);
            }
        });
        var future = channel.writeOneOutbound(body);
        channel.runPendingTasks(); future.syncUninterruptibly(); channel.checkException();
    }

    private static void boundaries() {
        for (var receiving : PacketFlow.values()) for (boolean local : new boolean[]{false, true}) {
            var channel = new EmbeddedChannel();
            try {
                Connection.configureSerialization(channel.pipeline(), receiving, local, null);
                require(channel.pipeline().get(ServerNetworkTransport.HANDLER)==null,"initial protocol never coalesces");
                for(var protocol:ConnectionProtocol.values()){
                    ServerNetworkTransport.configure(channel.pipeline(),receiving,local,protocol);
                    require((channel.pipeline().get(ServerNetworkTransport.HANDLER)!=null)==(!local&&receiving==PacketFlow.SERVERBOUND&&protocol==ConnectionProtocol.PLAY),"only remote server PLAY transports: "+protocol);
                }
            } finally { channel.finishAndReleaseAll(); }
        }
        for (String boundary : List.of("idle", "read", "limit", "remove", "close", "backpressure", "exception")) {
            var counter = new Counter(); var channel = new EmbeddedChannel();
            try {
                ServerNetworkTransport.configure(channel.pipeline(), PacketFlow.SERVERBOUND, false,ConnectionProtocol.PLAY);
                // The observation handler must be downstream of the flush consolidator.
                channel.pipeline().addFirst("counter", counter);
                int size = boundary.equals("limit") ? 257 : 7;
                burst(channel, () -> {
                if (boundary.equals("read")) channel.pipeline().fireChannelRead(Unpooled.EMPTY_BUFFER);
                for (int i = 0; i < size; i++) channel.pipeline().writeAndFlush(Unpooled.buffer(4).writeInt(i));
                require(counter.flushes == (boundary.equals("limit") ? 1 : 0), "bounded deferred flushes before " + boundary + ": " + counter.flushes);
                switch (boundary) {
                    case "read" -> channel.pipeline().fireChannelReadComplete();
                    case "remove" -> channel.pipeline().remove(ServerNetworkTransport.HANDLER);
                    case "close" -> channel.close();
                    case "backpressure" -> channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
                    case "exception" -> channel.pipeline().fireExceptionCaught(new IllegalStateException("expected transport test"));
                    default -> channel.runPendingTasks();
                }
                // EmbeddedChannel records the propagated test exception at the tail.
                if (boundary.equals("exception")) {
                    boolean observed = false;
                    try { channel.checkException(); } catch (IllegalStateException expected) { observed = expected.getMessage().equals("expected transport test"); }
                    require(observed, "exception still propagated");
                }
                });
                require(counter.flushes == (boundary.equals("limit") ? 2 : 1), "drain at " + boundary);
                for (int i = 0; i < size; i++) {
                    ByteBuf buffer = channel.readOutbound();
                    require(buffer != null, "every buffered write survives " + boundary);
                    try { require(buffer.readInt() == i, "write order at " + boundary); } finally { buffer.release(); }
                }
                require(channel.readOutbound() == null, "no duplicate bytes at " + boundary);
            } finally { channel.finishAndReleaseAll(); }
        }
    }

    /** Real native LOGIN codecs and SetCompression write-completion ordering. */
    private static void loginCompression() {
        for(boolean oldBoundary:new boolean[]{true,false}){
            var connection=new Connection(PacketFlow.SERVERBOUND);var server=new EmbeddedChannel();
            byte[] wire;
            try{
                Connection.configureSerialization(server.pipeline(),PacketFlow.SERVERBOUND,false,null);
                connection.configurePacketHandler(server.pipeline());connection.channelActive(server.pipeline().context(connection));
                connection.setupOutboundProtocol(net.minecraft.network.protocol.login.LoginProtocols.CLIENTBOUND);
                require(server.pipeline().get(ServerNetworkTransport.HANDLER)==null,"LOGIN protocol keeps immediate flush semantics");
                // Negative control reinstalls the exact old optimization. It must
                // reproduce the user's invalid zlib stream, not merely differ.
                if(oldBoundary)server.pipeline().addFirst(ServerNetworkTransport.HANDLER,new io.netty.handler.flush.FlushConsolidationHandler(256,true));
                burst(server,()->{
                    connection.send(new net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket(256),
                        PacketSendListener.thenRun(()->connection.setupCompression(256,true)));
                    connection.send(new net.minecraft.network.protocol.login.ClientboundHelloPacket("",new byte[4096],new byte[4],false));
                    connection.send(new net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket(
                        new com.mojang.authlib.GameProfile(new UUID(1,2),"TransportProbe"),new UUID(3,4)));
                });
                server.checkException();var collected=new ByteArrayOutputStream();ByteBuf data;
                while((data=server.readOutbound())!=null){try{byte[] bytes=new byte[data.readableBytes()];data.readBytes(bytes);collected.writeBytes(bytes);}finally{data.release();}}
                wire=collected.toByteArray();
            }catch(Exception failure){throw new IllegalStateException("LOGIN wire production",failure);}
            finally{server.finishAndReleaseAll();}
            var decoded=new ArrayList<Packet<?>>();var client=new EmbeddedChannel();boolean invalid=false;
            try{
                client.pipeline().addLast("splitter",new Varint21FrameDecoder(null));
                client.pipeline().addLast("login",new ChannelInboundHandlerAdapter(){
                    @Override public void channelRead(ChannelHandlerContext ctx,Object message){
                        var buffer=(ByteBuf)message;
                        try{
                            var packet=net.minecraft.network.protocol.login.LoginProtocols.CLIENTBOUND.codec().decode(buffer);
                            require(!buffer.isReadable(),"complete native LOGIN payload");decoded.add(packet);
                            if(packet instanceof net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket compression)
                                ctx.pipeline().addBefore("login","decompress",new CompressionDecoder(compression.getCompressionThreshold(),false));
                        }finally{buffer.release();}
                    }
                });
                client.writeInbound(Unpooled.wrappedBuffer(wire));client.checkException();
            }catch(Exception failure){
                for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(cause instanceof java.util.zip.DataFormatException)invalid=true;
                if(!oldBoundary||!invalid)throw new IllegalStateException("LOGIN native client decode",failure);
            }finally{client.finishAndReleaseAll();}
            if(oldBoundary)require(invalid,"old deferred LOGIN flush must reproduce invalid compression");
            else require(!invalid&&decoded.size()==3&&decoded.getLast() instanceof net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket,"native compressed LOGIN succeeds through LoginFinished");
        }
        SkyWars2.LOGGER.info("[check] native-login-wire PASS: original flush race reproduces DataFormatException; fixed SetCompression plus large/small LOGIN payloads decode");
    }

    private static void rayTraffic(GameController game) {
        try (var f = new WoolBucketChecks.Fixture(game, false)) {
            f.prepareParticipants(); f.move(f.owner, f.home.add(0, 60, 0));
            f.move(f.other, f.owner.position().add(10, 0, 0));
            f.owner.setYRot(0); f.owner.setXRot(0);
            var from = f.owner.getEyePosition(); var direction = f.owner.getLookAngle().normalize();
            for (int i = 0; i <= 20; i++) {
                var pos = net.minecraft.core.BlockPos.containing(from.add(direction.scale(i * .35)));
                require(f.level.hasChunkAt(pos) && f.level.getBlockState(pos).isAir(), "resident empty native ray scene");
            }
            f.client.packets().clear();
            require(game.items().bolts().useInfbolt(f.owner, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(LbcWands.INFBOLT)), "real native Infinite Bolt accepted");
            List<Packet<?>> modern = f.client.packets().stream().filter(p -> p instanceof ClientboundLevelParticlesPacket).toList();
            require(modern.size() == 7 && modern.stream().map(ClientboundLevelParticlesPacket.class::cast).mapToInt(ClientboundLevelParticlesPacket::count).sum() == 21,
                "seven native packets retain all21 ray particles");
            var particle = ((ClientboundLevelParticlesPacket) modern.getFirst()).particle();
            f.client.packets().clear();
            // Replay the original 21 native sendParticles calls at their original sample positions.
            for (int i = 0; i <= 20; i++) {
                var at = from.add(direction.scale(i * .35));
                f.level.sendParticles(particle, at.x, at.y, at.z, 1, .01, .01, .01, 0);
            }
            List<Packet<?>> original = f.client.packets().stream().filter(p -> p instanceof ClientboundLevelParticlesPacket).toList();
            var before = encode(f.owner, original, false, 256, false);
            var after = encode(f.owner, modern, true, 256, false);
            require(before.frames == 21 && after.frames == 7 && after.bytes.length < before.bytes.length / 2,
                "native ray payload and packet reduction");
            SkyWars2.LOGGER.info("[check] native-ray-wire InfiniteBolt particles=21 packets={}->{} bytes={}->{}", before.frames, after.frames, before.bytes.length, after.bytes.length);
            f.client.packets().clear();
            try {
                var ray = LbcBolts.class.getDeclaredMethod("rayAt", ServerPlayer.class, net.minecraft.world.phys.Vec3.class,
                    net.minecraft.world.phys.Vec3.class, double.class, net.minecraft.core.particles.ParticleOptions.class, boolean.class, boolean.class);
                ray.setAccessible(true);
                require(ray.invoke(game.items().bolts(), f.owner, from, direction, 7.0, net.minecraft.core.particles.ParticleTypes.CLOUD, false, false) == null,
                    "real common ray retains empty terminal");
            } catch (ReflectiveOperationException error) { throw new IllegalStateException("native shared ray", error); }
            List<Packet<?>> shared = f.client.packets().stream().filter(p -> p instanceof ClientboundLevelParticlesPacket).toList();
            require(shared.size() == 10 && shared.stream().map(ClientboundLevelParticlesPacket.class::cast).mapToInt(ClientboundLevelParticlesPacket::count).sum() == 30,
                "both native cloud/soul trails retain all30 particles in10 packets");
            f.client.packets().clear();
            for (int i = 0; i <= 14; i++) {
                var at = from.add(direction.scale(i * .5));
                f.level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, true, false, at.x, at.y, at.z, 1, .1, .1, .1, 0);
                f.level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, at.x, at.y, at.z, 1, .1, .1, .1, 0);
            }
            List<Packet<?>> source = f.client.packets().stream().filter(p -> p instanceof ClientboundLevelParticlesPacket).toList();
            var sharedBefore = encode(f.owner, source, false, 256, false);
            var sharedAfter = encode(f.owner, shared, true, 256, false);
            require(sharedBefore.frames == 30 && sharedAfter.frames == 10 && sharedAfter.bytes.length < sharedBefore.bytes.length / 2,
                "common native ray wire reduction");
            SkyWars2.LOGGER.info("[check] native-ray-wire CommonCloud particles=30 packets={}->{} bytes={}->{}", sharedBefore.frames, sharedAfter.frames, sharedBefore.bytes.length, sharedAfter.bytes.length);
        }
    }

    public static void run(GameController game) {
        boundaries();loginCompression();
        var player = Objects.requireNonNull(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha")));
        List<Packet<?>> packets = new ArrayList<>();
        for (int i = 0; i < 300; i++) packets.add(new ClientboundSystemChatPacket(Component.literal("native-packet-" + i + "-" + "payload".repeat(i % 4 == 0 ? 100 : 1)), false));
        packets.add(new ClientboundBundlePacket(List.of(new ClientboundSystemChatPacket(Component.literal("bundle-a"), false), new ClientboundSystemChatPacket(Component.literal("bundle-b"), false))));
        for (int threshold : new int[]{-1, 256}) for (boolean encrypted : new boolean[]{false, true}) compare(player, packets, threshold, encrypted);
        rayTraffic(game);
        SkyWars2.LOGGER.info("[check] PASS network-transport (native LOGIN compression transition with negative control; PLAY compression/encryption/bundles, byte-exact order, idle/read/limit/close/backpressure/removal/exception)");
    }
}
