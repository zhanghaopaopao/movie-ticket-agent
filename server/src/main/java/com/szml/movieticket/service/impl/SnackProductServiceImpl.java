package com.szml.movieticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.szml.movieticket.dto.SnackProductCreateDTO;
import com.szml.movieticket.dto.SnackProductStatusDTO;
import com.szml.movieticket.dto.SnackProductStockDTO;
import com.szml.movieticket.dto.SnackProductUpdateDTO;
import com.szml.movieticket.entity.Cinema;
import com.szml.movieticket.entity.SnackProduct;
import com.szml.movieticket.enumeration.ErrorCode;
import com.szml.movieticket.exception.OrderException;
import com.szml.movieticket.mapper.CinemaMapper;
import com.szml.movieticket.mapper.SnackProductMapper;
import com.szml.movieticket.service.SnackProductService;
import com.szml.movieticket.vo.SnackProductPageVO;
import com.szml.movieticket.vo.SnackProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** 零食商品管理服务实现。 */
@Service
@RequiredArgsConstructor
public class SnackProductServiceImpl extends ServiceImpl<SnackProductMapper, SnackProduct>
        implements SnackProductService {

    private final CinemaMapper cinemaMapper;

    @Override
    public SnackProductPageVO pageProducts(int page, int size, Long cinemaId, String keyword, Integer status) {
        LambdaQueryWrapper<SnackProduct> wrapper = new LambdaQueryWrapper<>();
        if (cinemaId != null) {
            wrapper.eq(SnackProduct::getCinemaId, cinemaId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(SnackProduct::getName, keyword.trim());
        }
        if (status != null) {
            validateStatus(status);
            wrapper.eq(SnackProduct::getStatus, status);
        }
        wrapper.orderByDesc(SnackProduct::getCreateTime).orderByDesc(SnackProduct::getId);

        Page<SnackProduct> result = page(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        SnackProductPageVO vo = new SnackProductPageVO();
        vo.setTotal(result.getTotal());
        vo.setPage((int) result.getCurrent());
        vo.setSize((int) result.getSize());
        vo.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return vo;
    }

    @Override
    public SnackProductVO getProduct(Long id) {
        return toVO(requireProduct(id));
    }

    @Override
    public void createProduct(SnackProductCreateDTO dto) {
        requireCinema(dto.getCinemaId());
        String name = normalizeName(dto.getName());
        ensureNameAvailable(dto.getCinemaId(), name, null);

        SnackProduct product = new SnackProduct();
        product.setCinemaId(dto.getCinemaId());
        product.setName(name);
        product.setDescription(trimToNull(dto.getDescription()));
        product.setImage(trimToNull(dto.getImage()));
        product.setPriceFen(dto.getPriceFen());
        product.setStock(dto.getStock());
        product.setSoldCount(0);
        product.setStatus(1);
        save(product);
    }

    @Override
    public void updateProduct(Long id, SnackProductUpdateDTO dto) {
        SnackProduct product = requireProduct(id);
        if (dto.getName() != null) {
            String name = normalizeName(dto.getName());
            ensureNameAvailable(product.getCinemaId(), name, id);
            product.setName(name);
        }
        if (dto.getDescription() != null) {
            product.setDescription(trimToNull(dto.getDescription()));
        }
        if (dto.getImage() != null) {
            product.setImage(trimToNull(dto.getImage()));
        }
        if (dto.getPriceFen() != null) {
            product.setPriceFen(dto.getPriceFen());
        }
        updateById(product);
    }

    @Override
    public void updateStatus(Long id, SnackProductStatusDTO dto) {
        validateStatus(dto.getStatus());
        SnackProduct product = requireProduct(id);
        product.setStatus(dto.getStatus());
        updateById(product);
    }

    @Override
    public void updateStock(Long id, SnackProductStockDTO dto) {
        SnackProduct product = baseMapper.selectForUpdate(id);
        if (product == null) {
            throw new OrderException(ErrorCode.SNACK_NOT_FOUND);
        }
        product.setStock(dto.getStock());
        updateById(product);
    }

    private SnackProduct requireProduct(Long id) {
        SnackProduct product = getById(id);
        if (product == null) {
            throw new OrderException(ErrorCode.SNACK_NOT_FOUND);
        }
        return product;
    }

    private void requireCinema(Long cinemaId) {
        if (cinemaId == null || cinemaMapper.selectById(cinemaId) == null) {
            throw new OrderException(ErrorCode.CINEMA_NOT_FOUND);
        }
    }

    private void ensureNameAvailable(Long cinemaId, String name, Long excludedId) {
        LambdaQueryWrapper<SnackProduct> wrapper = new LambdaQueryWrapper<SnackProduct>()
                .eq(SnackProduct::getCinemaId, cinemaId)
                .eq(SnackProduct::getName, name);
        if (excludedId != null) {
            wrapper.ne(SnackProduct::getId, excludedId);
        }
        if (count(wrapper) > 0) {
            throw new OrderException(ErrorCode.SNACK_NAME_DUPLICATE);
        }
    }

    private static String normalizeName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new OrderException(ErrorCode.PARAM_ERROR);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new OrderException(ErrorCode.SNACK_STATUS_INVALID);
        }
    }

    private SnackProductVO toVO(SnackProduct product) {
        SnackProductVO vo = new SnackProductVO();
        BeanUtils.copyProperties(product, vo);
        Cinema cinema = cinemaMapper.selectById(product.getCinemaId());
        if (cinema != null) {
            vo.setCinemaName(cinema.getName());
        }
        vo.setStatusDesc(Integer.valueOf(1).equals(product.getStatus()) ? "上架" : "下架");
        return vo;
    }
}
