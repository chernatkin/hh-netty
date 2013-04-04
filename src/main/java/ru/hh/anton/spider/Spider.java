package ru.hh.anton.spider;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/** Spides given url with given depth.<br>
 * Need external ClientBootstrap to perform spiding. */
public class Spider {

	private final static Logger logger = Logger.getLogger(Spider.class.getName());

	private final URL startURL;
	private final int depth;
	private final ClientBootstrap clientBootstrap;

	/** Set of known URLs, is used not to get in loop while spiding */
	private final Set<URL> knownURLs = new HashSet<URL>();

	/** Container for channels that perform loading of urls */
	private final Map<Channel, SpiderTask> pendingChannels = new HashMap<Channel, SpiderTask>();

	/** Queue of channels that successfully loaded content of urls */
	private final BlockingQueue<ChannelAndContent> unprocessedChannels = new LinkedBlockingQueue<ChannelAndContent>();

	/** List of urls and their contents */
	private final List<URLAndContent> processedURLs = new LinkedList<URLAndContent>();

	public Spider(final URL startURL, final int depth, ClientBootstrap clientBootstrap) {

		this.startURL = startURL;
		this.depth = depth;
		this.clientBootstrap = clientBootstrap;

	}

	/** Performs spiding.
	 * @return list of urls and their content.*/
	public List<URLAndContent> spide() {

		pendURL(this.startURL, this.depth);

		while (pendingChannels.size() > 0) {

			ChannelAndContent channelAndContent;

			try {
				channelAndContent = this.unprocessedChannels.poll(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				logger.warning(this + ": interrupted");
				closePendingChannels();
				return this.processedURLs;
			}

			// if no new content was added within specified timeout - return
			if(channelAndContent == null) {
				closePendingChannels();
				return this.processedURLs;
			}

			this.processContent(channelAndContent.getChannel(), channelAndContent.getContent());

		}

		return this.processedURLs;

	}

	private void closePendingChannels() {

		for (Map.Entry<Channel, SpiderTask> entry: this.pendingChannels.entrySet()) {

			logger.warning(this + ": closing hanging " + entry.getValue().getUrl());
			entry.getKey().close().awaitUninterruptibly();

		}

	}

	private boolean pendURL(final URL url, final int depth) {

		if (this.knownURLs.contains(url)) {
			return false;
		}

		if (depth < 0) {
			logger.warning(this + ": can not spide " + url + " on " + depth + " depth!");
			return false;
		}

		if (!"http".equalsIgnoreCase(url.getProtocol()) && !"https".equalsIgnoreCase(url.getProtocol())) {
			logger.warning(this + ": failed to pend " + url + ": only HTTP(S) is supported!");
			return false;
		}

		this.knownURLs.add(url);

		final int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
		final ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(url.getHost(), port));

		// TODO: can channelFuture connect between these lines of code?

		final Channel channel = channelFuture.getChannel();
		channel.setAttachment(this);

		final SpiderTask spiderTask = new SpiderTask(url, depth);
		this.pendingChannels.put(channel, spiderTask);

		channelFuture.addListener(new ChannelFutureWriter(url));

		logger.info(this + ": loading " + url + " depth " + depth + " channel " + channel.getId());

		return true;

	}

	/** Adds loaded content to the queue for processing */
	void contentReady(final Channel channel, final String content) {

		this.unprocessedChannels.add(new ChannelAndContent(channel, content));
		logger.info(this + ": channel " + channel.getId() + ": finished loading");

	}

	private void processContent(final Channel channel, final String content) {

		final SpiderTask spiderTask = this.pendingChannels.get(channel);

		logger.info(this + ": processing " + spiderTask.getUrl());

		this.pendingChannels.remove(channel);

		this.processedURLs.add(new URLAndContent(spiderTask.getUrl(), content));

		final int depth = spiderTask.getDepth();
		if (depth > 0) {
			this.pendURLsFromContent(content, depth - 1);
		}

		logger.info(this + ": processed " + spiderTask.getUrl());

	}

	private void pendURLsFromContent(final String content, final int depth) {

		final Document document = Jsoup.parse(content);
		final Elements elements = document.getElementsByTag("a");
		for (Element element : elements) {

			String href = element.attr("href");
			URL url;
			try {
				url = new URL(href);
			} catch (MalformedURLException e) {
				continue;
			}

			this.pendURL(url, depth);

		}

	}

	@Override
	public String toString() {
		return "Spider from " + this.startURL;
	}

}
