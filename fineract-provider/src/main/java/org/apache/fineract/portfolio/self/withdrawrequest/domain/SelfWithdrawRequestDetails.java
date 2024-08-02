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
package org.apache.fineract.portfolio.self.withdrawrequest.domain;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "m_self_withdraw_request_transactions_details")
public class SelfWithdrawRequestDetails extends AbstractPersistableCustom {

    @Column(name = "withdraw_request_id")
    private Long withdrawRequestId;
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_type")
    private String accountType;
    @Column(name = "amount")
    private BigDecimal transactionAmount;

    @Column(name = "payment_channel_details")
    private String paymentChannelDetails;

    public SelfWithdrawRequestDetails() {
        // no args constructor - required for serialization
    }

    public static SelfWithdrawRequestDetails fromJson(Long withdrawRequestId, Long accountId, String accountType,
            BigDecimal transactionAmount, String paymentChannelDetails) {
        return new SelfWithdrawRequestDetails(withdrawRequestId, accountId, accountType, transactionAmount, paymentChannelDetails);

    }

    private SelfWithdrawRequestDetails(Long withdrawRequestId, Long accountId, String accountType, BigDecimal transactionAmount,
            String paymentChannelDetails) {
        this.withdrawRequestId = withdrawRequestId;
        this.accountId = accountId;
        this.accountType = accountType;
        this.transactionAmount = transactionAmount;
        this.paymentChannelDetails = paymentChannelDetails;
    }

    public Long getWithdrawRequestId() {
        return withdrawRequestId;
    }

    public void setWithdrawRequestId(Long withdrawRequestId) {
        this.withdrawRequestId = withdrawRequestId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public void setPaymentChannelDetails(String paymentChannelDetails) {
        this.paymentChannelDetails = paymentChannelDetails;
    }

    public String getPaymentChannelDetails() {
        return paymentChannelDetails;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }
}
