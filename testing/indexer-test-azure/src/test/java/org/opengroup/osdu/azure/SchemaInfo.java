package org.opengroup.osdu.azure;

import java.util.StringJoiner;

public class SchemaInfo {

    private SchemaIdentity schemaIdentity;
    private SchemaStatus status;

    public SchemaIdentity getSchemaIdentity() {
        return schemaIdentity;
    }

    public void setSchemaIdentity(SchemaIdentity schemaIdentity) {
        this.schemaIdentity = schemaIdentity;
    }

    public SchemaStatus getStatus() {
        return status;
    }

    public void setStatus(SchemaStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SchemaInfo.class.getSimpleName() + "[", "]")
                .add("schemaIdentity=" + schemaIdentity)
                .add("status=" + status)
                .toString();
    }

    public enum SchemaStatus {
        PUBLISHED, OBSOLETE, DEVELOPMENT
    }
}
