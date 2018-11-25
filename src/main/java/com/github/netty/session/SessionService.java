package com.github.netty.session;

import com.github.netty.rpc.annotation.RpcService;

import java.util.List;

/**
 * session会话服务
 * @author acer01
 * 2018/8/19/019
 */
@RpcService(value = "/hrpc/SessionService",timeout = 1000)
public interface SessionService {

    /**
     * 获取session (根据id)
     * @param sessionId
     * @return
     */
    Session getSession(String sessionId);

    /**
     * 保存session
     * @param session
     */
    void saveSession(Session session);

    /**
     * 删除session
     * @param sessionId
     */
    void removeSession(String sessionId);

    /**
     * 删除session (批量)
     * @param sessionIdList
     */
    void removeSessionBatch(List<String> sessionIdList);

    /**
     * 改变sessionId
     * @param oldSessionId
     * @param newSessionId
     */
    void changeSessionId(String oldSessionId,String newSessionId);

    /**
     * 获取session数量
     * @return
     */
    int count();

}
