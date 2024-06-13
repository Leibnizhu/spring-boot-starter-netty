package io.gitlab.leibnizhu.sbnetty.request;

import com.google.common.base.Splitter;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import io.gitlab.leibnizhu.sbnetty.core.NettyAsyncContext;
import io.gitlab.leibnizhu.sbnetty.core.NettyContext;
import io.gitlab.leibnizhu.sbnetty.core.NettyRequestDispatcher;
import io.gitlab.leibnizhu.sbnetty.session.NettyHttpSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.NetUtil;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Leibniz
 */
public class NettyHttpServletRequest implements HttpServletRequest {
    public static final String DISPATCHER_TYPE = NettyRequestDispatcher.class.getName() + ".DISPATCHER_TYPE";

    private final ChannelHandlerContext ctx;
    private final NettyContext servletContext;
    private final HttpRequest request;
    private final HttpRequestInputStream inputStream;
    private final HttpPostRequestDecoder httpPostRequestDecoder;
    private boolean asyncSupported = true;
    private NettyAsyncContext asyncContext;

    public NettyHttpServletRequest(ChannelHandlerContext ctx, NettyContext servletContext,
                                   HttpRequest request, HttpPostRequestDecoder httpPostRequestDecoder,
                                   HttpRequestInputStream requestInputStream) {
        this.ctx = ctx;
        this.servletContext = servletContext;
        this.request = request;
        this.inputStream = requestInputStream;
        this.httpPostRequestDecoder = httpPostRequestDecoder;
        this.attributes = new ConcurrentHashMap<>();
        this.headers = request.headers();
        parseSession();
    }

    public boolean isKeepAlive() {
        return HttpUtil.isKeepAlive(request);
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
        if (nettyCookies.isEmpty()) {
            return;
        }
        this.cookies = new Cookie[nettyCookies.size()];
        Iterator<io.netty.handler.codec.http.cookie.Cookie> itr = nettyCookies.iterator();
        int i = 0;
        while (itr.hasNext()) {
            io.netty.handler.codec.http.cookie.Cookie nettyCookie = itr.next();
            Cookie servletCookie = new Cookie(nettyCookie.name(), nettyCookie.value());
//            servletCookie.setMaxAge(Ints.checkedCast(nettyCookie.maxAge()));
            if (nettyCookie.domain() != null) servletCookie.setDomain(nettyCookie.domain());
            if (nettyCookie.path() != null) servletCookie.setPath(nettyCookie.path());
            servletCookie.setHttpOnly(nettyCookie.isHttpOnly());
            this.cookies[i++] = servletCookie;
        }

        this.isCookieParsed = true;
    }

    /*====== Cookie 相关方法 结束 ======*/


    /*====== Header 相关方法 开始 ======*/
    private final HttpHeaders headers;

    @Override
    public long getDateHeader(String name) {
        String value = headers.get(name);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            try {
                return headers.getTimeMillis(name, -1L);
            } catch (Exception e2) {
                return -1L;
            }
        }
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

    private void checkAndParsePaths() {
        if (isPathsParsed) {
            return;
        }
        if (request.uri().startsWith(servletContext.getContextPath())) {
            String servletPath = request.uri().substring(servletContext.getContextPath().length());
            if (!servletPath.startsWith("/")) {
                servletPath = "/" + servletPath;
            }
            int queryInx = servletPath.indexOf('?');
            if (queryInx > -1) {
                this.queryString = servletPath.substring(queryInx + 1);
                servletPath = servletPath.substring(0, queryInx);
            }
            this.servletPath = servletPath;
            this.requestUri = this.servletContext.getContextPath() + servletPath; //TODO 加上pathInfo
        } else {
            this.servletPath = "";
            this.requestUri = "";
        }
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
        checkAndParsePaths();
        StringBuffer url = new StringBuffer();
        url.append("http:").append("//")
                .append(request.headers().get(HttpHeaderNames.HOST))
                .append(getRequestURI());
        return url;
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
        if (sessionId != null) {
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
            if (null != curSession) {
                this.isURLSession = true;
                recoverySession(curSession);
            }
        }
    }

