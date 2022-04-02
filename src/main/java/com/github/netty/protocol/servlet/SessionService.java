package com.github.netty.protocol.servlet;


import java.util.List;

/**
 * Session service
 * @author wangzihao
 * 2018/8/19/019
 */
public interface SessionService {

    /**
     * Get session (by id)
     * @param sessionId sessionId
     * @return Session
     */
    Session getSession(String sessionId);

    /**
     * Save the session
     * @param session session
     */
    void saveSession(Session session);

    /**
     * Delete session
     * @param sessionId sessionId
     */
    void removeSession( String sessionId);

    /**
     * Delete session (batch)
     * @param sessionIdList sessionIdList
     */
    void removeSessionBatch(List<String> sessionIdList);

    /**
     * Change the sessionId
     * @param oldSessionId oldSessionId
     * @param newSessionId newSessionId
     */
    void changeSessionId(String oldSessionId,  String newSessionId);

    /**
     * Get the number of sessions
     * @return count
     */
    int count();

}
