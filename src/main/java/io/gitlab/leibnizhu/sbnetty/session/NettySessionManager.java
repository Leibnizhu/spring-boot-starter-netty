package io.gitlab.leibnizhu.sbnetty.session;

import io.gitlab.leibnizhu.sbnetty.core.NettyContext;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Leibniz.Hu
 * Created on 2017-08-28 20:59.
 */
public class NettySessionManager {
    private NettyContext servletContext;
    private Map<String, NettyHttpSession> sessions = new ConcurrentHashMap<>();
    private static final int SESSION_LIFE_MILLISECONDS = 1000 * 60 * 30;
    private static final int SESSION_LIFE_CHECK_INTER = 1000 * 60;

    public NettySessionManager(NettyContext servletContext){
        this.servletContext = servletContext;
        new Thread(new checkInvalidSessions()).start();
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void invalidate(HttpSession session) {
        sessions.remove(session.getId());
    }

    public void updateAccessTime(HttpSession session){
        if(session instanceof NettyHttpSession){
            ((NettyHttpSession)session).updateAccessTime();
        }
    }

    public boolean checkValid(HttpSession session) {
        return session != null && session instanceof NettyHttpSession && sessions.get(session.getId()) != null;
    }

    public NettyHttpSession getSession(String id){
        return id == null ? null : sessions.get(id);
    }

    public NettyHttpSession createSession(){
        String id = createUniqueSessionId();
        NettyHttpSession newSession = new NettyHttpSession(id, this);
        sessions.put(id ,newSession);
        return newSession;
    }

    private String createUniqueSessionId() {
        String prefix = String.valueOf(100000 + new Random().nextInt(899999));
        return new StringBuilder().append(System.currentTimeMillis()).reverse().append(prefix).toString();
    }

    public void setOldSession(HttpSession session) {
        if(session instanceof NettyHttpSession){
            ((NettyHttpSession)session).setNew(false);
        }
    }

    /**
     * 超时的Session无效化，定期执行
     */
    private class checkInvalidSessions implements Runnable {
        @Override
        public void run() {
            while(true){
                try {
                    Thread.sleep(SESSION_LIFE_CHECK_INTER);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long curTime = System.currentTimeMillis();
                for(NettyHttpSession session : sessions.values()){
                    if(curTime - session.getLastAccessedTime() >= SESSION_LIFE_MILLISECONDS){
                        session.invalidate();
                    }
                }
            }
        }
    }
}
