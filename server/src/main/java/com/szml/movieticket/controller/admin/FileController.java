package com.szml.movieticket.controller.admin;

import com.szml.movieticket.dto.UploadResultDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传 Controller（B 端）。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传图片文件（海报等）。
     *
     * @param file 上传的图片文件，支持 jpg/png/webp，最大 5MB
     * @return 文件访问 URL
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadResultDTO> upload(@RequestParam("file") MultipartFile file) {
        log.info("上传图片文件, 文件名: {}, 文件大小: {}字节", file.getOriginalFilename(), file.getSize());
        UploadResultDTO uploadResultDTO = fileService.uploadImage(file);
        return Result.success(uploadResultDTO);
    }
}
