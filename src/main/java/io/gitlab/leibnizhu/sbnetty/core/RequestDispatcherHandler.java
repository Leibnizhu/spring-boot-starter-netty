package io.gitlab.leibnizhu.sbnetty.core;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 读入请求数据时，对请求URI获取分发器，找不到返回404错误.
 * 找到则调用FilterChain进行业务逻辑，最后关闭输出流
 */
@ChannelHandler.Sharable
class RequestDispatcherHandler extends SimpleChannelInboundHandler<RequestSession> {
    private final Log logger = LogFactory.getLog(getClass());
    private final NettyContext context;

    RequestDispatcherHandler(NettyContext context) {
        this.context = checkNotNull(context);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RequestSession requestSession) throws Exception {
        String requestURI = requestSession.getServletRequest().getRequestURI();
        try {
            NettyRequestDispatcher dispatcher = (NettyRequestDispatcher) context.getRequestDispatcher(requestURI);
            if (dispatcher == null) {
                requestSession.getServletResponse().sendError(404);
                return;
            }
            dispatcher.dispatch(requestSession.getServletRequest(), requestSession.getServletResponse());
        } finally {
            if (!requestSession.getServletRequest().isAsyncStarted()) {
                requestSession.destroy();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception caught during request", cause);
        ctx.close();
    }
}
