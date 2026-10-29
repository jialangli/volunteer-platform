package com.gdupt.volunteer.common;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储：活动封面图片上传（类型/大小白名单）、证明 PDF 落盘。
 * 文件统一存于 app.upload-dir，通过 /uploads/** 对外提供访问（WebConfig）。
 */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    /** 图片类型白名单 */
    private static final Set<String> ALLOWED_IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_IMAGE_BYTES = 5 * 1024 * 1024L;

    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * 保存上传图片，返回可访问的相对 URL（如 /uploads/covers/xxx.jpg）。
     *
     * @param file   上传文件
     * @param subDir 子目录（仅允许白名单内的子目录，防路径穿越）
     */
    public String storeImage(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件为空");
        }
        if (!ALLOWED_SUB_DIRS.contains(subDir)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法上传目录");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        String original = file.getOriginalFilename();
        String ext = extOf(original);
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = root().resolve(subDir);
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_WRITE_FAILED);
        }
        return "/uploads/" + subDir + "/" + filename;
    }

    /** 写入生成文件（证明 PDF），relativePath 形如 certs/cert-xxx.pdf，返回 /uploads/ 相对 URL */
    public String write(String relativePath, byte[] data) {
        try {
            Path file = root().resolve(relativePath);
            Files.createDirectories(file.getParent());
            Files.write(file, data);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_WRITE_FAILED);
        }
        return "/uploads/" + relativePath.replace('\\', '/');
    }

    /** 读取已生成文件（证明 PDF 下载） */
    public byte[] read(String relativeUrl) {
        if (relativeUrl == null || !relativeUrl.startsWith("/uploads/")) {
            throw new BusinessException(ErrorCode.CERT_PDF_NOT_GENERATED);
        }
        try {
            Path file = root().resolve(relativeUrl.substring("/uploads/".length()));
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.CERT_PDF_NOT_GENERATED);
        }
    }

    private Path root() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    private static String extOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 允许写入的子目录（防止路径穿越） */
    private static final Set<String> ALLOWED_SUB_DIRS = Set.of("covers");
}
