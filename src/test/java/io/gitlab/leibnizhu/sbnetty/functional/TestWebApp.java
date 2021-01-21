package io.gitlab.leibnizhu.sbnetty.functional;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 */
@Controller
@EnableAutoConfiguration(exclude = WebMvcAutoConfiguration.class)
@ComponentScan(basePackages = {"io.gitlab.leibnizhu.sbnetty"})
@EnableWebMvc
public class TestWebApp extends WebMvcConfigurerAdapter {
    private Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Hello, World!这是一条测试语句";

    @RequestMapping(value = "/plaintext", produces = "text/plain; chartset=UTF-8")
    @ResponseBody
    public String plaintext() {
        return MESSAGE;
    }

    @RequestMapping(value = "/async", produces = "text/plain")
    @ResponseBody
    public Callable<String> async() {
        return () -> MESSAGE;
    }

    @RequestMapping(value = "/json")
    @ResponseBody
    public Message json(@RequestParam String msg) {
        return new Message(MESSAGE + ". msg="+ msg);
    }

    @RequestMapping(value = "/session")
    @ResponseBody
    public Message session(@RequestParam String msg, HttpSession session, HttpServletRequest req) {
        if(session.getAttribute("aaa") == null){
            session.setAttribute("aaa", msg);
            log.info("sessionId={}, setAttribute aaa={}", session.getId(), msg);
        } else {
            String oldMsg = (String) session.getAttribute("aaa");
            log.info("sessionId={} is old Session, aaa={}, from Cookie:{}, from URL:{}, valid:{}", session.getId(), oldMsg, req.isRequestedSessionIdFromCookie(), req.isRequestedSessionIdFromURL(), req.isRequestedSessionIdValid());
        }
        return new Message(MESSAGE + ". msg="+ msg);
    }

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public String upload(HttpServletRequest request) throws IOException {
        ServletInputStream inputStream = request.getInputStream();
        int total = 0;
        while (true) {
            byte[] bytes = new byte[8192];
            int read = inputStream.read(bytes);
            if (read == -1) {
                break;
            }
            total += read;
        }
        return "Total bytes received: " + total;
    }

    @RequestMapping("/sleepy")
    @ResponseBody
    public String sleepy() throws InterruptedException {
        int millis = 500;
        Thread.sleep(millis);
        return "Yawn! I slept for " + millis + "ms";
    }

    @Bean
    public ServletRegistrationBean nullServletRegistration() {
        return new ServletRegistrationBean(new TestServlet(), "/null/*");
    }

    private class TestServlet extends HttpServlet{
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getOutputStream().print("Null Servlet Test");
            resp.getOutputStream().flush();
            resp.getOutputStream().close();
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(TestWebApp.class, args);
    }

/*    @Bean
    public HttpMessageConverter<String> getConverter(){
        StringHttpMessageConverter conv = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        conv.setWriteAcceptCharset(false);
//        List<MediaType> types = new ArrayList<>();
//        types.add(MediaType.TEXT_HTML);
//        types.add(MediaType.TEXT_PLAIN);
//        conv.setSupportedMediaTypes(types);
        return conv;
    }*/

/*
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        StringHttpMessageConverter converter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        converter.setWriteAcceptCharset(false);
        converters.add(converter);
    }
*/

    private static class Message {
        private final String message;

        public Message(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

}
