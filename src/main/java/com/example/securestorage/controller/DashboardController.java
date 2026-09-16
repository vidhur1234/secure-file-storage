package com.example.securestorage.controller;

import com.example.securestorage.dto.ApiResponse;
import com.example.securestorage.dto.DashboardStatsDto;
import com.example.securestorage.repository.FileMetadataRepository;
import com.example.securestorage.repository.RoleRepository;
import com.example.securestorage.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileMetadataRepository fileMetadataRepository;

    public DashboardController(UserRepository userRepository,
                               RoleRepository roleRepository,
                               FileMetadataRepository fileMetadataRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActive(true);
        long totalFiles = fileMetadataRepository.count();
        long totalRoles = roleRepository.count();

        long totalStorageBytes = fileMetadataRepository.findAll().stream()
                .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0L)
                .sum();

        DashboardStatsDto stats = new DashboardStatsDto(totalUsers, activeUsers, totalFiles, totalRoles, totalStorageBytes);
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics loaded", stats));
    }
}
