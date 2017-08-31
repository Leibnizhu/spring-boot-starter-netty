package io.gitlab.leibnizhu.sbnetty.request;

import io.gitlab.leibnizhu.sbnetty.core.NettyAsyncContext;
import io.gitlab.leibnizhu.sbnetty.core.NettyContext;
import io.gitlab.leibnizhu.sbnetty.core.NettyRequestDispatcher;
import io.gitlab.leibnizhu.sbnetty.core.ServletContentHandler;
import io.gitlab.leibnizhu.sbnetty.session.NettyHttpSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Leibniz
 */
public class NettyHttpServletRequest implements HttpServletRequest {
    public static final String DISPATCHER_TYPE = NettyRequestDispatcher.class.getName() + ".DISPATCHER_TYPE";

    private final ChannelHandlerContext ctx;
    private final NettyContext servletContext;
    private final HttpRequest request;
    private final HttpRequestInputStream inputStream;

    private boolean asyncSupported = true;
    private NettyAsyncContext asyncContext;
    private HttpServletResponse servletResponse;

    public NettyHttpServletRequest(ChannelHandlerContext ctx, ServletContentHandler handler, HttpRequest request, HttpServletResponse servletResponse) {
        this.ctx = ctx;
        this.servletContext = handler.getServletContext();
        this.request = request;
        this.servletResponse = servletResponse;
        this.inputStream = handler.getInputStream();
        this.attributes = new ConcurrentHashMap<>();
        this.headers = request.headers();

        parseCookie();
        parseSession();
    }

    @SuppressWarnings("unused")
    HttpRequest getNettyRequest() {
        return request;
    }


    /*====== Cookie 相关方法 开始 ======*/
    private Cookie[] cookies;
    private transient boolean isCookieParsed = false;

    @Override
    public Cookie[] getCookies() {
        if (!isCookieParsed) {
            parseCookie();
        }
        return cookies;
    }

    /**
     * 解析request中的Cookie到本类的cookies数组中
     *
     * @author Leibniz
     */
    private void parseCookie() {
        if (isCookieParsed) {
            return;
        }

        String cookieOriginStr = this.headers.get("Cookie");
        if (cookieOriginStr == null) {
            return;
        }
        Set<io.netty.handler.codec.http.cookie.Cookie> nettyCookies = ServerCookieDecoder.LAX.decode(cookieOriginStr);
        if (nettyCookies.size() == 0) {
            return;
        }
        this.cookies = new Cookie[nettyCookies.size()];
        Iterator<io.netty.handler.codec.http.cookie.Cookie> itr = nettyCookies.iterator();
        int i = 0;
        while (itr.hasNext()) {
            io.netty.handler.codec.http.cookie.Cookie nettyCookie = itr.next();
            Cookie servletCookie = new Cookie(nettyCookie.name(), nettyCookie.value());
//            servletCookie.setMaxAge(Ints.checkedCast(nettyCookie.maxAge()));
            if(nettyCookie.domain() != null) servletCookie.setDomain(nettyCookie.domain());
            if(nettyCookie.path() != null) servletCookie.setPath(nettyCookie.path());
            servletCookie.setHttpOnly(nettyCookie.isHttpOnly());
            this.cookies[i++] = servletCookie;
        }

        this.isCookieParsed = true;
    }

    /*====== Cookie 相关方法 结束 ======*/


    /*====== Header 相关方法 开始 ======*/
    private HttpHeaders headers;

    @Override
    public long getDateHeader(String name) {
        return this.headers.getTimeMillis(name);
    }

