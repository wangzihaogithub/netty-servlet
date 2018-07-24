package com.github.netty.core;

import com.github.netty.util.NamespaceUtil;
import com.github.netty.util.ProxyUtil;
import io.netty.channel.Channel;
import io.netty.channel.EventLoop;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;

/**
 * @author 84215
 */
public class NioServerSocketChannel extends io.netty.channel.socket.nio.NioServerSocketChannel {

    public NioServerSocketChannel() {
        super();
    }

    public NioServerSocketChannel(SelectorProvider provider) {
        super(provider);
    }

    public NioServerSocketChannel(ServerSocketChannel channel) {
        super(channel);
    }

    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        SocketChannel ch = javaChannel().accept();

        try {
            if (ch != null) {
                NioSocketChannel nioSocketChannel = newNioServerSocketChannel(ch);
                buf.add(nioSocketChannel);
                return 1;
            }
        } catch (Throwable t) {
            t.printStackTrace();
//            logger.warn("Failed to create a new channel from an accepted socket.", t);

            try {
                ch.close();
            } catch (Throwable t2) {
                t2.printStackTrace();
//                logger.warn("Failed to close a socket.", t2);
            }
        }
        return 0;
    }

    private NioSocketChannel newNioServerSocketChannel(SocketChannel socketChannel){
        NioSocketChannel myNioSocketChannel = ProxyUtil.newProxyByCglib(
                NioSocketChannel.class,
                NamespaceUtil.newIdName(this,"NioSocketChannel"),true,
                new Class[]{Channel.class, SocketChannel.class},
                new Object[]{this, socketChannel});
        return myNioSocketChannel;
    }

    @Override
    protected boolean isCompatible(EventLoop eventLoop) {
        return super.isCompatible(eventLoop);
    }

}
