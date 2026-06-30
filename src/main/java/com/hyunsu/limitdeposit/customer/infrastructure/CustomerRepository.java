package com.hyunsu.limitdeposit.customer.infrastructure;

import com.hyunsu.limitdeposit.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
