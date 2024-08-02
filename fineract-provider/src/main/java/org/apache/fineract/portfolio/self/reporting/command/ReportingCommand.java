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
package org.apache.fineract.portfolio.self.reporting.command;

import com.google.gson.JsonArray;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;

public class ReportingCommand {

    private final Integer paymentType;
    private final Integer paymentProof;
    private final Boolean isAccepted;
    private final Boolean isRejected;
    private final Date transactionDate;
    private final String transactionId;
    private final JsonArray split;
    private final Long clientId;

    public ReportingCommand(Integer paymentType, Integer paymentProof, Date transactionDate, String transactionId, JsonArray split,
            Long clientId) {
        this.paymentType = paymentType;
        this.paymentProof = paymentProof;
        this.split = split;
        this.isAccepted = false;
        this.isRejected = false;
        this.transactionDate = transactionDate;
        this.transactionId = transactionId;
        this.clientId = clientId;
        if (split.size() == 0) {
            List<ApiParameterError> errors = Collections.emptyList();
            throw new PlatformApiDataValidationException("self.report.empty.account", "You must specify which accounts are affected",
                    errors);
        }
    }

    public Integer getPaymentType() {
        return paymentType;
    }

    public Integer getPaymentProof() {
        return paymentProof;
    }

    public Boolean getAccepted() {
        return isAccepted;
    }

    public Boolean getRejected() {
        return isRejected;
    }

    public JsonArray getSplit() {
        return split;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Long getClientId() {
        return clientId;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }
}
