package io.gitlab.leibnizhu.sbnetty.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;

/**
 * 保存，计算URL-pattern与请求路径的匹配关系
 *
 * @author Leibniz.Hu
 * Created on 2017-08-25 11:32.
 */
public class RequestUrlPatternMapper {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private ContextVersion contextVersion;
    private String contextPath;

    public RequestUrlPatternMapper(String contextPath) {
        this.contextVersion = new ContextVersion("1.0", 0, new String[0]);
        this.contextPath = contextPath;
    }

    /**
     * 增加映射关系
     *
     * @param urlPattern urlPattern
     * @param servlet servlet对象
     * @param servletName servletName
     * @author Leibniz
     */
    public void addWrapper(String urlPattern, Servlet servlet, String servletName) {
        if (urlPattern.endsWith("/*")) {
            // 路径匹配
            String name = urlPattern.substring(0, urlPattern.length() - 2);
            MappedWrapper newWrapper = new MappedWrapper(name, servlet, servletName);
            MappedWrapper[] oldWrappers = contextVersion.wildcardWrappers;
            MappedWrapper[] newWrappers = new MappedWrapper[oldWrappers.length + 1];
            if (insertMap(oldWrappers, newWrappers, newWrapper)) {
                contextVersion.wildcardWrappers = newWrappers;
                int slashCount = slashCount(newWrapper.name);
                if (slashCount > contextVersion.nesting) {
                    contextVersion.nesting = slashCount;
                }
            }
        } else if (urlPattern.startsWith("*.")) {
            // 扩展名匹配
            String name = urlPattern.substring(2);
            MappedWrapper newWrapper = new MappedWrapper(name, servlet, servletName);
            MappedWrapper[] oldWrappers = contextVersion.extensionWrappers;
            MappedWrapper[] newWrappers =
                    new MappedWrapper[oldWrappers.length + 1];
            if (insertMap(oldWrappers, newWrappers, newWrapper)) {
                contextVersion.extensionWrappers = newWrappers;
            }
        } else if (urlPattern.equals("/")) {
            // Default资源匹配
            MappedWrapper newWrapper = new MappedWrapper("", servlet, servletName);
            contextVersion.defaultWrapper = newWrapper;
        } else {
            // 精确匹配
            final String name;
            if (urlPattern.length() == 0) {
                name = "/";
            } else {
                name = urlPattern;
            }
            MappedWrapper newWrapper = new MappedWrapper(name, servlet, servletName);
            MappedWrapper[] oldWrappers = contextVersion.exactWrappers;
            MappedWrapper[] newWrappers = new MappedWrapper[oldWrappers.length + 1];
            if (insertMap(oldWrappers, newWrappers, newWrapper)) {
                contextVersion.exactWrappers = newWrappers;
            }
        }
    }

    /**
     * 删除映射关系
     *
     * @param urlPattern
     */
    public void removeWrapper(String urlPattern) {
        log.debug("mapper.removeWrapper", contextVersion.name, urlPattern);
        if (urlPattern.endsWith("/*")) {
            //路径匹配
            String name = urlPattern.substring(0, urlPattern.length() - 2);
            MappedWrapper[] oldWrappers = contextVersion.wildcardWrappers;
            if (oldWrappers.length == 0) {
                return;
            }
            MappedWrapper[] newWrappers =
                    new MappedWrapper[oldWrappers.length - 1];
            if (removeMap(oldWrappers, newWrappers, name)) {
                // Recalculate nesting
                contextVersion.nesting = 0;
                for (int i = 0; i < newWrappers.length; i++) {
                    int slashCount = slashCount(newWrappers[i].name);
                    if (slashCount > contextVersion.nesting) {
                        contextVersion.nesting = slashCount;
                    }
                }
                contextVersion.wildcardWrappers = newWrappers;
            }
        } else if (urlPattern.startsWith("*.")) {
            // 扩展名匹配
            String name = urlPattern.substring(2);
            MappedWrapper[] oldWrappers = contextVersion.extensionWrappers;
            if (oldWrappers.length == 0) {
                return;
            }
            MappedWrapper[] newWrappers =
                    new MappedWrapper[oldWrappers.length - 1];
            if (removeMap(oldWrappers, newWrappers, name)) {
                contextVersion.extensionWrappers = newWrappers;
            }
        } else if (urlPattern.equals("/")) {
            // Default资源匹配
            contextVersion.defaultWrapper = null;
        } else {
            // 精确匹配
            String name;
            if (urlPattern.length() == 0) {
                name = "/";
            } else {
                name = urlPattern;
            }
            MappedWrapper[] oldWrappers = contextVersion.exactWrappers;
            if (oldWrappers.length == 0) {
                return;
            }
            MappedWrapper[] newWrappers =
                    new MappedWrapper[oldWrappers.length - 1];
            if (removeMap(oldWrappers, newWrappers, name)) {
                contextVersion.exactWrappers = newWrappers;
            }
        }
    }

