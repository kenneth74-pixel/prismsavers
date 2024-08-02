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
package org.apache.fineract.infrastructure.core.service;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.WordUtils;
import org.apache.fineract.infrastructure.configuration.data.SMTPCredentialsData;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.domain.ParentOrganisation;
import org.apache.fineract.infrastructure.configuration.domain.ParentOrganisationRepository;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class GmailBackedPlatformEmailService implements PlatformEmailService {

    private final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService;
    private final ParentOrganisationRepository organisationRepository;
    private final ConfigurationDomainService configurationService;

    @Autowired
    public GmailBackedPlatformEmailService(final ExternalServicesPropertiesReadPlatformService externalServicesReadPlatformService,
            ParentOrganisationRepository organisationRepository, ConfigurationDomainService configurationService) {
        this.externalServicesReadPlatformService = externalServicesReadPlatformService;
        this.organisationRepository = organisationRepository;
        this.configurationService = configurationService;
    }

    private String getAppropriatePortal(boolean isSelfService) {
        long parent = configurationService.retrieveParentOrganisation();
        Optional<ParentOrganisation> found = organisationRepository.findById(parent);
        if (found.isPresent()) {
            ParentOrganisation portalConf = found.get();
            return isSelfService ? portalConf.getUserPortal() : portalConf.getAdminPortal();
        }
        // retain defaults for if somebody sets an invalid value for parent organisation
        return isSelfService ? "https://banking.prismsavers.com" : "https://admin.prismsavers.com";
    }

    private String getAppropriateUrl() {
        long parent = configurationService.retrieveParentOrganisation();
        String logo = "https://nrfwqf.stripocdn.email/content/guids/CABINET_0549554ff66b3d6d0b3053c95923a1e6/images/28261608011864644.png";
        Optional<ParentOrganisation> found = organisationRepository.findById(parent);
        if (found.isPresent()) {
            ParentOrganisation portalConf = found.get();
            if (portalConf.getId() == 2) {
                logo = "https://admin.saccoapp.housingfinance.co.ug/images/kbank/hfb-images/hfb-logo.png";
            }
        }
        return logo;
    }

    @Override
    public void sendToUserAccount(String organisationName, String contactName, String address, String username, String unencodedPassword,
            boolean isSelfServiceUser) {

        final String subject = "Welcome to Prism Savers, " + contactName + "!";
        String uri = getAppropriatePortal(isSelfServiceUser);
        String fsp = WordUtils.capitalize(ThreadLocalContextUtil.getTenant().getTenantIdentifier());
        Map<String, Object> params = new HashMap<>();
        params.put("fsp", fsp);
        params.put("username", username);
        params.put("plainPassword", unencodedPassword);
        params.put("email", address);
        params.put("link", uri);
        final String mailHtml = composeEmailFromTemplate("mail-templates/welcome-user.mustache", params);
        final String altBody = "You are receiving this email because your email address: " + address
                + " has been used to create a user account on Prism Savers.\n\n"
                + "You can login using the following credentials:\n\nUsername: " + username + "\n" + "Password: " + unencodedPassword
                + "\n\n" + "Your administrator should have shared the Financial Services Provider with you."
                + "You must change this password upon first log in using Uppercase, Lowercase, number and character.\n"
                + "Thank you and welcome aboard.";
        final EmailDetail emailDetail = new EmailDetail(subject, altBody, address, contactName);
        emailDetail.setHtmlBody(mailHtml);
        sendDefinedEmail(emailDetail);
    }

    @Override
    public void sendPendingAccount(String organisationName, String contactName, String address, String username, String unencodedPassword) {
        final String subject = "Welcome to Prism Savers, " + contactName + "!";
        String uri = getAppropriatePortal(true);
        String fsp = WordUtils.capitalize(ThreadLocalContextUtil.getTenant().getTenantIdentifier());
        Map<String, Object> params = new HashMap<>();
        params.put("fsp", fsp);
        params.put("username", username);
        params.put("plainPassword", unencodedPassword);
        params.put("email", address);
        params.put("link", uri);
        final String mailHtml = composeEmailFromTemplate("mail-templates/pending-user.mustache", params);
        final String altBody = "You are receiving this email because your email address: " + address
                + " has been used to create a user account on Prism Savers.\n\n"
                + "Your administrator has not yet approved your account, so you won't be able to login yet. Please contact them to gain access.\n\n"
                + "Thank you and welcome aboard.";
        final EmailDetail emailDetail = new EmailDetail(subject, altBody, address, contactName);
        emailDetail.setHtmlBody(mailHtml);
        sendDefinedEmail(emailDetail);
    }

    @Override
    public void sendAccountActivated(String contactName, String address, String username) {
        final String subject = contactName + ", You have been approved!";
        String uri = getAppropriatePortal(true);
        String fsp = WordUtils.capitalize(ThreadLocalContextUtil.getTenant().getTenantIdentifier());
        Map<String, Object> params = new HashMap<>();
        params.put("fsp", fsp);
        params.put("username", username);
        params.put("link", uri);
        final String mailHtml = composeEmailFromTemplate("mail-templates/activated-user.mustache", params);
        final String altBody = "You are receiving this email because your email address: " + address
                + " was used to create a user account on Prism Savers.\n\n"
                + "Your administrator has approved your account, so you can now login.\n\n" + "Welcome aboard.";
        final EmailDetail emailDetail = new EmailDetail(subject, altBody, address, contactName);
        emailDetail.setHtmlBody(mailHtml);
        sendDefinedEmail(emailDetail);
    }

    @Override
    public void sendForgotPasswordEmail(String email, String resetToken, String username, boolean isSelfServiceUser) {
        final String subject = isSelfServiceUser ? "Self-Service-Password Reset Request, " + username
                : "Admin-Password Reset Request, " + username;
        final String urlPath = isSelfServiceUser ? "/#!/resetpassword?token=" : "/?token=";
        final String url = getAppropriatePortal(isSelfServiceUser).concat(urlPath);
        final String logoUrl = getAppropriateUrl();
        Map<String, Object> params = new HashMap<>();
        params.put("email", email);
        params.put("token", resetToken);
        params.put("link", url);
        params.put("logo", logoUrl);
        final String resetLink = url + resetToken;
        final String mailHtml = composeEmailFromTemplate("mail-templates/forgot-password.mustache", params);
        final String altBody = "You are receiving this email because we got a password reset request for your account. Please visit "
                + resetLink + " to reset your password.";
        final EmailDetail emailDetail = new EmailDetail(subject, altBody, email, username);
        emailDetail.setHtmlBody(mailHtml);
        sendDefinedEmail(emailDetail);
    }

    @Override
    public void sendMonthlyStatementsEmail(String email, String username, String firstname, String startdate, String enddate, List lrecords,
            List srecords) {
        final String subject = "Monthly Statements";
        Map<String, Object> params = new HashMap<>();
        params.put("startdate", startdate);
        params.put("enddate", enddate);
        params.put("firstname", firstname);
        params.put("lrecords", lrecords);
        params.put("srecords", srecords);
        final String mailHtml = composeEmailFromTemplate("mail-templates/monthly-statements.mustache", params);
        final String altBody = "Your email provider doesn't support HTML but this message cannot be formatted as plain text";
        final EmailDetail emailDetail = new EmailDetail(subject, altBody, email, username);
        emailDetail.setHtmlBody(mailHtml);
        sendDefinedEmail(emailDetail);
    }

    @Override
    public void sendLoanApplicationEmail(String principal, String clientName, String email, String username) {
        final String subject = "Loan Application created by user";
        Map<String, Object> params = new HashMap<>();
        params.put("principal", principal);
        params.put("clientName", clientName);
        params.put("username", username);
        final String mailHtml = composeEmailFromTemplate("mail-templates/loan-application.mustache", params);
        final String altBody = clientName + "has requested a loan of " + principal + ". Please login to portal approve this loan";
        final EmailDetail emailDetail = new EmailDetail(subject, altBody, email, username);
        emailDetail.setHtmlBody(mailHtml);
        sendDefinedEmail(emailDetail);
    }

    @Override
    public void sendEmailCampaign(String message, String clientName, String email, String campaignName) {
        final String subject = campaignName + " Campaign";
        Map<String, Object> params = new HashMap<>();
        params.put("clientName", clientName);
        params.put("message", message);
        final String mailHtml = composeEmailFromTemplate("mail-templates/email-campaigns.mustache", params);
        final String altBody = campaignName + " Campaign";
        final EmailDetail emailDetail = new EmailDetail(subject, altBody, email, clientName);
        emailDetail.setHtmlBody(mailHtml);
        sendDefinedEmail(emailDetail);
    }

    public String composeEmailFromTemplate(String templateName, Map<String, Object> params) {
        final MustacheFactory mf = new DefaultMustacheFactory();
        final Mustache mustache = mf.compile(templateName);
        final StringWriter stringWriter = new StringWriter();

        mustache.execute(stringWriter, params);
        return stringWriter.toString();
    }

    @Override
    public void sendDefinedEmail(EmailDetail emailDetails) {
        final SMTPCredentialsData smtpCredentialsData = this.externalServicesReadPlatformService.getSMTPCredentials();

        final String authuser = smtpCredentialsData.getUsername();
        final String authpwd = smtpCredentialsData.getPassword();

        final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpCredentialsData.getHost()); // smtp.gmail.com
        mailSender.setPort(Integer.parseInt(smtpCredentialsData.getPort())); // 587

        // Important: Enable less secure app access for the gmail account used in the following authentication

        mailSender.setUsername(authuser); // use valid gmail address
        mailSender.setPassword(authpwd); // use password of the above gmail account

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        // props.put("mail.debug", "true");

        // these are the added lines
        props.put("mail.smtp.starttls.enable", "true");

        props.put("mail.smtp.socketFactory.port", Integer.parseInt(smtpCredentialsData.getPort()));
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "true");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(smtpCredentialsData.getFromEmail());
            helper.setTo(emailDetails.getAddress());
            helper.setSubject(emailDetails.getSubject());
            helper.setText(emailDetails.getBody(), emailDetails.getHtmlBody());
            mailSender.send(message);

        } catch (Exception e) {
            throw new PlatformEmailSendException(e);
        }
    }
}
