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
 *
 */

package org.apache.fineract.extensions.approvals.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.extensions.approvals.data.ApprovalSetting;
import org.apache.fineract.extensions.approvals.domain.ApprovalConfiguration;
import org.apache.fineract.extensions.approvals.domain.ApprovalConfigurationRepository;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/approvalSetting")
@Component
@Scope("singleton")
@Tag(name = "Approval Setting", description = "Determines how many approvals are required for a command to be considered approved")
public class SettingResource {

    private final PlatformSecurityContext context;
    private final ApprovalConfigurationRepository configurationRepository;
    private final DefaultToApiJsonSerializer<ApprovalSetting> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    public SettingResource(PlatformSecurityContext context, ApprovalConfigurationRepository configurationRepository,
            DefaultToApiJsonSerializer<ApprovalSetting> toApiJsonSerializer,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.configurationRepository = configurationRepository;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Fetch Setting", description = "Retrieve the approval configuration")
    public String retrieve() {
        this.context.authenticatedUser().validateHasReadPermission("AUDIT");
        ApprovalConfiguration config = configurationRepository.findById(1L).orElseThrow();
        return this.toApiJsonSerializer.serialize(new ApprovalSetting(config));
    }

    @PUT
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Change the setting", description = "Set a new approval configuration")
    public String update(@Parameter(hidden = true) final String jsonRequestBody) {
        final CommandWrapper request = new CommandWrapperBuilder().updateApprovalSetting().withJson(jsonRequestBody).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(request);
        return this.toApiJsonSerializer.serialize(result);
    }
}
