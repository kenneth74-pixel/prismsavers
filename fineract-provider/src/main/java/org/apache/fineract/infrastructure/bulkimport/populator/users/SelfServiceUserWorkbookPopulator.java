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
package org.apache.fineract.infrastructure.bulkimport.populator.users;

import java.util.List;
import org.apache.fineract.infrastructure.bulkimport.constants.SelfServiceUserConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.populator.AbstractWorkbookPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.ClientSheetPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.OfficeSheetPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.PersonnelSheetPopulator;
import org.apache.fineract.infrastructure.bulkimport.populator.RoleSheetPopulator;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.poi.hssf.usermodel.HSSFDataValidationHelper;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;

public class SelfServiceUserWorkbookPopulator extends AbstractWorkbookPopulator {

    private List<ClientData> clients;
    private OfficeSheetPopulator officeSheetPopulator;
    private RoleSheetPopulator roleSheetPopulator;
    private ClientSheetPopulator clientSheetPopulator;
    private PersonnelSheetPopulator personnelSheetPopulator;

    public SelfServiceUserWorkbookPopulator(OfficeSheetPopulator officeSheetPopulator, ClientSheetPopulator clientSheetPopulator,
            PersonnelSheetPopulator personnelSheetPopulator, RoleSheetPopulator roleSheetPopulator) {
        this.officeSheetPopulator = officeSheetPopulator;
        this.roleSheetPopulator = roleSheetPopulator;
        this.clientSheetPopulator = clientSheetPopulator;
        this.personnelSheetPopulator = personnelSheetPopulator;

    }

    @Override
    public void populate(Workbook workbook, String dateFormat) {
        Sheet selfserviceuserSheet = workbook.createSheet(TemplatePopulateImportConstants.SELF_SERVICE_USER_SHEET_NAME);
        officeSheetPopulator.populate(workbook, dateFormat);
        roleSheetPopulator.populate(workbook, dateFormat);
        clientSheetPopulator.populate(workbook, dateFormat);
        personnelSheetPopulator.populate(workbook, dateFormat);
        setLayout(selfserviceuserSheet);
        setRules(selfserviceuserSheet);
        setDefaults(selfserviceuserSheet);
    }

