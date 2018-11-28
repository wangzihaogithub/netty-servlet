package com.github.netty.register;

import com.github.netty.core.ProtocolsRegister;
import com.github.netty.core.util.ApplicationX;
import com.github.netty.rpc.RpcRequest;
import com.github.netty.rpc.RpcResponse;
import com.github.netty.rpc.RpcServerHandler;
import com.github.netty.rpc.RpcUtil;
import com.github.netty.rpc.annotation.RpcService;
import com.github.netty.rpc.codec.RpcDecoder;
import com.github.netty.rpc.codec.RpcEncoder;
import com.github.netty.rpc.service.RpcCommandServiceImpl;
import com.github.netty.rpc.service.RpcDBServiceImpl;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 内部RPC协议注册器
 * @author acer01
 * 2018/11/25/025
 */
public class HRpcProtocolsRegister implements ProtocolsRegister {
    private RpcEncoder rpcEncoder = new RpcEncoder(RpcResponse.class);
    private RpcServerHandler rpcServerHandler = new RpcServerHandler();
    private Supplier rpcRequestSupplier = RpcRequest::new;
    private ApplicationX application;
    private AtomicBoolean addInstancePluginsFlag = new AtomicBoolean(false);

    public HRpcProtocolsRegister(ApplicationX application) {
        this.application = application;
    }

    public void addInstance(Object instance){
        rpcServerHandler.addInstance(instance);
    }

    public void addInstance(Object instance,String serviceName,Function<Method,String[]> methodToParameterNamesFunction){
        rpcServerHandler.addInstance(instance,serviceName,methodToParameterNamesFunction);
    }

    public boolean existInstance(Object instance){
        return rpcServerHandler.existInstance(instance);
    }

    @Override
    public String getProtocolName() {
        return "hrpc";
    }

    @Override
    public boolean canSupport(ByteBuf msg) {
        return RpcUtil.isRpcProtocols(msg);
    }

    @Override
    public void register(Channel channel) throws Exception {
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addLast(new RpcDecoder(rpcRequestSupplier));
        pipeline.addLast(rpcEncoder);
        pipeline.addLast(rpcServerHandler);
    }

    @Override
    public void onServerStart() throws Exception {
        //用户的服务
        List list = application.getBeanForAnnotation(RpcService.class);
        for(Object serviceImpl : list){
            if(existInstance(serviceImpl)){
                continue;
            }
            addInstance(serviceImpl);
        }
        addInstancePlugins();
    }

    @Override
    public void onServerStop() throws Exception {

    }

    /**
     * 添加扩展的实例
     */
    protected void addInstancePlugins(){
        if(addInstancePluginsFlag != null && addInstancePluginsFlag.compareAndSet(false,true)) {
            //默认开启rpc基本命令服务
            addInstance(new RpcCommandServiceImpl());
            //默认开启DB服务
            addInstance(new RpcDBServiceImpl());
            addInstancePluginsFlag = null;
        }
    }

    protected ApplicationX getApplication() {
        return application;
    }

}
