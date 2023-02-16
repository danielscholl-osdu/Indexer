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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Strings;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelatedObjectsSpec extends RelatedCondition {
    @JsonProperty("RelatedObjectID")
    private String relatedObjectID;

    @JsonProperty("RelatedObjectKind")
    private String relatedObjectKind;

    public boolean isValid() {
        return !Strings.isNullOrEmpty(relatedObjectID) && !Strings.isNullOrEmpty(relatedObjectKind);
    }

    /**
     * To have a valid hasValidCondition, both relatedConditionProperty and relatedObjectID must refer to the same nested object but different property
     * Only one level of nested is supported for now
     * @return
     */
    public boolean hasValidCondition() {
        if(!isValid() ||
                Strings.isNullOrEmpty(relatedConditionProperty) ||
                relatedConditionMatches == null ||
                relatedConditionMatches.isEmpty())
            return false;

        if(relatedObjectID.indexOf(ARRAY_SYMBOL + "." ) <= 0 || relatedConditionProperty.indexOf(ARRAY_SYMBOL + "." ) <= 0)
            return false;

        String delimiter = "\\[\\]\\.";
        String[] relatedObjectIDParts = relatedObjectID.split(delimiter);
        String[] relatedConditionPropertyParts = relatedConditionProperty.split(delimiter);
        return relatedObjectIDParts.length == 2 && relatedConditionPropertyParts.length == 2 && relatedObjectIDParts[0].equals(relatedConditionPropertyParts[0]);
    }
}
