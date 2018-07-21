# netty-container
一个基于netty实现的servlet容器, 可以很好的与springboot继承

使用方法
1.加入下面的代码

@Configuration
public class WebAppConfig extends WebMvcConfigurationSupport {

    /**
     * 注册netty容器
     * @return
     */
    @Bean
    public NettyEmbeddedServletContainerFactory nettyEmbeddedServletContainerFactory(){
        NettyEmbeddedServletContainerFactory factory = new NettyEmbeddedServletContainerFactory();
        return factory;
    }

    /**
     * 如果(您当前的config类继承了WebMvcConfigurationSupport){
     *      则需要注册ServletContextInitializer类, 并主动调用setServletContext(servletContext); 不然springboot会无法启动
     * }否则{
     *     没继承就没事,不用写这个servletContextInitializer方法
     * }
     * 
     * @return 
     */
    @Bean
    public ServletContextInitializer servletContextInitializer(){
        return this::setServletContext;
    }
  
}


2.恭喜你, 完成!
