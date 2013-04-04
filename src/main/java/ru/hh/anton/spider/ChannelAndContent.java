package ru.hh.anton.spider;

import org.jboss.netty.channel.Channel;

/** Helper class that holds Channel chanel and String content together */
class ChannelAndContent {

	private final Channel channel;
	private final String content;

	ChannelAndContent(final Channel channel, final String content) {
		this.channel = channel;
		this.content = content;
	}

	Channel getChannel() {
		return channel;
	}

	String getContent() {
		return content;
	}

}
