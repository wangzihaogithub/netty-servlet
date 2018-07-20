package com.github.netty.core;

import com.github.netty.core.ssl.SecureChatSslContextFactory;
import com.github.netty.util.ProxyUtil;
import com.github.netty.util.ClassIdFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.internal.PlatformDependent;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * https://blog.csdn.net/yrpting/article/details/52859721
 * jconsole
 *
 * 年轻代young  幸存者survivor 老年代tenured  1.8(包含)去除持久代permanent generation
 * -Xms10m初始栈大小 -Xmx430m最大从操作系统获取内存 -Xmn10m年轻代 打印gc -XX:+PrintGCDetails
 *
 * jvm调优-> 参考https://blog.csdn.net/ywb201314/article/details/52051701
 * https://my.oschina.net/go4it/blog/1628795
 *
 * young GC只收集young gen，
 * full GC会收集整个GC堆。
 *
 * JDK8里的HotSpot VM就没有perm gen了。请注意
 *
 * Young + old + perm
 *
 * 许多Web应用里对象会有这样的特征：
 ·(a) 有一部分对象几乎一直活着。这些可能是常用数据的cache之类的
 ·(b) 有一部分对象创建出来没多久之后就没用了。这些很可能会响应一个请求时创建出来的临时对象
 ·(c) 最后可能还有一些中间的对象，创建出来之后不会马上就死，但也不会一直活着。

 如果是这样的模式，那young gen可以设置得非常大，大到每次young GC的时候里面的多数对象(b)最好已经死了。
 想像一下，如果young gen太小，每次满了就触发一次young GC，那么young GC就会很频繁，或许很多临时对象(b)正好还在被是使用（还没死），这样的话young GC的收集效率就会比较低。要避免这样的情况，最好是就是把young gen设大一些。


 如果老年代设置过小，就会频繁触发full gc，full gc是非常耗时的。
 年轻代在经过n(hotspot默认是15)轮后会进入老年代，
 这样老年代顶不住了，就会触发full gc，回收时需要stop the world，这样系统经常发生长时间停顿，影响系统的吞吐量

 */
public abstract class AbstractNettyServer implements Runnable{

    private String name;
    private ServerBootstrap bootstrap;

    private EventLoopGroup boss;
    private EventLoopGroup worker;
    private ChannelFactory<?extends ServerChannel> channelFactory;
    private ChannelInitializer<?extends Channel> initializerChannelHandler;
    private final Thread parentThread;
    private ChannelFuture closeFuture;
    private Channel serverChannel;
    private InetSocketAddress socketAddress;
    private ByteBuffer byteBuffer;

    public AbstractNettyServer(int port) {
        this(new InetSocketAddress(port));
    }

    public AbstractNettyServer(InetSocketAddress address) {
        super();

//        byteBuffer = ByteBuffer.allocate(10 * 1024 *1024);
//        for(int i=0; i<byteBuffer.remaining(); i++){
//            byteBuffer.putInt((byte) i);
//        }

        this.socketAddress = address;
        this.parentThread = Thread.currentThread();
        this.name = ClassIdFactory.newIdName(this.getClass(),"nettyServer");
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
