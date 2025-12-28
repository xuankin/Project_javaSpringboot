package com.motorental.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên xe không được để trống")
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Biển số xe không được để trống")
    @Size(max = 50)
    @Column(name = "license_plate", unique = true, nullable = false, length = 50)
    private String licensePlate;

    @Column(columnDefinition = "TEXT") // Hỗ trợ mô tả dài
    private String description;

    @DecimalMin(value = "0.0", inclusive = false, message = "Giá thuê phải lớn hơn 0")
    @Column(name = "price_per_day", nullable = false, precision = 18, scale = 2)
    private BigDecimal pricePerDay;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @Size(max = 100)
    @Column(length = 100)
    private String brand;

    @Size(max = 100)
    @Column(length = 100)
    private String model;

    @Column(name = "year")
    private Integer year;

    @Size(max = 50)
    @Column(length = 50)
    private String color;

    @Column(name = "rental_count")
    @Builder.Default
    private Integer rentalCount = 0;

    // --- Relationships (Thêm Exclude) ---

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<VehicleImage> images = new HashSet<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<VehicleAvailability> availabilities = new HashSet<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<OrderDetail> orderDetails = new HashSet<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<RentalCartItem> cartItems = new HashSet<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Feedback> feedbacks = new HashSet<>();

    public void addImage(VehicleImage image) {
        images.add(image);
        image.setVehicle(this);
    }

    public void incrementRentalCount() {
        if (this.rentalCount == null) this.rentalCount = 0;
        this.rentalCount++;
    }

    public enum VehicleStatus {
        AVAILABLE("Available"),
        RENTED("Rented"),
        UNAVAILABLE("Unavailable"),
        MAINTENANCE("Maintenance");

        private final String displayName;
        VehicleStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}