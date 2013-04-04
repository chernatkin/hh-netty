package ru.hh.anton.server;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import ru.hh.anton.spider.SpiderPipelineFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

class Server {

	private final int port;

	public Server(final int port) {
		this.port = port;
	}

	public void run() {

		// prepare bootstrap for spider
		final ClientBootstrap clientBootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		clientBootstrap.setPipelineFactory(new SpiderPipelineFactory());

		// prepare bootstrap for HTTP server
		final ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		bootstrap.setPipelineFactory(new ServerPipelineFactory(clientBootstrap));

		bootstrap.bind(new InetSocketAddress(port));

		// I do not stop server properly. But neither do netty examples: http://netty.io/wiki/index.html

	}

}
