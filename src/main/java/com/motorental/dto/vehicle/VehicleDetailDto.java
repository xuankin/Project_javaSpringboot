package com.motorental.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDetailDto {
    private Long id;
    private String name;
    private String licensePlate;
    private String description;
    private BigDecimal pricePerDay;
    private String status;
    private String brand;
    private String model;
    private Integer year;
    private String color;
    private Integer rentalCount;

    private List<String> imageUrls; // Danh sách tất cả ảnh
    private String primaryImageUrl; // [FIX] Thêm trường này
    private Double averageRating;
}