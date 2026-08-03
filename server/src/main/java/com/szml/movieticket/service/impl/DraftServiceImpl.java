package com.szml.movieticket.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.DraftSaveDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.entity.Movie;
import com.szml.movieticket.entity.PurchaseDraft;
import com.szml.movieticket.entity.Seat;
import com.szml.movieticket.entity.Showtime;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.DraftException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.MovieMapper;
import com.szml.movieticket.mapper.PurchaseDraftMapper;
import com.szml.movieticket.mapper.SeatMapper;
import com.szml.movieticket.mapper.ShowtimeMapper;
import com.szml.movieticket.service.DraftService;
import com.szml.movieticket.vo.DraftVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 购票草稿服务实现类。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftServiceImpl extends ServiceImpl<PurchaseDraftMapper, PurchaseDraft> implements DraftService {

    private final MovieMapper movieMapper;
    private final CinemaMapper cinemaMapper;
    private final ShowtimeMapper showtimeMapper;
    private final SeatMapper seatMapper;

    @Override
    public DraftVO getCurrentDraft(Long userId) {
        PurchaseDraft draft = getOne(new LambdaQueryWrapper<PurchaseDraft>()
                .eq(PurchaseDraft::getUserId, userId)
                .eq(PurchaseDraft::getStatus, "ACTIVE"));
        if (draft == null) {
            return null;
        }
        return toVO(draft);
    }

    @Override
    @Transactional
    public DraftVO saveDraft(Long userId, DraftSaveDTO dto) {
        PurchaseDraft draft = getOne(new LambdaQueryWrapper<PurchaseDraft>()
                .eq(PurchaseDraft::getUserId, userId)
                .eq(PurchaseDraft::getStatus, "ACTIVE"));

        // 首次创建
        if (draft == null) {
            draft = new PurchaseDraft();
            draft.setUserId(userId);
            draft.setStatus("ACTIVE");
            draft.setSourceMode("TRADITIONAL");
            draft.setTicketCount(1);
            draft.setVersion(0);
        }

        // 乐观锁校验（首次创建 version=0 时跳过）
        if (draft.getVersion() > 0 && !draft.getVersion().equals(dto.getVersion())) {
            // 返回冲突，附带最新草稿
            throw new DraftException(ErrorCode.DRAFT_VERSION_CONFLICT);
        }

        List<String> clearedFields = new ArrayList<>();

        // 级联清除：影片变更 → 清影院、场次、座位
        if (isChanged(draft.getMovieId(), dto.getMovieId())) {
            draft.setCinemaId(null);
            draft.setShowtimeId(null);
            draft.setSeatsJson(null);
            clearedFields.add("cinemaId");
            clearedFields.add("showtimeId");
            clearedFields.add("seats");
        }

        // 级联清除：影院或日期变更 → 清场次、座位
        String newDateTimeJson = dto.getDateTime() != null ? JSONUtil.toJsonStr(dto.getDateTime()) : null;
        if (isChanged(draft.getCinemaId(), dto.getCinemaId())
                || isChanged(draft.getDateTimeJson(), newDateTimeJson)) {
            draft.setShowtimeId(null);
            draft.setSeatsJson(null);
            clearedFields.add("showtimeId");
            clearedFields.add("seats");
        }

        // 级联清除：场次变更 → 清座位
        if (isChanged(draft.getShowtimeId(), dto.getShowtimeId())) {
            draft.setSeatsJson(null);
            clearedFields.add("seats");
        }

        // 写入字段
        if (dto.getMovieId() != null) draft.setMovieId(dto.getMovieId());
        if (dto.getCinemaId() != null) draft.setCinemaId(dto.getCinemaId());
        if (dto.getDateTime() != null) draft.setDateTimeJson(JSONUtil.toJsonStr(dto.getDateTime()));
        if (dto.getShowtimeId() != null) draft.setShowtimeId(dto.getShowtimeId());
        if (dto.getTicketCount() != null && dto.getTicketCount() >= 1 && dto.getTicketCount() <= 6) {
            draft.setTicketCount(dto.getTicketCount());
        }
        if (dto.getBudget() != null) draft.setBudgetJson(JSONUtil.toJsonStr(dto.getBudget()));
        if (dto.getSeats() != null) draft.setSeatsJson(JSONUtil.toJsonStr(dto.getSeats()));
        if (StringUtils.hasText(dto.getSourceMode())) draft.setSourceMode(dto.getSourceMode());

        saveOrUpdate(draft);

        log.info("草稿保存成功, userId: {}, draftId: {}, version: {}, clearedFields: {}",
                userId, draft.getId(), draft.getVersion(), clearedFields);

        DraftVO vo = toVO(draft);
        vo.setClearedFields(clearedFields);
        return vo;
    }

    private DraftVO toVO(PurchaseDraft draft) {
        DraftVO vo = new DraftVO();
        vo.setId(draft.getId());
        vo.setVersion(draft.getVersion());
        vo.setStatus(draft.getStatus());
        vo.setSourceMode(draft.getSourceMode());
        vo.setTicketCount(draft.getTicketCount());
        vo.setOrderId(draft.getOrderId());

        // 影片
        if (draft.getMovieId() != null) {
            Movie movie = movieMapper.selectById(draft.getMovieId());
            if (movie != null) {
                DraftVO.Brief brief = new DraftVO.Brief();
                brief.setId(movie.getId());
                brief.setName(movie.getName());
                brief.setPoster(movie.getPoster());
                vo.setMovie(brief);
            }
        }

        // 影院
        if (draft.getCinemaId() != null) {
            Cinema cinema = cinemaMapper.selectById(draft.getCinemaId());
            if (cinema != null) {
                DraftVO.Brief brief = new DraftVO.Brief();
                brief.setId(cinema.getId());
                brief.setName(cinema.getName());
                vo.setCinema(brief);
            }
        }

        // 日期
        if (StringUtils.hasText(draft.getDateTimeJson())) {
            DraftVO.DateTimeRange dateTime = JSONUtil.toBean(draft.getDateTimeJson(), DraftVO.DateTimeRange.class);
            vo.setDateTime(dateTime);
        }

        // 场次
        if (draft.getShowtimeId() != null) {
            Showtime showtime = showtimeMapper.selectById(draft.getShowtimeId());
            if (showtime != null) {
                DraftVO.Brief brief = new DraftVO.Brief();
                brief.setId(showtime.getId());
                brief.setName(showtime.getStartAt() != null ? showtime.getStartAt().toString() : "");
                vo.setShowtime(brief);
            }
        }

        // 预算
        if (StringUtils.hasText(draft.getBudgetJson())) {
            vo.setBudget(JSONUtil.toBean(draft.getBudgetJson(), DraftVO.Budget.class));
        }

        // 座位：seatsJson 存的是座位ID数组，需查 Seat 表转为 rowNo/seatNo
        if (StringUtils.hasText(draft.getSeatsJson())) {
            List<Long> seatIds = JSONUtil.toList(draft.getSeatsJson(), Long.class);
            List<DraftVO.SeatItem> seatItems = new ArrayList<>();
            for (Long seatId : seatIds) {
                Seat seat = seatMapper.selectById(seatId);
                if (seat != null) {
                    DraftVO.SeatItem item = new DraftVO.SeatItem();
                    item.setRowNo(seat.getRowNo());
                    item.setSeatNo(seat.getSeatNo());
                    seatItems.add(item);
                }
            }
            vo.setSeats(seatItems);
        }

        // 是否可进入选座
        vo.setCanProceedToSeat(draft.getShowtimeId() != null && draft.getTicketCount() != null && draft.getTicketCount() > 0);

        return vo;
    }

    private boolean isChanged(Object oldValue, Object newValue) {
        if (newValue == null) return false;
        return !newValue.equals(oldValue);
    }
}
