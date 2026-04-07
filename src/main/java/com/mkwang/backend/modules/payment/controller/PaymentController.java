package com.mkwang.backend.modules.payment.controller;

import com.mkwang.backend.common.dto.ApiResponse;
import com.mkwang.backend.modules.payment.dto.request.PaymentCancelRequest;
import com.mkwang.backend.modules.payment.dto.request.PaymentRequest;
import com.mkwang.backend.modules.payment.dto.response.PaymentCallbackResult;
import com.mkwang.backend.modules.payment.dto.response.PaymentCancelResponse;
import com.mkwang.backend.modules.payment.dto.response.PaymentResponse;
import com.mkwang.backend.modules.payment.dto.response.PaymentStatusResponse;
import com.mkwang.backend.modules.payment.enums.PaymentGateway;
import com.mkwang.backend.modules.payment.exception.UnsupportedPaymentGatewayException;
import com.mkwang.backend.modules.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Create payment successfully", paymentService.createPayment(request)));
    }

    @GetMapping("/{gateway}/ipn")
    public ResponseEntity<ApiResponse<PaymentCallbackResult>> ipnCallback(
            @PathVariable String gateway,
            @RequestParam Map<String, String> params,
            HttpServletRequest request
    ) {
        PaymentGateway paymentGateway = parseGateway(gateway);

        Map<String, String> logParams = new HashMap<>(params);
        logParams.remove("vnp_SecureHash");

        log.info(
                "VNPay IPN received: gateway={}, remoteIp={}, txRef={}, amount={}, responseCode={}, transactionStatus={}, paramsWithoutHash={}",
                paymentGateway,
                request.getRemoteAddr(),
                params.get("vnp_TxnRef"),
                params.get("vnp_Amount"),
                params.get("vnp_ResponseCode"),
                params.get("vnp_TransactionStatus"),
                logParams
        );

        PaymentCallbackResult result = paymentService.handleCallback(paymentGateway, params);

        log.info(
                "VNPay IPN processed: gateway={}, txRef={}, success={}, message={}",
                paymentGateway,
                result.getTransactionRef(),
                result.isSuccess(),
                result.getMessage()
        );

        return ResponseEntity.ok(ApiResponse.success("Handle callback successfully", result));
    }

    @GetMapping("/{gateway}/return")
    public ResponseEntity<ApiResponse<PaymentCallbackResult>> returnCallback(
            @PathVariable String gateway,
            @RequestParam Map<String, String> params
    ) {
        PaymentGateway paymentGateway = parseGateway(gateway);
        PaymentCallbackResult result = paymentService.handleCallback(paymentGateway, params);
        return ResponseEntity.ok(ApiResponse.success("Handle callback successfully", result));
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<PaymentCancelResponse>> cancelPayment(
            @Valid @RequestBody PaymentCancelRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Cancel payment processed", paymentService.cancelPayment(request)));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> checkStatus(
            @RequestParam PaymentGateway gateway,
            @RequestParam String transactionRef
    ) {
        PaymentStatusResponse response = paymentService.checkStatus(gateway, transactionRef);
        return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", response));
    }

    private PaymentGateway parseGateway(String gateway) {
        try {
            return PaymentGateway.valueOf(gateway.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedPaymentGatewayException(gateway);
        }
    }
}
