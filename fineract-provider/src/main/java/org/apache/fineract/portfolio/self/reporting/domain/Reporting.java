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
package org.apache.fineract.portfolio.self.reporting.domain;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "m_self_reported_transactions")
public class Reporting extends AbstractPersistableCustom {

    @Column(name = "payment_type")
    private Integer paymentType;
    @Column(name = "is_rejected")
    private Boolean isRejected;
    @Column(name = "is_accepted")
    private Boolean isAccepted;
    @Column(name = "transaction_date")
    @Temporal(TemporalType.DATE)
    private Date transactionDate;
    @Column(name = "payment_proof")
    private Integer paymentProof;
    @Column(name = "transaction_id")
    private String transactionId;
    @Column(name = "client_id")
    private Long clientId;

    public Reporting() {}

    public static Reporting create(final Integer paymentType, final Date transactionDate, final Integer paymentProof,
            final String transactionId, final Long clientId) {

        return new Reporting(paymentType, false, false, transactionDate, paymentProof, transactionId, clientId);

    }

    private Reporting(final Integer paymentType, final Boolean isAccepted, final Boolean isRejected, final Date transactionDate,
            final Integer paymentProof, final String transactionId, final Long clientId) {
        this.paymentType = paymentType;
        this.transactionDate = transactionDate;
        this.transactionId = transactionId;
        this.isAccepted = isAccepted;
        this.isRejected = isRejected;
        this.paymentProof = paymentProof;
        this.clientId = clientId;

    }

    public Integer getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(Integer paymentType) {
        this.paymentType = paymentType;
    }

    public Boolean getRejected() {
        return isRejected;
    }

    public void setRejected(Boolean rejected) {
        isRejected = rejected;
    }

    public Boolean getAccepted() {
        return isAccepted;
    }

    public void setAccepted(Boolean accepted) {
        isAccepted = accepted;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Integer getPaymentProof() {
        return paymentProof;
    }

    public void setPaymentProof(Integer paymentProof) {
        this.paymentProof = paymentProof;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}
