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
package org.apache.fineract.extensions.payment.channels;

import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.util.Locale;
import org.apache.fineract.extensions.payment.api.PaymentApiConstants;
import org.apache.fineract.extensions.payment.event.PaymentResponseEvent;
import org.apache.fineract.extensions.payment.event.PaymentResponseSource;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

public abstract class SynchronousChannel {

    private static final Logger LOG = LoggerFactory.getLogger(SynchronousChannel.class);
    private final ApplicationContext applicationContext;
    private final FromJsonHelper jsonHelper;

    protected SynchronousChannel(ApplicationContext applicationContext, FromJsonHelper jsonHelper) {
        this.applicationContext = applicationContext;
        this.jsonHelper = jsonHelper;
    }

    protected BigDecimal getAmount(JsonElement params) {
        if (!this.jsonHelper.parameterExists(PaymentApiConstants.amountField, params)) {
            throw new RuntimeException("invalid payment request");
        }
        return this.jsonHelper.extractBigDecimalNamed(PaymentApiConstants.amountField, params, Locale.ENGLISH);
    }

    protected void publishPaymentEvent(Long commandId, String externalRef) {
        try {
            PaymentResponseSource source = new PaymentResponseSource(commandId, true);
            final String tenantIdentifier = ThreadLocalContextUtil.getTenant().getTenantIdentifier();
            PaymentResponseEvent applicationEvent = new PaymentResponseEvent(source, externalRef, tenantIdentifier);
            this.applicationContext.publishEvent(applicationEvent);
        } catch (Exception e) {
            LOG.error("failed to publish payment event", e);
        }
    }
}
