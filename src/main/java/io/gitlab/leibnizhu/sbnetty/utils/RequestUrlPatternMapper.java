package io.gitlab.leibnizhu.sbnetty.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

/**
 * 保存，计算URL-pattern与请求路径的匹配关系
 *
 * @author Leibniz.Hu
 * Created on 2017-08-25 11:32.
 */
public class RequestUrlPatternMapper {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private UrlPatternContext urlPatternContext;
    private String contextPath;

    public RequestUrlPatternMapper(String contextPath) {
        this.urlPatternContext = new UrlPatternContext();
        this.contextPath = contextPath;
    }

    /**
     * 增加映射关系
     *
     * @param urlPattern  urlPattern
     * @param servlet     servlet对象
     * @param servletName servletName
     */
    public void addServlet(String urlPattern, Servlet servlet, String servletName) throws ServletException {
        if (urlPattern.endsWith("/*")) {
            // 路径匹配
            String pattern = urlPattern.substring(0, urlPattern.length() - 1);
            for (MappedServlet ms : urlPatternContext.wildcardServlets) {
                if (ms.pattern.equals(pattern)) {
                    throw new ServletException("URL Pattern('" + urlPattern + "') already exists!");
                }
            }
            MappedServlet newServlet = new MappedServlet(pattern, servlet, servletName);
            urlPatternContext.wildcardServlets.add(newServlet);
            urlPatternContext.wildcardServlets.sort((o1, o2) -> o2.pattern.compareTo(o1.pattern));
            log.debug("Curretn Wildcard URL Pattern List = " + Arrays.toString(urlPatternContext.wildcardServlets.toArray()));
        } else if (urlPattern.startsWith("*.")) {
            // 扩展名匹配
            String pattern = urlPattern.substring(2);
            if (urlPatternContext.extensionServlets.get(pattern) != null) {
                throw new ServletException("URL Pattern('" + urlPattern + "') already exists!");
            }
            MappedServlet newServlet = new MappedServlet(pattern, servlet, servletName);
            urlPatternContext.extensionServlets.put(pattern, newServlet);
            log.debug("Curretn Extension URL Pattern List = " + Arrays.toString(urlPatternContext.extensionServlets.keySet().toArray()));
        } else if (urlPattern.equals("/")) {
            // Default资源匹配
            if (urlPatternContext.defaultServlet != null) {
                throw new ServletException("URL Pattern('" + urlPattern + "') already exists!");
            }
            urlPatternContext.defaultServlet = new MappedServlet("", servlet, servletName);
        } else {
            // 精确匹配
            String pattern;
            if (urlPattern.length() == 0) {
                pattern = "/";
            } else {
                pattern = urlPattern;
            }
            if (urlPatternContext.exactServlets.get(pattern) != null) {
                throw new ServletException("URL Pattern('" + urlPattern + "') already exists!");
            }
            MappedServlet newServlet = new MappedServlet(pattern, servlet, servletName);
            urlPatternContext.exactServlets.put(pattern, newServlet);
            log.debug("Curretn Exact URL Pattern List = " + Arrays.toString(urlPatternContext.exactServlets.keySet().toArray()));
        }
    }

    /**
     * 删除映射关系
     *
     * @param urlPattern
     */
    public void removeServlet(String urlPattern) {
        if (urlPattern.endsWith("/*")) {
            //路径匹配
            String pattern = urlPattern.substring(0, urlPattern.length() - 2);
            urlPatternContext.wildcardServlets.removeIf(mappedServlet -> mappedServlet.pattern.equals(pattern));
        } else if (urlPattern.startsWith("*.")) {
            // 扩展名匹配
            String pattern = urlPattern.substring(2);
            urlPatternContext.extensionServlets.remove(pattern);
        } else if (urlPattern.equals("/")) {
            // Default资源匹配
            urlPatternContext.defaultServlet = null;
        } else {
            // 精确匹配
            String pattern;
            if (urlPattern.length() == 0) {
                pattern = "/";
            } else {
                pattern = urlPattern;
            }
            urlPatternContext.exactServlets.remove(pattern);
        }
    }

