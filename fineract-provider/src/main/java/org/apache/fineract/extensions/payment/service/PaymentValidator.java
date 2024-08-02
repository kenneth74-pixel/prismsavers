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
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.apache.fineract.extensions.payment.api.PaymentApiConstants;
import org.apache.fineract.extensions.payment.channels.Channel;
import org.apache.fineract.extensions.payment.domain.PaymentChannel;
import org.apache.fineract.extensions.payment.domain.PaymentField;
import org.apache.fineract.extensions.payment.exception.PaymentNotSupported;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.stereotype.Component;

@Component
public class PaymentValidator {

    private final ChannelProvider channelProvider;
    private final FromJsonHelper jsonHelper;

    public PaymentValidator(ChannelProvider channelProvider, FromJsonHelper jsonHelper) {
        this.channelProvider = channelProvider;
        this.jsonHelper = jsonHelper;
    }

    public void checkRequiredParameters(final DataValidatorBuilder baseDataValidator, JsonElement section) {
        try {
            Integer channelId = jsonHelper.extractIntegerSansLocaleNamed(PaymentApiConstants.channelId, section);
            if (channelId == null) {
                throw new PaymentNotSupported("Payment channel must be provided");
            }
            final Channel handler = this.channelProvider.getHandler(PaymentChannel.fromInt(channelId));
            List<PaymentField> fields = handler.requiredParameters();
            for (PaymentField field : fields) {
                if (field.typeRequired().equals(String.class) && field.pattern().equals("")) {
                    String provided = jsonHelper.extractStringNamed(field.fieldName(), section);
                    baseDataValidator.reset().parameter(field.fieldName()).value(provided).notBlank().notNull().notExceedingLengthOf(50);
                }
                if (field.typeRequired().equals(String.class) && !field.pattern().equals("")) {
                    String provided = jsonHelper.extractStringNamed(field.fieldName(), section);
                    baseDataValidator.reset().parameter(field.fieldName()).value(provided).matchesRegularExpression(field.pattern())
                            .notExceedingLengthOf(50);
                }
                if (field.typeRequired().equals(BigDecimal.class)) {
                    BigDecimal provided = jsonHelper.extractBigDecimalNamed(field.fieldName(), section, Locale.ENGLISH);
                    baseDataValidator.reset().parameter(field.fieldName()).value(provided).notNull().zeroOrPositiveAmount();
                }
            }
        } catch (Throwable t) {
            throw new PaymentNotSupported("Payment could not be initiated");
        }
    }

    public boolean canUsePaymentChannel(PaymentChannel channel) {
        return this.channelProvider.getHandler(channel).isSupported();
    }

}
