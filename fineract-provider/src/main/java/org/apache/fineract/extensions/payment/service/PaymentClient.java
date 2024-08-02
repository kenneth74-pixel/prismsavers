/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.extensions.payment.service;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentClient {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentClient.class);

    public ResponseEntity<String> postRequest(String path, String jsonPayload) {
        String endpoint = System.getenv("PAYMENT_ENDPOINT");
        String apiKey = System.getenv("PAYMENT_KEY");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Token " + apiKey);

        HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
        RestTemplate restTemplate = new RestTemplate();
        try {
            return restTemplate.exchange(endpoint + path, HttpMethod.POST, request, String.class);
        } catch (RestClientException e) {
            LOG.error("payment gateway failure", e);
            throw new RuntimeException("payment gateway request failed");
        }
    }

    public ResponseEntity<String> getRequest(String path, Map<String, String> query) {
        String endpoint = System.getenv("PAYMENT_ENDPOINT");
        String apiKey = System.getenv("PAYMENT_KEY");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Token " + apiKey);

        HttpEntity<?> request = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        try {
            return restTemplate.exchange(endpoint + path, HttpMethod.GET, request, String.class, query);
        } catch (RestClientException e) {
            LOG.error("payment gateway failure", e);
            throw new RuntimeException("payment gateway request failed");
        }
    }
}
