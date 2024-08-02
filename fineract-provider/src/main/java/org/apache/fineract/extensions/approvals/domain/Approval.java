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
package org.apache.fineract.extensions.approvals.domain;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "k_approvals", uniqueConstraints = { @UniqueConstraint(columnNames = { "source_command", "approver_id" }), })
public class Approval extends AbstractPersistableCustom {

    @Temporal(TemporalType.DATE)
    @Column(name = "submitted_on_date", nullable = false)
    private Date submittedDate;

    @ManyToOne
    @JoinColumn(name = "source_command", nullable = false)
    private CommandSource sourceCommand;

    @ManyToOne
    @JoinColumn(name = "approver_id", nullable = false)
    private AppUser userId;

    @Column(name = "is_approval", nullable = false)
    private boolean isApproval;

    public Date getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(Date submittedDate) {
        this.submittedDate = submittedDate;
    }

    public CommandSource getSourceCommand() {
        return sourceCommand;
    }

    public void setSourceCommand(CommandSource sourceCommand) {
        this.sourceCommand = sourceCommand;
    }

    public AppUser getUserId() {
        return userId;
    }

    public void setUserId(AppUser userId) {
        this.userId = userId;
    }

    public boolean isApproval() {
        return isApproval;
    }

    public void setApproval(boolean approval) {
        isApproval = approval;
    }
}
