package com.github.netty.springboot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * netty容器自动配置
 * @author 84215
 */
@Configuration
public class NettyEmbeddedAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(NettyProperties.class)
    public NettyProperties nettyProperties(){
        NettyProperties config = new NettyProperties();
        return config;
    }

    @Bean
    @ConditionalOnMissingBean(NettyTcpServerFactory.class)
    public NettyTcpServerFactory nettyTcpServerFactory(){
        return new NettyTcpServerFactory(nettyProperties());
    }

}
