package com.motorental.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rental_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 50)
    @Column(name = "order_code", unique = true, nullable = false, length = 50)
    private String orderCode;

    @DecimalMin(value = "0.0")
    @Column(name = "total_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // --- THÊM TRƯỜNG MỚI: NƠI NHẬN XE ---
    @Column(name = "pickup_location", length = 255)
    private String pickupLocation;
    // -------------------------------------

    @Column(columnDefinition = "TEXT")
    private String notes;

    // --- Relationships ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @OneToMany(mappedBy = "rentalOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<OrderDetail> orderDetails = new HashSet<>();

    @OneToOne(mappedBy = "rentalOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Payment payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<VehicleAvailability> availabilities = new HashSet<>();

    // Helper methods
    public void addOrderDetail(OrderDetail detail) {
        orderDetails.add(detail);
        detail.setRentalOrder(this);
    }

    public void removeOrderDetail(OrderDetail detail) {
        orderDetails.remove(detail);
        detail.setRentalOrder(null);
    }

    @PrePersist
    private void generateOrderCode() {
        if (orderCode == null) {
            orderCode = "ORD" + System.currentTimeMillis();
        }
    }

    public enum OrderStatus {
        PENDING("Pending"),
        CONFIRMED("Confirmed"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");

        private final String displayName;
        OrderStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}