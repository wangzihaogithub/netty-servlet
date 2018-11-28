package com.github.netty.springboot.server;

import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletDefaultHttpServlet;
import com.github.netty.springboot.NettyProperties;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.Jsp;
import org.springframework.http.server.reactive.HttpHandler;

import java.net.InetSocketAddress;

/**
 * netty容器工厂 tcp层面的服务器工厂
 *
 * EmbeddedWebApplicationContext - createEmbeddedServletContainer
 * ImportAwareBeanPostProcessor
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyTcpServerFactory
        extends AbstractServletWebServerFactory
        implements ConfigurableReactiveWebServerFactory,ConfigurableServletWebServerFactory {
    protected NettyProperties properties;

    public NettyTcpServerFactory() {
        this(new NettyProperties());
    }

    public NettyTcpServerFactory(NettyProperties properties) {
        this.properties = properties;
    }

    // TODO: 2018/11/12/012  getWebServer(HttpHandler httpHandler)
    @Override
    public WebServer getWebServer(HttpHandler httpHandler) {
        return null;
    }

    /**
     * 获取servlet容器
     * @param initializers 初始化
     * @return
     */
    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        try {
            InetSocketAddress serverAddress = new InetSocketAddress(getAddress(),getPort());
            NettyTcpServer tcpServer = new NettyTcpServer(serverAddress, properties);

            ServletContext servletContext = new ServletContext(serverAddress);
            //配置tcp服务器
            configurableTcpServer(tcpServer,servletContext);

            //默认 servlet
            if (isRegisterDefaultServlet()) {
                ServletDefaultHttpServlet defaultServlet = new ServletDefaultHttpServlet();
                servletContext.addServlet("default",defaultServlet).addMapping("/");
            }

            //jsp
            Jsp jsp = getJsp();
            if(shouldRegisterJspServlet()){
                //
            }

            //初始化
            ServletContextInitializer[] servletContextInitializers = mergeInitializers(initializers);
            for (ServletContextInitializer initializer : servletContextInitializers) {
                initializer.onStartup(servletContext);
            }

            return tcpServer;
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(),e);
        }
    }

    /**
     * 配置tpc服务器
     * @param tcpServer
     * @param servletContext
     * @throws Exception
     */
    protected void configurableTcpServer(NettyTcpServer tcpServer,ServletContext servletContext) throws Exception {
        //添加httpServlet协议注册器
        tcpServer.addProtocolsRegister(new HttpServletProtocolsRegisterSpringAdapter(properties,servletContext,this));
        //添加内部rpc协议注册器
        tcpServer.addProtocolsRegister(new HRpcProtocolsRegisterSpringAdapter(properties.getApplication()));
    }

}
