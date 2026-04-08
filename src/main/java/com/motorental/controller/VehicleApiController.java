package com.motorental.controller;

import com.motorental.dto.vehicle.VehicleDto;
import com.motorental.entity.Vehicle;
import com.motorental.repository.VehicleRepository;
import com.motorental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleApiController {

    private final VehicleRepository vehicleRepository;
    private final com.motorental.repository.VehicleLocationHistoryRepository historyRepository;

    @PostMapping("/{id}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Long id, @RequestBody Map<String, Double> coords) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow();
        if (coords.containsKey("lat") && coords.containsKey("lng")) {
            Double lat = coords.get("lat");
            Double lng = coords.get("lng");
            
            vehicle.setLatitude(lat);
            vehicle.setLongitude(lng);
            vehicleRepository.save(vehicle);

            // Lưu vào lịch sử để vẽ đường đi (Polyline)
            com.motorental.entity.VehicleLocationHistory history = new com.motorental.entity.VehicleLocationHistory();
            history.setVehicle(vehicle);
            history.setLatitude(lat);
            history.setLongitude(lng);
            historyRepository.save(history);

            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("Invalid coordinates");
    }

    @GetMapping("/{id}/location")
    public ResponseEntity<Map<String, Object>> getLocation(@PathVariable Long id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow();
        
        // Lấy lịch sử 100 điểm gần nhất
        List<com.motorental.entity.VehicleLocationHistory> history = 
            historyRepository.findTop100ByVehicleIdOrderByTimestampDesc(id);

        return ResponseEntity.ok(Map.of(
            "lat", vehicle.getLatitude() != null ? vehicle.getLatitude() : 0.0,
            "lng", vehicle.getLongitude() != null ? vehicle.getLongitude() : 0.0,
            "history", history.stream().map(h -> Map.of("lat", h.getLatitude(), "lng", h.getLongitude()))
                        .collect(java.util.stream.Collectors.toList())
        ));
    }
    @GetMapping("/all-locations")
    public ResponseEntity<List<Map<String, Object>>> getAllLocations() {
        return ResponseEntity.ok(vehicleRepository.findAll().stream().map(v -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", v.getId());
            map.put("name", v.getName());
            map.put("licensePlate", v.getLicensePlate());
            map.put("lat", v.getLatitude() != null ? v.getLatitude() : 0.0);
            map.put("lng", v.getLongitude() != null ? v.getLongitude() : 0.0);
            map.put("status", v.getStatus());
            
            // Lấy ảnh chính (hoặc ảnh bất kỳ) để hiện lên bản đồ Admin
            String primaryImage = v.getImages().stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .map(com.motorental.entity.VehicleImage::getImageUrl)
                    .findFirst()
                    .orElse(v.getImages().isEmpty() ? "" : v.getImages().iterator().next().getImageUrl());
            map.put("image", primaryImage);
            
            return map;
        }).collect(Collectors.toList()));
    }
}
