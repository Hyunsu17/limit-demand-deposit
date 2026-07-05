package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.domain.AccountOpenApplication;
import com.hyunsu.limitdeposit.account.domain.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * D8 — TX2 실패 시 보상 처리. 새 트랜잭션으로 APP_STATUS를 시스템오류(6)로 전환하여
 * 신청중(PENDING) 상태에 stuck되어 재시도(D7-A)가 막히는 것을 방지한다.
 */
@Service
@RequiredArgsConstructor
public class AccountOpenCompensationService {

    private final AccountOpenApplicationRepository accountOpenApplicationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSystemError(Long applicationId) {
        // [Claude] REQUIRES_NEW — 실패한 TX2와 독립된 새 트랜잭션에서 상태만 갱신해야
        // [Claude] 롤백에 휩쓸리지 않고 SYSTEM_ERROR 마킹이 살아남는다 (D8 핵심)
        AccountOpenApplication application = accountOpenApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST,
                        "보상 대상 신청건을 찾을 수 없습니다: " + applicationId));
        application.markSystemError();
    }
}
