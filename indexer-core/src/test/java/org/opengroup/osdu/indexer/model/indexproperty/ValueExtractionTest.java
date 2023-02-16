package org.opengroup.osdu.indexer.model.indexproperty;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
public class ValueExtractionTest {
    @Test
    public void hasValidCondition_return_true() {
        ValueExtraction valueExtraction = new ValueExtraction();
        valueExtraction.setValuePath("data.NameAliases[].AliasName");
        valueExtraction.setRelatedConditionProperty("data.NameAliases[].AliasNameTypeID");
        List<String> matches = Arrays.asList("opendes:reference-data--AliasNameType:UniqueIdentifier:","reference-data--AliasNameType:RegulatoryName:");
        valueExtraction.setRelatedConditionMatches(matches);

        Assert.assertTrue(valueExtraction.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_with_null_propertyPath() {
        ValueExtraction valueExtraction = new ValueExtraction();
        valueExtraction.setValuePath("data.NameAliases[].AliasName");
        Assert.assertFalse(valueExtraction.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_with_empty_matches() {
        ValueExtraction valueExtraction = new ValueExtraction();
        valueExtraction.setValuePath("data.NameAliases[].AliasName");
        valueExtraction.setRelatedConditionProperty("data.NameAliases[].AliasNameTypeID");
        valueExtraction.setRelatedConditionMatches(new ArrayList<>());

        Assert.assertFalse(valueExtraction.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_with_unmatched_propertyPath() {
        ValueExtraction valueExtraction = new ValueExtraction();
        valueExtraction.setValuePath("data.NameAliases[].AliasName");
        valueExtraction.setRelatedConditionProperty("data.VerticalMeasurementID[].VerticalMeasurementID");
        List<String> matches = Arrays.asList("KB");
        valueExtraction.setRelatedConditionMatches(matches);

        Assert.assertFalse(valueExtraction.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_without_nested_property() {
        ValueExtraction valueExtraction = new ValueExtraction();
        valueExtraction.setValuePath("data.NameAliases[]");
        valueExtraction.setRelatedConditionProperty("data.NameAliases[]");
        List<String> matches = Arrays.asList("opendes:reference-data--AliasNameType:UniqueIdentifier:","reference-data--AliasNameType:RegulatoryName:");
        valueExtraction.setRelatedConditionMatches(matches);
        Assert.assertFalse(valueExtraction.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_for_none_nested_property() {
        ValueExtraction valueExtraction = new ValueExtraction();
        valueExtraction.setValuePath("data.NameAliases.AliasName");
        valueExtraction.setRelatedConditionProperty("data.NameAliases.AliasNameTypeID");
        List<String> matches = Arrays.asList("opendes:reference-data--AliasNameType:UniqueIdentifier:","reference-data--AliasNameType:RegulatoryName:");
        valueExtraction.setRelatedConditionMatches(matches);
        Assert.assertFalse(valueExtraction.hasValidCondition());
    }
}
