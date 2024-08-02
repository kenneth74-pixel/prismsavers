/*
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
 *
 */

package org.apache.fineract.extensions.approvals.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.Collection;
import org.apache.fineract.extensions.approvals.data.ApprovalData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class ApprovalReadService {

    private final JdbcTemplate jdbcTemplate;

    public ApprovalReadService(final RoutingDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final class ApprovalMapper implements RowMapper<ApprovalData> {

        public String schema(Long auditId) {
            return " p.id as id, p.submitted_on_date as submitted, if(p.is_approval, 'APPROVED','REJECTED') as vote, "
                    + " u.username as user from k_approvals p left join m_appuser u on p.approver_id = u.id" + " where p.source_command = "
                    + auditId;
        }

        @Override
        public ApprovalData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String user = rs.getString("user");
            final String vote = rs.getString("vote");
            final ZonedDateTime dateTime = JdbcSupport.getDateTime(rs, "submitted");
            return new ApprovalData(id, user, dateTime, vote);
        }
    }

    public Collection<ApprovalData> retrieveApprovals(Long auditId) {
        final ApprovalReadService.ApprovalMapper rm = new ApprovalReadService.ApprovalMapper();
        String sqlBuilder = "select" + rm.schema(auditId);
        return this.jdbcTemplate.query(sqlBuilder, rm);
    }
}
