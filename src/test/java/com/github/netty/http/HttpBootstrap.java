package com.github.netty.http;

import com.github.netty.StartupServer;
import com.github.netty.core.AbstractProtocol;
import com.github.netty.protocol.HttpServletProtocol;
import com.github.netty.protocol.servlet.ServletContext;

import java.util.ArrayList;
import java.util.List;

public class HttpBootstrap {

    public static void main(String[] args) {
        StartupServer startupServer = new StartupServer(8080);

        List<AbstractProtocol> protocols = new ArrayList<>();
        protocols.add(newHttpServletProtocol());
        for (AbstractProtocol protocol : protocols) {
            startupServer.getProtocolHandlers().add(protocol);
            startupServer.getServerListeners().add(protocol);
        }
        startupServer.start();
    }

    private static HttpServletProtocol newHttpServletProtocol(){
        ServletContext servletContext = new ServletContext();
        servletContext.addServlet("myHttpServlet",new MyHttpServlet())
                .addMapping("/test/sayHello");
        return new HttpServletProtocol(null, servletContext);
    }
}
