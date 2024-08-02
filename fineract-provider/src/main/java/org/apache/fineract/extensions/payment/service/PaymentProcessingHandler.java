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
package org.apache.fineract.extensions.payment.service;

import com.google.gson.JsonElement;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.extensions.payment.api.PaymentApiConstants;
import org.apache.fineract.extensions.payment.channels.Channel;
import org.apache.fineract.extensions.payment.domain.PaymentChannel;
import org.apache.fineract.extensions.payment.domain.PaymentTransaction;
import org.apache.fineract.extensions.payment.domain.PaymentTransactionJpa;
import org.apache.fineract.extensions.payment.event.PaymentResponseEvent;
import org.apache.fineract.extensions.payment.event.PaymentResponseSource;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentProcessingHandler {

    private static final Map<String, List<String>> paymentActionMap = new HashMap<>();

    static {
        paymentActionMap.put("LOAN", List.of("DISBURSE", "REPAYMENT"));
        paymentActionMap.put("SAVINGSACCOUNT", List.of("DEPOSIT", "WITHDRAWAL"));
        paymentActionMap.put("JOURNALENTRY", List.of("CREATE"));
    }

    private static final Logger LOG = LoggerFactory.getLogger(PaymentProcessingHandler.class);

    private final FromJsonHelper jsonHelper;
    private final PaymentTransactionJpa paymentRepository;
    private final ApplicationContext applicationContext;
    private final ChannelProvider channelProvider;

    public PaymentProcessingHandler(FromJsonHelper jsonHelper, PaymentTransactionJpa paymentRepository,
            ApplicationContext applicationContext, ChannelProvider channelProvider) {
        this.jsonHelper = jsonHelper;
        this.paymentRepository = paymentRepository;
        this.applicationContext = applicationContext;
        this.channelProvider = channelProvider;
    }

    public boolean requiresPaymentIntegration(CommandWrapper command) {
        String entity = command.entityName();
        String action = command.actionName();
        String payload = command.getJson();

        if (isSupportedAction(entity, action)) {
            return requestedPayment(payload);
        }
        return false;
    }

    private boolean requestedPayment(String payload) {
        JsonElement data = this.jsonHelper.parse(payload);
        return this.jsonHelper.parameterExists(PaymentApiConstants.paymentChannelDetails, data);
    }

    private boolean isSupportedAction(String entity, String action) {
        if (paymentActionMap.containsKey(entity)) {
            List<String> supportedActions = paymentActionMap.get(entity);
            return supportedActions.contains(action);
        }
        return false;
    }

    @Transactional
    public void initiatePaymentRequest(String jsonPayload, Long commandId) {
        try {
            JsonElement data = this.jsonHelper.parse(jsonPayload);
            var paymentDetails = this.jsonHelper.extractJsonObjectNamed(PaymentApiConstants.paymentChannelDetails, data);
            var channelId = this.jsonHelper.extractIntegerSansLocaleNamed(PaymentApiConstants.channelId, paymentDetails);
            var accountName = this.jsonHelper.extractStringNamed(PaymentApiConstants.accountNameField, paymentDetails);

            final Channel handler = this.channelProvider.getHandler(PaymentChannel.fromInt(channelId));
            PaymentTransaction record = handler.createRequest(commandId, paymentDetails);

            record.setSubmittedDate(new Date());
            record.setOriginReference(commandId);
            record.setPaymentChannel(channelId);
            record.setAccountName(accountName);
            this.paymentRepository.save(record);
        } catch (RuntimeException ex) {
            LOG.error("failed to process payment event", ex);
            publishPaymentEvent(commandId);
        }
    }

    public boolean requiresPermissionCheck(CommandWrapper command) {
        if (requiresPaymentIntegration(command)) {
            String entity = command.entityName();
            String action = command.actionName();
            String entityAction = entity + "_" + action;
            List<String> inBound = List.of("LOAN_REPAYMENT", "SAVINGSACCOUNT_DEPOSIT");
            return !inBound.contains(entityAction);
        }
        return true;
    }

    private void publishPaymentEvent(Long commandId) {
        try {
            PaymentResponseSource source = new PaymentResponseSource(commandId, false);
            final String tenantIdentifier = ThreadLocalContextUtil.getTenant().getTenantIdentifier();
            PaymentResponseEvent applicationEvent = new PaymentResponseEvent(source, "", tenantIdentifier);
            this.applicationContext.publishEvent(applicationEvent);
        } catch (Exception e) {
            LOG.error("failed to publish payment event", e);
        }
    }
}
