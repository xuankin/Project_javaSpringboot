package com.motorental.service;

import com.motorental.dto.cart.AddToCartDto;
import com.motorental.dto.cart.CartDto;
import com.motorental.dto.cart.CartItemDto;
import com.motorental.entity.RentalCart;
import com.motorental.entity.RentalCartItem;
import com.motorental.entity.User;
import com.motorental.entity.Vehicle;
import com.motorental.repository.RentalCartItemRepository;
import com.motorental.repository.RentalCartRepository;
import com.motorental.repository.UserRepository;
import com.motorental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RentalCartRepository cartRepository;
    private final RentalCartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public RentalCart getCartEntity(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow();
                    RentalCart cart = new RentalCart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    public CartDto getCartByUserId(String userId) {
        RentalCart cart = getCartEntity(userId);

        CartDto dto = new CartDto();
        dto.setId(cart.getId());
        dto.setItems(cart.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()));

        // Tính tổng tiền giỏ hàng
        BigDecimal total = cart.getItems().stream()
                .map(RentalCartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalAmount(total);
        dto.setTotalItems(cart.getItems().size());

        return dto;
    }

    @Transactional
    public void addToCart(String userId, AddToCartDto dto) {
        RentalCart cart = getCartEntity(userId);
        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Xe không tồn tại"));

        // Kiểm tra xem xe đã có trong giỏ chưa
        Optional<RentalCartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getVehicle().getId().equals(vehicle.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Cập nhật lại ngày thuê
            RentalCartItem item = existingItem.get();
            item.setStartDate(dto.getStartDate());
            item.setEndDate(dto.getEndDate());
            // @PreUpdate trong Entity sẽ tự tính lại rentalDays và totalPrice
            cartItemRepository.save(item);
        } else {
            // Thêm mới
            RentalCartItem newItem = new RentalCartItem();
            newItem.setRentalCart(cart);
            newItem.setVehicle(vehicle);
            newItem.setStartDate(dto.getStartDate());
            newItem.setEndDate(dto.getEndDate());
            newItem.setPricePerDay(vehicle.getPricePerDay());
            // @PrePersist sẽ tính toán còn lại

            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }
    }

    @Transactional
    public void removeFromCart(String userId, Long cartItemId) {
        // Cần check quyền sở hữu (UserId) để bảo mật
        cartItemRepository.deleteById(cartItemId);
    }

    @Transactional
    public void clearCart(String userId) {
        RentalCart cart = getCartEntity(userId);
        cartItemRepository.deleteByRentalCartId(cart.getId());
    }

    private CartItemDto mapItemToDto(RentalCartItem item) {
        Vehicle v = item.getVehicle();
        String img = v.getImages().isEmpty() ? "" : v.getImages().iterator().next().getImageUrl();

        return CartItemDto.builder()
                .id(item.getId())
                .vehicleId(v.getId())
                .vehicleName(v.getName())
                .vehicleImage(img)
                .startDate(item.getStartDate())
                .endDate(item.getEndDate())
                .rentalDays(item.getRentalDays())
                .pricePerDay(item.getPricePerDay())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}