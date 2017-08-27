package io.gitlab.leibnizhu.sbnetty.registration;

import io.gitlab.leibnizhu.sbnetty.core.NettyContext;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;

/**
 * Filter的注册器，一个Filter对应一个注册器
 */
public class NettyFilterRegistration extends AbstractNettyRegistration implements FilterRegistration.Dynamic {
    private volatile boolean initialised;
    private Filter filter;
    private Collection<String> urlPatternMappings = new LinkedList<>();

    public NettyFilterRegistration(NettyContext context, String filterName, String className, Filter filter) {
        super(filterName, className, context);
        this.filter = filter;
    }

    public Filter getFilter() throws ServletException {
        if (!initialised) {
            synchronized (this) {
                if (!initialised) {
                    if (null == filter) {
                        try {
                            filter = (Filter) Class.forName(getClassName()).newInstance(); //反射获取实例
                        } catch (Exception e) {
                            throw new ServletException(e);
                        }
                    }
                    filter.init(this); //初始化Filter
                    initialised = true;
                }
            }
        }
        return filter;
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {
        NettyContext context = getNettyContext();
        for (String urlPattern : urlPatterns) {
            context.addFilterMapping(dispatcherTypes, isMatchAfter, urlPattern);
        }
        urlPatternMappings.addAll(Arrays.asList(urlPatterns));
    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return urlPatternMappings;
    }

    //Servlet相关的不管
    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {

    }

    @Override
    public Collection<String> getServletNameMappings() {
        return null;
    }
}
