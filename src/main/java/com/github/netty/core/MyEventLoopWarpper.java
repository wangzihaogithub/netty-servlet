package com.github.netty.core;

import io.netty.channel.*;
import io.netty.util.concurrent.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * 版本5.0.0.Alpha2 更新
 *  EventLoop.class
 *  1.增加unwrap方法
 *  2.增加register方法
 *  3.增加close方法
 *
 * @author 84215
 */
public class MyEventLoopWarpper implements EventLoop {
    
    private EventLoop source;

    public MyEventLoopWarpper(EventLoop source) {
        Objects.requireNonNull(source);
        this.source = source;
    }

    public EventLoop getSource() {
        return source;
    }

    @Override
    public EventLoopGroup parent() {
        return getSource().parent();
    }

    @Override
    public EventLoop unwrap() {
        return getSource();
    }

    @Override
    public boolean inEventLoop() {
        return getSource().inEventLoop();
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        return getSource().inEventLoop();
    }

    @Override
    public <V> Promise<V> newPromise() {
        return getSource().newPromise();
    }

    @Override
    public <V> ProgressivePromise<V> newProgressivePromise() {
        return getSource().newProgressivePromise();
    }

    @Override
    public <V> Future<V> newSucceededFuture(V v) {
        return getSource().newSucceededFuture(v);
    }

    @Override
    public <V> Future<V> newFailedFuture(Throwable throwable) {
        return getSource().newFailedFuture(throwable);
    }

    @Override
    public Future<?> submit(Runnable runnable) {
        return getSource().submit(runnable);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getSource().invokeAll(tasks);
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return getSource().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getSource().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return getSource().invokeAny(tasks, timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Runnable runnable, T t) {
        return getSource().submit(runnable, t);
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        return getSource().submit(callable);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long l, TimeUnit timeUnit) {
        return getSource().schedule(runnable, l, timeUnit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long l, TimeUnit timeUnit) {
        return getSource().schedule(callable, l, timeUnit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
        return getSource().scheduleAtFixedRate(runnable, l, l1, timeUnit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long l, long l1, TimeUnit timeUnit) {
        return getSource().scheduleWithFixedDelay(runnable, l, l1, timeUnit);
    }

    @Override
    public boolean isShuttingDown() {
        return getSource().isShuttingDown();
    }

    @Override
    public Future<?> shutdownGracefully() {
        return getSource().shutdownGracefully();
    }

    @Override
    public Future<?> shutdownGracefully(long l, long l1, TimeUnit timeUnit) {
        return getSource().shutdownGracefully();
    }

    @Override
    public Future<?> terminationFuture() {
        return getSource().terminationFuture();
    }

    @Override
    public void shutdown() {
        getSource().shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return getSource().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return getSource().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return getSource().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return getSource().awaitTermination(timeout, unit);
    }

    @Override
    public EventLoop next() {
        return getSource().next();
    }

    @Override
    public ChannelFuture register(Channel channel) {
        return getSource().register(channel);
    }

    @Override
    public ChannelFuture register(Channel channel, ChannelPromise promise) {
        return getSource().register(channel, promise);
    }

    @Override
    public <E extends EventExecutor> Set<E> children() {
        return getSource().children();
    }

    @Override
    public ChannelHandlerInvoker asInvoker() {
        return getSource().asInvoker();
    }

    @Override
    public void execute(Runnable command) {
        getSource().execute(command);
    }

    @Override
    public String toString() {
        return getSource().toString();
    }

    @Override
    public void close() throws Exception {
        getSource().close();
    }
}
