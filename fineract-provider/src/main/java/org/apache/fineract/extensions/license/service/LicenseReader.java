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
package org.apache.fineract.extensions.license.service;

import org.apache.fineract.extensions.license.domain.LicenseData;
import org.apache.fineract.extensions.license.exception.LicenseLoadError;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class LicenseReader {

    private final RestTemplate restTemplate;

    public LicenseReader() {
        this.restTemplate = new RestTemplate();
    }

    @Cacheable(value = "licenseAllowed", key = "T(org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil).getTenant().getTenantIdentifier().concat('LICENSE')")
    public LicenseData getLicense() {
        String tenantId = ThreadLocalContextUtil.getTenant().getTenantIdentifier();
        String endpoint = System.getenv("TENANT_MANAGER_API");
        String apiKey = System.getenv("TENANT_MANAGER_KEY");
        String apiEndpoint = endpoint + "/" + tenantId;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + apiKey);

        HttpEntity<?> req = new HttpEntity<>(headers);

        try {
            ResponseEntity<LicenseData> result = restTemplate.exchange(apiEndpoint, HttpMethod.GET, req, LicenseData.class);
            return result.getBody();
        } catch (Exception e) {
            throw new LicenseLoadError();
        }
    }
}
