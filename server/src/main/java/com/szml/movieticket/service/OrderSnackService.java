package com.szml.movieticket.service;

import com.szml.movieticket.dto.OrderSnackSelectionDTO;
import com.szml.movieticket.vo.SnackOrderItemVO;
import com.szml.movieticket.vo.SnackSelectionVO;

import java.util.List;

public interface OrderSnackService {

    SnackSelectionVO getSelection(Long userId, Long orderId);

    SnackSelectionVO replaceSelection(Long userId, Long orderId, OrderSnackSelectionDTO dto);

    List<SnackOrderItemVO> listOrderItems(Long orderId);

    int getSnackAmountFen(Long orderId);

    void markSold(Long orderId);

    void releaseReserved(Long orderId);
}
