package org.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class RateLimitFailRedirectGatewayFilterFactory
	extends AbstractGatewayFilterFactory<RateLimitFailRedirectGatewayFilterFactory.Config> {

	public RateLimitFailRedirectGatewayFilterFactory() {
		super(Config.class);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) ->
			chain.filter(exchange).then(Mono.defer(() -> {
				ServerHttpResponse response = exchange.getResponse();
				HttpStatus status = (HttpStatus)response.getStatusCode();

				if (status != null && status.value() == 429) {
					response.setStatusCode(HttpStatus.SEE_OTHER); // 303 redirect
					response.getHeaders().set("Location", config.getRedirectUrl());
				}
				return Mono.empty();
			}));
	}

	public static class Config {
		private String redirectUrl;

		public String getRedirectUrl() {
			return redirectUrl;
		}

		public void setRedirectUrl(String redirectUrl) {
			this.redirectUrl = redirectUrl;
		}
	}
}