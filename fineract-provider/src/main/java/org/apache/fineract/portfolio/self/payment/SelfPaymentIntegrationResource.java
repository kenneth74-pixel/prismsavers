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
package org.apache.fineract.portfolio.self.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.extensions.payment.api.PaymentIntegrationApiResource;
import org.apache.fineract.extensions.payment.api.PaymentIntegrationApiResourceSwagger;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
@Path("/extensions/payment/self")
@Tag(name = "Payment Integration", description = "Payment Integration adds interaction with real Money")
public class SelfPaymentIntegrationResource {

    private final PaymentIntegrationApiResource apiResource;

    public SelfPaymentIntegrationResource(PaymentIntegrationApiResource apiResource) {
        this.apiResource = apiResource;
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
        return apiResource.checkDestinationAccount(jsonRequestBody);
    }

    @Path("/available/{channelId}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Checks if a given payment channel can be used", method = "POST")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = PaymentIntegrationApiResourceSwagger.ChannelAvailabilityResponse.class))) })
    public String checkPaymentEligibility(@PathParam("channelId") int id) {
        return apiResource.checkPaymentEligibility(id);
    }
}
