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

    @Scheduled(fixedRate = 60000)
    public void simulateMovement() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getLatitude() == null || vehicle.getLongitude() == null) {
                // Đặt mặc định tại Thủ Đức, TP.HCM cho khớp với khu vực của người dùng
                vehicle.setLatitude(10.8491);
                vehicle.setLongitude(106.7720);
                vehicleRepository.save(vehicle);
            }
        }
    }
}
