package ru.hh.anton.spider;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;

import java.util.logging.Logger;

class SpiderHandler extends SimpleChannelUpstreamHandler {

	private final static Logger logger = Logger.getLogger(SpiderHandler.class.getName());

	private HttpResponse response;
	private final StringBuilder contentBuilder = new StringBuilder();

	@Override
	public void messageReceived(final ChannelHandlerContext channelHandlerContext, final MessageEvent messageEvent) throws Exception {

		// TODO: maybe HttpChunkAggregator can help?

		if (this.response == null) {

			this.response = (HttpResponse) messageEvent.getMessage();

			if (!response.isChunked()) {

				final ChannelBuffer content = response.getContent();
				if (content.readable()) {
					this.contentBuilder.append(content.toString(CharsetUtil.UTF_8));
					contentReady(channelHandlerContext);
				}
			}

		} else {

			final HttpChunk chunk = (HttpChunk) messageEvent.getMessage();
			if (chunk.isLast()) {
				contentReady(channelHandlerContext);
			} else {
				this.contentBuilder.append(chunk.getContent().toString(CharsetUtil.UTF_8));
			}

		}

	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent) {

		logger.warning("Channel " + channelHandlerContext.getChannel().getId() + ": caught " + exceptionEvent.getCause().getLocalizedMessage());
		channelHandlerContext.getChannel().close();
		// TODO: remove from pendingChannels

	}

	private void contentReady(final ChannelHandlerContext channelHandlerContext) {

		final Channel channel = channelHandlerContext.getChannel();
		channel.close();
		((Spider) channel.getAttachment()).contentReady(channel, contentBuilder.toString());

	}

}
