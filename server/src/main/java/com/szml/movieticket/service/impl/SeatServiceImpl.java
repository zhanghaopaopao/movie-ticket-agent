package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.entity.Seat;
import com.szml.movieticket.mapper.SeatMapper;
import com.szml.movieticket.service.SeatService;
import org.springframework.stereotype.Service;

/**
 * 座位服务实现类。
 *
 * @author zhanghao
 * @since 2026-08-10
 */
@Service
public class SeatServiceImpl extends ServiceImpl<SeatMapper, Seat> implements SeatService {
}
