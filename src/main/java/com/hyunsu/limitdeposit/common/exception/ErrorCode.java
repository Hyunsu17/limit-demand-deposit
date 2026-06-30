package com.hyunsu.limitdeposit.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", "잔액이 부족합니다."),
    DUPLICATE_ACCOUNT(HttpStatus.CONFLICT, "DUPLICATE_ACCOUNT", "이미 존재하는 계좌입니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.CONFLICT, "LOCK_ACQUISITION_FAILED", "락 획득에 실패했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
