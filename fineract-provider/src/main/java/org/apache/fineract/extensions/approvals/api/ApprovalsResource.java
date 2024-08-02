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
import java.util.Collection;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.extensions.approvals.data.ApprovalData;
import org.apache.fineract.extensions.approvals.service.ApprovalReadService;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/approvals")
@Component
@Scope("singleton")
@Tag(name = "Approvals")
public class ApprovalsResource {

    private final PlatformSecurityContext context;
    private final ApprovalReadService approvalReadService;
    private final DefaultToApiJsonSerializer<ApprovalData> toApiJsonSerializerAudit;

    public ApprovalsResource(PlatformSecurityContext context, ApprovalReadService approvalReadService,
            DefaultToApiJsonSerializer<ApprovalData> toApiJsonSerializerAudit) {
        this.context = context;
        this.approvalReadService = approvalReadService;
        this.toApiJsonSerializerAudit = toApiJsonSerializerAudit;
    }

    @GET
    @Path("/{auditId}")
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "List approvals for a given command")
    public String retrieveApprovals(@PathParam("auditId") @Parameter(description = "auditId") final Long auditId) {
        this.context.authenticatedUser().validateHasReadPermission("AUDIT");
        final Collection<ApprovalData> approvals = this.approvalReadService.retrieveApprovals(auditId);
        return this.toApiJsonSerializerAudit.serialize(approvals);
    }

}
