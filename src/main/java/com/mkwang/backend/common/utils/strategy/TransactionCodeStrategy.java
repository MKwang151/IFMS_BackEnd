package com.mkwang.backend.common.utils.strategy;

import com.mkwang.backend.common.utils.BusinessCodeStrategy;
import com.mkwang.backend.common.utils.BusinessCodeType;
import com.mkwang.backend.common.utils.CodeFormatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * TXN-{8 hex} → TXN-8829145A (random, no DB call)
 */
@Service
@RequiredArgsConstructor
public class TransactionCodeStrategy implements BusinessCodeStrategy {

    private final CodeFormatUtils formatUtils;

    @Override
    public BusinessCodeType getType() {
        return BusinessCodeType.TRANSACTION;
    }

    @Override
    public String generate(long sequence, String... params) {
        return "TXN-" + formatUtils.randomHex(8);
    }
}
