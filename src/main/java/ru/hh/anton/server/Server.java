package ru.hh.anton.server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

class Server {

	private final int port;

	public Server(final int port) {
		this.port = port;
	}

	public void run() {

		final ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		bootstrap.setPipelineFactory(new ServerPipelineFactory());

		bootstrap.bind(new InetSocketAddress(port));

	}

}
