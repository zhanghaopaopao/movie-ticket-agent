package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存用户搜索历史请求。
 */
@Data
public class SearchHistorySaveDTO {

    @NotBlank(message = "搜索关键词不能为空")
    @Size(max = 128, message = "搜索关键词不能超过128个字符")
    private String keyword;
}
