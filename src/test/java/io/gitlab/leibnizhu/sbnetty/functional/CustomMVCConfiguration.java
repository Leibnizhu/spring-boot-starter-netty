package io.gitlab.leibnizhu.sbnetty.functional;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Leibniz.Hu
 * Created on 2017-08-31 21:13.
 */
@Configuration
public class CustomMVCConfiguration extends WebMvcConfigurerAdapter {

    @Bean
    public HttpMessageConverter<String> responseBodyConverter() {
        StringHttpMessageConverter conv = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        conv.setWriteAcceptCharset(false);
        return conv;
    }

    @Bean
    FastJsonHttpMessageConverter getFastJsonConv() {
        FastJsonHttpMessageConverter conv = new FastJsonHttpMessageConverter();

        FastJsonConfig oFastJsonConfig = new FastJsonConfig();
        oFastJsonConfig.setSerializerFeatures(
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat
        );
        conv.setFastJsonConfig(oFastJsonConfig);

        List<MediaType> types = new ArrayList<>();
        types.add(MediaType.APPLICATION_JSON_UTF8);
        types.add(MediaType.APPLICATION_JSON);
        conv.setSupportedMediaTypes(types);
        return conv;
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        super.configureMessageConverters(converters);
        converters.add(responseBodyConverter());
        converters.add(getFastJsonConv());
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false);
    }
}
