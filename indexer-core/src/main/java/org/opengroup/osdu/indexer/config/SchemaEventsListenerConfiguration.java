package org.opengroup.osdu.indexer.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class SchemaEventsListenerConfiguration {

    @Value("${listner.schema.event.create:true}")
    private boolean listenCreateEvent;

    @Value("${listner.schema.event.update:true}")
    private boolean listenUpdateEvent;
}
