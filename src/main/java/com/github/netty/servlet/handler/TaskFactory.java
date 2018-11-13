package com.github.netty.servlet.handler;

import com.github.netty.springboot.NettyProperties;
import com.github.netty.servlet.ServletContext;
import io.netty.channel.ChannelHandlerContext;

/**
 * IO任务包的工厂类
 * @author 84215
 */
public interface TaskFactory {

    /**
     * 新建一个任务
     * @param servletContext servlet上下文
     * @param config 容器的配置信息
     * @param context 连接
     * @param msg 消息 (注意! : 不会自动释放, 需要手动释放)
     * @return
     */
    Runnable newTask(ServletContext servletContext, NettyProperties config, ChannelHandlerContext context, Object msg);

}
