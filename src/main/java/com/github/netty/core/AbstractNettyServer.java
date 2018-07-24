package com.github.netty.core;

import com.github.netty.util.HostUtil;
import com.github.netty.util.NamespaceUtil;
import com.github.netty.util.ProxyUtil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.util.internal.PlatformDependent;

import java.net.InetSocketAddress;


/**
 * 一个抽象的netty服务端
 * @author 84215
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
    private boolean enableEpoll;

    public AbstractNettyServer(int port) {
        this(new InetSocketAddress(port));
    }

    public AbstractNettyServer(InetSocketAddress address) {
        super();
        this.enableEpoll = HostUtil.isLinux();
        this.socketAddress = address;
        this.name = NamespaceUtil.newIdName(this.getClass(),"nettyServer");
        this.bootstrap = newServerBootstrap();
        this.boss = newBossEventLoopGroup();
        this.worker = newWorkerEventLoopGroup();
        this.channelFactory = newServerChannelFactory();
        this.initializerChannelHandler = newInitializerChannelHandler();
    }


    protected abstract ChannelInitializer<?extends Channel> newInitializerChannelHandler();

    protected ServerBootstrap newServerBootstrap(){
        return new NettyServerBootstrap();
    }

    protected EventLoopGroup newWorkerEventLoopGroup() {
        EventLoopGroup worker;
        int nEventLoopCount = Runtime.getRuntime().availableProcessors() * 2;
        if(enableEpoll){
            worker = new EpollEventLoopGroup(nEventLoopCount);
        }else {
            NioEventLoopWorkerGroup jdkWorker = new NioEventLoopWorkerGroup(nEventLoopCount);
            worker = ProxyUtil.newProxyByJdk(jdkWorker, jdkWorker.toString(), true);
        }
        return worker;
    }

    protected EventLoopGroup newBossEventLoopGroup() {
        EventLoopGroup boss;
        if(enableEpoll){
            boss = new EpollEventLoopGroup(1);
        }else {
            NioEventLoopBossGroup jdkBoss = new NioEventLoopBossGroup(1);
            boss = ProxyUtil.newProxyByJdk(jdkBoss, jdkBoss.toString(), true);
        }
        return boss;
    }

    protected ChannelFactory<? extends ServerChannel> newServerChannelFactory() {
        ChannelFactory<? extends ServerChannel> channelFactory;
        if(enableEpoll){
            channelFactory = EpollServerSocketChannel::new;
        }else {
            ChannelFactory<NioServerSocketChannel> serverChannelFactory = new NioServerChannelFactory();
            channelFactory = ProxyUtil.newProxyByJdk(serverChannelFactory, serverChannelFactory.toString(), true);
        }
        return channelFactory;
    }

    @Override
    public final void run() {
        bootstrap
                .group(boss, worker)
                .channelFactory(channelFactory)
                .childHandler(initializerChannelHandler)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
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
                boss.shutdownGracefully().sync();
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
        if(socketAddress == null){
            return 0;
        }
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
