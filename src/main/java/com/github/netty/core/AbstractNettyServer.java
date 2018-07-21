package com.github.netty.core;

import com.github.netty.core.ssl.SecureChatSslContextFactory;
import com.github.netty.util.NamespaceUtil;
import com.github.netty.util.ProxyUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.PlatformDependent;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;


/**
 * 一个抽象的netty服务端
 */
public abstract class AbstractNettyServer implements Runnable{

    private String name;
    private ServerBootstrap bootstrap;

    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private ChannelFactory<?extends ServerChannel> channelFactory;
    private ChannelInitializer<?extends Channel> initializerChannelHandler;
    private ChannelFuture closeFuture;
    private Channel serverChannel;
    private InetSocketAddress socketAddress;

    public AbstractNettyServer(int port) {
        this(new InetSocketAddress(port));
    }

    public AbstractNettyServer(InetSocketAddress address) {
        super();
        this.socketAddress = address;
        this.name = NamespaceUtil.newIdName(this.getClass(),"nettyServer");
        this.bootstrap = newServerBootstrap();
        this.boss = newBossEventLoopGroup();
        this.worker = newWorkerEventLoopGroup();
        this.channelFactory = newServerChannelFactory();
        this.initializerChannelHandler = newInitializerChannelHandler();
    }


    protected abstract ChannelInitializer<?extends Channel> newInitializerChannelHandler();

    protected SSLEngine newSSLEngine(){
        SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
        return engine;
    }

    protected ServerBootstrap newServerBootstrap(){
        return new ServerBootstrap();
    }

    protected EventLoopGroup newWorkerEventLoopGroup() {
        EventLoopGroup worker = new MyWorkerNioEventLoopGroup();
        return ProxyUtil.newProxyByJdk(worker,worker.toString(),true);
    }

    protected EventLoopGroup newBossEventLoopGroup() {
        EventLoopGroup boss = new MyBossNioEventLoopGroup(1);
        return ProxyUtil.newProxyByJdk(boss, boss.toString(),true);
    }

    protected ChannelFactory<? extends ServerChannel> newServerChannelFactory() {
        ChannelFactory<NioServerSocketChannel> serverChannelFactory = new MyServerChannelFactory();
        return ProxyUtil.newProxyByJdk(serverChannelFactory,serverChannelFactory.toString(),true);
    }

    @Override
    public final void run() {
        bootstrap
                .group(boss, worker)
                .channelFactory(channelFactory)
                .childHandler(initializerChannelHandler)
                .option(ChannelOption.SO_BACKLOG, 128) // determining the number of connections queued
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);

        try {
            ChannelFuture channelFuture = bootstrap.bind(socketAddress);
            //堵塞
            channelFuture.await();
            //唤醒后获取异常
            Throwable cause = channelFuture.cause();

            startAfter(cause);

            //没异常就 堵塞住close的回调
            if(cause == null) {
                serverChannel = channelFuture.channel();
                closeFuture = serverChannel.closeFuture();
                closeFuture.sync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                boss.shutdownGracefully().sync();         //7
                worker.shutdownGracefully().sync();
                serverChannel.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if(closeFuture != null) {
            closeFuture.notify();
        }
    }

    public int getPort() {
        return socketAddress.getPort();
    }

    protected void startAfter(Throwable cause){
        //有异常抛出
        if(cause != null){
            PlatformDependent.throwException(cause);
        }
    }

    @Override
    public String toString() {
        return name+"{" +
                "port=" + getPort() +
                '}';
    }

}
