package com.szml.movieticket.controller;

import com.szml.movieticket.service.AlipayCallbackService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/** Alipay asynchronous notification and synchronous return endpoints. */
@RestController
@RequestMapping("/api/payment/alipay")
@RequiredArgsConstructor
public class AlipayPaymentController {

    private final AlipayCallbackService alipayCallbackService;

    @PostMapping(value = "/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public String notify(@RequestParam Map<String, String> params) {
        return alipayCallbackService.handleNotification(params);
    }

    @GetMapping("/return")
    public void returnFromAlipay(@RequestParam(required = false) String out_trade_no,
                                 @RequestParam(defaultValue = "false") boolean cancelled,
                                 HttpServletResponse response) throws IOException {
        String target = cancelled
                ? alipayCallbackService.buildFrontendReturnUrl(out_trade_no, true)
                : alipayCallbackService.buildFrontendReturnUrl(out_trade_no);
        response.sendRedirect(target);
    }
}
