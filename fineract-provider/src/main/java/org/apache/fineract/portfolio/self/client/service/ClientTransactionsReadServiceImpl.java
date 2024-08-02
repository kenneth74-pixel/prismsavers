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
package org.apache.fineract.portfolio.self.client.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.portfolio.client.domain.ClientEnumerations;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionEnumData;
import org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionEnumData;
import org.apache.fineract.portfolio.savings.service.SavingsEnumerations;
import org.apache.fineract.portfolio.self.client.data.TransactionSummaryData;
import org.apache.fineract.portfolio.self.client.data.TransactionsSummaryCollectionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ClientTransactionsReadServiceImpl implements ClientTransactionsReadService {

    private final JdbcTemplate jdbcTemplate;
    private final ClientReadPlatformService clientReadPlatformService;

    @Autowired
    public ClientTransactionsReadServiceImpl(ClientReadPlatformService clientReadPlatformService, final RoutingDataSource dataSource) {
        this.clientReadPlatformService = clientReadPlatformService;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public TransactionsSummaryCollectionData retrieveTransactions(Long clientId) {
        this.clientReadPlatformService.retrieveOne(clientId);
        final String loanwhereClause = " where l.client_id = ? ORDER BY tr.transaction_date DESC LIMIT 5";
        final String savingswhereClause = " where sa.client_id = ? ORDER BY tr.transaction_date DESC LIMIT 5";
        final String clientwhereClause = " where c.id = ? ORDER BY tr.transaction_date DESC LIMIT 5";
        final List<TransactionSummaryData> savingsTransactions = retrieveTrxDetails(savingswhereClause, new Object[] { clientId });
        final List<TransactionSummaryData> loanTransactions = retrieveLoanTrxDetails(loanwhereClause, new Object[] { clientId });
        final List<TransactionSummaryData> clientTransactions = retrieveClientTrxDetails(clientwhereClause, new Object[] { clientId });
        return new TransactionsSummaryCollectionData(savingsTransactions, loanTransactions, clientTransactions);
    }

    private List<TransactionSummaryData> retrieveTrxDetails(final String savingswhereClause, final Object[] inputs) {
        final SavingsTransactionsDataMapper savingsTrxDataMapper = new SavingsTransactionsDataMapper();
        final String savingstrxSql = "select " + savingsTrxDataMapper.schema() + savingswhereClause;
        return this.jdbcTemplate.query(savingstrxSql, savingsTrxDataMapper, inputs);
    }

    private List<TransactionSummaryData> retrieveLoanTrxDetails(final String loanwhereClause, final Object[] inputs) {
        final LoanTransactionsDataMapper loanTrxDataMapper = new LoanTransactionsDataMapper();
        final String loantrxSql = "select " + loanTrxDataMapper.schema() + loanwhereClause;
        return this.jdbcTemplate.query(loantrxSql, loanTrxDataMapper, inputs);
    }

    private List<TransactionSummaryData> retrieveClientTrxDetails(final String clientwhereClause, final Object[] inputs) {
        final ClientTransactionsDataMapper clientTrxDataMapper = new ClientTransactionsDataMapper();
        final String clienttrxSql = "select " + clientTrxDataMapper.schema() + clientwhereClause;
        return this.jdbcTemplate.query(clienttrxSql, clientTrxDataMapper, inputs);
    }

    private static final class SavingsTransactionsDataMapper implements RowMapper<TransactionSummaryData> {

        public String schema() {
            return "tr.id as id,tr.transaction_type_enum as transactionType, "
                    + "sa.account_no as accountNumber, tr.transaction_date as transactionDate, tr.amount as amount, sa.currency_code as currencyCode "
                    + "from m_savings_account sa " + "JOIN m_savings_account_transaction tr on tr.savings_account_id = sa.id "
                    + "JOIN m_currency rc on rc.code = sa.currency_code";
        }

        @Override
        public TransactionSummaryData mapRow(ResultSet rs, int i) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final int transactionTypeInt = JdbcSupport.getInteger(rs, "transactionType");
            final SavingsAccountTransactionEnumData transactionType = SavingsEnumerations.transactionType(transactionTypeInt);
            final Date transactionDate = rs.getDate("transactionDate");
            final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "amount");
            final String currencyCode = rs.getString("currencyCode");
            final String accountNumber = rs.getString("accountNumber");

            return TransactionSummaryData.savingsInstance(id, transactionType.getValue(), accountNumber, amount, currencyCode,
                    transactionDate);
        }
    }

    private static final class LoanTransactionsDataMapper implements RowMapper<TransactionSummaryData> {

        public String schema() {
            return "tr.id as id,tr.transaction_type_enum as transactionType, "
                    + "l.account_no as accountNumber, tr.transaction_date as transactionDate, tr.amount as amount, l.currency_code as currencyCode "
                    + "from m_loan l " + "JOIN m_loan_transaction tr on tr.loan_id = l.id "
                    + "JOIN m_currency rc on rc.code = l.currency_code";
        }

        @Override
        public TransactionSummaryData mapRow(ResultSet rs, int i) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final int transactionTypeInt = JdbcSupport.getInteger(rs, "transactionType");
            final LoanTransactionEnumData transactionType = LoanEnumerations.transactionType(transactionTypeInt);
            final Date transactionDate = rs.getDate("transactionDate");
            final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "amount");
            final String currencyCode = rs.getString("currencyCode");
            final String accountNumber = rs.getString("accountNumber");

            return TransactionSummaryData.loanInstance(id, transactionType.getValue(), accountNumber, amount, currencyCode,
                    transactionDate);
        }
    }

    private static final class ClientTransactionsDataMapper implements RowMapper<TransactionSummaryData> {

        public String schema() {
            return "tr.id as id,tr.transaction_type_enum as transactionType, "
                    + "tr.transaction_date as transactionDate, tr.amount as amount, tr.currency_code as currencyCode " + "from m_client c "
                    + "JOIN m_client_transaction tr on tr.client_id = c.id ";
        }

        @Override
        public TransactionSummaryData mapRow(ResultSet rs, int i) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final int transactionTypeInt = JdbcSupport.getInteger(rs, "transactionType");
            final EnumOptionData transactionType = ClientEnumerations.clientTransactionType(transactionTypeInt);
            final Date transactionDate = rs.getDate("transactionDate");
            final BigDecimal amount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "amount");
            final String currencyCode = rs.getString("currencyCode");

            return TransactionSummaryData.clientInstance(id, transactionType.getValue(), amount, currencyCode, transactionDate);
        }
    }
}
