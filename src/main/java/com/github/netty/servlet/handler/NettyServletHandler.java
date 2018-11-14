package com.github.netty.servlet.handler;

import com.github.netty.springboot.NettyProperties;
import com.github.netty.core.AbstractChannelHandler;
import com.github.netty.servlet.ServletContext;
import com.github.netty.servlet.ServletHttpSession;
import com.github.netty.servlet.support.HttpServletObject;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * servlet处理器 (服务器的入口)
 * @author acer01
 *  2018/7/1/001
 */
@ChannelHandler.Sharable
public class NettyServletHandler extends AbstractChannelHandler<Object> {

    private Executor dispatcherExecutor;
    private ServletContext servletContext;
    private NettyProperties config;
    private static final HttpTaskFactory HTTP_TASK_FACTORY = new HttpTaskFactory();
    private static final AttributeKey<TaskFactory> CHANNEL_ATTR_KEY_TASK_FACTORY = AttributeKey.valueOf(TaskFactory.class + "#Handler-TaskFactory");

    public NettyServletHandler(ServletContext servletContext, NettyProperties config) {
        super(false);
        this.servletContext = Objects.requireNonNull(servletContext);
        this.config = Objects.requireNonNull(config);
        this.dispatcherExecutor = config.getServerHandlerExecutor();
    }

    @Override
    protected void onMessageReceived(ChannelHandlerContext context, Object msg) throws Exception {
        TaskFactory taskFactory = getTaskFactory(context.channel());
        Runnable task = taskFactory.newTask(servletContext,config,context,msg);

        if(dispatcherExecutor != null){
            dispatcherExecutor.execute(task);
        }else {
            task.run();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        saveAndClearSession(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error(cause.toString());
        saveAndClearSession(ctx);
        ctx.channel().close();
    }

    /**
     * 保存并且清空会话
     * @param ctx
     */
    protected void saveAndClearSession(ChannelHandlerContext ctx){
        ServletHttpSession httpSession = HttpServletObject.getSession(ctx);
        if(httpSession != null) {
            if (httpSession.isValid()) {
                servletContext.getSessionService().saveSession(httpSession.unwrap());
                logger.info("saveHttpSession : sessionId="+httpSession.getId());
            } else if (httpSession.getId() != null) {
                servletContext.getSessionService().removeSession(httpSession.getId());
                logger.info("removeHttpSession : sessionId="+httpSession.getId());
            }
            httpSession.clear();
        }
    }

    /**
     * 把IO任务包工厂类 放到这个连接上
     * @param channel
     * @param taskFactory
     */
    public static void setTaskFactory(Channel channel, TaskFactory taskFactory){
        channel.attr(CHANNEL_ATTR_KEY_TASK_FACTORY).set(taskFactory);
    }

    /**
     * 取出这个连接上的 IO任务包工厂类
     * @param channel
     * @return
     */
    public static TaskFactory getTaskFactory(Channel channel){
        TaskFactory taskFactory = channel.attr(CHANNEL_ATTR_KEY_TASK_FACTORY).get();
        return taskFactory == null? HTTP_TASK_FACTORY : taskFactory;
    }

}
