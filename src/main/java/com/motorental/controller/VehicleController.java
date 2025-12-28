package com.motorental.controller;

import com.motorental.dto.feedback.FeedbackDto;
import com.motorental.dto.vehicle.VehicleDetailDto;
import com.motorental.dto.vehicle.VehicleDto;
import com.motorental.service.FeedbackService;
import com.motorental.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final FeedbackService feedbackService;

    // Danh sách xe (Tìm kiếm + Phân trang)
    @GetMapping
    public String listVehicles(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String status,
                               @RequestParam(defaultValue = "0") int page,
                               Model model) {
        Page<VehicleDto> vehiclePage = vehicleService.searchVehicles(keyword, status, PageRequest.of(page, 9));

        model.addAttribute("vehicles", vehiclePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", vehiclePage.getTotalPages());
        model.addAttribute("keyword", keyword); // Giữ lại từ khóa search trên thanh tìm kiếm
        model.addAttribute("status", status);

        return "vehicles/list";
    }

    // Chi tiết xe
    @GetMapping("/{id}")
    public String vehicleDetail(@PathVariable Long id, Model model) {
        VehicleDetailDto vehicle = vehicleService.getVehicleDetail(id);
        List<FeedbackDto> feedbacks = feedbackService.getFeedbacksByVehicleId(id);

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("feedbacks", feedbacks);
        model.addAttribute("newFeedback", new FeedbackDto()); // Form để user viết đánh giá mới

        return "vehicles/detail";
    }
}