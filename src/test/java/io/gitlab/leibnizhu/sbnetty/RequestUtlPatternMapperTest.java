package io.gitlab.leibnizhu.sbnetty;

import io.gitlab.leibnizhu.sbnetty.utils.RequestUrlPatternMapper;
import org.junit.Test;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

/**
 * @author Leibniz.Hu
 * Created on 2017-08-27 10:25.
 */
public class RequestUtlPatternMapperTest {
    private RequestUrlPatternMapper mapper = new RequestUrlPatternMapper("/netty");
    private String[] cases = new String[]{
            "/netty/",
            "/netty/aaa",
            "/netty/jsonaa/a/dfd/json",
            "/netty/json/",
            "/netty/json/bggg",
            "/netty/json/a/dfd/json",
            "/netty/json/b/dfd/json",
            "/netty/json/a/b/dfd/json",
            "/netty/json/b/a/dfd/json"
    };
    private double[] times = new double[cases.length];
    private int totalCount = 100000;

    @Test
    public void testAll() throws ServletException {
        addPattern();

        for (int i = 0; i < totalCount; i++) {
            testMatchs();
        }
        testMatchs(true);
    }

    private void testMatchs() {
        testMatchs(false);
    }

    private void testMatchs(boolean showLog) {
        for(int i = 0; i< cases.length; i++){
            testCase(cases[i], i, showLog);
        }
    }

    private void testCase(String s, int order) {
        testCase(s, order, false);
    }

    private void testCase(String s, int order, boolean showLog) {
        long t1 = System.nanoTime();
        String result = mapper.getServletNameByRequestURI(s);
        times[order] = times[order] + (System.nanoTime() - t1);
        if (showLog) {
            System.out.println(s + " : " + result + ". Average cost time(ns): " + times[order] / totalCount + " ns");
        }
    }

    private void addPattern() throws ServletException {
        Servlet obj = new DispatcherServlet();
        mapper.addServlet("/json/a/*", obj, "a");
        mapper.addServlet("/json/a/b/*", obj, "ab");
        mapper.addServlet("/json/b/*", obj, "b");
        mapper.addServlet("/json/*", obj, "jsonsall");
        mapper.addServlet("/json/", obj, "jsonroot");
        mapper.addServlet("/", obj, "root");
    }
}
