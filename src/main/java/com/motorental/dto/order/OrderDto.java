package com.motorental.dto.order;

import com.motorental.dto.payment.PaymentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private String orderCode;
    private String status; // PENDING, CONFIRMED...
    private BigDecimal totalPrice;
    private String notes;
    private LocalDateTime createdAt;

    // User info (tránh trả về full UserDto)
    private String userName;
    private String userEmail;

    private List<OrderDetailDto> orderDetails;
    private PaymentDto payment;
}