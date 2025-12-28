package com.motorental.repository;

import com.motorental.entity.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleImageRepository extends JpaRepository<VehicleImage, Long> {
    // STT 2 & 13: Lấy ảnh để hiển thị chi tiết hoặc quản lý
    List<VehicleImage> findByVehicleId(Long vehicleId);

    // STT 13: Xóa ảnh cũ khi update xe
    void deleteByVehicleId(Long vehicleId);
}