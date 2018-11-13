package com.github.netty.springboot;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * netty容器自动配置
 * @author 84215
 */
@Configuration
public class NettyEmbeddedAutoConfiguration {

    @Bean("nettyServerFactory")
    @DependsOn("nettyProperties")
    @ConditionalOnMissingBean(NettyTcpServerFactory.class)
    public NettyTcpServerFactory nettyTcpServerFactory(@Qualifier("nettyProperties") NettyProperties nettyProperties){
        return new NettyTcpServerFactory(nettyProperties);
    }

}
