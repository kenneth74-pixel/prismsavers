/*
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
package org.apache.fineract.extensions.payment.api;

import io.swagger.v3.oas.annotations.media.Schema;

public final class PaymentIntegrationApiResourceSwagger {

    private PaymentIntegrationApiResourceSwagger() {}

    @Schema(description = "Payment status response")
    public static final class PaymentStatusResponse {

        private PaymentStatusResponse() {}

        @Schema(name = "externalReference", example = "WB899824542", required = true)
        public String externalReference;
    }

    @Schema(description = "Channel Availability Response")
    public static final class ChannelAvailabilityResponse {

        private ChannelAvailabilityResponse() {}

        @Schema(example = "true")
        public boolean canUse;
        @Schema(example = "4")
        public Integer channelId;
        @Schema(example = "hfb.in.wallet")
        public String channelName;
    }

    @Schema(description = "Check Account Details Request")
    public static final class CheckAccountRequest {

        private CheckAccountRequest() {}

        @Schema(example = "4")
        public Integer channelId;
        @Schema(example = "0779999722")
        public String accountNo;
    }

    @Schema(description = "Check Account Details Response")
    public static final class CheckAccountResponse {

        private CheckAccountResponse() {}

        @Schema(example = "John Doe")
        public String accountName;
    }
}
