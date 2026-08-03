package com.szml.movieticket.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.szml.movieticket.dto.UploadResultDTO;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.BusinessException;
import com.szml.movieticket.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * 文件服务实现类，上传到腾讯云 COS。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;
    private static final String IMMUTABLE_CACHE_CONTROL = "public, max-age=31536000, immutable";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final COSClient cosClient;

    @Value("${cos.client.bucket}")
    private String bucket;

    @Value("${cos.client.host}")
    private String host;

    @Override
    public UploadResultDTO uploadImage(MultipartFile file) {
        // 校验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("文件大小超限, size: {}, fileName: {}", file.getSize(), file.getOriginalFilename());
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEED);
        }

        // 校验文件格式
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("文件格式不支持, extension: {}, fileName: {}", extension, originalFilename);
            throw new BusinessException(ErrorCode.FILE_FORMAT_INVALID);
        }

        // 生成 COS 存储路径
        String cosKey = "posters/" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;

        try {
            // MultipartFile → 临时文件 → 上传 COS
            Path tempFile = Files.createTempFile("upload-", "." + extension);
            file.transferTo(tempFile.toFile());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, cosKey, tempFile.toFile());
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(resolveContentType(extension));
            metadata.setCacheControl(IMMUTABLE_CACHE_CONTROL);
            putObjectRequest.setMetadata(metadata);
            cosClient.putObject(putObjectRequest);

            // 清理临时文件
            Files.deleteIfExists(tempFile);

            String fileUrl = host + "/" + cosKey;

            log.info("文件上传成功, cosKey: {}, url: {}, size: {}", cosKey, fileUrl, file.getSize());

            UploadResultDTO result = new UploadResultDTO();
            result.setUrl(fileUrl);
            result.setFileName(cosKey);
            result.setSize(file.getSize());
            return result;
        } catch (IOException e) {
            log.error("文件上传失败, fileName: {}", originalFilename, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    /**
     * 从文件名中提取扩展名。
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String resolveContentType(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
