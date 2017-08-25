package io.gitlab.leibnizhu.sbnetty.utils;

import javax.servlet.Servlet;

/**
 * @author Leibniz.Hu
 * Created on 2017-08-25 12:28.
 */
public class MappingData {

    int contextSlashCount = 0;
    Servlet servlet = null;
    String servletName;

    String contextPath ;
    String requestPath ;
    String wrapperPath ;
    String pathInfo ;
    String redirectPath ;

    public void recycle() {
        contextSlashCount = 0;
        servlet = null;
        contextPath = null;
        requestPath = null;
        wrapperPath = null;
        pathInfo = null;
        redirectPath = null;
    }

}
