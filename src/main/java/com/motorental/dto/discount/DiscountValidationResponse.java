package com.motorental.dto.discount;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscountValidationResponse {
    private boolean valid;
    private String message;
    private int discountPercentage;
}
