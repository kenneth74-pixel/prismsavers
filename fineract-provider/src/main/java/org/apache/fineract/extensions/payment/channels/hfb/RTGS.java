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
package org.apache.fineract.extensions.payment.channels.hfb;

import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.util.List;
import net.minidev.json.JSONObject;
import org.apache.fineract.extensions.payment.annotations.PayChannel;
import org.apache.fineract.extensions.payment.api.PaymentApiConstants;
import org.apache.fineract.extensions.payment.channels.Channel;
import org.apache.fineract.extensions.payment.channels.SynchronousChannel;
import org.apache.fineract.extensions.payment.domain.PaymentChannel;
import org.apache.fineract.extensions.payment.domain.PaymentConfiguration;
import org.apache.fineract.extensions.payment.domain.PaymentField;
import org.apache.fineract.extensions.payment.domain.PaymentTransaction;
import org.apache.fineract.extensions.payment.service.PaymentClient;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@PayChannel(channel = PaymentChannel.HFB_BANK_RTGS)
public class RTGS extends SynchronousChannel implements Channel {

    private final PaymentConfiguration configService;
    private final FromJsonHelper jsonHelper;
    private final PaymentClient paymentClient;

    public RTGS(PaymentConfiguration configService, FromJsonHelper jsonHelper, PaymentClient paymentClient,
            ApplicationContext applicationContext) {
        super(applicationContext, jsonHelper);
        this.configService = configService;
        this.jsonHelper = jsonHelper;
        this.paymentClient = paymentClient;
    }

    @Override
    public boolean isSupported() {
        return !configService.getHFBAccount().equals("");
    }

    @Override
    @SuppressWarnings("Duplicates")
    public List<PaymentField> requiredParameters() {
        return List.of(new PaymentField(PaymentApiConstants.accountNameField, String.class, ""),
                new PaymentField(PaymentApiConstants.amountField, BigDecimal.class, ""),
                new PaymentField(PaymentApiConstants.accountNumberField, String.class, ""),
                new PaymentField(PaymentApiConstants.commentField, String.class, ""),
                new PaymentField(PaymentApiConstants.bankNameField, String.class, ""),
                new PaymentField(PaymentApiConstants.branchNameField, String.class, ""),
                new PaymentField(PaymentApiConstants.accountTypeField, String.class, ""),
                new PaymentField(PaymentApiConstants.swiftCodeField, String.class, ""));
    }

    @Override
    @SuppressWarnings("Duplicates")
    public PaymentTransaction createRequest(Long sourceCommand, JsonElement json) {
        String bankAccount = configService.getHFBAccount();
        String toAccount = this.jsonHelper.extractStringNamed(PaymentApiConstants.accountNumberField, json);
        String comment = this.jsonHelper.extractStringNamed(PaymentApiConstants.commentField, json);
        BigDecimal transactionAmount = getAmount(json);
        String bankName = this.jsonHelper.extractStringNamed(PaymentApiConstants.bankNameField, json);
        String branchName = this.jsonHelper.extractStringNamed(PaymentApiConstants.branchNameField, json);
        String swiftCode = this.jsonHelper.extractStringNamed(PaymentApiConstants.swiftCodeField, json);
        String accountType = this.jsonHelper.extractStringNamed(PaymentApiConstants.accountTypeField, json);
        String accountName = this.jsonHelper.extractStringNamed(PaymentApiConstants.accountNameField, json);

        JSONObject request = new JSONObject();
        request.put("tenantId", ThreadLocalContextUtil.getTenant().getTenantIdentifier());
        request.put("accountTo", toAccount);
        request.put("accountFrom", bankAccount);
        request.put("amount", transactionAmount);
        request.put("transferType", "rtgs");
        request.put("comment", comment.toUpperCase());
        request.put("name", accountName);
        request.put("location", branchName);
        request.put("bank", bankName);
        request.put("accountType", accountType);
        request.put("swiftCode", swiftCode);

        var httpResponse = this.paymentClient.postRequest("/api/transfer", request.toJSONString());
        if (!httpResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("payment request unsuccessful");
        }

        String jsonResponse = httpResponse.getBody();
        var response = this.jsonHelper.parse(jsonResponse);
        String externalReference = this.jsonHelper.extractStringNamed("reference", response);

        publishPaymentEvent(sourceCommand, externalReference);

        PaymentTransaction result = new PaymentTransaction();
        result.setExternalReference(externalReference);
        result.setAccountTo(toAccount);
        result.setAccountFrom(bankAccount);
        result.setAmount(transactionAmount);
        return result;
    }

}
