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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "k_approval_configuration")
public class ApprovalConfiguration extends AbstractPersistableCustom {

    @Column(name = "approval_mode_enum", nullable = false)
    private Integer approvalMode;

    @Column(name = "required_approvers", nullable = false)
    private Integer requiredApprovals;

    @ManyToOne
    @JoinColumn(name = "required_user")
    private AppUser requiredUser;

    public void setApprovalMode(ApprovalMode approvalMode) {
        this.approvalMode = approvalMode.getValue();
    }

    public void setRequiredUser(AppUser requiredUser) {
        this.requiredUser = requiredUser;
    }

    public void setRequiredApprovals(Integer requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    public ApprovalMode getApprovalMode() {
        return ApprovalMode.fromInt(approvalMode);
    }

    public AppUser getRequiredUser() {
        return requiredUser;
    }

    public Integer getRequiredApprovals() {
        return requiredApprovals;
    }
}
