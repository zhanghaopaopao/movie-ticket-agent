package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.SnackProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SnackProductMapper extends BaseMapper<SnackProduct> {

    /** 锁定商品行，用于并发预占或释放库存。 */
    @Select("SELECT * FROM snack_product WHERE id = #{id} FOR UPDATE")
    SnackProduct selectForUpdate(Long id);
}
