package de.t0bx.sentiencefriends.proxy.netty;

import de.t0bx.sentiencefriends.api.netty.codec.PacketDecoder;
import de.t0bx.sentiencefriends.api.netty.codec.PacketEncoder;
import de.t0bx.sentiencefriends.api.netty.codec.VarIntFrameDecoder;
import de.t0bx.sentiencefriends.api.network.FriendsPacket;
import de.t0bx.sentiencefriends.proxy.ProxyPlugin;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MasterNettyManager {

    private int port;
    private final Logger logger = ProxyPlugin.getInstance().getLogger();

    @Getter
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    private Thread nettyThread;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private Channel serverChannel;

    public MasterNettyManager(int port) {
        this.port = port;

        this.nettyThread = new Thread(() -> {
            try {
                this.createNettyPipeline();
            } catch (InterruptedException exception) {
                this.logger.error("Netty Thread interrupted", exception);
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                this.logger.error("Failed to start Netty server", exception);
            }
        }, "Netty-Thread");

        this.nettyThread.setDaemon(true);
        this.nettyThread.start();
    }

    private void createNettyPipeline() throws InterruptedException {
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("varint-frame", new VarIntFrameDecoder());
                            pipeline.addLast("packet-decoder", new PacketDecoder());
                            pipeline.addLast("packet-encoder", new PacketEncoder());
                            pipeline.addLast("packet-handler", new PacketHandler());
                        }
                    });

            ChannelFuture future = bootstrap.bind(this.port).sync();
            this.serverChannel = future.channel();

            this.logger.info("Netty server started on port {}", this.port);

            this.serverChannel.closeFuture().sync();
        } finally {
            if (this.bossGroup != null) this.bossGroup.shutdownGracefully().sync();
            if (this.workerGroup != null) this.workerGroup.shutdownGracefully().sync();
            this.logger.info("Netty EventLoopGroups shut down gracefully");
        }
    }

    public void shutdown() {
        logger.info("Netty server shutting down...");

        for (Channel channel : channels.values()) {
            if (channel.isOpen()) {
                channel.close();
            }
        }

        if (serverChannel != null && serverChannel.isOpen()) {
            try {
                serverChannel.close().sync();
                logger.info("Server channel closed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while closing server channel", e);
            }
        }

        try {
            if (bossGroup != null) bossGroup.shutdownGracefully().sync();
            if (workerGroup != null) workerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.info("Netty shutdown complete");
    }

    public void addChannel(String channelName, Channel channel) {
        this.channels.put(channelName, channel);
    }

    public void removeChannel(String channelName) {
        this.channels.remove(channelName);
    }

    public void sendPacket(FriendsPacket packet) {
        for (Channel channel : this.channels.values()) {
            if (channel.isActive()) {
                channel.writeAndFlush(packet);
            }
        }
    }

    public void sendPacket(String serverName, FriendsPacket packet) {
        Channel channel = this.channels.getOrDefault(serverName, null);
        if (channel == null) return;

        channel.writeAndFlush(packet);
    }
}
