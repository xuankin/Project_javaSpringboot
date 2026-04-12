package com.motorental.controller;

import com.motorental.dto.order.OrderDto;
import com.motorental.entity.VehicleLocationHistory;
import com.motorental.repository.VehicleLocationHistoryRepository;
import com.motorental.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderTrackingController {

    private final OrderService orderService;
    private final VehicleLocationHistoryRepository locationHistoryRepository;

    @GetMapping("/detail/{id}/track")
    public String trackOrder(@PathVariable("id") Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        try {
            OrderDto order = orderService.getOrderById(id);
            model.addAttribute("order", order);
            return "orders/track-map";
        } catch (Exception e) {
            return "redirect:/orders/my-orders";
        }
    }

    @GetMapping("/api/{id}/vehicle-location/history")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getOrderLocationHistory(@PathVariable("id") Long id) {
        List<VehicleLocationHistory> history = locationHistoryRepository.findTop100ByOrderIdOrderByTimestampDesc(id);
        
        // Cần copy ra một list mới để có thể reverse (nếu list trả ra từ JPA là unmodifiable)
        List<VehicleLocationHistory> modifiableList = new java.util.ArrayList<>(history);
        Collections.reverse(modifiableList); // Từ cũ đến mới
        
        List<Map<String, Object>> result = modifiableList.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("lat", h.getLatitude());
            map.put("lng", h.getLongitude());
            map.put("timestamp", h.getTimestamp());
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
}
