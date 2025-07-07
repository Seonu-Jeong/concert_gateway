package org.example.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import reactor.core.publisher.Mono;

@Configuration
public class KeyResolverConfig {

	@Bean
	@Primary
	KeyResolver pathKeyResolver() {
		return exchange -> Mono.just(exchange.getRequest().getPath().toString());
	}
}