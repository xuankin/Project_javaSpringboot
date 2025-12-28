package com.motorental.service;

import com.motorental.dto.vehicle.VehicleDetailDto;
import com.motorental.dto.vehicle.VehicleDto;
import com.motorental.entity.Vehicle;
import com.motorental.entity.VehicleImage;
import com.motorental.repository.FeedbackRepository;
import com.motorental.repository.VehicleImageRepository;
import com.motorental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleImageRepository vehicleImageRepository;
    private final FeedbackRepository feedbackRepository;
    private final ModelMapper modelMapper;

    // --- Public Methods ---

    public Page<VehicleDto> searchVehicles(String keyword, String statusStr, Pageable pageable) {
        Vehicle.VehicleStatus status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = Vehicle.VehicleStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        return vehicleRepository.searchVehicles(keyword, status, pageable)
                .map(this::mapToDto);
    }

    public List<VehicleDto> getPopularVehicles() {
        // Lấy top 6 xe phổ biến
        return vehicleRepository.findTopPopularVehicles(Pageable.ofSize(6)).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public VehicleDetailDto getVehicleDetail(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Xe không tồn tại"));

        VehicleDetailDto dto = modelMapper.map(vehicle, VehicleDetailDto.class);

        // Map images
        List<String> images = vehicle.getImages().stream()
                .map(VehicleImage::getImageUrl)
                .collect(Collectors.toList());
        dto.setImageUrls(images);
        if (!images.isEmpty()) dto.setPrimaryImageUrl(images.get(0));

        // Map rating
        Double avgRating = feedbackRepository.getAverageRatingByVehicleId(id);
        dto.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        return dto;
    }

    // --- Admin Methods ---

    @Transactional
    public void createVehicle(VehicleDto dto, List<MultipartFile> images) {
        if (vehicleRepository.existsByLicensePlate(dto.getLicensePlate())) {
            throw new RuntimeException("Biển số xe đã tồn tại!");
        }

        Vehicle vehicle = modelMapper.map(dto, Vehicle.class);
        vehicle.setStatus(Vehicle.VehicleStatus.AVAILABLE);
        vehicle.setRentalCount(0);

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        saveImages(savedVehicle, images);
    }

    @Transactional
    public void updateVehicle(Long id, VehicleDto dto, List<MultipartFile> images) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Xe không tồn tại"));

        // Update basic info
        vehicle.setName(dto.getName());
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setPricePerDay(dto.getPricePerDay());
        vehicle.setDescription(dto.getDescription());
        vehicle.setYear(dto.getYear());
        vehicle.setColor(dto.getColor());

        if (dto.getStatus() != null) {
            vehicle.setStatus(Vehicle.VehicleStatus.valueOf(dto.getStatus()));
        }

        vehicleRepository.save(vehicle);

        // Nếu có upload ảnh mới thì xử lý (Logic tùy chọn: Xóa cũ thêm mới hoặc chỉ thêm mới)
        if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
            // Ví dụ: Giữ ảnh cũ, thêm ảnh mới. Nếu muốn reset thì gọi vehicleImageRepository.deleteByVehicleId
            saveImages(vehicle, images);
        }
    }

    @Transactional
    public void deleteVehicle(Long id) {
        // Chỉ nên soft delete hoặc kiểm tra ràng buộc khóa ngoại
        // Ở đây demo xóa cứng:
        vehicleRepository.deleteById(id);
    }

    // --- Helpers ---

    private VehicleDto mapToDto(Vehicle vehicle) {
        VehicleDto dto = modelMapper.map(vehicle, VehicleDto.class);

        // Set ảnh đại diện
        String primaryImage = vehicle.getImages().stream()
                .filter(VehicleImage::getIsPrimary)
                .map(VehicleImage::getImageUrl)
                .findFirst()
                .orElse(vehicle.getImages().isEmpty() ? "/images/default.jpg" : vehicle.getImages().iterator().next().getImageUrl());
        dto.setPrimaryImageUrl(primaryImage);

        return dto;
    }

    private void saveImages(Vehicle vehicle, List<MultipartFile> files) {
        if (files == null) return;

        // Đảm bảo thư mục tồn tại
        Path uploadPath = Paths.get("uploads/");
        try {
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Files.copy(file.getInputStream(), uploadPath.resolve(fileName));

                VehicleImage image = new VehicleImage();
                image.setVehicle(vehicle);
                image.setImageUrl("/uploads/" + fileName);
                image.setIsPrimary(vehicle.getImages().isEmpty()); // Ảnh đầu tiên là ảnh chính

                vehicleImageRepository.save(image);
                vehicle.getImages().add(image); // Sync entity
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file: " + e.getMessage());
        }
    }
}