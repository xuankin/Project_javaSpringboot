package com.motorental.repository;

import com.motorental.entity.VehicleLocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VehicleLocationHistoryRepository extends JpaRepository<VehicleLocationHistory, Long> {
    
    // Lấy 100 điểm gần nhất cho mượt bản đồ
    List<VehicleLocationHistory> findTop100ByVehicleIdOrderByTimestampDesc(Long vehicleId);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM VehicleLocationHistory v WHERE v.vehicle.id = :vehicleId " +
           "AND v.timestamp >= :startDate AND v.timestamp <= :endDate " +
           "ORDER BY v.timestamp ASC")
    List<VehicleLocationHistory> findByVehicleIdAndDateRange(
            @org.springframework.data.repository.query.Param("vehicleId") Long vehicleId,
            @org.springframework.data.repository.query.Param("startDate") LocalDateTime startDate,
            @org.springframework.data.repository.query.Param("endDate") LocalDateTime endDate);
}
