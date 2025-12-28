package com.motorental.repository;

import com.motorental.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    // STT 4: Register - Gán quyền mặc định (ROLE_USER)
    Optional<Role> findByName(String name);
}