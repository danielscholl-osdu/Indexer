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
