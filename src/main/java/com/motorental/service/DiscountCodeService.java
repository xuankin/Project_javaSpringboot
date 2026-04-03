package com.motorental.service;

import com.motorental.model.DiscountCode;
import com.motorental.repository.DiscountCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DiscountCodeService {

    private final DiscountCodeRepository discountCodeRepository;

    public List<DiscountCode> getAllDiscountCodes() {
        return discountCodeRepository.findAll();
    }

    public DiscountCode createDiscountCode(DiscountCode discountCode) {
        // You can add more validation here if needed
        return discountCodeRepository.save(discountCode);
    }

    public Optional<DiscountCode> findByCode(String code) {
        return discountCodeRepository.findByCode(code);
    }

    public Optional<DiscountCode> findById(Long id) {
        return discountCodeRepository.findById(id);
    }

    public boolean isCodeValid(String code) {
        Optional<DiscountCode> discountOpt = findByCode(code);
        if (discountOpt.isEmpty()) {
            return false;
        }
        DiscountCode discountCode = discountOpt.get();
        return discountCode.isActive() && !discountCode.getExpiryDate().isBefore(LocalDate.now());
    }

    public void deleteDiscountCode(Long id) {
        discountCodeRepository.deleteById(id);
    }
}
