package com.github.netty.core;

import com.github.netty.util.ProxyUtil;
import com.github.netty.util.NamespaceUtil;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.ExecutorServiceFactory;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class MyWorkerNioEventLoopGroup extends NioEventLoopGroup {

    public MyWorkerNioEventLoopGroup() {
        super();
    }

    public MyWorkerNioEventLoopGroup(int nEventLoops) {
        super(nEventLoops);
    }

    public MyWorkerNioEventLoopGroup(int nEventLoops, Executor executor) {
        super(nEventLoops, executor);
    }

    public MyWorkerNioEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory) {
        super(nEventLoops, executorServiceFactory);
    }

    public MyWorkerNioEventLoopGroup(int nEventLoops, Executor executor, SelectorProvider selectorProvider) {
        super(nEventLoops, executor, selectorProvider);
    }

    public MyWorkerNioEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory, SelectorProvider selectorProvider) {
        super(nEventLoops, executorServiceFactory, selectorProvider);
    }

    @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        EventLoop eventLoop = super.newChild(executor, args);
        String newName = toString()+ "-" + NamespaceUtil.newIdName(this, "nioEventLoop");
        return ProxyUtil.newProxyByJdk(eventLoop,newName ,true);
    }

    @Override
    protected ExecutorService newDefaultExecutorService(int nEventExecutors) {
        return super.newDefaultExecutorService(nEventExecutors);
    }

    @Override
    public String toString() {
        return NamespaceUtil.getIdNameClass(this,"worker");
    }

}
