package com.motorental.service;

import com.motorental.dto.order.CreateOrderDto;
import com.motorental.dto.order.OrderDto;
import com.motorental.dto.order.OrderDetailDto;
import com.motorental.dto.payment.PaymentDto;
import com.motorental.entity.*;
import com.motorental.repository.*;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final RentalOrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository; //
    private final VehicleAvailabilityRepository availabilityRepository; //
    private final CartService cartService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final ModelMapper modelMapper;

    @Transactional(rollbackFor = Exception.class)
    public OrderDto createOrderFromCart(String userId, CreateOrderDto createOrderDto) {
        // 1. Lấy giỏ hàng
        RentalCart cart = cartService.getCartEntity(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống!");
        }

        // 2. CHECK AVAILABILITY (Rất quan trọng)
        // Duyệt từng xe trong giỏ để kiểm tra xem có ai đặt chưa
        for (RentalCartItem item : cart.getItems()) {
            List<VehicleAvailability> conflicts = availabilityRepository.findConflictingAvailabilities(
                    item.getVehicle().getId(),
                    item.getStartDate(),
                    item.getEndDate()
            );
            if (!conflicts.isEmpty()) {
                throw new RuntimeException("Xe '" + item.getVehicle().getName() +
                        "' đã bị người khác đặt trong khoảng thời gian này. Vui lòng chọn xe khác.");
            }
        }

        // 3. Tạo Order
        RentalOrder order = new RentalOrder();
        order.setUser(cart.getUser());
        order.setNotes(createOrderDto.getNotes());
        order.setStatus(RentalOrder.OrderStatus.PENDING);

        // 4. Chuyển Cart Items -> Order Details & Tạo Lịch Booked
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (RentalCartItem item : cart.getItems()) {
            OrderDetail detail = new OrderDetail();
            detail.setRentalOrder(order);
            detail.setVehicle(item.getVehicle());
            detail.setStartDate(item.getStartDate());
            detail.setEndDate(item.getEndDate());
            detail.setPricePerDay(item.getPricePerDay());
            detail.setRentalDays(item.getRentalDays());
            detail.setTotalPrice(item.getTotalPrice());

            order.addOrderDetail(detail); // Helper method trong Entity
            totalPrice = totalPrice.add(item.getTotalPrice());

            // Tạo bản ghi Availability để khóa lịch
            VehicleAvailability availability = new VehicleAvailability();
            availability.setVehicle(item.getVehicle());
            availability.setOrder(order);
            availability.setStartDate(item.getStartDate());
            availability.setEndDate(item.getEndDate());
            availability.setStatus(VehicleAvailability.AvailabilityStatus.BOOKED);

            // Lưu vào list của Order để Cascade lưu luôn
            order.getAvailabilities().add(availability);

            // Tăng lượt thuê của xe
            item.getVehicle().incrementRentalCount();
        }

        order.setTotalPrice(totalPrice);

        // 5. Lưu Order (Cascade sẽ lưu OrderDetail và VehicleAvailability)
        RentalOrder savedOrder = orderRepository.save(order);

        // 6. Xóa giỏ hàng
        cartService.clearCart(userId);

        // 7. Gửi email xác nhận (Async)
        try {
            emailService.sendOrderConfirmationEmail(cart.getUser().getEmail(), savedOrder);
        } catch (Exception e) {
            System.err.println("Lỗi gửi mail: " + e.getMessage());
        }

        return mapToDto(savedOrder);
    }

    public List<OrderDto> getOrdersByUserId(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public OrderDto getOrderById(Long orderId) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
        return mapToDto(order);
    }

    // --- Admin Methods ---

    public List<OrderDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc(Pageable.unpaged()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelOrder(Long orderId, String userId) {
        RentalOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // Nếu là user thường hủy, check quyền sở hữu
        if (userId != null && !order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn này");
        }

        if (order.getStatus() == RentalOrder.OrderStatus.COMPLETED ||
                order.getStatus() == RentalOrder.OrderStatus.CANCELLED) {
            throw new RuntimeException("Không thể hủy đơn hàng ở trạng thái này");
        }

        order.setStatus(RentalOrder.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Xóa lịch availability để nhả xe cho người khác thuê
        availabilityRepository.deleteByOrderId(orderId);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String statusStr) {
        RentalOrder order = orderRepository.findById(orderId).orElseThrow();
        RentalOrder.OrderStatus newStatus = RentalOrder.OrderStatus.valueOf(statusStr);

        // Nếu chuyển sang Cancelled -> Xóa lịch
        if (newStatus == RentalOrder.OrderStatus.CANCELLED) {
            availabilityRepository.deleteByOrderId(orderId);
        }

        // Nếu trước đó là Cancelled mà chuyển lại Confirmed -> Cần check lại lịch (Phức tạp, tạm bỏ qua cho đơn giản)

        order.setStatus(newStatus);
        orderRepository.save(order);

        // Gửi mail báo cập nhật
        emailService.sendOrderStatusUpdateEmail(order.getUser().getEmail(), order, order.getStatus().name(), statusStr);
    }

    // --- Helper Mapping ---
    private OrderDto mapToDto(RentalOrder order) {
        OrderDto dto = modelMapper.map(order, OrderDto.class);
        dto.setUserName(order.getUser().getFullName());
        dto.setUserEmail(order.getUser().getEmail());

        // Map list details
        List<OrderDetailDto> details = order.getOrderDetails().stream().map(d -> {
            OrderDetailDto dDto = modelMapper.map(d, OrderDetailDto.class);
            dDto.setVehicleName(d.getVehicle().getName());
            dDto.setVehicleId(d.getVehicle().getId());
            return dDto;
        }).collect(Collectors.toList());
        dto.setOrderDetails(details);

        // Map payment if exists
        if (order.getPayment() != null) {
            dto.setPayment(modelMapper.map(order.getPayment(), PaymentDto.class));
        }

        return dto;
    }
}