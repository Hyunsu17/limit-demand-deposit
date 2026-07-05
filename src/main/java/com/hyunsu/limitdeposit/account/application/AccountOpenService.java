package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.application.dto.AccountOpenRequest;
import com.hyunsu.limitdeposit.account.domain.NcisCheckResponse;
import com.hyunsu.limitdeposit.account.domain.NcisClient;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 계좌개설 전체 플로우 오케스트레이션 (D4).
 * TX1 commit → NCIS 동기 호출(트랜잭션 밖) → TX2(Week2에서 구현, ACCT_LEDGER 확정)
 * 의도적으로 클래스 레벨 @Transactional을 두지 않는다 — 트랜잭션 경계는 각 단계 서비스가 스스로 가진다.
 */
@Service
@RequiredArgsConstructor
public class AccountOpenService {

    private final AccountOpenApplyService applyService;
    private final NcisClient ncisClient;
    private final AccountOpenCompensationService compensationService;
    // TODO: Week2 — private final AccountOpenConfirmService confirmService; (TX2: ACCT_LEDGER 개설 확정)

    public void openAccount(AccountOpenRequest request) {
        // [Claude] TX1 — 신청 등록 + NCIS 이력 선적재. 여기까지 커밋되어 추적 근거가 남는다
        Long applicationId = applyService.apply(request);

        // [Claude] NCIS 동기 호출 — 트랜잭션 밖(커넥션 점유 방지, D3/D4).
        // [Claude] 타임아웃/통신오류도 예외 없이 ERROR 결과로 돌아온다 (D10)
        NcisCheckResponse response = ncisClient.check(request.customerId());

        // [Claude] NCIS 결과 분기
        switch (response.result()) {
            case APPROVED ->
                // [Claude] TX2(Week2) 자리 — confirmService.confirm(applicationId, ...)로 ACCT_LEDGER 개설 확정,
                // [Claude] 실패 시 compensationService.markSystemError(applicationId) 보상(D8).
                // [Claude] Account(ACCT_LEDGER) 엔티티가 아직 없어 Week2에서 구현한다.
                    throw new UnsupportedOperationException(
                            "TX2(계좌 개설 확정)는 Week2에서 구현 예정 — applicationId=" + applicationId);
            case REJECTED ->
                // [Claude] TODO Week2: NCIS 결과(N)를 NcisCheckHistory.complete + application.reject로 반영 후 통지
                    throw new BusinessException(ErrorCode.NCIS_CHECK_REJECTED);
            case ERROR ->
                // [Claude] TODO Week2: NCIS 결과(E)를 NcisCheckHistory.complete + application.markCommError로 반영
                    throw new BusinessException(ErrorCode.NCIS_COMMUNICATION_ERROR);
            case PROCESSING ->
                // [Claude] NCIS 응답이 P(처리중)로 돌아오는 것은 규약 위반 — 방어적 처리
                    throw new IllegalStateException("NCIS 응답이 처리중(PROCESSING)으로 반환될 수 없습니다");
        }
    }
}
