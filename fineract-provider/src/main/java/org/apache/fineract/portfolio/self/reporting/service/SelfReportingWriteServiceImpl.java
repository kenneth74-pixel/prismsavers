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
package org.apache.fineract.portfolio.self.reporting.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.util.Optional;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanAccountNotActiveException;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.exception.SavingsAccountNotActiveException;
import org.apache.fineract.portfolio.self.reporting.command.ReportingCommand;
import org.apache.fineract.portfolio.self.reporting.command.ReportingCommandValidator;
import org.apache.fineract.portfolio.self.reporting.domain.Reporting;
import org.apache.fineract.portfolio.self.reporting.domain.ReportingRepository;
import org.apache.fineract.portfolio.self.reporting.domain.SelfReportedDetails;
import org.apache.fineract.portfolio.self.reporting.domain.SelfReportedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfReportingWriteServiceImpl implements SelfReportingWriteService {

    private final PlatformSecurityContext context;
    private final ReportingRepository reportingRepository;
    private final SelfReportedRepository selfReportedRepository;
    private final ReportingCommandValidator fromApiJsonDeserializer;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;

    @Autowired
    public SelfReportingWriteServiceImpl(PlatformSecurityContext context, ReportingRepository reportingRepository,
            SelfReportedRepository selfReportedRepository, ReportingCommandValidator fromApiJsonDeserializer,
            SavingsAccountRepository savingsAccountRepository, LoanRepository loanRepository) {
        this.context = context;
        this.reportingRepository = reportingRepository;
        this.selfReportedRepository = selfReportedRepository;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.savingsAccountRepository = savingsAccountRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    @Override
    public Long createSelfReport(ReportingCommand command) {
        long accountId = 0;
        String accountType = null;
        BigDecimal amount = null;
        this.context.authenticatedUser();

        final Reporting report = Reporting.create(command.getPaymentType(), command.getTransactionDate(), command.getPaymentProof(),
                command.getTransactionId(), command.getClientId());
        this.reportingRepository.save(report);
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
                final SelfReportedDetails newDetails = SelfReportedDetails.fromJson(report.getId(), accountId, accountType, amount);
                this.selfReportedRepository.save(newDetails);
            }

        }
        return report.getId();

    }

    @Override
    public CommandProcessingResult updateReportToAccepted(Long id) {
        final Optional<Reporting> reportToUpdate = this.reportingRepository.findById(id);
        if (reportToUpdate.isPresent()) {
            Reporting report = reportToUpdate.get();
            report.setAccepted(true);
            this.reportingRepository.saveAndFlush(report);

        }
        return new CommandProcessingResult(id);
    }

    @Override
    public CommandProcessingResult updateReportToRejected(Long id) {
        final Optional<Reporting> reportToUpdate = this.reportingRepository.findById(id);
        if (reportToUpdate.isPresent()) {
            Reporting report = reportToUpdate.get();
            report.setRejected(true);
            this.reportingRepository.saveAndFlush(report);

        }
        return new CommandProcessingResult(id);
    }

}
