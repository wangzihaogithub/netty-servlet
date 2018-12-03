package com.github.netty.springboot.server;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.AbstractNettyServer;
import com.github.netty.core.ProtocolsRegister;
import com.github.netty.core.util.NettyThreadX;
import com.github.netty.springboot.NettyProperties;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;

import java.net.InetSocketAddress;
import java.util.*;

/**
 * netty容器
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyEmbeddedServletContainer extends AbstractNettyServer implements EmbeddedServletContainer {

    /**
     * 容器配置信息
     */
    private final NettyProperties properties;
    /**
     * servlet线程
     */
    private Thread servletServerThread;
    /**
     * 协议注册器列表
     */
    private List<ProtocolsRegister> protocolsRegisterList = new LinkedList<>();

    public NettyEmbeddedServletContainer(InetSocketAddress serverAddress, NettyProperties properties){
        super(serverAddress);
        this.properties = properties;
    }

    @Override
    public void start() throws EmbeddedServletContainerException {
        try{
            super.setIoRatio(properties.getServerIoRatio());
            super.setIoThreadCount(properties.getServerIoThreads());
            for(ProtocolsRegister protocolsRegister : protocolsRegisterList){
                protocolsRegister.onServerStart();
            }

            List<ProtocolsRegister> inApplicationProtocolsRegisterList = new ArrayList<>(properties.getApplication().findBeanForType(ProtocolsRegister.class));
            inApplicationProtocolsRegisterList.sort(Comparator.comparing(ProtocolsRegister::order));
            for(ProtocolsRegister protocolsRegister : inApplicationProtocolsRegisterList){
                protocolsRegister.onServerStart();
                protocolsRegisterList.add(protocolsRegister);
            }

            protocolsRegisterList.sort(Comparator.comparing(ProtocolsRegister::order));
        } catch (Exception e) {
            throw new EmbeddedServletContainerException(e.getMessage(),e);
        }
        servletServerThread = new NettyThreadX(this,getName());
        servletServerThread.start();
    }

    @Override
    public void stop() throws EmbeddedServletContainerException {
        try{
            for(ProtocolsRegister protocolsRegister : protocolsRegisterList){
                protocolsRegister.onServerStop();
            }
        } catch (Exception e) {
            throw new EmbeddedServletContainerException(e.getMessage(),e);
        }
        super.stop();
        if(servletServerThread != null) {
            servletServerThread.interrupt();
        }
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    /**
     * 初始化 IO执行器
     * @return
     */
    @Override
    protected ChannelInitializer<? extends Channel> newInitializerChannelHandler() {
        return new ChannelInitializer<SocketChannel>() {
            ChannelHandler dynamicProtocolHandler = new DynamicProtocolHandler();
            @ChannelHandler.Sharable
            class DynamicProtocolHandler extends AbstractChannelHandler<ByteBuf> {
                private DynamicProtocolHandler() {
                    super(false);
                }

                @Override
                protected void onMessageReceived(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                    Channel channel = ctx.channel();
                    channel.pipeline().remove(this);
                    for(ProtocolsRegister protocolsRegister : protocolsRegisterList){
                        if(protocolsRegister.canSupport(msg)){
                            logger.info("channel register by [{0}]",protocolsRegister.getProtocolName());
                            protocolsRegister.register(channel);
                            channel.pipeline().fireChannelRead(msg);
                            return;
                        }
                    }
                }
            }

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                //HTTP编码解码
                pipeline.addLast("DynamicProtocolHandler", dynamicProtocolHandler);
            }
        };
    }

    /**
     * 添加协议注册器
     * @param protocolsRegister
     */
    public void addProtocolsRegister(ProtocolsRegister protocolsRegister){
        protocolsRegisterList.add(protocolsRegister);
    }

}
