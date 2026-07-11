package com.hyunsu.limitdeposit.product.domain;

import java.time.LocalDate;
import java.util.Optional;

public interface ProductRepository {

    /**
     * 기준일에 유효한 상품(금리 구간) 조회 — APPY_STT_DT <= 기준일 <= APPY_END_DT(NULL이면 무기한)
     */
    Optional<Product> findEffective(String prodCd, LocalDate baseDate);
}
