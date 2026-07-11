package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.domain.account.Account;
import com.hyunsu.limitdeposit.account.domain.account.AccountLedgerHistory;
import com.hyunsu.limitdeposit.account.domain.account.AccountLedgerHistoryRepository;
import com.hyunsu.limitdeposit.account.domain.account.AccountNumberGenerator;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplication;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.account.domain.account.AccountRepository;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckHistory;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckHistoryRepository;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckResult;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import com.hyunsu.limitdeposit.product.domain.Product;
import com.hyunsu.limitdeposit.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * TX2(D4) — NCIS 응답 반영 + 개설 확정.
 * 승인(Y): NCIS 이력 완료 → PROD_MST 정책 조회 → 채번(D5) → ACCT_LEDGER + 최초 이력 INSERT → 신청 승인
 * 반려(N)/통신오류(E): NCIS 이력 완료 + 신청 상태 전이만 수행
 */
@Service
@RequiredArgsConstructor
public class AccountOpenConfirmService {

    private final AccountOpenApplicationRepository accountOpenApplicationRepository;
    private final NcisCheckHistoryRepository ncisCheckHistoryRepository;
    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final AccountLedgerHistoryRepository accountLedgerHistoryRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    /**
     * @return 발급된 계좌번호
     */
    @Transactional
    public String approve(Long applicationId, String resMsg) {
        AccountOpenApplication application = loadApplication(applicationId);
        NcisCheckHistory ncisCheckHistory = loadNcisCheckHistory(applicationId);
        ncisCheckHistory.complete(NcisCheckResult.APPROVED, resMsg);

        // [Claude] PROD_MST에서 유효기간 내 상품 조회 → 기본 입금/지급한도정책ID 취득.
        // [Claude] D6 정정 — 지급한도정책은 계좌 등급(P01/P02) FK로 원장에 저장. 나이 값만 저장 안 함(거래 시 동적 결정)
        Product product = productRepository.findEffective(application.getProdCd(), LocalDate.now())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        String acctNo = accountNumberGenerator.nextAcctNo();
        // [Claude] D7-B — 동시 개설 충돌 시 CUSTOMER_ID UNIQUE 위반 예외가 여기서 발생,
        // [Claude] 트랜잭션 전체 롤백 후 오케스트레이터의 D8 보상으로 이어진다
        Account account = accountRepository.save(Account.open(
                acctNo,
                application.getCustomerId(),
                product.getProdCd(),
                ncisCheckHistory.getId(),
                product.getBaseDepLmtPolicyId(),
                product.getBasePymLmtPolicyId()));

        accountLedgerHistoryRepository.save(
                AccountLedgerHistory.openRecord(account, application.getApplicationChannel()));

        application.approve(acctNo);
        return acctNo;
    }

    @Transactional
    public void reject(Long applicationId, String resMsg) {
        AccountOpenApplication application = loadApplication(applicationId);
        loadNcisCheckHistory(applicationId).complete(NcisCheckResult.REJECTED, resMsg);
        application.reject(resMsg);
    }

    @Transactional
    public void markCommunicationError(Long applicationId, String resMsg) {
        AccountOpenApplication application = loadApplication(applicationId);
        loadNcisCheckHistory(applicationId).complete(NcisCheckResult.ERROR, resMsg);
        application.markCommError();
    }

    private AccountOpenApplication loadApplication(Long applicationId) {
        return accountOpenApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST,
                        "개설 확정 대상 신청건을 찾을 수 없습니다: " + applicationId));
    }

    private NcisCheckHistory loadNcisCheckHistory(Long applicationId) {
        return ncisCheckHistoryRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST,
                        "선적재된 NCIS 이력을 찾을 수 없습니다: " + applicationId));
    }
}
