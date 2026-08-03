package com.hyunsu.limitdeposit.transaction.presentation.dto;

import com.hyunsu.limitdeposit.transaction.domain.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 대외 입금 요청 전문. 타행/송금기관이 계좌번호를 직접 지정해 보낸다(고객 인증 기반이 아님).
 *
 * <p>[Claude] @Valid 는 HTTP 400 메시지를 위한 것이고, 불변식의 정본은 DepositCommand 생성자다
 * (2026-07-21 결정 ①). 웹 계층을 거치지 않는 호출자가 생겨도 검증이 새지 않게 하기 위함.
 */
@Getter
public class DepositApiRequest {

    @NotBlank(message = "계좌번호는 필수입니다.")
    private String acctNo;

    @NotNull(message = "입금액은 필수입니다.")
    @Positive(message = "입금액은 0보다 커야 합니다.")
    private BigDecimal amount;

    // [Claude] 개설(Channel)과 달리 서버가 고정하지 않는다 — 입금은 유입 경로(ATM/타행/영업점)가 실제로 갈리고
    // [Claude] 그 값이 TRANS_HISTORY 에 그대로 남아야 거래내역이 경로를 설명할 수 있다
    @NotNull(message = "거래채널은 필수입니다.")
    private ChannelType channelType;

    /** 적요 — 송금인명 등. 선택 */
    private String description;
}
