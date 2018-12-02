package com.github.netty.session;

import com.github.netty.core.util.LoggerFactoryX;
import com.github.netty.core.util.LoggerX;
import com.github.netty.core.util.NamespaceUtil;
import com.github.netty.servlet.support.ResourceManager;
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

    private SessionService sessionService;

    public CompositeSessionServiceImpl() {
    }

    public void enableLocalMemorySession(){
        removeSessionService();
        this.sessionService = new LocalMemorySessionServiceImpl();
    }

    public void enableRemoteRpcSession(InetSocketAddress address, NettyProperties config){
        removeSessionService();
        this.sessionService = new RemoteRpcSessionServiceImpl(address,config);
    }

    public void enableLocalFileSession(ResourceManager resourceManager){
        removeSessionService();
        this.sessionService = new LocalFileSessionServiceImpl(resourceManager);
    }

    public void removeSessionService(){
        if(sessionService == null){
            return;
        }
        try {
            if (sessionService instanceof LocalMemorySessionServiceImpl) {
                ((LocalMemorySessionServiceImpl) sessionService).getSessionInvalidThread().interrupt();
            } else if (sessionService instanceof LocalFileSessionServiceImpl) {
                ((LocalFileSessionServiceImpl) sessionService).getSessionInvalidThread().interrupt();
            }
        }catch (Exception e){
            //
        }
        sessionService = null;
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
        if(sessionService == null) {
            enableLocalMemorySession();
        }
        return sessionService;
    }

    @Override
    public String toString() {
        return name;
    }

}
