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
package org.apache.fineract.extensions.license.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public record LicenseData(Long allowedUsers, String licenseExpiryDate, Long allowedSms, String smsExpiryDate) implements Serializable {

    @JsonCreator
    public LicenseData(@JsonProperty("allowedUsers") Long allowedUsers, @JsonProperty("licenseExpiryDate") String licenseExpiryDate,
            @JsonProperty("allowedSms") Long allowedSms, @JsonProperty("smsExpiryDate") String smsExpiryDate) {
        this.allowedUsers = allowedUsers;
        this.licenseExpiryDate = licenseExpiryDate;
        this.allowedSms = allowedSms;
        this.smsExpiryDate = smsExpiryDate;
    }

}
