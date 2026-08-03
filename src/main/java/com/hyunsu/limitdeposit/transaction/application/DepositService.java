package com.hyunsu.limitdeposit.transaction.application;

import com.hyunsu.limitdeposit.transaction.application.dto.DepositCommand;
import com.hyunsu.limitdeposit.transaction.application.dto.DepositResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 입금 전체 플로우 오케스트레이션. TX1(원본 선적재) commit → TX2(락·한도판단·원장반영).
 *
 * <p>[Claude] 계좌개설(AccountOpenService)과 같이 클래스 레벨 @Transactional 을 두지 않는다 —
 * 두면 두 단계가 한 트랜잭션으로 합쳐져 선적재 분리가 무의미해진다. 경계는 각 단계 서비스가 스스로 가진다.
 *
 * <p>[Claude] 계좌개설과 달리 <b>보상 서비스를 두지 않는다</b>(2026-07-22 Q5). TX2 가 죽으면 원본은
 * PENDING 으로 남고 재처리 배치가 회수한다 — 개설은 신청이 PENDING 에 stuck 되면 고객 재시도가
 * D7-A 에 막히지만, 입금은 사람 개입 없이 배치가 이어받을 수 있기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class DepositService {

    private final TransactionRawPreloadService preloadService;
    private final DepositProcessService processService;

    public DepositResult deposit(DepositCommand command) {
        // [Claude] TX1 — 여기까지 커밋되어 성공/실패와 무관하게 부인방지 근거가 남는다
        Long rawSeq = preloadService.preload(
                command.channelType(), command.acctNo(), command.amount(), command.rawData());

        // [Claude] TX2 — 거절도 정상 반환값으로 돌아온다(예외 아님). 호출자가 명시적으로 분기한다
        return processService.process(rawSeq, command);
    }
}
