package ru.hh.anton.spider;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.*;

import java.net.URL;

/** Listens to ChannelFuture and writes url get request **/
class ChannelFutureWriter implements ChannelFutureListener {

	private final URL url;

	ChannelFutureWriter(URL url) {
		this.url = url;
	}

	@Override
	public void operationComplete(final ChannelFuture channelFuture) throws Exception {

		if (!channelFuture.isSuccess()) {
			// TODO: log
			channelFuture.getCause().printStackTrace();

			// TODO: remove from pending
			return;
		}

		final String correctedPath = this.url.getPath().length() > 0 ? this.url.getPath() : "/";
		final HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, correctedPath);
		request.setHeader(HttpHeaders.Names.HOST, this.url.getHost());
		request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
		request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
		channelFuture.getChannel().write(request);

	}
}
