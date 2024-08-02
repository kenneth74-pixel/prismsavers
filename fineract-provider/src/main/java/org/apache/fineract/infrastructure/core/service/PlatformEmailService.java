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
package org.apache.fineract.infrastructure.core.service;

import java.util.List;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;

public interface PlatformEmailService {

    void sendToUserAccount(String organisationName, String contactName, String address, String username, String unencodedPassword,
            boolean isSelfServiceUser);

    void sendPendingAccount(String organisationName, String contactName, String address, String username, String unencodedPassword);

    void sendDefinedEmail(EmailDetail emailDetails);

    void sendAccountActivated(String contactName, String address, String username);

    void sendForgotPasswordEmail(String email, String resetToken, String username, boolean isSelfServiceUser);

    void sendMonthlyStatementsEmail(String email, String startdate, String enddate, String username, String firstname, List lrecords,
            List srecords);

    void sendLoanApplicationEmail(String principal, String clientName, String email, String username);

    void sendEmailCampaign(String message, String clientName, String email, String campaignName);

}
