package com.github.netty.http;

import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class HttpTests {

    public static void main(String[] args) throws IOException {
        jdk.incubator.http.HttpClient client = jdk.incubator.http.HttpClient.newBuilder()
                .build();

        try {
            HttpResponse<String> send = client.send(HttpRequest.newBuilder(new URI("localhost/test")).GET().build(), HttpResponse.BodyHandler.asString());

            System.out.println("client = " + send);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        System.out.println("client = " + client);
    }

}
