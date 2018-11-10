package com.github.netty.springboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

/**
 * RPC客户端自动配置
 * @author 84215
 */
@Configuration
public class NettyRpcClientAutoConfiguration {

    @Value("${netty.address:localhost:9000}")
    private String address;

    @Bean
    @ConditionalOnMissingBean(NettyRpcLoadBalanced.class)
    public NettyRpcLoadBalanced nettyRpcLoadBalanced(){
        return new PropertyLoadBalanced();
    }

    public class PropertyLoadBalanced implements NettyRpcLoadBalanced {
        private String lastAddressStr;
        private InetSocketAddress lastAddress;

        @Override
        public InetSocketAddress chooseAddress(String serviceId) {
            if(!address.equals(lastAddressStr)){
                lastAddressStr = address;
                String[] addressArr = lastAddressStr.split(":");
                lastAddress = new InetSocketAddress(addressArr[0], Integer.parseInt(addressArr[1]));
            }
            return lastAddress;
        }

    }

}
