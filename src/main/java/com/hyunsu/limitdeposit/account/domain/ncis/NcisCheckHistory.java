package com.hyunsu.limitdeposit.account.domain.ncis;

import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * NCIS_CHECK_HIST — 1인1계좌 중복확인(정보집중기관) 이력.
 * TX1에서 P(처리중)로 선적재 후, NCIS 동기 호출 결과를 TX2에서 반영한다 (D4).
 */
@Entity
@Table(name = "ncis_check_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NcisCheckHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ncis_check_id")
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "req_dttm", nullable = false)
    private LocalDateTime reqDttm;

    @Column(name = "res_dttm")
    private LocalDateTime resDttm;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_result", nullable = false, length = 20)
    private NcisCheckResult checkResult;

    @Column(name = "res_msg", length = 200)
    private String resMsg;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_channel", length = 20)
    private Channel checkChannel;

    private NcisCheckHistory(Long applicationId, Channel checkChannel) {
        this.applicationId = applicationId;
        this.checkChannel = checkChannel;
        this.reqDttm = LocalDateTime.now();
        this.checkResult = NcisCheckResult.PROCESSING;
    }

    /**
     * TX1(D4) — NCIS 호출 전 P(처리중) 상태로 선적재
     */
    public static NcisCheckHistory preRecord(Long applicationId, Channel channel) {
        return new NcisCheckHistory(applicationId, channel);
    }

    /**
     * TX2(D4) — NCIS 동기 호출 응답을 반영
     */
    public void complete(NcisCheckResult result, String resMsg) {
        // [Claude] 응답값이 PROCESSING인 것은 NCIS 규약 위반 — 잘못된 인자이므로 IllegalArgumentException
        if (result == NcisCheckResult.PROCESSING) {
            throw new IllegalArgumentException("NCIS 응답 결과는 처리중(PROCESSING)일 수 없습니다");
        }
        // [Claude] 이력 불변성 — 한 번 완료된 이력의 재완료는 흐름/코드 버그이므로 IllegalStateException
        // [Claude] (AccountOpenApplication.requirePending과 동일한 컨벤션)
        if (this.checkResult != NcisCheckResult.PROCESSING) {
            throw new IllegalStateException("이미 완료된 NCIS 이력은 다시 완료할 수 없습니다. 현재: " + this.checkResult);
        }
        this.resDttm = LocalDateTime.now();
        this.checkResult = result;
        this.resMsg = resMsg;
    }
}
