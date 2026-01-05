package com.motorental.dto.dashboard;

import com.motorental.entity.RentalOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    // --- Các chỉ số tổng quan ---
    private Long totalUsers;
    private Long totalVehicles;
    private Long totalOrders;

    // --- Trạng thái đơn hàng (Số lượng) ---
    private Long pendingOrders;
    private Long confirmedOrders;
    private Long completedOrders;
    private Long cancelledOrders;

    // --- Trạng thái xe ---
    private Long availableVehicles;
    private Long rentedVehicles;

    // --- Doanh thu ---
    private Double totalRevenue;
    private Double monthlyRevenue; // Doanh thu tháng hiện tại

    // --- Danh sách đơn hàng gần đây ---
    private List<RentalOrder> recentOrders;

    // --- DỮ LIỆU CHO BIỂU ĐỒ (CHARTS) ---
    // 1. Biểu đồ doanh thu 6 tháng gần nhất
    private List<String> revenueLabels; // VD: ["Tháng 1", "Tháng 2", ...]
    private List<Double> revenueData;   // VD: [1000000, 2500000, ...]

    // 2. Biểu đồ tròn trạng thái đơn hàng (Dữ liệu dạng list để dễ map vào JS)
    private List<Long> orderStatusCounts; // Thứ tự: [Pending, Confirmed, Completed, Cancelled]
}