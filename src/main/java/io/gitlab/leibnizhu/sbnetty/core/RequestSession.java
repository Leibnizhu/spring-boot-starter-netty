package io.gitlab.leibnizhu.sbnetty.core;

import io.gitlab.leibnizhu.sbnetty.request.HttpRequestInputStream;
import io.gitlab.leibnizhu.sbnetty.request.NettyHttpServletRequest;
import io.gitlab.leibnizhu.sbnetty.response.NettyHttpServletResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.ReferenceCountUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;


public class RequestSession {
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private final HttpRequest nettyRequest;
    private final NettyHttpServletRequest servletRequest;
    private final NettyHttpServletResponse servletResponse;

    private final HttpRequestInputStream inputStream;
    private HttpPostRequestDecoder httpPostRequestDecoder;


    public NettyHttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public NettyHttpServletResponse getServletResponse() {
        return servletResponse;
    }

    public RequestSession(ChannelHandlerContext ctx, HttpRequest request, NettyContext servletContext) {
        this.nettyRequest = request;
        this.inputStream = new HttpRequestInputStream();

        if (request.method().equals(HttpMethod.POST)) {
            String contentType = String.valueOf(request.headers().get(HttpHeaderNames.CONTENT_TYPE));
            if (HttpPostRequestDecoder.isMultipart(request)
                    || contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")
            ) {
                // 对于x-www-form-urlencoded和multipart，使用netty的解析器来处理
                httpPostRequestDecoder = new HttpPostRequestDecoder(request);
            }
        }
        this.servletRequest = new NettyHttpServletRequest(ctx, servletContext, request, httpPostRequestDecoder, inputStream);
        this.servletResponse = new NettyHttpServletResponse(ctx, servletContext, servletRequest);
    }


    public void destroy() {
        if (!destroyed.compareAndSet(false, true)) {
            return;
        }

        closeQuietly(inputStream);
        closeQuietly(servletResponse.getOutputStream());

        if (httpPostRequestDecoder != null) {
            httpPostRequestDecoder.destroy();
            httpPostRequestDecoder = null;
        }
        ReferenceCountUtil.release(nettyRequest);
    }

    public static void closeQuietly(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (final IOException ignore) {

            }
        }
    }

    public void offer(HttpContent msg) {
        if (destroyed.get()) {
            return;
        }

        if (httpPostRequestDecoder != null) {
            httpPostRequestDecoder.offer(msg);
        } else {
            inputStream.offer(msg);
        }
    }
}
