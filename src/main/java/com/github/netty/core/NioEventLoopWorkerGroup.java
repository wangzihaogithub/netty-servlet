package com.github.netty.core;

import com.github.netty.util.ProxyUtil;
import com.github.netty.util.NamespaceUtil;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.ExecutorServiceFactory;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public class NioEventLoopWorkerGroup extends NioEventLoopGroup {

    public NioEventLoopWorkerGroup() {
        super();
    }

    public NioEventLoopWorkerGroup(int nEventLoops) {
        super(nEventLoops);
    }

    public NioEventLoopWorkerGroup(int nEventLoops, Executor executor) {
        super(nEventLoops, executor);
    }

    public NioEventLoopWorkerGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory) {
        super(nEventLoops, executorServiceFactory);
    }

    public NioEventLoopWorkerGroup(int nEventLoops, Executor executor, SelectorProvider selectorProvider) {
        super(nEventLoops, executor, selectorProvider);
    }

    public NioEventLoopWorkerGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory, SelectorProvider selectorProvider) {
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
