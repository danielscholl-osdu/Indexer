package org.opengroup.osdu.indexer.di;

import org.opengroup.osdu.core.gcp.PubSub.PubSubExtensions;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class PubSubExtensionsFactory extends AbstractFactoryBean<PubSubExtensions> {


    @Override
    public Class<?> getObjectType() {
        return PubSubExtensions.class;
    }

    @Override
    protected PubSubExtensions createInstance() throws Exception {
        return new PubSubExtensions();
    }
}
