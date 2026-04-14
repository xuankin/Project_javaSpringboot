package com.motorental.controller;

import com.motorental.entity.RentalOrder;
import com.motorental.entity.User;
import com.motorental.entity.UserLocationHistory;
import com.motorental.entity.Vehicle;
import com.motorental.repository.RentalOrderRepository;
import com.motorental.repository.UserLocationHistoryRepository;
import com.motorental.repository.UserRepository;
import com.motorental.repository.VehicleRepository;
import com.motorental.repository.VehicleLocationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class UserTrackingApiController {

    private final UserLocationHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final RentalOrderRepository orderRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleLocationHistoryRepository vehicleHistoryRepository;

    @PostMapping("/update-location")
    @Transactional
    public ResponseEntity<?> updateLocation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Double> coords) {

        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }

        if (coords.containsKey("lat") && coords.containsKey("lng")) {
            Double lat = coords.get("lat");
            Double lng = coords.get("lng");

            userRepository.findByUsername(userDetails.getUsername()).ifPresent(user -> {
                UserLocationHistory history = UserLocationHistory.builder()
                        .user(user)
                        .latitude(lat)
                        .longitude(lng)
                        .build();
                historyRepository.save(history);

                // Update real-time location for rented vehicles
                List<RentalOrder.OrderStatus> activeStatuses = List.of(
                    RentalOrder.OrderStatus.PENDING, 
                    RentalOrder.OrderStatus.CONFIRMED, 
                    RentalOrder.OrderStatus.ACTIVE
                );

                List<RentalOrder> activeOrders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                        .filter(o -> activeStatuses.contains(o.getStatus()))
                        .collect(Collectors.toList());

                for (RentalOrder order : activeOrders) {
                    order.getOrderDetails().forEach(detail -> {
                        Vehicle vehicle = detail.getVehicle();
                        vehicle.setLatitude(lat);
                        vehicle.setLongitude(lng);
                        
                        // Nếu đang di chuyển thực tế, đảm bảo trạng thái xe là RENTED 
                        // để bản đồ hiện xanh dương (đang thuê) và GpsSimulationService bỏ qua mô phỏng
                        if (vehicle.getStatus() != Vehicle.VehicleStatus.RENTED) {
                            vehicle.setStatus(Vehicle.VehicleStatus.RENTED);
                        }
                        
                        vehicleRepository.save(vehicle);

                        // Save to vehicle history as well
                        com.motorental.entity.VehicleLocationHistory vHistory = new com.motorental.entity.VehicleLocationHistory();
                        vHistory.setVehicle(vehicle);
                        vHistory.setOrder(order);
                        vHistory.setLatitude(lat);
                        vHistory.setLongitude(lng);
                        vehicleHistoryRepository.save(vHistory);
                    });
                }
            });

            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().body("Invalid coordinates");
    }

    @GetMapping("/tracked-users")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getTrackedUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (User user : users) {
            List<UserLocationHistory> historyList = historyRepository.findByUserIdOrderByTimestampDesc(user.getId());
            if (!historyList.isEmpty()) {
                UserLocationHistory h = historyList.get(0); // Latest location
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", "user-" + user.getId());
                map.put("realId", user.getId());
                map.put("name", user.getFullName());
                map.put("licensePlate", user.getUsername());
                map.put("lat", h.getLatitude());
                map.put("lng", h.getLongitude());
                map.put("status", "USER");
                map.put("image", user.getAvatarUrl());
                result.add(map);
            }
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/user-history/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Double>>> getUserHistory(@PathVariable String userId) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7); // Last 1 week
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        List<UserLocationHistory> historyList = historyRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        List<Map<String, Double>> coords = historyList.stream()
                .map(h -> Map.of("lat", h.getLatitude(), "lng", h.getLongitude()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(coords);
    }
}
