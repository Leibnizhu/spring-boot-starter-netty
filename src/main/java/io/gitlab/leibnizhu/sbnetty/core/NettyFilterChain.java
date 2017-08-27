package io.gitlab.leibnizhu.sbnetty.core;

import javax.servlet.*;
import java.io.IOException;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * 实现过滤器链
 */
public class NettyFilterChain implements FilterChain {
    /**
     * 考虑到每个请求只有一个线程处理，而且ServletContext在每次请求时都会new 一个SimpleFilterChain对象
     * 所以这里把过滤器链的Iterator作为FilterChain的私有变量，没有线程安全问题
     */
    private final Iterator<Filter> filterIterator;
    private final Servlet servlet;

    public NettyFilterChain(Servlet servlet, Iterable<Filter> filters) throws ServletException {
        this.filterIterator = checkNotNull(filters).iterator();
        this.servlet = checkNotNull(servlet);
    }

    /**
     * 每个Filter在处理完请求之后调用FilterChain的这个方法。
     * 这时候应该找到下一个Filter，调用其doFilter()方法。
     * 如果没有下一个了，应该调用servlet的service()方法了
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (filterIterator.hasNext()) {
            Filter filter = filterIterator.next();
            filter.doFilter(request, response, this);
        } else {
            servlet.service(request, response);
        }
    }
}
