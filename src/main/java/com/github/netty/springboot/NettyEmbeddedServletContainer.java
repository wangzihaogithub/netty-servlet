package com.github.netty.springboot;

import com.github.netty.core.AbstractNettyServer;
import com.github.netty.core.util.ApplicationX;
import com.github.netty.core.util.NettyThreadX;
import com.github.netty.servlet.handler.NettyServletHandler;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletFilterRegistration;
import com.github.netty.servlet.ServletRegistration;
import com.github.netty.servlet.support.ServletEventListenerManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.annotation.Resource;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import java.io.File;
import java.util.Arrays;
import java.util.Map;

/**
 * netty容器
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyEmbeddedServletContainer extends AbstractNettyServer implements EmbeddedServletContainer {

    /**
     * servlet上下文
     */
    private final ServletContext servletContext;
    /**
     * 容器配置信息
     */
    private final NettyProperties config;
    /**
     * servlet线程
     */
    private final Thread servletServerThread;
    /**
     * https配置信息
     */
    private SslContext sslContext;

    public static final String HANDLER_SSL = "SSL";
    public static final String HANDLER_CHUNKED_WRITE = "ChunkedWrite";
    public static final String HANDLER_AGGREGATOR = "Aggregator";
    public static final String HANDLER_SERVLET = "Servlet";
    public static final String HANDLER_HTTP_CODEC = "HttpCodec";

    public NettyEmbeddedServletContainer(ServletContext servletContext,Ssl ssl,NettyProperties config) throws SSLException {
        super("Netty",servletContext.getServletServerAddress());

        this.servletContext = servletContext;
        this.config = config;
        this.servletServerThread = new NettyThreadX(this,getName());
        super.setIoRatio(config.getServerIoRatio());
        super.setWorkerCount(config.getServerWorkerCount());
        // TODO: 10月16日/0016   ssl没测试能不能用
        if(ssl != null && ssl.isEnabled()){
            this.sslContext = newSslContext(ssl);
        }
    }

    @Override
    public void start() throws EmbeddedServletContainerException {
        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasServletContextListener()){
            listenerManager.onServletContextInitialized(new ServletContextEvent(servletContext));
        }

        servletServerThread.start();

        initFilter(servletContext);
        initServlet(servletContext);

        //注入到spring对象里
        injectToSpringApplication();
    }

    /**
     * 注入到spring对象里
     */
    protected void injectToSpringApplication(){
        ApplicationX application = config.getApplication();
        application.addInjectAnnotation(Autowired.class, Resource.class);
        application.addInstance(servletContext.getSessionService());
        application.addInstance(servletContext);
        application.addInstance(config);
        WebApplicationContext springApplication = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        String[] beans = springApplication.getBeanDefinitionNames();
        for (String beanName : beans) {
            Object bean = springApplication.getBean(beanName);
            application.addInstance(beanName,bean,false);
        }

        application.scanner("com.github.netty").inject();
    }

    /**
     * 初始化 IO执行器
     * @return
     */
    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<SocketChannel>() {
            private ChannelHandler servletHandler = new NettyServletHandler(servletContext,config);

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();

                if (sslContext != null) {
                    SSLEngine engine = sslContext.newEngine(ch.alloc());
                    engine.setUseClientMode(false);
                    pipeline.addLast(HANDLER_SSL, new SslHandler(engine,true));
                }

                //HTTP编码解码
                pipeline.addLast(HANDLER_HTTP_CODEC, new HttpServerCodec(4096, 8192, 8192, false));

                //HTTP请求body聚合，设置最大消息值为 5M
                pipeline.addLast(HANDLER_AGGREGATOR, new HttpObjectAggregator(5 * 1024 * 1024));

                //内容压缩
//                    pipeline.addLast("ContentCompressor", new HttpContentCompressor());
//                pipeline.addLast("ContentDecompressor", new HttpContentDecompressor());

                //分段写入, 用于流传输, 防止响应数据过大
//                pipeline.addLast("ChunkedWrite",new ChunkedWriteHandler());

                //业务调度器, 让对应的Servlet处理请求
                pipeline.addLast(HANDLER_SERVLET, servletHandler);
            }
        };
    }

    @Override
    public void stop() throws EmbeddedServletContainerException {
        ServletEventListenerManager listenerManager = servletContext.getServletEventListenerManager();
        if(listenerManager.hasServletContextListener()){
            listenerManager.onServletContextDestroyed(new ServletContextEvent(servletContext));
        }

        destroyFilter();
        destroyServlet();
        super.stop();
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    /**
     * 初始化过滤器
     * @param servletContext
     */
    protected void initFilter(ServletContext servletContext){
        Map<String, ServletFilterRegistration> servletFilterRegistrationMap = servletContext.getFilterRegistrations();
        for(Map.Entry<String,ServletFilterRegistration> entry : servletFilterRegistrationMap.entrySet()){
            ServletFilterRegistration registration = entry.getValue();
            try {
                registration.getFilter().init(registration.getFilterConfig());
                registration.setInitParameter("_init","true");
            } catch (ServletException e) {
                throw new EmbeddedServletContainerException(e.getMessage(),e);
            }
        }
    }

    /**
     * 初始化servlet
     * @param servletContext
     */
    protected void initServlet(ServletContext servletContext){
        Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
        for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
            ServletRegistration registration = entry.getValue();
            try {
                registration.getServlet().init(registration.getServletConfig());
                registration.setInitParameter("_init","true");
            } catch (ServletException e) {
                throw new EmbeddedServletContainerException(e.getMessage(),e);
            }
        }
    }

    /**
     * 销毁过滤器
     */
    protected void destroyFilter(){
        Map<String, ServletFilterRegistration> servletRegistrationMap = servletContext.getFilterRegistrations();
        for(Map.Entry<String,ServletFilterRegistration> entry : servletRegistrationMap.entrySet()){
            ServletFilterRegistration registration = entry.getValue();
            Filter filter = registration.getFilter();
            if(filter == null) {
                continue;
            }
            String initFlag = registration.getInitParameter("_init");
            if(initFlag != null && "true".equals(initFlag)){
                filter.destroy();
            }
        }
    }

    /**
     * 销毁servlet
     */
    protected void destroyServlet(){
        Map<String, ServletRegistration> servletRegistrationMap = servletContext.getServletRegistrations();
        for(Map.Entry<String,ServletRegistration> entry : servletRegistrationMap.entrySet()){
            ServletRegistration registration = entry.getValue();
            Servlet servlet = registration.getServlet();
            if(servlet == null) {
                continue;
            }
            String initFlag = registration.getInitParameter("_init");
            if(initFlag != null && "true".equals(initFlag)){
                servlet.destroy();
            }
        }
    }

    /**
     * 配置 https
     * @param ssl
     * @return
     * @throws SSLException
     */
    protected SslContext newSslContext(Ssl ssl) throws SSLException {
        File certChainFile = new File(ssl.getTrustStore());
        File keyFile = new File(ssl.getKeyStore());
        String keyPassword = ssl.getKeyPassword();

        SslContext sslContext = SslContextBuilder.forServer(certChainFile,keyFile,keyPassword)
                .ciphers(Arrays.asList(ssl.getCiphers()))
                .protocols(ssl.getProtocol())
                .build();
        return sslContext;
    }

}
