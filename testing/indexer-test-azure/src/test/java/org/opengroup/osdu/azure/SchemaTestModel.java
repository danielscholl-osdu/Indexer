package org.opengroup.osdu.azure;

import java.util.StringJoiner;

public class SchemaTestModel {

    private SchemaIdentity schemaIdentity;
    private Object schema;

    public SchemaIdentity getSchemaIdentity() {
        return schemaIdentity;
    }

    public void setSchemaIdentity(SchemaIdentity schemaIdentity) {
        this.schemaIdentity = schemaIdentity;
    }

    public Object getSchema() {
        return schema;
    }

    public void setSchema(Object schema) {
        this.schema = schema;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SchemaTestModel.class.getSimpleName() + "[", "]")
                .add("schemaIdentity=" + schemaIdentity)
                .add("schema=" + schema)
                .toString();
    }
}
