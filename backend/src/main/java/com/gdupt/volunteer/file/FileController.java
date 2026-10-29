package com.gdupt.volunteer.file;

import com.gdupt.volunteer.common.ApiResponse;
import com.gdupt.volunteer.common.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传：活动封面图片等（登录用户即可上传，dir 限定子目录防路径穿越）。
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** 上传图片（默认活动封面目录 covers），返回可访问 URL（/uploads/...） */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dir", defaultValue = "covers") String dir) {
        return ApiResponse.ok(Map.of("url", fileStorageService.storeImage(file, dir)));
    }
}
