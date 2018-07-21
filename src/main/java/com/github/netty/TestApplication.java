package com.github.netty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 *
 * @author acer01
 * @date 2017/10/10/010
 */
@RestController
@SpringBootApplication
public class TestApplication {

    @RequestMapping
    public Object hallo(){
        return "啊";
    }

    /**
     * Start
     */
    public static void main(String[] args) throws IOException {
        ConfigurableApplicationContext context = SpringApplication.run(TestApplication.class, args);

        System.out.println("启动结束..");
    }



}
