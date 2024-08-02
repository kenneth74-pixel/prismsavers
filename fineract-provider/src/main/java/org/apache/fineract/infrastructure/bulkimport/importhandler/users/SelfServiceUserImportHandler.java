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
package org.apache.fineract.infrastructure.bulkimport.importhandler.users;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.bulkimport.constants.SelfServiceUserConstants;
import org.apache.fineract.infrastructure.bulkimport.constants.TemplatePopulateImportConstants;
import org.apache.fineract.infrastructure.bulkimport.data.Count;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandler;
import org.apache.fineract.infrastructure.bulkimport.importhandler.ImportHandlerUtils;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SelfServiceUserImportHandler implements ImportHandler {

    private Workbook workbook;
    private List<AppUserData> users;
    private List<String> statuses;

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public SelfServiceUserImportHandler(PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @Override
    public Count process(Workbook workbook, String locale, String dateFormat) {
        this.workbook = workbook;
        users = new ArrayList<>();
        statuses = new ArrayList<>();
        readExcelFile();
        return importEntity(dateFormat);
    }

    private void readExcelFile() {
        Sheet selfserviceusersSheet = workbook.getSheet(TemplatePopulateImportConstants.SELF_SERVICE_USER_SHEET_NAME);
        Integer noOfEntries = ImportHandlerUtils.getNumberOfRows(selfserviceusersSheet, TemplatePopulateImportConstants.FIRST_COLUMN_INDEX);
        for (int rowIndex = 1; rowIndex <= noOfEntries; rowIndex++) {
            Row row;
            row = selfserviceusersSheet.getRow(rowIndex);
            if (ImportHandlerUtils.isNotImported(row, SelfServiceUserConstants.STATUS_COL)) {
                users.add(readUsers(row));
            }
        }
    }

    private AppUserData readUsers(Row row) {
        String officeName = ImportHandlerUtils.readAsString(SelfServiceUserConstants.OFFICE_NAME_COL, row);
        Long officeId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.OFFICE_SHEET_NAME), officeName);
        String staffName = ImportHandlerUtils.readAsString(SelfServiceUserConstants.STAFF_NAME_COL, row);
        Long staffId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.STAFF_SHEET_NAME), staffName);
        String clientName = ImportHandlerUtils.readAsString(SelfServiceUserConstants.CLIENT_NAME_COL, row);
        Long clientId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.CLIENT_SHEET_NAME), clientName);
        String username = ImportHandlerUtils.readAsString(SelfServiceUserConstants.USERNAME_COL, row);
        String firstname = ImportHandlerUtils.readAsString(SelfServiceUserConstants.FIRSTNAME_COL, row);
        String lastname = ImportHandlerUtils.readAsString(SelfServiceUserConstants.LASTNAME_COL, row);
        String email = ImportHandlerUtils.readAsString(SelfServiceUserConstants.EMAIL_COL, row);
        String status = ImportHandlerUtils.readAsString(SelfServiceUserConstants.STATUS_COL, row);
        statuses.add(status);
        List<Long> clientIds = new ArrayList<>();
        clientIds.add(clientId);

        List<Long> rolesIds = new ArrayList<>();
        for (int cellNo = SelfServiceUserConstants.ROLE_NAME_START_COL; cellNo < SelfServiceUserConstants.ROLE_NAME_END_COL; cellNo++) {
            String roleName = ImportHandlerUtils.readAsString(cellNo, row);
            if (roleName == null) break;
            Long roleId = ImportHandlerUtils.getIdByName(workbook.getSheet(TemplatePopulateImportConstants.ROLES_SHEET_NAME), roleName);
            if (!rolesIds.contains(roleId)) rolesIds.add(roleId);
        }

        return AppUserData.importInstance(officeId, staffId, username, firstname, lastname, email, clientIds, rolesIds, row.getRowNum());
    }

    private Count importEntity(String dateFormat) {
        Sheet selfserviceuserSheet = workbook.getSheet(TemplatePopulateImportConstants.SELF_SERVICE_USER_SHEET_NAME);
        int successCount = 0;
        int errorCount = 0;
        String errorMessage = "";
        GsonBuilder gsonBuilder = new GsonBuilder();
        for (AppUserData user : users) {
            try {
                JsonObject userJsonob = gsonBuilder.create().toJsonTree(user).getAsJsonObject();
                userJsonob.addProperty("isSelfServiceUser", true);
                String payload = userJsonob.toString();
                final CommandWrapper commandRequest = new CommandWrapperBuilder() //
                        .createUser() //
                        .withJson(payload) //
                        .build(); //
                final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
                successCount++;
                Cell statusCell = selfserviceuserSheet.getRow(user.getRowIndex()).createCell(SelfServiceUserConstants.STATUS_COL);
                statusCell.setCellValue(TemplatePopulateImportConstants.STATUS_CELL_IMPORTED);
                statusCell.setCellStyle(ImportHandlerUtils.getCellStyle(workbook, IndexedColors.LIGHT_GREEN));

            } catch (RuntimeException ex) {
                errorCount++;
                ex.printStackTrace();
                errorMessage = ImportHandlerUtils.getErrorMessage(ex);
                ImportHandlerUtils.writeErrorMessage(selfserviceuserSheet, user.getRowIndex(), errorMessage,
                        SelfServiceUserConstants.STATUS_COL);
            }
        }
        selfserviceuserSheet.setColumnWidth(SelfServiceUserConstants.STATUS_COL, TemplatePopulateImportConstants.SMALL_COL_SIZE);
        ImportHandlerUtils.writeString(SelfServiceUserConstants.STATUS_COL,
                selfserviceuserSheet.getRow(TemplatePopulateImportConstants.ROWHEADER_INDEX),
                TemplatePopulateImportConstants.STATUS_COL_REPORT_HEADER);
        return Count.instance(successCount, errorCount);
    }

}
