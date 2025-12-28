package com.motorental.config;

import com.motorental.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationSuccessHandler successHandler; // Inject handler vừa tạo

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. PUBLIC: Cho phép truy cập tự do
                        .requestMatchers(
                                "/",
                                "/home",
                                "/register",
                                "/login",
                                "/vehicles",
                                "/vehicles/**",
                                "/css/**", "/js/**", "/images/**", "/uploads/**", "/webjars/**"
                        ).permitAll()

                        // 2. ADMIN: Chỉ Admin mới vào được trang quản trị
                        // QUAN TRỌNG: Sửa đường dẫn từ "/templates/admin/**" thành "/admin/**"
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 3. USER: Các trang cần đăng nhập
                        .requestMatchers(
                                "/cart/**",
                                "/orders/**",
                                "/my-orders",
                                "/payments/**",
                                "/profile",
                                "/feedbacks/add"
                        ).authenticated()

                        // 4. Các request còn lại phải xác thực
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        // QUAN TRỌNG: Thay defaultSuccessUrl bằng successHandler để phân quyền chuyển hướng
                        .successHandler(successHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")
                );

        return http.build();
    }
}