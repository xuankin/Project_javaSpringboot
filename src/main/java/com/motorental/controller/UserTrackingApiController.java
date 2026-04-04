package com.motorental.controller;

import com.motorental.entity.User;
import com.motorental.entity.UserLocationHistory;
import com.motorental.repository.UserLocationHistoryRepository;
import com.motorental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class UserTrackingApiController {

    private final UserLocationHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @PostMapping("/update-location")
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
            });

            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().body("Invalid coordinates");
    }
}
