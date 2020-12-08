package org.opengroup.osdu.indexer.azure.di;

import org.opengroup.osdu.indexer.service.IndexSchemaServiceImpl;
import org.opengroup.osdu.indexer.service.SchemaService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

@Primary
@Service
public class IndexSchemaServiceOverrideImpl extends IndexSchemaServiceImpl {

    private final SchemaService schemaService;

    @Inject
    public IndexSchemaServiceOverrideImpl(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    protected String getSchema(String kind) throws URISyntaxException, UnsupportedEncodingException {
        return this.schemaService.getSchema(kind);
    }
}
