package com.szml.movieticket.controller.user;

import com.szml.movieticket.context.UserContext;
import com.szml.movieticket.dto.OrderSnackSelectionDTO;
import com.szml.movieticket.result.Result;
import com.szml.movieticket.service.OrderSnackService;
import com.szml.movieticket.vo.SnackSelectionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/orders/{orderId}/snacks")
@RequiredArgsConstructor
public class OrderSnackController {

    private final OrderSnackService orderSnackService;

    @GetMapping
    public Result<SnackSelectionVO> get(@PathVariable Long orderId) {
        return Result.success(orderSnackService.getSelection(UserContext.getUserId(), orderId));
    }

    @PutMapping
    public Result<SnackSelectionVO> replace(@PathVariable Long orderId,
                                            @Valid @RequestBody OrderSnackSelectionDTO dto) {
        return Result.success(orderSnackService.replaceSelection(UserContext.getUserId(), orderId, dto));
    }
}
