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
package org.apache.fineract.extensions.license.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.fineract.extensions.license.domain.LicenseData;
import org.apache.fineract.extensions.license.exception.ClientsExceedLicense;
import org.apache.fineract.extensions.license.exception.InvalidLicenseException;
import org.apache.fineract.extensions.license.exception.InvalidSmsLicenseException;
import org.apache.fineract.extensions.license.exception.LicenseLoadError;
import org.apache.fineract.extensions.license.exception.SmsExceedLicense;
import org.apache.fineract.extensions.license.exception.SmsLicenseError;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LicenseUsageValidator {

    private final LicenseReader licenseReader;
    private final ClientRepositoryWrapper clientRepository;
    private final SmsMessageRepository smsMessageRepository;
    private static final Logger LOG = LoggerFactory.getLogger(LicenseUsageValidator.class);

    public LicenseUsageValidator(LicenseReader licenseReader, ClientRepositoryWrapper clientRepository,
            SmsMessageRepository smsMessageRepository) {
        this.licenseReader = licenseReader;
        this.clientRepository = clientRepository;
        this.smsMessageRepository = smsMessageRepository;
    }

    private boolean isLicenseValidationEnabled() {
        // validate license unless an ENV variable says otherwise
        return !"FALSE".equals(System.getenv("VALIDATE_LICENSE"));
    }

    public void checkLicense() {
        if (isLicenseValidationEnabled()) {
            try {
                LicenseData license = licenseReader.getLicense();
                Date validityDate = new SimpleDateFormat("yyyy-MM-dd").parse(license.licenseExpiryDate());
                Date presentDate = new Date();
                if (validityDate.before(presentDate)) {
                    throw new InvalidLicenseException();
                }
            } catch (LicenseLoadError | ParseException | NullPointerException ignored) {
                // if we can't load or parse License, ignore rather than interrupt authentication
            }
        }
    }

    public void validateAdditionalClient() {
        if (isLicenseValidationEnabled()) {
            try {
                LicenseData license = licenseReader.getLicense();
                Long alreadyActive = clientRepository.getActiveClientCount();
                if (alreadyActive >= license.allowedUsers()) {
                    throw new ClientsExceedLicense();
                }
            } catch (LicenseLoadError | NullPointerException ignored) {
                // ignore rather than interrupt operations
            }
        }
    }

    public void validateAdditionalSms() {
        if (isLicenseValidationEnabled()) {
            try {
                Long alreadyActive = smsMessageRepository.countActiveSentMessages();
                LicenseData license = licenseReader.getLicense();

                if (alreadyActive >= license.allowedSms()) {
                    LOG.error("You have reached the maximum number of sms that your license permits");
                    throw new SmsExceedLicense();
                }
                checkSmsLicense();
            } catch (LicenseLoadError | NullPointerException ignored) {
                // ignore rather than interrupt operations
            }
        }
    }

    public void checkSmsLicense() {
        if (isLicenseValidationEnabled()) {
            try {
                LicenseData license = licenseReader.getLicense();
                if (license.smsExpiryDate() == null) {
                    LOG.error("You do not have a paid sms license , please purchase an sms license to gain access to sms");
                    throw new SmsLicenseError();
                }
                Date validityDate = new SimpleDateFormat("yyyy-MM-dd").parse(license.smsExpiryDate());
                Date presentDate = new Date();
                if (validityDate.before(presentDate)) {
                    LOG.error("Your sms license has expired, please renew your subscription to regain access");
                    throw new InvalidSmsLicenseException();
                }
            } catch (LicenseLoadError | ParseException | NullPointerException ignored) {
                // if we can't load or parse License, ignore rather than interrupt authentication
            }
        }
    }
}
