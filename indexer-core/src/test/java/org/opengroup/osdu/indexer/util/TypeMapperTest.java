package org.opengroup.osdu.indexer.util;

import org.junit.Test;
import org.opengroup.osdu.core.common.model.indexer.ElasticType;
import org.opengroup.osdu.core.common.model.indexer.StorageType;
import org.opengroup.osdu.core.common.model.search.RecordMetaAttribute;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TypeMapperTest {

    @Test
    public void getIndexerTypeTest() {
        assertEquals(ElasticType.KEYWORD.getValue(), TypeMapper.getIndexerType(StorageType.LINK.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.KEYWORD_ARRAY.getValue(), TypeMapper.getIndexerType(StorageType.LINK_ARRAY.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.BOOLEAN.getValue(), TypeMapper.getIndexerType(StorageType.BOOLEAN.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.TEXT.getValue(), TypeMapper.getIndexerType(StorageType.STRING.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.INTEGER.getValue(), TypeMapper.getIndexerType(StorageType.INT.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.FLOAT.getValue(), TypeMapper.getIndexerType(StorageType.FLOAT.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.DOUBLE.getValue(), TypeMapper.getIndexerType(StorageType.DOUBLE.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.DOUBLE_ARRAY.getValue(), TypeMapper.getIndexerType(StorageType.DOUBLE_ARRAY.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.LONG.getValue(), TypeMapper.getIndexerType(StorageType.LONG.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.DATE.getValue(), TypeMapper.getIndexerType(StorageType.DATETIME.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.GEO_POINT.getValue(), TypeMapper.getIndexerType(StorageType.GEO_POINT.getValue(), ElasticType.TEXT.getValue()));
        assertEquals(ElasticType.GEO_SHAPE.getValue(), TypeMapper.getIndexerType(StorageType.GEO_SHAPE.getValue(), ElasticType.TEXT.getValue()));

        assertEquals(ElasticType.KEYWORD.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.KIND));
        assertEquals(ElasticType.KEYWORD.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.TYPE));
        assertEquals(ElasticType.KEYWORD.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.ID));
        assertEquals(ElasticType.KEYWORD.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.NAMESPACE));
        assertEquals(ElasticType.LONG.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.VERSION));
        assertEquals(ElasticType.KEYWORD.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.X_ACL));

        assertEquals(ElasticType.CONSTANT_KEYWORD.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.AUTHORITY));
        assertEquals(ElasticType.CONSTANT_KEYWORD.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.SOURCE));
        assertEquals(ElasticType.KEYWORD.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.CREATE_USER));
        assertEquals(ElasticType.KEYWORD.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.MODIFY_USER));
        assertEquals(ElasticType.DATE.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.CREATE_TIME));
        assertEquals(ElasticType.DATE.getValue(), TypeMapper.getIndexerType(RecordMetaAttribute.MODIFY_TIME));
    }

    @Test
    public void validate_meta_attributes() {
        List<String> keys = TypeMapper.getMetaAttributesKeys();

        String[] meta = new String[] {"id", "kind", "authority", "source", "namespace", "type", "version", "acl", "tags", "legal", "ancestry", "createUser", "modifyUser", "createTime", "modifyTime", "index"};
        for(String attributeKey : meta) {
            assertTrue(keys.contains(attributeKey));
        }
    }
}
