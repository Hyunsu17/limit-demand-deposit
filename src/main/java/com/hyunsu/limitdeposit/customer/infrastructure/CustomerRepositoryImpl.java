package com.hyunsu.limitdeposit.customer.infrastructure;

import com.hyunsu.limitdeposit.customer.domain.Customer;
import com.hyunsu.limitdeposit.customer.domain.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CustomerRepositoryImpl implements CustomerRepository {

    private final CustomerJpaRepository jpaRepository;

    @Override
    public Customer save(Customer customer) {
        return jpaRepository.save(customer);
    }

    @Override
    public Optional<Customer> findById(Long customerId) {
        return jpaRepository.findById(customerId);
    }

    @Override
    public Optional<Customer> findByLoginId(String loginId) {
        return jpaRepository.findByLoginId(loginId);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return jpaRepository.existsByLoginId(loginId);
    }
}
