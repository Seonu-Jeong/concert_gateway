package org.example.gateway.filter;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WaitingCheckFilterFactory extends AbstractGatewayFilterFactory<WaitingCheckFilterFactory.Config> {

	private final WebClient webClient;

	public WaitingCheckFilterFactory(WebClient.Builder webClientBuilder) {
		super(Config.class);
		this.webClient = webClientBuilder.build();
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			return webClient.get()
				.uri(config.getCheckApiUrl())
				.retrieve()
				.bodyToMono(Boolean.class)
				.flatMap(shouldRedirect -> {
					if (Boolean.TRUE.equals(shouldRedirect)) {
						ServerHttpResponse response = exchange.getResponse();
						response.setStatusCode(HttpStatus.SEE_OTHER);
						response.getHeaders().setLocation(URI.create(config.getRedirectUrl()));
						return response.setComplete();
					} else {
						return chain.filter(exchange);
					}
				});
		};
	}

	public static class Config {
		/**
		 * 외부 검증 API URL
		 */
		private String checkApiUrl;

		/**
		 * True일 때 리디렉션할 URL
		 */
		private String redirectUrl;

		public String getCheckApiUrl() {
			return checkApiUrl;
		}

		public void setCheckApiUrl(String checkApiUrl) {
			this.checkApiUrl = checkApiUrl;
		}

		public String getRedirectUrl() {
			return redirectUrl;
		}

		public void setRedirectUrl(String redirectUrl) {
			this.redirectUrl = redirectUrl;
		}
	}
}