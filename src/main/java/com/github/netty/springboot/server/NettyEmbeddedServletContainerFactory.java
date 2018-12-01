package com.github.netty.springboot.server;

import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletDefaultHttpServlet;
import com.github.netty.springboot.NettyProperties;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.JspServlet;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * netty容器工厂
 *
 * EmbeddedWebApplicationContext - createEmbeddedServletContainer
 * ImportAwareBeanPostProcessor
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyEmbeddedServletContainerFactory extends AbstractEmbeddedServletContainerFactory implements ResourceLoaderAware {
    protected ResourceLoader resourceLoader;
    protected NettyProperties properties;

    public NettyEmbeddedServletContainerFactory() {
        this(new NettyProperties());
    }

    public NettyEmbeddedServletContainerFactory(NettyProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取servlet容器
     * @param initializers 初始化
     * @return
     */
    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... initializers) {
        try {
            //临时目录
            File documentRoot = getValidDocumentRoot();
            File docBase = documentRoot != null? documentRoot : createTempDir("nettyx-docbase");

            //服务器端口
            InetSocketAddress serverAddress = new InetSocketAddress(getAddress() == null? InetAddress.getLoopbackAddress():getAddress(),getPort());
            ClassLoader classLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();
            NettyEmbeddedServletContainer server = new NettyEmbeddedServletContainer(serverAddress, properties);

            ServletContext servletContext = new ServletContext(serverAddress,classLoader,docBase.getAbsolutePath());

            //配置tcp服务器
            configurableTcpServer(server,servletContext);

            //默认 servlet
            if (isRegisterDefaultServlet()) {
                ServletDefaultHttpServlet defaultServlet = new ServletDefaultHttpServlet();
                servletContext.addServlet("default",defaultServlet).addMapping("/");
            }

            //jsp
            JspServlet jsp = getJspServlet();
            if(shouldRegisterJspServlet()){
                //
            }

            //初始化
            ServletContextInitializer[] servletContextInitializers = mergeInitializers(initializers);
            for (ServletContextInitializer initializer : servletContextInitializers) {
                initializer.onStartup(servletContext);
            }

            return server;
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
    protected void configurableTcpServer(NettyEmbeddedServletContainer tcpServer,ServletContext servletContext) throws Exception {
        //添加httpServlet协议注册器
        tcpServer.addProtocolsRegister(new HttpServletProtocolsRegisterSpringAdapter(properties,servletContext,this));
        //添加内部rpc协议注册器
        tcpServer.addProtocolsRegister(new HRpcProtocolsRegisterSpringAdapter(properties.getApplication()));
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
