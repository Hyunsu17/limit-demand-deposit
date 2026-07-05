package com.hyunsu.limitdeposit.account.domain;

/**
 * 신청/확인 채널. ACCT_OPEN_APPLICATION.APPLICATION_CHANNEL, NCIS_CHECK_HIST.CHECK_CHANNEL 공용.
 */
public enum Channel {
    BRANCH,           // 1: 영업점
    NON_FACE_TO_FACE  // 2: 비대면
}
