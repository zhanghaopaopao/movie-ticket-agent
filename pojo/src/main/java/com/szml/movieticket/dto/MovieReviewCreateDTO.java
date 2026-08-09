package com.szml.movieticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发布影片评论请求。 */
@Data
public class MovieReviewCreateDTO {

    /** 回复的父影评 ID，空表示发布顶级影评。 */
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 500, message = "评论内容不能超过500个字符")
    private String content;
}
