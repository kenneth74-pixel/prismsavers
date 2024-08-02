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
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;

public abstract class AsynchronousChannel {

    private final FromJsonHelper jsonHelper;

    protected AsynchronousChannel(FromJsonHelper jsonHelper) {
        this.jsonHelper = jsonHelper;
    }

    protected BigDecimal getAmount(JsonElement params) {
        if (!this.jsonHelper.parameterExists(PaymentApiConstants.amountField, params)) {
            throw new RuntimeException("invalid payment request");
        }
        return this.jsonHelper.extractBigDecimalNamed(PaymentApiConstants.amountField, params, Locale.ENGLISH);
    }
}
