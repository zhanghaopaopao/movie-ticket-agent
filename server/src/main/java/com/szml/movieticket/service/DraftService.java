package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.dto.DraftSaveDTO;
import com.szml.movieticket.entity.PurchaseDraft;
import com.szml.movieticket.vo.DraftVO;

/**
 * 购票草稿服务接口。
 *
 * @author zhanghao
 * @since 2026-08-02
 */
public interface DraftService extends IService<PurchaseDraft> {

    /**
     * 获取当前用户活动草稿。
     *
     * @param userId 用户ID
     * @return 草稿 VO，不存在时返回 null
     */
    DraftVO getCurrentDraft(Long userId);

    /**
     * 保存/更新草稿（乐观锁 + 级联清除）。
     *
     * @param userId 用户ID
     * @param dto    草稿数据
     * @return 更新后的草稿
     */
    DraftVO saveDraft(Long userId, DraftSaveDTO dto);
}
