package com.github.netty.http;

import com.github.netty.StartupServer;
import com.github.netty.protocol.HttpServletProtocol;
import com.github.netty.protocol.servlet.DefaultServlet;
import com.github.netty.protocol.servlet.ServletContext;

public class FileApplication {
    public static void main(String[] args) {
        StartupServer server = new StartupServer(80);
        server.addProtocol(newHttpProtocol());
        server.start();
        // http://localhost/myfile.html
        // http://localhost/a/myfile.html
    }

    private static HttpServletProtocol newHttpProtocol() {
        ServletContext servletContext = new ServletContext();
//        servletContext.setDocBase("D://demo", "/webapp");
        servletContext.setDocBase(System.getProperty("user.dir"), "/webapp");

        HttpServletProtocol protocol = new HttpServletProtocol(servletContext);
//        protocol.setEnableH2c(true);
//        protocol.setSslFileJks();
        return protocol;
    }
}