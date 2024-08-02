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
package org.apache.fineract.extensions.payment.service;

import com.google.gson.JsonElement;
import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.commands.domain.CommandSourceRepository;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.exception.CommandNotFoundException;
import org.apache.fineract.commands.service.CommandProcessingService;
import org.apache.fineract.extensions.payment.exception.TransactionNotPendingPayment;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.stereotype.Component;

@Component
public class PaymentResultHandler {

    private final CommandSourceRepository commandSourceRepository;
    private final FromJsonHelper jsonHelper;
    private final CommandProcessingService commandProcessingService;

    public PaymentResultHandler(CommandSourceRepository commandSourceRepository, FromJsonHelper jsonHelper,
            CommandProcessingService commandProcessingService) {
        this.commandSourceRepository = commandSourceRepository;
        this.jsonHelper = jsonHelper;
        this.commandProcessingService = commandProcessingService;
    }

    public CommandProcessingResult completeTransaction(Long transactionId, String paymentRef) {
        CommandSource commandSource = validatePaymentTransaction(transactionId);
        commandSource.markAsPaymentCompleted();
        this.commandSourceRepository.save(commandSource);

        final CommandWrapper wrapper = CommandWrapper.fromExistingCommand(transactionId, commandSource.getActionName(),
                commandSource.getEntityName(), commandSource.resourceId(), commandSource.subresourceId(), commandSource.getResourceGetUrl(),
                commandSource.getProductId(), commandSource.getOfficeId(), commandSource.getGroupId(), commandSource.getClientId(),
                commandSource.getLoanId(), commandSource.getSavingsId(), commandSource.getTransactionId(),
                commandSource.getCreditBureauId(), commandSource.getOrganisationCreditBureauId(), commandSource.json());

        final JsonElement parsedCommand = this.jsonHelper.parse(commandSource.json());
        // this relies on there being PaymentDetails on all supported transaction types
        var source = parsedCommand.getAsJsonObject();
        source.addProperty("receiptNumber", paymentRef);
        final JsonElement modifiedCommand = this.jsonHelper.parse(source.toString());
        final String modifiedJson = this.jsonHelper.toJson(modifiedCommand);

        final JsonCommand command = JsonCommand.fromExistingCommand(transactionId, modifiedJson, modifiedCommand, this.jsonHelper,
                commandSource.getEntityName(), commandSource.resourceId(), commandSource.subresourceId(), commandSource.getGroupId(),
                commandSource.getClientId(), commandSource.getLoanId(), commandSource.getSavingsId(), commandSource.getTransactionId(),
                commandSource.getResourceGetUrl(), commandSource.getProductId(), commandSource.getCreditBureauId(),
                commandSource.getOrganisationCreditBureauId());

        return this.commandProcessingService.processAndLogCommand(wrapper, command, true);
    }

    public CommandProcessingResult cancelTransaction(Long transactionId) {
        CommandSource commandSource = validatePaymentTransaction(transactionId);
        commandSource.markAsPaymentFailed();
        this.commandSourceRepository.save(commandSource);
        return CommandProcessingResult.commandOnlyResult(transactionId);
    }

    private CommandSource validatePaymentTransaction(final Long transactionId) {
        CommandSource sourceCommand = this.commandSourceRepository.findById(transactionId)
                .orElseThrow(() -> new CommandNotFoundException(transactionId));
        if (!sourceCommand.isMarkedAsAwaitingPayment()) {
            throw new TransactionNotPendingPayment(transactionId);
        }
        return sourceCommand;
    }
}
