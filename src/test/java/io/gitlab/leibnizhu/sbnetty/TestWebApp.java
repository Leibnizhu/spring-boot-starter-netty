package io.gitlab.leibnizhu.sbnetty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.Callable;

/**
 */
@Controller
@EnableAutoConfiguration(exclude = WebMvcAutoConfiguration.class)
@ComponentScan
@EnableWebMvc
public class TestWebApp {
    private static final String MESSAGE = "Hello, World!这是一条测试语句";

    @RequestMapping(value = "/plaintext", produces = "text/plain")
    @ResponseBody
    public String plaintext() {
        return MESSAGE;
    }

    @RequestMapping(value = "/async", produces = "text/plain")
    @ResponseBody
    public Callable<String> async() {
        return () -> MESSAGE;
    }

    @RequestMapping(value = "/json", produces = "application/json")
    @ResponseBody
    public Message json() {
        return new Message(MESSAGE);
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
