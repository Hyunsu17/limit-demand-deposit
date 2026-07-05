package com.hyunsu.limitdeposit.account.presentation;

import com.hyunsu.limitdeposit.account.application.AccountOpenService;
import com.hyunsu.limitdeposit.account.application.dto.AccountOpenRequest;
import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.account.presentation.dto.AccountOpenApiRequest;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import com.hyunsu.limitdeposit.customer.domain.Customer;
import com.hyunsu.limitdeposit.customer.domain.CustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountOpenService accountOpenService;
    private final CustomerRepository customerRepository;

    @PostMapping
    public ResponseEntity<Void> openAccount(@AuthenticationPrincipal UserDetails userDetails,
                                             @RequestBody @Valid AccountOpenApiRequest request) {

        Customer customer = customerRepository.findByLoginId(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // channel은 클라이언트 입력이 아니라 서버가 고정
        AccountOpenRequest openRequest = new AccountOpenRequest(
                customer.getId(), request.getProdCd(), Channel.NON_FACE_TO_FACE);

        // [Claude] NCIS 결과(반려/통신오류)는 BusinessException으로 전파되어 GlobalExceptionHandler가 응답 변환
        accountOpenService.openAccount(openRequest);

        // [Claude] 개설 확정까지 동기 처리되므로 성공 시 201 Created (TX2 완성 후 실제 반환 도달)
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