    @Override
    public String getHeader(String name) {
        return this.headers.get(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return Collections.enumeration(this.headers.getAll(name));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(this.headers.names());
    }

    @Override
    public int getIntHeader(String name) {
        String headerStringValue = this.headers.get(name);
        if (headerStringValue == null) {
            return -1;
        }
        return Integer.parseInt(headerStringValue);
    }
    /*====== Header 相关方法 结束 ======*/


    /*====== 各种路径 相关方法 开始 ======*/
    private String servletPath;
    private String queryString;
    private String pathInfo;
    private String requestUri;
    private transient boolean isPathsParsed = false;

    private void checkAndParsePaths(){
        if(isPathsParsed){
            return;
        }

        String servletPath = request.uri().replace(servletContext.getContextPath(), "");
        if (!servletPath.startsWith("/")) {
            servletPath = "/" + servletPath;
        }
        int queryInx = servletPath.indexOf('?');
        if (queryInx > -1) {
            this.queryString = servletPath.substring(queryInx + 1, servletPath.length());
            servletPath = servletPath.substring(0, queryInx);
        }
        this.servletPath = servletPath;
        this.requestUri = this.servletContext.getContextPath() + servletPath; //TODO 加上pathInfo
        this.pathInfo = null;

        isPathsParsed = true;
    }

    @Override
    public String getMethod() {
        return request.method().name();
    }

    //TODO ServletPath和PathInfo应该是互补的，根据URL-Pattern匹配的路径不同而不同
    // 现在把PathInfo恒为null，ServletPath恒为uri-contextPath
    // 可以满足SpringBoot的需求，但不满足ServletPath和PathInfo的语义
    // 需要在RequestUrlPatternMapper匹配的时候设置,new NettyRequestDispatcher的时候传入MapperData
    @Override
    public String getPathInfo() {
        checkAndParsePaths();
        return this.pathInfo;
    }

    @Override
    public String getQueryString() {
        checkAndParsePaths();
        return this.queryString;
    }

    @Override
    public String getRequestURI() {
        checkAndParsePaths();
        return this.requestUri;
    }

    @Override
    public String getServletPath() {
        checkAndParsePaths();
        return this.servletPath;
    }

    @Override
    public String getContextPath() {
        return servletContext.getContextPath();
    }

    @Override
    public StringBuffer getRequestURL() {
        return null;
    }

    @Override
    @Deprecated
    public String getRealPath(String path) {
        return null;
    }
     /*====== 各种路径 相关方法 结束 ======*/


    /*====== Session 相关方法 开始 ======*/
    private NettyHttpSession session;
    private boolean isCookieSession;
    private boolean isURLSession;

    /**
     * 先后看请求路径和Cookie中是否有sessionid
     * 有，则从SessionManager获取session对象放入session属性
     * 如果session对象过期，则创建一个新的并放入
     * 无，则创建一个新Session并放入
     */
    private void parseSession() {
        String sessionId;
        NettyHttpSession curSession;

        //从Cookie解析SessionID
        sessionId = getSessionIdFromCookie();
        if(sessionId != null){
            curSession = servletContext.getSessionManager().getSession(sessionId);
            if (null != curSession) {
                this.isCookieSession = true;
                recoverySession(curSession);
                return;
            }
        }

        if (!this.isCookieSession) {
            // 从请求路径解析SessionID
            sessionId = getSessionIdFromUrl();
            curSession = servletContext.getSessionManager().getSession(sessionId);
            if(null != curSession){
                this.isURLSession = true;
                recoverySession(curSession);
                return;
            }
        }
        //Cookie和请求参数中都没拿到Session，则创建一个
        if (this.session == null) {
            this.session = createtSession();
        }
    }

    /**
     * @return 从URL解析到的SessionID
     */
    private String getSessionIdFromUrl() {
        StringBuilder u = new StringBuilder(request.uri());
        int sessionStart = u.toString().indexOf(";" + NettyHttpSession.SESSION_REQUEST_PARAMETER_NAME + "=");
        if(sessionStart == -1) {
            return null;
        }
        int sessionEnd = u.toString().indexOf(';', sessionStart + 1);
        if (sessionEnd == -1)
            sessionEnd = u.toString().indexOf('?', sessionStart + 1);
        if (sessionEnd == -1) // still
            sessionEnd = u.length();
        return u.substring(sessionStart + NettyHttpSession.SESSION_REQUEST_PARAMETER_NAME.length() + 2, sessionEnd);
    }

    /**
     * @return 从Cookie解析到的SessionID
     */
    private String getSessionIdFromCookie() {
        if(cookies == null){
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(NettyHttpSession.SESSION_COOKIE_NAME)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 恢复旧Session
     * @param curSession 要恢复的Session对象
     */
    private void recoverySession(NettyHttpSession curSession) {
        this.session = curSession;
        this.session.setNew(false);
        this.servletContext.getSessionManager().updateAccessTime(this.session);
    }

    @Override
    public HttpSession getSession(boolean create) {
        boolean valid = isRequestedSessionIdValid(); //在管理器存在，且没到期
        //可用则直接返回
        if (valid) {
            return session.getSession();
        }
        //不可用则判断是否新建
        if (!create) {
            session = null; //如果过期了设为null
            return null;
        }
        //不可用且允许新建则新建之
        this.session = createtSession();
        return this.session.getSession();
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public String changeSessionId() {
        this.session = createtSession();
        return this.session.getId();
    }

    private NettyHttpSession createtSession() {
        return servletContext.getSessionManager().createSession();
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return servletContext.getSessionManager().checkValid(session);
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return isCookieSession;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return isURLSession;
    }

    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    @Override
    public String getRequestedSessionId() {
        return session.getId();
    }
    /*====== Session 相关方法 结束 ======*/



    /*====== Request Parameters 相关方法 开始 ======*/

    private transient boolean isParameterParsed = false; //请求参数是否已经解析

    private Map<String, String[]> paramMap = new HashMap<>(); //存储请求参数

    /**
     * 解析请求参数
     */
    private void parseParameter() {
        if (isParameterParsed) {
            return;
        }

        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            List<String> valueList = entry.getValue();
            String[] valueArray = new String[valueList.size()];
            paramMap.put(entry.getKey(), valueList.toArray(valueArray));
        }

        this.isParameterParsed = true;
    }

    /**
     * 检查请求参数是否已经解析。
     * 如果还没有则解析之。
     */
    private void checkParameterParsed() {
        if (!isParameterParsed) {
            parseParameter();
        }
    }

    @Override
    public String getParameter(String name) {
        checkParameterParsed();
        String[] values = paramMap.get(name);
        if (values == null || values.length == 0) {
            return null;
        }
        return values[0];
    }

    @Override
    public Enumeration<String> getParameterNames() {
        checkParameterParsed();
        return Collections.enumeration(paramMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        checkParameterParsed();
        return paramMap.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        checkParameterParsed();
        return paramMap;
    }

    /*====== Request Parameters 相关方法 结束 ======*/


    /*====== 请求协议、地址、端口 相关方法 开始 ======*/
    @Override
    public String getProtocol() {
        return request.protocolVersion().toString();
    }

    @Override
    public String getScheme() {
        return request.protocolVersion().protocolName();
    }

    private InetSocketAddress socketAddress; //请求的服务地址
    private transient boolean isServerParsed = false; //请求服务地址是否已经解析过

    private void checkAndParseServer() {
        if (isServerParsed) {
            return;
        }
        String hostHeader = headers.get("Host");
        if (hostHeader != null) {
            String[] parsed = hostHeader.split(":");
            if (parsed.length > 1) {
                socketAddress = new InetSocketAddress(parsed[0], Integer.parseInt(parsed[1]));
            } else {
                socketAddress = new InetSocketAddress(parsed[0], 80);
            }
        }
        isServerParsed = true;
    }

    @Override
    public String getServerName() {
        checkAndParseServer();
        return socketAddress.getHostName();
    }

    @Override
    public int getServerPort() {
        checkAndParseServer();
        return socketAddress.getPort();
    }

    @Override
    public String getLocalName() {
        return "localhost";
    }

    @Override
    public String getLocalAddr() {
        return "127.0.0.1";
    }

    @Override
    public int getLocalPort() {
        return 0;
    }

    @Override
    public String getRemoteAddr() {
        return ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
    }

    @Override
    public String getRemoteHost() {
        return ((InetSocketAddress) ctx.channel().remoteAddress()).getHostName();
    }

    @Override
    public int getRemotePort() {
        return ((InetSocketAddress) ctx.channel().remoteAddress()).getPort();
    }

    /*====== 请求协议、地址、端口 相关方法 结束 ======*/


    /*====== Request Attributes 相关方法 开始 ======*/
    private final Map<String, Object> attributes;

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object o) {
        attributes.put(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public DispatcherType getDispatcherType() {
        return attributes.containsKey(DISPATCHER_TYPE) ? (DispatcherType) attributes.get(DISPATCHER_TYPE) : DispatcherType.REQUEST;
    }

    /*====== Request Attributes 相关方法 结束 ======*/



    /*====== 异步 相关方法 开始 ======*/

    @Override
    public AsyncContext startAsync() {
        return startAsync(this, null);
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        return ((NettyAsyncContext) getAsyncContext()).startAsync(servletRequest, servletResponse);
    }

    @Override
    public boolean isAsyncStarted() {
        return null != asyncContext && asyncContext.isAsyncStarted();
    }

    @SuppressWarnings("unused")
    void setAsyncSupported(boolean asyncSupported) {
        this.asyncSupported = asyncSupported;
    }

    @Override
    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    @Override
    public AsyncContext getAsyncContext() {
        if (null == asyncContext) {
            asyncContext = new NettyAsyncContext(this, ctx);
        }
        return asyncContext;
    }

     /*====== 异步 相关方法 结束 ======*/

    /*====== multipart/form-data 相关方法 开始 ======*/
    @Override
    public Collection<Part> getParts() throws IOException, IllegalStateException, ServletException {
        //TODO
        return null;
    }

    @Override
    public Part getPart(String name) throws IOException, IllegalStateException, ServletException {
        //TODO
        return null;
    }

     /*====== multipart/form-data 相关方法 结束 ======*/

    @Override
    public boolean isSecure() {
        return getScheme().equalsIgnoreCase("HTTPS");
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    public ServletResponse getServletResponse() {
        return servletResponse;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(inputStream, getCharacterEncoding()));
    }

    @Override
    public int getContentLength() {
        return inputStream.getCurrentLength();
    }

    @Override
    public long getContentLengthLong() {
        return (long) getContentLength();
    }

    private String characterEncoding;

    @Override
    public String getCharacterEncoding() {
        if (characterEncoding == null) {
            characterEncoding = parseCharacterEncoding();
        }
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        characterEncoding = env;
    }

    @Override
    public String getContentType() {
        return headers.get("content-type");
    }

    private static final String DEFAULT_CHARSET = "UTF-8";

    private String parseCharacterEncoding() {
        String contentType = getContentType();
        if (contentType == null) {
            return DEFAULT_CHARSET;
        }
        int start = contentType.indexOf("charset=");
        if (start < 0) {
            return DEFAULT_CHARSET;
        }
        String encoding = contentType.substring(start + 8);
        int end = encoding.indexOf(';');
        if (end >= 0) {
            encoding = encoding.substring(0, end);
        }
        encoding = encoding.trim();
        if ((encoding.length() > 2) && (encoding.startsWith("\""))
                && (encoding.endsWith("\""))) {
            encoding = encoding.substring(1, encoding.length() - 1);
        }
        return encoding.trim();
    }


    /*====== 以下是暂不处理的接口方法 ======*/

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    @Override
    public String getAuthType() {
        return null;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
    }

    @Override
    public void logout() throws ServletException {
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        return null;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public String getRemoteUser() {
        return null;
    }
}
