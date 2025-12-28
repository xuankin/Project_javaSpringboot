package com.motorental.config;

import com.motorental.entity.Role;
import com.motorental.entity.User;
import com.motorental.repository.RoleRepository;
import com.motorental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Tạo Roles
        createRoleIfNotFound("ROLE_USER");
        Role adminRole = createRoleIfNotFound("ROLE_ADMIN");

        // 2. Tạo tài khoản Admin mặc định (Username: admin / Pass: admin123)
        // Sửa lỗi: Đổi "templates/admin" thành "admin"
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin"); // Username chuẩn
            admin.setEmail("admin@motorental.com");
            admin.setFullName("Administrator");
            admin.setPhoneNumber("0909000111");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setIsActive(true);
            admin.setRoles(new HashSet<>(Collections.singletonList(adminRole)));

            userRepository.save(admin);
            System.out.println(">>> Đã tạo tài khoản Admin mặc định: admin / admin123");
        }
    }

    private Role createRoleIfNotFound(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(name);
                    return roleRepository.save(role);
                });
    }
}