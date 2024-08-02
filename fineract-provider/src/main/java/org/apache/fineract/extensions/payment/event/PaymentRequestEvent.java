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
package org.apache.fineract.extensions.payment.event;

import org.springframework.context.ApplicationEvent;

public class PaymentRequestEvent extends ApplicationEvent {

    private final String payload;

    private final String tenantIdentifier;

    public PaymentRequestEvent(PaymentRequestSource source, String payload, String tenantIdentifier) {
        super(source);
        this.payload = payload;
        this.tenantIdentifier = tenantIdentifier;
    }

    public String getPayload() {
        return payload;
    }

    public String getTenantIdentifier() {
        return tenantIdentifier;
    }

    @Override
    public PaymentRequestSource getSource() {
        return (PaymentRequestSource) super.getSource();
    }
}
