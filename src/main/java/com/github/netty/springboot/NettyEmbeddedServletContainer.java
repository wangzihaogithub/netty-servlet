package com.github.netty.springboot;

import com.github.netty.servlet.ServletContext;
import com.github.netty.core.AbstractNettyServer;
import com.github.netty.servlet.ServletRegistration;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;

import javax.net.ssl.SSLEngine;
import javax.servlet.ServletException;
import java.util.Map;

/**
 * Created by acer01 on 2018/7/14/014.
 */
public class NettyEmbeddedServletContainer extends AbstractNettyServer implements EmbeddedServletContainer {

    private ServletContext servletContext;
    private boolean isSsl;
    private EventExecutorGroup eventExecutorGroup;

    public NettyEmbeddedServletContainer(ServletContext servletContext,boolean isSsl) {
        super(servletContext.getServerSocketAddress());
        this.servletContext = servletContext;
        this.isSsl = isSsl;
        this.eventExecutorGroup = new DefaultEventExecutorGroup(50);
    }

    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                if (isSsl) {
                    SSLEngine engine = newSSLEngine();
                    engine.setNeedClientAuth(true); //ssl双向认证
                    engine.setUseClientMode(false);
                    engine.setWantClientAuth(true);
                    engine.setEnabledProtocols(new String[]{"SSLv3"});
                    pipeline.addLast("ssl", new SslHandler(engine));
                }

//                pipeline.addLast("decoder", new HttpRequestDecoder())
//                        .addLast("encoder", new HttpResponseEncoder())
//                        .addLast("aggregator", new HttpObjectAggregator(maxContentLength))
//                        .addLast("contentCompressor", new HttpContentCompressor())
////                        .addLast("myHttpHandler", new HttpDemoServer(isSsl));
//                .addLast(eventExecutorGroup,new NettyServletDispatcherHandler(servletContext));

                pipeline.addLast("HttpCodec", new HttpServerCodec(4096, 8192, 8192, false)); //HTTP编码解码Handler
                pipeline.addLast("ServletCodec", new NettyServletCodecHandler(servletContext)); //处理请求，读入数据，生成Request和Response对象
                pipeline.addLast(eventExecutorGroup, "Dispatcher", new NettyServletDispatcherHandler(servletContext)); //获取请求分发器，让对应的Servlet处理请求，同时处理404情况
            }
        };
    }

    @Override
    public void start() throws EmbeddedServletContainerException {
        initServlet();
        servletContext.setInitialized(true);

        String serverInfo = servletContext.getServerInfo();

        Thread serverThread = new Thread(this);
        serverThread.setName(serverInfo);
        serverThread.setUncaughtExceptionHandler((thread,throwable)->{
            //
        });
        serverThread.start();

        System.out.println("启动成功 "+serverInfo+"["+getPort()+"]...");
    }

    @Override
    public void stop() throws EmbeddedServletContainerException {
        destroyServlet();
        super.stop();
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    private void initServlet(){
        Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
        for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
            ServletRegistration registration = entry.getValue();
            try {
                registration.getServlet().init(registration.getServletConfig());
            } catch (ServletException e) {
                throw new EmbeddedServletContainerException(e.getMessage(),e);
            }
        }
    }

    private void destroyServlet(){
        Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
        for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
            ServletRegistration registration = entry.getValue();
            registration.getServlet().destroy();
        }
    }

}
