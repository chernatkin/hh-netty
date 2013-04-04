package ru.hh.anton.spider;

import java.net.URL;

public class URLAndContent {

	private final URL url;
	private final String content;

	URLAndContent(final URL url, final String content) {
		this.url = url;
		this.content = content;
	}

	public URL getUrl() {
		return url;
	}

	public String getContent() {
		return content;
	}

}
