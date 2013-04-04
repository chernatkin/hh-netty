package ru.hh.anton.spider;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.util.CharsetUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

class SpiderHandler extends IdleStateAwareChannelHandler {

	private HttpResponse response;
	private final StringBuilder contentBuilder = new StringBuilder();

	@Override
	public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) {
		/*if (e.getState() == IdleState.READER_IDLE) {
			e.getChannel().close();
		} else if (e.getState() == IdleState.WRITER_IDLE) {
			e.getChannel().write(new PingMessage());

		} */

		System.out.println("Idle " + getURL(ctx));

	}

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

		super.messageReceived(channelHandlerContext, messageEvent);

	}

	@Override
	public void exceptionCaught(final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent) {

		// TODO: log
		System.err.println("Got exception when spiding " + exceptionEvent.getChannel().getAttachment());
		exceptionEvent.getCause().printStackTrace();
		exceptionEvent.getChannel().close();

		((SpiderTask) channelHandlerContext.getChannel().getAttachment()).markCurrentURLFinished();

	}

	private void contentReady(final ChannelHandlerContext channelHandlerContext) {

		final SpiderTask spiderTask = (SpiderTask) channelHandlerContext.getChannel().getAttachment();

		this.saveContentToFile(spiderTask.getUrl());

		final int depth = spiderTask.getDepth();
		if (depth > 0) {
			this.createSpiders(depth - 1, spiderTask.getSpider());
		}

		spiderTask.markCurrentURLFinished();
		if (!spiderTask.getSpider().hasPendingURLs()) {
			System.out.println("Finished!");
		} else {
			System.out.println("Still pending: " + spiderTask.getSpider().getPendingURLs());
		}

	}

	private void saveContentToFile(final URL url) {

		final String urlPath = url.getPath().length() > 0 ? url.getPath() : "/";

		String fileName;
		try {
			fileName =  URLEncoder.encode(urlPath, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO: log
			System.err.println("Failed to save url '" + url + "': " + e.getLocalizedMessage());
			return;
		}

		// TODO: make folder configurable
		final String path = "content/" + url.getHost() + "/";
		new File(path).mkdirs();

		PrintWriter printWriter;
		try {
			printWriter = new PrintWriter(path + fileName);
		} catch (FileNotFoundException e) {
			System.err.println("Failed to save url '" + url + "': " + e.getLocalizedMessage());
			return;
		}

		printWriter.println(this.contentBuilder.toString());
		printWriter.close();

	}

	private void createSpiders(final int depth, final Spider spider) {

		final Document document = Jsoup.parse(this.contentBuilder.toString());
		final Elements elements = document.getElementsByTag("a");
		for (Element element : elements) {

			String href = element.attr("href");
			URL url;
			try {
				url = new URL(href);
			} catch (MalformedURLException e) {
				continue;
			}

			spider.spideURL(url, depth);

		}

	}

	private URL getURL(final ChannelHandlerContext channelHandlerContext) {
		return ((SpiderTask) channelHandlerContext.getChannel().getAttachment()).getUrl();
	}

}
