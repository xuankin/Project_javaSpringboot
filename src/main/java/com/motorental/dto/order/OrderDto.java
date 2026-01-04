package com.motorental.dto.order;

import com.motorental.dto.payment.PaymentDto;
import com.motorental.entity.RentalOrder.OrderStatus; // Import Enum
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private String orderCode;

    // Đổi từ String sang OrderStatus để dùng được .name() trong HTML
    private OrderStatus status;

    private BigDecimal totalPrice;
    private String notes;
    private LocalDateTime createdAt;

    // --- THÊM TRƯỜNG MỚI ---
    private String pickupLocation;
    // -----------------------

    // --- CÁC TRƯỜNG BỔ SUNG ĐỂ SỬA LỖI MY-ORDERS.HTML ---
    // (Lấy từ xe đầu tiên trong đơn hàng để hiển thị danh sách)
    private Long vehicleId;
    private String vehicleName;
    private LocalDate startDate;
    private LocalDate endDate;
    // ---------------------------------------------------

    private String userName;
    private String userEmail;

    private List<OrderDetailDto> orderDetails;
    private PaymentDto payment;
}