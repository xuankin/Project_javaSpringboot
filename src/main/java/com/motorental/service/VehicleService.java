package com.motorental.service;

import com.motorental.dto.vehicle.VehicleDetailDto;
import com.motorental.dto.vehicle.VehicleDto;
import com.motorental.entity.Vehicle;
import com.motorental.entity.VehicleAvailability;
import com.motorental.entity.VehicleImage;
import com.motorental.repository.FeedbackRepository;
import com.motorental.repository.VehicleAvailabilityRepository;
import com.motorental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private static final String UPLOAD_DIR = "uploads";
    private static final String URL_PREFIX = "/uploads/";

    private final VehicleRepository vehicleRepository;
    private final VehicleAvailabilityRepository availabilityRepository;
    private final FeedbackRepository feedbackRepository;
    private final ModelMapper modelMapper;

    // --- PHẦN KHÁCH HÀNG (SEARCH & VIEW) ---

    // 1. [MỚI] Lấy danh sách hãng xe
    public List<String> getAllBrands() {
        return vehicleRepository.findAllBrands();
    }

    // 2. [CẬP NHẬT] Tìm kiếm xe với bộ lọc đầy đủ
    public Page<VehicleDetailDto> searchVehiclesDetail(String keyword, String brand, Double maxPrice, Pageable pageable) {
        return vehicleRepository.searchVehiclesDetail(keyword, brand, maxPrice, pageable)
                .map(this::mapToDetailDto);
    }

    public Page<VehicleDto> searchVehicles(String keyword, String statusStr, Pageable pageable) {
        Vehicle.VehicleStatus status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try { status = Vehicle.VehicleStatus.valueOf(statusStr); } catch (IllegalArgumentException e) {}
        }
        return vehicleRepository.searchVehicles(keyword, status, pageable).map(this::mapToDto);
    }

    public List<VehicleDto> getPopularVehicles() {
        return vehicleRepository.findTopPopularVehicles(Pageable.ofSize(6)).stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    public VehicleDetailDto getVehicleDetail(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id).orElseThrow(() -> new RuntimeException("Xe không tồn tại"));
        return mapToDetailDto(vehicle);
    }

    public List<VehicleAvailability> getFutureBookings(Long vehicleId) {
        return availabilityRepository.findFutureBookings(vehicleId, List.of(VehicleAvailability.AvailabilityStatus.BOOKED, VehicleAvailability.AvailabilityStatus.COMPLETED));
    }

    // --- ADMIN: CREATE & UPDATE (GIỮ NGUYÊN) ---

    @Transactional
    public void createVehicle(VehicleDto dto, List<MultipartFile> imageFiles) throws IOException {
        if (vehicleRepository.existsByLicensePlateAndIdNot(dto.getLicensePlate(), -1L)) {
            throw new RuntimeException("Biển số xe " + dto.getLicensePlate() + " đã tồn tại!");
        }

        Vehicle vehicle = modelMapper.map(dto, Vehicle.class);
        Vehicle saved = vehicleRepository.save(vehicle);
        if (hasNewImages(imageFiles)) {
            saveImages(saved, imageFiles, true);
        }
    }

    @Transactional
    public void updateVehicle(Long id, VehicleDto dto, List<MultipartFile> imageFiles) throws IOException {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe id: " + id));

        if (dto.getLicensePlate() != null && !dto.getLicensePlate().equals(vehicle.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlateAndIdNot(dto.getLicensePlate(), id)) {
                throw new RuntimeException("Biển số xe đã tồn tại!");
            }
            vehicle.setLicensePlate(dto.getLicensePlate());
        }

        vehicle.setName(dto.getName());
        vehicle.setDescription(dto.getDescription());
        vehicle.setPricePerDay(dto.getPricePerDay());
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setYear(dto.getYear());
        vehicle.setColor(dto.getColor());

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            vehicle.setStatus(Vehicle.VehicleStatus.valueOf(dto.getStatus()));
        }

        if (hasNewImages(imageFiles)) {
            List<String> oldUrls = vehicle.getImages().stream()
                    .map(VehicleImage::getImageUrl).collect(Collectors.toList());
            vehicle.getImages().clear();
            for (String url : oldUrls) {
                deleteFileByUrl(url);
            }
            saveImages(vehicle, imageFiles, true);
        }

        vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }

    // --- HELPER FUNCTIONS ---

    private void saveImages(Vehicle vehicle, List<MultipartFile> files, boolean firstPrimary) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        boolean primaryAssigned = false;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            String safeName = (original == null) ? "img" : original.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            String fileName = UUID.randomUUID() + "_" + safeName;
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            VehicleImage image = new VehicleImage();
            image.setVehicle(vehicle);
            image.setImageUrl(URL_PREFIX + fileName);
            if (firstPrimary && !primaryAssigned) {
                image.setIsPrimary(true);
                primaryAssigned = true;
            } else {
                image.setIsPrimary(false);
            }
            vehicle.getImages().add(image);
        }
    }

    private void deleteFileByUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) return;
        try {
            String fileName = imageUrl.substring(URL_PREFIX.length());
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            System.err.println("Không thể xóa file ảnh cũ: " + e.getMessage());
        }
    }

    private boolean hasNewImages(List<MultipartFile> files) {
        return files != null && !files.isEmpty() && files.stream().anyMatch(f -> f != null && !f.isEmpty());
    }

    private VehicleDto mapToDto(Vehicle vehicle) {
        VehicleDto dto = modelMapper.map(vehicle, VehicleDto.class);
        dto.setPrimaryImageUrl(getPrimaryImageUrl(vehicle));
        return dto;
    }

    private VehicleDetailDto mapToDetailDto(Vehicle vehicle) {
        VehicleDetailDto dto = modelMapper.map(vehicle, VehicleDetailDto.class);
        List<String> images = vehicle.getImages().stream().map(VehicleImage::getImageUrl).collect(Collectors.toList());
        dto.setImageUrls(images);
        dto.setPrimaryImageUrl(getPrimaryImageUrl(vehicle));
        Double avg = feedbackRepository.getAverageRatingByVehicleId(vehicle.getId());
        dto.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        return dto;
    }

    private String getPrimaryImageUrl(Vehicle vehicle) {
        if (vehicle.getImages() == null || vehicle.getImages().isEmpty()) return "/images/default.jpg";
        return vehicle.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(VehicleImage::getImageUrl)
                .findFirst()
                .orElse(vehicle.getImages().iterator().next().getImageUrl());
    }
}