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
 * @author Leibniz
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
