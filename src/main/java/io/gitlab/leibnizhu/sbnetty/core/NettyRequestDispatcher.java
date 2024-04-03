package io.gitlab.leibnizhu.sbnetty.core;

import io.gitlab.leibnizhu.sbnetty.request.NettyHttpServletRequest;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * 分发器，除了传统的forward和include，把正常的Servlet调用也放在这里dispatch()方法
 */
public class NettyRequestDispatcher implements RequestDispatcher {
    private final FilterChain filterChain;
    private final String path;

    NettyRequestDispatcher(FilterChain filterChain, String path) {
        this.filterChain = filterChain;
        this.path = path;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        request.setAttribute(NettyHttpServletRequest.DISPATCHER_TYPE, DispatcherType.FORWARD);
        NettyHttpServletRequest servletRequest = (NettyHttpServletRequest) request;
        filterChain.doFilter(new HttpServletRequestWrapper(servletRequest) {
            @Override
            public String getRequestURI() {
                return path;
            }
        }, response);
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        request.setAttribute(NettyHttpServletRequest.DISPATCHER_TYPE, DispatcherType.INCLUDE);
        // TODO implement
    }

    void dispatch(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        request.setAttribute(NettyHttpServletRequest.DISPATCHER_TYPE, DispatcherType.ASYNC);
        filterChain.doFilter(request, response);
    }
}
