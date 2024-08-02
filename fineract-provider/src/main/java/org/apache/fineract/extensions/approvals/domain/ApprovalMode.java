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

public enum ApprovalMode {

    QUORUM(1, "approvalMode.quorum"), ALL(2, "approvalMode.all"), LEADER(3, "approvalMode.leader");

    private final Integer value;
    private final String code;

    ApprovalMode(Integer value, String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }

    public static ApprovalMode fromInt(Integer value) {
        return switch (value) {
            case 1 -> QUORUM;
            case 2 -> ALL;
            case 3 -> LEADER;
            default -> throw new RuntimeException("invalid approval mode");
        };
    }
}
