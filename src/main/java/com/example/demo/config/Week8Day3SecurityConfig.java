package com.example.demo.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

@Configuration(proxyBeanMethods = false)
@Profile("spr-w8-d3")
public class Week8Day3SecurityConfig {

    // SPR-W8-D3-01: 学習用の2ユーザーをメモリに置き、起動時に渡されたパスワードを BCrypt でハッシュ化する。
    @Bean
    UserDetailsService trainingUsers(
            @Value("${app.security.training.user-password}") String userPassword,
            @Value("${app.security.training.admin-password}") String adminPassword) {
        Assert.hasText(userPassword, "SPR_W8_D3_USER_PASSWORD must be set");
        Assert.hasText(adminPassword, "SPR_W8_D3_ADMIN_PASSWORD must be set");
        Assert.isTrue(!userPassword.equals(adminPassword), "The training passwords must be different");

        var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(
                User.withUsername("training-user").password(encoder.encode(userPassword)).roles("USER").build(),
                User.withUsername("training-admin").password(encoder.encode(adminPassword)).roles("ADMIN").build());
    }

    // SPR-W8-D3-02: あいさつは公開し、社員情報の参照は両ロール、登録・変更・削除は管理者に許可する。
    @Bean
    SecurityFilterChain week8Day3SecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/hello").permitAll()
                        .requestMatchers(HttpMethod.GET, "/employees", "/employees/{id}").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/employees").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/employees/{id}").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/employees/{id}").hasRole("ADMIN")
                        .anyRequest().denyAll())
                // SPR-W8-D3-03: 保護対象では未認証は 401、認証済みで権限不足の場合は 403。
                // 更新リクエストは既定の CSRF 保護も通過する必要があり、トークン不足も 403 になる。
                .httpBasic(withDefaults())
                .build();
    }
}
