package ru.hh.anton.spider;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;

public class SpiderStarter {

	public static void main(String[] args) throws MalformedURLException {

		// check args
		if (args.length != 2) {
			System.err.println(
					"Usage: " + Spider.class.getSimpleName() + " <URL> <depth>");
			return;
		}

		final URL url = new URL(args[0]);
		final int depth = Integer.parseInt(args[1]);

		// prepare bootstrap for spider
		final ClientBootstrap clientBootstrap = new ClientBootstrap(
				new NioClientSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));
		clientBootstrap.setPipelineFactory(new SpiderPipelineFactory());
		clientBootstrap.setOption("connectTimeoutMillis", 1000);

		// create and run spider
		final Spider spider = new Spider(clientBootstrap);
		spider.spideURL(url, depth);

	}

}
