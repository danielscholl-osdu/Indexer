package org.opengroup.osdu.indexer.schema.converter.interfaces;

public interface SchemaToStorageFormat {
    String convertToString(String schemaServiceFormat, String kind);
}
