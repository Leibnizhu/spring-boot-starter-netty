package io.gitlab.leibnizhu.sbnetty.response;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.net.MediaType;
import io.gitlab.leibnizhu.sbnetty.core.NettyContext;
import io.gitlab.leibnizhu.sbnetty.request.NettyHttpServletRequest;
import io.gitlab.leibnizhu.sbnetty.session.NettyHttpSession;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.concurrent.FastThreadLocal;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;

/**
 * Http响应对象
 */
public class NettyHttpServletResponse implements HttpServletResponse {
    /**
     * SimpleDateFormat非线程安全，为了节省内存提高效率，把他放在ThreadLocal里
     * 用于设置HTTP响应头的时间信息
     */
    private static final FastThreadLocal<DateFormat> FORMAT = new FastThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            DateFormat df = new SimpleDateFormat("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.ENGLISH);
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            return df;
        }
    };

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final String DEFAULT_CHARACTER_ENCODING = Charsets.UTF_8.name();

    private final NettyContext servletContext;
    private NettyHttpServletRequest request;

    private HttpResponse response;

    private HttpResponseOutputStream outputStream;
    private boolean usingOutputStream;
    private PrintWriter writer;
    private boolean committed;
    private List<Cookie> cookies;
    private String contentType;
    private String characterEncoding = DEFAULT_CHARACTER_ENCODING;
    private Locale locale;

    /**
     * 构造方法
     *
     * @param ctx            Netty的Context
     * @param servletContext ServletContext
     * @param response       Netty自带的http响应对象，初始化为200
     */
    public NettyHttpServletResponse(ChannelHandlerContext ctx, NettyContext servletContext, HttpResponse response) {
        this.servletContext = servletContext;
        this.response = response;
        this.outputStream = new HttpResponseOutputStream(ctx, this);
        cookies = new ArrayList<>();
    }

    /**
     * 设置基本的请求头
     */
    public HttpResponse getNettyResponse() {
        if (committed) {
            return response;
        }
        committed = true;
        HttpHeaders headers = response.headers();
        if (null != contentType) {
            String value = null == characterEncoding ? contentType : contentType + "; charset=" + characterEncoding; //Content Type 响应头的内容
            headers.set(HttpHeaderNames.CONTENT_TYPE, value);
        }
        CharSequence date = getFormattedDate();
        headers.set(HttpHeaderNames.DATE, date); // 时间日期响应头
        headers.set(HttpHeaderNames.SERVER, servletContext.getServerInfo()); //服务器信息响应头

        // cookies处理
        long curTime = System.currentTimeMillis(); //用于根据maxAge计算Cookie的Expires
        //先处理Session ，如果是新Session需要通过Cookie写入
        if (request.getSession().isNew()) {
            String sessionCookieStr = NettyHttpSession.SESSION_COOKIE_NAME + "=" + request.getRequestedSessionId() + "; path=/; Expires="
                    + FORMAT.get().format(new Date(curTime + 1800000)) + "; domain=" + request.getServerName();
            headers.add(HttpHeaderNames.SET_COOKIE, sessionCookieStr);
        }
        //其他业务或框架设置的cookie，逐条写入到响应头去
        for (Cookie cookie : cookies) {
            StringBuilder sb = new StringBuilder();
            sb.append(cookie.getName()).append("=").append(cookie.getValue())
                    .append("; Expires=").append(FORMAT.get().format(new Date(cookie.getMaxAge())));
            if (cookie.getPath() != null) sb.append("; path=").append(cookie.getPath());
            if (cookie.getDomain() != null) sb.append("; domain=").append(cookie.getDomain());
            headers.add(HttpHeaderNames.SET_COOKIE, sb.toString());
        }
        return response;
    }

    public NettyHttpServletResponse setRequest(NettyHttpServletRequest request) {
        this.request = request;
        return this;
    }

    /**
     * @return 线程安全的获取当前时间格式化后的字符串
     */
    @VisibleForTesting
    private CharSequence getFormattedDate() {
        return new AsciiString(FORMAT.get().format(new Date()));
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return response.headers().contains(name);
    }

    //TODO 还没想明白怎么在服务器判断客户端是否支持Cookie，所以先不写了
    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return null;
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        checkNotCommitted();
        response.setStatus(new HttpResponseStatus(sc, msg));
    }

    @Override
    public void sendError(int sc) throws IOException {
        checkNotCommitted();
        response.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        checkNotCommitted();
        response.setStatus(HttpResponseStatus.FOUND);
        response.headers().set("Location", location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        response.headers().set(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {
        response.headers().add(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
        if (name == null || name.length() == 0 || value == null) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        if (setHeaderField(name, value)) {
            return;
        }
        response.headers().set(name, value);
    }

    private boolean setHeaderField(String name, String value) {
        char c = name.charAt(0);//减少判断的时间，提高效率
        if ('C' == c || 'c' == c) {
            if (HttpHeaderNames.CONTENT_TYPE.contentEqualsIgnoreCase(name)) {
                setContentType(value);
                return true;
            }
        }
        return false;
    }

    @Override
    public void addHeader(String name, String value) {
        if (name == null || name.length() == 0 || value == null) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        if (setHeaderField(name, value)) {
            return;
        }
        response.headers().add(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        if (name == null || name.length() == 0) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        response.headers().set(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (name == null || name.length() == 0) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        response.headers().add(name, value);
    }

    @Override
    public void setContentType(String type) {
        if (isCommitted()) {
            return;
        }
        if (hasWriter()) {
            return;
        }
        if (null == type) {
            contentType = null;
            return;
        }
        MediaType mediaType = MediaType.parse(type);
        Optional<Charset> charset = mediaType.charset();
        if (charset.isPresent()) {
            setCharacterEncoding(charset.get().name());
        }
        contentType = mediaType.type() + '/' + mediaType.subtype();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setStatus(int sc) {
        response.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    @Deprecated
    public void setStatus(int sc, String sm) {
        response.setStatus(new HttpResponseStatus(sc, sm));
    }

    @Override
    public int getStatus() {
        return response.status().code();
    }

    @Override
    public String getHeader(String name) {
        return response.headers().get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return response.headers().getAll(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return response.headers().names();
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    //Writer和OutputStream不能同时使用

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        checkState(!hasWriter(), "getWriter has already been called for this response");
        usingOutputStream = true;
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        checkState(!usingOutputStream, "getOutputStream has already been called for this response");
        if (!hasWriter()) {
            writer = new PrintWriter(outputStream);
        }
        return writer;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        if (hasWriter()) {
            return;
        }
        characterEncoding = charset;
    }

    private boolean hasWriter() {
        return null != writer;
    }

    @Override
    public void setContentLength(int len) {
        HttpUtil.setContentLength(response, len);
    }

    @Override
    public void setContentLengthLong(long len) {
        HttpUtil.setContentLength(response, len);
    }

    @Override
    public void setBufferSize(int size) {
        checkNotCommitted();
        outputStream.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
        return outputStream.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        checkNotCommitted();
        outputStream.flush();
    }

    @Override
    public void resetBuffer() {
        checkNotCommitted();
        outputStream.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    private void checkNotCommitted() {
        checkState(!committed, "Cannot perform this operation after response has been committed");
    }

    @Override
    public void reset() {
        resetBuffer();
        usingOutputStream = false;
        writer = null;
    }

    @Override
    public void setLocale(Locale loc) {
        locale = loc;
    }

    @Override
    public Locale getLocale() {
        return null == locale ? DEFAULT_LOCALE : locale;
    }
}
