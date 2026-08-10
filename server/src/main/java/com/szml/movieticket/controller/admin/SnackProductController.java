package com.szml.movieticket.controller.admin;

import com.szml.movieticket.dto.SnackProductCreateDTO;
import com.szml.movieticket.dto.SnackProductStatusDTO;
import com.szml.movieticket.dto.SnackProductStockDTO;
import com.szml.movieticket.dto.SnackProductUpdateDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.SnackProductService;
import com.szml.movieticket.vo.SnackProductPageVO;
import com.szml.movieticket.vo.SnackProductVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/snacks")
@RequiredArgsConstructor
public class SnackProductController {

    private final SnackProductService snackProductService;

    @GetMapping
    public Result<SnackProductPageVO> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Long cinemaId,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status) {
        return Result.success(snackProductService.pageProducts(page, size, cinemaId, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<SnackProductVO> detail(@PathVariable Long id) {
        return Result.success(snackProductService.getProduct(id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody SnackProductCreateDTO dto) {
        snackProductService.createProduct(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SnackProductUpdateDTO dto) {
        snackProductService.updateProduct(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody SnackProductStatusDTO dto) {
        snackProductService.updateStatus(id, dto);
        return Result.success();
    }

    @PutMapping("/{id}/stock")
    public Result<Void> updateStock(@PathVariable Long id, @Valid @RequestBody SnackProductStockDTO dto) {
        snackProductService.updateStock(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        snackProductService.deleteProduct(id);
        return Result.success();
    }
}
