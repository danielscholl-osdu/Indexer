package org.opengroup.osdu.indexer.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengroup.osdu.core.common.Constants;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.indexer.IndexSchema;
import org.opengroup.osdu.core.common.model.indexer.JobStatus;
import org.opengroup.osdu.indexer.schema.converter.config.SchemaConverterPropertiesConfig;
import org.opengroup.osdu.indexer.util.parser.BooleanParser;
import org.opengroup.osdu.indexer.util.parser.DateTimeParser;
import org.opengroup.osdu.indexer.util.parser.GeoShapeParser;
import org.opengroup.osdu.indexer.util.parser.NumberParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {StorageIndexerPayloadMapper.class, AttributeParsingServiceImpl.class, NumberParser.class,
	BooleanParser.class, DateTimeParser.class,
	GeoShapeParser.class, GeometryConversionService.class, JobStatus.class, SchemaConverterPropertiesConfig.class,JaxRsDpsLog.class})
public class StorageIndexerPayloadMapperTest {

	public static final String FIRST_OBJECT_INNER_PROPERTY = "FirstObjectInnerProperty";
	public static final String SECOND_OBJECT_INNER_PROPERTY = "SecondObjectInnerProperty";
	public static final String FIRST_OBJECT_TEST_VALUE = "first-object-test-value";
	public static final String SECOND_OBJECT_TEST_VALUE = "second-object-test-value";
	public static final String OBJECT_PROPERTY = "ObjectProperty";
	public static final String NESTED_PROPERTY = "NestedProperty";
	public static final String FIRST_NESTED_INNER_PROPERTY = "FirstNestedInnerProperty";
	public static final String SECOND_NESTED_INNER_PROPERTY = "SecondNestedInnerProperty";
	public static final String FIRST_NESTED_VALUE = "first-nested-value";
	public static final String SECOND_NESTED_VALUE = "second-nested-value";
	public static final String FLATTENED_PROPERTY = "FlattenedProperty";
	public static final String FIRST_FLATTENED_INNER_PROPERTY = "FirstFlattenedInnerProperty";
	public static final String SECOND_FLATTENED_INNER_PROPERTY = "SecondFlattenedInnerProperty";
	public static final String FIRST_FLATTENED_TEST_VALUE = "first-flattened-test-value";
	public static final String SECOND_FLATTENED_TEST_VALUE = "second-flattened-test-value";
	public static final String RECORD_TEST_ID = "test-id";

	private static IndexSchema indexSchema;
	private static Map<String, Object> storageRecordData;

	@Autowired
	private StorageIndexerPayloadMapper payloadMapper;

	@BeforeClass
	public static void setUp() {
		HashMap<String, Object> dataMap = new HashMap<>();
		dataMap.put("TextProperty", "text");
		dataMap.put("TextArrayProperty", "text_array");
		dataMap.put("DoubleProperty", "double");
		dataMap.put(OBJECT_PROPERTY, "object");
		dataMap.put(FLATTENED_PROPERTY, "flattened");
		dataMap.put(NESTED_PROPERTY, ImmutableMap.of(
			Constants.TYPE, "nested",
			Constants.PROPERTIES, ImmutableMap.of(
				FIRST_NESTED_INNER_PROPERTY, "text",
				SECOND_NESTED_INNER_PROPERTY, "double")
		));
		dataMap.put("DateProperty", "date");
		indexSchema = IndexSchema.builder().kind("kind").type(Constants.TYPE).dataSchema(dataMap).build();

		storageRecordData = new HashMap<>();
		storageRecordData.put("TextProperty", "Testing");
		storageRecordData.put("TextArrayProperty", Arrays.asList("test", "test-value"));
		storageRecordData.put("DoubleProperty", "0.1");

		storageRecordData.put(OBJECT_PROPERTY, Arrays.asList(
			ImmutableMap.of(FIRST_OBJECT_INNER_PROPERTY, FIRST_OBJECT_TEST_VALUE),
			ImmutableMap.of(SECOND_OBJECT_INNER_PROPERTY, SECOND_OBJECT_TEST_VALUE)
		));

		storageRecordData.put(FLATTENED_PROPERTY, Arrays.asList(
			ImmutableMap.of(FIRST_FLATTENED_INNER_PROPERTY, FIRST_FLATTENED_TEST_VALUE),
			ImmutableMap.of(SECOND_FLATTENED_INNER_PROPERTY, SECOND_FLATTENED_TEST_VALUE)
		));

		storageRecordData.put(NESTED_PROPERTY, Arrays.asList(
			ImmutableMap.of(FIRST_NESTED_INNER_PROPERTY, FIRST_NESTED_VALUE, SECOND_NESTED_INNER_PROPERTY,"0.1"),
			ImmutableMap.of(FIRST_NESTED_INNER_PROPERTY, SECOND_NESTED_VALUE, SECOND_NESTED_INNER_PROPERTY,"0.2")
		));
		storageRecordData.put("DateProperty", "2021-03-02T00:17:20.640Z");
	}

