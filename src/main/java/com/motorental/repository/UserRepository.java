package com.motorental.repository;

import com.motorental.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // STT 5: Login - Tìm user theo username
    Optional<User> findByUsername(String username);

    // STT 4: Register - Kiểm tra tồn tại
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // STT 12: Admin Dashboard - Đếm tổng user (dùng count() có sẵn của JpaRepository)
}