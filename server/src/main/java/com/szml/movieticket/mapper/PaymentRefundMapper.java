package com.szml.movieticket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.szml.movieticket.entity.PaymentRefund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 支付退款记录 Mapper。 */
@Mapper
public interface PaymentRefundMapper extends BaseMapper<PaymentRefund> {

    /** 按主键锁定退款记录，避免并发回调重复结算。 */
    @Select("SELECT * FROM payment_refund WHERE id = #{id} FOR UPDATE")
    PaymentRefund selectForUpdate(Long id);
}
