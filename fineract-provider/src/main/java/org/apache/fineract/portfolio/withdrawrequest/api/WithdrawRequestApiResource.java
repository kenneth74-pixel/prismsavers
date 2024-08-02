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
package org.apache.fineract.portfolio.withdrawrequest.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.portfolio.self.withdrawrequest.data.WithdrawRequestData;
import org.apache.fineract.portfolio.self.withdrawrequest.domain.SelfWithdrawRequestDetails;
import org.apache.fineract.portfolio.self.withdrawrequest.domain.SelfWithdrawRequestRepository;
import org.apache.fineract.portfolio.self.withdrawrequest.domain.WithdrawRequest;
import org.apache.fineract.portfolio.self.withdrawrequest.domain.WithdrawRequestRepository;
import org.apache.fineract.portfolio.self.withdrawrequest.exception.SelfWithdrawRequestNotFoundException;
import org.apache.fineract.portfolio.self.withdrawrequest.service.SelfWithdrawRequestReadService;
import org.apache.fineract.portfolio.self.withdrawrequest.service.SelfWithdrawRequestWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/withdrawrequest")
@Component
@Scope("singleton")
public class WithdrawRequestApiResource {

    private final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList("id", "isAccepted", "isRejected", "transactionAmount", "transactionDate", "savingsId"));
    private final SelfWithdrawRequestReadService selfWithdrawRequestReadService;
    private final SelfWithdrawRequestWriteService selfWithdrawRequestWriteService;
    private final ToApiJsonSerializer<WithdrawRequestData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final WithdrawRequestRepository withdrawRequestRepository;
    private final SelfWithdrawRequestRepository selfWithdrawRequestRepository;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public WithdrawRequestApiResource(SelfWithdrawRequestReadService selfWithdrawRequestReadService,
            SelfWithdrawRequestWriteService selfWithdrawRequestWriteService, ToApiJsonSerializer<WithdrawRequestData> toApiJsonSerializer,
            ApiRequestParameterHelper apiRequestParameterHelper, WithdrawRequestRepository withdrawRequestRepository,
            SelfWithdrawRequestRepository selfWithdrawRequestRepository,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, FromJsonHelper fromApiJsonHelper) {
        this.selfWithdrawRequestReadService = selfWithdrawRequestReadService;
        this.selfWithdrawRequestWriteService = selfWithdrawRequestWriteService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.withdrawRequestRepository = withdrawRequestRepository;
        this.selfWithdrawRequestRepository = selfWithdrawRequestRepository;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    @GET
    public String retrieveAll(@Context final UriInfo uriInfo) {
        final Collection<WithdrawRequestData> withdrawRequests = this.selfWithdrawRequestReadService.retrievePendingWithdrawRequests();
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, withdrawRequests, this.RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{withdrawRequestId}")
    public String retrieveOne(@PathParam("withdrawRequestId") final Long withdrawRequestId, @Context final UriInfo uriInfo) {
        final WithdrawRequestData withdrawRequest = this.selfWithdrawRequestReadService.retrieveOne(withdrawRequestId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, withdrawRequest, this.RESPONSE_DATA_PARAMETERS);
    }

    @PUT
    @Path("{withdrawRequestId}/accepted")
    public String updateReportToAccepted(@PathParam("withdrawRequestId") final Long withdrawRequestId) {

        final Optional<WithdrawRequest> trxToUpdate = this.withdrawRequestRepository.findById(withdrawRequestId);
        SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy");
        if (trxToUpdate.isPresent()) {
            WithdrawRequest withdrawRequest = trxToUpdate.get();
            this.selfWithdrawRequestWriteService.updateWithdrawRequestToAccepted(withdrawRequestId);
            List<SelfWithdrawRequestDetails> details = this.selfWithdrawRequestRepository.findWithdrawRequestDetailsById(withdrawRequestId);
            for (SelfWithdrawRequestDetails detail : details) {
                JsonObject result = new JsonObject();
                JsonElement paymentChannelDetails = null;

                if (detail.getPaymentChannelDetails() != null) {
                    paymentChannelDetails = this.fromApiJsonHelper.parse(detail.getPaymentChannelDetails());

                }

                result.addProperty("locale", "en");
                result.addProperty("dateFormat", "dd MMMM yyyy");
                result.addProperty("transactionDate", formatter.format(withdrawRequest.getTransactionDate()));
                result.addProperty("transactionAmount", detail.getTransactionAmount());
                if (paymentChannelDetails != null) {
                    result.add("paymentChannelDetails", paymentChannelDetails);
                }

                if (detail.getAccountType().equals("Savings")) {
                    final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(result.toString());
                    final CommandWrapper commandRequest = builder.savingsAccountWithdrawal(detail.getAccountId()).build();
                    CommandProcessingResult response = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
                    System.out.println(this.toApiJsonSerializer.serialize(response));
                }

            }
            JsonObject response = new JsonObject();
            response.addProperty("success", "Status has been updated to accepted");
            return this.toApiJsonSerializer.serialize(response);

        } else {
            throw new SelfWithdrawRequestNotFoundException(withdrawRequestId);
        }

    }

    @PUT
    @Path("{withdrawRequestId}/rejected")
    public String updateWithdrawRequestToRejected(@PathParam("withdrawRequestId") final Long withdrawRequestId) {
        CommandProcessingResult result = this.selfWithdrawRequestWriteService.updateWithdrawRequestToRejected(withdrawRequestId);
        final Optional<WithdrawRequest> withdrawRequestToReject = this.withdrawRequestRepository.findById(withdrawRequestId);
        if (!withdrawRequestToReject.isPresent()) {
            throw new SelfWithdrawRequestNotFoundException(withdrawRequestId);
        }
        return this.toApiJsonSerializer.serialize(result);

    }
}
