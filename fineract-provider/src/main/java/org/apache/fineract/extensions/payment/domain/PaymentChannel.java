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
package org.apache.fineract.extensions.payment.domain;

import java.util.HashMap;
import java.util.Map;

public enum PaymentChannel {

    HFB_BANK_INTERNAL(1, "hfb.out.internal_transfer"), HFB_BANK_EFT(2, "hfb.out.eft"), HFB_BANK_RTGS(3, "hfb.out.rtgs"), HFB_WALLET_IN(4,
            "hfb.in.wallet"), HFB_WALLET_OUT(5, "hfb.out.wallet");

    private final Integer value;
    private final String code;

    private static final Map<Integer, PaymentChannel> paymentChannels = new HashMap<>();

    static {
        for (var channel : PaymentChannel.values()) {
            paymentChannels.put(channel.value, channel);
        }
    }

    PaymentChannel(Integer value, String code) {
        this.value = value;
        this.code = code;
    }

    public static PaymentChannel fromInt(Integer value) {
        return paymentChannels.get(value);
    }

    public String getCode() {
        return code;
    }

    public Integer getValue() {
        return value;
    }

}
