package com.motorental.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String fullName;
    private String username;
    private String email;
    private String phoneNumber;
    private Boolean isActive;
    private Set<String> roles; // Chỉ trả về tên role (ví dụ: ["ROLE_USER"])
}