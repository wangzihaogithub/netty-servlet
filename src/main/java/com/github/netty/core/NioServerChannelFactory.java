package com.github.netty.core;

import com.github.netty.util.NamespaceUtil;
import com.github.netty.util.ProxyUtil;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFactory;

/**
 * @author 84215
 */
public class NioServerChannelFactory implements ChannelFactory<NioServerSocketChannel> {

    @Override
    public NioServerSocketChannel newChannel() {
        try {
//            MyNioServerSocketChannel myNioServerSocketChannel = new MyNioServerSocketChannel(eventLoop,childGroup);
            NioServerSocketChannel myNioServerSocketChannel = ProxyUtil.newProxyByCglib(NioServerSocketChannel.class,
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