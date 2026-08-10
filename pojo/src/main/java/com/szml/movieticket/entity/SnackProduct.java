package com.szml.movieticket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 影院零食商品。 */
@Data
@TableName("snack_product")
public class SnackProduct {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cinemaId;

    private String name;

    private String description;

    private String image;

    /** 价格以分为单位保存。 */
    private Integer priceFen;

    /** 可售库存，锁定订单时会先从这里预占。 */
    private Integer stock;

    private Integer soldCount;

    /** 1 表示上架，0 表示下架。 */
    private Integer status;

    /** 逻辑删除：0=正常 1=已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
