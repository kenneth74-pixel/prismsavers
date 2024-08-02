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

import java.text.DecimalFormat;
import java.util.Objects;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.springframework.stereotype.Component;

@Component
public class PaymentConfiguration {

    private final ConfigurationReadPlatformService configService;

    public PaymentConfiguration(ConfigurationReadPlatformService configService) {
        this.configService = configService;
    }

    public String getHFBAccount() {
        GlobalConfigurationPropertyData conf = configService.retrieveGlobalConfiguration("HFB");
        if (!conf.isEnabled()) return "";
        if (!Objects.equals(conf.getStringValue(), null)) {
            return conf.getStringValue();
        } else if (!Objects.equals(conf.getValue(), null) && !Objects.equals(conf.getValue(), 0L)) {
            // HFB accounts have 10 characters, pad if necessary
            DecimalFormat df = new DecimalFormat("0000000000");
            return df.format(conf.getValue());
        }
        return "";
    }
}
