package com.github.netty.session;

import com.github.netty.core.util.LoggerFactoryX;
import com.github.netty.core.util.LoggerX;
import com.github.netty.core.util.NamespaceUtil;
import com.github.netty.springboot.NettyProperties;

import java.net.InetSocketAddress;
import java.util.List;

/**
 *  组合会话服务
 * @author 84215
 */
public class CompositeSessionServiceImpl implements SessionService {

    private LoggerX logger = LoggerFactoryX.getLogger(getClass());
    private String name = NamespaceUtil.newIdName(getClass());

    private SessionService localSessionService;
    private SessionService remoteSessionService;

    public CompositeSessionServiceImpl() {
        this.localSessionService = new LocalSessionServiceImpl();
    }

    public void enableRemoteSession(InetSocketAddress address,NettyProperties config){
        this.remoteSessionService = new RemoteSessionServiceImpl(address,config);
    }

    @Override
    public void saveSession(Session session) {
        try {
            getSessionServiceImpl().saveSession(session);
        }catch (Throwable t){
            logger.error(t.toString());
        }
    }

    @Override
    public void removeSession(String sessionId) {
        getSessionServiceImpl().removeSession(sessionId);
    }

    @Override
    public void removeSessionBatch(List<String> sessionIdList) {
        getSessionServiceImpl().removeSessionBatch(sessionIdList);
    }

    @Override
    public Session getSession(String sessionId) {
        try {
            // TODO: 10月16日/0016 缺少自动切换功能
            return getSessionServiceImpl().getSession(sessionId);
        }catch (Throwable t){
            logger.error(t.toString());
            return null;
        }
    }

    @Override
    public void changeSessionId(String oldSessionId, String newSessionId) {
        getSessionServiceImpl().changeSessionId(oldSessionId, newSessionId);
    }

    @Override
    public int count() {
        return getSessionServiceImpl().count();
    }

    protected SessionService getSessionServiceImpl() {
        if(remoteSessionService != null) {
            return remoteSessionService;
        }
        return localSessionService;
    }

    @Override
    public String toString() {
        return name;
    }

}
