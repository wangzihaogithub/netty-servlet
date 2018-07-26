package com.github.netty.springboot;

import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletDefaultHttpServlet;
import com.github.netty.servlet.ServletSessionCookieConfig;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * EmbeddedWebApplicationContext - createEmbeddedServletContainer
 * ImportAwareBeanPostProcessor
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyEmbeddedServletContainerFactory extends AbstractEmbeddedServletContainerFactory implements EmbeddedServletContainerFactory , ResourceLoaderAware {

    protected ResourceLoader resourceLoader;

    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... initializers) {
        try {
            ServletContext servletContext = newServletContext();
            NettyEmbeddedServletContainer container = newNettyEmbeddedServletContainer(servletContext);

            if (isRegisterDefaultServlet()) {
                registerDefaultServlet(servletContext);
            }

            for (ServletContextInitializer initializer : initializers) {
                initializer.onStartup(servletContext);
            }
            return container;
        }catch (Exception e){
            throw new IllegalStateException(e);
        }
    }

    /**
     * 注册默认servlet
     * @param servletContext servlet上下文
     */
    protected void registerDefaultServlet(ServletContext servletContext){
        ServletDefaultHttpServlet defaultServlet = new ServletDefaultHttpServlet();
        servletContext.addServlet("default",defaultServlet);
    }

    /**
     * 新建netty容器
     * @param servletContext servlet上下文
     * @return netty容器
     * @throws SSLException ssl异常
     */
    protected NettyEmbeddedServletContainer newNettyEmbeddedServletContainer(ServletContext servletContext) throws SSLException {
        Ssl ssl = getSsl();
        NettyEmbeddedServletContainer container = new NettyEmbeddedServletContainer(servletContext,ssl,50);
        return container;
    }

    /**
     * 新建servlet上下文
     * @return
     */
    private ServletContext newServletContext(){
        ClassLoader parentClassLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();
        ServletSessionCookieConfig sessionCookieConfig = loadSessionCookieConfig();

        ServletContext servletContext = new ServletContext(
                new InetSocketAddress(getAddress(),getPort()),
                new URLClassLoader(new URL[]{}, parentClassLoader),
                getContextPath(),
                getServerHeader(),
                sessionCookieConfig);
        return servletContext;
    }

    /**
     * 加载session的cookie配置
     * @return cookie配置
     */
    protected ServletSessionCookieConfig loadSessionCookieConfig(){
        ServletSessionCookieConfig sessionCookieConfig = new ServletSessionCookieConfig();
        sessionCookieConfig.setMaxAge(-1);

        sessionCookieConfig.setSessionTimeout(getSessionTimeout());
        return sessionCookieConfig;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
