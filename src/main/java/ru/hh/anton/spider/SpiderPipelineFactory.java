package ru.hh.anton.spider;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

class SpiderPipelineFactory implements ChannelPipelineFactory {

	private final Timer timer;
	private final ChannelHandler idleStateHandler;

	public SpiderPipelineFactory() {
		this.timer = new HashedWheelTimer();
		this.idleStateHandler = new IdleStateHandler(this.timer, 1, 1, 1);
	}

	public ChannelPipeline getPipeline() throws Exception {

		ChannelPipeline pipeline = Channels.pipeline();

		// TODO: ssl
		/*if (ssl) {
			SSLEngine engine =
					SecureChatSslContextFactory.getClientContext().createSSLEngine();
			engine.setUseClientMode(true);

			pipeline.addLast("ssl", new SslHandler(engine));
		}*/

		pipeline.addLast("codec", new HttpClientCodec());

		pipeline.addLast("inflater", new HttpContentDecompressor());

		pipeline.addLast("idle", this.idleStateHandler);

		pipeline.addLast("handler", new SpiderHandler());

		return pipeline;

	}

}
