package com.github.netty.http;

import com.github.netty.core.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;

public class HttpTests {

    public static void main(String[] args) throws IOException {
        URL url = new URL("http://localhost:8080/test/sayHello?name=xiaowang");
        InputStream inputStream = url.openStream();
        String responseBody = IOUtil.readInput(inputStream);

        assert Objects.equals("hi! xiaowang",responseBody);
    }

}
