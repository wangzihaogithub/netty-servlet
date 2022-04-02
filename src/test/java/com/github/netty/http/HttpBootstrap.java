package com.github.netty.http;

import com.github.netty.StartupServer;
import com.github.netty.protocol.HttpServletProtocol;
import com.github.netty.protocol.servlet.ServletContext;

public class HttpBootstrap {

    public static void main(String[] args) {
        StartupServer server = new StartupServer(8080);
        server.addProtocol(newHttpProtocol());
        server.start();
    }

    private static HttpServletProtocol newHttpProtocol() {
        ServletContext servletContext = new ServletContext();
        servletContext.addServlet("myHttpServlet", new MyHttpFileServlet())
                .addMapping("/test");
        servletContext.addServlet("ServletDefaultHttpServlet", new MyHttpServlet())
                .addMapping("/test/hello");

        HttpServletProtocol protocol = new HttpServletProtocol(servletContext);
        return protocol;
    }
}
