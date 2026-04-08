package com.motorental.service;

import com.motorental.entity.Vehicle;
import com.motorental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class GpsSimulationService {

    private final VehicleRepository vehicleRepository;
    private final com.motorental.repository.VehicleLocationHistoryRepository historyRepository;
    private final Random random = new Random();

    @Scheduled(fixedRate = 3000)
    public void simulateMovement() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        
        for (Vehicle vehicle : vehicles) {
            // [MỚI] Nếu xe vừa được cập nhật thực tế (bởi User bật GPS) trong vòng 10 giây qua,
            // thì Server sẽ tạm ngưng giả lập cho xe đó để tránh bị "nhảy" tọa độ.
            if (vehicle.getUpdatedAt() != null &&
                (System.currentTimeMillis() - vehicle.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli() < 10000)) {
                continue;
            }

            if (vehicle.getLatitude() != null && vehicle.getLongitude() != null 
                && vehicle.getStatus() == Vehicle.VehicleStatus.RENTED) {
                
                double latOffset = (random.nextDouble() - 0.5) * 0.0001;
                double lngOffset = (random.nextDouble() - 0.5) * 0.0001;
                
                vehicle.setLatitude(vehicle.getLatitude() + latOffset);
                vehicle.setLongitude(vehicle.getLongitude() + lngOffset);
                
                vehicleRepository.save(vehicle);

                // Lưu vào lịch sử cho bản đồ admin/user thấy đường kẻ
                com.motorental.entity.VehicleLocationHistory history = new com.motorental.entity.VehicleLocationHistory();
                history.setVehicle(vehicle);
                history.setLatitude(vehicle.getLatitude());
                history.setLongitude(vehicle.getLongitude());
                historyRepository.save(history);
            }
        }
    }
}
