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
package org.apache.fineract.extensions.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /*
     * This entire class is only necessary because the adhoc email notifications exist. Once we have switched over to
     * using campaigns (which respond to events and the like) we should also remove this class completely.
     */

    @Override
    public Executor getAsyncExecutor() {
        final CustomizableThreadFactory threadNameAwareFactory = new CustomizableThreadFactory("threadAsync");

        return new ThreadPoolExecutor(2, 10, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(500), threadNameAwareFactory) {

            @Override
            public void execute(@NotNull Runnable originalTask) {
                final var tenant = ThreadLocalContextUtil.getTenant();
                super.execute(() -> {
                    ThreadLocalContextUtil.setTenant(tenant);
                    originalTask.run();
                });
            }
        };
    }

}
