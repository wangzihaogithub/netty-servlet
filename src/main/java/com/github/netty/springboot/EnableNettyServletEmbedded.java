package com.github.netty.springboot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Inherited
@Import(NettyEmbeddedAutoConfiguration.class)
public @interface EnableNettyServletEmbedded {

}
