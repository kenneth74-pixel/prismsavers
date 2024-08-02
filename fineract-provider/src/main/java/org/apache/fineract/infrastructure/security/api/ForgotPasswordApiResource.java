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
package org.apache.fineract.infrastructure.security.api;

import com.google.gson.Gson;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import net.minidev.json.JSONObject;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.UserDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/forgot-password")
@Component
@Scope("singleton")
public class ForgotPasswordApiResource {

    public static class ForgotPasswordRequest {

        public String email;
    }

    private final UserDomainService userDomainService;
    private final DefaultToApiJsonSerializer toApiJsonSerializer;
    private final AppUserRepository userRepository;

    @Autowired
    public ForgotPasswordApiResource(UserDomainService userDomainService, DefaultToApiJsonSerializer toApiJsonSerializer,
            AppUserRepository userRepository) {
        this.userDomainService = userDomainService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.userRepository = userRepository;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public String forgotPassword(String apiRequestBodyAsJson) {
        ForgotPasswordApiResource.ForgotPasswordRequest request = new Gson().fromJson(apiRequestBodyAsJson,
                ForgotPasswordApiResource.ForgotPasswordRequest.class);
        if (request == null) {
            throw new IllegalArgumentException("Field cannot be null " + apiRequestBodyAsJson);
        }

        this.userDomainService.forgotPassword(request.email);
        JSONObject result = new JSONObject();
        result.put("success", "Reset password email has been sent");
        return this.toApiJsonSerializer.serialize(result);
    }
}
