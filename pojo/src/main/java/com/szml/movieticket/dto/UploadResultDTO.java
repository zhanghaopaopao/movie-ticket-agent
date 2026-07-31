package com.szml.movieticket.dto;

import lombok.Data;

/**
 * 文件上传结果 DTO。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Data
public class UploadResultDTO {

    /** 文件访问 URL */
    private String url;

    /** 文件名 */
    private String fileName;

    /** 文件大小（字节） */
    private long size;
}
