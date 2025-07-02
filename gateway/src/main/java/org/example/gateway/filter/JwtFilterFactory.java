package org.example.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

@Component
public class JwtFilterFactory extends AbstractGatewayFilterFactory<JwtFilterFactory.Config> {

	// TODO 키 공유 방법 고려
	private static final String SECRET_KEY = "YOUR_SECRET_KEY_SHOULD_BE_AT_LEAST_256_BITS_LONG_";

	public JwtFilterFactory() {
		super(Config.class);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (	exchange, chain) -> {

			// JWT 토큰 유효성 검사
			String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				return unauthorized(exchange, "Missing or invalid Authorization header");
			}

			String token = authHeader.substring(7);

			try {
				SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
				Claims claims = Jwts.parser()
					.setSigningKey(key)
					.build()
					.parseClaimsJws(token)
					.getBody();

				// 유효하면 필요 시 claims에서 userId, role 등을 꺼내 request attribute로 전달 가능
				ServerHttpRequest mutatedRequest = exchange.getRequest()
					.mutate()
					.header("userId", claims.getSubject())
					.build();

				//TODO 중복 JWT 검사 성능 개선을 위해 게이트웨이 서명 추가

				return chain.filter(exchange.mutate().request(mutatedRequest).build());
			} catch (Exception e) {
				return unauthorized(exchange, "Invalid JWT: " + e.getMessage());
			}

		};
	}

	private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
		exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);

		exchange.getResponse().getHeaders().add("Content-Type", "application/json");

		try {
			ObjectMapper mapper = new ObjectMapper();
			String body = mapper.writeValueAsString(Map.of(
				"status", 401,
				"message", message
			));

			DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
			DataBuffer dataBuffer = bufferFactory.wrap(body.getBytes(StandardCharsets.UTF_8));

			return exchange.getResponse().writeWith(Mono.just(dataBuffer));
		} catch (Exception e) {
			return exchange.getResponse().setComplete();
		}
	}


	public static class Config {
		//Put the configuration properties for your filter here
	}

}