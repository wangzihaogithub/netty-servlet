# netty-container
一个基于netty实现的servlet容器, 可以很好的与springboot集成 (jdk1.8+)

作者邮箱 : 842156727@qq.com
github地址 : https://github.com/wangzihaogithub

###使用方法
####1.添加依赖, 在pom.xml中加入

    <dependency>
      <groupId>com.github.wangzihaogithub</groupId>
      <artifactId>netty-container</artifactId>
      <version>1.0.0</version>
    </dependency>
	
	
####2.注册进springboot容器中

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
         * @return
         */
        @Bean
        public ServletContextInitializer servletContextInitializer(){
            return this::setServletContext;
        }
     }

#### 3.完成!, 快去启动服务看看吧

