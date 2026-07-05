package com.hyunsu.limitdeposit.account.domain;

/**
 * NCIS(정보집중기관) 실명번호 중복확인 포트 (D3: 동기 호출)
 */
public interface NcisClient {

    /**
     * 타임아웃/통신오류 시에도 예외를 던지지 않고 ERROR 결과로 반환한다 (D10)
     */
    NcisCheckResponse check(Long customerId);
}
