package org.kosa.apigatewayservice.filter;


import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthorizationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthorizationGatewayFilterFactory.Config> {

    public JwtAuthorizationGatewayFilterFactory() {
        super(Config.class);
    }

    public static class Config {
        // 필터에 전달할 설정 값이 있다면 여기에 정의
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authorizationHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            try {
                String token = authorizationHeader.replace("Bearer ", "");
                Jwts.parser()
                        .setSigningKey("verySecretKeyThatIsAtLeast32BytesLong1234".getBytes())
                        .parseClaimsJws(token);
            } catch (JwtException | IllegalArgumentException e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            return chain.filter(exchange);
        };
    }
}