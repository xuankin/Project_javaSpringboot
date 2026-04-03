package com.motorental.controller;

import com.motorental.dto.discount.DiscountValidationResponse;
import com.motorental.model.DiscountCode;
import com.motorental.service.DiscountCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountApiController {

    private final DiscountCodeService discountCodeService;

    @GetMapping("/validate")
    public ResponseEntity<?> validateDiscountCode(@RequestParam("code") String code) {
        if (!discountCodeService.isCodeValid(code)) {
            return ResponseEntity.badRequest().body(new DiscountValidationResponse(false, "Mã giảm giá không hợp lệ hoặc đã hết hạn.", 0));
        }

        Optional<DiscountCode> discountOpt = discountCodeService.findByCode(code);
        if (discountOpt.isPresent()) {
            DiscountCode discount = discountOpt.get();
            return ResponseEntity.ok(new DiscountValidationResponse(true, "Áp dụng mã thành công!", discount.getDiscountPercentage()));
        }

        return ResponseEntity.badRequest().body(new DiscountValidationResponse(false, "Lỗi không xác định.", 0));
    }
}
