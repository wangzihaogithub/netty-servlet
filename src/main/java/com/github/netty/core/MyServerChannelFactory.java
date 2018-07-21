package com.github.netty.core;

import com.github.netty.util.ProxyUtil;
import com.github.netty.util.NamespaceUtil;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFactory;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class MyServerChannelFactory implements ChannelFactory<NioServerSocketChannel> {

    @Override
    public NioServerSocketChannel newChannel() {
        try {
//            MyNioServerSocketChannel myNioServerSocketChannel = new MyNioServerSocketChannel(eventLoop,childGroup);
            MyNioServerSocketChannel myNioServerSocketChannel = ProxyUtil.newProxyByCglib(MyNioServerSocketChannel.class,
                    toString() + "-"+ NamespaceUtil.newIdName(this,"serverSocketChannel"),
                    true);

            return myNioServerSocketChannel;
        } catch (Throwable t) {
            throw new ChannelException("Unable to create Channel from class nioServerSocketChannel", t);
        }
    }

    @Override
    public String toString() {
        return NamespaceUtil.getIdNameClass(this,getClass().getSimpleName());
    }

}