package com.motorental.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "rental_cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalCartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Min(value = 1, message = "Số ngày thuê phải ít nhất 1 ngày")
    @Column(name = "rental_days", nullable = false)
    private Integer rentalDays;

    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "price_per_day", nullable = false, precision = 18, scale = 2)
    private BigDecimal pricePerDay;

    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "total_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_cart_id", nullable = false)
    @ToString.Exclude
    private RentalCart rentalCart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @ToString.Exclude
    private Vehicle vehicle;

    @PrePersist
    @PreUpdate
    private void calculateFields() {
        if (startDate != null && endDate != null) {
            if (endDate.isBefore(startDate)) {
                throw new IllegalStateException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
            }
            long days = ChronoUnit.DAYS.between(startDate, endDate);
            this.rentalDays = (int) (days == 0 ? 1 : days);
        }

        if (rentalDays != null && pricePerDay != null) {
            totalPrice = pricePerDay.multiply(BigDecimal.valueOf(rentalDays));
        }
    }
}