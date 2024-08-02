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
package org.apache.fineract.portfolio.self.withdrawrequest.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.portfolio.self.withdrawrequest.command.WithdrawRequestCommand;
import org.apache.fineract.portfolio.self.withdrawrequest.service.SelfWithdrawRequestWriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/self/withdraw-request")
@Component
@Scope("singleton")
public class SelfWithdrawRequestApiResource {

    private final SelfWithdrawRequestWriteService selfWithdrawRequestWriteService;

    public static class SelfWithdrawRequest {

        public String clientName;
        public String reason;
        public String accountBalance;
        public String transactionDate;
        public JsonArray split;

    }

    private final DefaultToApiJsonSerializer toApiJsonSerializer;

    @Autowired
    public SelfWithdrawRequestApiResource(SelfWithdrawRequestWriteService selfWithdrawRequestWriteService,
            DefaultToApiJsonSerializer toApiJsonSerializer) {
        this.selfWithdrawRequestWriteService = selfWithdrawRequestWriteService;
        this.toApiJsonSerializer = toApiJsonSerializer;

    }

    @POST
    @Path("{clientId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    public String create(@PathParam("clientId") final Long clientId, final String apiRequestBodyAsJson) throws ParseException {

        SelfWithdrawRequestApiResource.SelfWithdrawRequest request = new Gson().fromJson(apiRequestBodyAsJson,
                SelfWithdrawRequestApiResource.SelfWithdrawRequest.class);
        if (request == null) {
            throw new IllegalArgumentException("Field cannot be null " + apiRequestBodyAsJson);
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        final Date transactionDate = format.parse(request.transactionDate);
        final WithdrawRequestCommand withdrawRequestCommand = new WithdrawRequestCommand(request.split, transactionDate, request.reason,
                clientId);
        final Long selfWithdrawRequest = this.selfWithdrawRequestWriteService.createSelfWithdrawRequest(withdrawRequestCommand);
        return this.toApiJsonSerializer.serialize(CommandProcessingResult.resourceResult(selfWithdrawRequest, null));
    }
}
