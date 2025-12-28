package com.motorental.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {
    private Long id;
    private BigDecimal amount;
    private String method; // CASH, VNPAY...
    private String paymentStatus; // PENDING, COMPLETED...
    private String transactionId;
    private LocalDateTime paymentDate;
    private String orderCode; // Reference ngược lại Order Code
}