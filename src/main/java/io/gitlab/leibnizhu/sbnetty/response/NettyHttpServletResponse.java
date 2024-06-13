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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final NettyHttpServletRequest httpServletRequest;

    private final HttpResponse response;

    private final HttpResponseOutputStream outputStream;
    private boolean usingOutputStream;
    private PrintWriter writer;
    private final List<Cookie> cookies;
    private String contentType;
    private String characterEncoding = DEFAULT_CHARACTER_ENCODING;
    private Locale locale;
    private final AtomicBoolean hasWriteHeader = new AtomicBoolean(false);
    private final ChannelHandlerContext ctx;


    public NettyHttpServletResponse(ChannelHandlerContext ctx, NettyContext servletContext, NettyHttpServletRequest httpServletRequest) {
        this.ctx = ctx;
        this.servletContext = servletContext;
        this.httpServletRequest = httpServletRequest;
        this.outputStream = new HttpResponseOutputStream(ctx, this);
        this.cookies = new ArrayList<>();

        this.response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, false);
        HttpUtil.setKeepAlive(response, httpServletRequest.isKeepAlive());
    }



    private boolean useChunked = false;

    public void ensureResponseHeader(boolean hasBody) {
        if (!hasWriteHeader.compareAndSet(false, true)) {
            return;
        }
        if (!HttpUtil.isContentLengthSet(response)) {
            if (hasBody) {
                // 在开始写body的时候，都还没有contentLength出现，那么这个请求应该是trunk的
                response.headers().set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
                useChunked = true;
            } else {
                HttpUtil.setContentLength(response, 0L);
            }
        }
        HttpHeaders headers = response.headers();
        if (null != contentType) {
            String value = null == characterEncoding ? contentType : contentType + "; charset=" + characterEncoding; //Content Type 响应头的内容
            headers.set(HttpHeaderNames.CONTENT_TYPE, value);
        }
        CharSequence date = getFormattedDate();
        headers.set(HttpHeaderNames.DATE, date); // 时间日期响应头
        headers.set(HttpHeaderNames.SERVER, servletContext.getServerInfo()); //服务器信息响应头


        HttpSession session = httpServletRequest.getSession(false);
        if (session != null && session.isNew()) {
            String sessionCookieStr = NettyHttpSession.SESSION_COOKIE_NAME +
                    "=" + httpServletRequest.getRequestedSessionId() +
                    "; path=/; domain=" +
                    httpServletRequest.getServerName();
            headers.add(HttpHeaderNames.SET_COOKIE, sessionCookieStr);
        }

        for (Cookie cookie : cookies) {
            StringBuilder sb = new StringBuilder();
            sb.append(cookie.getName()).append("=").append(cookie.getValue())
                    .append("; max-Age=").append(cookie.getMaxAge());
            if (cookie.getPath() != null) sb.append("; path=").append(cookie.getPath());
            if (cookie.getDomain() != null) sb.append("; domain=").append(cookie.getDomain());
            headers.add(HttpHeaderNames.SET_COOKIE, sb.toString());
        }
        ctx.write(response);
    }

    public boolean isKeepAlive() {
        return httpServletRequest.isKeepAlive();
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

    @Override
    public String encodeURL(String url) {
        if (!httpServletRequest.isRequestedSessionIdFromCookie()) {
            //来自Cookie的Session ID,则客户端肯定支持Cookie，无需重写URL
            return url;
        }
        return url + ";" + NettyHttpSession.SESSION_REQUEST_PARAMETER_NAME + "=" + httpServletRequest.getRequestedSessionId();
    }

    @Override
    public String encodeRedirectURL(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(String url) {
        return encodeRedirectURL(url);
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
        if (name == null || name.isEmpty() || value == null) {
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
        if (name == null || name.isEmpty() || value == null) {
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
        if (name == null || name.isEmpty()) {
            return;
        }
        if (isCommitted()) {
            return;
        }
        response.headers().set(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        if (name == null || name.isEmpty()) {
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
    public ServletOutputStream getOutputStream() {
        checkState(!hasWriter(), "getWriter has already been called for this response");
        usingOutputStream = true;
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() {
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
        outputStream.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
        return outputStream.getBufferSize();
    }

    @Override
    public void flushBuffer() {
        outputStream.flush();
    }

    @Override
    public void resetBuffer() {
        outputStream.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return outputStream.isHasCommit();
    }

    private void checkNotCommitted() throws IOException {
        if (outputStream.isHasCommit()) {
            throw new IOException("Cannot perform this operation after response has been committed");
        }
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
