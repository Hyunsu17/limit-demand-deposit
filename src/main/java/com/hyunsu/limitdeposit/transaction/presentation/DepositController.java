package com.hyunsu.limitdeposit.transaction.presentation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import com.hyunsu.limitdeposit.transaction.application.DepositService;
import com.hyunsu.limitdeposit.transaction.application.dto.DepositCommand;
import com.hyunsu.limitdeposit.transaction.application.dto.DepositResult;
import com.hyunsu.limitdeposit.transaction.domain.ProcessFailReason;
import com.hyunsu.limitdeposit.transaction.presentation.dto.DepositApiRequest;
import com.hyunsu.limitdeposit.transaction.presentation.dto.DepositApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {

    private final DepositService depositService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<DepositApiResponse> deposit(@RequestBody @Valid DepositApiRequest request) {

        DepositCommand command = new DepositCommand(
                request.getAcctNo(),
                request.getAmount(),
                request.getChannelType(),
                request.getDescription(),
                toRawData(request));

        DepositResult result = depositService.deposit(command);

        if (result.isRejected()) {
            // [Claude] 여기서의 throw 는 안전하다 — 처리 TX 는 이미 커밋됐고(거절도 정상 반환이었으므로)
            // [Claude] TRANS_RAW 의 FAILED + 사유가 DB 에 남은 뒤다. Service 안에서 던졌다면
            // [Claude] 그 마킹까지 롤백됐을 것이고, 그것이 2026-07-30 Q5 가 결과 객체를 택한 이유다.
            throw new BusinessException(toErrorCode(result.failReason()));
        }

        return ResponseEntity.ok(DepositApiResponse.from(result));
    }

    /**
     * [Claude] TRANS_RAW.raw_data — 요청 본문을 JSON 문자열로 적재한다(2026-07-30 Q6).
     * 금융결제원 전문 포맷을 흉내내지 않는다.
     */
    private String toRawData(DepositApiRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            // [Claude] 자기 DTO 직렬화가 실패하는 것은 업무 오류가 아니라 배선 오류다
            throw new IllegalStateException("입금 요청 원본 직렬화에 실패했습니다.", e);
        }
    }

    private ErrorCode toErrorCode(ProcessFailReason failReason) {
        return switch (failReason) {
            case ACCOUNT_NOT_FOUND -> ErrorCode.ACCOUNT_NOT_FOUND;
            case ACCOUNT_NOT_ACTIVE -> ErrorCode.ACCOUNT_NOT_ACTIVE;
            case DEPOSIT_LIMIT_EXCEEDED -> ErrorCode.DEPOSIT_LIMIT_EXCEEDED;
            // [Claude] 입금은 SYSTEM_ERROR 로 "거절"하지 않는다 — 처리 중 예상 못 한 실패는 예외로 터져
            // [Claude] 원본이 PENDING 으로 남고 재처리 배치가 회수한다(2026-07-22 Q5). 여기 도달하면 배선 오류
            case SYSTEM_ERROR -> throw new IllegalStateException(
                    "입금 거절 사유로 SYSTEM_ERROR 가 반환될 수 없습니다.");
        };
    }
}
