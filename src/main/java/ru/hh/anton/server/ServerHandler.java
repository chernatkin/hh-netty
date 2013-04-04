package ru.hh.anton.server;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import ru.hh.anton.spider.Spider;
import ru.hh.anton.spider.URLAndContent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
class ServerHandler extends SimpleChannelUpstreamHandler {

	private final ClientBootstrap spiderBootstrap;

	ServerHandler(ClientBootstrap spiderBootstrap) {

		this.spiderBootstrap = spiderBootstrap;

	}

	@Override
	public void messageReceived(final ChannelHandlerContext channelHandlerContext, final MessageEvent messageEvent) {

		final HttpRequest request = (HttpRequest) messageEvent.getMessage();

		final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
		final Map<String, List<String>> params = queryStringDecoder.getParameters();

		// extract params
		final StringBuilder errorsBuilder = new StringBuilder();

		final URL url = getUrlParam(params, errorsBuilder);
		final int depth = getDepthParam(params, errorsBuilder);

		// send errors if any and return
		if (errorsBuilder.length() > 0) {
			writeResponse(messageEvent.getChannel(), errorsBuilder.toString().trim());
			return;
		}

		this.spideAndRespond(url, depth, messageEvent.getChannel());

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {

		// TODO: log
		e.getCause().printStackTrace();
		e.getChannel().close();

	}

	/** Extracts url from map of parameters.<br>
	 * In case of errors appends them to errorsBuilder and return null. */
	private URL getUrlParam(final Map<String, List<String>> params, final StringBuilder errorsBuilder) {

		final List<String> urlStrings = params.get("url");
		if (urlStrings == null) {

			errorsBuilder.append("\r\nMissing url parameter!");

		} else if (urlStrings.size() != 1) {

			// TODO: support multiple urls
			errorsBuilder.append("\r\nMultiple urls is not supported yet!");

		} else {

			final String urlString = urlStrings.get(0);
			try {
				return new URL(urlString);
			} catch (final MalformedURLException e) {
				errorsBuilder.append("\r\nMalformed url '" + urlString + "':" + e.getLocalizedMessage());
			}

		}

		return null;

	}

	/** Extracts depth from map of parameters.<br>
	 * In case of errors appends them to errorsBuilder and returns -1. */
	private int getDepthParam(final Map<String, List<String>> params, final StringBuilder errorsBuilder) {

		final List<String> depthStrings = params.get("depth");
		if (depthStrings == null) {

			errorsBuilder.append("\r\nMissing depth parameter!");

		} else if (depthStrings.size() != 1) {

			errorsBuilder.append("\r\nOnly one depth parameter is supported!");

		} else {

			final String depthString = depthStrings.get(0);
			int depth;
			try {
				depth = Integer.parseInt(depthString);
			} catch (final NumberFormatException e) {
				errorsBuilder.append("\r\nFailed to convert depth '" + depthString + "' to number!");
				return -1;
			}

			if (depth < 0) {
				errorsBuilder.append("\r\nDepth must be greater or equal to 0!");
			} else {
				return depth;
			}

		}

		return -1;

	}

	/** Spides given url on given depth async and writes respond to the given channel */
	private void spideAndRespond(final URL url, final int depth, final Channel channel) {

		// TODO: thread pool
		new Thread(new Runnable() {

			@Override
			public void run() {

				final List<URLAndContent> urlsAndContent = new Spider(url, depth, spiderBootstrap).spide();

				for (URLAndContent urlAndContent: urlsAndContent) {
					saveContentToFile(urlAndContent.getUrl(), urlAndContent.getContent());
				}

				final StringBuilder responseBuilder = new StringBuilder();
				responseBuilder.append("You requested to spide:");
				responseBuilder.append("\r\nurl: " + url);
				responseBuilder.append("\r\ndepth: " + depth);
				responseBuilder.append("\r\n");
				responseBuilder.append("\r\nWe spided:");
				responseBuilder.append("\r\n" + urlsAndContent.size() + " urls");

				writeResponse(channel, responseBuilder.toString());

			}
		}).start();

	}

	private static void saveContentToFile(final URL url, final String content) {

		final String urlPath = url.getPath().length() > 0 ? url.getPath() : "/";

		String fileName;
		try {
			fileName = URLEncoder.encode(urlPath, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			// TODO: log
			System.err.println("Failed to save url '" + url + "': " + e.getLocalizedMessage());
			return;
		}

		// TODO: make folder configurable
		final String path = "content/" + url.getHost() + "/";
		//noinspection ResultOfMethodCallIgnored
		new File(path).mkdirs();

		PrintWriter printWriter;
		try {
			printWriter = new PrintWriter(path + fileName);
		} catch (FileNotFoundException e) {
			System.err.println("Failed to save url '" + url + "': " + e.getLocalizedMessage());
			return;
		}

		printWriter.println(content);
		printWriter.close();

	}

	/** Writes responseString to the give channel as HttpResponse */
	private void writeResponse(final Channel channel, final String responseString) {

		// Build the response object.
		final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.setContent(ChannelBuffers.copiedBuffer(responseString, CharsetUtil.UTF_8));
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

		// Write to channel and close
		final ChannelFuture future = channel.write(response);
		future.addListener(ChannelFutureListener.CLOSE);

	}

}
