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
package org.apache.fineract.portfolio.self.withdrawrequest.command;

import com.google.gson.JsonArray;
import java.util.Date;

public class WithdrawRequestCommand {

    private final JsonArray split;
    private final String reason;
    private final Boolean isAccepted;
    private final Boolean isRejected;
    private final Date transactionDate;
    private final Long clientId;

    public WithdrawRequestCommand(JsonArray split, Date transactionDate, String reason, Long clientId) {
        this.reason = reason;
        this.split = split;
        this.isAccepted = false;
        this.isRejected = false;
        this.transactionDate = transactionDate;
        this.clientId = clientId;

    }

    public String getReason() {
        return reason;
    }

    public JsonArray getSplit() {
        return split;
    }

    public Boolean getAccepted() {
        return isAccepted;
    }

    public Boolean getRejected() {
        return isRejected;
    }

    public Long getClientId() {
        return clientId;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }
}
