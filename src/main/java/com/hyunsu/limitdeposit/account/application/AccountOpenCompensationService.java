package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplication;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * D8 — TX2 실패 시 보상 처리. 새 트랜잭션으로 신청 상태를 전환하여
 * 신청중(PENDING) 상태에 stuck되어 재시도(D7-A)가 막히는 것을 방지한다.
 */
@Service
@RequiredArgsConstructor
public class AccountOpenCompensationService {

    // [Claude] D7-B — 동시 개설 경합으로 CUSTOMER_ID UNIQUE 위반이 발생한 경우의 반려 사유
    private static final String DUPLICATE_REJECT_REASON = "동시 개설 요청으로 인한 계좌 중복(D7-B)";

    private final AccountOpenApplicationRepository accountOpenApplicationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSystemError(Long applicationId) {
        // [Claude] REQUIRES_NEW — 실패한 TX2와 독립된 새 트랜잭션에서 상태만 갱신해야
        // [Claude] 롤백에 휩쓸리지 않고 SYSTEM_ERROR 마킹이 살아남는다 (D8 핵심)
        loadApplication(applicationId).markSystemError();
    }

    /**
     * D7-B — TX2에서 CUSTOMER_ID UNIQUE 위반(동시 개설 경합)이 발생한 경우.
     * 시스템오류가 아닌 반려(REJECTED)로 마킹한다 — 재시도해도 D2에서 동일하게 막힐 정상적인 비즈니스 상태이기 때문.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectDuplicate(Long applicationId) {
        loadApplication(applicationId).reject(DUPLICATE_REJECT_REASON);
    }

    private AccountOpenApplication loadApplication(Long applicationId) {
        return accountOpenApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST,
                        "보상 대상 신청건을 찾을 수 없습니다: " + applicationId));
    }
}
