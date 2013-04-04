package ru.hh.anton.server;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

class ServerHandler extends SimpleChannelUpstreamHandler {

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

		// TODO: spide

		final StringBuilder responseBuilder = new StringBuilder();
		responseBuilder.append("url: " + url);
		responseBuilder.append("\r\ndepth: " + depth);

		writeResponse(messageEvent.getChannel(), responseBuilder.toString());

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

	private void writeResponse(final Channel channel, final String reponseString) {

		// Build the response object.
		final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		response.setContent(ChannelBuffers.copiedBuffer(reponseString, CharsetUtil.UTF_8));
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

		// Write the response & close connection
		final ChannelFuture future = channel.write(response);
		future.addListener(ChannelFutureListener.CLOSE);

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {

		// TODO: log
		e.getCause().printStackTrace();
		e.getChannel().close();
	}

}
