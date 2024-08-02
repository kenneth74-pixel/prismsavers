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
package org.apache.fineract.extensions.payment.listener;

import org.apache.fineract.extensions.payment.event.PaymentRequestEvent;
import org.apache.fineract.extensions.payment.service.PaymentProcessingHandler;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.TenantDetailsService;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentRequestEventListener implements ApplicationListener<PaymentRequestEvent> {

    private final TenantDetailsService tenantDetailsService;
    private final PaymentProcessingHandler paymentProcessingHandler;

    public PaymentRequestEventListener(TenantDetailsService tenantDetailsService, PaymentProcessingHandler paymentProcessingHandler) {
        this.tenantDetailsService = tenantDetailsService;
        this.paymentProcessingHandler = paymentProcessingHandler;
    }

    @Override
    public void onApplicationEvent(PaymentRequestEvent event) {
        final String tenantIdentifier = event.getTenantIdentifier();
        final FineractPlatformTenant tenant = this.tenantDetailsService.loadTenantById(tenantIdentifier);
        ThreadLocalContextUtil.setTenant(tenant);
        paymentProcessingHandler.initiatePaymentRequest(event.getPayload(), event.getSource().commandSource());
    }
}
