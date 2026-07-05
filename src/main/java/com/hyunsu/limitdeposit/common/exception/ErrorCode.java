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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),

    CUSTOMER_NOT_FOUND(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "고객을 찾을 수 없습니다."),
    UNDER_AGE(HttpStatus.BAD_REQUEST, "UNDER_AGE", "가입 가능 연령(만 14세) 미만입니다."),
    APPLICATION_IN_PROGRESS(HttpStatus.CONFLICT, "APPLICATION_IN_PROGRESS", "이미 진행 중인 계좌개설 신청이 있습니다."),
    NCIS_CHECK_REJECTED(HttpStatus.CONFLICT, "NCIS_CHECK_REJECTED", "정보집중기관 확인 결과 개설이 불가합니다."),
    NCIS_COMMUNICATION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "NCIS_COMMUNICATION_ERROR", "정보집중기관 통신 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
