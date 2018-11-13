package com.github.netty.springboot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({NettyRpcClientAutoConfiguration.class,NettyRpcClientsRegistrar.class})
public @interface EnableNettyRpcClients {

	String[] value() default {};
	String[] basePackages() default {};
}