    /**
     * @return 从URL解析到的SessionID
     */
    private String getSessionIdFromUrl() {
        StringBuilder u = new StringBuilder(request.uri());
        int sessionStart = u.toString().indexOf(";" + NettyHttpSession.SESSION_REQUEST_PARAMETER_NAME + "=");
        if (sessionStart == -1) {
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
        Cookie[] cookies = getCookies();
        if (cookies == null) {
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
     *
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
    private final Map<String, String[]> paramMap = new HashMap<>(); //存储请求参数


    private void fillRequestParams() {
        if (isParameterParsed) {
            return;
        }
        Map<String, List<String>> multiMap = new HashMap<>();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri(), characterEncodingModel);
        Map<String, List<String>> params = queryStringDecoder.parameters();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            List<String> valueList = entry.getValue();
            multiMap.put(entry.getKey(), valueList);
        }
        if (httpPostRequestDecoder != null) {
            for (InterfaceHttpData bodyHttpData : httpPostRequestDecoder.getBodyHttpDatas()) {
                if (bodyHttpData instanceof Attribute) {
                    Attribute attribute = (Attribute) bodyHttpData;
                    try {
                        String value = attribute.getValue();
                        String name = attribute.getName();
                        multiMap.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        multiMap.forEach((s, strings) -> paramMap.put(s, strings.toArray(new String[0])));
        this.isParameterParsed = true;
    }

    @Override
    public String getParameter(String name) {
        fillRequestParams();
        String[] values = paramMap.get(name);
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        fillRequestParams();
        return Collections.enumeration(paramMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        fillRequestParams();
        return paramMap.get(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        fillRequestParams();
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

    private static final Splitter splitter = Splitter.on(':').omitEmptyStrings().trimResults();

    private static class HostAndPort {
        private String host;
        private int port;

        static HostAndPort of(String host, int port) {
            HostAndPort hostAndPort = new HostAndPort();
            hostAndPort.host = host;
            hostAndPort.port = port;
            return hostAndPort;
        }

        static HostAndPort of(String host) {
            return of(host, 80);
        }
    }

    private final java.util.function.Supplier<HostAndPort> hostAndPort =
            Suppliers.memoize(new Supplier<HostAndPort>() {
                @Override
                public HostAndPort get() {
                    String host = request.headers().get(HttpHeaderNames.HOST);
                    if (host == null) {
                        InetSocketAddress addr = (InetSocketAddress) ctx.channel().localAddress();
                        return HostAndPort.of(NetUtil.getHostname(addr), addr.getPort());
                    }
                    List<String> strings = splitter.splitToList(host);
                    return strings.size() == 1 ?
                            HostAndPort.of(host) :
                            HostAndPort.of(strings.get(0), Integer.parseInt(strings.get(1)));
                }
            });


    @Override
    public String getServerName() {
        return hostAndPort.get().host;
    }

    @Override
    public int getServerPort() {
        return hostAndPort.get().port;
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
        if (httpPostRequestDecoder == null || !httpPostRequestDecoder.isMultipart()) {
            return null;
        }
        return httpPostRequestDecoder.getBodyHttpDatas().stream()
                .map(NettyPart::of).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Part getPart(String name) throws IOException, IllegalStateException, ServletException {
        if (httpPostRequestDecoder == null || !httpPostRequestDecoder.isMultipart()) {
            return null;
        }
        return Optional.ofNullable(httpPostRequestDecoder.getBodyHttpData(name))
                .map(NettyPart::of)
                .orElse(null);
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


    @Override
    public ServletInputStream getInputStream() {
        return inputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(inputStream, getCharacterEncoding()));
    }

    @Override
    public int getContentLength() {
        return request.headers().getInt(HttpHeaderNames.CONTENT_LENGTH, -1);
    }

    @Override
    public long getContentLengthLong() {
        return getContentLength();
    }

    private String characterEncoding;

    private Charset characterEncodingModel = StandardCharsets.UTF_8;

    @Override
    public String getCharacterEncoding() {
        if (characterEncoding == null) {
            characterEncoding = parseCharacterEncoding();
        }

        characterEncodingModel = Charset.forName(characterEncoding);

        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) {
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
        if (path == null) {
            return null;
        }

        int fragmentPos = path.indexOf('#');
        if (fragmentPos > -1) {
            path = path.substring(0, fragmentPos);
        }

        // If the path is already context-relative, just pass it through
        if (path.startsWith("/")) {
            return servletContext.getRequestDispatcher(path);
        }
        String pathInfo = getPathInfo();
        String requestPath = null;

        if (pathInfo == null) {
            requestPath = servletPath;
        } else {
            requestPath = servletPath + pathInfo;
        }

        int pos = requestPath.lastIndexOf('/');
        String relative = null;

        if (pos >= 0) {
            relative = requestPath.substring(0, pos + 1) + path;
        } else {
            relative = requestPath + path;
        }
        return servletContext.getRequestDispatcher(relative);
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
