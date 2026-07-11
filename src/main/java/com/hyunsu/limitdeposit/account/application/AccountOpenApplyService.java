package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.application.dto.AccountOpenRequest;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplication;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckHistory;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TX1(D4) — 로컬 검증(D1) 통과 후 신청 등록 + NCIS 이력 선적재.
 * NCIS 동기 호출은 이 트랜잭션 밖에서 이루어진다 (커넥션 점유 방지).
 */
@Service
@RequiredArgsConstructor
public class AccountOpenApplyService {

    private final AccountOpenValidationService validationService;
    private final AccountOpenApplicationRepository accountOpenApplicationRepository;
    private final NcisCheckHistoryRepository ncisCheckHistoryRepository;

    /**
     * @return 생성된 신청 ID (이후 NCIS 호출 및 TX2에서 참조)
     */
    @Transactional
    public Long apply(AccountOpenRequest request) {
        // [Claude] D1 로컬 사전검증 — 실패 시 여기서 예외로 롤백되어 신청 자체가 남지 않는다
        validationService.validate(request.customerId());

        // [Claude] 신청 레코드 INSERT (APP_STATUS=PENDING). save 후 id가 채번된다
        AccountOpenApplication application = AccountOpenApplication.apply(
                request.customerId(), request.prodCd(), request.channel());
        accountOpenApplicationRepository.save(application);

        // [Claude] NCIS 이력 선적재 (CHECK_RESULT=PROCESSING). 아직 외부 호출 전 — 추적 근거만 남긴다
        NcisCheckHistory ncisCheckHistory = NcisCheckHistory.preRecord(
                application.getId(), request.channel());
        ncisCheckHistoryRepository.save(ncisCheckHistory);

        return application.getId();
    }
}
