package io.gitlab.leibnizhu.sbnetty.bootstrap;

import io.netty.bootstrap.Bootstrap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * 配置加载内置Netty容器的工厂类Bean。
 * 最早是直接将EmbeddedNettyFactory加@Component注解，这样集成在任何环境中都会加载，可能引起端口冲突。
 * 所以通过这个配置类，配置在当前上下文缺少EmbeddedServletContainerFactory接口实现类时（即缺少内置Servlet容器），加载EmbeddedNettyFactory
 * 这样SpringBoot项目在引入这个maven依赖，并且排除了内置tomcat依赖、且没引入其他servlet容器（如jetty）时，就可以通过工厂类加载并启动netty容器了。
 *
 * @author Leibniz 2017-08-24
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
public class EmbeddedNettyAutoConfiguration {
    @Configuration
    @ConditionalOnClass({Bootstrap.class})
    @ConditionalOnMissingBean(value = EmbeddedServletContainerFactory.class, search = SearchStrategy.CURRENT)
    public static class EmbeddedNetty {
        @Bean
        public EmbeddedNettyFactory embeddedNettyFactory() {
            return new EmbeddedNettyFactory();
        }
    }
}
