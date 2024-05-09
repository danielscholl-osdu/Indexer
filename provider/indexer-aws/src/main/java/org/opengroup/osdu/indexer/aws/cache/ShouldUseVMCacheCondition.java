/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.aws.cache;

import org.opengroup.osdu.core.aws.ssm.K8sLocalParameterProvider;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ShouldUseVMCacheCondition implements Condition {

    K8sLocalParameterProvider provider;
    boolean disableCache;

    public ShouldUseVMCacheCondition() {
        this(new K8sLocalParameterProvider(), Boolean.parseBoolean(System.getenv("DISABLE_CACHE")));
    }

    public ShouldUseVMCacheCondition(K8sLocalParameterProvider provider, boolean disableCache) {
        this.provider = provider;
        this.disableCache = disableCache;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return provider.getLocalMode() && !disableCache;
    }
}
