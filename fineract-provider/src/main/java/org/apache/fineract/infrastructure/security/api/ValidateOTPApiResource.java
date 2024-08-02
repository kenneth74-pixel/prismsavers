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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import net.minidev.json.JSONObject;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.UserDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Path("/self/validate")
@Component
@Scope("singleton")
public class ValidateOTPApiResource {

    public static class OTPRequest {

        public String password;
        public String repeatPassword;
    }

    private static final long EXPIRE_TOKEN_AFTER_MINUTES = 60 * 24;
    private final UserDomainService userDomainService;
    private final AppUserRepository userRepository;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final DefaultToApiJsonSerializer toApiJsonSerializer;
    private final GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    @Autowired
    public ValidateOTPApiResource(UserDomainService userDomainService, AppUserRepository userRepository,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, DefaultToApiJsonSerializer toApiJsonSerializer) {
        this.userDomainService = userDomainService;
        this.userRepository = userRepository;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.toApiJsonSerializer = toApiJsonSerializer;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    public String validateOTP(@QueryParam("otp") String otp, String apiRequestBodyAsJson) {
        ValidateOTPApiResource.OTPRequest request = new Gson().fromJson(apiRequestBodyAsJson, ValidateOTPApiResource.OTPRequest.class);
        if (request.password == null) {
            return "Invalid JSON";
        }
        Optional<AppUser> user = Optional.ofNullable(this.userRepository.findAppUserByOTP(otp));

        if (!user.isPresent()) {
            JSONObject result = new JSONObject();
            result.put("not-found", "Invalid OTP provided");
            return this.toApiJsonSerializer.serialize(result);
        } else if (isTokenExpired(user.get().getTokenCreationDate())) {
            JSONObject result = new JSONObject();
            result.put("expired", "OTP has expired");
            return this.toApiJsonSerializer.serialize(result);
        } else {
            AppUser appUser = user.get();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(appUser, appUser.getPassword(),
                    authoritiesMapper.mapAuthorities(appUser.getAuthorities()));
            SecurityContextHolder.getContext().setAuthentication(auth);
            final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                    .updateUser(appUser.getId()) //
                    .withJson(apiRequestBodyAsJson) //
                    .build();
            final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
            this.userDomainService.invalidateOtp(otp);
            return this.toApiJsonSerializer.serialize(result);
        }

    }

    private boolean isTokenExpired(final LocalDateTime tokenCreationDate) {

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        Duration diff = Duration.between(tokenCreationDate, now);

        return diff.toMinutes() >= EXPIRE_TOKEN_AFTER_MINUTES;
    }

}
