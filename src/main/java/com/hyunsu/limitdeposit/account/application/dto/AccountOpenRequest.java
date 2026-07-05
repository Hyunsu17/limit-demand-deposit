package com.hyunsu.limitdeposit.account.application.dto;

import com.hyunsu.limitdeposit.account.domain.Channel;

public record AccountOpenRequest(
        Long customerId,
        String prodCd,
        Channel channel
) {
}
