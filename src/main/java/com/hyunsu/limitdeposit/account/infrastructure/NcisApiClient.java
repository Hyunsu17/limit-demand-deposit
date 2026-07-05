package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.NcisCheckResponse;
import com.hyunsu.limitdeposit.account.domain.NcisClient;
import org.springframework.stereotype.Component;

/**
 * NCIS 외부 API 연동 (WireMock으로 대체 예정). connect-timeout 3s / read-timeout 5s (D10).
 */
@Component
public class NcisApiClient implements NcisClient {

    @Override
    public NcisCheckResponse check(Long customerId) {
        // TODO: RestTemplate/WebClient로 NCIS 실명번호 중복확인 API 동기 호출
        // TODO: 타임아웃/통신오류 발생 시 예외를 던지지 말고 NcisCheckResponse(ERROR, ...) 반환 (D10)
        return null;
    }
}
