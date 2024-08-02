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
package org.apache.fineract.portfolio.self.documentupload.api;

import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.documentmanagement.command.DocumentCommand;
import org.apache.fineract.infrastructure.documentmanagement.data.DocumentData;
import org.apache.fineract.infrastructure.documentmanagement.service.DocumentWritePlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.self.client.service.AppuserClientMapperReadService;
import org.apache.fineract.portfolio.self.loanaccount.service.AppuserLoansMapperReadService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("self/{entityType}/{entityId}/upload")
@Component
@Scope("singleton")
public class DocumentUploadApiResource {

    private final PlatformSecurityContext context;
    private final DocumentWritePlatformService documentWritePlatformService;
    private final ToApiJsonSerializer<DocumentData> toApiJsonSerializer;
    private final AppuserLoansMapperReadService appuserLoansMapperReadService;
    private final AppuserClientMapperReadService clientMapperReadService;

    @Autowired
    public DocumentUploadApiResource(PlatformSecurityContext context, DocumentWritePlatformService documentWritePlatformService,
            ToApiJsonSerializer<DocumentData> toApiJsonSerializer, AppuserLoansMapperReadService appuserLoansMapperReadService,
            AppuserClientMapperReadService clientMapperReadService) {
        this.context = context;
        this.documentWritePlatformService = documentWritePlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.appuserLoansMapperReadService = appuserLoansMapperReadService;
        this.clientMapperReadService = clientMapperReadService;
    }

    @POST
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createDocument(@PathParam("entityType") final String entityType, @PathParam("entityId") final Long entityId,
            @HeaderParam("Content-Length") final Long fileSize, @FormDataParam("file") final InputStream inputStream,
            @FormDataParam("file") final FormDataContentDisposition fileDetails, @FormDataParam("file") final FormDataBodyPart bodyPart,
            @FormDataParam("name") final String name, @FormDataParam("description") final String description) {

        if (entityType.equals("loans")) {
            validateAppuserEntityMapping(entityId);
            final DocumentCommand documentCommand = new DocumentCommand(null, null, entityType, entityId, name, fileDetails.getFileName(),
                    fileSize, bodyPart.getMediaType().toString(), description, null);

            final Long documentId = this.documentWritePlatformService.createDocument(documentCommand, inputStream);

            return this.toApiJsonSerializer.serialize(CommandProcessingResult.resourceResult(documentId, null));
        }
        if (entityType.equals("clients")) {
            validateAppuserClientsMapping(entityId);
            final DocumentCommand documentCommand = new DocumentCommand(null, null, entityType, entityId, name, fileDetails.getFileName(),
                    fileSize, bodyPart.getMediaType().toString(), description, null);

            final Long documentId = this.documentWritePlatformService.createDocument(documentCommand, inputStream);

            return this.toApiJsonSerializer.serialize(CommandProcessingResult.resourceResult(documentId, null));
        }
        return "Document created";
    }

    private void validateAppuserEntityMapping(final Long entityId) {
        AppUser user = this.context.authenticatedUser();
        final boolean isLoanMappedToUser = this.appuserLoansMapperReadService.isLoanMappedToUser(entityId, user.getId());
        if (!isLoanMappedToUser) {
            throw new LoanNotFoundException(entityId);
        }
    }

    private void validateAppuserClientsMapping(final Long clientId) {
        AppUser user = this.context.authenticatedUser();
        final boolean mappedClientId = this.clientMapperReadService.isClientMappedToUser(clientId, user.getId());
        if (!mappedClientId) {
            throw new ClientNotFoundException(clientId);
        }
    }
}
