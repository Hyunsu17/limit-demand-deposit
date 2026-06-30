package com.hyunsu.limitdeposit.common.auth;

import com.hyunsu.limitdeposit.common.auth.dto.LoginRequest;
import com.hyunsu.limitdeposit.common.auth.dto.SignupRequest;
import com.hyunsu.limitdeposit.common.auth.dto.TokenResponse;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import com.hyunsu.limitdeposit.customer.domain.Customer;
import com.hyunsu.limitdeposit.customer.infrastructure.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public void signup(SignupRequest request) {
        if (customerRepository.existsByLoginId(request.getLoginId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 사용 중인 아이디입니다.");
        }
        customerRepository.save(Customer.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .build());
    }

    public TokenResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(request.getPassword(), customer.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return TokenResponse.of(
                jwtProvider.generateAccessToken(customer.getLoginId()),
                jwtProvider.generateRefreshToken(customer.getLoginId())
        );
    }
}
