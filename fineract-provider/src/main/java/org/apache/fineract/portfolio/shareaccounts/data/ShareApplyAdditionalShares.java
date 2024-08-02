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
package org.apache.fineract.portfolio.shareaccounts.data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ShareApplyAdditionalShares {

    private final transient Integer rowIndex;

    private final LocalDate requestedDate;

    private final String dateFormat;

    private final String locale;
    private final Integer requestedShares;
    private final BigDecimal unitPrice;

    public static ShareApplyAdditionalShares importInstance(LocalDate requestedDate, Integer rowIndex, String locale,
            Integer requestedShares, BigDecimal unitPrice, String dateFormat) {
        return new ShareApplyAdditionalShares(requestedDate, rowIndex, locale, requestedShares, unitPrice, dateFormat);
    }

    private ShareApplyAdditionalShares(LocalDate activatedOnDate, Integer rowIndex, String locale, Integer requestedShares,
            BigDecimal unitPrice, String dateFormat) {
        this.requestedDate = activatedOnDate;
        this.requestedShares = requestedShares;
        this.unitPrice = unitPrice;
        this.rowIndex = rowIndex;
        this.dateFormat = dateFormat;
        this.locale = locale;
    }

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public String getLocale() {
        return locale;
    }

    public Integer getRequestedShares() {
        return requestedShares;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }
}
