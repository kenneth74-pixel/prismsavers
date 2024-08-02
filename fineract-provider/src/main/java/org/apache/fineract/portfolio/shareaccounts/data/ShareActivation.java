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

import java.time.LocalDate;

public class ShareActivation {

    private final transient Integer rowIndex;

    private final LocalDate activatedDate;

    private final String dateFormat;

    private final String locale;

    public static ShareActivation importInstance(LocalDate activatedDate, Integer rowIndex, String locale, String dateFormat) {
        return new ShareActivation(activatedDate, rowIndex, locale, dateFormat);
    }

    private ShareActivation(LocalDate activatedOnDate, Integer rowIndex, String locale, String dateFormat) {
        this.activatedDate = activatedOnDate;
        this.rowIndex = rowIndex;
        this.dateFormat = dateFormat;
        this.locale = locale;
    }

    public LocalDate getActivatedOnDate() {
        return activatedDate;
    }

    public String getLocale() {
        return locale;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }
}
