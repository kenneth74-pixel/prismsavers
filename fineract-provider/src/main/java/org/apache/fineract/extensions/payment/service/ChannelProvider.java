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
package org.apache.fineract.extensions.payment.service;

import java.util.HashMap;
import org.apache.fineract.extensions.payment.annotations.PayChannel;
import org.apache.fineract.extensions.payment.channels.Channel;
import org.apache.fineract.extensions.payment.domain.PaymentChannel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("singleton")
public class ChannelProvider implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelProvider.class);
    private ApplicationContext applicationContext;
    private HashMap<String, String> registeredHandlers;

    public Channel getHandler(PaymentChannel channel) {
        String handlerBean = this.registeredHandlers.get(channel.getCode());
        return (Channel) this.applicationContext.getBean(handlerBean);
    }

    private void initializeHandlerRegistry() {
        if (this.registeredHandlers == null) {
            this.registeredHandlers = new HashMap<>();
            final String[] handlerBeans = this.applicationContext.getBeanNamesForAnnotation(PayChannel.class);
            for (String name : handlerBeans) {
                try {
                    final PayChannel paymentChannel = this.applicationContext.findAnnotationOnBean(name, PayChannel.class);
                    assert paymentChannel != null;
                    this.registeredHandlers.put(paymentChannel.channel().getCode(), name);
                } catch (final Throwable th) {
                    LOGGER.error("Unable to register payment channel handler {}", name, th);
                }
            }
        }
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        this.initializeHandlerRegistry();
    }
}
