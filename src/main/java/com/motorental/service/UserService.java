package com.motorental.service;

import com.motorental.dto.user.UserDto;
import com.motorental.dto.user.UserRegistrationDto;
import com.motorental.entity.Role;
import com.motorental.entity.User;
import com.motorental.repository.RoleRepository;
import com.motorental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors; // Cần thêm cái này để map Role

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Transactional
    public void registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new RuntimeException("Email đã tồn tại!");
        }

        User user = new User();
        user.setFullName(registrationDto.getFullName());
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPhoneNumber(registrationDto.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        user.setIsActive(true);

        // Mặc định role USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Lỗi hệ thống: Không tìm thấy Role mặc định."));
        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));

        userRepository.save(user);
    }

    // SỬA: Map thủ công để lấy được danh sách tên Role chính xác
    public UserDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return UserDto.builder()
                .id(user.getId() != null ? user.getId().toString() : null)
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                // Chuyển đổi Set<Role> thành Set<String> (tên role)
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Transactional
    public void updateUserProfile(String username, UserDto userDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Chỉ cập nhật các trường cho phép
        user.setFullName(userDto.getFullName());
        user.setPhoneNumber(userDto.getPhoneNumber());

        // Không cập nhật username, email, password ở đây

        userRepository.save(user);
    }
}