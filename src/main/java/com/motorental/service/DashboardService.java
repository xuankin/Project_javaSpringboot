package com.motorental.service;

import com.motorental.dto.dashboard.DashboardStatsDto;
import com.motorental.entity.RentalOrder;
import com.motorental.entity.Vehicle;
import com.motorental.repository.RentalOrderRepository;
import com.motorental.repository.UserRepository;
import com.motorental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RentalOrderRepository orderRepository;

    public DashboardStatsDto getDashboardStats() {
        DashboardStatsDto stats = new DashboardStatsDto();

        // 1. Số liệu tổng quan
        stats.setTotalUsers(userRepository.count());
        stats.setTotalVehicles(vehicleRepository.count());
        stats.setTotalOrders(orderRepository.count());

        // 2. Doanh thu tổng
        Double totalRev = orderRepository.getTotalRevenue();
        stats.setTotalRevenue(totalRev != null ? totalRev : 0.0);

        // 3. Doanh thu tháng hiện tại
        LocalDate now = LocalDate.now();
        stats.setMonthlyRevenue(orderRepository.getMonthlyRevenue(now.getYear(), now.getMonthValue()));

        // 4. Số liệu chi tiết trạng thái
        long pending = orderRepository.countByStatus(RentalOrder.OrderStatus.PENDING);
        long confirmed = orderRepository.countByStatus(RentalOrder.OrderStatus.CONFIRMED);
        long completed = orderRepository.countByStatus(RentalOrder.OrderStatus.COMPLETED);
        long cancelled = orderRepository.countByStatus(RentalOrder.OrderStatus.CANCELLED);

        stats.setPendingOrders(pending);
        stats.setConfirmedOrders(confirmed);
        stats.setCompletedOrders(completed);
        stats.setCancelledOrders(cancelled);

        // List cho biểu đồ tròn (Pie Chart)
        stats.setOrderStatusCounts(Arrays.asList(pending, confirmed, completed, cancelled));

        stats.setAvailableVehicles(vehicleRepository.countByStatus(Vehicle.VehicleStatus.AVAILABLE));
        stats.setRentedVehicles(vehicleRepository.countByStatus(Vehicle.VehicleStatus.RENTED));

        // 5. Đơn hàng gần đây (Top 5)
        stats.setRecentOrders(orderRepository.findAll(
                PageRequest.of(0, 5, Sort.by("id").descending())
        ).getContent());

        // 6. Dữ liệu biểu đồ doanh thu (6 tháng gần nhất)
        List<String> labels = new ArrayList<>();
        List<Double> data = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            int m = date.getMonthValue();
            int y = date.getYear();

            // Label: "Tháng 1", "Tháng 2"...
            labels.add("Tháng " + m + "/" + y);

            // Data
            Double monthRev = orderRepository.getMonthlyRevenue(y, m);
            data.add(monthRev != null ? monthRev : 0.0);
        }
        stats.setRevenueLabels(labels);
        stats.setRevenueData(data);

        return stats;
    }
}