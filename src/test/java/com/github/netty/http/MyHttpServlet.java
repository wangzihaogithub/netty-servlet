package com.github.netty.http;

import com.github.netty.core.util.IOUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class MyHttpServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        for (int i = 0; i < 1000; i++) {
            PrintWriter writer = response.getWriter();
            writer.write("hello");
            writer.flush();
        }
    }

    public static void main(String[] args) throws IOException {
        createBigFile(1024 * 1024 * 1024);
    }

    private static void createBigFile(int chunkBytes) throws IOException {
        AtomicInteger i = new AtomicInteger();
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < 1024; j++) {
            sb.append(j);
        }
        sb.append("\n");
        byte[] bytes = sb.toString().getBytes();

        IOUtil.writeFile(new Iterator<ByteBuffer>() {
            @Override
            public boolean hasNext() {
                return i.incrementAndGet() < chunkBytes;
            }

            @Override
            public ByteBuffer next() {
                return ByteBuffer.wrap(bytes);
            }
        }, "D://", "aaa.txt", true);
    }

}
