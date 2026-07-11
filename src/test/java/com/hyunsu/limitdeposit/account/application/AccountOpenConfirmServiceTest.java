package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.account.domain.account.Account;
import com.hyunsu.limitdeposit.account.domain.account.AccountLedgerHistoryRepository;
import com.hyunsu.limitdeposit.account.domain.account.AccountNumberGenerator;
import com.hyunsu.limitdeposit.account.domain.account.AccountRepository;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckHistory;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckHistoryRepository;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckResult;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplication;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.account.domain.opening.ApplicationStatus;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import com.hyunsu.limitdeposit.product.domain.Product;
import com.hyunsu.limitdeposit.product.domain.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountOpenConfirmServiceTest {

    @InjectMocks
    AccountOpenConfirmService accountOpenConfirmService;

    @Mock
    AccountOpenApplicationRepository accountOpenApplicationRepository;
    @Mock
    NcisCheckHistoryRepository ncisCheckHistoryRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    AccountRepository accountRepository;
    @Mock
    AccountLedgerHistoryRepository accountLedgerHistoryRepository;
    @Mock
    AccountNumberGenerator accountNumberGenerator;
    @Mock
    Product product;

    private static final Long APPLICATION_ID = 1L;
    private static final String ACCT_NO = "1000000000001";


    private AccountOpenApplication pendingApplication() {
        return AccountOpenApplication.apply(1L, "HNDEP001", Channel.NON_FACE_TO_FACE);
    }

    private NcisCheckHistory processingNcisHistory() {
        return NcisCheckHistory.preRecord(APPLICATION_ID, Channel.NON_FACE_TO_FACE);
    }

    // ---- approve ----

    @Test
    @DisplayName("NCIS_승인이면_계좌가_개설되고_신청이_승인상태로_전이된다")
    void approve_success() {
        // given
        AccountOpenApplication application = pendingApplication();
        NcisCheckHistory ncisCheckHistory = processingNcisHistory();

        when(accountOpenApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(ncisCheckHistoryRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.of(ncisCheckHistory));
        when(product.getProdCd()).thenReturn("HNDEP001");
        when(product.getBaseDepLmtPolicyId()).thenReturn("D01");
        when(product.getBasePymLmtPolicyId()).thenReturn("P01");
        when(productRepository.findEffective(any(), any())).thenReturn(Optional.of(product));
        when(accountNumberGenerator.nextAcctNo()).thenReturn(ACCT_NO);
        when(accountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        String result = accountOpenConfirmService.approve(APPLICATION_ID, "성공메시지");

        // then
        assertThat(result).isEqualTo(ACCT_NO);
        assertThat(application.getAppStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(application.getAcctNo()).isEqualTo(ACCT_NO);
        assertThat(ncisCheckHistory.getCheckResult()).isEqualTo(NcisCheckResult.APPROVED);
        assertThat(ncisCheckHistory.getResMsg()).isEqualTo("성공메시지");

        // Account에 product 값이 제대로 매핑됐는지
        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertThat(savedAccount.getAcctNo()).isEqualTo(ACCT_NO);
        assertThat(savedAccount.getDepLmtPolicyId()).isEqualTo("D01");
        assertThat(savedAccount.getPymtLmtPolicyId()).isEqualTo("P01");

        // account 저장 -> ledger 저장 순서
        InOrder order = inOrder(accountRepository, accountLedgerHistoryRepository);
        order.verify(accountRepository).save(any());
        order.verify(accountLedgerHistoryRepository).save(any());
    }

    @Test
    @DisplayName("상품을_찾을_수_없으면_PRODUCT_NOT_FOUND_예외가_발생한다")
    void approve_productNotFound_exception_throws() {
        // given
        when(accountOpenApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApplication()));
        when(ncisCheckHistoryRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.of(processingNcisHistory()));
        when(productRepository.findEffective(any(), any())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountOpenConfirmService.approve(APPLICATION_ID, "성공메시지"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Test
    @DisplayName("신청건을_찾을_수_없으면_예외가_발생한다")
    void approve_applicationNotFound_exception_throws() {
        // given
        when(accountOpenApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountOpenConfirmService.approve(APPLICATION_ID, "성공 메시지"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    @DisplayName("선적재된_NCIS_이력을_찾을_수_없으면_예외가_발생한다")
    void approve_ncisCheckHistoryNotFound_exception_throws() {
        // given
        when(accountOpenApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApplication()));
        when(ncisCheckHistoryRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountOpenConfirmService.approve(APPLICATION_ID, "성공 메시지"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

    }

    // ---- reject ----

    @Test
    @DisplayName("NCIS_반려이면_이력이_반려로_완료되고_신청이_반려상태로_전이된다")
    void reject_success() {
        //given
        AccountOpenApplication application = pendingApplication();
        NcisCheckHistory ncisCheckHistory = processingNcisHistory();

        when(accountOpenApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(ncisCheckHistoryRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.of(ncisCheckHistory));

        // when
        accountOpenConfirmService.reject(APPLICATION_ID, "반려사유");

        //then
        assertThat(application.getAppStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(application.getRejectRsn()).isEqualTo("반려사유");
        assertThat(ncisCheckHistory.getCheckResult()).isEqualTo(NcisCheckResult.REJECTED);
        assertThat(ncisCheckHistory.getResMsg()).isEqualTo("반려사유");
    }

    // ---- markCommunicationError ----

    @Test
    @DisplayName("NCIS_통신오류이면_이력이_오류로_완료되고_신청이_통신오류상태로_전이된다")
    void markCommunicationError_success() {
        //given
        AccountOpenApplication application = pendingApplication();
        NcisCheckHistory ncisCheckHistory = processingNcisHistory();

        when(accountOpenApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
        when(ncisCheckHistoryRepository.findByApplicationId(APPLICATION_ID)).thenReturn(Optional.of(ncisCheckHistory));

        //when
        accountOpenConfirmService.markCommunicationError(APPLICATION_ID, "통신오류 사유");

        //then
        assertThat(application.getAppStatus()).isEqualTo(ApplicationStatus.COMM_ERROR);
        assertThat(ncisCheckHistory.getCheckResult()).isEqualTo(NcisCheckResult.ERROR);
        assertThat(ncisCheckHistory.getResMsg()).isEqualTo("통신오류 사유");

    }
}
