package com.motorental.controller;

import com.motorental.dto.feedback.FeedbackDto;
import com.motorental.dto.vehicle.VehicleDetailDto;
import com.motorental.entity.VehicleAvailability;
import com.motorental.service.FeedbackService;
import com.motorental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final FeedbackService feedbackService;

    // Trang danh sách xe
    @GetMapping
    public String listVehicles(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) Double maxPrice,
            @PageableDefault(size = 9) Pageable pageable,
            Model model) {

        Page<VehicleDetailDto> vehiclePage = vehicleService.searchVehicles(keyword, maxPrice, pageable);

        model.addAttribute("vehicles", vehiclePage.getContent());
        model.addAttribute("currentPage", vehiclePage.getNumber());
        model.addAttribute("totalPages", vehiclePage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("maxPrice", maxPrice);

        return "vehicles/list";
    }

    // Trang chi tiết xe
    // [FIX] Mapping phải khớp với link trong list.html là /vehicles/detail/{id}
    @GetMapping("/detail/{id}")
    public String vehicleDetail(@PathVariable Long id, Model model) {
        // 1. Lấy thông tin xe
        VehicleDetailDto vehicle = vehicleService.getVehicleDetail(id);
        model.addAttribute("vehicle", vehicle);

        // 2. Lấy danh sách đánh giá (Feedback)
        List<FeedbackDto> feedbacks = feedbackService.getFeedbacksByVehicleId(id);
        model.addAttribute("feedbacks", feedbacks);

        // Form feedback rỗng để người dùng nhập mới (nếu cần)
        model.addAttribute("feedback", new FeedbackDto());

        // 3. Lấy lịch xe đã đặt (để chặn trên giao diện)
        List<VehicleAvailability> bookings = vehicleService.getFutureBookings(id);

        // [QUAN TRỌNG] Chuyển đổi sang định dạng JSON-friendly cho Flatpickr (JavaScript)
        List<Map<String, String>> blockedDates = bookings.stream()
                .map(b -> Map.of(
                        "from", b.getStartDate().toString(),
                        "to", b.getEndDate().toString()
                ))
                .collect(Collectors.toList());

        model.addAttribute("blockedDates", blockedDates);

        return "vehicles/detail";
    }
}