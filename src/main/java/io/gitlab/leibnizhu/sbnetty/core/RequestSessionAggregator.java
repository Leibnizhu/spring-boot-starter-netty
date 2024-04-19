package io.gitlab.leibnizhu.sbnetty.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

/**
 * 不使用HttpObjectAggregator，因为在处理文件上传时，HttpObjectAggregator将文件内容存储在内存中，
 * 在处理大文件上传时，会有内存溢出风险
 */
public class RequestSessionAggregator extends SimpleChannelInboundHandler<HttpObject> {

    private RequestSession requestSession;
    private final NettyContext servletContext;

    public RequestSessionAggregator(NettyContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = ReferenceCountUtil.retain((HttpRequest) msg);
            requestSession = new RequestSession(ctx, request, servletContext);
            if (HttpUtil.is100ContinueExpected(request)) { //请求头包含Expect: 100-continue
                ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
            }
        } else if (msg instanceof HttpContent) {
            requestSession.offer((HttpContent) msg);
            if (msg instanceof LastHttpContent) {
                ctx.fireChannelRead(requestSession);
                requestSession = null;
            }
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (requestSession != null) {
            requestSession.destroy();
        }
    }
}