package org.opengroup.osdu.indexer.model.indexproperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Strings;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValueExtraction extends RelatedCondition {
    @JsonProperty("ValuePath")
    private String valuePath;

    public boolean isValid() {
        return !Strings.isNullOrEmpty(valuePath);
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

        if(valuePath.indexOf(ARRAY_SYMBOL + "." ) <= 0 || relatedConditionProperty.indexOf(ARRAY_SYMBOL + "." ) <= 0)
            return false;

        String delimiter = "\\[\\]\\.";
        String[] valuePathParts = valuePath.split(delimiter);
        String[] relatedConditionPropertyParts = relatedConditionProperty.split(delimiter);
        return valuePathParts.length == 2 && relatedConditionPropertyParts.length == 2 && valuePathParts[0].equals(relatedConditionPropertyParts[0]);
    }
}
