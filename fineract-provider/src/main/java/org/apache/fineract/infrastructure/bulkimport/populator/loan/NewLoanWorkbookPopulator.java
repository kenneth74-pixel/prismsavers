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
package org.apache.fineract.infrastructure.bulkimport.populator.loan;

import java.util.List;
import org.apache.fineract.infrastructure.bulkimport.constants.LoanTransactionsConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.ExtrasSheetPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.OfficeSheetPopulator;
import org.apache.poi.hssf.usermodel.HSSFDataValidationHelper;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;

public class NewLoanWorkbookPopulator extends AbstractWorkbookPopulator {

    private final ExtrasSheetPopulator extrasSheetPopulator;
    private final OfficeSheetPopulator officeSheetPopulator;

    public NewLoanWorkbookPopulator(ExtrasSheetPopulator extrasSheetPopulator, OfficeSheetPopulator officeSheetPopulator) {
        this.extrasSheetPopulator = extrasSheetPopulator;
        this.officeSheetPopulator = officeSheetPopulator;
    }

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        Sheet newloansSheet = workbook.createSheet(TemplatePopulateImportConstants.NEW_LOANS_SHEET_NAME);
        extrasSheetPopulator.populate(workbook, dateFormat);
        officeSheetPopulator.populate(workbook, dateFormat);
        DataFormat fmt = workbook.createDataFormat();
        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setDataFormat(fmt.getFormat("@"));
        newloansSheet.setDefaultColumnStyle(LoanTransactionsConstants.ACCOUNT_NO_COL, textStyle);
        newloansSheet.setDefaultColumnStyle(LoanTransactionsConstants.MOBILE_NO_COL, textStyle);
        CellStyle cellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        newloansSheet.setDefaultColumnStyle(LoanTransactionsConstants.TRANSACTION_DATE_COL, cellStyle);
        setLayout(newloansSheet);
        setRules(newloansSheet);

    }

    private void setLayout(Sheet worksheet) {
        Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        worksheet.setColumnWidth(LoanTransactionsConstants.FIRST_NAME_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.LAST_NAME_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.PRODUCT_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.ACCOUNT_NO_COL, TemplatePopulateImportConstants.LARGE_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.AMOUNT_REPAID_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.TRANSACTION_DATE_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.MOBILE_NO_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.USE_EXTERNAL_IDS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.PRINCIPAL_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.NO_OF_REPAYMENTS, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.PAYMENT_TYPE_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(LoanTransactionsConstants.CLIENT_EXTERNAL_ID_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);

        writeString(LoanTransactionsConstants.FIRST_NAME_COL, rowHeader, "Client First Name*");
        writeString(LoanTransactionsConstants.LAST_NAME_COL, rowHeader, "Client Last Name*");
        writeString(LoanTransactionsConstants.PRODUCT_COL, rowHeader, "Product*");
        writeString(LoanTransactionsConstants.PRINCIPAL_COL, rowHeader, "Principal*");
        writeString(LoanTransactionsConstants.NO_OF_REPAYMENTS, rowHeader, "No. of Repayments*");
        writeString(LoanTransactionsConstants.ACCOUNT_NO_COL, rowHeader, "Loan Account No.*");
        writeString(LoanTransactionsConstants.AMOUNT_REPAID_COL, rowHeader, "Amount Repaid");
        writeString(LoanTransactionsConstants.TRANSACTION_DATE_COL, rowHeader, "Transaction Date*");
        writeString(LoanTransactionsConstants.MOBILE_NO_COL, rowHeader, "Mobile Number");
        writeString(LoanTransactionsConstants.USE_EXTERNAL_IDS_COL, rowHeader, "Use your own externalIds*");
        writeString(LoanTransactionsConstants.PAYMENT_TYPE_COL, rowHeader, "Payment Type");
        writeString(LoanTransactionsConstants.STATUS_COL, rowHeader, "Status");
        writeString(LoanTransactionsConstants.CLIENT_EXTERNAL_ID_COL, rowHeader, "External IDs");
    }

    private void setRules(Sheet worksheet) {
        CellRangeAddressList activeRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                LoanTransactionsConstants.USE_EXTERNAL_IDS_COL, LoanTransactionsConstants.USE_EXTERNAL_IDS_COL);
        CellRangeAddressList paymentTypeRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                LoanTransactionsConstants.PAYMENT_TYPE_COL, LoanTransactionsConstants.PAYMENT_TYPE_COL);

        DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) worksheet);
        setNames(worksheet);
        DataValidationConstraint activeConstraint = validationHelper.createExplicitListConstraint(new String[] { "True", "False" });
        DataValidation activeValidation = validationHelper.createValidation(activeConstraint, activeRange);
        DataValidationConstraint paymentTypeConstraint = validationHelper.createFormulaListConstraint("PaymentTypes");
        DataValidation paymentTypeValidation = validationHelper.createValidation(paymentTypeConstraint, paymentTypeRange);

        worksheet.addValidationData(activeValidation);
        worksheet.addValidationData(paymentTypeValidation);

    }

    private void setNames(Sheet worksheet) {
        Workbook newSavingsWorkbook = worksheet.getWorkbook();
        Name paymentTypeGroup = newSavingsWorkbook.createName();
        List<String> officeNames = officeSheetPopulator.getOfficeNames();

        // Office Names
        Name officeGroup = newSavingsWorkbook.createName();
        officeGroup.setNameName("Office");
        officeGroup.setRefersToFormula(TemplatePopulateImportConstants.OFFICE_SHEET_NAME + "!$B$2:$B$" + (officeNames.size() + 1));

        paymentTypeGroup.setNameName("PaymentTypes");
        paymentTypeGroup.setRefersToFormula(
                TemplatePopulateImportConstants.EXTRAS_SHEET_NAME + "!$D$2:$D$" + (extrasSheetPopulator.getPaymentTypesSize() + 1));

    }
}
