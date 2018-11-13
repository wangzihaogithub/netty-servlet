# netty-servlet
一个基于netty实现的servlet容器, 可以替代tomcat或jetty. 导包即用,容易与springboot集成 (jdk1.8+)

作者邮箱 : 842156727@qq.com

github地址 : https://github.com/wangzihaogithub

---

#### 优势:

测试信息 : 笔记本[4g内存,4代I5(4核cpu) ], JVM参数 : -Xms300m -Xmn300m -Xmx500m -XX:+PrintGCDetails

1.单体应用,连接复用qps=10000+ , tomcat=8000+

2.单体应用,连接不复用qps达到5100+, tomcat=4600+

3.单体应用,双jvm(1.servlet jvm, 2.session jvm), session会话存储分离, qps达到8000+, 
 
 tomcat底层虽然支持,但非常复杂,大家往往都用springboot-redis, 但redis与spring集成后, 无法发挥其原本的性能

----

### 使用方法

#### 1.添加依赖, 在pom.xml中加入 （注: 1.x.x+版本是用于springboot1.0，2.x.x+版本用于springboot2.0）

    <dependency>
      <groupId>com.github.wangzihaogithub</groupId>
      <artifactId>netty-servlet</artifactId>
      <version>2.0.0</version>
    </dependency>
	
	
#### 2.开启netty容器

    @EnableNettyServletEmbedded//切换容器的注解
    @SpringBootApplication
    public class ExampleServletApplication {
    
        public static void main(String[] args) {
            SpringApplication.run(ExampleServletApplication.class, args);
        }
    }

#### 3.完成!

    2018-11-13 19:29:46.176  INFO 17544 --- [           main] c.g.n.e.s.ExampleServletApplication      : Started ExampleServletApplication in 1.847 seconds (JVM running for 2.988)
    2018-11-13 19:29:46.424  INFO 17544 --- [ettyTcpServer@1] c.g.netty.springboot.NettyTcpServer      : NettyTcpServer@1 start [port = 10002, os = windows 10, pid = 17544]...
