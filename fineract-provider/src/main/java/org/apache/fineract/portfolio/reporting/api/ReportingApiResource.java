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

package org.apache.fineract.portfolio.reporting.api;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import net.minidev.json.JSONObject;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.portfolio.self.reporting.data.ReportingData;
import org.apache.fineract.portfolio.self.reporting.domain.Reporting;
import org.apache.fineract.portfolio.self.reporting.domain.ReportingRepository;
import org.apache.fineract.portfolio.self.reporting.domain.SelfReportedDetails;
import org.apache.fineract.portfolio.self.reporting.domain.SelfReportedRepository;
import org.apache.fineract.portfolio.self.reporting.exception.SelfReportNotFoundException;
import org.apache.fineract.portfolio.self.reporting.service.SelfReportingReadService;
import org.apache.fineract.portfolio.self.reporting.service.SelfReportingWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/reporting")
@Component
@Scope("singleton")
public class ReportingApiResource {

    private final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<>(Arrays.asList("id", "paymentType", "paymentProof", "isAccepted",
            "isRejected", "transactionAmount", "transactionDate", "savingsId", "transactionId"));
    private final SelfReportingReadService selfReportingReadService;
    private final SelfReportingWriteService selfReportingWriteService;
    private final ToApiJsonSerializer<ReportingData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final ReportingRepository reportingRepository;
    private final SelfReportedRepository selfReportedRepository;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public ReportingApiResource(SelfReportingReadService selfReportingReadService, SelfReportingWriteService selfReportingWriteService,
            ToApiJsonSerializer<ReportingData> toApiJsonSerializer, ApiRequestParameterHelper apiRequestParameterHelper,
            ReportingRepository reportingRepository, SelfReportedRepository selfReportedRepository,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.selfReportingReadService = selfReportingReadService;
        this.selfReportingWriteService = selfReportingWriteService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.reportingRepository = reportingRepository;
        this.selfReportedRepository = selfReportedRepository;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @GET
    public String retrieveAll(@Context final UriInfo uriInfo) {
        final Collection<ReportingData> reports = this.selfReportingReadService.retrievePendingReports();
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, reports, this.RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{reportId}")
    public String retrieveOne(@PathParam("reportId") final Long reportId, @Context final UriInfo uriInfo) {
        final ReportingData report = this.selfReportingReadService.retrieveOne(reportId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, report, this.RESPONSE_DATA_PARAMETERS);
    }

    @PUT
    @Path("{reportId}/accepted")
    public String updateReportToAccepted(@PathParam("reportId") final Long reportId) {

        final Optional<Reporting> trxToUpdate = this.reportingRepository.findById(reportId);
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy");
        if (trxToUpdate.isPresent()) {
            Reporting report = trxToUpdate.get();
            this.selfReportingWriteService.updateReportToAccepted(reportId);
            List<SelfReportedDetails> details = this.selfReportedRepository.findReportDetailsById(reportId);
            for (SelfReportedDetails detail : details) {
                JSONObject result = new JSONObject();
                result.put("locale", "en");
                result.put("dateFormat", "dd MMMM yyyy");
                result.put("transactionDate", formatter.format(report.getTransactionDate()));
                result.put("transactionAmount", detail.getTransactionAmount());
                result.put("paymentTypeId", report.getPaymentType());

                if (detail.getAccountType().equals("Savings")) {
                    final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(result.toJSONString());
                    final CommandWrapper commandRequest = builder.savingsAccountDeposit(detail.getAccountId()).build();
                    CommandProcessingResult response = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
                    System.out.println(this.toApiJsonSerializer.serialize(response));
                }
                if (detail.getAccountType().equals("Loan")) {
                    final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(result.toJSONString());
                    final CommandWrapper commandRequest = builder.loanRepaymentTransaction(detail.getAccountId()).build();
                    CommandProcessingResult response = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
                    System.out.println(this.toApiJsonSerializer.serialize(response));
                }

            }
            JSONObject response = new JSONObject();
            response.put("success", "Status has been updated to accepted");
            return this.toApiJsonSerializer.serialize(response);

        } else {
            throw new SelfReportNotFoundException(reportId);
        }

    }

    @PUT
    @Path("{reportId}/rejected")
    public String updateReportToRejected(@PathParam("reportId") final Long reportId) {
        CommandProcessingResult result = this.selfReportingWriteService.updateReportToRejected(reportId);
        final Optional<Reporting> reportToReject = this.reportingRepository.findById(reportId);
        if (!reportToReject.isPresent()) {
            throw new SelfReportNotFoundException(reportId);
        }
        return this.toApiJsonSerializer.serialize(result);

    }
}
