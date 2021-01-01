package org.opengroup.osdu.models.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.StringJoiner;

public class SchemaIdentity {

    private  String authority;
    private  String source;
    private  String entityType;
    private String schemaVersionMajor;
    private String schemaVersionMinor;
    private String schemaVersionPatch;

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getSchemaVersionMajor() {
        return schemaVersionMajor;
    }

    public void setSchemaVersionMajor(String schemaVersionMajor) {
        this.schemaVersionMajor = schemaVersionMajor;
    }

    public String getSchemaVersionMinor() {
        return schemaVersionMinor;
    }

    public void setSchemaVersionMinor(String schemaVersionMinor) {
        this.schemaVersionMinor = schemaVersionMinor;
    }

    public String getSchemaVersionPatch() {
        return schemaVersionPatch;
    }

    public void setSchemaVersionPatch(String schemaVersionPatch) {
        this.schemaVersionPatch = schemaVersionPatch;
    }

    @JsonIgnore
    public String getId() {
        return authority + ":" + source + ":" + entityType + ":" +
                schemaVersionMajor + "." + schemaVersionMinor + "." + schemaVersionPatch;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SchemaIdentity.class.getSimpleName() + "[", "]")
                .add("AUTHORITY='" + authority + "'")
                .add("SOURCE='" + source + "'")
                .add("ENTITY_TYPE='" + entityType + "'")
                .add("schemaVersionMajor='" + schemaVersionMajor + "'")
                .add("schemaVersionMinor='" + schemaVersionMinor + "'")
                .add("schemaVersionPatch='" + schemaVersionPatch + "'")
                .toString();
    }
}
