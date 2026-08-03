package com.hyunsu.limitdeposit.transaction.application;

import com.hyunsu.limitdeposit.account.domain.account.Account;
import com.hyunsu.limitdeposit.account.domain.account.AccountRepository;
import com.hyunsu.limitdeposit.account.domain.account.vo.DepositLimit;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import com.hyunsu.limitdeposit.product.domain.DepositLimitPolicy;
import com.hyunsu.limitdeposit.product.domain.DepositLimitPolicyRepository;
import com.hyunsu.limitdeposit.transaction.application.dto.DepositCommand;
import com.hyunsu.limitdeposit.transaction.application.dto.DepositResult;
import com.hyunsu.limitdeposit.transaction.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

/**
 * TX2 — 입금 처리. 선적재(TX1)가 커밋된 뒤 별도 트랜잭션으로 실행된다.
 *
 * <p>[Claude] 입금프로세스 STEP 1~2 의 구현: ACCT_LEDGER 단일 락 → 계좌상태·2중한도 판단 →
 * CASE 1(비정상)·CASE 2(성공)·CASE 3(한도초과) 분기.
 *
 * <p>[Claude] 세 갈래 중 거절 두 개는 예외가 아니라 {@link DepositResult} 로 반환한다 —
 * 예외를 던지면 같은 TX 의 TRANS_RAW FAILED 마킹까지 롤백된다(2026-07-30 Q5).
 *
 * <p>[Claude] 반대로 <b>예상 못 한 실패는 예외로 터뜨리는 것이 맞다</b>. 그 경우 원본은 PENDING 으로
 * 남고 재처리 배치가 회수 주체가 된다(2026-07-22 Q5 — 계좌개설과 달리 보상 서비스를 두지 않는 이유).
 */
@Service
@RequiredArgsConstructor
public class DepositProcessService {

    private final AccountRepository accountRepository;
    private final TransactionRawRepository transactionRawRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final DepositLimitPolicyRepository depositLimitPolicyRepository;

    @Transactional
    public DepositResult process(Long rawSeq, DepositCommand command) {
        // [Claude] 선적재는 이미 커밋됐다 — 여기서 못 찾으면 배선 오류이지 업무 거절이 아니다
        TransactionRaw raw = transactionRawRepository.findById(rawSeq)
                .orElseThrow(() -> new IllegalStateException(
                        "선적재된 거래원본을 찾을 수 없습니다. rawSeq=" + rawSeq));

        // [Claude] STEP 1 — ACCT_LEDGER 를 가장 먼저 잠근다(락 규칙 1·2). 월누계는 원장 없이
        // [Claude] TRANS_HISTORY 실시간 SUM 이므로 이 거래가 잠그는 행은 이 하나뿐이다
        Optional<Account> found = accountRepository.findByAcctNoForUpdate(command.acctNo());
        if (found.isEmpty()) {
            return reject(raw, ProcessFailReason.ACCOUNT_NOT_FOUND);
        }
        Account account = found.get();

        // [Claude] CASE 1 — 해지/동결 계좌. Account.deposit() 안의 가드에 맡기지 않고 여기서 먼저 묻는다.
        // [Claude] 그 가드는 BusinessException 을 던지므로 태우면 FAILED 마킹이 롤백된다
        if (!account.isActive()) {
            return reject(raw, ProcessFailReason.ACCOUNT_NOT_ACTIVE);
        }

        BigDecimal depositable = calculateDepositableAmount(account);

        // [Claude] CASE 3 — 한도 초과. 반송(return leg) 거래를 만들지 않고 거절로 종결한다(2026-07-28 결정)
        if (command.amount().compareTo(depositable) > 0) {
            return reject(raw, ProcessFailReason.DEPOSIT_LIMIT_EXCEEDED);
        }

        // [Claude] CASE 2 — 입금 확정. account/raw 는 이 TX 에서 읽은 영속 엔티티라 더티체킹으로 UPDATE 된다
        account.deposit(command.amount());

        TransactionHistory history = transactionHistoryRepository.save(TransactionHistory.record(
                rawSeq,
                command.acctNo(),
                TransactionCodes.DEPOSIT,
                command.amount(),
                account.getBalance(),   // 반영 후 잔액
                command.channelType(),
                command.description()));

        raw.markCompleted();

        return DepositResult.success(rawSeq, history.getId(), account.getBalance());
    }

    /**
     * 2중 한도 — MIN(보관한도 − 잔액, 월한도 − 당월누계). 판단 자체는 {@link DepositLimit} VO 가 소유하고
     * 여기서는 그 계산에 필요한 데이터(정책·누계)를 수집하기만 한다(2026-07-22 Q1).
     */
    private BigDecimal calculateDepositableAmount(Account account) {
        LocalDate today = LocalDate.now();

        // [Claude] 정책 부재는 기준정보 누락(시스템 오류)이지 고객 거절 사유가 아니다 —
        // [Claude] 예외로 터뜨려 원본을 PENDING 으로 남기고 재처리 대상이 되게 한다
        DepositLimitPolicy policy = depositLimitPolicyRepository
                .findEffective(account.getDepLmtPolicyId(), today)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEPOSIT_POLICY_NOT_FOUND));

        // [Claude] 당월 = 역월(1일~말일) — 2026-07-30 Q7
        YearMonth thisMonth = YearMonth.from(today);
        BigDecimal monthlyAccumulated = transactionHistoryRepository.sumDepositAmount(
                account.getAcctNo(), thisMonth.atDay(1), thisMonth.atEndOfMonth());

        return DepositLimit.of(policy.getBalLmtAmt(), policy.getMonthlyDpLmtAmt())
                .depositableAmount(account.getBalance(), monthlyAccumulated);
    }

    private DepositResult reject(TransactionRaw raw, ProcessFailReason failReason) {
        raw.markFailed(failReason);
        return DepositResult.rejected(raw.getId(), failReason);
    }
}
