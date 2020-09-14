# netty-servlet
一个基于netty实现的servlet容器, 可以替代tomcat或jetty. 导包即用,容易与springboot集成 (jdk1.8+)

作者邮箱 : 842156727@qq.com

github地址 : https://github.com/wangzihaogithub/netty-servlet

已迁移至新项目,支持这个项目的所有功能, 同时多了新特性.
 
https://github.com/wangzihaogithub/spring-boot-protocol

---

#### 优势:

测试信息 : 笔记本[4g内存,4代I5(4核cpu) ], JVM参数 : -Xms300m -Xmn300m -Xmx500m -XX:+PrintGCDetails

1.单体应用,连接复用qps=10000+ , tomcat=8000+

2.单体应用,连接不复用qps达到5100+, tomcat=4600+

3.单体应用,双jvm(1.servlet jvm, 2.session jvm), session会话存储分离, qps达到8000+, 
 

----

### 使用方法

#### 1.添加依赖, 在pom.xml中加入 （注: 1.x.x版本是用于springboot1.0，2.x.x版本用于springboot2.0）

    <dependency>
      <groupId>com.github.wangzihaogithub</groupId>
      <artifactId>spring-boot-protocol</artifactId>
      <version>2.0.12</version>
    </dependency>
	
	
#### 2.开启netty容器

    @EnableNettyEmbedded//切换容器的注解
    @SpringBootApplication
    public class ExampleApplication {
    
        public static void main(String[] args) {
            SpringApplication.run(ExampleApplication.class, args);
        }
    }

#### 3.启动, 已经成功替换tomcat, 切换至 NettyTcpServer!
	2019-02-28 22:06:16.192  INFO 9096 --- [er-Boss-NIO-2-1] c.g.n.springboot.server.NettyTcpServer   : NettyTcpServer@1 start (port = 10004, pid = 9096, protocol = [my-protocol, http, nrpc, mqtt], os = windows 8.1) ...
	2019-02-28 22:06:16.193  INFO 9096 --- [           main] c.g.example.ProtocolApplication10004     : Started ProtocolApplication10004 in 2.508 seconds (JVM running for 3.247)    
---

#### 补充: Servlet的概念

1. Servlet是一个接受Request实体类, 并填充Response实体类的过程. 整个过程是为了处理一次http交互, 这个过程分为5种类型( 参考DispatcherType )
 
注: 一次http交互, 可能会经历多次执行类型;

2. servlet中大部分类型, 都会遵守这样的流程:  filterChain(N Filter) -> Servlet(1);

通过ServletRequest#getDispatcherType可以获得当前的执行类型.

3.执行类型逻辑如下

    public enum javax.servlet.DispatcherType {
        FORWARD (
                    执行:        根据url或name找到servlet, 并执行filterChain(N Filter) -> Servlet(1)
                    触发:        由RequestDispatcher#forward
                    特性:        可以对request,Response进行任何操作

        INCLUDE ( 
                     执行:        根据url或name找到servlet, 并执行  filterChain(N Filter) -> Servlet(1)
                     触发:        RequestDispatcher#include
                     特性:        不能修改Response的header, status code , 重置body. 只能写入body.

        REQUEST(
                     执行:        根据url找到servlet, 并执行  filterChain(N Filter) -> Servlet(1)
                     触发:        收到客户端的请求后
                     特性:        可以对request,Response进行任何操作 (正常的流程)

        ASYNC(
                     执行:        返回AsyncContext(本质是个装有tcp长连接的实体类)
                     触发:        ServletRequest#startAsync
                     特性:        无阻塞并同时释放了当前线程, 不会关闭tcp连接, 并且返回AsyncContext, 
                                  用户可以将AsyncContext装在集合中, 在定时任务或者单线程中操作AsyncContext同时批量处理大量的请求),  
                                  如果一直不调用AsyncContext#complete, 则客户端阻塞(如果不是异步客户端), 服务端非阻塞.

        ERROR(
                     执行:        根据Response的status code 或 Exception 或 url找到servlet, 不执行Filter-> 执行Servlet(1)
                     触发:        ErrorPageManager#handleErrorPage
                     特性:        当的filter或者servlet都没有捕获异常, 那么会转发到错误页servlet去构造时错误页面的响应
                     备注:        当进入DispatcherType 执行流程后, 会被容器的try,catch代码包裹.
    }
