/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.self.withdrawrequest.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.fineract.extensions.payment.service.PaymentValidator;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BusinessEntity;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BusinessEvents;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanAccountNotActiveException;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotActiveException;
import org.apache.fineract.portfolio.self.withdrawrequest.command.WithdrawRequestCommand;
import org.apache.fineract.portfolio.self.withdrawrequest.command.WithdrawRequestCommandValidator;
import org.apache.fineract.portfolio.self.withdrawrequest.domain.SelfWithdrawRequestDetails;
import org.apache.fineract.portfolio.self.withdrawrequest.domain.SelfWithdrawRequestRepository;
import org.apache.fineract.portfolio.self.withdrawrequest.domain.WithdrawRequest;
import org.apache.fineract.portfolio.self.withdrawrequest.domain.WithdrawRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfWithdrawRequestWriteServiceImpl implements SelfWithdrawRequestWriteService {

    private final PlatformSecurityContext context;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final SelfWithdrawRequestRepository selfWithdrawRequestRepository;
    private final WithdrawRequestCommandValidator fromApiJsonDeserializer;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final PaymentValidator paymentValidator;
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public SelfWithdrawRequestWriteServiceImpl(PlatformSecurityContext context, WithdrawRequestRepository withdrawRequestRepository,
            SelfWithdrawRequestRepository selfWithdrawRequestRepository, WithdrawRequestCommandValidator fromApiJsonDeserializer,
            SavingsAccountRepository savingsAccountRepository, LoanRepository loanRepository,
            BusinessEventNotifierService businessEventNotifierService, PaymentValidator paymentValidator,
            FromJsonHelper fromApiJsonHelper) {
        this.context = context;
        this.withdrawRequestRepository = withdrawRequestRepository;
        this.selfWithdrawRequestRepository = selfWithdrawRequestRepository;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.savingsAccountRepository = savingsAccountRepository;
        this.loanRepository = loanRepository;
        this.businessEventNotifierService = businessEventNotifierService;
        this.paymentValidator = paymentValidator;

        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    @Transactional
    @Override
    public Long createSelfWithdrawRequest(WithdrawRequestCommand command) {
        long accountId = 0;
        String accountType = null;
        BigDecimal amount = null;
        JsonObject withdrawRequestPaymentChannelDetails = null;

        this.context.authenticatedUser();

        final WithdrawRequest withdrawRequest = WithdrawRequest.create(command.getTransactionDate(), command.getClientId());
        this.withdrawRequestRepository.save(withdrawRequest);
        final JsonArray parts = command.getSplit();
        if (parts != null) {
            for (int i = 0; i < parts.size(); i++) {

                final JsonObject jsonObject = parts.get(i).getAsJsonObject();
                this.fromApiJsonDeserializer.validateForCreate(jsonObject.toString());

                if (jsonObject.get("accountId") != null) {
                    accountId = jsonObject.get("accountId").getAsLong();
                }
                if (jsonObject.get("accountType") != null) {
                    accountType = jsonObject.get("accountType").getAsString();
                    if (accountType.equals("Savings")) {
                        Optional<SavingsAccount> savingsOptional = Optional
                                .ofNullable(this.savingsAccountRepository.findActiveAccountById(accountId));
                        if (!savingsOptional.isPresent()) {
                            throw new SavingsAccountNotActiveException(accountId);
                        }
                    }
                    if (accountType.equals("Loan")) {
                        Optional<Loan> loanOptional = Optional.ofNullable(this.loanRepository.findActiveLoanById(accountId));
                        if (!loanOptional.isPresent()) {
                            throw new LoanAccountNotActiveException(accountId);
                        }
                    }

                }
                if (jsonObject.get("amount") != null) {
                    amount = jsonObject.get("amount").getAsBigDecimal();
                }
                if (jsonObject.get("paymentChannelDetails") != null) {
                    withdrawRequestPaymentChannelDetails = jsonObject.get("paymentChannelDetails").getAsJsonObject();
                    JsonElement paymentDetails = this.fromApiJsonHelper.parse(withdrawRequestPaymentChannelDetails.toString());
                    final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                    final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                            .resource("withdrawalRequest");
                    this.paymentValidator.checkRequiredParameters(baseDataValidator, paymentDetails);
                    final SelfWithdrawRequestDetails newDetails = SelfWithdrawRequestDetails.fromJson(withdrawRequest.getId(), accountId,
                            accountType, amount, withdrawRequestPaymentChannelDetails.toString());
                    this.selfWithdrawRequestRepository.save(newDetails);
                    this.businessEventNotifierService.notifyBusinessEventWasExecuted(BusinessEvents.SAVINGS_WITHDRAW_REQUEST,
                            constructEntityMap(BusinessEntity.SAVINGS_WITHDRAW_REQUEST, newDetails));

                } else {

                    final SelfWithdrawRequestDetails newDetails = SelfWithdrawRequestDetails.fromJson(withdrawRequest.getId(), accountId,
                            accountType, amount, null);
                    this.selfWithdrawRequestRepository.save(newDetails);
                    this.businessEventNotifierService.notifyBusinessEventWasExecuted(BusinessEvents.SAVINGS_WITHDRAW_REQUEST,
                            constructEntityMap(BusinessEntity.SAVINGS_WITHDRAW_REQUEST, newDetails));
                }

            }

        }
        return withdrawRequest.getId();

    }

    @Override
    public CommandProcessingResult updateWithdrawRequestToAccepted(Long id) {
        final Optional<WithdrawRequest> withdrawRequestToUpdate = this.withdrawRequestRepository.findById(id);
        if (withdrawRequestToUpdate.isPresent()) {
            WithdrawRequest withdrawRequest = withdrawRequestToUpdate.get();
            withdrawRequest.setAccepted(true);
            this.withdrawRequestRepository.saveAndFlush(withdrawRequest);

        }
        return new CommandProcessingResult(id);
    }

    @Override
    public CommandProcessingResult updateWithdrawRequestToRejected(Long id) {
        final Optional<WithdrawRequest> withdrawRequestToUpdate = this.withdrawRequestRepository.findById(id);
        if (withdrawRequestToUpdate.isPresent()) {
            WithdrawRequest withdrawRequest = withdrawRequestToUpdate.get();
            withdrawRequest.setRejected(true);
            this.withdrawRequestRepository.saveAndFlush(withdrawRequest);

        }
        return new CommandProcessingResult(id);
    }

    private Map<BusinessEventNotificationConstants.BusinessEntity, Object> constructEntityMap(final BusinessEntity entityEvent,
            Object entity) {
        Map<BusinessEntity, Object> map = new HashMap<>(1);
        map.put(entityEvent, entity);
        return map;
    }

}
