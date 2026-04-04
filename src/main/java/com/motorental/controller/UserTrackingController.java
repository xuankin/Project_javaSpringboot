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
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        
        LocalDate filterDate = (date != null) ? date : LocalDate.now();
        LocalDateTime startOfDay = filterDate.atStartOfDay();
        LocalDateTime endOfDay = filterDate.atTime(LocalTime.MAX);

        List<UserLocationHistory> history = historyRepository.findByUserIdAndDateRange(
                user.getId(), startOfDay, endOfDay);

        model.addAttribute("history", history);
        model.addAttribute("selectedDate", filterDate);

        return "tracking/history";
    }
}
