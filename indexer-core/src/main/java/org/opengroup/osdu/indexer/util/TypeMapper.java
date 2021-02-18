// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.indexer.util;

import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.model.entitlements.AclRole;
import org.opengroup.osdu.core.common.model.indexer.ElasticType;
import org.opengroup.osdu.core.common.model.indexer.Records;
import org.opengroup.osdu.core.common.model.indexer.StorageType;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;

import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.Map;

public class TypeMapper {

    private static final Map<String, String> storageToIndexerType = new HashMap<>();

    private static final Map<String, Object> metaAttributeIndexerType = new HashMap<>();

    static {

        metaAttributeIndexerType.put(RecordMetaAttribute.KIND.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.TYPE.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.ID.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.NAMESPACE.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.VERSION.getValue(), ElasticType.LONG.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.X_ACL.getValue(), ElasticType.KEYWORD.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.ACL.getValue(), getAclIndexerMapping());
        metaAttributeIndexerType.put(RecordMetaAttribute.TAGS.getValue(), ElasticType.FLATTENED.getValue());
        metaAttributeIndexerType.put(RecordMetaAttribute.LEGAL.getValue(), getLegalIndexerMapping());
        metaAttributeIndexerType.put(RecordMetaAttribute.ANCESTRY.getValue(), getAncestryIndexerMapping());
        metaAttributeIndexerType.put(RecordMetaAttribute.INDEX_STATUS.getValue(), getIndexStatusMapping());

        storageToIndexerType.put(StorageType.LINK.getValue(), ElasticType.KEYWORD.getValue());
        storageToIndexerType.put(StorageType.LINK_ARRAY.getValue(), ElasticType.KEYWORD_ARRAY.getValue());
        storageToIndexerType.put(StorageType.BOOLEAN.getValue(), ElasticType.BOOLEAN.getValue());
        storageToIndexerType.put(StorageType.BOOLEAN_ARRAY.getValue(), ElasticType.BOOLEAN_ARRAY.getValue());
        storageToIndexerType.put(StorageType.STRING.getValue(), ElasticType.TEXT.getValue());
        storageToIndexerType.put(StorageType.STRING_ARRAY.getValue(), ElasticType.TEXT_ARRAY.getValue());
        storageToIndexerType.put(StorageType.INT.getValue(), ElasticType.INTEGER.getValue());
        storageToIndexerType.put(StorageType.INT_ARRAY.getValue(), ElasticType.INTEGER_ARRAY.getValue());
        storageToIndexerType.put(StorageType.FLOAT.getValue(), ElasticType.FLOAT.getValue());
        storageToIndexerType.put(StorageType.FLOAT_ARRAY.getValue(), ElasticType.FLOAT_ARRAY.getValue());
        storageToIndexerType.put(StorageType.DOUBLE.getValue(), ElasticType.DOUBLE.getValue());
        storageToIndexerType.put(StorageType.DOUBLE_ARRAY.getValue(), ElasticType.DOUBLE_ARRAY.getValue());
        storageToIndexerType.put(StorageType.LONG.getValue(), ElasticType.LONG.getValue());
        storageToIndexerType.put(StorageType.LONG_ARRAY.getValue(), ElasticType.LONG_ARRAY.getValue());
        storageToIndexerType.put(StorageType.DATETIME.getValue(), ElasticType.DATE.getValue());
        storageToIndexerType.put(StorageType.DATETIME_ARRAY.getValue(), ElasticType.DATE_ARRAY.getValue());
        storageToIndexerType.put(StorageType.GEO_POINT.getValue(), ElasticType.GEO_POINT.getValue());
        storageToIndexerType.put(StorageType.GEO_SHAPE.getValue(), ElasticType.GEO_SHAPE.getValue());
    }


    public static String getIndexerType(String storageType) {
        String indexedType = storageToIndexerType.getOrDefault(storageType, null);
        if (indexedType != null && indexedType.endsWith("_array")) {
            return StringUtils.substringBefore(indexedType, "_");
        }
        return indexedType;
    }

    public static Object getIndexerType(RecordMetaAttribute attribute) {
        return metaAttributeIndexerType.getOrDefault(attribute.getValue(), null);
    }

    private static Object getAclIndexerMapping() {
        Map<String, Object> aclRoleMapping = new HashMap<>();
        aclRoleMapping.put(AclRole.VIEWERS.getValue() , Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());
        aclRoleMapping.put(AclRole.OWNERS.getValue(), Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());

        Map<String, Object> aclProperties = new HashMap<>();
        aclProperties.put(Constants.PROPERTIES, aclRoleMapping);

        return aclProperties;
    }

    private static Object getLegalIndexerMapping() {
        Map<String, Object> legalComplianceMapping = new HashMap<>();
        legalComplianceMapping.put("legaltags", Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());
        legalComplianceMapping.put("otherRelevantDataCountries", Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());
        legalComplianceMapping.put("status", Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());

        Map<String, Object> legalProperties = new HashMap<>();
        legalProperties.put(Constants.PROPERTIES, legalComplianceMapping);

        return legalProperties;
    }

    private static Object getAncestryIndexerMapping() {
        Map<String, Object> ancestryMapping = new HashMap<>();
        ancestryMapping.put("parents", Records.Type.builder().type(ElasticType.KEYWORD.getValue()).build());

        Map<String, Object> ancestryProperties = new HashMap<>();
        ancestryProperties.put(Constants.PROPERTIES, ancestryMapping);

        return ancestryProperties;
    }

    private static Object getIndexStatusMapping() {
        Map<String, Object> indexStatusMapping = new HashMap<>();
        indexStatusMapping.put("statusCode", Records.Type.builder().type(ElasticType.INTEGER.getValue()).build());
        indexStatusMapping.put("trace", Records.Type.builder().type(ElasticType.TEXT.getValue()).build());
        indexStatusMapping.put("lastUpdateTime", Records.Type.builder().type(ElasticType.DATE.getValue()).build());

        Map<String, Object> indexStatusProperties = new HashMap<>();
        indexStatusProperties.put(Constants.PROPERTIES, indexStatusMapping);

        return indexStatusProperties;
    }
}