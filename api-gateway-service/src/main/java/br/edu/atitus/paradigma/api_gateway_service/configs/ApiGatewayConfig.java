package br.edu.atitus.paradigma.api_gateway_service.configs;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayConfig {
	@Bean
	RouteLocator getGatewayRoute(RouteLocatorBuilder builder) {
		return builder.routes()
				.route(route -> route
						.path("/cambio-service/**")
						.uri("lb://cambio-service"))
				.route(route -> route
					.path("/produto-service/**")
					.uri("lb://produto-service"))
					.build();
	}
}
