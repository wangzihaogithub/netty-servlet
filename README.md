# netty-servlet
一个基于netty实现的servlet容器, 可以替代tomcat或jetty. 导包即用,容易与springboot集成 (jdk1.8+)

作者邮箱 : 842156727@qq.com

github地址 : https://github.com/wangzihaogithub

---

#### 优势:

测试信息 : 笔记本[4g内存,4代I5(4核cpu) ], JVM参数 : -Xms300m -Xmn300m -Xmx500m -XX:+PrintGCDetails

1.单体应用,连接复用qps=10000+ , tomcat=8000+

2.单体应用,连接不复用qps达到5100+, tomcat=4600+

3.单体应用,双jvm(1.servlet jvm, 2.session jvm), session会话存储分离, qps达到1300+, 
 
 tomcat底层虽然支持,但非常复杂,大家往往都用springboot-redis, 但redis与spring集成后, 无法发挥其原本的性能

----

### 使用方法

#### 1.添加依赖, 在pom.xml中加入

    <dependency>
      <groupId>com.github.wangzihaogithub</groupId>
      <artifactId>netty-servlet</artifactId>
      <version>1.2.0</version>
    </dependency>
	
	
#### 2.注册进springboot容器中

    @Configuration
    public class WebAppConfig {
    
        /**
         * 注册netty容器
         * @return
         */
        @Bean
        public NettyEmbeddedServletContainerFactory nettyEmbeddedServletContainerFactory(){
            NettyEmbeddedServletContainerFactory factory = new NettyEmbeddedServletContainerFactory();
            return factory;
        }
     
     }

#### 3.完成! 快去启动服务看看吧

