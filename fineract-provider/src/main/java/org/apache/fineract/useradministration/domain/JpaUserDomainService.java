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
package org.apache.fineract.useradministration.domain;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.fineract.infrastructure.campaigns.sms.data.SmsProviderData;
import org.apache.fineract.infrastructure.campaigns.sms.service.SmsCampaignDropdownReadPlatformService;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.PlatformEmailService;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageStatusType;
import org.apache.fineract.infrastructure.sms.scheduler.SmsMessageScheduledJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaUserDomainService implements UserDomainService {

    private final AppUserRepository userRepository;
    private final PlatformPasswordEncoder applicationPasswordEncoder;
    private final PlatformEmailService emailService;
    private final SmsCampaignDropdownReadPlatformService smsCampaignDropdownReadPlatformService;
    private final SmsMessageRepository smsMessageRepository;
    private final SmsMessageScheduledJobService smsMessageScheduledJobService;

    @Autowired
    public JpaUserDomainService(final AppUserRepository userRepository, final PlatformPasswordEncoder applicationPasswordEncoder,
            final PlatformEmailService emailService, SmsCampaignDropdownReadPlatformService smsCampaignDropdownReadPlatformService,
            SmsMessageRepository smsMessageRepository, SmsMessageScheduledJobService smsMessageScheduledJobService) {
        this.userRepository = userRepository;
        this.applicationPasswordEncoder = applicationPasswordEncoder;
        this.emailService = emailService;
        this.smsCampaignDropdownReadPlatformService = smsCampaignDropdownReadPlatformService;
        this.smsMessageRepository = smsMessageRepository;
        this.smsMessageScheduledJobService = smsMessageScheduledJobService;
    }

    @Transactional
    @Override
    public void create(final AppUser appUser, final Boolean sendPasswordToEmail, final Boolean pendingActivation) {

        generateKeyUsedForPasswordSalting(appUser);

        final String unencodedPassword = appUser.getPassword();

        final String encodePassword = this.applicationPasswordEncoder.encode(appUser);
        appUser.updatePassword(encodePassword);

        this.userRepository.saveAndFlush(appUser);

        if (sendPasswordToEmail && !pendingActivation) {
            this.emailService.sendToUserAccount(appUser.getOffice().getName(), appUser.getFirstname(), appUser.getEmail(),
                    appUser.getUsername(), unencodedPassword, appUser.isSelfServiceUser());
        }

        if (pendingActivation) {
            this.emailService.sendPendingAccount(appUser.getOffice().getName(), appUser.getFirstname(), appUser.getEmail(),
                    appUser.getUsername(), unencodedPassword);
        }
    }

    private void generateKeyUsedForPasswordSalting(final AppUser appUser) {
        this.userRepository.save(appUser);
    }

    @Override
    public void forgotPassword(String email) {
        List<AppUser> users = userRepository.findAppUserByEmail(email).stream().filter(AppUser::isEnabled).toList();
        for (AppUser user : users) {
            if (user.isDeleted()) {
                continue; // skip deleted accounts
            }
            user.setResetToken(UUID.randomUUID().toString());
            user.setTokenCreationDate(LocalDateTime.now(ZoneId.of("UTC")));
            userRepository.save(user);
            this.emailService.sendForgotPasswordEmail(user.getEmail(), user.getResetToken(), user.getUsername(), user.isSelfServiceUser());
        }
        if (users.size() < 1) {
            throw new PlatformDataIntegrityException("error.msg.invalid.user.email", "No user matching the provided email.");
        }
    }

    @Override
    public void resetPassword(String token) {
        Optional<AppUser> userOptional = Optional.ofNullable(this.userRepository.findAppUserByResetToken(token));
        if (userOptional.isPresent()) {
            AppUser resetUser = userOptional.get();
            resetUser.setResetToken(null);
            resetUser.setTokenCreationDate(null);
            userRepository.save(resetUser);
        }
    }

    @Override
    public void setOtp(AppUser appUser, String phoneNo) {
        Collection<SmsProviderData> smsProviders = this.smsCampaignDropdownReadPlatformService.retrieveSmsProviders();
        if (smsProviders.isEmpty()) {
            throw new PlatformDataIntegrityException("error.msg.mobile.service.provider.not.available",
                    "Mobile service provider not available.");
        }
        Long randomPlaceHolder = 12687L;
        String username = appUser.getUsername();
        String fsp = String.valueOf(ThreadLocalContextUtil.getTenant().getId() + randomPlaceHolder);
        Optional<AppUser> userOptional = Optional.ofNullable(this.userRepository.findAppUserByName(username));
        if (userOptional.isPresent()) {
            AppUser user = userOptional.get();
            Long modifiedUserId = user.getId() + randomPlaceHolder;
            user.setOTPToken(RandomStringUtils.randomNumeric(6));
            user.setTokenCreationDate(LocalDateTime.now(ZoneId.of("UTC")));
            userRepository.save(user);

            Long providerId = new ArrayList<>(smsProviders).get(0).getId();
            final String message = "Hello  " + user.getFirstname() + "!" + "Please dial *284*797# to complete registration on Kanzu Banking"
                    + "\nUsernumber: " + modifiedUserId + "\nPasscode: " + user.getOTPToken() + "\nFinancial Provider Number: " + fsp;

            SmsMessage smsMessage = SmsMessage.instance(null, null, null, null, SmsMessageStatusType.PENDING, message, phoneNo, null,
                    false);
            this.smsMessageRepository.save(smsMessage);
            this.smsMessageScheduledJobService.sendTriggeredMessage(new ArrayList<>(Collections.singletonList(smsMessage)), providerId);
        }
    }

    @Override
    public void invalidateOtp(String otp) {
        Optional<AppUser> appUser = Optional.ofNullable(this.userRepository.findAppUserByOTP(otp));
        if (appUser.isPresent()) {
            AppUser user = appUser.get();
            user.setOTPToken(null);
            user.setTokenCreationDate(null);
            userRepository.save(user);
        }
    }

    @Override
    public List<AppUser> findUsersWithPermissions(String... permissionCodes) {
        List<AppUser> users = userRepository.findAll();
        // because we have lazily granted self-service users all permission in the past, we must explicitly exclude them
        // here until that's fixed
        return users.stream().filter(u -> u.isEnabled() && !u.isSelfServiceUser() && !u.hasNotPermissionForAnyOf(permissionCodes))
                .collect(Collectors.toList());
    }
}
