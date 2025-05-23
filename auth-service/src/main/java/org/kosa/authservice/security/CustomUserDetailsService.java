package org.kosa.authservice.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final WebClient webClient;

    public CustomUserDetailsService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://localhost:8081").build(); // user-service 주소
    }

    @Override
    public UserDetails loadUserByUsername(String userid) throws UsernameNotFoundException {
        UserDto user = webClient.get()
                .uri("/api/users/{userid}", userid)
                .retrieve()
                .bodyToMono(UserDto.class)
                .block();

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserid())
                .password(user.getPasswd())
                .roles(user.getRole() != null ? user.getRole() : "USER")
                .build();
    }
}