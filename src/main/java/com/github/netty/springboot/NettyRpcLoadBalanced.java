package com.github.netty.springboot;

import java.net.InetSocketAddress;

/**
 * @author 84215
 */
@FunctionalInterface
public interface NettyRpcLoadBalanced {

    /**
     * 挑选一个IP地址
     * @param serviceId 服务ID
     * @return IP地址
     */
    InetSocketAddress chooseAddress(String serviceId);

}
