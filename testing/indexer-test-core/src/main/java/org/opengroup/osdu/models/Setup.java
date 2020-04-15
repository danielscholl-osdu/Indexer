package org.opengroup.osdu.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import org.opengroup.osdu.util.HTTPClient;

@Data
@NoArgsConstructor
public class Setup {
    private String tenantId;
    private String kind;
    private String index;
    private String viewerGroup;
    private String ownerGroup;
    private String mappingFile;
    private String recordFile;
    private String schemaFile;
    private HTTPClient httpClient;
    private Map<String, String> headers;
}
