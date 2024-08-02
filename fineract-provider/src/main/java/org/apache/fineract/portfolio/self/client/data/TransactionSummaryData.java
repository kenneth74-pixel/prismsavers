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
package org.apache.fineract.portfolio.self.client.data;

import java.math.BigDecimal;
import java.util.Date;

public class TransactionSummaryData {

    private final Long id;
    private final String transactionType;
    private final String accountNumber;
    private final BigDecimal amount;
    private final String currencyCode;
    private final Date transactionDate;
    private final Boolean plusMinus;
    private final String accountType;

    public TransactionSummaryData(Long id, String transactionType, String accountNumber, BigDecimal amount, String currencyCode,
            Date transactionDate, Boolean plusMinus, String accountType) {
        this.id = id;
        this.transactionType = transactionType;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.transactionDate = transactionDate;
        this.plusMinus = plusMinus;
        this.accountType = accountType;
    }

    public static TransactionSummaryData loanInstance(final Long id, final String transactionType, final String accountNumber,
            final BigDecimal amount, final String currencyCode, final Date transactionDate) {
        return new TransactionSummaryData(id, transactionType, accountNumber, amount, currencyCode, transactionDate, false, "loan");
    }

    public static TransactionSummaryData savingsInstance(final Long id, final String transactionType, final String accountNumber,
            final BigDecimal amount, final String currencyCode, final Date transactionDate) {
        return new TransactionSummaryData(id, transactionType, accountNumber, amount, currencyCode, transactionDate, true, "savings");
    }

    public static TransactionSummaryData clientInstance(final Long id, final String transactionType, final BigDecimal amount,
            final String currencyCode, final Date transactionDate) {
        return new TransactionSummaryData(id, transactionType, null, amount, currencyCode, transactionDate, false, "client");
    }
}
