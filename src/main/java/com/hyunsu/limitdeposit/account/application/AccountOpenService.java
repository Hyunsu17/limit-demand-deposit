package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.application.dto.AccountOpenRequest;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckResponse;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisClient;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 계좌개설 전체 플로우 오케스트레이션 (D4).
 * TX1 commit → NCIS 동기 호출(트랜잭션 밖) → TX2(NCIS 결과 반영 + ACCT_LEDGER 개설 확정)
 * 의도적으로 클래스 레벨 @Transactional을 두지 않는다 — 트랜잭션 경계는 각 단계 서비스가 스스로 가진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountOpenService {

    private final AccountOpenApplyService applyService;
    private final NcisClient ncisClient;
    private final AccountOpenConfirmService confirmService;
    private final AccountOpenCompensationService compensationService;

    public void openAccount(AccountOpenRequest request) {
        // [Claude] TX1 — 신청 등록 + NCIS 이력 선적재. 여기까지 커밋되어 추적 근거가 남는다
        Long applicationId = applyService.apply(request);

        // [Claude] NCIS 동기 호출 — 트랜잭션 밖(커넥션 점유 방지, D3/D4).
        // [Claude] 타임아웃/통신오류도 예외 없이 ERROR 결과로 돌아온다 (D10)
        NcisCheckResponse response = ncisClient.check(request.customerId());

        // [Claude] NCIS 결과 분기 — 어느 경로든 TX2에서 NCIS 이력(Y/N/E) + 신청 상태를 반영한다
        switch (response.result()) {
            case APPROVED -> confirmApproved(applicationId, response.message());
            case REJECTED -> {
                confirmService.reject(applicationId, response.message());
                throw new BusinessException(ErrorCode.NCIS_CHECK_REJECTED);
            }
            case ERROR -> {
                confirmService.markCommunicationError(applicationId, response.message());
                throw new BusinessException(ErrorCode.NCIS_COMMUNICATION_ERROR);
            }
            case PROCESSING ->
                // [Claude] NCIS 응답이 P(처리중)로 돌아오는 것은 규약 위반 — 방어적 처리
                    throw new IllegalStateException("NCIS 응답이 처리중(PROCESSING)으로 반환될 수 없습니다");
        }
    }

    private void confirmApproved(Long applicationId, String resMsg) {
        try {
            confirmService.approve(applicationId, resMsg);
        } catch (DataIntegrityViolationException e) {
            // [Claude] D7-B — CUSTOMER_ID UNIQUE 위반은 시스템오류가 아니라 동시 개설 경합에 의한
            // [Claude] 정상적인 중복 상태. 재시도해도 D2에서 동일하게 막히므로 그 결과를 그대로 알려준다
            // TODO 커밋시점에 DataIntegrityViolationException이 아니라 TransactionSystemException로 감싸질 수 있음
            // 확인필요
            log.warn("TX2(계좌 개설 확정) 실패 — 동시 개설 경합(D7-B). applicationId={}", applicationId, e);
            compensationService.rejectDuplicate(applicationId);
            throw new BusinessException(ErrorCode.DUPLICATE_ACCOUNT);
        } catch (Exception e) {
            // [Claude] D8 — TX2 실패 시 새 트랜잭션으로 시스템오류 마킹.
            // [Claude] PENDING에 stuck되면 D7-A 검증에 막혀 고객 재시도가 불가능해진다
            log.error("TX2(계좌 개설 확정) 실패 — 보상 처리 수행. applicationId={}", applicationId, e);
            compensationService.markSystemError(applicationId);
            throw new BusinessException(ErrorCode.ACCOUNT_OPEN_FAILED);
        }
    }
}
