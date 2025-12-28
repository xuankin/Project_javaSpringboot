package com.motorental.dto.dashboard;

import com.motorental.entity.RentalOrder; // Nhớ import entity RentalOrder
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private Long totalUsers;
    private Long totalVehicles;
    private Long totalOrders;

    private Long pendingOrders;   // Dùng cái này thay cho "newOrders"
    private Long confirmedOrders;
    private Long completedOrders;
    private Long cancelledOrders;

    private Long availableVehicles;
    private Long rentedVehicles;

    private Double totalRevenue;
    private Double monthlyRevenue;

    // THÊM MỚI: Danh sách đơn hàng gần đây
    private List<RentalOrder> recentOrders;

    private Map<String, Long> ordersByStatus;
}