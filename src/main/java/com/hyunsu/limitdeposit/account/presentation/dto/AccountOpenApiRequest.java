package com.hyunsu.limitdeposit.account.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AccountOpenApiRequest {

    @NotBlank(message = "상품코드는 필수입니다.")
    private String prodCd;
}
