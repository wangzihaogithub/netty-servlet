package com.github.netty.servlet;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 一个默认的servlet
 *
 * @author acer01
 *  2018/7/15/015
 */
public class ServletDefaultHttpServlet extends HttpServlet {

    public static final ServletDefaultHttpServlet INSTANCE = new ServletDefaultHttpServlet();

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AsyncContext context = request.startAsync();
        context.start(()->{
            try {
                response.getWriter().write("aaa啊");
                context.complete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
