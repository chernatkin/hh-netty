package ru.hh.anton.spider;

import java.net.URL;

class SpiderTask {

	private final URL url;
	private final int depth;
	private final Spider spider;

	SpiderTask(final URL url, final int depth, final Spider spider) {
		this.url = url;
		this.depth = depth;
		this.spider = spider;
	}

	public URL getUrl() {
		return url;
	}

	public int getDepth() {
		return depth;
	}

	public Spider getSpider() {
		return spider;
	}

	public void markCurrentURLFinished() {
		this.getSpider().markURLFinished(this.url);
		System.out.println("Finished " + this.url);
	}

}
