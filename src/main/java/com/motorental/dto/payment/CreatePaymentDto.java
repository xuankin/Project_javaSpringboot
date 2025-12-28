package com.motorental.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentDto {
    private Long orderId;
    private BigDecimal amount;
    private String bankCode;
    private String method;    // [FIX] Thêm
    private String returnUrl; // [FIX] Thêm
}