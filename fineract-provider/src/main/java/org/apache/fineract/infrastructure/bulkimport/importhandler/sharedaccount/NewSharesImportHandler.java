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
package org.apache.fineract.infrastructure.bulkimport.importhandler.sharedaccount;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.ClientPersonConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.NewSharesConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.EnumOptionDataIdSerializer;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsActivation;
import org.apache.fineract.portfolio.savings.data.SavingsApproval;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsProduct;
import org.apache.fineract.portfolio.savings.domain.SavingsProductRepository;
import org.apache.fineract.portfolio.shareaccounts.data.ShareAccountData;
import org.apache.fineract.portfolio.shareaccounts.data.ShareActivation;
import org.apache.fineract.portfolio.shareaccounts.data.ShareAdditionalShareApproval;
import org.apache.fineract.portfolio.shareaccounts.data.ShareApplyAdditionalShares;
import org.apache.fineract.portfolio.shareaccounts.data.ShareApproval;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccount;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountRepository;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccountRepositoryWrapper;
import org.apache.fineract.portfolio.shareaccounts.service.ShareAccountCommandsServiceImpl;
import org.apache.fineract.portfolio.shareaccounts.service.ShareAccountWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProduct;
import org.apache.fineract.portfolio.shareproducts.domain.ShareProductRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NewSharesImportHandler implements ImportHandler {

    private Workbook workbook;
    private final ClientRepository repository;
    private final ShareAccountRepository accountRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final ShareAccountWritePlatformServiceJpaRepositoryImpl shareAccountWritePlatformServiceJpaRepository;
    private final ShareAccountCommandsServiceImpl shareAccountCommandsService;
    private final ShareProductRepository sharesProductRepository;
    private final SavingsProductRepository savingsProductRepository;
    private final ShareAccountRepositoryWrapper repositoryWrapper;

    @Autowired
    public NewSharesImportHandler(ClientRepository repository, ShareAccountRepository accountRepository,
            SavingsAccountRepository savingsAccountRepository,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            ShareAccountWritePlatformServiceJpaRepositoryImpl shareAccountWritePlatformServiceJpaRepository,
            ShareAccountCommandsServiceImpl shareAccountCommandsService, ShareProductRepository sharesProductRepository,
            SavingsProductRepository savingsProductRepository, ShareAccountRepositoryWrapper repositoryWrapper) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.shareAccountWritePlatformServiceJpaRepository = shareAccountWritePlatformServiceJpaRepository;
        this.shareAccountCommandsService = shareAccountCommandsService;
        this.sharesProductRepository = sharesProductRepository;
        this.savingsProductRepository = savingsProductRepository;
        this.repositoryWrapper = repositoryWrapper;
    }

    @Override
    public Count process(Workbook workbook, String locale, String dateFormat) {
        this.workbook = workbook;
        return importEntity(locale, dateFormat);
    }

    private Long getClient(String firstname, String lastname) {
        List<Client> clients = this.repository.findAll();
        Long clientId = null;
        for (Client client : clients) {
            if (client.getFirstname().equalsIgnoreCase(firstname) && client.getLastname().equalsIgnoreCase(lastname)) {
                clientId = client.getId();
                break;
            }
        }
        return clientId;
    }

    private Long getSavingsProduct(String productName) {
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

    private Long getSharesProduct(String productName) {
        List<ShareProduct> products = this.sharesProductRepository.findAll();
        Long productId = null;
        for (ShareProduct product : products) {
            if (product.getProductName().equalsIgnoreCase(productName)) {
                productId = product.getId();
                break;
            }
        }
        return productId;
    }

    private SavingsActivation readSavingsActivation(Row row, String locale, String dateFormat) {
        LocalDate activationDate = ImportHandlerUtils.readAsDate(NewSharesConstants.TRANSACTION_DATE_COL, row);
        if (activationDate != null)
            return SavingsActivation.importInstance(activationDate, row.getRowNum(), locale, dateFormat);
        else
            return null;
    }

    private SavingsApproval readSavingsApproval(Row row, String locale, String dateFormat) {
        LocalDate approvalDate = ImportHandlerUtils.readAsDate(NewSharesConstants.TRANSACTION_DATE_COL, row);
        if (approvalDate != null)
            return SavingsApproval.importInstance(approvalDate, row.getRowNum(), locale, dateFormat);
        else
            return null;
    }

    private ShareActivation readShareActivation(Row row, String locale, String dateFormat) {
        LocalDate activationDate = ImportHandlerUtils.readAsDate(NewSharesConstants.TRANSACTION_DATE_COL, row);
        if (activationDate != null)
            return ShareActivation.importInstance(activationDate, row.getRowNum(), locale, dateFormat);
        else
            return null;
    }

    private ShareApproval readShareApproval(Row row, String locale, String dateFormat) {
        LocalDate approvalDate = ImportHandlerUtils.readAsDate(NewSharesConstants.TRANSACTION_DATE_COL, row);
        if (approvalDate != null)
            return ShareApproval.importInstance(approvalDate, row.getRowNum(), locale, dateFormat);
        else
            return null;
    }

    private ShareApplyAdditionalShares readShareApplyAdditionalApproval(Row row, String locale, Integer requestedShares,
            BigDecimal unitPrice, String dateFormat) {
        LocalDate requestedDate = ImportHandlerUtils.readAsDate(NewSharesConstants.TRANSACTION_DATE_COL, row);
        if (requestedDate != null)
            return ShareApplyAdditionalShares.importInstance(requestedDate, row.getRowNum(), locale, requestedShares, unitPrice,
                    dateFormat);
        else
            return null;
    }

    private ShareAdditionalShareApproval readShareApproveAdditionalShare(Row row, String locale, List<Map<String, Object>> requestedShares,
            String dateFormat) {
        LocalDate requestedDate = ImportHandlerUtils.readAsDate(NewSharesConstants.TRANSACTION_DATE_COL, row);
        if (requestedDate != null)
            return ShareAdditionalShareApproval.importInstance(requestedShares, row.getRowNum(), locale, dateFormat);
        else
            return null;
    }

    private void importShareApplyAdditionalShares(Long shareAccountId, Row row, String locale, Integer requestedShares,
            BigDecimal unitPrice, String dateFormat) {
        ShareApplyAdditionalShares approval = readShareApplyAdditionalApproval(row, locale, requestedShares, unitPrice, dateFormat);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        String payload = gsonBuilder.create().toJson(approval);
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createAccountCommand("share", shareAccountId, "applyadditionalshares")//
                .withJson(payload) //
                .build(); //

        CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> idsMap = new HashMap<>();
        idsMap.put("id", result.getChanges().get("additionalshares"));
        list.add(idsMap);
        importApproveAdditionalShare(shareAccountId, row, locale, list, dateFormat);
    }

    private void importApproveAdditionalShare(Long shareAccountId, Row row, String locale, List<Map<String, Object>> requestedShares,
            String dateFormat) {
        ShareAdditionalShareApproval approval = readShareApproveAdditionalShare(row, locale, requestedShares, dateFormat);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        String payload = gsonBuilder.create().toJson(approval);

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createAccountCommand("share", shareAccountId, "approveadditionalshares")//
                .withJson(payload) //
                .build(); //

        CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

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

    private void importShareApproval(Long sharesId, Row row, String locale, String dateFormat) {
        ShareApproval approval = readShareApproval(row, locale, dateFormat);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        String payload = gsonBuilder.create().toJson(approval);

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createAccountCommand("share", sharesId, "approve")//
                .withJson(payload) //
                .build(); //

        commandsSourceWritePlatformService.logCommandSource(commandRequest);

    }

    private void importShareActivation(Long sharesId, Row row, String locale, String dateFormat) {
        ShareActivation activation = readShareActivation(row, locale, dateFormat);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        String payload = gsonBuilder.create().toJson(activation);

        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createAccountCommand("share", sharesId, "activate")//
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

    private CommandProcessingResult importShare(ShareAccountData shareaccount, Row row, String locale, String dateFormat,
            String externalId) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        gsonBuilder.registerTypeAdapter(EnumOptionData.class, new EnumOptionDataIdSerializer());
        JsonObject shareJsonob = gsonBuilder.create().toJsonTree(shareaccount).getAsJsonObject();
        shareJsonob.remove("isDormancyTrackingActive");
        String payload = shareJsonob.toString();
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createAccount("share")//
                .withJson(payload) //
                .build(); //
        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        ShareAccount shareAccount = this.accountRepository.findByExternalId(externalId);
        importShareApproval(shareAccount.getId(), row, locale, dateFormat);
        importShareActivation(shareAccount.getId(), row, locale, dateFormat);

        return result;
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

    //

    public Count importEntity(String locale, String dateFormat) {
        Sheet newsharesSheet = workbook.getSheet(TemplatePopulateImportConstants.NEW_SHARES_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(newsharesSheet, TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        List<Client> clients = this.repository.findAll();
        int successCount = 0;
        int errorCount = 0;
        String errorMessage = "";
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        Long officeId = getOfficeId();
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = newsharesSheet.getRow(rowIndex);
            String firstName = ImportHandlerUtils.readAsString(NewSharesConstants.FIRST_NAME_COL, row);
            String lastName = ImportHandlerUtils.readAsString(NewSharesConstants.LAST_NAME_COL, row);
            LocalDate activationDate = ImportHandlerUtils.readAsDate(NewSharesConstants.TRANSACTION_DATE_COL, row);
            LocalDate submittedOn;
            LocalDate transactionDate;
            submittedOn = activationDate;
            transactionDate = activationDate;
            LocalDate submittedOnDate = ImportHandlerUtils.readAsDate(NewSharesConstants.TRANSACTION_DATE_COL, row);
            Boolean useExternalIds = ImportHandlerUtils.readAsBoolean(NewSharesConstants.USE_EXTERNAL_IDS_COL, row);

            BigDecimal totalNoShares = null;
            if (ImportHandlerUtils.readAsDouble(NewSharesConstants.TOTAL_NO_SHARES_COL, row) != null) {
                totalNoShares = BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(NewSharesConstants.TOTAL_NO_SHARES_COL, row));
            }

            String savingProductName = ImportHandlerUtils.readAsString(NewSharesConstants.SAVINGS_PRODUCT_COL, row);
            String sharesProductName = ImportHandlerUtils.readAsString(NewSharesConstants.SHARE_PRODUCT_COL, row);

            String accountNumber = null;
            String externalId = null;
            String clientExternalId = null;
            if (ImportHandlerUtils.readAsString(NewSharesConstants.ACCOUNT_NO_COL, row) != null) {
                accountNumber = ImportHandlerUtils.readAsString(NewSharesConstants.ACCOUNT_NO_COL, row);
                if (useExternalIds) {
                    externalId = accountNumber;
                } else {
                    externalId = firstName.toLowerCase() + "." + lastName.toLowerCase() + "." + accountNumber;
                }
            }
            String clientExternalIdSheetValue = ImportHandlerUtils.readAsString(NewSharesConstants.CLIENT_EXTERNAL_ID_COL, row);

            if (clientExternalIdSheetValue != null) {
                clientExternalId = clientExternalIdSheetValue;
            }

            BigDecimal amount = null;

            Long legalFormId = 1L;
            Long clientId;
            try {
                if (clients.size() == 0) {
                    ClientData client = ClientData.importClientPersonInstance(legalFormId, row.getRowNum(), firstName, lastName, null,
                            submittedOn, activationDate, true, clientExternalId, officeId, null, null, null, null, null, null, false, null,
                            locale, dateFormat);
                    String payload = gsonBuilder.create().toJson(client);
                    final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                            .createClient() //
                            .withJson(payload) //
                            .build(); //
                    CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
                    clientId = result.getClientId();
                } else {
                    clientId = getClient(firstName, lastName);
                    if (clientId == null) {
                        ClientData client = ClientData.importClientPersonInstance(legalFormId, row.getRowNum(), firstName, lastName, null,
                                submittedOn, activationDate, true, null, 1L, null, null, null, null, null, null, false, null, locale,
                                dateFormat);
                        String payload = gsonBuilder.create().toJson(client);
                        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                                .createClient() //
                                .withJson(payload) //
                                .build(); //
                        CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
                        clientId = result.getClientId();
                    }
                }
                if (externalId != null) {
                    ShareAccount shareAccount = this.accountRepository.findByExternalId(externalId);
                    SavingsAccount savingsAccount = this.savingsAccountRepository.findByExternalId(externalId);

                    if (savingsAccount == null) {
                        Long productId = getSavingsProduct(savingProductName);
                        Long shareProductId = getSharesProduct(sharesProductName);
                        SavingsAccountData savingsaccount = SavingsAccountData.importInstanceIndividual(clientId, productId, 1L,
                                submittedOnDate, null, null, null, null, null, null, null, null, false, row.getRowNum(), externalId, null,
                                false, null, locale, dateFormat);
                        CommandProcessingResult result = importSavings(savingsaccount, row, locale, dateFormat);
                        ShareAccountData shareAccountData = ShareAccountData.importInstance(clientId, shareProductId,
                                totalNoShares.intValue(), externalId, submittedOnDate, null, null, null, null, submittedOnDate, false, null,
                                result.getSavingsId(), row.getRowNum(), locale, dateFormat);
                        CommandProcessingResult shareresult = importShare(shareAccountData, row, locale, dateFormat, externalId);

                    } else {
                        Long productId = getSharesProduct(sharesProductName);

                        if (shareAccount == null) {
                            ShareAccountData shareAccountData = ShareAccountData.importInstance(clientId, productId,
                                    totalNoShares.intValue(), externalId, submittedOnDate, null, null, null, null, submittedOnDate, false,
                                    null, savingsAccount.getId(), row.getRowNum(), locale, dateFormat);
                            CommandProcessingResult shareresult = importShare(shareAccountData, row, locale, dateFormat, externalId);

                        } else {
                            importShareApplyAdditionalShares(shareAccount.getId(), row, locale, totalNoShares.intValue(),
                                    shareAccount.getShareProduct().getUnitPrice(), dateFormat);

                        }

                    }
                }
                successCount++;
                Cell statusCell = newsharesSheet.getRow(rowIndex).createCell(NewSharesConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));

            } catch (RuntimeException ex) {
                errorCount++;
                ex.printStackTrace();
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(newsharesSheet, rowIndex, errorMessage, NewSharesConstants.STATUS_COL);
            }
        }
        newsharesSheet.setColumnWidth(ClientPersonConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(ClientPersonConstants.STATUS_COL,
                newsharesSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COLUMN_HEADER);

        return Count.instance(successCount, errorCount);
    }
}
