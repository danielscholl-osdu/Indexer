/*
 * Copyright © Schlumberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexer.cache;

import org.opengroup.osdu.core.common.cache.VmCache;
import org.opengroup.osdu.indexer.model.indexproperty.ParentChildRelatedObjectsSpec;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParentChildRelatedObjectsSpecsCache extends VmCache<String, List<ParentChildRelatedObjectsSpec>> {
    public ParentChildRelatedObjectsSpecsCache() {
        super(600, 1000);
    }
}
