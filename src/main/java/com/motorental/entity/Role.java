package com.motorental.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "Tên vai trò không được để trống")
    @Size(max = 256)
    @Column(unique = true, nullable = false, length = 256)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @ManyToMany(mappedBy = "roles")
    @Builder.Default
    @JsonIgnore             // Ngắt vòng lặp JSON
    @ToString.Exclude       // Ngắt vòng lặp toString
    @EqualsAndHashCode.Exclude
    private Set<User> users = new HashSet<>();

    public Role(String name) {
        this.name = name;
    }
}