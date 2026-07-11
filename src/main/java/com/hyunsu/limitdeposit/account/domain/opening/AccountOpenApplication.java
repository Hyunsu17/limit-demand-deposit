package com.hyunsu.limitdeposit.account.domain.opening;

import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ACCT_OPEN_APPLICATION — 계좌개설 신청 이력 (실패 케이스 포함, 절대 삭제하지 않는다)
 */
@Entity
@Table(name = "account_open_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountOpenApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "prod_cd", nullable = false, length = 10)
    private String prodCd;

    @Column(name = "application_dttm", nullable = false)
    private LocalDateTime applicationDttm;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_channel", nullable = false, length = 20)
    private Channel applicationChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_status", nullable = false, length = 20)
    private ApplicationStatus appStatus;

    @Column(name = "reject_rsn", length = 200)
    private String rejectRsn;

    @Column(name = "acct_no", length = 20)
    private String acctNo;

    @Column(name = "complete_dttm")
    private LocalDateTime completeDttm;

    private AccountOpenApplication(Long customerId, String prodCd, Channel applicationChannel) {
        this.customerId = customerId;
        this.prodCd = prodCd;
        this.applicationChannel = applicationChannel;
        this.applicationDttm = LocalDateTime.now();
        this.appStatus = ApplicationStatus.PENDING;
    }

    /**
     * TX1(D4) 진입점 — 로컬 사전검증(D1) 통과 후 호출되는 신청 생성 팩토리 메서드
     */
    public static AccountOpenApplication apply(Long customerId, String prodCd, Channel channel) {
        // TODO: 필드 유효성(고객/상품코드 등)은 상위 검증 서비스 책임 — 여기서는 순수 생성만
        return new AccountOpenApplication(customerId, prodCd, channel);
    }

    public boolean isPending() {
        return this.appStatus == ApplicationStatus.PENDING;
    }

    /**
     * TX2(Week2) — NCIS 승인 + ACCT_LEDGER 개설 확정 이후 호출
     */
    public void approve(String acctNo) {
        requirePending();
        this.acctNo = acctNo;                        // [Claude] 승인 시에만 계좌번호가 채워진다
        this.appStatus = ApplicationStatus.APPROVED;
        this.completeDttm = LocalDateTime.now();
    }

    /**
     * TX2(Week2) — NCIS 결과 N(불가) 수신 시 호출
     */
    public void reject(String reason) {
        requirePending();
        this.rejectRsn = reason;
        this.appStatus = ApplicationStatus.REJECTED;
        this.completeDttm = LocalDateTime.now();
    }

    /**
     * D10 — NCIS 통신오류/타임아웃 수신 시 호출
     */
    public void markCommError() {
        requirePending();
        this.appStatus = ApplicationStatus.COMM_ERROR;
        this.completeDttm = LocalDateTime.now();
    }

    /**
     * D8 — TX2 실패 시 보상 처리로 호출 (PENDING에 stuck되는 것 방지)
     */
    public void markSystemError() {
        requirePending();
        this.appStatus = ApplicationStatus.SYSTEM_ERROR;
        this.completeDttm = LocalDateTime.now();
    }

    // [Claude] 상태 전이 가드 — 종결 처리는 신청중(PENDING)에서만 허용. 위반은 사용자 입력 오류가
    // [Claude] 아니라 흐름/코드 버그이므로 BusinessException이 아닌 IllegalStateException으로 던진다.
    private void requirePending() {
        if (!isPending()) {
            throw new IllegalStateException("신청중(PENDING) 상태에서만 처리할 수 있습니다. 현재: " + appStatus);
        }
    }
}
