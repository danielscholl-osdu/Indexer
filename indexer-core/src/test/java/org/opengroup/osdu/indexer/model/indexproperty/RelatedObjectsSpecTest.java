package org.opengroup.osdu.indexer.model.indexproperty;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
public class RelatedObjectsSpecTest {

    @Test
    public void hasValidCondition_return_true() {
        RelatedObjectsSpec spec = new RelatedObjectsSpec();
        spec.setRelatedObjectKind("osdu:wks:master-data--GeoPoliticalEntity:1.");
        spec.setRelatedObjectID("data.GeoContexts[].GeoPoliticalEntityID");
        spec.setRelatedConditionProperty("data.GeoContexts[].GeoTypeID");
        List<String> matches = Arrays.asList("opendes:reference-data--GeoPoliticalEntityType:Country:");
        spec.setRelatedConditionMatches(matches);

        Assert.assertTrue(spec.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_with_null_propertyPath() {
        RelatedObjectsSpec spec = new RelatedObjectsSpec();
        spec.setRelatedObjectKind("osdu:wks:master-data--GeoPoliticalEntity:1.");
        spec.setRelatedObjectID("data.GeoContexts[].GeoPoliticalEntityID");
        Assert.assertFalse(spec.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_with_empty_matches() {
        RelatedObjectsSpec spec = new RelatedObjectsSpec();
        spec.setRelatedObjectKind("osdu:wks:master-data--GeoPoliticalEntity:1.");
        spec.setRelatedObjectID("data.GeoContexts[].GeoPoliticalEntityID");
        spec.setRelatedConditionProperty("data.GeoContexts[].GeoTypeID");
        spec.setRelatedConditionMatches(new ArrayList<>());

        Assert.assertFalse(spec.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_with_unmatched_propertyPath() {
        RelatedObjectsSpec spec = new RelatedObjectsSpec();
        spec.setRelatedObjectKind("osdu:wks:master-data--GeoPoliticalEntity:1.");
        spec.setRelatedObjectID("data.GeoContexts[].GeoPoliticalEntityID");
        spec.setRelatedConditionProperty("data.VerticalMeasurementID[].VerticalMeasurementID");
        List<String> matches = Arrays.asList("KB");
        spec.setRelatedConditionMatches(matches);

        Assert.assertFalse(spec.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_without_nested_property() {
        RelatedObjectsSpec spec = new RelatedObjectsSpec();
        spec.setRelatedObjectKind("osdu:wks:master-data--GeoPoliticalEntity:1.");
        spec.setRelatedObjectID("data.GeoContexts[]");
        spec.setRelatedConditionProperty("data.GeoContexts[]");
        List<String> matches = Arrays.asList("opendes:reference-data--GeoPoliticalEntityType:Country:");
        spec.setRelatedConditionMatches(matches);
        Assert.assertFalse(spec.hasValidCondition());
    }

    @Test
    public void hasValidCondition_return_false_for_none_nested_property() {
        RelatedObjectsSpec spec = new RelatedObjectsSpec();
        spec.setRelatedObjectKind("osdu:wks:master-data--GeoPoliticalEntity:1.");
        spec.setRelatedObjectID("data.GeoContexts.GeoPoliticalEntityID");
        spec.setRelatedConditionProperty("data.GeoContexts.GeoTypeID");
        List<String> matches = Arrays.asList("opendes:reference-data--GeoPoliticalEntityType:Country:");
        spec.setRelatedConditionMatches(matches);
        Assert.assertFalse(spec.hasValidCondition());
    }

}
