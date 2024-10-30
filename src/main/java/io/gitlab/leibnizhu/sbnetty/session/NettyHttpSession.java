package io.gitlab.leibnizhu.sbnetty.session;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Leibniz.Hu
 * Created on 2017-08-28 20:57.
 */
public class NettyHttpSession implements HttpSession, Serializable {
    public static final String SESSION_COOKIE_NAME = "JSESSIONID";
    public static final String SESSION_REQUEST_PARAMETER_NAME = "jsessionid";
    private NettySessionManager manager;
    private long creationTime;
    private long lastAccessedTime;
    private int interval = NettySessionManager.SESSION_LIFE_SECONDS;
    private String id;

    NettyHttpSession(String id, NettySessionManager manager){
        long curTime = System.currentTimeMillis();
        this.creationTime = curTime;
        this.lastAccessedTime = curTime;
        this.id = id;
        this.manager = manager;
        this.sessionFacade = new NettyHttpSessionFacade(this);
    }

    private HttpSession sessionFacade;

    public HttpSession getSession(){
        return sessionFacade;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    void updateAccessTime() {
        lastAccessedTime = System.currentTimeMillis();
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return manager.getServletContext();
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.interval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return interval;
    }

    @Override
    @Deprecated
    public HttpSessionContext getSessionContext() {
        return null;
    }

    private Map<String, Object> attributes = new ConcurrentHashMap<>();

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    @Deprecated
    public Object getValue(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    @Deprecated
    public String[] getValueNames() {
        Set<String> nameSet = attributes.keySet();
        String[] nameArray = new String[nameSet.size()];
        return nameSet.toArray(nameArray);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    @Deprecated
    public void putValue(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    @Deprecated
    public void removeValue(String name) {
        attributes.remove(name);
    }

    @Override
    public void invalidate() {
        attributes.clear();
        attributes = null;
        manager.invalidate(this);
        manager = null;
    }

    private boolean isNew = true;
    @Override
    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew){
        this.isNew = isNew;
    }

    /**
     * 是否过期
     */
    public boolean expire(){
        return System.currentTimeMillis() - creationTime >= interval * 1000;
    }
}
