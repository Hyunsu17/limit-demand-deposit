package com.hyunsu.limitdeposit.product.infrastructure;

import com.hyunsu.limitdeposit.product.domain.Product;
import com.hyunsu.limitdeposit.product.domain.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    @Override
    public Optional<Product> findEffective(String prodCd, LocalDate baseDate) {
        return jpaRepository.findEffective(prodCd, baseDate);
    }
}
