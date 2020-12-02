package org.opengroup.osdu.models.schema;

import java.util.StringJoiner;

public class SchemaModel {

    private SchemaInfo schemaInfo;
    private Object schema;

    public Object getSchema() {
        return schema;
    }

    public void setSchema(Object schema) {
        this.schema = schema;
    }

    public SchemaInfo getSchemaInfo() {
        return schemaInfo;
    }

    public void setSchemaInfo(SchemaInfo schemaInfo) {
        this.schemaInfo = schemaInfo;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SchemaModel.class.getSimpleName() + "[", "]")
                .add("schemaInfo=" + schemaInfo)
                .add("schema=" + schema)
                .toString();
    }
}
