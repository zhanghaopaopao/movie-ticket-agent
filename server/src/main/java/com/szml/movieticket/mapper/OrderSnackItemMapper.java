package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.OrderSnackItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderSnackItemMapper extends BaseMapper<OrderSnackItem> {

    /** 锁定单条零食明细。 */
    @Select("SELECT * FROM order_snack_item WHERE id = #{id} FOR UPDATE")
    OrderSnackItem selectForUpdate(Long id);

    /** 按零食 ID 顺序锁定订单中的零食明细，避免并发更新库存时死锁。 */
    @Select("SELECT * FROM order_snack_item WHERE order_id = #{orderId} ORDER BY snack_id ASC FOR UPDATE")
    List<OrderSnackItem> selectByOrderForUpdate(Long orderId);
}
