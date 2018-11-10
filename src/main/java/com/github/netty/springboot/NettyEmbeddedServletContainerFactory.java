package com.github.netty.springboot;

import com.github.netty.core.util.StringUtil;
import com.github.netty.core.util.ThreadPoolX;
import com.github.netty.servlet.*;
import com.github.netty.servlet.util.MimeMappingsX;
import com.github.netty.session.CompositeSessionServiceImpl;
import com.github.netty.session.SessionService;
import org.springframework.boot.context.embedded.*;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.DispatcherServlet;

import javax.net.ssl.SSLException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * netty容器工厂
 *
 * EmbeddedWebApplicationContext - createEmbeddedServletContainer
 * ImportAwareBeanPostProcessor
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyEmbeddedServletContainerFactory extends AbstractEmbeddedServletContainerFactory implements EmbeddedServletContainerFactory , ResourceLoaderAware {

    protected ResourceLoader resourceLoader;
    private NettyProperties config;

    public NettyEmbeddedServletContainerFactory() {
        this(new NettyProperties());
    }

    public NettyEmbeddedServletContainerFactory(NettyProperties config) {
        this.config = config;
    }

    /**
     * 获取servlet容器
     * @param initializers 初始化
     * @return
     */
    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... initializers) {
        try {
            ServletContext servletContext = newServletContext();
            NettyEmbeddedServletContainer container = newNettyEmbeddedServletContainer(servletContext);

            //默认 servlet
            if (isRegisterDefaultServlet()) {
                registerDefaultServlet(servletContext);
            }

            //jsp servlet
            JspServlet jspServlet = getJspServlet();
            if(shouldRegisterJspServlet()){
                registerJspServlet(servletContext,jspServlet);
            }

            //初始化
            ServletContextInitializer[] servletContextInitializers = mergeInitializers(initializers);
            for (ServletContextInitializer initializer : servletContextInitializers) {
                initializer.onStartup(servletContext);
            }

            return container;
        }catch (Exception e){
            throw new IllegalStateException(e.getMessage(),e);
        }
    }

    /**
     * 新建servlet上下文
     * @return
     */
    protected ServletContext newServletContext(){
        ClassLoader parentClassLoader = resourceLoader != null ? resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader();
        ServletSessionCookieConfig sessionCookieConfig = loadSessionCookieConfig();
        MimeMappingsX mimeMappings = newMimeMappings();

        ServletContext servletContext = new ServletContext(
                new InetSocketAddress(getAddress(),getPort()),
                new URLClassLoader(new URL[]{}, parentClassLoader),
                getContextPath(),
                getServerHeader(),
                sessionCookieConfig,
                mimeMappings);

        //session超时时间
        servletContext.setSessionTimeout(getSessionTimeout());
        servletContext.setSessionService(newSessionService());
        servletContext.setAsyncExecutorSupplier(newAsyncExecutorSupplier());
        servletContext.getServletEventListenerManager().setServletAddedListener(servlet -> {
            if(servlet instanceof DispatcherServlet){
//                return null;
//                return new SpringDispatcherServlet();
            }
            return servlet;
        });
        return servletContext;
    }

    /**
     * 新建mime映射
     * @return
     */
    protected MimeMappingsX newMimeMappings(){
        MimeMappings mimeMappings = getMimeMappings();
        MimeMappingsX mimeMappingsX = new MimeMappingsX();
        for (MimeMappings.Mapping mapping :mimeMappings) {
            mimeMappingsX.add(mapping.getExtension(),mapping.getMimeType());
        }
        return mimeMappingsX;
    }

    /**
     * 新建会话服务
     * @return
     */
    protected SessionService newSessionService(){
        //组合会话
        CompositeSessionServiceImpl compositeSessionService = new CompositeSessionServiceImpl();
        String remoteSessionServerAddress = config.getSessionRemoteServerAddress();
        //启用远程会话管理, 利用RPC
        if(StringUtil.isNotEmpty(remoteSessionServerAddress)) {
            InetSocketAddress address;
            if(remoteSessionServerAddress.contains(":")){
                String[] addressArr = remoteSessionServerAddress.split(":");
                address = new InetSocketAddress(addressArr[0], Integer.parseInt(addressArr[1]));
            }else {
                address = new InetSocketAddress(remoteSessionServerAddress,80);
            }
            compositeSessionService.enableRemoteSession(address,config);
        }
        return compositeSessionService;
    }

    /**
     * 注册默认servlet
     * @param servletContext servlet上下文
     */
    protected void registerDefaultServlet(ServletContext servletContext){
        ServletDefaultHttpServlet defaultServlet = new ServletDefaultHttpServlet();
        servletContext.addServlet("default",defaultServlet).addMapping("/");
    }

    /**
     * 注册 jsp servlet
     * @param servletContext servlet上下文
     */
    protected void registerJspServlet(ServletContext servletContext,JspServlet jspServlet){

    }

    /**
     * 新建netty容器
     * @param servletContext servlet上下文
     * @return netty容器
     * @throws SSLException ssl异常
     */
    protected NettyEmbeddedServletContainer newNettyEmbeddedServletContainer(ServletContext servletContext) throws SSLException {
        Ssl ssl = getSsl();
        NettyEmbeddedServletContainer container = new NettyEmbeddedServletContainer(servletContext,ssl,config);
        return container;
    }

    protected Supplier<ExecutorService> newAsyncExecutorSupplier(){
        return new Supplier<ExecutorService>() {
            private ExecutorService executorService;
            @Override
            public ExecutorService get() {
                if(executorService == null) {
                    synchronized (this){
                        if(executorService == null) {
                            executorService = new ThreadPoolX("Async",8);
//                            executorService = new DefaultEventExecutorGroup(15);
                        }
                    }
                }
                return executorService;
            }
        };
    }

    /**
     * 加载session的cookie配置
     * @return cookie配置
     */
    protected ServletSessionCookieConfig loadSessionCookieConfig(){
        ServletSessionCookieConfig sessionCookieConfig = new ServletSessionCookieConfig();
        sessionCookieConfig.setMaxAge(-1);
        return sessionCookieConfig;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
