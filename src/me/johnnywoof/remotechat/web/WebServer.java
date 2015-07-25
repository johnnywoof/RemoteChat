package me.johnnywoof.remotechat.web;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import me.johnnywoof.remotechat.Settings;

import java.net.InetSocketAddress;

public class WebServer implements Runnable {

	public final WebServerHandler webServerHandler = new WebServerHandler();

	@Override
	public void run() {

		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
							//.handler(new LoggingHandler(LogLevel.INFO))
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ChannelPipeline p = ch.pipeline();
							p.addLast("encoder", new HttpResponseEncoder());
							p.addLast("decoder", new HttpRequestDecoder());
							p.addLast("aggregator", new HttpObjectAggregator(65536));
							p.addLast("handler", WebServer.this.webServerHandler);
						}
					});

			InetSocketAddress bindAddress = new InetSocketAddress(Settings.bindIP, Settings.bindPort);

			Channel ch = b.bind(bindAddress).sync().channel();

			System.out.println("Web server started on " + bindAddress.toString());

			ch.closeFuture().sync();
		} catch (InterruptedException e) {
			/*No printing needed*/
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

	}

}
