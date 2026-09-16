package com.example.securestorage.controller;

import com.example.securestorage.dto.ApiResponse;
import com.example.securestorage.dto.FileMetadataDto;
import com.example.securestorage.entity.User;
import com.example.securestorage.security.UserPrincipal;
import com.example.securestorage.service.FileStorageService;
import com.example.securestorage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final UserService userService;

    public FileController(FileStorageService fileStorageService, UserService userService) {
        this.fileStorageService = fileStorageService;
        this.userService = userService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileMetadataDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetRoleId", required = false) Long targetRoleId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request) {

        User user = userService.findByUsername(currentUser.getUsername());
        FileMetadataDto uploaded = fileStorageService.uploadFile(file, targetRoleId, user, getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success("File encrypted with AES-256 and uploaded successfully", uploaded));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileMetadataDto>>> getAccessibleFiles(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = userService.findByUsername(currentUser.getUsername());
        List<FileMetadataDto> files = fileStorageService.getAccessibleFiles(user);
        return ResponseEntity.ok(ApiResponse.success("Accessible files retrieved successfully", files));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileMetadataDto>> getFileById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = userService.findByUsername(currentUser.getUsername());
        FileMetadataDto fileDto = fileStorageService.getFileById(id, user);
        return ResponseEntity.ok(ApiResponse.success("File metadata retrieved", fileDto));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request) {

        User user = userService.findByUsername(currentUser.getUsername());
        FileStorageService.DecryptedFileResult result = fileStorageService.downloadFile(id, user, getClientIp(request));

        ByteArrayResource resource = new ByteArrayResource(result.getData());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.getFileName() + "\"")
                .header("X-Integrity-Status", "VERIFIED")
                .header("X-File-Hash", result.getFileHash())
                .body(resource);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<FileMetadataDto>>> searchFiles(
            @RequestParam(value = "keyword", required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        User user = userService.findByUsername(currentUser.getUsername());
        List<FileMetadataDto> searchResults = fileStorageService.searchFiles(keyword, user);
        return ResponseEntity.ok(ApiResponse.success("Search completed successfully", searchResults));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request) {

        User user = userService.findByUsername(currentUser.getUsername());
        fileStorageService.deleteFile(id, user, getClientIp(request));
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
