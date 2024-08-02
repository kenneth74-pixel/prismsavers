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
package org.apache.fineract.infrastructure.bulkimport.populator.shareaccount;

import java.util.List;
import org.apache.fineract.infrastructure.bulkimport.constants.NewSharesConstants;
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

public class NewSharesWorkbookPopulator extends AbstractWorkbookPopulator {

    private ExtrasSheetPopulator extrasSheetPopulator;
    private final OfficeSheetPopulator officeSheetPopulator;

    public NewSharesWorkbookPopulator(OfficeSheetPopulator officeSheetPopulator) {
        this.officeSheetPopulator = officeSheetPopulator;
    }

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        Sheet newsavingsSheet = workbook.createSheet(TemplatePopulateImportConstants.NEW_SHARES_SHEET_NAME);
        officeSheetPopulator.populate(workbook, dateFormat);
        DataFormat fmt = workbook.createDataFormat();
        CellStyle textStyle = workbook.createCellStyle();
        textStyle.setDataFormat(fmt.getFormat("@"));
        newsavingsSheet.setDefaultColumnStyle(NewSharesConstants.ACCOUNT_NO_COL, textStyle);
        CellStyle cellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd"));
        newsavingsSheet.setDefaultColumnStyle(NewSharesConstants.TRANSACTION_DATE_COL, cellStyle);
        setLayout(newsavingsSheet);
        setRules(newsavingsSheet);
    }

    private void setLayout(Sheet worksheet) {
        Row rowHeader = worksheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);
        worksheet.setColumnWidth(NewSharesConstants.FIRST_NAME_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(NewSharesConstants.LAST_NAME_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(NewSharesConstants.SHARE_PRODUCT_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(NewSharesConstants.SAVINGS_PRODUCT_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(NewSharesConstants.ACCOUNT_NO_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(NewSharesConstants.TRANSACTION_DATE_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        worksheet.setColumnWidth(NewSharesConstants.TOTAL_NO_SHARES_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(NewSharesConstants.USE_EXTERNAL_IDS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        worksheet.setColumnWidth(NewSharesConstants.CLIENT_EXTERNAL_ID_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);

        writeString(NewSharesConstants.FIRST_NAME_COL, rowHeader, "Client First Name*");
        writeString(NewSharesConstants.LAST_NAME_COL, rowHeader, "Client Last Name*");
        writeString(NewSharesConstants.SHARE_PRODUCT_COL, rowHeader, "Share Product Name");
        writeString(NewSharesConstants.SAVINGS_PRODUCT_COL, rowHeader, "Savings Product Name");
        writeString(NewSharesConstants.ACCOUNT_NO_COL, rowHeader, "Shares Account No.");
        writeString(NewSharesConstants.TRANSACTION_DATE_COL, rowHeader, "Transaction Date*");
        writeString(NewSharesConstants.TOTAL_NO_SHARES_COL, rowHeader, "Total Number Of Shares");
        writeString(NewSharesConstants.USE_EXTERNAL_IDS_COL, rowHeader, "Use your own externalIds*");
        writeString(NewSharesConstants.CLIENT_EXTERNAL_ID_COL, rowHeader, "External IDs");

    }

    private void setRules(Sheet worksheet) {
        CellRangeAddressList activeRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                NewSharesConstants.USE_EXTERNAL_IDS_COL, NewSharesConstants.USE_EXTERNAL_IDS_COL);

        setNames(worksheet);
        DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) worksheet);
        DataValidationConstraint activeConstraint = validationHelper.createExplicitListConstraint(new String[] { "True", "False" });
        DataValidation activeValidation = validationHelper.createValidation(activeConstraint, activeRange);
        worksheet.addValidationData(activeValidation);

    }

    private void setNames(Sheet worksheet) {
        Workbook newSavingsWorkbook = worksheet.getWorkbook();
        List<String> officeNames = officeSheetPopulator.getOfficeNames();

        // Office Names
        Name officeGroup = newSavingsWorkbook.createName();
        officeGroup.setNameName("Office");
        officeGroup.setRefersToFormula(TemplatePopulateImportConstants.OFFICE_SHEET_NAME + "!$B$2:$B$" + (officeNames.size() + 1));

    }

}
