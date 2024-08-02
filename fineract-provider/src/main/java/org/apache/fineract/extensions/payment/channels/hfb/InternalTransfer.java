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
import java.util.regex.Pattern;
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
@PayChannel(channel = PaymentChannel.HFB_BANK_INTERNAL)
public class InternalTransfer extends SynchronousChannel implements Channel {

    private final PaymentConfiguration configService;
    private final FromJsonHelper jsonHelper;
    private final PaymentClient paymentClient;

    public InternalTransfer(PaymentConfiguration configService, FromJsonHelper jsonHelper, PaymentClient paymentClient,
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
    public List<PaymentField> requiredParameters() {
        final Pattern regex = Pattern.compile("^\\d{10}$");
        return List.of(new PaymentField(PaymentApiConstants.amountField, BigDecimal.class, ""),
                new PaymentField(PaymentApiConstants.accountNumberField, String.class, regex.pattern()),
                new PaymentField(PaymentApiConstants.commentField, String.class, ""));
    }

    @Override
    @SuppressWarnings("Duplicates")
    public PaymentTransaction createRequest(Long sourceCommand, JsonElement json) {
        String bankAccount = configService.getHFBAccount();
        String toAccount = this.jsonHelper.extractStringNamed(PaymentApiConstants.accountNumberField, json);
        String comment = this.jsonHelper.extractStringNamed(PaymentApiConstants.commentField, json);
        BigDecimal transactionAmount = getAmount(json);

        JSONObject request = new JSONObject();
        request.put("tenantId", ThreadLocalContextUtil.getTenant().getTenantIdentifier());
        request.put("accountTo", toAccount);
        request.put("accountFrom", bankAccount);
        request.put("amount", transactionAmount);
        request.put("transferType", "internal");
        request.put("comment", comment.toUpperCase());

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
