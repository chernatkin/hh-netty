package ru.hh.anton.spider;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.*;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

public class Spider {

	private final ClientBootstrap clientBootstrap;
	/** Map of urls to their statuses: false - if url is in work, true - if url is done */
	private final ConcurrentHashMap<URL, Boolean> urlsStatuses = new ConcurrentHashMap<URL, Boolean>();

	public Spider(final ClientBootstrap clientBootstrap) {

		this.clientBootstrap = clientBootstrap;

	}

	public void spideURL(final URL url, final int depth) {

		// TODO: do we need to put and write synchronously?
		final Boolean prevValue = this.urlsStatuses.putIfAbsent(url, Boolean.FALSE);
		if (prevValue != null) {
			return;
		}

		// TODO: can we really create url without protocol?
		final String protocol = url.getProtocol() == null ? "http" : url.getProtocol();

		int port = url.getPort();
		if (port == -1) {
			if ("http".equalsIgnoreCase(protocol)) {
				port = 80;
			} else if ("https".equalsIgnoreCase(protocol)) {
				port = 443;
			}
		}

		if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
			System.err.println("Only HTTP(S) is supported.");
			return;
		}

		System.out.println("Spiding " + url + " with depth " + depth);

		final SpiderTask spiderTask = new SpiderTask(url, depth, this);
		final ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(url.getHost(), port));
		channelFuture.addListener(new ChannelFutureWriter(spiderTask));

	}

	private static class ChannelFutureWriter implements ChannelFutureListener {

		private final SpiderTask spiderTask;

		private ChannelFutureWriter(final SpiderTask spiderTask) {
			this.spiderTask = spiderTask;
		}

		@Override
		public void operationComplete(final ChannelFuture channelFuture) throws Exception {

			final Channel channel = channelFuture.getChannel();
			if (!channelFuture.isSuccess()) {
				// TODO: log
				channelFuture.getCause().printStackTrace();
				return;
			}

			channel.setAttachment(spiderTask);

			final URL url = spiderTask.getUrl();
			final String originalPath = url.getPath();
			final String correctedPath = originalPath.length() > 0 ? originalPath : "/";
			final HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, correctedPath);
			request.setHeader(HttpHeaders.Names.HOST, url.getHost());
			request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
			request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);

			channel.write(request);

			// TODO: do we need to close something?

		}
	}

	public void markURLFinished(final URL url) {

		this.urlsStatuses.put(url, Boolean.TRUE);

	}

	public boolean hasPendingURLs() {

		// TODO: slow!
		return urlsStatuses.containsValue(Boolean.FALSE);

	}

	// TODO: remove!
	public String getPendingURLs() {

		final StringBuilder stringBuilder = new StringBuilder();
		for (ConcurrentHashMap.Entry<URL, Boolean> entry: this.urlsStatuses.entrySet()) {
			if (!entry.getValue()) {
				stringBuilder.append(" " + entry.getKey());
			}
		}

		return stringBuilder.toString();

	}

}
