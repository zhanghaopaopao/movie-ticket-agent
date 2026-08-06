package com.szml.movieticket.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.szml.movieticket.dto.SnackProductCreateDTO;
import com.szml.movieticket.dto.SnackProductStatusDTO;
import com.szml.movieticket.dto.SnackProductStockDTO;
import com.szml.movieticket.dto.SnackProductUpdateDTO;
import com.szml.movieticket.entity.SnackProduct;
import com.szml.movieticket.vo.SnackProductPageVO;
import com.szml.movieticket.vo.SnackProductVO;

public interface SnackProductService extends IService<SnackProduct> {

    SnackProductPageVO pageProducts(int page, int size, Long cinemaId, String keyword, Integer status);

    SnackProductVO getProduct(Long id);

    void createProduct(SnackProductCreateDTO dto);

    void updateProduct(Long id, SnackProductUpdateDTO dto);

    void updateStatus(Long id, SnackProductStatusDTO dto);

    void updateStock(Long id, SnackProductStockDTO dto);
}
