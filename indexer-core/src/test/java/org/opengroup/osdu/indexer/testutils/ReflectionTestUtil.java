package org.opengroup.osdu.indexer.testutils;

import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;

public class ReflectionTestUtil {

    public static <T> void setFieldValueForClass(T targetObject, String fieldName, Object fieldValue) {
        Field field = ReflectionUtils.findField(targetObject.getClass(), fieldName);
        field.setAccessible(true);
        ReflectionUtils.setField(field, targetObject, fieldValue);
    }
}
