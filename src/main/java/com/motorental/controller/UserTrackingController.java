package com.motorental.controller;

import com.motorental.entity.User;
import com.motorental.entity.UserLocationHistory;
import com.motorental.repository.UserLocationHistoryRepository;
import com.motorental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserTrackingController {

    private final UserLocationHistoryRepository historyRepository;
    private final UserRepository userRepository;

    @GetMapping("/tracking")
    public String trackingHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        
        LocalDate finalStartDate = (startDate != null) ? startDate : LocalDate.now();
        LocalDate finalEndDate = (endDate != null) ? endDate : LocalDate.now();
        
        if (finalStartDate.isAfter(finalEndDate)) {
            LocalDate temp = finalStartDate;
            finalStartDate = finalEndDate;
            finalEndDate = temp;
        }

        LocalDateTime startOfDay = finalStartDate.atStartOfDay();
        LocalDateTime endOfDay = finalEndDate.atTime(LocalTime.MAX);

        List<UserLocationHistory> history = historyRepository.findByUserIdAndDateRange(
                user.getId(), startOfDay, endOfDay);

        List<java.util.Map<String, Object>> historyDto = history.stream().map(h -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("latitude", h.getLatitude());
            map.put("longitude", h.getLongitude());
            map.put("timestamp", h.getTimestamp().toString());
            return map;
        }).collect(java.util.stream.Collectors.toList());

        model.addAttribute("historyDataJson", historyDto);
        model.addAttribute("historyCount", historyDto.size());
        model.addAttribute("startDate", finalStartDate);
        model.addAttribute("endDate", finalEndDate);

        return "tracking/history";
    }
}
