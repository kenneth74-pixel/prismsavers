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

package org.apache.fineract.extensions.registration;

import com.google.gson.Gson;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.fineract.infrastructure.core.exception.PlatformInternalServerException;
import org.apache.fineract.infrastructure.core.service.migration.TenantDatabaseUpgradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
@Path("/update-tenant")
public class TenantUpdateResource {

    public static class Request {

        public String tenantId;
    }

    private static final Logger LOG = LoggerFactory.getLogger(TenantUpdateResource.class);

    private final TenantDatabaseUpgradeService upgradeService;

    public TenantUpdateResource(TenantDatabaseUpgradeService upgradeService) {
        this.upgradeService = upgradeService;
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    @Consumes({ MediaType.APPLICATION_JSON })
    public String updateTenant(String apiRequestBodyAsJson) {
        TenantUpdateResource.Request req = new Gson().fromJson(apiRequestBodyAsJson, TenantUpdateResource.Request.class);
        try {
            LOG.info("running migrations to configure new tenant database for {}", req.tenantId);
            upgradeService.afterPropertiesSet();
            return "{\"message\": \"Successfully set up account\"}";
        } catch (Exception ex) {
            LOG.warn("failed to configure new tenant database", ex);
            throw new PlatformInternalServerException("error.msg.platform.server.side.error", "Failed to complete tenant setup");
        }
    }

}
