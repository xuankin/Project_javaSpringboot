package com.motorental.repository;

import com.motorental.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    // Lấy chi tiết của 1 đơn hàng
    List<OrderDetail> findByRentalOrderId(Long rentalOrderId);

    // STT 11: Feedback - Kiểm tra user đã từng thuê xe này và hoàn thành chưa để cho phép đánh giá
    @Query("SELECT CASE WHEN COUNT(od) > 0 THEN true ELSE false END " +
            "FROM OrderDetail od " +
            "WHERE od.rentalOrder.user.id = :userId " +
            "AND od.vehicle.id = :vehicleId " +
            "AND od.rentalOrder.status = 'COMPLETED'")
    boolean hasUserRentedVehicle(@Param("userId") String userId, @Param("vehicleId") Long vehicleId);
}