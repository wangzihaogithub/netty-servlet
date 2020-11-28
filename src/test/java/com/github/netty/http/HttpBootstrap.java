package com.github.netty.http;

import com.github.netty.StartupServer;
import com.github.netty.protocol.HttpServletProtocol;
import com.github.netty.protocol.servlet.ServletContext;

public class HttpBootstrap {

    public static void main(String[] args) {
        HttpServletProtocol httpProtocol = newHttpProtocol();

        StartupServer server = new StartupServer(8080);
        server.getProtocolHandlers().add(httpProtocol);
        server.getServerListeners().add(httpProtocol);
        server.start();
    }

    private static HttpServletProtocol newHttpProtocol(){
        ServletContext servletContext = new ServletContext();
        servletContext.addServlet("myHttpServlet",new MyHttpFileServlet())
                .addMapping("/test");
        servletContext.addServlet("ServletDefaultHttpServlet",new MyHttpServlet())
                .addMapping("/test/hello");

        HttpServletProtocol protocol = new HttpServletProtocol(servletContext);
        protocol.setMaxBufferBytes(1024 * 1024);//每个连接的输出流缓冲区上限,网速好就写大点. (字节. 1M)
        return protocol;
    }
}
