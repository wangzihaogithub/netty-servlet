# netty-servlet
一个基于netty实现的servlet容器, 可以替代tomcat或jetty. 导包即用,容易与springboot集成 (jdk1.8+)

作者邮箱 : 842156727@qq.com

github地址 : https://github.com/wangzihaogithub/netty-servlet

已迁移至新项目,支持这个项目的所有功能, 同时多了新特性.
 
https://github.com/wangzihaogithub/spring-boot-protocol

---

#### 优势:

    1.支持异步http请求聚合, 然后用 select * from id in (httpRequestList). 
    示例：https://github.com/wangzihaogithub/spring-boot-protocol# com.github.netty.http.example.HttpGroupByApiController.java
    
    2.支持异步零拷贝。sendFile, mmap. 
    示例：https://github.com/wangzihaogithub/spring-boot-protocol# com.github.netty.http.example.HttpZeroCopyController.java
    
    
测试信息 : 笔记本[4g内存,4代I5(4核cpu) ], JVM参数 : -Xms300m -Xmn300m -Xmx500m -XX:+PrintGCDetails

1.单体应用,连接复用qps=10000+ , tomcat=8000+

2.单体应用,连接不复用qps达到5100+, tomcat=4600+

3.单体应用,双jvm(1.servlet jvm, 2.session jvm), session会话存储分离, qps达到8000+, 
 

----

### 使用方法

#### 1.添加依赖, 在pom.xml中加入 [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.wangzihaogithub/spring-boot-protocol/badge.svg)](https://search.maven.org/search?q=g:com.github.wangzihaogithub%20AND%20a:spring-boot-protocol)

```xml
<!-- https://mvnrepository.com/artifact/com.github.wangzihaogithub/spring-boot-protocol -->
<dependency>
  <groupId>com.github.wangzihaogithub</groupId>
  <artifactId>spring-boot-protocol</artifactId>
  <version>2.2.3</version>
</dependency>
```
	
#### 2.写个自己的servlet业务逻辑类
    
    public class MyHttpServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().write("hi! doGet");
        }
    }
    
#### 3.写个main方法，启动服务

    public class HttpBootstrap {
    
        public static void main(String[] args) {
            StartupServer server = new StartupServer(8080);
            server.addProtocol(newHttpProtocol());
            server.start();
        }
    
        private static HttpServletProtocol newHttpProtocol() {
            ServletContext servletContext = new ServletContext();
            servletContext.addServlet("myHttpServlet", new MyHttpServlet())
                    .addMapping("/test");
            return new HttpServletProtocol(servletContext);
        }
    }

    
#### 4.打开浏览器

    访问http://localhost:8080/test
    页面显示 hi! doGet， 测试成功！
 
    
---

#### 补充: Servlet的概念

1.Servlet是一个接受Request实体类, 并填充Response实体类的过程. 整个过程是为了处理一次http交互, 这个过程分为5种类型( 参考DispatcherType )
 
注: 一次http交互, 可能会经历多次执行类型;

2.servlet中大部分类型, 都会遵守这样的流程:  filterChain(N Filter) -> Servlet(1);

通过ServletRequest#getDispatcherType可以获得当前的执行类型.

3.执行类型逻辑如下

    public enum javax.servlet.DispatcherType {
        FORWARD (
                    执行:        根据url或name找到servlet, 并执行filterChain(N Filter) -> Servlet(1)
                    触发:        调用RequestDispatcher#forward
                    特性:        可以对request,Response进行任何操作

        INCLUDE ( 
                     执行:        根据url或name找到servlet, 并执行  filterChain(N Filter) -> Servlet(1)
                     触发:        调用RequestDispatcher#include
                     特性:        不能修改Response的header, status code , 重置body. 只能写入body.

        REQUEST(
                     执行:        根据url找到servlet, 并执行  filterChain(N Filter) -> Servlet(1)
                     触发:        收到客户端的请求后
                     特性:        可以对request,Response进行任何操作 (正常的流程)

        ASYNC(
                     执行:        返回AsyncContext(本质是个装有tcp长连接的实体类)
                     触发:        调用ServletRequest#startAsync
                     特性:        无阻塞并同时释放了当前线程, 不会关闭tcp连接, 并且返回AsyncContext, 
                                  用户可以将AsyncContext装在集合中, 在定时任务或者单线程中操作AsyncContext同时批量处理大量的请求),  
                                  如果一直不调用AsyncContext#complete, 则客户端阻塞(如果不是异步客户端), 服务端非阻塞.

        ERROR(
                     执行:        根据Response的status code 或 Exception 或 url找到servlet, 不执行Filter-> 执行Servlet(1)
                     触发:        出现连Filter都没有捕获的异常, ErrorPageManager#handleErrorPage. 注: spring是个Servlet, Servlet被Filter包裹
                     特性:        当的filter或者servlet都没有捕获异常, 那么会转发到错误页servlet去构造时错误页面的响应
                     备注:        当进入DispatcherType 执行流程后, 会被容器的try,catch代码包裹.
    }


4.注意: 重定向(sendRedirect)的跳转是由客户端实现的, 并不由Servlet实现. 
Servlet只负责设置Response的status为302 与设置header的Location字段.