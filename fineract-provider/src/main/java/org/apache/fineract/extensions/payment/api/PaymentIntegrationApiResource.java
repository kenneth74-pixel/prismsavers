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
package org.apache.fineract.extensions.payment.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.data.AuditData;
import org.apache.fineract.extensions.payment.domain.PaymentChannel;
import org.apache.fineract.extensions.payment.service.PaymentClient;
import org.apache.fineract.extensions.payment.service.PaymentResultHandler;
import org.apache.fineract.extensions.payment.service.PaymentValidator;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformInternalServerException;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
@Path("/extensions/payment")
@Tag(name = "Payment Integration", description = "Payment Integration adds interaction with real Money")
public class PaymentIntegrationApiResource {

    private final DefaultToApiJsonSerializer<AuditData> toApiJsonSerializerAudit;
    private final PaymentResultHandler paymentResultHandler;
    private final PaymentValidator paymentValidator;
    private final FromJsonHelper jsonHelper;
    private final PaymentClient paymentClient;

    public PaymentIntegrationApiResource(DefaultToApiJsonSerializer<AuditData> toApiJsonSerializerAudit,
            PaymentResultHandler paymentResultHandler, PaymentValidator paymentValidator, FromJsonHelper jsonHelper,
            PaymentClient paymentClient) {
        this.toApiJsonSerializerAudit = toApiJsonSerializerAudit;
        this.paymentResultHandler = paymentResultHandler;
        this.paymentValidator = paymentValidator;
        this.jsonHelper = jsonHelper;
        this.paymentClient = paymentClient;
    }

    @Path("/available/{channelId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Checks if a given payment channel can be used", method = "POST")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PaymentIntegrationApiResourceSwagger.ChannelAvailabilityResponse.class))) })
    public String checkPaymentEligibility(@PathParam("channelId") int id) {
        // add error handling
        PaymentChannel channel = PaymentChannel.fromInt(id);
        boolean eligible = paymentValidator.canUsePaymentChannel(channel);
        JSONObject response = new JSONObject();
        response.put("canUse", eligible);
        response.put("channelId", channel.getValue());
        response.put("channelName", channel.getCode());
        return response.toJSONString();
    }

    @Path("/callback/{id}")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Updates a pending payment transaction", method = "POST")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PaymentIntegrationApiResourceSwagger.PaymentStatusResponse.class)))
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "OK") })
    public String processPaymentResponse(@Parameter(hidden = true) final String jsonRequestBody, @PathParam("id") Long id,
            @QueryParam("command") @Parameter(description = "command") final String commandParam) {
        var reqBody = jsonHelper.parse(jsonRequestBody);
        String externalReference = jsonHelper.extractStringNamed("externalReference", reqBody);
        CommandProcessingResult result;
        if (is(commandParam, "complete")) {
            result = paymentResultHandler.completeTransaction(id, externalReference);
        } else if (is(commandParam, "cancel")) {
            result = paymentResultHandler.cancelTransaction(id);
        } else {
            throw new UnrecognizedQueryParamException("command", commandParam);
        }
        return this.toApiJsonSerializerAudit.serialize(result);
    }

    @Path("/check")
    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Checks provided account details and returns an account name", method = "POST")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = PaymentIntegrationApiResourceSwagger.CheckAccountRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PaymentIntegrationApiResourceSwagger.CheckAccountResponse.class))) })
    public String checkDestinationAccount(final String jsonRequestBody) {
        this.validateCheckAccountRequest(jsonRequestBody);

        var reqBody = jsonHelper.parse(jsonRequestBody);
        String accountNumber = this.jsonHelper.extractStringNamed("accountNo", reqBody);
        JSONObject request = new JSONObject();
        request.put("accountNumber", accountNumber);
        request.put("tenantId", ThreadLocalContextUtil.getTenant().getTenantIdentifier());

        int channelId = this.jsonHelper.extractIntegerSansLocaleNamed(PaymentApiConstants.channelId, reqBody);
        PaymentChannel channel = PaymentChannel.fromInt(channelId);
        var result = switch (channel) {
            case HFB_WALLET_IN, HFB_WALLET_OUT -> this.paymentClient.postRequest("/api/check-phone", request.toJSONString());
            case HFB_BANK_INTERNAL -> this.paymentClient.postRequest("/api/check-account", request.toJSONString());
            default -> throw new PlatformInternalServerException("error.unsupported.payment.channel",
                    "Unsupported Payment Channel For Account Check");
        };

        JSONObject response = new JSONObject();
        if (result.getStatusCode().is2xxSuccessful()) {
            var respBody = jsonHelper.parse(result.getBody());
            response.put(PaymentApiConstants.accountNameField,
                    jsonHelper.extractStringNamed(PaymentApiConstants.accountNameField, respBody));
        }
        // strange error handling here but should suffice, empty response means we don't know
        return response.toJSONString();
    }

    private void validateCheckAccountRequest(String json) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors);

        var reqBody = jsonHelper.parse(json);
        String accountNumber = this.jsonHelper.extractStringNamed("accountNo", reqBody);
        int channelId = this.jsonHelper.extractIntegerSansLocaleNamed(PaymentApiConstants.channelId, reqBody);
        baseDataValidator.reset().parameter(PaymentApiConstants.channelId).value(channelId).notBlank();
        baseDataValidator.reset().parameter("accountNo").value(accountNumber).notBlank();

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }
}
