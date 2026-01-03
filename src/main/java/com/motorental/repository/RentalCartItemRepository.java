package com.motorental.repository;

import com.motorental.entity.RentalCartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // <-- 1. Nhớ import cái này
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RentalCartItemRepository extends JpaRepository<RentalCartItem, Long> {

    // STT 7: Xóa sạch giỏ hàng
    @Modifying
    @Transactional
    @Query("DELETE FROM RentalCartItem i WHERE i.rentalCart.id = :cartId")
    void deleteByRentalCartId(@Param("cartId") Long cartId); // <-- 2. Thêm @Param("cartId") vào đây
}