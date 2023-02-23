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
import com.google.api.client.util.Strings;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RelatedCondition {
    protected static final String ARRAY_SYMBOL = "[]";

    protected String relatedConditionProperty;

    protected List<String> relatedConditionMatches;

    protected boolean hasValidCondition(String property) {
        if(Strings.isNullOrEmpty(property) ||
                Strings.isNullOrEmpty(relatedConditionProperty) ||
                relatedConditionMatches == null ||
                relatedConditionMatches.isEmpty())
            return false;

        if(property.indexOf(ARRAY_SYMBOL + "." ) <= 0 || property.endsWith(ARRAY_SYMBOL) ||
           relatedConditionProperty.indexOf(ARRAY_SYMBOL + "." ) <= 0 || relatedConditionProperty.endsWith(ARRAY_SYMBOL))
            return false;

        String delimiter = "\\[\\]\\.";
        String[] propertyParts = property.split(delimiter);
        String[] relatedConditionPropertyParts = relatedConditionProperty.split(delimiter);
        if(propertyParts.length != relatedConditionPropertyParts.length || propertyParts.length < 2)
            return false;

        for(int i = 0; i < propertyParts.length -1; i++) {
            if(!propertyParts[i].equals(relatedConditionPropertyParts[i]))
                return false;
        }
        return true;
    }
}
