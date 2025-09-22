package de.t0bx.sentiencefriends.lobby.netty;

import de.t0bx.sentiencefriends.api.netty.codec.PacketDecoder;
import de.t0bx.sentiencefriends.api.netty.codec.PacketEncoder;
import de.t0bx.sentiencefriends.api.netty.codec.VarIntFrameDecoder;
import de.t0bx.sentiencefriends.api.network.packets.ChannelIdentifyPacket;
import de.t0bx.sentiencefriends.lobby.LobbyPlugin;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;

@Getter
public class NettyManager {
    private final String host;
    private final int port;
    private volatile Channel channel;
    private final EventLoopGroup group = new NioEventLoopGroup();
    private final Thread nettyThread;
    private volatile boolean running = true;
    private final int reconnectDelayMillis = 5000;

    public NettyManager(String host, int port) {
        this.host = host;
        this.port = port;

        this.nettyThread = new Thread(this::runNettyLoop, "Netty-Reconnect-Thread");
        this.nettyThread.setDaemon(true);
        this.nettyThread.start();
    }

    private void runNettyLoop() {
        while (running) {
            try {
                connect();
                channel.closeFuture().sync();
                if (running) {
                    LobbyPlugin.getInstance().getLogger().severe("Disconnected from master. Reconnecting in 5 seconds...");
                    Thread.sleep(reconnectDelayMillis);
                }
            } catch (Exception e) {
                LobbyPlugin.getInstance().getLogger().severe("Connection failed. Retrying in 5 seconds...");
                try {
                    Thread.sleep(reconnectDelayMillis);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
    }

    private void connect() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("varint-frame", new VarIntFrameDecoder());
                        pipeline.addLast("packet-decoder", new PacketDecoder());
                        pipeline.addLast("packet-encoder", new PacketEncoder());
                        pipeline.addLast("packet-handler", new PacketHandler());
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        this.channel = future.channel();

        LobbyPlugin.getInstance().getLogger().info("Connected to master server.");
        this.channel.writeAndFlush(new ChannelIdentifyPacket(LobbyPlugin.getInstance().getChannelName()));
    }

    public void close() {
        running = false;
        try {
            if (channel != null && channel.isOpen()) {
                channel.close().sync();
            }
        } catch (InterruptedException ignored) {}

        group.shutdownGracefully();
        nettyThread.interrupt();
    }

}
