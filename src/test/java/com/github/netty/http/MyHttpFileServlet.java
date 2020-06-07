package com.github.netty.http;

import com.github.netty.core.util.IOUtil;
import com.github.netty.protocol.servlet.NettyOutputStream;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.stream.ChunkedFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class MyHttpFileServlet extends HttpServlet {
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = "D://aaa.txt";
        BufferedInputStream bis = null;
        try {
            File file = new File(path);
            if (file.exists()) {
                long p = 0L;
                long toLength = 0L;
                long contentLength = 0L;
                int rangeSwitch = 0;
                long fileLength;
                String rangBytes = "";
                fileLength = file.length();

                InputStream ins = new FileInputStream(file);
                bis = new BufferedInputStream(ins);

                response.reset();
                response.setHeader("Accept-Ranges", "bytes");

                // client requests a file block download start byte
                String range = request.getHeader("Range");
                if (range != null && range.trim().length() > 0 && !"null".equals(range)) {
                    response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                    rangBytes = range.replaceAll("bytes=", "");
                    if (rangBytes.endsWith("-")) {
                        rangeSwitch = 1;
                        p = Long.parseLong(rangBytes.substring(0, rangBytes.indexOf("-")));
                        contentLength = fileLength - p;
                    } else {
                        rangeSwitch = 2;
                        String temp1 = rangBytes.substring(0, rangBytes.indexOf("-"));
                        String temp2 = rangBytes.substring(rangBytes.indexOf("-") + 1);
                        p = Long.parseLong(temp1);
                        toLength = Long.parseLong(temp2);
                        contentLength = toLength - p + 1;
                    }
                } else {
                    contentLength = fileLength;
                }

                response.setHeader("Content-Length", Long.toString(contentLength));

                if (rangeSwitch == 1) {
                    String contentRange = new StringBuffer("bytes ").append(p).append("-")
                            .append((fileLength - 1)).append("/")
                            .append(fileLength).toString();
                    response.setHeader("Content-Range", contentRange);
                    bis.skip(p);
                } else if (rangeSwitch == 2) {
                    String contentRange = range.replace("=", " ") + "/" + fileLength;
                    response.setHeader("Content-Range", contentRange);
                    bis.skip(p);
                } else {
                    String contentRange = new StringBuffer("bytes ").append("0-").append(fileLength - 1).append("/")
                            .append(fileLength).toString();
                    response.setHeader("Content-Range", contentRange);
                }

                String fileName = file.getName();
                response.setContentType("application/octet-stream");
                response.addHeader("Content-Disposition", "attachment;filename=" + fileName);

                OutputStream out = response.getOutputStream();

//                NettyOutputStream nettyOutputStream = (NettyOutputStream) out;
//                nettyOutputStream.write(file,0,file.length());

                int n = 0;
                long readLength = 0;
                int bsize = 1024 * 8;
                byte[] bytes = new byte[bsize];

                if (rangeSwitch == 2) {
                    while (readLength <= contentLength - bsize) {
                        n = bis.read(bytes);
                        readLength += n;
                        out.write(bytes, 0, n);
                    }
                    if (readLength <= contentLength) {
                        n = bis.read(bytes, 0, (int) (contentLength - readLength));
                        out.write(bytes, 0, n);
                    }
                } else {
                    while ((n = bis.read(bytes)) != -1) {
                        out.write(bytes, 0, n);
                    }
                }
                out.flush();
                out.close();
                bis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
