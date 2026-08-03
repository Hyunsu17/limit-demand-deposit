package com.hyunsu.limitdeposit.transaction.application.dto;

import com.hyunsu.limitdeposit.transaction.domain.ChannelType;

import java.math.BigDecimal;

/**
 * 입금 요청 입력 모델. 웹 계층의 {@code DepositApiRequest} 를 application 경계에서 받는 형태로 변환한 것.
 *
 * <p>[Claude] 검증 2단계 중 ① 입력값 검증(엔티티 상태가 필요 없는 것)이 여기 산다(2026-07-21 결정).
 * 생성자를 통과한 커맨드는 "계좌번호가 있고 금액이 양수"임을 보장하므로, 아래 Service·도메인이
 * 그 사실을 암묵적 사전조건으로 가정하지 않아도 된다.
 *
 * <p>[Claude] {@code rawData} 는 TRANS_RAW 선적재용 요청 원본 JSON(2026-07-30 Q6).
 * 잔액·한도 판단에는 쓰이지 않고 부인방지 근거로만 보존된다.
 */
public record DepositCommand(
        String acctNo,
        BigDecimal amount,
        ChannelType channelType,
        String description,
        String rawData
) {

    public DepositCommand {
        if (acctNo == null || acctNo.isBlank()) {
            throw new IllegalArgumentException("계좌번호는 필수입니다.");
        }
        // [Claude] 0원 입금도 막는다 — 원장이 움직이지 않는 거래를 TRANS_HISTORY 에 남기면
        // [Claude] "거래내역 = 원장이 실제로 움직인 것"(2026-07-28 결정)이 깨진다
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("입금액은 0보다 커야 합니다.");
        }
        if (channelType == null) {
            throw new IllegalArgumentException("거래채널은 필수입니다.");
        }
        if (rawData == null || rawData.isBlank()) {
            throw new IllegalArgumentException("거래원본(raw_data)은 필수입니다.");
        }
    }
}
