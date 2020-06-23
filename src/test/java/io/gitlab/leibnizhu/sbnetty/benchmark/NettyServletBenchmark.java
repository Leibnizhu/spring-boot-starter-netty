package io.gitlab.leibnizhu.sbnetty.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Leibniz.Hu
 * Created on 2017-09-01 21:12.
 */
@BenchmarkMode(Mode.Throughput)
//@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class NettyServletBenchmark {
    @Benchmark
    @Warmup(iterations = 10)
    @Measurement(iterations = 20)
    public void plaintext() {
        getUrl("http://localhost:9999/netty/plaintext", false);
    }

    @Benchmark
    @Warmup(iterations = 10)
    @Measurement(iterations = 20)
    public void json() {
        getUrl("http://localhost:9999/netty/json?msg=1", false);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + NettyServletBenchmark.class.getSimpleName() + ".*")
                .forks(1)
                .build();
        new Runner(opt).run();
    }

    private String getUrl(String url, boolean read) {
        BufferedReader br = null;
        InputStream is = null;
        StringBuilder sbuf = new StringBuilder();
        try {
            URL reqURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) reqURL.openConnection(); // 进行连接，但是实际上getrequest要在下一句的connection.getInputStream() 函数中才会真正发到服务器
            connection.setDoOutput(false);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(200);
            connection.setDoInput(true);
            connection.connect();
            if (read) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    sbuf.append(line).append("\n");
                }
            } else {
                is = connection.getInputStream();

            }
        } catch (IOException e) {
            System.out.println("连接服务器'" + url + "'时发生错误：" + e.getMessage());
        } finally {
            try {
                if (null != br) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sbuf.toString();
    }
}
