/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.extensions.payment.domain;

import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "k_payment_integration_transaction", uniqueConstraints = { @UniqueConstraint(columnNames = { "external_reference" }),
        @UniqueConstraint(columnNames = { "internal_reference" }), })
public class PaymentTransaction extends AbstractPersistableCustom {

    @Temporal(TemporalType.DATE)
    @Column(name = "submitted_on_date", nullable = false)
    private Date submittedDate;
    @Column(name = "internal_reference", nullable = false)
    private Long originReference;
    @Column(name = "external_reference")
    private String externalReference;
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    @Column(name = "payment_type_enum", nullable = false)
    private Integer paymentChannel;
    @Column(name = "account_from", nullable = false)
    private String accountFrom;
    @Column(name = "account_to", nullable = false)
    private String accountTo;
    @Column(name = "account_name")
    private String accountName;
    @Column(name = "location")
    private String location;
    @Column(name = "bank_name")
    private String bankName;
    @Column(name = "swift_code")
    private String swiftCode;
    @Column(name = "account_type")
    private String accountType;

    public Date getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
    }

    public Long getOriginReference() {
        return originReference;
    }

    public void setOriginReference(Long originReference) {
        this.originReference = originReference;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getPaymentChannel() {
        return paymentChannel;
    }

    public void setPaymentChannel(Integer paymentChannel) {
        this.paymentChannel = paymentChannel;
    }

    public String getAccountFrom() {
        return accountFrom;
    }

    public void setAccountFrom(String accountFrom) {
        this.accountFrom = accountFrom;
    }

    public String getAccountTo() {
        return accountTo;
    }

    public void setAccountTo(String accountTo) {
        this.accountTo = accountTo;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getSwiftCode() {
        return swiftCode;
    }

    public void setSwiftCode(String swiftCode) {
        this.swiftCode = swiftCode;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
