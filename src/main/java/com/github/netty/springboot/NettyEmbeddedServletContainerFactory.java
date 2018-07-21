package com.github.netty.springboot;

import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletDefaultHttpServlet;
import com.github.netty.servlet.ServletSessionCookieConfig;
import com.github.netty.util.ProxyUtil;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

import javax.servlet.ServletException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Executors;

/**
 *
 * EmbeddedWebApplicationContext -> createEmbeddedServletContainer
 * ImportAwareBeanPostProcessor
 *
 * Created by acer01 on 2018/7/14/014.
 */
public class NettyEmbeddedServletContainerFactory extends AbstractEmbeddedServletContainerFactory implements EmbeddedServletContainerFactory , ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... initializers) {
        ProxyUtil.setEnableProxy(false);

        ServletContext servletContext = newServletContext();
        NettyEmbeddedServletContainer container = newNettyEmbeddedServletContainer(servletContext);

        if(isRegisterDefaultServlet()){
            registerDefaultServlet(servletContext);
        }

        for(ServletContextInitializer initializer : initializers){
            try {
                initializer.onStartup(servletContext);
            } catch (ServletException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return container;
    }

    /**
     * 注册默认servlet
     * @param servletContext
     */
    private void registerDefaultServlet(ServletContext servletContext){
        ServletDefaultHttpServlet defaultServlet = new ServletDefaultHttpServlet();
        servletContext.addServlet("default",defaultServlet);
    }

    /**
     * 新建netty容器
     * @param servletContext
     * @return
     */
    private NettyEmbeddedServletContainer newNettyEmbeddedServletContainer(ServletContext servletContext){
        Ssl ssl = getSsl();
        boolean isSsl = ssl != null && ssl.isEnabled();
        NettyEmbeddedServletContainer container = new NettyEmbeddedServletContainer(servletContext,isSsl);
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
                Executors.newFixedThreadPool(8),
                new URLClassLoader(new URL[]{}, parentClassLoader),
                getContextPath(),
                getServerHeader(),
                sessionCookieConfig);
        return servletContext;
    }

    /**
     * 加载session的cookie配置
     * @return
     */
    private ServletSessionCookieConfig loadSessionCookieConfig(){
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
