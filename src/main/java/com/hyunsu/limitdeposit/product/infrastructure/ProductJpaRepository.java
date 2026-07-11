package com.hyunsu.limitdeposit.product.infrastructure;

import com.hyunsu.limitdeposit.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

interface ProductJpaRepository extends JpaRepository<Product, Long> {

    @Query("""
            SELECT p FROM Product p
            WHERE p.prodCd = :prodCd
              AND p.appySttDt <= :baseDate
              AND (p.appyEndDt IS NULL OR p.appyEndDt >= :baseDate)
            """)
    Optional<Product> findEffective(@Param("prodCd") String prodCd, @Param("baseDate") LocalDate baseDate);
}
