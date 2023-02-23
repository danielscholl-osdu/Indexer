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

package org.opengroup.osdu.indexer.model.indexproperty;

import lombok.Data;

@Data
public class ParentChildRelatedObjectsSpec {
    private String parentKind;
    private String parentObjectId;
    private String childKind;

    @Override
    public boolean equals(Object another) {
        if(another == null || !(another instanceof ParentChildRelatedObjectsSpec))
            return false;

        ParentChildRelatedObjectsSpec anotherSpec = (ParentChildRelatedObjectsSpec)another;
        return anotherSpec.hashCode() == this.hashCode();
    }

    @Override
    public int hashCode() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append((parentKind != null)? parentKind : "__");
        stringBuilder.append("<>");
        stringBuilder.append((childKind != null)? childKind : "__");
        stringBuilder.append("<>");
        stringBuilder.append((parentObjectId != null)? parentObjectId : "__");
        return stringBuilder.toString().hashCode();
    }
}
