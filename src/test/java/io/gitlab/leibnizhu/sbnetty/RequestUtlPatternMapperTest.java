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

    @Test
    public void testAll() throws ServletException {
        addPattern();
        testMatchs();
    }

    private void testMatchs() {
        testCase("/netty/json/a/dfd/json");
        testCase("/netty/json/b/dfd/json");
        testCase("/netty/json/a/b/dfd/json");
        testCase("/netty/json/b/a/dfd/json");
        testCase("/netty/json/");
        testCase("/netty/json/bggg");
        testCase("/netty/aaa");
        testCase("/netty/");
    }

    private void testCase(String s) {
        System.out.println(s + " : " + mapper.getServletNameByRequestURI(s));
    }

    private void addPattern() throws ServletException {
        Servlet obj = new DispatcherServlet();
        mapper.addWrapper("/json/a/*", obj, "a");
        mapper.addWrapper("/json/a/b/*", obj, "ab");
        mapper.addWrapper("/json/b/*", obj, "b");
        mapper.addWrapper("/json/*", obj, "jsonsall");
        mapper.addWrapper("/json/", obj, "jsonroot");
        mapper.addWrapper("/", obj, "root");
    }
}
