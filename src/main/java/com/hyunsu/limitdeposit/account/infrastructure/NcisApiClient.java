package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckResponse;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckResult;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * NCIS 외부 API 연동 (테스트에서는 WireMock이 이 엔드포인트를 대체한다).
 * D10 — connect-timeout 3s / read-timeout 5s, 타임아웃/통신오류는 예외 없이 ERROR 결과로 변환.
 */
@Slf4j
@Component
public class NcisApiClient implements NcisClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final String DUPLICATE_CHECK_URI = "/api/ncis/duplicate-check";

    private final RestClient restClient;

    public NcisApiClient(@Value("${ncis.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public NcisCheckResponse check(Long customerId) {
        try {
            NcisApiResponse response = restClient.post()
                    .uri(DUPLICATE_CHECK_URI)
                    .body(new NcisApiRequest(customerId))
                    .retrieve()
                    .body(NcisApiResponse.class);
            if (response == null) {
                return new NcisCheckResponse(NcisCheckResult.ERROR, "NCIS 응답 본문이 비어 있습니다");
            }
            return new NcisCheckResponse(toResult(response.result()), response.message());
        } catch (RestClientException e) {
            // [Claude] D10 — 타임아웃/4xx/5xx/커넥션 실패 모두 통신오류(E)로 수렴
            log.warn("NCIS 통신 오류. customerId={}", customerId, e);
            return new NcisCheckResponse(NcisCheckResult.ERROR, "NCIS 통신 오류");
        }
    }

    private NcisCheckResult toResult(String code) {
        return switch (code) {
            case "Y" -> NcisCheckResult.APPROVED;
            case "N" -> NcisCheckResult.REJECTED;
            case null, default -> NcisCheckResult.ERROR;
        };
    }

    // [Claude] NCIS 전문 규격 (WireMock 스텁도 이 규격을 따른다)
    record NcisApiRequest(Long customerId) {
    }

    record NcisApiResponse(String result, String message) {
    }
}
