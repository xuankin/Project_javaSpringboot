package com.motorental.service;

import com.motorental.dto.vehicle.VehicleDetailDto;
import com.motorental.dto.vehicle.VehicleDto;
import com.motorental.entity.Vehicle;
import com.motorental.entity.VehicleAvailability;
import com.motorental.entity.VehicleImage;
import com.motorental.repository.FeedbackRepository;
import com.motorental.repository.VehicleAvailabilityRepository;
import com.motorental.repository.VehicleImageRepository;
import com.motorental.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    // =========================
    // PUBLIC
    // =========================
    public Page<VehicleDto> searchVehicles(String keyword, String statusStr, Pageable pageable) {
        Vehicle.VehicleStatus status = null;
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                status = Vehicle.VehicleStatus.valueOf(statusStr);
            } catch (IllegalArgumentException ignored) {}
        }
        return vehicleRepository.searchVehicles(keyword, status, pageable).map(this::mapToDto);
    }

    // --- Phương thức mới: Lấy danh sách xe phổ biến (gọi từ HomeController) ---
    public List<VehicleDto> getPopularVehicles() {
        return vehicleRepository.findTopPopularVehicles(Pageable.ofSize(6)).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<VehicleAvailability> getFutureBookings(Long vehicleId) {
        return availabilityRepository.findFutureBookings(
                vehicleId,
                List.of(VehicleAvailability.AvailabilityStatus.BOOKED,
                        VehicleAvailability.AvailabilityStatus.COMPLETED)
        );
    }

    public VehicleDetailDto getVehicleDetail(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Xe không tồn tại"));

        VehicleDetailDto dto = modelMapper.map(vehicle, VehicleDetailDto.class);

        List<String> images = vehicle.getImages().stream()
                .map(VehicleImage::getImageUrl)
                .collect(Collectors.toList());
        dto.setImageUrls(images);

        // primary
        String primary = vehicle.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(VehicleImage::getImageUrl)
                .findFirst()
                .orElse(images.isEmpty() ? "/images/default.jpg" : images.get(0));

        dto.setPrimaryImageUrl(primary);

        Double avgRating = feedbackRepository.getAverageRatingByVehicleId(id);
        dto.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        return dto;
    }

    // =========================
    // ADMIN
    // =========================
    @Transactional
    public void createVehicle(VehicleDto dto, List<MultipartFile> imageFiles) throws IOException {
        Vehicle vehicle = modelMapper.map(dto, Vehicle.class);

        Vehicle saved = vehicleRepository.save(vehicle);

        // nếu có ảnh thì lưu, ảnh đầu tiên là primary
        if (hasNewImages(images)) {
            saveImages(saved, images, true);
        }
    }

    // --- Phương thức mới: Cập nhật xe (gọi từ AdminVehicleController) ---
    @Transactional
    public void updateVehicle(Long id, VehicleDto dto, List<MultipartFile> imageFiles) throws IOException {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));

        // ✅ Check trùng biển số (trừ chính nó)
        String newPlate = dto.getLicensePlate();
        if (newPlate != null && !newPlate.isBlank()) {
            // Nếu repo của bạn chưa có existsByLicensePlateAndIdNot thì xem mục (2) bên dưới
            if (vehicleRepository.existsByLicensePlateAndIdNot(newPlate, id)) {
                throw new RuntimeException("Biển số xe đã tồn tại!");
            }
            // ✅ FIX: cập nhật biển số
            vehicle.setLicensePlate(newPlate);
        }

        // Update basic info
        vehicle.setName(dto.getName());
        vehicle.setLicensePlate(dto.getLicensePlate());
        vehicle.setDescription(dto.getDescription());
        vehicle.setPricePerDay(dto.getPricePerDay());
        vehicle.setBrand(dto.getBrand());
        vehicle.setModel(dto.getModel());
        vehicle.setYear(dto.getYear());
        vehicle.setColor(dto.getColor());

        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            vehicle.setStatus(Vehicle.VehicleStatus.valueOf(dto.getStatus()));
        }

        vehicleRepository.save(vehicle);

        // ✅ Nếu upload ảnh mới -> thay ảnh (xóa cũ + set mới làm primary)
        if (hasNewImages(images)) {
            replaceImages(vehicle, images);
        }

        vehicleRepository.save(vehicle);
    }

    // --- Phương thức mới: Xóa xe (gọi từ AdminVehicleController) ---
    @Transactional
    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }

    // =========================
    // HELPERS
    // =========================
    private VehicleDto mapToDto(Vehicle vehicle) {
        VehicleDto dto = modelMapper.map(vehicle, VehicleDto.class);

        String primaryImage = vehicle.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(VehicleImage::getImageUrl)
                .findFirst()
                .orElse(vehicle.getImages().isEmpty()
                        ? "/images/default.jpg"
                        : vehicle.getImages().iterator().next().getImageUrl());

        dto.setPrimaryImageUrl(primaryImage);
        return dto;
    }

    private boolean hasNewImages(List<MultipartFile> files) {
        return files != null && !files.isEmpty() && files.stream().anyMatch(f -> f != null && !f.isEmpty());
    }

    /**
     * Xóa ảnh cũ (DB + file) rồi lưu ảnh mới, ảnh đầu tiên là primary.
     */
    private void replaceImages(Vehicle vehicle, List<MultipartFile> newFiles) {
        // copy để tránh ConcurrentModification
        List<VehicleImage> oldImages = new ArrayList<>(vehicle.getImages());

        for (VehicleImage img : oldImages) {
            deleteFileByUrl(img.getImageUrl());
            vehicleImageRepository.delete(img);
        }
        vehicle.getImages().clear();

        saveImages(vehicle, newFiles, true);
    }

    /**
     * Lưu ảnh lên thư mục uploads và tạo VehicleImage.
     * @param firstPrimary nếu true: file đầu tiên sẽ là primary
     */
    private void saveImages(Vehicle vehicle, List<MultipartFile> files, boolean firstPrimary) {
        if (files == null) return;

        Path uploadPath = Paths.get(UPLOAD_DIR);
        try {
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            boolean primaryAssigned = false;

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                String original = file.getOriginalFilename();
                String safeName = (original == null) ? "image" : original.replaceAll("\\s+", "_");
                String fileName = UUID.randomUUID() + "_" + safeName;

                Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

                VehicleImage image = new VehicleImage();
                image.setVehicle(vehicle);
                image.setImageUrl(URL_PREFIX + fileName);

                boolean isPrimary = false;
                if (firstPrimary && !primaryAssigned) {
                    isPrimary = true;
                    primaryAssigned = true;
                }
                image.setIsPrimary(isPrimary);

                vehicleImageRepository.save(image);
                vehicle.getImages().add(image);
            }
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file: " + e.getMessage());
        }

        // Trả về đường dẫn tương đối để lưu vào DB (ví dụ: /uploads/abc.jpg)
        return "/" + UPLOAD_DIR + fileName;
    }

    private void deleteFileByUrl(String imageUrl) {
        if (imageUrl == null) return;
        if (!imageUrl.startsWith(URL_PREFIX)) return;

        String fileName = imageUrl.substring(URL_PREFIX.length());
        Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {}
    }
}
