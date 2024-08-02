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

import java.util.Date;
import java.util.List;
import org.apache.fineract.commands.domain.CommandSource;
import org.apache.fineract.commands.exception.MultipleUserApprovalsException;
import org.apache.fineract.extensions.approvals.domain.Approval;
import org.apache.fineract.extensions.approvals.domain.ApprovalConfiguration;
import org.apache.fineract.extensions.approvals.domain.ApprovalConfigurationRepository;
import org.apache.fineract.extensions.approvals.domain.ApprovalsRepository;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.UserDomainService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ApprovalWriteService {

    private final PlatformSecurityContext context;
    private final ApprovalsRepository approvalsRepository;
    private final UserDomainService userDomainService;
    private final ApprovalConfigurationRepository approvalSettingsService;

    public ApprovalWriteService(PlatformSecurityContext context, ApprovalsRepository approvalsRepository,
            UserDomainService userDomainService, ApprovalConfigurationRepository approvalSettingsService) {
        this.context = context;
        this.approvalsRepository = approvalsRepository;
        this.userDomainService = userDomainService;
        this.approvalSettingsService = approvalSettingsService;
    }

    /**
     * Creates a new command rejection
     *
     * @param commandSourceInput
     *            the command being approved
     */
    public void reject(final CommandSource commandSourceInput) {
        final AppUser user = this.context.authenticatedUser();

        try {
            Approval vote = new Approval();
            vote.setSourceCommand(commandSourceInput);
            vote.setUserId(user);
            vote.setSubmittedDate(new Date());
            vote.setApproval(false);
            this.approvalsRepository.save(vote);
        } catch (DataIntegrityViolationException e) {
            throw new MultipleUserApprovalsException(commandSourceInput.getId());
        }
    }

    /**
     * Creates a new approval.
     * <p>
     * The result depends on the Approval Setting. <br>
     * QUORUM requires a number of approvals <br>
     * ALL requires everyone's approval <br>
     * LEADER requires a specific approval [and optionally a certain number of others]
     *
     * @param commandSourceInput
     *            the command being approved
     * @return true if approval requirements have been met
     */
    public boolean approve(final CommandSource commandSourceInput) {
        final AppUser user = this.context.authenticatedUser();

        try {
            Approval vote = new Approval();
            vote.setSourceCommand(commandSourceInput);
            vote.setUserId(user);
            vote.setApproval(true);
            vote.setSubmittedDate(new Date());
            this.approvalsRepository.save(vote);
        } catch (DataIntegrityViolationException e) {
            throw new MultipleUserApprovalsException(commandSourceInput.getId());
        }

        final ApprovalConfiguration settings = this.approvalSettingsService.findById(1L).orElseThrow();
        final List<Approval> approvals = this.approvalsRepository.findApprovalsBySourceCommand(commandSourceInput);
        final String commandPermission = commandSourceInput.getActionName() + "_" + commandSourceInput.getEntityName() + "_CHECKER";

        // Quorum works for use-cases without the chairman, e.g. single,any two ,any three , any four,
        // In each of the cases below, we subtract 1 when comparing against approvals.size() because in all these
        // cases, Maker-Checker is ON and it already introduces a single approver
        return switch (settings.getApprovalMode()) {
            case QUORUM -> approvals.size() >= settings.getRequiredApprovals() - 1;
            case ALL -> approvals.size() == getCountOfFSPApprovers(userDomainService, commandPermission);
            case LEADER -> approvals.size() >= (settings.getRequiredApprovals() - 1)
                    && validateRequiredApprovers(approvals, settings, commandSourceInput);
        };
    }

    public boolean validateRequiredApprovers(List<Approval> approvals, ApprovalConfiguration settings, CommandSource commandSourceInput) {
        Boolean leaderExist = false;
        String requiredApprover = settings.getRequiredUser().getUsername();
        String taskMaker = commandSourceInput.getMaker().getUsername();
        for (Approval approver : approvals) {
            if (approver.getUserId().getUsername().equals(requiredApprover) || taskMaker.equals(requiredApprover)) {
                leaderExist = true;
                break;
            }
        }
        return leaderExist;
    }

    public Long getCountOfFSPApprovers(UserDomainService userDomainService, String commandPermission) {
        return userDomainService.findUsersWithPermissions(commandPermission).stream()
                .filter(u -> !u.getisSystemAccount() && !u.getUsername().equals("mifos") && !u.getEmail().contains("kanzucode.com"))
                .count();
    }

}
