package io.gitlab.leibnizhu.sbnetty.core;

import com.google.common.base.StandardSystemProperty;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;

import java.net.InetSocketAddress;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Netty servle容器
 * 处理请求，返回响应
 * 目前不支持JSP，考虑到SpringBoot多用于REST+前后端分离，也不会去实现JSP
 *
 * @author Leibniz
 */
public class NettyContainer implements EmbeddedServletContainer {
    private final Log log = LogFactory.getLog(getClass());

    private final InetSocketAddress address; //监听端口地址
    private final NettyContext servletContext; //Context

    //Netty所需的线程池，分别用于接收/监听请求以及处理请求读写
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private DefaultEventExecutorGroup servletExecutor;

    public NettyContainer(InetSocketAddress address, NettyContext servletContext) {
        this.address = address;
        this.servletContext = servletContext;
    }

    @Override
    public void start() throws EmbeddedServletContainerException {
        servletContext.setInitialised(false);

        ServerBootstrap sb = new ServerBootstrap();
        //根据不同系统初始化对应的EventLoopGroup
        if ("Linux".equals(StandardSystemProperty.OS_NAME.value())) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();//不带参数，线程数传入0,实际解析为 Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2));
            sb.channel(EpollServerSocketChannel.class)
                    .group(bossGroup, workerGroup)
                    .option(EpollChannelOption.TCP_CORK, true);
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            sb.channel(NioServerSocketChannel.class)
                    .group(bossGroup, workerGroup);
        }
        sb.option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 100);
        log.info("Bootstrap configuration: " + sb.toString());

        servletExecutor = new DefaultEventExecutorGroup(50);
        sb.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast("codec", new HttpServerCodec(4096, 8192, 8192, false)); //HTTP编码解码Handler
                p.addLast("servletInput", new ServletContentHandler(servletContext)); //处理请求，读入数据，生成Request和Response对象
                p.addLast(checkNotNull(servletExecutor), "filterChain", new RequestDispatcherHandler(servletContext)); //
            }
        });

        servletContext.setInitialised(true);

        ChannelFuture future = sb.bind(address).awaitUninterruptibly();
        Throwable cause = future.cause();
        if (null != cause) {
            throw new EmbeddedServletContainerException("Could not start Netty server", cause);
        }
        log.info(servletContext.getServerInfo() + " started on port: " + getPort());
    }

    /**
     * 优雅地关闭各种资源
     * @throws EmbeddedServletContainerException
     */
    @Override
    public void stop() throws EmbeddedServletContainerException {
        try {
            if (null != bossGroup) {
                bossGroup.shutdownGracefully().await();
            }
            if (null != workerGroup) {
                workerGroup.shutdownGracefully().await();
            }
            if (null != servletExecutor) {
                servletExecutor.shutdownGracefully().await();
            }
        } catch (InterruptedException e) {
            throw new EmbeddedServletContainerException("Container stop interrupted", e);
        }
    }

    @Override
    public int getPort() {
        return address.getPort();
    }
}
