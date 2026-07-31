package com.szml.movieticket.service;

import com.szml.movieticket.dto.UploadResultDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
public interface FileService {

    /**
     * 上传图片文件到 COS。
     *
     * @param file MultipartFile，支持 jpg/png/webp，最大 5MB
     * @return 上传结果（url, fileName, size）
     */
    UploadResultDTO uploadImage(MultipartFile file);
}