	@Test
	public void mapDataPayloadTestNested() {
		Map<String, Object> stringObjectMap = payloadMapper.mapDataPayload(indexSchema, storageRecordData,
			RECORD_TEST_ID);
		Object nestedProperty = stringObjectMap.get(NESTED_PROPERTY);

		assertTrue(nestedProperty instanceof List);
		List<Map<String, Object>> nestedProperty1 = (List<Map<String, Object>>) nestedProperty;
		Object firstNestedInnerProperty = nestedProperty1.get(0).get(FIRST_NESTED_INNER_PROPERTY);
		assertEquals(FIRST_NESTED_VALUE,firstNestedInnerProperty);
		Object secondNestedInnerProperty = nestedProperty1.get(0).get(SECOND_NESTED_INNER_PROPERTY);
		assertEquals(0.1,secondNestedInnerProperty);
		Object firstNestedInnerProperty1 = nestedProperty1.get(1).get(FIRST_NESTED_INNER_PROPERTY);
		assertEquals(SECOND_NESTED_VALUE,firstNestedInnerProperty1);
		Object secondNestedInnerProperty1 = nestedProperty1.get(1).get(SECOND_NESTED_INNER_PROPERTY);
		assertEquals(0.2,secondNestedInnerProperty1);
	}

	@Test
	public void mapDataPayloadTestFlattened() {
		Map<String, Object> stringObjectMap = payloadMapper.mapDataPayload(indexSchema, storageRecordData,
			RECORD_TEST_ID);
		Object objectProperty = stringObjectMap.get(FLATTENED_PROPERTY);

		assertTrue(objectProperty instanceof List);
		List<Map<String, Object>> objectProperties = (List<Map<String, Object>>) objectProperty;
		Object firstInnerProperty = objectProperties.get(0).get(FIRST_FLATTENED_INNER_PROPERTY);
		assertEquals(FIRST_FLATTENED_TEST_VALUE,firstInnerProperty);
		Object secondInnerProperty = objectProperties.get(1).get(SECOND_FLATTENED_INNER_PROPERTY);
		assertEquals(SECOND_FLATTENED_TEST_VALUE,secondInnerProperty);
	}

	@Test
	public void mapDataPayloadTestObject() {
		Map<String, Object> stringObjectMap = payloadMapper.mapDataPayload(indexSchema, storageRecordData,
			RECORD_TEST_ID);
		Object objectProperty = stringObjectMap.get(OBJECT_PROPERTY);

		assertTrue(objectProperty instanceof List);
		List<Map<String, Object>> objectProperties = (List<Map<String, Object>>) objectProperty;
		Object firstInnerProperty = objectProperties.get(0).get(FIRST_OBJECT_INNER_PROPERTY);
		assertEquals(FIRST_OBJECT_TEST_VALUE,firstInnerProperty);
		Object secondInnerProperty = objectProperties.get(1).get(SECOND_OBJECT_INNER_PROPERTY);
		assertEquals(SECOND_OBJECT_TEST_VALUE,secondInnerProperty);
	}
}