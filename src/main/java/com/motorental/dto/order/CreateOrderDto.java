package com.motorental.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDto {

    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod; // "CASH", "VNPAY", etc.

    private String notes;

    // --- Các trường Optional dùng cho chức năng "Thuê ngay" (không qua giỏ hàng) ---
    private Long vehicleId;
    private LocalDate startDate;
    private LocalDate endDate;
}