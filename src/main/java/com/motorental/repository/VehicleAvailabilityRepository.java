package com.motorental.repository;

import com.motorental.entity.VehicleAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface VehicleAvailabilityRepository extends JpaRepository<VehicleAvailability, Long> {

    @Query("SELECT va FROM VehicleAvailability va WHERE va.vehicle.id = :vehicleId " +
            "AND va.status IN :statuses " +
            "AND va.endDate >= CURRENT_DATE " +
            "ORDER BY va.startDate ASC")
    List<VehicleAvailability> findFutureBookings(
            @Param("vehicleId") Long vehicleId,
            @Param("statuses") List<VehicleAvailability.AvailabilityStatus> statuses);

    @Query("SELECT va FROM VehicleAvailability va WHERE va.vehicle.id = :vehicleId " +
            "AND va.status IN :statuses " +
            "AND ((va.startDate <= :endDate AND va.endDate >= :startDate))")
    List<VehicleAvailability> findConflictingAvailabilities(
            @Param("vehicleId") Long vehicleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<VehicleAvailability.AvailabilityStatus> statuses);

    @Modifying
    @Query("DELETE FROM VehicleAvailability va WHERE va.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);
}