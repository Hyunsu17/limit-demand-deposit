package com.hyunsu.limitdeposit.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        // [Claude] 4xx는 정상적인 업무 거절이라 로그를 남기지 않는다(한도초과·계좌없음 등이 에러 로그를 채운다).
        // [Claude] 5xx는 이름만 BusinessException 일 뿐 시스템 오류이므로 스택을 남긴다
        if (errorCode.getHttpStatus().is5xxServerError()) {
            log.error("시스템 오류 — code={}", errorCode.getCode(), e);
        }

        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.INVALID_REQUEST.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message));
    }

    /**
     * [Claude] 본문 자체를 못 읽는 경우 — 깨진 JSON, enum 에 없는 값(channelType: "FOO"), 타입 불일치.
     * 클라이언트 잘못이므로 400 이다. 핸들러가 없으면 아래 handleException 으로 떨어져 500 이 나가는데,
     * 대외 수신 API(입금)에서는 상대 기관이 "내 전문이 틀렸다"와 "우리 시스템이 죽었다"를 구분해야 한다.
     *
     * <p>파싱 실패 상세는 내부 필드명·타입을 드러내므로 응답에 싣지 않고 로그로만 남긴다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문 파싱 실패 — {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, "요청 본문을 해석할 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        // [Claude] 예상 못 한 예외는 여기가 마지막 관문이다 — 삼키면 원인을 알 방법이 없다.
        // [Claude] 특히 입금 TX2 가 죽으면 원본이 PENDING 으로 남고 재처리 배치가 회수하는 설계인데(2026-07-22 Q5),
        // [Claude] 스택이 없으면 배치가 같은 이유로 재실패하는 것을 진단할 수 없다
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."));
    }
}