    public String getServletNameByRequestURI(String absoluteUri){
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

        // 优先进行精确匹配
        MappedWrapper[] exactWrappers = contextVersion.exactWrappers;
        internalMapExactWrapper(exactWrappers, path, mappingData);

        // 然后进行路径匹配
        MappedWrapper[] wildcardWrappers = contextVersion.wildcardWrappers;
        if (mappingData.servlet == null) {
            internalMapWildcardWrapper(wildcardWrappers, contextVersion.nesting, path, mappingData);
            //TODO 暂不考虑JSP的处理
        }

        if (mappingData.servlet == null && noServletPath) {
            // 路径为空时，重定向到“/”
            mappingData.redirectPath = contextPath;
            return;
        }

        // 后缀名匹配
        MappedWrapper[] extensionWrappers = contextVersion.extensionWrappers;
        if (mappingData.servlet == null) {
            internalMapExtensionWrapper(extensionWrappers, path, mappingData);
        }

        //TODO 暂不考虑Welcome资源

        // Default Servlet
        if (mappingData.servlet == null) {
            if (contextVersion.defaultWrapper != null) {
                mappingData.servlet = contextVersion.defaultWrapper.object;
                mappingData.servletName = contextVersion.defaultWrapper.servletName;
                mappingData.requestPath = path;
                mappingData.wrapperPath = path;
            }
            //TODO 暂不考虑请求静态目录资源
            if (path.charAt(path.length() - 1) != '/') {
            }
        }
    }


    /**
     * 精确匹配
     */
    private void internalMapExactWrapper(MappedWrapper[] servlets, String path, MappingData mappingData) {
        MappedWrapper servlet = exactFind(servlets, path);
        if (servlet != null) {
            mappingData.requestPath = servlet.name;
            mappingData.servlet = servlet.object;
            mappingData.servletName = servlet.servletName;
            if (path.equals("/")) {
                // Special handling for Context Root mapped servlet
                mappingData.pathInfo = "/";
                mappingData.wrapperPath = "";
                mappingData.contextPath = "";
            } else {
                mappingData.wrapperPath = servlet.name;
            }
        }
    }


    /**
     * 路径匹配
     */
    private void internalMapWildcardWrapper(MappedWrapper[] servlets, int nesting, String path, MappingData mappingData) {
        int pathEnd = path.length();
        int lastSlash = -1;
        int length = -1;
        int pos = find(servlets, path);
        if (pos != -1) {
            boolean found = false;
            while (pos >= 0) {
                if (path.startsWith(servlets[pos].name)) {
                    length = servlets[pos].name.length();
                    if (path.length() == length) {
                        found = true;
                        break;
                    } else if (path.startsWith("/")) {
                        found = true;
                        break;
                    }
                }
                if (lastSlash == -1) {
                    lastSlash = nthSlash(path, nesting + 1);
                } else {
                    lastSlash = lastSlash(path);
                }
                path = path.substring(0, lastSlash);
                pos = find(servlets, path);
            }
            path = path.substring(0, pathEnd);
            if (found) {
                mappingData.wrapperPath = servlets[pos].name;
                if (path.length() > length) {
                    mappingData.pathInfo = path.substring(length);
                }
                mappingData.requestPath = path;
                mappingData.servlet = servlets[pos].object;
                mappingData.servletName = servlets[pos].servletName;
            }
        }
    }

    /**
     * 后缀名匹配
     */
    private void internalMapExtensionWrapper(MappedWrapper[] servlets, String path, MappingData mappingData) {
        char[] buf  = new char[path.length()];
        path.getChars(0, path.length(), buf, 0);
        int pathEnd = path.length();
        int servletPath = 0;
        int slash = -1;
        for (int i = pathEnd - 1; i >= servletPath; i--) {
            if (buf[i] == '/') {
                slash = i;
                break;
            }
        }
        if (slash >= 0) {
            int period = -1;
            for (int i = pathEnd - 1; i > slash; i--) {
                if (buf[i] == '.') {
                    period = i;
                    break;
                }
            }
            if (period >= 0) {
                path = path.substring(period + 1, pathEnd);
                MappedWrapper servlet = exactFind(servlets, path);
                if (servlet != null) {
                    mappingData.wrapperPath = new String(buf, servletPath, pathEnd - servletPath);
                    mappingData.requestPath = new String(buf, servletPath, pathEnd - servletPath);
                    mappingData.servlet = servlet.object;
                    mappingData.servletName = servlet.servletName;
                }
            }
        }
    }

