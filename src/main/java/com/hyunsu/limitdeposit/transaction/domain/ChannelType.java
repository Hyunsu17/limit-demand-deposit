package com.hyunsu.limitdeposit.transaction.domain;

/**
 * TRANS_RAW / TRANS_HISTORY 의 거래 채널. 개설·확인용 {@code Channel}(영업점/비대면)과는 다른 축이다.
 */
public enum ChannelType {
    ATM,        // 1: ATM
    INTERNET,   // 2: 인터넷
    KAKAO,      // 3: 카톡
    INTERBANK,  // 4: 타행이체
    BRANCH      // 5: 영업점
}
