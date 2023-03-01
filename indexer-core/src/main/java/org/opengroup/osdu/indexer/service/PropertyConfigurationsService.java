package org.opengroup.osdu.indexer.service;

import org.opengroup.osdu.core.common.model.search.RecordChangedMessages;
import org.opengroup.osdu.core.common.model.storage.Schema;
import org.opengroup.osdu.core.common.model.storage.SchemaItem;
import org.opengroup.osdu.indexer.model.indexproperty.PropertyConfigurations;

import java.util.List;
import java.util.Map;

public interface PropertyConfigurationsService {
    PropertyConfigurations getPropertyConfiguration(String kind);

    Map<String, Object> getRelatedObjectData(String relatedObjectKind, String relatedObjectId);

    Map<String, Object> getExtendedProperties(String objectId, Map<String, Object> originalDataMap, PropertyConfigurations propertyConfigurations);

    List<SchemaItem> getExtendedSchemaItems(Schema originalSchema, Map<String, Schema> relatedObjectKindSchemas, PropertyConfigurations propertyConfigurations);

    String resolveConcreteKind(String kind);

    void updateAssociatedRecords(RecordChangedMessages message, Map<String, List<String>> processedKindIdsMap);
}
