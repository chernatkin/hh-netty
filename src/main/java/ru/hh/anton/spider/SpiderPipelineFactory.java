package ru.hh.anton.spider;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpContentDecompressor;

public class SpiderPipelineFactory implements ChannelPipelineFactory {

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

		pipeline.addLast("handler", new SpiderHandler());

		return pipeline;

	}

}
