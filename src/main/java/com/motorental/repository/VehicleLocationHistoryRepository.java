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

    // Mặc dù lấy Top 100 gần nhất, nhưng ta cần đảo ngược để vẽ từ cũ đến mới (Dựa theo timestamp)
}
