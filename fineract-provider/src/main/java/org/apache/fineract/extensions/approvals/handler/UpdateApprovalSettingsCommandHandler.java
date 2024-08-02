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

package org.apache.fineract.extensions.approvals.handler;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.extensions.approvals.domain.ApprovalConfiguration;
import org.apache.fineract.extensions.approvals.domain.ApprovalConfigurationRepository;
import org.apache.fineract.extensions.approvals.domain.ApprovalMode;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.springframework.stereotype.Service;

@Service
@CommandType(entity = "APPROVAL_SETTINGS", action = "UPDATE")
public class UpdateApprovalSettingsCommandHandler implements NewCommandSourceHandler {

    private final FromJsonHelper fromApiJsonHelper;
    private final ApprovalConfigurationRepository configurationRepository;
    private final PlatformSecurityContext context;
    private final AppUserRepository userService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    public UpdateApprovalSettingsCommandHandler(FromJsonHelper fromApiJsonHelper, ApprovalConfigurationRepository configurationRepository,
            PlatformSecurityContext context, AppUserRepository userService,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.configurationRepository = configurationRepository;
        this.context = context;
        this.userService = userService;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        this.validate(command.json());
        this.context.authenticatedUser().validateHasPermissionTo("UPDATE_APPROVAL_SETTINGS");
        String mode = command.stringValueOfParameterNamed("approvalMode");
        Integer required = command.integerValueOfParameterNamed("requiredApprovals");
        Long userId = command.longValueOfParameterNamed("requiredUser");
        AppUser user = null;
        // later check that the user meets certain criteria; not self-service, active, etc
        if (userId != null) {
            user = userService.findById(userId).orElseThrow();
        }
        toggleMakerChecker(required);
        ApprovalConfiguration config = configurationRepository.findById(1L).orElseThrow();
        config.setApprovalMode(ApprovalMode.valueOf(mode));
        config.setRequiredApprovals(required);
        config.setRequiredUser(user);
        configurationRepository.save(config);
        // later set `changes` so we can see just what the command modified
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).build();
    }

    private void validate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Set<String> supportedParameters = new HashSet<>(List.of("approvalMode", "requiredApprovals", "requiredUser", "locale"));

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors);

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        String mode = this.fromApiJsonHelper.extractStringNamed("approvalMode", element);
        try {
            ApprovalMode validMode = ApprovalMode.valueOf(mode);
            Long userId = this.fromApiJsonHelper.extractLongNamed("requiredUser", element);
            if (validMode.equals(ApprovalMode.LEADER)) {
                baseDataValidator.reset().parameter("requiredUser").value(userId).notBlank().notNull();
            }
        } catch (RuntimeException e) {
            baseDataValidator.reset().parameter("approvalMode").value(mode).isOneOfEnumValues(ApprovalMode.class);
        }

        Integer required = this.fromApiJsonHelper.extractIntegerSansLocaleNamed("requiredApprovals", element);
        baseDataValidator.reset().parameter("requiredApprovals").value(required).integerGreaterThanZero();

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    /*
     * We disable maker checker if the signing mandate is for one approver because a signing mandate of one approver
     * (single) is the same as having maker-checker OFF. With more than one approver or when the approver mode is
     * switched on as "All", we turn maker-checker ON
     *
     */

    public void toggleMakerChecker(Integer requiredApprovals) {
        JSONObject payload = new JSONObject();
        if (requiredApprovals == 1) {
            payload.put("enabled", "false");
        } else {
            payload.put("enabled", "true");
        }
        Long makerCheckerConfigurationID = 1L;
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .updateGlobalConfiguration(makerCheckerConfigurationID).withJson(payload.toJSONString()) //
                .build();
        this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }
}
