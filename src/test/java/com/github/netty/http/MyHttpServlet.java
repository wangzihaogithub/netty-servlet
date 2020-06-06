package com.github.netty.http;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.Collection;

public class MyHttpServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Collection<Part> parts = request.getParts();
        for (Part part : parts) {
            String submittedFileName = part.getSubmittedFileName();
            System.out.println("submittedFileName = " + submittedFileName);
        }

        String name = request.getParameter("name");
        response.getWriter().write("hi! " + name);
    }
}
