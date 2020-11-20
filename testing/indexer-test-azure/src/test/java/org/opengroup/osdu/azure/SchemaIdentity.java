package org.opengroup.osdu.azure;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.StringJoiner;

public class SchemaIdentity {
    @JsonProperty("authority")
    public final String AUTHORITY = "INDEXER-SERVICE";
    @JsonProperty("source")
    public final String SOURCE = "INTEGRATION-TEST";
    @JsonProperty("entityType")
    public final String ENTITY_TYPE = "SCHEMA";
    private String versionPatch;
    private String versionMinor;
    private String versionMajor;

    public String getVersionPatch() {
        return versionPatch;
    }

    public void setVersionPatch(String versionPatch) {
        this.versionPatch = versionPatch;
    }

    public String getVersionMinor() {
        return versionMinor;
    }

    public void setVersionMinor(String versionMinor) {
        this.versionMinor = versionMinor;
    }

    public String getVersionMajor() {
        return versionMajor;
    }

    public void setVersionMajor(String versionMajor) {
        this.versionMajor = versionMajor;
    }

    @JsonIgnore
    public String getId() {
        return AUTHORITY + ":" + SOURCE + ":" + ENTITY_TYPE + ":" +
                versionMajor + "." + versionMinor + "." + versionPatch;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SchemaIdentity.class.getSimpleName() + "[", "]")
                .add("AUTHORITY='" + AUTHORITY + "'")
                .add("SOURCE='" + SOURCE + "'")
                .add("ENTITY_TYPE='" + ENTITY_TYPE + "'")
                .add("versionPatch='" + versionPatch + "'")
                .add("versionMinor='" + versionMinor + "'")
                .add("versionMajor='" + versionMajor + "'")
                .toString();
    }
}