    public String getServletNameByRequestURI(String absoluteUri) {
        MappingData mappingData = new MappingData();
        try {
            matchRequestPath(absoluteUri, mappingData);
        } catch (IOException e) {
            log.error("Throwing exception when getting Servlet Name by request URI, maybe cause by lacking of buffer size.", e);
        }
        return mappingData.servletName;
    }

    /**
     * Wrapper mapping.
     *
     * @throws IOException buffer大小不足
     */
    private void matchRequestPath(String absolutePath, MappingData mappingData) throws IOException {
        // 处理ContextPath，获取访问的相对URI
        boolean noServletPath = absolutePath.equals(contextPath) || absolutePath.equals(contextPath + "/");
        if (!absolutePath.startsWith(contextPath)) {
            return;
        }
        String path = noServletPath ? "/" : absolutePath.substring(contextPath.length());
        //去掉查询字符串
        int queryInx = path.indexOf('?');
        if(queryInx > -1){
            path = path.substring(0, queryInx);
        }

        // 优先进行精确匹配
        internalMapExactWrapper(urlPatternContext.exactServlets, path, mappingData);

        // 然后进行路径匹配
        if (mappingData.servlet == null) {
            internalMapWildcardWrapper(urlPatternContext.wildcardServlets, path, mappingData);
            //TODO 暂不考虑JSP的处理
        }

        if (mappingData.servlet == null && noServletPath) {
            // 路径为空时，重定向到“/”
            mappingData.servlet = urlPatternContext.defaultServlet.object;
            mappingData.servletName = urlPatternContext.defaultServlet.servletName;
            return;
        }

        // 后缀名匹配
        if (mappingData.servlet == null) {
            internalMapExtensionWrapper(urlPatternContext.extensionServlets, path, mappingData);
        }

        //TODO 暂不考虑Welcome资源

        // Default Servlet
        if (mappingData.servlet == null) {
            if (urlPatternContext.defaultServlet != null) {
                mappingData.servlet = urlPatternContext.defaultServlet.object;
                mappingData.servletName = urlPatternContext.defaultServlet.servletName;
            }
            //TODO 暂不考虑请求静态目录资源
            if (path.charAt(path.length() - 1) != '/') {
            }
        }
    }


    /**
     * 精确匹配
     */
    private void internalMapExactWrapper(Map<String, MappedServlet> servlets, String path, MappingData mappingData) {
        MappedServlet servlet = servlets.get(path);
        if (servlet != null) {
            mappingData.servlet = servlet.object;
            mappingData.servletName = servlet.servletName;
        }
    }

    /**
     * 路径匹配
     */
    private void internalMapWildcardWrapper(List<MappedServlet> servlets, String path, MappingData mappingData) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        MappedServlet result = null;
        for (MappedServlet ms : servlets) {
            if (path.startsWith(ms.pattern)) {
                result = ms;
                break;
            }
        }
        if (result != null) {
            mappingData.servlet = result.object;
            mappingData.servletName = result.servletName;
        }
    }

    /**
     * 后缀名匹配
     */
    private void internalMapExtensionWrapper(Map<String, MappedServlet> servlets, String path, MappingData mappingData) {
        int dotInx = path.lastIndexOf('.');
        path = path.substring(dotInx + 1);
        MappedServlet servlet = servlets.get(path);
        if (servlet != null) {
            mappingData.servlet = servlet.object;
            mappingData.servletName = servlet.servletName;
        }
    }

    /*
     * 以下是用到的内部类
     */

    private class UrlPatternContext {
        MappedServlet defaultServlet = null; //默认Servlet
        Map<String, MappedServlet> exactServlets = new HashMap<>(); //精确匹配
        List<MappedServlet> wildcardServlets = new LinkedList<>(); //路径匹配
        Map<String, MappedServlet> extensionServlets = new HashMap<>(); //扩展名匹配
    }

    private class MappedServlet extends MapElement<Servlet> {
        @Override
        public String toString() {
            return pattern;
        }

        String servletName;

        MappedServlet(String name, Servlet servlet, String servletName) {
            super(name, servlet);
            this.servletName = servletName;
        }
    }

    private class MapElement<T> {
        final String pattern;
        final T object;

        MapElement(String pattern, T object) {
            this.pattern = pattern;
            this.object = object;
        }
    }

}
