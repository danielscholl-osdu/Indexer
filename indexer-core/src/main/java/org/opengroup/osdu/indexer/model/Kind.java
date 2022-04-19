package org.opengroup.osdu.indexer.model;

import lombok.Getter;

@Getter
public class Kind {

    private String kind;
    private String authority;
    private String source;
    private String type;
    private String version;

    public Kind(String kind) {
        this.kind = kind;
        String[] parts = this.kind.split(":");

        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid Kind, must be in format: authority:source:type:version");
        }

        this.authority = parts[0];
        this.source = parts[1];
        this.type = parts[2];
        this.version = parts[3];
    }
}
