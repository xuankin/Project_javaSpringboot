package com.motorental.repository;

import com.motorental.entity.RentalCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RentalCartRepository extends JpaRepository<RentalCart, Long> {
    // STT 6: Lấy giỏ hàng của user hiện tại
    Optional<RentalCart> findByUserId(String userId);
}