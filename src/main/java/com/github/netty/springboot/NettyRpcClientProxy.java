package com.github.netty.springboot;

import com.github.netty.rpc.RpcClient;
import com.github.netty.rpc.RpcClientInstance;
import com.github.netty.rpc.exception.RpcConnectException;
import com.github.netty.rpc.exception.RpcException;
import io.netty.util.concurrent.FastThreadLocal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RPC客户端 (线程安全)
 * @author 84215
 */
public class NettyRpcClientProxy implements InvocationHandler {

    private String serviceId;
    private String serviceName;
    private Class<?> interfaceClass;
    private NettyProperties config;
    private NettyRpcLoadBalanced loadBalanced;

    private static final FastThreadLocal<Map<InetSocketAddress,RpcClient>> CLIENT_MAP_THREAD_LOCAL = new FastThreadLocal<Map<InetSocketAddress,RpcClient>>(){
        @Override
        protected Map<InetSocketAddress,RpcClient> initialValue() throws Exception {
            return new HashMap<>(5);
        }
    };

    NettyRpcClientProxy(String serviceId, String serviceName, Class interfaceClass, NettyProperties config, NettyRpcLoadBalanced loadBalanced) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.interfaceClass = interfaceClass;
        this.config = config;
        this.loadBalanced = loadBalanced;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcClient rpcClient = getClient();
        Object instance = rpcClient.getInstance(serviceName);
        if(instance == null){
            instance = rpcClient.newInstance(interfaceClass,config.getRpcTimeout(),serviceName);
        }
        RpcClientInstance rpcClientInstance = (RpcClientInstance) Proxy.getInvocationHandler(instance);
        return rpcClientInstance.invoke(proxy,method,args);
    }

    /**
     * ping一次 会新建客户端并销毁客户端
     * @return ping返回的消息
     * @throws RpcException
     */
    public byte[] pingOnceAfterDestroy() throws RpcException{
        InetSocketAddress address = chooseAddress();
        RpcClient rpcClient = new RpcClient("Ping-",address);
        rpcClient.setSocketChannelCount(1);
        rpcClient.setWorkerCount(1);
        rpcClient.run();

        try {
            byte[] response = rpcClient.getRpcCommandService().ping();
            return response;
        }finally {
            rpcClient.stop();
        }
    }

    /**
     * 获取RPC客户端 (从当前线程获取,如果没有则自动创建)
     * @return
     */
    private RpcClient getClient(){
        InetSocketAddress address = chooseAddress();
        Map<InetSocketAddress,RpcClient> rpcClientMap = CLIENT_MAP_THREAD_LOCAL.get();
        RpcClient rpcClient = rpcClientMap.get(address);
        if(rpcClient == null) {
            rpcClient = new RpcClient(address);
            rpcClient.setSocketChannelCount(1);
            rpcClient.setWorkerCount(config.getRpcClientWorkerCount());
            rpcClient.run();
            if (config.isEnablesRpcClientAutoReconnect()) {
                rpcClient.enableAutoReconnect(config.getRpcClientHeartIntervalSecond(), TimeUnit.SECONDS,null);
            }
            rpcClientMap.put(address,rpcClient);
        }
        return rpcClient;
    }

    private InetSocketAddress chooseAddress(){
        InetSocketAddress address;
        try {
            address = loadBalanced.chooseAddress(serviceId);
        }catch (Exception e){
            throw new RpcConnectException("选择客户端地址失败",e);
        }
        if (address == null) {
            throw new NullPointerException("选择客户端地址失败, 获取客户端地址为null");
        }
        return address;
    }

}
