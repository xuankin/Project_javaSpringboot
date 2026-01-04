package com.motorental.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderDto {
    private String notes;
    private String pickupLocation;

    // Thêm trường này để nhận phương thức thanh toán từ form
    private String paymentMethod;
}