    private void setRules(Sheet selfusersheet) {
        CellRangeAddressList officeNameRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                SelfServiceUserConstants.OFFICE_NAME_COL, SelfServiceUserConstants.OFFICE_NAME_COL);
        CellRangeAddressList staffNameRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                SelfServiceUserConstants.STAFF_NAME_COL, SelfServiceUserConstants.STAFF_NAME_COL);
        CellRangeAddressList clientNameRange = new CellRangeAddressList(1, SpreadsheetVersion.EXCEL97.getLastRowIndex(),
                SelfServiceUserConstants.CLIENT_NAME_COL, SelfServiceUserConstants.CLIENT_NAME_COL);

        DataValidationHelper validationHelper = new HSSFDataValidationHelper((HSSFSheet) selfusersheet);
        List<OfficeData> offices = officeSheetPopulator.getOffices();
        setNames(selfusersheet, offices);

        DataValidationConstraint officeNameConstraint = validationHelper.createFormulaListConstraint("Office");
        DataValidationConstraint staffNameConstraint = validationHelper
                .createFormulaListConstraint("INDIRECT(CONCATENATE(\"Staff_\",$A1))");
        DataValidationConstraint clientNameConstraint = validationHelper
                .createFormulaListConstraint("INDIRECT(CONCATENATE(\"Client_\",$A1))");

        DataValidation officeValidation = validationHelper.createValidation(officeNameConstraint, officeNameRange);
        DataValidation staffValidation = validationHelper.createValidation(staffNameConstraint, staffNameRange);
        DataValidation clientValidation = validationHelper.createValidation(clientNameConstraint, clientNameRange);

        selfusersheet.addValidationData(officeValidation);
        selfusersheet.addValidationData(staffValidation);
        selfusersheet.addValidationData(clientValidation);

    }

    private void setNames(Sheet selfusersheet, List<OfficeData> offices) {
        Workbook selfserviceuserWorkbook = selfusersheet.getWorkbook();
        Name officeUser = selfserviceuserWorkbook.createName();
        officeUser.setNameName("Office");
        officeUser.setRefersToFormula(TemplatePopulateImportConstants.OFFICE_SHEET_NAME + "!$B$2:$B$" + (offices.size() + 1));
        Name Roles = selfserviceuserWorkbook.createName();
        Roles.setNameName("Roles");

        for (Integer i = 0; i < offices.size(); i++) {
            Integer[] officeNameToBeginEndIndexesOfStaff = personnelSheetPopulator.getOfficeNameToBeginEndIndexesOfStaff().get(i);
            Integer[] officeNameToBeginEndIndexesOfClients = clientSheetPopulator.getOfficeNameToBeginEndIndexesOfClients().get(i);

            Name userOfficeName = selfserviceuserWorkbook.createName();
            Name clientName = selfserviceuserWorkbook.createName();
            if (officeNameToBeginEndIndexesOfStaff != null) {
                userOfficeName.setNameName("Staff_" + offices.get(i).name().trim().replaceAll("[ )(]", "_"));
                userOfficeName.setRefersToFormula(TemplatePopulateImportConstants.STAFF_SHEET_NAME + "!$B$"
                        + officeNameToBeginEndIndexesOfStaff[0] + ":$B$" + officeNameToBeginEndIndexesOfStaff[1]);
            }
            if (officeNameToBeginEndIndexesOfClients != null) {
                clientName.setNameName("Client_" + offices.get(i).name().trim().replaceAll("[ )(]", "_"));
                clientName.setRefersToFormula(TemplatePopulateImportConstants.CLIENT_SHEET_NAME + "!$B$"
                        + officeNameToBeginEndIndexesOfClients[0] + ":$B$" + officeNameToBeginEndIndexesOfClients[1]);
            }
        }

    }

    private void setLayout(Sheet selfserviceuserSheet) {
        Row rowHeader = selfserviceuserSheet.createRow(TemplatePopulateImportConstants.ROWHEADER_INDEX);
        rowHeader.setHeight(TemplatePopulateImportConstants.ROW_HEADER_HEIGHT);

        selfserviceuserSheet.setColumnWidth(SelfServiceUserConstants.OFFICE_NAME_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        selfserviceuserSheet.setColumnWidth(SelfServiceUserConstants.CLIENT_NAME_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        selfserviceuserSheet.setColumnWidth(SelfServiceUserConstants.STAFF_NAME_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        selfserviceuserSheet.setColumnWidth(SelfServiceUserConstants.USERNAME_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        selfserviceuserSheet.setColumnWidth(SelfServiceUserConstants.FIRSTNAME_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        selfserviceuserSheet.setColumnWidth(SelfServiceUserConstants.LASTNAME_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        selfserviceuserSheet.setColumnWidth(SelfServiceUserConstants.EMAIL_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);
        selfserviceuserSheet.setColumnWidth(SelfServiceUserConstants.ROLE_NAME_START_COL, TemplatePopulateImportConstants.MEDIUM_COL_SIZE);

        writeString(SelfServiceUserConstants.OFFICE_NAME_COL, rowHeader, "Office Name *");
        writeString(SelfServiceUserConstants.STAFF_NAME_COL, rowHeader, "Staff Name");
        writeString(SelfServiceUserConstants.CLIENT_NAME_COL, rowHeader, "Client Name");
        writeString(SelfServiceUserConstants.USERNAME_COL, rowHeader, "User name");
        writeString(SelfServiceUserConstants.FIRSTNAME_COL, rowHeader, "First name ");
        writeString(SelfServiceUserConstants.LASTNAME_COL, rowHeader, "Last/Middle name ");
        writeString(SelfServiceUserConstants.EMAIL_COL, rowHeader, "Email *");
        writeString(SelfServiceUserConstants.ROLE_NAME_START_COL, rowHeader, "Role Name *(Enter in consecutive cells horizontally)");
    }

    private void setDefaults(Sheet worksheet) {
        for (Integer rowNo = 1; rowNo < 1000; rowNo++) {
            Row row = worksheet.createRow(rowNo);
            writeFormula(SelfServiceUserConstants.FIRSTNAME_COL, row, "IF(ISERROR(LEFT($C" + (rowNo + 1) + ",SEARCH(\" \",$C" + (rowNo + 1)
                    + ")-1)),\"\", LEFT($C" + (rowNo + 1) + ",SEARCH(\" \",$C" + (rowNo + 1) + ")-1))");
            writeFormula(SelfServiceUserConstants.LASTNAME_COL, row,
                    "IF(ISERROR(MID($C" + (rowNo + 1) + ", SEARCH(\" \",$C" + (rowNo + 1) + ") +1, SEARCH(\"(\",$C" + (rowNo + 1)
                            + ") - SEARCH(\" \",$C" + (rowNo + 1) + ")-1)),\"\",MID($C" + (rowNo + 1) + ", SEARCH(\" \",$C" + (rowNo + 1)
                            + ") +1, SEARCH(\"(\",$C" + (rowNo + 1) + ") - SEARCH(\" \",$C" + (rowNo + 1) + ")-1))");
            writeFormula(SelfServiceUserConstants.USERNAME_COL, row,
                    "IF(ISERROR(LOWER(LEFT(SUBSTITUTE(C" + (rowNo + 1) + ",\" \",\".\"),SEARCH(\"(\",$C" + (rowNo + 1)
                            + ")-1))),\"\",LOWER(LEFT(SUBSTITUTE(C" + (rowNo + 1) + ",\" \",\".\"),SEARCH(\"(\",$C" + (rowNo + 1)
                            + ")-1)))");
            writeFormula(SelfServiceUserConstants.ROLE_NAME_START_COL, row,
                    "IF(ISBLANK($C" + (rowNo + 1) + "), \"\", \"Self_Service_User\")");
        }
    }
}
