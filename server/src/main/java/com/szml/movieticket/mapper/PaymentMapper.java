package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper。
 *
 * @author zhanghao
 * @since 2026-07-31
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
