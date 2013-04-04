package ru.hh.anton.spider;

import java.net.URL;

/** Helper class that holds together URL and int depth */
class SpiderTask {

	private final URL url;
	private final int depth;

	SpiderTask(final URL url, final int depth) {

		this.url = url;
		this.depth = depth;

	}

	URL getUrl() {
		return url;
	}

	int getDepth() {
		return depth;
	}

}
