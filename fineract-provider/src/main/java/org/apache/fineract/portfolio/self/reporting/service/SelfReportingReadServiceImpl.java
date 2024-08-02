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
package org.apache.fineract.portfolio.self.reporting.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.portfolio.self.reporting.data.ReportingData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class SelfReportingReadServiceImpl implements SelfReportingReadService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public SelfReportingReadServiceImpl(final RoutingDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Collection<ReportingData> retrievePendingReports() {
        final SavingsReportMapper rm = new SavingsReportMapper();
        final String sql = "select " + rm.reportSchema();
        return this.jdbcTemplate.query(sql, rm, new Object[] {});
    }

    @Override
    public ReportingData retrieveOne(Long reportId) {
        final SavingsReportMapper rm = new SavingsReportMapper();
        final String sql = "select " + rm.singleReportSchema();
        return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { reportId });
    }

    private static final class SavingsReportMapper implements RowMapper<ReportingData> {

        public String reportSchema() {
            return "report_id as id, sum(ifnull(amount, 0)) as transactionAmount, "
                    + "mst.is_rejected as isRejected, mst.is_accepted as isAccepted, mst.client_id as clientId,c.display_name as clientName, mst.payment_type as paymentType, "
                    + "mst.transaction_date as transactionDate, mst.transaction_id as transactionId, mst.payment_proof as paymentProof "
                    + "from m_self_reported_transactions_details msrt JOIN m_self_reported_transactions mst on msrt.report_id = mst.id JOIN m_client c on c.id = mst.client_id "
                    + " where mst.is_accepted = 0 and mst.is_rejected = 0 group by report_id order by mst.transaction_date ASC";
        }

        public String singleReportSchema() {
            return "report_id as id, sum(ifnull(amount, 0)) as transactionAmount, "
                    + "mst.is_rejected as isRejected, mst.is_accepted as isAccepted, mst.client_id as clientId,c.display_name as clientName, mst.payment_type as paymentType, "
                    + "mst.transaction_date as transactionDate, mst.transaction_id as transactionId, mst.payment_proof as paymentProof "
                    + "from m_self_reported_transactions_details msrt JOIN m_self_reported_transactions mst on msrt.report_id = mst.id JOIN m_client c on c.id = mst.client_id "
                    + " where mst.is_accepted = 0 and mst.is_rejected = 0 and msrt.report_id=? group by report_id order by mst.transaction_date ASC";
        }

        @Override
        public ReportingData mapRow(ResultSet rs, int i) throws SQLException {
            final Long id = JdbcSupport.getLong(rs, "id");
            final Long clientId = JdbcSupport.getLong(rs, "clientId");
            final Integer paymentType = JdbcSupport.getInteger(rs, "paymentType");
            final Integer paymentProof = JdbcSupport.getIntegerDefaultToNullIfZero(rs, "paymentProof");
            final Boolean isAccepted = rs.getBoolean("isAccepted");
            final Boolean isRejected = rs.getBoolean("isRejected");
            final BigDecimal transactionAmount = JdbcSupport.getBigDecimalDefaultToNullIfZero(rs, "transactionAmount");
            final Date transactionDate = rs.getDate("transactionDate");
            final String transactionId = rs.getString("transactionId");
            final String clientName = rs.getString("clientName");

            return new ReportingData(id, paymentType, paymentProof, isAccepted, isRejected, transactionAmount, transactionDate,
                    transactionId, clientId, clientName);
        }
    }
}
