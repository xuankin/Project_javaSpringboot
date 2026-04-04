package com.motorental.config;

import com.motorental.entity.UserLocationHistory;
import com.motorental.repository.UserLocationHistoryRepository;
import com.motorental.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UserLocationHistoryRepository locationHistoryRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // --- SAVE LOCATION ---
        String latStr = request.getParameter("latitude");
        String lngStr = request.getParameter("longitude");

        if (latStr != null && lngStr != null && !latStr.isEmpty() && !lngStr.isEmpty()) {
            try {
                Double latitude = Double.parseDouble(latStr);
                Double longitude = Double.parseDouble(lngStr);
                String username = authentication.getName();

                userRepository.findByUsername(username).ifPresent(user -> {
                    UserLocationHistory history = UserLocationHistory.builder()
                            .user(user)
                            .latitude(latitude)
                            .longitude(longitude)
                            .build();
                    locationHistoryRepository.save(history);
                });
            } catch (NumberFormatException e) {
                // Ignore invalid coordinates
            }
        }
        // ---------------------

        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        // Kiểm tra role ADMIN (Spring Security thường lưu là ROLE_ADMIN)
        if (roles.contains("ROLE_ADMIN")) {
            response.sendRedirect("/admin/dashboard");
        } else {
            response.sendRedirect("/");
        }
    }
}