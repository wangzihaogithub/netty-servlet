package com.github.netty.springboot;

import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.core.AbstractNettyServer;
import com.github.netty.core.ProtocolsRegister;
import com.github.netty.core.util.NettyThreadX;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * netty容器 tcp层面的服务器
 *
 * @author acer01
 *  2018/7/14/014
 */
public class NettyTcpServer extends AbstractNettyServer implements WebServer {

    /**
     * 容器配置信息
     */
    private final NettyProperties config;
    /**
     * servlet线程
     */
    private Thread servletServerThread;
    /**
     * 协议注册器列表
     */
    private List<ProtocolsRegister> protocolsRegisterList = new ArrayList<>();

    public NettyTcpServer(InetSocketAddress serverAddress, NettyProperties config){
        super(serverAddress);
        this.config = config;
    }

    @Override
    public void start() throws WebServerException {
        try{
            super.setIoRatio(config.getServerIoRatio());
            super.setWorkerCount(config.getServerWorkerCount());
            for(ProtocolsRegister protocolsRegister : protocolsRegisterList){
                protocolsRegister.onServerStart();
            }
        } catch (Exception e) {
            throw new WebServerException(e.getMessage(),e);
        }
        servletServerThread = new NettyThreadX(this,getName());
        servletServerThread.start();
    }

    @Override
    public void stop() throws WebServerException {
        try{
            for(ProtocolsRegister protocolsRegister : protocolsRegisterList){
                protocolsRegister.onServerStop();
            }
        } catch (Exception e) {
            throw new WebServerException(e.getMessage(),e);
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
