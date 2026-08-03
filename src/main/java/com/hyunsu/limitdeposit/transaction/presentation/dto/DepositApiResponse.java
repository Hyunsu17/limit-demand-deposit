package com.hyunsu.limitdeposit.transaction.presentation.dto;

import com.hyunsu.limitdeposit.transaction.application.dto.DepositResult;

import java.math.BigDecimal;

/**
 * 입금 성공 응답. 거절은 이 형태로 오지 않고 ErrorResponse(4xx)로 나간다.
 */
public record DepositApiResponse(
        Long txnSeq,
        BigDecimal balanceAfter
) {

    public static DepositApiResponse from(DepositResult result) {
        return new DepositApiResponse(result.txnSeq(), result.balanceAfter());
    }
}
