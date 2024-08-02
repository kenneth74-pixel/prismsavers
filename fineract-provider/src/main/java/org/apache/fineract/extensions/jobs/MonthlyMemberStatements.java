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
package org.apache.fineract.extensions.jobs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.PlatformEmailService;
import org.apache.fineract.infrastructure.core.service.RoutingDataSourceServiceFactory;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service(value = "monthlyMemberStatements")
public class MonthlyMemberStatements {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyMemberStatements.class);
    private final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

    private final ConfigurationDomainService configService;
    private final RoutingDataSourceServiceFactory dataSourceServiceFactory;
    private final PlatformEmailService emailService;

    public MonthlyMemberStatements(ConfigurationDomainService configService, RoutingDataSourceServiceFactory dataSourceServiceFactory,
            PlatformEmailService emailService) {
        this.configService = configService;
        this.dataSourceServiceFactory = dataSourceServiceFactory;
        this.emailService = emailService;
    }

    @CronTarget(jobName = JobName.MONTHLY_MEMBER_STATEMENTS)
    public void generateMonthlyStatements() {
        if (!this.configService.isMonthlyStatementsEnabled()) {
            return;
        }
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(this.dataSourceServiceFactory.determineDataSourceService().retrieveDataSource());

        String membersQuery = "select mc.firstname, mc.lastname, ma.email, ma.username, mc.id"
                + " from m_appuser ma join m_selfservice_user_client_mapping msucm on msucm.appuser_id = ma.id"
                + " join m_client mc on msucm.client_id = mc.id where ma.email is not null and mc.status_enum = ?";
        List<Map<String, String>> members = jdbcTemplate.query(membersQuery, (r, rowNum) -> {
            Map<String, String> row = new HashMap<>();
            row.put("firstName", r.getString("firstname"));
            row.put("lastName", r.getString("lastname"));
            row.put("email", r.getString("email"));
            row.put("username", r.getString("username"));
            row.put("clientId", "id");
            return row;
        }, ClientStatus.ACTIVE.getValue());

        logger.info("generating monthly statements for {} members", members.size());
        LocalDateTime startDate = LocalDateTime.now().minusMonths(1).withDayOfMonth(1).withTime(0, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.now().withDayOfMonth(1).withTime(0, 0, 0, 0);

        for (Map<String, String> member : members) {
            Object[] params = new Object[] { member.get("clientId"), formatter.print(startDate), formatter.print(endDate) };
            String savingsQuery = "select transaction_date, msa.account_no, rev.enum_value, currency_code, amount, running_balance_derived"
                    + " from m_savings_account_transaction join m_savings_account msa on m_savings_account_transaction.savings_account_id = msa.id"
                    + " join r_enum_value rev on transaction_type_enum = rev.enum_id and rev.enum_name = 'savings_transaction_type_enum'"
                    + " where is_reversed = 0 and msa.client_id = ? and transaction_date between ? and ?"
                    + " order by msa.account_no, transaction_date";
            List<Map<String, String>> sRecords = jdbcTemplate.query(savingsQuery, (r, rn) -> {
                Map<String, String> row = mapBase(r);
                row.put("running_balance", r.getString("running_balance_derived"));
                return row;

            }, params);
            logger.info("savings transaction records: {}", sRecords.size());
            String loansQuery = "select transaction_date, ml.account_no, rev.enum_value, currency_code, amount, outstanding_loan_balance_derived"
                    + " from m_loan_transaction join m_loan ml on m_loan_transaction.loan_id = ml.id"
                    + " join r_enum_value rev on transaction_type_enum = rev.enum_id and rev.enum_name = 'loan_transaction_type_enum'"
                    + " where is_reversed = 0 and ml.client_id = ? and transaction_date between ? and ?"
                    + " order by ml.account_no, transaction_date";

            List<Map<String, String>> lRecords = jdbcTemplate.query(loansQuery, (r, rn) -> {
                Map<String, String> row = mapBase(r);
                row.put("running_balance", r.getString("outstanding_loan_balance_derived"));
                return row;
            }, params);
            logger.info("loan transaction records: {}", lRecords.size());
            this.emailService.sendMonthlyStatementsEmail(member.get("email"), member.get("username"), member.get("firstName"),
                    formatter.print(startDate), formatter.print(endDate), lRecords, sRecords);
        }
    }

    private Map<String, String> mapBase(ResultSet r) throws SQLException {
        Map<String, String> row = new HashMap<>();
        row.put("transactionDate", r.getString("transaction_date"));
        row.put("account", r.getString("account_no"));
        row.put("transactionType", StringUtils.capitalize(r.getString("enum_value").toLowerCase()));
        row.put("amount", r.getString("amount"));
        return row;
    }
}
