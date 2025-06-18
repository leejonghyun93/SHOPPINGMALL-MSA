package org.kosa.apigatewayservice.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        GatewayFilter filter = new OrderedGatewayFilter((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            if (config.isPreLogger()) {
                log.info("🚀 [{}] Request: {} {} from {} | Headers: {}",
                        config.getBaseMessage(),
                        request.getMethod(),
                        request.getURI().getPath(),
                        request.getRemoteAddress(),
                        request.getHeaders().getFirst("User-Agent"));
            }

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                if (config.isPostLogger()) {
                    log.info("✅ [{}] Response: {} for {} {} | Content-Type: {}",
                            config.getBaseMessage(),
                            response.getStatusCode(),
                            request.getMethod(),
                            request.getURI().getPath(),
                            response.getHeaders().getFirst("Content-Type"));
                }
            }));
        }, Ordered.LOWEST_PRECEDENCE);

        return filter;
    }

    @Data
    public static class Config {
        private String baseMessage = "Gateway";
        private boolean preLogger = true;
        private boolean postLogger = true;
    }
}