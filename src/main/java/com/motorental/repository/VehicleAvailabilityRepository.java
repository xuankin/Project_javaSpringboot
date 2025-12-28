package com.motorental.repository;

import com.motorental.entity.VehicleAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface VehicleAvailabilityRepository extends JpaRepository<VehicleAvailability, Long> {

    // STT 3: Schedule - Lấy lịch đặt tương lai của 1 xe để hiển thị
    @Query("SELECT va FROM VehicleAvailability va WHERE va.vehicle.id = :vehicleId " +
            "AND va.endDate >= CURRENT_DATE " +
            "ORDER BY va.startDate ASC")
    List<VehicleAvailability> findFutureBookings(@Param("vehicleId") Long vehicleId);

    // STT 7: Place Order - Quan trọng: Kiểm tra xe có bị trùng lịch không
    // Logic: (StartA <= EndB) and (EndA >= StartB) là có giao nhau
    @Query("SELECT va FROM VehicleAvailability va WHERE va.vehicle.id = :vehicleId " +
            "AND va.status IN ('BOOKED', 'COMPLETED') " +
            "AND (va.startDate <= :endDate AND va.endDate >= :startDate)")
    List<VehicleAvailability> findConflictingAvailabilities(
            @Param("vehicleId") Long vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Hỗ trợ hủy đơn: Xóa lịch khi đơn hàng bị hủy
    void deleteByOrderId(Long orderId);
}