    /*
     * 以下是用到的内部类
     */

    private class ContextVersion extends MapElement<Object> {
        final int slashCount;
        String[] welcomeResources;
        MappedWrapper defaultWrapper = null;
        MappedWrapper[] exactWrappers = new MappedWrapper[0];
        MappedWrapper[] wildcardWrappers = new MappedWrapper[0];
        MappedWrapper[] extensionWrappers = new MappedWrapper[0];
        int nesting = 0;

        ContextVersion(String version, int slashCount, String[] welcomeResources) {
            super(version, null);
            this.slashCount = slashCount;
            this.welcomeResources = welcomeResources;
        }
    }

    private class MappedWrapper extends MapElement<Servlet> {
        String servletName;
        MappedWrapper(String name, Servlet servlet, String servletName) {
            super(name, servlet);
            this.servletName = servletName;
        }
    }

    private class MapElement<T> {
        final String name;
        final T object;

        MapElement(String name, T object) {
            this.name = name;
            this.object = object;
        }
    }

    /*
     * 以下是匹配用到的一些私有方法
     */

    private static <T> boolean insertMap(MapElement<T>[] oldMap, MapElement<T>[] newMap, MapElement<T> newElement) {
        int pos = find(oldMap, newElement.name);
        if ((pos != -1) && (newElement.name.equals(oldMap[pos].name))) {
            return false;
        }
        System.arraycopy(oldMap, 0, newMap, 0, pos + 1);
        newMap[pos + 1] = newElement;
        System.arraycopy
                (oldMap, pos + 1, newMap, pos + 2, oldMap.length - pos - 1);
        return true;
    }

    private static <T> boolean removeMap(MapElement<T>[] oldMap, MapElement<T>[] newMap, String name) {
        int pos = find(oldMap, name);
        if ((pos != -1) && (name.equals(oldMap[pos].name))) {
            System.arraycopy(oldMap, 0, newMap, 0, pos);
            System.arraycopy(oldMap, pos + 1, newMap, pos,
                    oldMap.length - pos - 1);
            return true;
        }
        return false;
    }

    private static <T> int find(MapElement<T>[] map, String name) {
        int start = 0;
        int end = name.length();
        int a = 0;
        int b = map.length - 1;

        // Special cases: -1 and 0
        if (b == -1) {
            return -1;
        }

        if (compare(name, start, end, map[0].name) < 0) {
            return -1;
        }
        if (b == 0) {
            return 0;
        }

        int i;
        while (true) {
            i = (b + a) / 2;
            int result = compare(name, start, end, map[i].name);
            if (result == 1) {
                a = i;
            } else if (result == 0) {
                return i;
            } else {
                b = i;
            }
            if ((b - a) == 1) {
                int result2 = compare(name, start, end, map[b].name);
                if (result2 < 0) {
                    return a;
                } else {
                    return b;
                }
            }
        }
    }

    private static int compare(String name, int start, int end, String compareTo) {
        int result = 0;
        char[] c = new char[name.length()];
        name.getChars(0, name.length(), c, 0);
        int len = compareTo.length();
        if ((end - start) < len) {
            len = end - start;
        }
        for (int i = 0; (i < len) && (result == 0); i++) {
            if (c[i + start] > compareTo.charAt(i)) {
                result = 1;
            } else if (c[i + start] < compareTo.charAt(i)) {
                result = -1;
            }
        }
        if (result == 0) {
            if (compareTo.length() > (end - start)) {
                result = -1;
            } else if (compareTo.length() < (end - start)) {
                result = 1;
            }
        }
        return result;
    }

    private static <T, E extends MapElement<T>> E exactFind(E[] map, String name) {
        for(E e : map){
            if(e.name.equals(name)){
                return e;
            }
        }
        return null;
    }

    /**
     * 统计"/"个数
     */
    private static int slashCount(String name) {
        int pos = -1;
        int count = 0;
        while ((pos = name.indexOf('/', pos + 1)) != -1) {
            count++;
        }
        return count;
    }

    /**
     * 最后一个“/”的下标
     */
    private static int lastSlash(String name) {
        char[] c = new char[name.length()];
        name.getChars(0, name.length(), c, 0);
        int end = name.length();
        int start = 0;
        int pos = end;

        while (pos > start) {
            if (c[--pos] == '/') {
                break;
            }
        }
        return (pos);
    }

    /**
     * 第n个“/”的下标
     */
    private static int nthSlash(String name, int n) {
        char[] c = new char[name.length()];
        name.getChars(0, name.length(), c, 0);
        int end = name.length();
        int pos = 0;
        int count = 0;

        while (pos < end) {
            if ((c[pos++] == '/') && ((++count) == n)) {
                pos--;
                break;
            }
        }
        return (pos);
    }
}