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
package org.apache.fineract.infrastructure.bulkimport.importhandler.savings;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.ClientPersonConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.NewSavingsConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.EnumOptionDataIdSerializer;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.SavingsAccountTransactionEnumValueSerialiser;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionEnumData;
import org.apache.fineract.portfolio.savings.data.SavingsActivation;
import org.apache.fineract.portfolio.savings.data.SavingsApproval;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsProduct;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NewSavingsImportHandler implements ImportHandler {

    private Workbook workbook;
    private final ClientRepositoryWrapper repository;
    private final SavingsAccountRepository accountRepository;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final SavingsProductRepository savingsProductRepository;
    private final SavingsAccountRepositoryWrapper repositoryWrapper;

    @Autowired
    public NewSavingsImportHandler(ClientRepositoryWrapper repository, SavingsAccountRepository accountRepository,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            SavingsProductRepository savingsProductRepository, SavingsAccountRepositoryWrapper repositoryWrapper) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.savingsProductRepository = savingsProductRepository;
        this.repositoryWrapper = repositoryWrapper;
    }

    @Override
    public Count process(Workbook workbook, String locale, String dateFormat) {
        this.workbook = workbook;
        return importEntity(locale, dateFormat);
    }

    private Long getProduct(String productName) {
        List<SavingsProduct> products = this.savingsProductRepository.findAll();
        Long productId = null;
        for (SavingsProduct product : products) {
            if (product.getName().equalsIgnoreCase(productName)) {
                productId = product.getId();
                break;
            }
        }
        return productId;
    }

    private SavingsActivation readSavingsActivation(Row row, String locale, String dateFormat) {
        LocalDate activationDate = ImportHandlerUtils.readAsDate(NewSavingsConstants.TRANSACTION_DATE_COL, row);
        if (activationDate != null)
            return SavingsActivation.importInstance(activationDate, row.getRowNum(), locale, dateFormat);
        else
            return null;
    }

    private SavingsApproval readSavingsApproval(Row row, String locale, String dateFormat) {
        LocalDate approvalDate = ImportHandlerUtils.readAsDate(NewSavingsConstants.TRANSACTION_DATE_COL, row);
        if (approvalDate != null)
            return SavingsApproval.importInstance(approvalDate, row.getRowNum(), locale, dateFormat);
        else
            return null;
    }

    private void importSavingsApproval(Long savingsId, Row row, String locale, String dateFormat) {
        SavingsApproval approval = readSavingsApproval(row, locale, dateFormat);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        String payload = gsonBuilder.create().toJson(approval);
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .approveSavingsAccountApplication(savingsId)//
                .withJson(payload) //
                .build(); //
        commandsSourceWritePlatformService.logCommandSource(commandRequest);

    }

    private void importSavingsActivation(Long savingsId, Row row, String locale, String dateFormat) {
        SavingsActivation activation = readSavingsActivation(row, locale, dateFormat);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        String payload = gsonBuilder.create().toJson(activation);
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .savingsAccountActivation(savingsId)//
                .withJson(payload) //
                .build(); //
        commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private CommandProcessingResult importSavings(SavingsAccountData savingaccount, Row row, String locale, String dateFormat) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        gsonBuilder.registerTypeAdapter(EnumOptionData.class, new EnumOptionDataIdSerializer());
        JsonObject savingsJsonob = gsonBuilder.create().toJsonTree(savingaccount).getAsJsonObject();
        savingsJsonob.remove("isDormancyTrackingActive");
        String payload = savingsJsonob.toString();
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createSavingsAccount() //
                .withJson(payload) //
                .build(); //
        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
        importSavingsApproval(result.getSavingsId(), row, locale, dateFormat);
        importSavingsActivation(result.getSavingsId(), row, locale, dateFormat);
        return result;
    }

    private void importSavingTransaction(SavingsAccountTransactionData transaction, Row row, String locale, String dateFormat) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        gsonBuilder.registerTypeAdapter(SavingsAccountTransactionEnumData.class, new SavingsAccountTransactionEnumValueSerialiser());

        JsonObject savingsTransactionJsonob = gsonBuilder.create().toJsonTree(transaction).getAsJsonObject();
        savingsTransactionJsonob.remove("transactionType");
        savingsTransactionJsonob.remove("reversed");
        savingsTransactionJsonob.remove("interestedPostedAsOn");
        savingsTransactionJsonob.remove("isManualTransaction");
        savingsTransactionJsonob.remove("chargesPaidByData");
        String payload = savingsTransactionJsonob.toString();
        CommandWrapper commandRequest = null;
        if (transaction.getTransactionType().getValue().equals("Withdrawal")) {
            commandRequest = new CommandWrapperBuilder() //
                    .savingsAccountWithdrawal(transaction.getSavingsAccountId()) //
                    .withJson(payload) //
                    .build(); //

        } else if (transaction.getTransactionType().getValue().equals("Deposit")) {
            commandRequest = new CommandWrapperBuilder() //
                    .savingsAccountDeposit(transaction.getSavingsAccountId()) //
                    .withJson(payload) //
                    .build();
        }
        commandsSourceWritePlatformService.logCommandSource(commandRequest);

    }

    private Long getOfficeId() {
        final int OFFICE_ID_COL = 0;
        Sheet officeSheet = workbook.getSheet(TemplatePopulateImportConstants.OFFICE_SHEET_NAME);
        Row row;
        int rowIndex = 1; // Begin from the second row to capture the ID of the office

        row = officeSheet.getRow(rowIndex);
        Long officeId = ImportHandlerUtils.readAsLong(OFFICE_ID_COL, row);
        return officeId;
    }

    public Count importEntity(String locale, String dateFormat) {
        Sheet newsavingsSheet = workbook.getSheet(TemplatePopulateImportConstants.NEW_SAVINGS_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(newsavingsSheet, TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage = "";
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        Long officeId = getOfficeId();
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = newsavingsSheet.getRow(rowIndex);
            String firstName = ImportHandlerUtils.readAsString(NewSavingsConstants.FIRST_NAME_COL, row);
            String lastName = ImportHandlerUtils.readAsString(NewSavingsConstants.LAST_NAME_COL, row);
            LocalDate activationDate = ImportHandlerUtils.readAsDate(NewSavingsConstants.TRANSACTION_DATE_COL, row);
            LocalDate submittedOn;
            LocalDate transactionDate;
            submittedOn = activationDate;
            transactionDate = activationDate;
            LocalDate submittedOnDate = ImportHandlerUtils.readAsDate(NewSavingsConstants.TRANSACTION_DATE_COL, row);
            Boolean useExternalIds = ImportHandlerUtils.readAsBoolean(NewSavingsConstants.USE_EXTERNAL_IDS_COL, row);

            BigDecimal minRequiredOpeningBalance = null;
            if (ImportHandlerUtils.readAsDouble(NewSavingsConstants.MIN_OPENING_BALANCE_COL, row) != null) {
                minRequiredOpeningBalance = BigDecimal
                        .valueOf(ImportHandlerUtils.readAsDouble(NewSavingsConstants.MIN_OPENING_BALANCE_COL, row));
            }

            String productName = ImportHandlerUtils.readAsString(NewSavingsConstants.PRODUCT_COL, row);
            String accountNumber = null;
            String externalId = null;
            String clientExternalId = null;
            if (ImportHandlerUtils.readAsString(NewSavingsConstants.ACCOUNT_NO_COL, row) != null) {
                accountNumber = ImportHandlerUtils.readAsString(NewSavingsConstants.ACCOUNT_NO_COL, row);
                if (useExternalIds) {
                    externalId = accountNumber;
                } else {
                    externalId = firstName.toLowerCase() + "." + lastName.toLowerCase() + "." + accountNumber;
                }
            }

            String clientExternalIdSheetValue = ImportHandlerUtils.readAsString(NewSavingsConstants.CLIENT_EXTERNAL_ID_COL, row);

            if (clientExternalIdSheetValue != null) {
                clientExternalId = clientExternalIdSheetValue;
            }

            String transactionType = ImportHandlerUtils.readAsString(NewSavingsConstants.TRANSACTION_TYPE_COL, row);
            SavingsAccountTransactionEnumData savingsAccountTransactionEnumData = new SavingsAccountTransactionEnumData(null, null,
                    transactionType);

            BigDecimal amount = null;
            if (ImportHandlerUtils.readAsDouble(NewSavingsConstants.AMOUNT_COL, row) != null)
                amount = BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(NewSavingsConstants.AMOUNT_COL, row));
            String paymentType = ImportHandlerUtils.readAsString(NewSavingsConstants.PAYMENT_TYPE_COL, row);
            Long paymentTypeId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.EXTRAS_SHEET_NAME),
                    paymentType);
            Long legalFormId = 1L;
            Long clientId;
            try {
                var found = this.repository.findByName(firstName, lastName);
                if (found.isEmpty()) {
                    clientId = createClientFromLimitedData(locale, dateFormat, gsonBuilder, row, firstName, lastName, activationDate,
                            submittedOn, legalFormId, officeId, clientExternalId);
                } else {
                    clientId = found.get();
                }
                if (externalId != null) {
                    SavingsAccount savingsAccount = this.accountRepository.findByExternalId(externalId);
                    if (savingsAccount == null) {
                        Long productId = getProduct(productName);
                        SavingsAccountData savingsaccount = SavingsAccountData.importInstanceIndividual(clientId, productId, 1L,
                                submittedOnDate, null, null, null, null, null, minRequiredOpeningBalance, null, null, false,
                                row.getRowNum(), externalId, null, false, null, locale, dateFormat);
                        CommandProcessingResult result = importSavings(savingsaccount, row, locale, dateFormat);
                        if (amount.compareTo(BigDecimal.ZERO) > 0) {
                            SavingsAccountTransactionData transaction = SavingsAccountTransactionData.importInstance(amount,
                                    transactionDate, 1L, accountNumber, null, null, null, null, result.getSavingsId(),
                                    savingsAccountTransactionEnumData, row.getRowNum(), locale, dateFormat);
                            importSavingTransaction(transaction, row, locale, dateFormat);
                        }

                    } else {
                        SavingsAccountTransactionData transaction = SavingsAccountTransactionData.importInstance(amount, transactionDate,
                                paymentTypeId, savingsAccount.getAccountNumber(), null, null, null, null, savingsAccount.getId(),
                                savingsAccountTransactionEnumData, row.getRowNum(), locale, dateFormat);
                        importSavingTransaction(transaction, row, locale, dateFormat);
                    }
                    successCount++;
                    Cell statusCell = newsavingsSheet.getRow(rowIndex).createCell(NewSavingsConstants.STATUS_COL);
                    statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                    statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
                } else {
                    throw new RuntimeException("The account number is mandatory");
                }

            } catch (RuntimeException ex) {
                errorCount++;
                ex.printStackTrace();
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(newsavingsSheet, rowIndex, errorMessage, NewSavingsConstants.STATUS_COL);
            }
        }
        newsavingsSheet.setColumnWidth(ClientPersonConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(ClientPersonConstants.STATUS_COL,
                newsavingsSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COLUMN_HEADER);

        return Count.instance(successCount, errorCount);
    }

    private Long createClientFromLimitedData(String locale, String dateFormat, GsonBuilder gsonBuilder, Row row, String firstName,
            String lastName, LocalDate activationDate, LocalDate submittedOn, Long legalFormId, Long officeId, String externalId) {
        Long clientId;
        ClientData client = ClientData.importClientPersonInstance(legalFormId, row.getRowNum(), firstName, lastName, null, submittedOn,
                activationDate, true, externalId, officeId, null, null, null, null, null, null, false, null, locale, dateFormat);
        String payload = gsonBuilder.create().toJson(client);
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createClient() //
                .withJson(payload) //
                .build(); //
        CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
        clientId = result.getClientId();
        return clientId;
    }
}
