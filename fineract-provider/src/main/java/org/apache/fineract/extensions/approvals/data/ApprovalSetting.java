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

package org.apache.fineract.extensions.approvals.data;

import org.apache.fineract.extensions.approvals.domain.ApprovalConfiguration;

public final class ApprovalSetting {

    @SuppressWarnings("unused")
    private final String approvalMode;
    @SuppressWarnings("unused")
    private final Integer requiredApprovals;
    @SuppressWarnings("unused")
    private final String requiredUser;
    @SuppressWarnings("unused")
    private final Long requiredUserId;

    public ApprovalSetting(ApprovalConfiguration config) {
        var user = config.getRequiredUser();
        var name = user != null ? user.getDisplayName() : null;
        var userId = user != null ? user.getId() : null;

        this.approvalMode = config.getApprovalMode().name();
        this.requiredApprovals = config.getRequiredApprovals();
        this.requiredUser = name;
        this.requiredUserId = userId;
    }
}
