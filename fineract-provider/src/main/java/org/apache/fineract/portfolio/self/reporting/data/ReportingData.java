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
package org.apache.fineract.portfolio.self.reporting.data;

import java.math.BigDecimal;
import java.util.Date;

public class ReportingData {

    private final Long id;
    private final Integer paymentType;
    private final Integer paymentProof;
    private final Boolean isAccepted;
    private final Boolean isRejected;
    private final BigDecimal amount;
    private final Date transactionDate;
    private final String transactionId;
    private final Long clientId;
    private final String clientName;

    public ReportingData(Long id, Integer paymentType, Integer paymentProof, Boolean isAccepted, Boolean isRejected, BigDecimal amount,
            Date transactionDate, String transactionId, Long clientId, String clientName) {
        this.id = id;
        this.paymentType = paymentType;
        this.paymentProof = paymentProof;
        this.isAccepted = isAccepted;
        this.isRejected = isRejected;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.transactionId = transactionId;
        this.clientId = clientId;
        this.clientName = clientName;
    }
}
