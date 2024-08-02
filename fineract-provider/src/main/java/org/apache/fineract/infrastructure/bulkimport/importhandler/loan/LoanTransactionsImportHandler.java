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
package org.apache.fineract.infrastructure.bulkimport.importhandler.loan;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.LoanTransactionsConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.EnumOptionDataValueSerializer;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanApprovalData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoanTransactionsImportHandler implements ImportHandler {

    private Workbook workbook;
    private final ClientRepositoryWrapper repository;
    private final LoanProductRepository loanProductRepository;
    private final LoanRepository loanRepository;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    private final LoanProductReadPlatformService loanProductReadPlatformService;

    @Autowired
    public LoanTransactionsImportHandler(ClientRepositoryWrapper repository, LoanProductRepository loanProductRepository,
            LoanRepository loanRepository, final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService,
            LoanProductReadPlatformService loanProductReadPlatformService) {
        this.repository = repository;
        this.loanProductRepository = loanProductRepository;
        this.loanRepository = loanRepository;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.loanProductReadPlatformService = loanProductReadPlatformService;
    }

    @Override
    public Count process(Workbook workbook, String locale, String dateFormat) {
        this.workbook = workbook;

        return importEntity(locale, dateFormat);
    }

    private Long getProduct(String productName) {
        List<LoanProduct> products = this.loanProductRepository.findAll();
        Long productId = null;
        for (LoanProduct product : products) {
            if (product.productName().equalsIgnoreCase(productName)) {
                productId = product.getId();
                break;
            }
        }
        return productId;
    }

    private LoanApprovalData readApprovalDate(Row row, String locale, String dateFormat) {
        LocalDate activationDate = ImportHandlerUtils.readAsDate(LoanTransactionsConstants.TRANSACTION_DATE_COL, row);
        if (activationDate != null)
            return LoanApprovalData.importInstance(activationDate, row.getRowNum(), locale, dateFormat);
        else
            return null;
    }

    private DisbursementData readDisbursementDate(Row row, String locale, String dateFormat) {
        LocalDate disbursementDate = ImportHandlerUtils.readAsDate(LoanTransactionsConstants.TRANSACTION_DATE_COL, row);
        String linkAccountId = null;
        if (disbursementDate != null)
            return DisbursementData.importInstance(disbursementDate, linkAccountId, row.getRowNum(), locale, dateFormat);
        else
            return null;
    }

    private void importLoanApproval(Long loanId, Row row, String locale, String dateFormat) {
        LoanApprovalData approval = readApprovalDate(row, locale, dateFormat);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        String payload = gsonBuilder.create().toJson(approval);
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .approveLoanApplication(loanId)//
                .withJson(payload) //
                .build(); //
        commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private void importLoanDisbursement(Long loanId, Row row, String locale, String dateFormat) {
        DisbursementData disbursement = readDisbursementDate(row, locale, dateFormat);
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        String payload = gsonBuilder.create().toJson(disbursement);
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .disburseLoanApplication(loanId)//
                .withJson(payload) //
                .build(); //
        commandsSourceWritePlatformService.logCommandSource(commandRequest);
    }

    private CommandProcessingResult importLoan(LoanAccountData loan, Row row, String locale, String dateFormat) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        gsonBuilder.registerTypeAdapter(EnumOptionData.class, new EnumOptionDataValueSerializer());
        JsonObject loanJsonOb = gsonBuilder.create().toJsonTree(loan).getAsJsonObject();
        loanJsonOb.remove("isLoanProductLinkedToFloatingRate");
        loanJsonOb.remove("isInterestRecalculationEnabled");
        loanJsonOb.remove("isFloatingInterestRate");
        JsonArray chargesJsonAr = loanJsonOb.getAsJsonArray("charges");
        if (chargesJsonAr != null) {
            for (int i = 0; i < chargesJsonAr.size(); i++) {
                JsonElement chargesJsonElement = chargesJsonAr.get(i);
                JsonObject chargeJsonOb = chargesJsonElement.getAsJsonObject();
                chargeJsonOb.remove("penalty");
                chargeJsonOb.remove("paid");
                chargeJsonOb.remove("waived");
                chargeJsonOb.remove("chargePayable");
            }
        }
        loanJsonOb.remove("isTopup");
        loanJsonOb.remove("isRatesEnabled");
        String payload = loanJsonOb.toString();
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .createLoanApplication() //
                .withJson(payload) //
                .build(); //
        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
        importLoanApproval(result.getLoanId(), row, locale, dateFormat);
        importLoanDisbursement(result.getLoanId(), row, locale, dateFormat);
        return result;
    }

    private void importLoanRepayment(LoanTransactionData transaction, Row row, String locale, String dateFormat) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        JsonObject loanRepaymentJsonob = gsonBuilder.create().toJsonTree(transaction).getAsJsonObject();
        loanRepaymentJsonob.remove("manuallyReversed");
        loanRepaymentJsonob.remove("numberOfRepayments");
        String payload = loanRepaymentJsonob.toString();
        final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                .loanRepaymentTransaction(transaction.getAccountId()) //
                .withJson(payload) //
                .build(); //
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
        Sheet loanSheet = workbook.getSheet(TemplatePopulateImportConstants.NEW_LOANS_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(loanSheet, TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage = "";
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(dateFormat));
        Long officeId = getOfficeId();
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = loanSheet.getRow(rowIndex);
            String firstName = ImportHandlerUtils.readAsString(LoanTransactionsConstants.FIRST_NAME_COL, row);
            String lastName = ImportHandlerUtils.readAsString(LoanTransactionsConstants.LAST_NAME_COL, row);
            LocalDate activationDate = ImportHandlerUtils.readAsDate(LoanTransactionsConstants.TRANSACTION_DATE_COL, row);
            String productName = ImportHandlerUtils.readAsString(LoanTransactionsConstants.PRODUCT_COL, row);
            String mobileNo = null;
            if (ImportHandlerUtils.readAsLong(LoanTransactionsConstants.MOBILE_NO_COL, row) != null)
                mobileNo = "0" + ImportHandlerUtils.readAsLong(LoanTransactionsConstants.MOBILE_NO_COL, row).toString();
            EnumOptionData loanTypeEnumOption = null;
            loanTypeEnumOption = new EnumOptionData(null, null, "individual");
            BigDecimal principal = null;
            String externalId = null;
            String clientExternalId = null;

            String clientExternalIdSheetValue = ImportHandlerUtils.readAsString(LoanTransactionsConstants.CLIENT_EXTERNAL_ID_COL, row);

            if (clientExternalIdSheetValue != null) {
                clientExternalId = clientExternalIdSheetValue;
            }

            if (ImportHandlerUtils.readAsDouble(LoanTransactionsConstants.PRINCIPAL_COL, row) != null)
                principal = BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(LoanTransactionsConstants.PRINCIPAL_COL, row));
            Integer numberOfRepayments = ImportHandlerUtils.readAsInt(LoanTransactionsConstants.NO_OF_REPAYMENTS, row);
            Integer loanTerm = numberOfRepayments;
            LocalDate submittedOn;
            submittedOn = activationDate;
            Long legalFormId = 1L;
            Long clientId;
            EnumOptionData loanTermFrequencyEnum = null;
            String loanTermFrequencyId = "2";
            loanTermFrequencyEnum = new EnumOptionData(null, null, loanTermFrequencyId);
            BigDecimal amount = null;
            if (ImportHandlerUtils.readAsDouble(LoanTransactionsConstants.AMOUNT_REPAID_COL, row) != null)
                amount = BigDecimal.valueOf(ImportHandlerUtils.readAsDouble(LoanTransactionsConstants.AMOUNT_REPAID_COL, row));
            String repaymentType = ImportHandlerUtils.readAsString(LoanTransactionsConstants.PAYMENT_TYPE_COL, row);
            Long repaymentTypeId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.EXTRAS_SHEET_NAME),
                    repaymentType);
            Boolean useExternalIds = ImportHandlerUtils.readAsBoolean(LoanTransactionsConstants.USE_EXTERNAL_IDS_COL, row);
            String accountNumber = null;
            if (ImportHandlerUtils.readAsString(LoanTransactionsConstants.ACCOUNT_NO_COL, row) != null) {
                accountNumber = ImportHandlerUtils.readAsString(LoanTransactionsConstants.ACCOUNT_NO_COL, row);
                if (useExternalIds) {
                    externalId = accountNumber;
                } else {
                    externalId = firstName.toLowerCase() + "." + lastName.toLowerCase() + "." + accountNumber;
                }
            }
            try {
                var found = this.repository.findByName(firstName, lastName);
                if (found.isEmpty()) {
                    clientId = createClientFromLimitedData(locale, dateFormat, gsonBuilder, row, firstName, lastName, activationDate,
                            mobileNo, submittedOn, legalFormId, officeId, clientExternalId);
                } else {
                    clientId = found.get();
                }
                if (externalId != null) {
                    Loan loanaccount = this.loanRepository.findActiveLoanByExternalId(externalId);
                    if (loanaccount == null) {
                        Long productId = getProduct(productName);
                        LoanProductData detail = this.loanProductReadPlatformService.retrieveLoanProduct(productId);
                        EnumOptionData repaidEveryFrequencyEnums = null;
                        String repaidEveryFrequencyId = String.valueOf(detail.getRepaymentFrequencyType().getId());
                        repaidEveryFrequencyEnums = new EnumOptionData(null, null, repaidEveryFrequencyId);
                        EnumOptionData interestMethodEnum = null;
                        String interestMethodId = String.valueOf(detail.getInterestType().getId());
                        interestMethodEnum = new EnumOptionData(null, null, interestMethodId);
                        String interestCalculationPeriodTypeId = String.valueOf(detail.getInterestCalculationPeriodType().getId());
                        EnumOptionData interestCalculationPeriodTypeEnums = null;
                        interestCalculationPeriodTypeEnums = new EnumOptionData(null, null, interestCalculationPeriodTypeId);
                        String amortizationId = String.valueOf(detail.getAmortizationType().getId());
                        EnumOptionData amortizationEnums = null;
                        amortizationEnums = new EnumOptionData(null, null, amortizationId);

                        LoanAccountData loan = LoanAccountData.importInstanceIndividual(loanTypeEnumOption, clientId, productId, 1L,
                                submittedOn, null, principal, numberOfRepayments, detail.getRepaymentEvery(), repaidEveryFrequencyEnums,
                                loanTerm, loanTermFrequencyEnum, detail.getInterestRatePerPeriod(), submittedOn, amortizationEnums,
                                interestMethodEnum, interestCalculationPeriodTypeEnums, detail.getInArrearsTolerance(),
                                detail.getTransactionProcessingStrategyId(), detail.getGraceOnPrincipalPayment(),
                                detail.getGraceOnInterestPayment(), detail.getGraceOnInterestCharged(), null, null, row.getRowNum(),
                                externalId, null, null, null, locale, dateFormat, null);
                        CommandProcessingResult loanresult = importLoan(loan, row, locale, dateFormat);
                        if (amount != null && repaymentType != null) {
                            LoanTransactionData transaction = LoanTransactionData.importInstance(amount, activationDate, repaymentTypeId,
                                    accountNumber, null, null, null, null, loanresult.getLoanId(), "", row.getRowNum(), locale, dateFormat);
                            importLoanRepayment(transaction, row, locale, dateFormat);
                        }

                    } else {
                        LoanTransactionData transaction = LoanTransactionData.importInstance(amount, activationDate, repaymentTypeId,
                                loanaccount.getAccountNumber(), null, null, null, null, loanaccount.getId(), "", row.getRowNum(), locale,
                                dateFormat);
                        importLoanRepayment(transaction, row, locale, dateFormat);
                    }
                    successCount++;
                    Cell statusCell = loanSheet.getRow(rowIndex).createCell(LoanTransactionsConstants.STATUS_COL);
                    statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                    statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));
                } else {
                    throw new RuntimeException("The account number is mandatory");
                }

            } catch (RuntimeException ex) {
                errorCount++;
                ex.printStackTrace();
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(loanSheet, rowIndex, errorMessage, LoanTransactionsConstants.STATUS_COL);
            }

        }

        return Count.instance(successCount, errorCount);
    }

    private Long createClientFromLimitedData(String locale, String dateFormat, GsonBuilder gsonBuilder, Row row, String firstName,
            String lastName, LocalDate activationDate, String mobileNo, LocalDate submittedOn, Long legalFormId, Long officeId,
            String externalId) {
        Long clientId;
        ClientData client = ClientData.importClientPersonInstance(legalFormId, row.getRowNum(), firstName, lastName, null, submittedOn,
                activationDate, true, externalId, officeId, null, mobileNo, null, null, null, null, false, null, locale, dateFormat);
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
