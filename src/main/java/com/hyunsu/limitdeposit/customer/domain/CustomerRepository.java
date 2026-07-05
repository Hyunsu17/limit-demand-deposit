package com.hyunsu.limitdeposit.customer.domain;

import java.util.Optional;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(Long customerId);

    Optional<Customer> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
