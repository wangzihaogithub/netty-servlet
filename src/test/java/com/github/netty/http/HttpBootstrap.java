package com.github.netty.http;

import com.github.netty.StartupServer;
import com.github.netty.protocol.HttpServletProtocol;
import com.github.netty.protocol.servlet.DefaultServlet;
import com.github.netty.protocol.servlet.ServletContext;

public class HttpBootstrap {

    public static void main(String[] args) {
        StartupServer server = new StartupServer(8080);
        server.addProtocol(newHttpProtocol());
        server.start();
    }

    private static HttpServletProtocol newHttpProtocol() {
        ServletContext servletContext = new ServletContext();
        servletContext.setDocBase(System.getProperty("user.dir"), "/webapp");
        servletContext.addServlet("ServletDefaultHttpServlet", new MyHttpServlet())
                .addMapping("/hello");

        HttpServletProtocol protocol = new HttpServletProtocol(servletContext);
        return protocol;
    }
}
